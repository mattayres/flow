/*
 * Copyright 2017 Lithium Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lithium.flow.runner;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;

import com.lithium.flow.config.Config;
import com.lithium.flow.config.ConfigBuilder;
import com.lithium.flow.config.ConfigLoader;
import com.lithium.flow.config.Configs;
import com.lithium.flow.config.loaders.FileConfigLoader;
import com.lithium.flow.filer.Filer;
import com.lithium.flow.shell.Exec;
import com.lithium.flow.shell.Shell;
import com.lithium.flow.util.HostUtils;
import com.lithium.flow.util.Logs;
import com.lithium.flow.util.Main;
import com.lithium.flow.util.Needle;
import com.lithium.flow.util.Sleep;
import com.lithium.flow.util.Threader;
import com.lithium.flow.util.Unchecked;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

/**
 * @author Matt Ayres
 */
public class RunnerMain {
	private static final Logger log = Logs.getLogger();

	private final Config runnerConfig;
	private final Filer destFiler;
	private final VaultRun vaultRun;
	private final RunnerContext context;
	private final String host;
	private Needle runNeedle;
	private Exec runExec;

	public RunnerMain(@Nonnull Config config, @Nonnull Config deployConfig, @Nonnull RunnerContext context)
			throws IOException {
		checkNotNull(config);
		checkNotNull(deployConfig);
		this.context = checkNotNull(context);

		runnerConfig = config.toBuilder().addAll(subConfig(deployConfig, "runner")).build();

		String name = runnerConfig.getString("name");
		host = runnerConfig.getString("host");
		String user = context.getAccess().getLogin(host).getUser();
		log.info("deploying {} to {}@{}", name, user, host);

		vaultRun = new VaultRun(context.getVault(), context.getAccess().getPrompt(), runnerConfig, deployConfig);

		destFiler = new FasterShellFiler(getShell().getFiler(), this::getShell);

		if (runnerConfig.getBoolean("kill.only", false)) {
			kill();
			return;
		}

		RunnerSync sync = new RunnerSync(runnerConfig, context, destFiler);
		sync.sync();

		kill();

		deploy(deployConfig);
	}

	@Nonnull
	private Shell getShell() throws IOException {
		return context.getShore().getShell(host);
	}

	public void deploy(@Nonnull Config deployConfig) throws IOException {
		vaultRun.deploy(destFiler);

		String configOut = runnerConfig.getString("config.out");
		destFiler.createDirs(new File(configOut).getParent());
		writeConfig(deployConfig, destFiler.writeFile(configOut), Charsets.UTF_8);
		log.debug("wrote: {}", configOut);

		String classpath = Joiner.on(":").join(context.getClasspath(runnerConfig.getString("dest.dir")));
		log.debug("classpath: {}", classpath);

		installJava();

		destFiler.close();

		if (!runnerConfig.getBoolean("run", true)) {
			return;
		}

		String prefix = StringUtils.rightPad(runnerConfig.getString("name"), 15, '.') + " ";
		run(prefix, classpath, vaultRun.getEnv());
	}

	private void run(@Nonnull String prefix, @Nonnull String classpath, @Nullable String env) {
		try {
			runNeedle = context.getRunThreader().needle();

			List<String> commands = new ArrayList<>();
			commands.add("export CLASSPATH=" + classpath);
			if (env != null) {
				commands.add(env);
			}
			commands.add("cd " + runnerConfig.getString("dest.dir"));
			for (String link : runnerConfig.getList("links", Configs.emptyList())) {
				Iterator<String> it = Splitter.on(':').split(link).iterator();
				String target = it.next();
				String linkName = it.next();
				commands.add("rm -f " + linkName);
				commands.add("ln -sf " + target + " " + linkName);
			}

			String command = runnerConfig.getString("java.command");
			if (runnerConfig.getTime("relay", "0") > 0) {
				commands.add("export RELAY_COMMAND='" + command + "'");
				commands.add(runnerConfig.getString("relay.command"));
			} else {
				commands.add(command);
			}

			commands.forEach(run -> log.debug("running: {}", run));
			runExec = getShell().exec(commands);
			runNeedle.execute("out@" + host, () -> runExec.out().forEach(line -> System.out.println(prefix + line)));
			runNeedle.execute("err@" + host, () -> runExec.err().forEach(line -> System.err.println(prefix + line)));

		} catch (IOException e) {
			log.warn("exec failed", e);
		}
	}

	private void finish() {
		if (runNeedle != null) {
			runNeedle.finish();
			IOUtils.closeQuietly(runExec);
		}
		log.debug("finished");
	}

	private void writeConfig(@Nonnull Config config, @Nonnull OutputStream out, @Nonnull Charset charset)
			throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, charset));
		Map<String, String> map = runnerConfig.getBoolean("config.sorted", true)
				? config.asSortedMap() : config.asMap();
		for (Map.Entry<String, String> entry : map.entrySet()) {
			writer.write(entry.getKey() + " = " + entry.getValue() + "\r\n");
		}
		writer.close();
	}

	private void installJava() throws IOException {
		String check = runnerConfig.getString("java.check");
		String md5 = runnerConfig.getString("java.md5");
		String install = runnerConfig.getString("java.install");

		String checkMd5 = getShell().exec(check).line();
		log.debug("java check md5: {}", checkMd5);
		if (!md5.equals(checkMd5)) {
			log.debug("java install: " + install);
			getShell().exec(install).exit();
		}
	}

	@Nullable
	private Integer getPid(@Nonnull String key) throws IOException {
		String grep = "grep \"D" + key + "=" + runnerConfig.getString("name") + " \"";
		String command = "ps auxw | " + grep + " | grep -v grep | awk '{ print $2 }'";
		log.debug("running: {}", command);
		String pid = getShell().exec(command).line();
		return pid.isEmpty() ? null : Integer.parseInt(pid);
	}

	private void killPid(@Nonnull Integer pid, boolean force) throws IOException {
		getShell().exec("kill " + (force ? "-9 " : "") + pid).exit();
	}

	private void kill() throws IOException {
		kill("relay");
		kill("name");
	}

	private void kill(@Nonnull String key) throws IOException {
		Integer pid = getPid(key);
		if (pid == null) {
			return;
		}

		log.debug("found existing pid {}", pid);
		if (runnerConfig.getBoolean("kill", false)) {
			killPid(pid, false);
			log.debug("killing pid {}", pid);
			if (!Sleep.softly(5000)) {
				return;
			}
		} else {
			log.debug("aborting");
			return;
		}

		pid = getPid(key);
		if (pid != null) {
			log.debug("failed to kill pid {}", pid);
			if (runnerConfig.getBoolean("kill.force", false)) {
				killPid(pid, true);
				log.debug("force killing pid {}", pid);
				Sleep.softly(2000);
			} else {
				log.debug("aborting");
			}
		}
	}

	@Nonnull
	private static Config subConfig(@Nonnull Config config, @Nonnull String prefix) {
		Config allowConfig = config.toBuilder().allowUndefined(true).build();
		ConfigBuilder builder = Configs.newBuilder();
		for (String key : config.getPrefixKeys(prefix)) {
			builder.setString(key.replace(prefix + ".", ""), allowConfig.getString(key));
		}
		return builder.build();
	}

	public static void main(String[] args) throws Exception {
		Config config = Main.config();
		Logs.configure(config);

		RunnerContext context = new RunnerContext(config);

		List<RunnerMain> runners = new CopyOnWriteArrayList<>();

		for (String path : config.getList("config")) {
			Threader parallel = new Threader(config.getInt("parallel", -1));

			File file = new File(path);
			ConfigLoader loader = new FileConfigLoader(file.getParent());
			Config deployConfig = Configs.newBuilder().addLoader(loader).addAll(subConfig(config, "deploy"))
					.include(file.getAbsolutePath()).build();

			if (deployConfig.containsKey("runner.hosts")) {
				List<String> hosts = deployConfig.getList("runner.hosts", Splitter.on(' '));
				HostUtils.expand(hosts).forEach(runHost -> {
					Config runConfig = deployConfig.toBuilder().setString("runner.host", runHost).build();
					parallel.execute(runHost, () -> runners.add(new RunnerMain(config, runConfig, context)));
				});
			} else {
				List<String> nums = deployConfig.getList("runner.nums", singletonList("0"), Splitter.on(' '));
				HostUtils.expand(nums).forEach(num -> {
					Config runConfig = deployConfig.toBuilder().setString("num", num).build();
					parallel.execute(num, () -> runners.add(new RunnerMain(config, runConfig, context)));
				});
			}

			Thread.sleep(config.getTime("sleep", "0"));
			parallel.finish();
		}

		context.getProgress().finish();

		runners.forEach(runner -> Unchecked.run(runner::finish));
		context.close();
	}
}

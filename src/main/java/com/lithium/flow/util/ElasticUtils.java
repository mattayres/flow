/*
 * Copyright 2015 Lithium Technologies, Inc.
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

package com.lithium.flow.util;

import com.lithium.flow.config.Config;

import java.net.InetAddress;
import java.util.List;

import javax.annotation.Nonnull;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;

import com.google.common.base.Splitter;

/**
 * @author Matt Ayres
 */
public class ElasticUtils {
	private static final Logger log = Logs.getLogger();

	@Nonnull
	public static Client buildClient(@Nonnull Config config) {
		String name = config.getString("elastic.name");
		if (config.getBoolean("elastic.node", false)) {
			return NodeBuilder.nodeBuilder().client(true).clusterName(name).node().client();
		} else {
			Settings.Builder settings = Settings.settingsBuilder();
			settings.put("cluster.name", name);

			config.keySet().stream()
					.filter(key -> key.startsWith("elastic:"))
					.forEach(key -> settings.put(key.replaceFirst("^elastic:", ""), config.getString(key)));

			List<String> hosts = config.getList("elastic.hosts", Splitter.on(' '));
			int port = config.getInt("elastic.port", 9300);
			TransportClient client = TransportClient.builder().settings(settings).build();
			for (String host : HostUtils.expand(hosts)) {
				log.debug("adding host: {}", host);
				InetAddress address = Unchecked.get(() -> InetAddress.getByName(host));
				client.addTransportAddress(new InetSocketTransportAddress(address, port));
			}
			return client;
		}
	}
}

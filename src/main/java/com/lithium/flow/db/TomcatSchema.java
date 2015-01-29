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

package com.lithium.flow.db;

import static com.google.common.base.Preconditions.checkNotNull;

import com.lithium.flow.config.Config;
import com.lithium.flow.util.Logs;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.Nonnull;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.slf4j.Logger;

/**
 * @author Matt Ayres
 */
public class TomcatSchema extends AbstractSchema {
	private static final Logger log = Logs.getLogger();

	private final DataSource dataSource;

	public TomcatSchema(@Nonnull Config config) {
		checkNotNull(config);

		log.debug("{} sql.url: {}", config.getName(), config.getString("sql.url"));

		PoolProperties poolProps = new PoolProperties();
		poolProps.setUrl(config.getString("sql.url"));
		poolProps.setUsername(config.getString("sql.login"));
		poolProps.setPassword(config.getString("sql.password"));
		poolProps.setDriverClassName(config.getString("sql.driver"));
		poolProps.setMinIdle(config.getInt("sql.pool.minIdle", 1));
		poolProps.setMaxIdle(config.getInt("sql.pool.maxIdle", 3));
		poolProps.setMaxActive(config.getInt("sql.pool.maxActive", 5));
		poolProps.setInitialSize(poolProps.getMinIdle());
		poolProps.setValidationQuery("SELECT 1");
		poolProps.setValidationQueryTimeout((int) config.getTime("sql.pool.validationQueryTimeout", "5s"));
		poolProps.setTestOnBorrow(config.getBoolean("sql.pool.testOnBorrow", true));
		poolProps.setDefaultReadOnly(config.getBoolean("sql.pool.defaultReadOnly", true));
		dataSource = new DataSource(poolProps);
	}

	@Override
	@Nonnull
	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	@Override
	public void close() throws IOException {
		dataSource.close();
	}
}

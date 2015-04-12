/*
 * JedisPooler.java
 * Created on Apr 8, 2015
 *
 * Copyright 2015 Lithium Technologies, Inc. 
 * San Francisco, California, U.S.A.  All Rights Reserved.
 *
 * This software is the  confidential and proprietary information
 * of  Lithium  Technologies,  Inc.  ("Confidential Information")
 * You shall not disclose such Confidential Information and shall 
 * use  it  only in  accordance  with  the terms of  the  license 
 * agreement you entered into with Lithium.
 */

package com.lithium.flow.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nonnull;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * @author Matt Ayres
 */
public class JedisPooler extends JedisPool {
	public JedisPooler(GenericObjectPoolConfig poolConfig, String host, int port, int timeout) {
		super(poolConfig, host, port, timeout);
	}

	public <T> T apply(@Nonnull Function<Jedis, T> function) {
		checkNotNull(function);
		try (Jedis jedis = getResource()) {
			return function.apply(jedis);
		}
	}

	public void accept(@Nonnull Consumer<Jedis> consumer) {
		checkNotNull(consumer);
		try (Jedis jedis = getResource()) {
			consumer.accept(jedis);
		}
	}
}

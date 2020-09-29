/*
 * Copyright 2020 Jeremy KUHN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.winterframework.mod.configuration.app;

import java.util.List;

import io.lettuce.core.RedisClient;
import io.winterframework.mod.configuration.converter.StringValueConverter;
import io.winterframework.mod.configuration.source.RedisConfigurationSource;
import io.winterframework.mod.configuration.source.RedisConfigurationSource.RedisConfigurationQueryResult;

/**
 * @author jkuhn
 *
 */
public class ConfigurationApp {

	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "DEBUG");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		RedisConfigurationSource source = new RedisConfigurationSource(new StringValueConverter(), RedisClient.create("redis://localhost:6379"));
		
		long t0 = System.nanoTime();
		List<RedisConfigurationQueryResult> results = source
			.get("test").and()
			.get("test").withParameters("env", "prod").and()
			.get("test").withParameters("env", "prod", "node", "n1").and()
			.get("test").withParameters("env", "prod", "node", "n2")
			.execute()
			.collectList()
			.block();
		System.out.println(System.nanoTime() - t0);
		
		System.out.println(results.size());
	}
}

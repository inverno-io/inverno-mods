/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.configuration.internal;

import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.DefaultingStrategy;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class LookupDefaultingStrategyTest {
	
	@Test
	public void testGetDefaultingKeys() {
		DefaultingStrategy strategy = DefaultingStrategy.lookup();
		
		ConfigurationKey key = ConfigurationKey.of("name", "k1", "v1", "k2", "v2", "k3", "v3");
		
		List<ConfigurationKey> defaultingKeys = strategy.getDefaultingKeys(key);
		
		Assertions.assertEquals(4, defaultingKeys.size());
		
		Assertions.assertEquals(key, defaultingKeys.get(0));
		Assertions.assertEquals(ConfigurationKey.of("name", "k1", "v1", "k2", "v2"), defaultingKeys.get(1));
		Assertions.assertEquals(ConfigurationKey.of("name", "k1", "v1"), defaultingKeys.get(2));
		Assertions.assertEquals(ConfigurationKey.of("name"), defaultingKeys.get(3));
		
	}
	
	@Test
	public void testGetListDefaultingKeys() {
		DefaultingStrategy strategy = DefaultingStrategy.lookup();
		
		ConfigurationKey key = ConfigurationKey.of("name", "k1", "v1", "k2", "v2", "k3", "v3");
		
		List<ConfigurationKey> defaultingKeys = strategy.getListDefaultingKeys(key);
		
		Assertions.assertEquals(4, defaultingKeys.size());
		
		Assertions.assertEquals(key, defaultingKeys.get(0));
		Assertions.assertEquals(ConfigurationKey.of("name", ConfigurationKey.Parameter.of("k1", "v1"), ConfigurationKey.Parameter.of("k2", "v2"), ConfigurationKey.Parameter.undefined("k3")), defaultingKeys.get(1));
		Assertions.assertEquals(ConfigurationKey.of("name", ConfigurationKey.Parameter.of("k1", "v1"), ConfigurationKey.Parameter.undefined("k2"), ConfigurationKey.Parameter.undefined("k3")), defaultingKeys.get(2));
		Assertions.assertEquals(ConfigurationKey.of("name", ConfigurationKey.Parameter.undefined("k1"), ConfigurationKey.Parameter.undefined("k2"), ConfigurationKey.Parameter.undefined("k3")), defaultingKeys.get(3));
	}
}

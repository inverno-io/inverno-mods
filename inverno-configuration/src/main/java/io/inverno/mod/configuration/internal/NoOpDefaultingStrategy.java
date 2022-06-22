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

/**
 * <p>
 * A {@link DefaultingStrategy} that does not support any defaulting and simply returns the original query for the source to query.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class NoOpDefaultingStrategy implements DefaultingStrategy {

	/**
	 * The NoOp defaulting strategy singleton.
	 */
	public static final NoOpDefaultingStrategy INSTANCE = new NoOpDefaultingStrategy();
	
	@Override
	public List<ConfigurationKey> getDefaultingKeys(ConfigurationKey queryKey) {
		return List.of(queryKey);
	}

	@Override
	public List<ConfigurationKey> getListDefaultingKeys(ConfigurationKey queryKey) {
		return List.of(queryKey);
	}
}

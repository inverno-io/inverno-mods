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
import io.inverno.mod.configuration.source.CompositeConfigurationStrategy;

/**
 * <p>
 * A NoOp composite configuration strategy used to query sources in a composite configuration source with a NoOp defaulting strategy.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see NoOpDefaultingStrategy
 */
public class NoOpCompositeConfigurationStrategy extends AbstractCompositeConfigurationStrategy {

	/**
	 * <p>
	 * Creates a NoOp composite configuration strategy.
	 * </p>
	 * 
	 * @param ignoreFailure true to ignore all failure, false otherwise 
	 */
	public NoOpCompositeConfigurationStrategy(boolean ignoreFailure) {
		super(ignoreFailure);
	}
	
	@Override
	public CompositeDefaultingStrategy createDefaultingStrategy() {
		return NoOpCompositeDefaultingStrategy.INSTANCE;
	}
	
	/**
	 * <p>
	 * A NoOp defaulting strategy that does not support any defaulting.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	private static class NoOpCompositeDefaultingStrategy extends NoOpDefaultingStrategy implements CompositeConfigurationStrategy.CompositeDefaultingStrategy {

		/**
		 * The NoOp defaulting strategy singleton.
		 */
		public static final NoOpCompositeDefaultingStrategy INSTANCE = new NoOpCompositeDefaultingStrategy();
		
		@Override
		public void putResult(ConfigurationKey queryKey, ConfigurationKey resultKey) {
			// NoOp
		}
	}
}

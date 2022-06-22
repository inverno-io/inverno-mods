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
package io.inverno.mod.configuration.source;

import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.ConfigurationSourceException;
import io.inverno.mod.configuration.DefaultingStrategy;
import io.inverno.mod.configuration.internal.LookupCompositeConfigurationStrategy;
import io.inverno.mod.configuration.internal.NoOpCompositeConfigurationStrategy;

/**
 * <p>
 * A composite configuration strategy allows to specifies the behaviour of a {@link CompositeConfigurationStrategy}.
 * </p>
 * 
 * <p>
 * It basically specifies how the composite source determines whether a result supersedes the result of a previous round (i.e. from a higher priority source), when a result is considered as
 * resolved and what to do in case a source returns an error result.
 * </p>
 * 
 * <p>
 * It also provides a contextual defaulting strategy to use on the underlying sources. The {@link CompositeDefaultingStrategy} keeps track of the results of previous rounds in order to optimize the
 * defaulting queries to execute on the subsequent sources.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public interface CompositeConfigurationStrategy {
	
	/**
	 * <p>
	 * Returns a NoOp composite configuration strategy that ignores failures.
	 * </p>
	 * 
	 * <p>
	 * The NoOp defaulting strategy does not support defaulting.
	 * </p>
	 * 
	 * @return a NoOp composite configuration strategy
	 */
	static CompositeConfigurationStrategy noOp() {
		return new NoOpCompositeConfigurationStrategy(true);
	}
	
	/**
	 * <p>
	 * Returns a NoOp composite configuration strategy.
	 * </p>
	 * 
	 * <p>
	 * The NoOp strategy does not support defaulting.
	 * </p>
	 * 
	 * @param ignoreFailure true to ignore all failure, false otherwise 
	 * 
	 * @return a NoOp composite configuration strategy
	 */
	static CompositeConfigurationStrategy noOp(boolean ignoreFailure) {
		return new NoOpCompositeConfigurationStrategy(ignoreFailure);
	}
	
	/**
	 * <p>
	 * Returns a lookup composite configuration strategy that ignores failures.
	 * </p>
	 * 
	 * <p>
	 * The lookup strategy supports defaulting by prioritizing query parameters from left to right (see {@link DefaultingStrategy#lookup()}.
	 * </p>
	 * 
	 * @return a lookup composite configuration strategy
	 */
	static CompositeConfigurationStrategy lookup() {
		return new LookupCompositeConfigurationStrategy(true);
	}
	
	/**
	 * <p>
	 * Returns a lookup composite configuration strategy that ignores failures.
	 * </p>
	 * 
	 * <p>
	 * The lookup strategy supports defaulting by prioritizing query parameters from left to right (see {@link DefaultingStrategy#lookup()}.
	 * </p>
	 * 
	 * @param ignoreFailure true to ignore all failure, false otherwise 
	 * 
	 * @return a lookup composite configuration strategy
	 */
	static CompositeConfigurationStrategy lookup(boolean ignoreFailure) {
		return new LookupCompositeConfigurationStrategy(ignoreFailure);
	}
	
	/**
	 * <p>
	 * Indicates whether or not the specified configuration source error should be ignored when resolving a result.
	 * </p>
	 *
	 * @param error a configuration source error
	 *
	 * @return true to ignore the error, false otherwise
	 */
	boolean ignoreFailure(ConfigurationSourceException error);
	
	/**
	 * <p>
	 * Determines whether the specified result key supersedes the previous result key retained from previous sources for the specified original query key.
	 * </p>
	 *
	 * @param queryKey    the configuration key representing the original query
	 * @param previousKey the configuration key of the previous result
	 * @param resultKey   the configuration key of the current result
	 *
	 * @return true if the result key superseds the previous result key, false otherwise
	 */
	boolean isSuperseded(ConfigurationKey queryKey, ConfigurationKey previousKey, ConfigurationKey resultKey);

	/**
	 * <p>
	 * Determines whether the specified result key resolves the specified original query key (ie. there can't be any better result).
	 * </p>
	 *
	 * @param queryKey  the configuration key representing the original query
	 * @param resultKey the result to test
	 *
	 * @return true if the result resolves the query, false otherwise
	 */
	boolean isResolved(ConfigurationKey queryKey, ConfigurationKey resultKey);
	
	/**
	 * <p>
	 * Returns the defaulting strategy that must be applied to sources before executing queries.
	 * </p>
	 * 
	 * @return a new composite defaulting strategy
	 */
	CompositeDefaultingStrategy createDefaultingStrategy();
	
	/**
	 * <p>
	 * A defaulting strategy that can keep track of query results in order to optimize defaulting queries for subsequent rounds.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	interface CompositeDefaultingStrategy extends DefaultingStrategy {
		
		/**
		 * <p>
		 * Records the specified result key for the specified original query key.
		 * </p>
		 * 
		 * @param queryKey the original query key
		 * @param resultKey the result key
		 */
		void putResult(ConfigurationKey queryKey, ConfigurationKey resultKey);
	}
}

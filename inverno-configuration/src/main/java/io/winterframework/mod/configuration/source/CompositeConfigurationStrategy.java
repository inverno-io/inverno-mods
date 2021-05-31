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
package io.inverno.mod.configuration.source;

import io.inverno.mod.configuration.ConfigurationProperty;
import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.ConfigurationQuery;
import io.inverno.mod.configuration.ConfigurationSourceException;
import io.inverno.mod.configuration.ExecutableConfigurationQuery;
import io.inverno.mod.configuration.source.CompositeConfigurationSource.CompositeConfigurationQuery;

/**
 * <p>
 * A composite configuration strategy specifies what queries must be executed on
 * the sources of a composite configuration source for an original query and
 * what results should eventually be retained.
 * </p>
 * 
 * <p>
 * A composite configuration source uses a strategy as followed:
 * </p>
 * 
 * <ol>
 * <li>for an original query, the source query which will actually be executed
 * on a source is populated by invoking
 * {@link CompositeConfigurationStrategy#populateSourceQuery(ConfigurationKey, ConfigurationQuery, ConfigurationProperty)}
 * method, this can result in multiple queries</li>
 * <li>the composite source executes the populated source query and retains the
 * first non-empty result that supersedes current best result using the
 * {@link CompositeConfigurationStrategy#isSuperseded(ConfigurationKey, ConfigurationProperty, ConfigurationProperty)}
 * method</li>
 * <li>the composite source then determined whether the result resolves the
 * query (ie. there can't be any better result) using the
 * {@link CompositeConfigurationStrategy#isResolved(ConfigurationKey, ConfigurationProperty)}
 * method, if so, the composite source finally retains the result and does not
 * query remaining sources for the original query.</li>
 * </ol>
 * 
 * <p>
 * In case of error when querying a source, the composite source can choose to
 * ignore that error, it then considers that this particular source didn't
 * returned any results or it can retain the faulty result which might still be
 * superseded by next sources.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see CompositeConfigurationQuery
 * @see DefaultCompositeConfigurationStrategy
 */
public interface CompositeConfigurationStrategy {

	/**
	 * <p>
	 * Indicates whether or not the specified configuration source error should be
	 * ignored when resolving a result.
	 * </p>
	 * 
	 * @param error a configuration source error
	 * 
	 * @return true to ignore the error, false otherwise
	 */
	boolean ignoreFailure(ConfigurationSourceException error);
	
	/**
	 * <p>
	 * Determines whether the specified result supersedes the previous result
	 * retained from previous sources for the specified original query.
	 * </p>
	 * 
	 * @param queryKey       the configuration key representing the original query
	 * @param previousResult the previous result
	 * @param result         the result to test
	 * 
	 * @return true if the result superseds the previous result, false otherwise
	 */
	boolean isSuperseded(ConfigurationKey queryKey, ConfigurationProperty<?,?> previousResult, ConfigurationProperty<?,?> result);

	/**
	 * <p>
	 * Determines whether the specified result resolves the specified original query
	 * (ie. there can't be any better result).
	 * </p>
	 * 
	 * @param queryKey the configuration key representing the original query
	 * @param result   the result to test
	 * 
	 * @return true if the result resolves the query, false otherwise
	 */
	boolean isResolved(ConfigurationKey queryKey, ConfigurationProperty<?,?> result);

	/**
	 * <p>
	 * Populates the source query created by a composite source to query one of its
	 * sources to retrieves values for the specified original query.
	 * </p>
	 * 
	 * <p>
	 * This method takes the previous result retained from previous sources into
	 * account to filter out queries that can't possibly supersedes it.
	 * </p>
	 * 
	 * @param queryKey       the configuration key representing the original query
	 * @param sourceQuery    the source query
	 * @param previousResult the previous result
	 * 
	 * @return a populated executable configuration query
	 */
	ExecutableConfigurationQuery<?, ? ,?> populateSourceQuery(ConfigurationKey queryKey, ConfigurationQuery<?, ?, ?> sourceQuery, ConfigurationProperty<?,?> previousResult);
}

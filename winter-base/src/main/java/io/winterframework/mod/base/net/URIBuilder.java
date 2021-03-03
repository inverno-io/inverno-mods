/*
 * Copyright 2021 Jeremy KUHN
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
package io.winterframework.mod.base.net;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * @author jkuhn
 *
 */
public interface URIBuilder {
	
	URIBuilder scheme(String scheme);
	
	URIBuilder userInfo(String userInfo);
	
	URIBuilder host(String host);
	
	URIBuilder port(Integer port);
	
	URIBuilder port(String port);
	
	default URIBuilder path(String path) {
		return this.path(path, true);
	}
	
	URIBuilder path(String path, boolean ignoreTrailingSlash);
	
	URIBuilder segment(String segment);
	
	URIBuilder clearPath();
	
	URIBuilder queryParameter(String name, String value);
	
	URIBuilder clearQuery();
	
	URIBuilder fragment(String fragment);
	
	List<String> getParameterNames();
	
	List<String> getPathParameterNames();
	
	Map<String, List<String>> getQueryParameters(Object... values) throws URIBuilderException;
	
	Map<String, List<String>> getQueryParameters(Map<String, ?> values) throws URIBuilderException;
	
	Map<String, List<String>> getRawQueryParameters() throws URIBuilderException;
	
	URI build(Object... values) throws URIBuilderException;
	
	URI build(Object[] values, boolean escapeSlash) throws URIBuilderException;
	
	URI build(Map<String, ?> values) throws URIBuilderException;
	
	URI build(Map<String, ?> values, boolean escapeSlash) throws URIBuilderException;
	
	String buildString(Object... values) throws URIBuilderException;
	
	String buildString(Object[] values, boolean escapeSlash) throws URIBuilderException;
	
	String buildString(Map<String, ?> values) throws URIBuilderException;
	
	String buildString(Map<String, ?> values, boolean escapeSlash) throws URIBuilderException;
	
	String buildRawString() throws URIBuilderException;

	String buildPath(Object... values);
	
	String buildPath(Object[] values, boolean escapeSlash);
	
	String buildPath(Map<String, ?> values);

	String buildPath(Map<String, ?> values, boolean escapeSlash);
	
	String buildRawPath() throws URIBuilderException;
	
	String buildQuery(Object... values) throws URIBuilderException;
	
	String buildQuery(Map<String, ?> values) throws URIBuilderException;
	
	String buildRawQuery() throws URIBuilderException;
	
	// In order to have a consistent implementation we'll return a regexp that
	// matches when a param {param1} is either a regular segment, '.' or '..'
	// followed by a regula segment, '.' or '..':
	// eg: /a/b/{param1}/../d
	// - /a/b/d could be a match, when param1 = 'abc'
	// - /d could be a match when param1 = '..'
	// - /a/d could be a match when param1 = '.'
	// when building a pattern we should ideally consider these use cases but such
	// construct is a bit unusual if what you want in the end is a URI pattern so
	// the best most logical, consistent and simple approach would be to treat
	// parameterized segment as regular segment (ie. neither '.' nor '..') and
	// remove them from the builder if they are followed by '..'
	default URIPattern buildPattern() {
		return this.buildPattern(false);
	}
	
	URIPattern buildPattern(boolean matchTrailingSlash);
	
	default URIPattern buildPathPattern() {
		return this.buildPathPattern(false);
	}
	
	URIPattern buildPathPattern(boolean matchTrailingSlash);
}

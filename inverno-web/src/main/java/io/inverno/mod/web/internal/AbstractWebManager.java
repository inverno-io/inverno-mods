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
package io.inverno.mod.web.internal;

import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.base.net.URIPattern;
import io.inverno.mod.base.net.URIs;
import io.inverno.mod.http.base.Method;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * Base Web manager class.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 * 
 * @param <A> the type of Web manager
 */
class AbstractWebManager<A extends AbstractWebManager<A>> {

	protected Set<String> paths;
	protected Set<URIPattern> pathPatterns;

	protected Set<Method> methods;

	protected Set<String> consumes;

	protected Set<String> produces;

	protected Set<String> languages;

	@SuppressWarnings("unchecked")
	public A path(String path, boolean matchTrailingSlash) throws IllegalArgumentException {
		Objects.requireNonNull(path);
		if (!path.startsWith("/")) {
			throw new IllegalArgumentException("Path must be absolute");
		}

		URIBuilder pathBuilder = URIs.uri(path, URIs.RequestTargetForm.ABSOLUTE, false, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);

		String rawPath = pathBuilder.buildRawPath();
		List<String> pathParameterNames = pathBuilder.getParameterNames();
		if (pathParameterNames.isEmpty()) {
			// Static path
			if (this.paths == null) {
				this.paths = new HashSet<>();
			}
			this.paths.add(rawPath);
			if (matchTrailingSlash) {
				if (rawPath.endsWith("/")) {
					this.paths.add(rawPath.substring(0, rawPath.length() - 1));
				} else {
					this.paths.add(rawPath + "/");
				}
			}
		} else {
			// PathPattern
			if (this.pathPatterns == null) {
				this.pathPatterns = new HashSet<>();
			}
			this.pathPatterns.add(pathBuilder.buildPathPattern(matchTrailingSlash));
		}
		return (A)this;
	}

	@SuppressWarnings("unchecked")
	public A method(Method method) {
		Objects.requireNonNull(method);
		if (this.methods == null) {
			this.methods = new HashSet<>();
		}
		this.methods.add(method);
		return (A)this;
	}

	@SuppressWarnings("unchecked")
	public A consumes(String mediaRange) {
		Objects.requireNonNull(mediaRange);
		if (this.consumes == null) {
			this.consumes = new LinkedHashSet<>();
		}
		this.consumes.add(mediaRange);
		return (A)this;
	}

	@SuppressWarnings("unchecked")
	public A produces(String mediaType) {
		Objects.requireNonNull(mediaType);
		if (this.produces == null) {
			this.produces = new LinkedHashSet<>();
		}
		this.produces.add(mediaType);
		return (A)this;
	}

	@SuppressWarnings("unchecked")
	public A language(String language) {
		Objects.requireNonNull(language);
		if (this.languages == null) {
			this.languages = new LinkedHashSet<>();
		}
		this.languages.add(language);
		return (A)this;
	}
}
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
package io.winterframework.mod.web.lab.router.base;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import io.winterframework.mod.web.lab.router.PathPattern;

/**
 * @author jkuhn
 *
 */
public class GenericPathPattern implements PathPattern {

	private Pattern pathPattern;
	
	private List<String> pathParameterNames;
	
	public GenericPathPattern(Pattern pathPattern, List<String> pathParameterNames) {
		Objects.requireNonNull(pathPattern);
		Objects.requireNonNull(pathParameterNames);
		this.pathPattern = pathPattern;
		this.pathParameterNames = pathParameterNames;
	}

	@Override
	public Pattern getPattern() {
		return this.pathPattern;
	}

	@Override
	public List<String> getPathParameterNames() {
		return this.pathParameterNames;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((pathPattern == null) ? 0 : pathPattern.pattern().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GenericPathPattern other = (GenericPathPattern) obj;
		if (pathPattern == null) {
			if (other.pathPattern != null)
				return false;
		} else if (!pathPattern.pattern().equals(other.pathPattern.pattern()))
			return false;
		return true;
	}
	
	
}

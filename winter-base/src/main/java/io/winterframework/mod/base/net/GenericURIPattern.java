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

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author jkuhn
 *
 */
class GenericURIPattern implements URIPattern {

	private final String rawValue;
	private final String regex;
	private final List<String> groupNames;
	
	private Pattern pattern;
	
	public GenericURIPattern(String rawValue, String regex, List<String> groupNames) {
		this.rawValue = rawValue;
		this.regex = regex;
		this.groupNames = groupNames != null ? Collections.unmodifiableList(groupNames) : List.of();
	}

	@Override
	public Pattern getPattern() {
		if(this.pattern == null) {
			this.pattern = Pattern.compile(this.regex);
		}
		return this.pattern;
	}

	@Override
	public String getPatternString() {
		return this.regex;
	}

	@Override
	public URIMatcher matcher(String uri) {
		return new GenericURIMatcher(this.getPattern().matcher(uri), this.groupNames);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((groupNames == null) ? 0 : groupNames.hashCode());
		result = prime * result + ((regex == null) ? 0 : regex.hashCode());
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
		GenericURIPattern other = (GenericURIPattern) obj;
		if (groupNames == null) {
			if (other.groupNames != null)
				return false;
		} else if (!groupNames.equals(other.groupNames))
			return false;
		if (regex == null) {
			if (other.regex != null)
				return false;
		} else if (!regex.equals(other.regex))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return this.rawValue;
	}
}

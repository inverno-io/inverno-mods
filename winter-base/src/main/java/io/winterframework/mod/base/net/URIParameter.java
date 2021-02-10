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

import java.nio.charset.Charset;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

/**
 * @author jkuhn
 *
 */
// {<name>[:<pattern>]}
class URIParameter {

	private static final String DEFAULT_PATTERN = "[^/]*";
	
	private final int offset;
	
	private final int length;
	
	private final Predicate<Integer> allowedCharacters;
	
	private final Charset charset;
	
	private final String name;
	
	private final String pattern;
	
	public URIParameter(int offset, int length, String name, Charset charset) {
		this(offset, length, name, URIParameter.DEFAULT_PATTERN, null, charset);
	}
	
	public URIParameter(int offset, int length, String name, Predicate<Integer> allowedCharacters, Charset charset) {
		this(offset, length, name, URIParameter.DEFAULT_PATTERN, allowedCharacters, charset);
	}
	
	public URIParameter(int offset, int length, String name, String pattern, Charset charset) {
		this(offset, length, name, pattern, null, charset);
	}
	
	public URIParameter(int offset, int length, String name, String pattern, Predicate<Integer> allowedCharacters, Charset charset) {
		this.offset = offset;
		this.length = length;
		this.allowedCharacters = allowedCharacters;
		this.charset = charset;
		this.name = StringUtils.isNotBlank(name) ? name : null;
		this.pattern = pattern;
	}
	
	public int getOffset() {
		return offset;
	}
	
	public int getLength() {
		return length;
	}
	
	public String getName() {
		return name;
	}
	
	public String getPattern() {
		StringBuilder parameterPattern = new StringBuilder("(");
		if(this.name != null) {
			parameterPattern.append("?<").append(this.name).append(">");
		}
		parameterPattern.append(this.pattern).append(")");
		return parameterPattern.toString();
	}
	
	public String checkValue(String value) {
		if(!URIs.checkURIComponent(value, this.allowedCharacters, this.charset).matches(this.pattern)) {
			throw new URIBuilderException("Value for parameter " + this.name + " does not match expected pattern " + this.pattern);
		}
		return value;
	}
}

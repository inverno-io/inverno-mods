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
package io.inverno.mod.base.net;

import java.nio.charset.Charset;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * Represents a parameter in a URI component specified in the component's raw value in the form <code>{{@literal <name>[:<pattern>]}}</code>.
 * </p>
 *
 * <p>
 * A URI parameter has a name and a pattern which default to <code>{@literal [^/]*}</code>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see URIBuilder
 */
class URIParameter implements URIComponentPart {

	public static final String WILDCARD_PATTERN = "[^/]*";
	
	public static final String QUESTION_MARK_PATTERN = "[^/]";
	
	private final int offset;
	
	private final int length;
	
	private final Predicate<Integer> allowedCharacters;
	
	private final Charset charset;
	
	private final String name;
	
	private final String pattern;
	
	private final boolean custom;
	private final boolean wildcard;
	private final boolean questionMark;
	
	/**
	 * <p>
	 * Creates the URI parameter defined at the specified offset and of the specified length within a component's raw value and with the specified name and charset and default pattern.
	 * </p>
	 *
	 * @param offset  the offset within a component's raw value
	 * @param length  the length of the parameter within a component's raw value
	 * @param name    the parameter name
	 * @param charset a charset
	 */
	public URIParameter(int offset, int length, String name, Charset charset) {
		this(offset, length, name, URIParameter.WILDCARD_PATTERN, null, charset);
	}
	
	/**
	 * <p>
	 * Creates the URI parameter defined at the specified offset and of the specified length within a component's raw value and with the specified name, allowed characters predicate and charset and
	 * default pattern.
	 * </p>
	 *
	 * @param offset            the offset within a component's raw value
	 * @param length            the length of the parameter within a component's raw value
	 * @param name              the parameter name
	 * @param allowedCharacters the allowed characters predicate
	 * @param charset           a charset
	 */
	public URIParameter(int offset, int length, String name, Predicate<Integer> allowedCharacters, Charset charset) {
		this(offset, length, name, URIParameter.WILDCARD_PATTERN, allowedCharacters, charset);
	}
	
	/**
	 * <p>
	 * Creates the URI parameter defined at the specified offset and of the specified length within a component's raw value and with the specified name, pattern and charset.
	 * </p>
	 *
	 * @param offset  the offset within a component's raw value
	 * @param length  the length of the parameter within a component's raw value
	 * @param name    the parameter name
	 * @param pattern the parameter pattern
	 * @param charset a charset
	 */
	public URIParameter(int offset, int length, String name, String pattern, Charset charset) {
		this(offset, length, name, pattern, null, charset);
	}

	/**
	 * <p>
	 * Creates the URI parameter defined at the specified offset and of the specified length within a component's raw value and with the specified name, pattern, allowed characters predicate and
	 * charset.
	 * </p>
	 *
	 * @param offset            the offset within a component's raw value
	 * @param length            the length of the parameter within a component's raw value
	 * @param name              the parameter name
	 * @param pattern           the parameter pattern
	 * @param allowedCharacters the allowed characters predicate
	 * @param charset           a charset
	 */
	public URIParameter(int offset, int length, String name, String pattern, Predicate<Integer> allowedCharacters, Charset charset) {
		this.offset = offset;
		this.length = length;
		this.allowedCharacters = allowedCharacters;
		this.charset = charset;
		this.name = StringUtils.isNotBlank(name) ? name : null;
		this.pattern = pattern;
		this.wildcard = this.pattern.equals(WILDCARD_PATTERN);
		this.questionMark = !this.wildcard && this.pattern.equals(QUESTION_MARK_PATTERN);
		this.custom = !this.wildcard && !this.questionMark;
	}

	/**
	 * <p>
	 * Returns the position of the parameter within a component's raw value.
	 * </p>
	 *
	 * @return the parameter position
	 */
	public int getOffset() {
		return offset;
	}
	
	/**
	 * <p>
	 * Returns the length of the parameter within a component's raw value.
	 * </p>
	 *
	 * @return the parameter length
	 */
	public int getLength() {
		return length;
	}
	
	/**
	 * <p>
	 * Returns the name of the parameter.
	 * </p>
	 *
	 * @return the parameter name
	 */
	public String getName() {
		return name;
	}

	/**
	 * <p>
	 * Returns the pattern of the parameter.
	 * </p>
	 */
	@Override
	public String getValue() {
		return this.pattern;
	}
	
	/**
	 * <p>
	 * Returns the pattern of the parameter.
	 * </p>
	 *
	 * @return the parameter pattern
	 */
	public String getPattern() {
		StringBuilder parameterPattern = new StringBuilder("(");
		if(this.name != null) {
			parameterPattern.append("?<").append(this.name).append(">");
		}
		parameterPattern.append(this.pattern).append(")");
		return parameterPattern.toString();
	}

	@Override
	public boolean isStatic() {
		return false;
	}
	
	/**
	 * <p>
	 * Returns true if the parameter represents a custom pattern.
	 * <p>
	 * 
	 * <p>
	 * A custom pattern is anything other than the {@link #WILDCARD_PATTERN} or {@link #QUESTION_MARK_PATTERN} pattern.
	 * </p>
	 * 
	 * @return true if this is a custom pattern parameter, false otherwise
	 */
	@Override
	public boolean isCustom() {
		return custom;
	}
	
	/**
	 * <p>
	 * Returns true if the parameter represents a {@link #WILDCARD_PATTERN}.
	 * <p>
	 * 
	 * @return true if this is a wildcard pattern parameter, false otherwise
	 */
	@Override
	public boolean isWildcard() {
		return wildcard;
	}
	
	/**
	 * <p>
	 * Returns true if the parameter represents a {@link #QUESTION_MARK_PATTERN}.
	 * <p>
	 * 
	 * @return true if this is a question mark pattern parameter, false otherwise
	 */
	@Override
	public boolean isQuestionMark() {
		return questionMark;
	}
	
	/**
	 * <p>
	 * Returns the unnamed non-capturing pattern of the parameter.
	 * </p>
	 *
	 * @return the parameter pattern
	 */
	public String getUnnamedNonCapturingPattern() {
		StringBuilder parameterPattern = new StringBuilder("(?:");
		parameterPattern.append(this.pattern).append(")");
		return parameterPattern.toString();
	}
	
	/**
	 * <p>
	 * Checks that the specified value matches the parameter pattern and is valid according to the allowed characters of the URI component that defines it.
	 * </p>
	 *
	 * @param value The value to check
	 *
	 * @return the value if it is valid
	 */
	public String checkValue(String value) {
		if(!URIs.checkURIComponent(value, this.allowedCharacters, this.charset).matches(this.pattern)) {
			throw new URIBuilderException("Value for parameter " + (this.name != null ? this.name : "UNNAMED") + " does not match expected pattern " + this.pattern);
		}
		return value;
	}
}

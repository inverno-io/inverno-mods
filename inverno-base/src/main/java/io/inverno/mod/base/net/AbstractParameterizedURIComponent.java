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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * Base class for {@link ParameterizedURIComponent} implementations.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ParameterizedURIComponent
 */
abstract class AbstractParameterizedURIComponent implements ParameterizedURIComponent {
	
	protected final URIFlags flags;
	protected final String rawValue;
	
	protected final Predicate<Integer> escapedCharacters;
	protected final Predicate<Integer> allowedCharacters;
	
	protected final Charset charset;
	
	protected final List<URIParameter> parameters;
	
	private String pattern;
	private List<String> patternGroupNames;
	
	/**
	 * <p>
	 * Creates a parameterized URI component with the specified flags, charset and raw value.
	 * </p>
	 * 
	 * @param flags    URI flags
	 * @param charset  a charset
	 * @param rawValue a raw value
	 */
	public AbstractParameterizedURIComponent(URIFlags flags, Charset charset, String rawValue) {
		this(flags, charset, rawValue, null, null);
	}

	/**
	 * <p>
	 * Creates a parameterized URI component with the specified flags, charset, raw value and escaped characters predicate.
	 * </p>
	 * 
	 * @param flags             URI flags
	 * @param charset           a charset
	 * @param rawValue          a raw value
	 * @param escapedCharacters an escaped character predicate
	 */
	public AbstractParameterizedURIComponent(URIFlags flags, Charset charset, String rawValue, Predicate<Integer> escapedCharacters) {
		this(flags, charset, rawValue, escapedCharacters, null);
	}
	
	/**
	 * <p>
	 * Creates a parameterized URI component with the specified flags, charset, raw value, escaped characters predicate and allowed characters predicate.
	 * </p>
	 * 
	 * @param flags             URI flags
	 * @param charset           a charset
	 * @param rawValue          a raw value
	 * @param escapedCharacters an escaped character predicate
	 * @param allowedCharacters an allowed character predicate
	 */
	public AbstractParameterizedURIComponent(URIFlags flags, Charset charset, String rawValue, Predicate<Integer> escapedCharacters, Predicate<Integer> allowedCharacters) {
		this.flags = flags;
		this.charset = charset;
		this.rawValue = rawValue;
		this.escapedCharacters = escapedCharacters;
		this.allowedCharacters = allowedCharacters;
		this.parameters = new LinkedList<>();
		if(this.flags.isParameterized()) {
			URIs.scanURIComponent(this.rawValue, this.allowedCharacters, this.charset, this.parameters::add, null);
		}
	}

	@Override
	public String getRawValue() {
		return this.rawValue;
	}
	
	@Override
	public String getValue() {
		return this.getValue(List.of());
	}
	
	@Override
	public String getPattern() {
		if(this.pattern == null) {
			if(this.rawValue == null) {
				this.patternGroupNames = List.of();
				this.pattern = "";
			}
			else {
				this.patternGroupNames = new LinkedList<>();
				StringBuilder patternBuilder = new StringBuilder();
				int valueIndex = 0;
				for(URIParameter parameter : this.parameters) {
					if(parameter.getOffset() > valueIndex) {
						patternBuilder.append("(").append(Pattern.quote(this.rawValue.substring(valueIndex, parameter.getOffset()))).append(")");
						this.patternGroupNames.add(null);
					}
					patternBuilder.append(parameter.getPattern());
					this.patternGroupNames.add(parameter.getName());
					valueIndex = parameter.getOffset() + parameter.getLength();
				}
				if(valueIndex < this.rawValue.length()) {
					patternBuilder.append("(").append(Pattern.quote(this.rawValue.substring(valueIndex))).append(")");
					this.patternGroupNames.add(null);
				}
				this.pattern = patternBuilder.toString();
			}
		}
		return this.pattern;
	}
	
	@Override
	public List<String> getPatternGroupNames() {
		if(this.patternGroupNames == null) {
			this.getPattern();
		}
		return this.patternGroupNames;
	}
	
	@Override
	public boolean isPresent() {
		return this.rawValue != null;
	}
	
	@Override
	public List<URIParameter> getParameters() {
		return Collections.unmodifiableList(this.parameters);
	}
	
	@Override
	public String getValue(List<Object> values) {
		if(this.rawValue == null) {
			return "";
		}
		if(this.parameters.isEmpty()) {
			return URIs.encodeURIComponent(this.rawValue, this.escapedCharacters, this.charset);
		}
		if(values.size() != this.parameters.size()) {
			throw new IllegalArgumentException("Missing values to generate component: " + this.parameters.stream().map(URIParameter::getName).skip(values.size()).collect(Collectors.joining(", ")));
		}
		
		StringBuilder result = new StringBuilder();
		int valueIndex = 0;
		for(int i = 0;i<this.parameters.size();i++) {
			URIParameter parameter = this.parameters.get(i);
			String parameterValue = parameter.checkValue(values.get(i).toString());
			if(parameter.getOffset() > valueIndex) {
				result.append(URIs.encodeURIComponent(this.rawValue.substring(valueIndex, parameter.getOffset()), this.escapedCharacters, this.charset));
			}
			result.append(URIs.encodeURIComponent(parameterValue, this.escapedCharacters, this.charset));
			valueIndex = parameter.getOffset() + parameter.getLength();
		}
		if(valueIndex < this.rawValue.length()) {
			result.append(URIs.encodeURIComponent(this.rawValue.substring(valueIndex), this.escapedCharacters, this.charset));
		}
		return result.toString();
	}
	
	@Override
	public String getValue(Map<String, ?> values) {
		if(this.rawValue == null) {
			return "";
		}
		if(this.parameters.isEmpty()) {
			return URIs.encodeURIComponent(this.rawValue, this.escapedCharacters, this.charset);
		}
		String missingValues = this.parameters.stream().map(URIParameter::getName).filter(name -> !values.containsKey(name)).collect(Collectors.joining(", "));
		if(!StringUtils.isEmpty(missingValues)) {
			throw new IllegalArgumentException("Missing values to generate component: " + missingValues);
		}
		
		StringBuilder result = new StringBuilder();
		int valueIndex = 0;
		for(URIParameter parameter : this.parameters) {
			String parameterValue = parameter.checkValue(values.get(parameter.getName()).toString());
			if(parameter.getOffset() > valueIndex) {
				result.append(URIs.encodeURIComponent(this.rawValue.substring(valueIndex, parameter.getOffset()), this.escapedCharacters, this.charset));
			}
			result.append(URIs.encodeURIComponent(parameterValue, this.escapedCharacters, this.charset));
			valueIndex = parameter.getOffset() + parameter.getLength();
		}
		if(valueIndex < this.rawValue.length()) {
			result.append(URIs.encodeURIComponent(this.rawValue.substring(valueIndex), this.escapedCharacters, this.charset));
		}
		return result.toString();
	}
}

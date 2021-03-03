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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * @author jkuhn
 *
 */
class QueryParameterComponent implements ParameterizedURIComponent {
	
	private static final Predicate<Integer> ESCAPED_CHARACTERS =  b -> {
		return !(Character.isLetterOrDigit(b) || b == '-' || b == '.' || b == '_' || b == '~' || b == '!' || b == '$' || b == '&' || b == '\'' || b == '(' || b == ')' || b == '*' || b == '+' || b == ',' || b == ';' || b == ':' || b == '@' || b == '/' || b == '?');
	};
	
	private final URIFlags flags;
	private final String rawName;
	private final String rawValue;
	
	private final Charset charset;
	
	private final List<URIParameter> parameters;
	
	private String pattern;
	private List<String> patternGroupNames;
	
	public QueryParameterComponent(URIFlags flags, Charset charset, String rawName, Object value) {
		this.flags = flags;
		this.charset = charset;
		this.rawName = rawName;
		this.rawValue = value != null ? value.toString() : null;
		this.parameters = new LinkedList<>();
		
		if(this.flags.isParameterized() && StringUtils.isNotBlank(this.rawValue)) {
			byte[] valueBytes = this.rawValue.getBytes(charset);
			String parameterName = null;
			Integer parameterIndex = null;
			for(int i=0;i<valueBytes.length;i++) {
				byte nextByte = valueBytes[i];
				if(nextByte == '{' && parameterIndex == null) {
					// open parameter
					parameterIndex = i;
				}
				else if(nextByte == '}' && parameterIndex != null && valueBytes[i-1] != '\\') {
					// close parameter
					if(parameterName == null) {
						parameterName = new String(valueBytes, parameterIndex + 1, i - (parameterIndex + 1)); 
						this.parameters.add(new URIParameter(parameterIndex, i - parameterIndex + 1, parameterName, this.charset));
					}
					else {
						int patternIndex = parameterIndex + 1 + parameterName.length() + 1;
						this.parameters.add(new URIParameter(parameterIndex, i - parameterIndex + 1, parameterName, new String(valueBytes, patternIndex, i - patternIndex), this.charset));
					}
					parameterName = null;
					parameterIndex = null;
				}
				else if(nextByte == ':' && parameterIndex != null && parameterName == null) {
					parameterName = new String(valueBytes, parameterIndex + 1, i - (parameterIndex + 1));
				}
			}
			if(parameterIndex != null) {
				throw new IllegalArgumentException("Invalid query parameter value with incomplete parameter: " + this.rawValue);
			}
		}
	}
	
	public String getRawParameterName() {
		return this.rawName;
	}
	
	public String getRawParameterValue() {
		return this.rawValue != null ? this.rawValue: "";
	}
	
	public String getParameterName() {
		return URIs.encodeURIComponent(this.rawName, QueryParameterComponent.ESCAPED_CHARACTERS, this.charset);
	}
	
	public String getParameterValue(Object... values) {
		StringBuilder result = new StringBuilder();
		if(this.parameters.isEmpty()) {
			return result.append(URIs.encodeURIComponent(this.rawValue, QueryParameterComponent.ESCAPED_CHARACTERS, this.charset)).toString();
		}
		if(values.length != this.parameters.size()) {
			throw new IllegalArgumentException("Missing values to generate query parameter " + this.rawName + ": " + this.parameters.stream().map(URIParameter::getName).skip(values.length).collect(Collectors.joining(", ")));
		}
		
		int valueIndex = 0;
		for(int i = 0;i<this.parameters.size();i++) {
			URIParameter parameter = this.parameters.get(i);
			String parameterValue = parameter.checkValue(values[i].toString());
			if(parameter.getOffset() > valueIndex) {
				result.append(URIs.encodeURIComponent(this.rawValue.substring(valueIndex, parameter.getOffset()), QueryParameterComponent.ESCAPED_CHARACTERS, this.charset));
			}
			result.append(URIs.encodeURIComponent(parameterValue, QueryParameterComponent.ESCAPED_CHARACTERS, this.charset));
			valueIndex = parameter.getOffset() + parameter.getLength();
		}
		if(valueIndex < this.rawValue.length()) {
			result.append(URIs.encodeURIComponent(this.rawValue.substring(valueIndex), QueryParameterComponent.ESCAPED_CHARACTERS, this.charset));
		}
		return result.toString();
	}
	
	public String getParameterValue(Map<String, ?> values) {
		StringBuilder result = new StringBuilder();
		if(this.parameters.isEmpty()) {
			return result.append(URIs.encodeURIComponent(this.rawValue, QueryParameterComponent.ESCAPED_CHARACTERS, this.charset)).toString();
		}
		String missingValues = this.parameters.stream().map(URIParameter::getName).filter(name -> !values.containsKey(name)).collect(Collectors.joining(", "));
		if(!StringUtils.isEmpty(missingValues)) {
			throw new IllegalArgumentException("Missing values to generate query parameter " + this.rawName + ": " + missingValues);
		}
		
		int valueIndex = 0;
		for(URIParameter parameter : this.parameters) {
			String parameterValue = parameter.checkValue(values.get(parameter.getName()).toString());
			if(parameter.getOffset() > valueIndex) {
				result.append(URIs.encodeURIComponent(this.rawValue.substring(valueIndex, parameter.getOffset()), QueryParameterComponent.ESCAPED_CHARACTERS, this.charset));
			}
			result.append(URIs.encodeURIComponent(parameterValue, QueryParameterComponent.ESCAPED_CHARACTERS, this.charset));
			valueIndex = parameter.getOffset() + parameter.getLength();
		}
		if(valueIndex < this.rawValue.length()) {
			result.append(URIs.encodeURIComponent(this.rawValue.substring(valueIndex), QueryParameterComponent.ESCAPED_CHARACTERS, this.charset));
		}
		return result.toString();
	}
	
	@Override
	public String getRawValue() {
		return this.rawName + "=" + (this.rawValue != null ? this.rawValue: "");
	}
	
	@Override
	public String getValue() {
		return this.getValue(new Object[0]);
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
				patternBuilder.append(Pattern.quote(this.rawName)).append("=");
				if(this.parameters.isEmpty()) {
					patternBuilder.append(Pattern.quote(this.rawValue));
				}
				else {
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
	public String getValue(Object... values) {
		return new StringBuilder().append(this.getParameterName()).append("=").append(this.getParameterValue(values)).toString();
		/*StringBuilder result = new StringBuilder();
		result.append(URIs.encodeURIComponent(this.rawName, QueryParameterComponent.ESCAPED_CHARACTERS, this.charset)).append("=");
		if(this.parameters.isEmpty()) {
			return result.append(URIs.encodeURIComponent(this.rawValue, QueryParameterComponent.ESCAPED_CHARACTERS, this.charset)).toString();
		}
		if(values.length != this.parameters.size()) {
			throw new IllegalArgumentException("Missing values to generate query parameter " + this.rawName + ": " + this.parameters.stream().map(URIParameter::getName).skip(values.length).collect(Collectors.joining(", ")));
		}
		
		int valueIndex = 0;
		for(int i = 0;i<this.parameters.size();i++) {
			URIParameter parameter = this.parameters.get(i);
			String parameterValue = parameter.checkValue(values[i].toString());
			if(parameter.getOffset() > valueIndex) {
				result.append(URIs.encodeURIComponent(this.rawValue.substring(valueIndex, parameter.getOffset()), QueryParameterComponent.ESCAPED_CHARACTERS, this.charset));
			}
			result.append(URIs.encodeURIComponent(parameterValue, QueryParameterComponent.ESCAPED_CHARACTERS, this.charset));
			valueIndex = parameter.getOffset() + parameter.getLength();
		}
		if(valueIndex < this.rawValue.length()) {
			result.append(URIs.encodeURIComponent(this.rawValue.substring(valueIndex), QueryParameterComponent.ESCAPED_CHARACTERS, this.charset));
		}
		return result.toString();*/
	}
	
	@Override
	public String getValue(Map<String, ?> values) {
		return new StringBuilder().append(this.getParameterName()).append("=").append(this.getParameterValue(values)).toString();
		/*StringBuilder result = new StringBuilder();
		result.append(URIs.encodeURIComponent(this.rawName, QueryParameterComponent.ESCAPED_CHARACTERS, this.charset)).append("=");
		if(this.parameters.isEmpty()) {
			return result.append(URIs.encodeURIComponent(this.rawValue, QueryParameterComponent.ESCAPED_CHARACTERS, this.charset)).toString();
		}
		String missingValues = this.parameters.stream().map(URIParameter::getName).filter(name -> !values.containsKey(name)).collect(Collectors.joining(", "));
		if(!StringUtils.isEmpty(missingValues)) {
			throw new IllegalArgumentException("Missing values to generate query parameter " + this.rawName + ": " + missingValues);
		}
		
		int valueIndex = 0;
		for(URIParameter parameter : this.parameters) {
			String parameterValue = parameter.checkValue(values.get(parameter.getName()).toString());
			if(parameter.getOffset() > valueIndex) {
				result.append(URIs.encodeURIComponent(this.rawValue.substring(valueIndex, parameter.getOffset()), QueryParameterComponent.ESCAPED_CHARACTERS, this.charset));
			}
			result.append(URIs.encodeURIComponent(parameterValue, QueryParameterComponent.ESCAPED_CHARACTERS, this.charset));
			valueIndex = parameter.getOffset() + parameter.getLength();
		}
		if(valueIndex < this.rawValue.length()) {
			result.append(URIs.encodeURIComponent(this.rawValue.substring(valueIndex), QueryParameterComponent.ESCAPED_CHARACTERS, this.charset));
		}
		return result.toString();*/
	}
}

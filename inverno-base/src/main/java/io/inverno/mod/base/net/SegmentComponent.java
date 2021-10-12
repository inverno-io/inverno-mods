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
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * A URI component representing a segment part of a path in a URI as defined by
 * <a href="https://tools.ietf.org/html/rfc3986#section-3.3">RFC 3986 Section 3.3</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see ParameterizedURIComponent
 */
class SegmentComponent implements ParameterizedURIComponent {

	private static final Predicate<Integer> ESCAPED_CHARACTERS_SLASH =  b -> {
		return !(Character.isLetterOrDigit(b) || b == '-' || b == '.' || b == '_' || b == '~' || b == '!' || b == '$' || b == '&' || b == '\'' || b == '(' || b == ')' || b == '*' || b == '+' || b == ',' || b == ';' || b == '=' || b == ':' || b == '@');
	};
	
	private static final Predicate<Integer> ESCAPED_CHARACTERS_NO_SLASH =  b -> {
		return !(Character.isLetterOrDigit(b) || b == '-' || b == '.' || b == '_' || b == '~' || b == '!' || b == '$' || b == '&' || b == '\'' || b == '(' || b == ')' || b == '*' || b == '+' || b == ',' || b == ';' || b == '=' || b == ':' || b == '@' || b == '/');
	};
	
	private final URIFlags flags;
	private final String rawValue;
	
	private final Charset charset;

	private final List<URIParameter> parameters;
	private boolean previousWildcard = false;
	
	private String pattern;
	private List<String> patternGroupNames;
	
	private String segmentRawValue;
	
	private boolean directoriesPattern;
	private boolean terminal;
	
	/**
	 * <p>
	 * Creates a segment component with the specified flags, charset and raw value.
	 * </p>
	 * 
	 * @param flags    URI flags
	 * @param charset  a charset
	 * @param rawValue a raw value
	 */
	public SegmentComponent(URIFlags flags, Charset charset, String rawValue) {
		this(flags, charset, rawValue, false);
	}
	
	private SegmentComponent(URIFlags flags, Charset charset, String path, boolean consumePath) {
		this.flags = flags;
		this.charset = charset;
		this.parameters = new LinkedList<>();
		
		BiPredicate<Integer, Byte> breakPredicate = null;
		if(this.flags.isPathPattern()) {
			breakPredicate = (i, nextByte) -> {
				if(directoriesPattern && nextByte != '/') {
					throw new URIBuilderException("Invalid usage of path pattern '**' which is exclusive: /" + path.substring(0, i+1));
				}
				if(nextByte == '?') {
					this.parameters.add(new URIParameter(i, 1, null, "[^/]", charset));
				}
				else if(nextByte == '*') {
					if(this.previousWildcard) {
						if(i - 1 > 0) {
							throw new URIBuilderException("Invalid usage of path pattern '**' which is exclusive: /" + path.substring(0, i+1));
						}
						this.parameters.add(new URIParameter(i-1, 2, null, ".*", charset));
						this.previousWildcard = false;
						this.directoriesPattern = true;
					}
					else {
						this.previousWildcard = true;
					}
				}
				else if(this.previousWildcard) {
					this.parameters.add(new URIParameter(i-1, 1, null, "[^/]*", charset));
					this.previousWildcard = false;
				}
				return false;
			};
		}
		
		if(consumePath) {
			BiPredicate<Integer, Byte> consumeBreakPredicate = (i, nextByte) -> {
				if(nextByte == '/') {
					this.segmentRawValue = i == 0 ? "" : path.substring(0, i);
					return true;
				}
				return false;
			};
			breakPredicate = breakPredicate != null ? breakPredicate.or(consumeBreakPredicate) : consumeBreakPredicate;
		}
		
		Consumer<URIParameter> parameterHandler = null;
		if(this.flags.isParameterized()) {
			parameterHandler = this.parameters::add;
		}
		
		URIs.scanURIComponent(path, null, charset, parameterHandler, breakPredicate);
		this.rawValue = this.segmentRawValue != null ? this.segmentRawValue : path;
		if(this.previousWildcard) {
			this.parameters.add(new URIParameter(this.rawValue.length() - 1, 1, null, "[^/]*", charset));
		}
	}

	/**
	 * <p>
	 * Returns true if this segment represent a directories path pattern: {@code **}.
	 * </p>
	 *
	 * @return true if the segment is a directories path pattern, false otherwise
	 */
	public boolean isDirectoriesPattern() {
		return this.directoriesPattern;
	}
	
	/**
	 * <p>
	 * Returns true if segment is terminal to indicate that no more segment can be added to the path.
	 * </p>
	 * 
	 * <p>
	 * A segment is terminal when it is preceeded by a directories path pattern segment ({@code **}) and matches a wildcard path pattern ({@code *}) which is the case of a regular path parameter.
	 * </p>
	 * 
	 * @return true if the segment is terminal, false otherwise
	 */
	public boolean isTerminal() {
		return this.terminal;
	}
	
	/**
	 * <p>
	 * Injects the next segment into the segment to set the exit pattern when the segment represents a directories path pattern.
	 * </p>
	 * 
	 * <p>
	 * If the segment does not represent a directories path pattern, this is a noop.
	 * </p>
	 * 
	 * @param nextSegment the next segment in the path
	 */
	public void setNextSegment(SegmentComponent nextSegment) throws IllegalStateException {
		if(this.directoriesPattern) {
			String nextSegmentUnnamedNonCapturingPattern = nextSegment.getUnnamedNonCapturingPattern();
			if(nextSegmentUnnamedNonCapturingPattern.equals("(?:[^/]*)")) {
				// The next segment is then terminal
				this.pattern = "([^/]*(?<![^/]*$)(?:/[^/]*(?<!/[^/]*$))*)";
				nextSegment.terminal = true;
			}
			else {
				this.pattern = String.format("([^/]*(?<!%1$s)(?:/[^/]*(?<!/%1$s))*)", nextSegmentUnnamedNonCapturingPattern);
			}
			this.patternGroupNames = new LinkedList<>();
			this.patternGroupNames.add(null);
		}
	}
	
	/**
	 * <p>
	 * Returns a list of segment components contained in the specified path considering heading and trailing slashes or not.
	 * </p>
	 *
	 * <p>
	 * When heading and/or trailing are not ignored, empty segment components are added to to the resulting list.
	 * </p>
	 *
	 * @param flags               URI flags
	 * @param charset             a charset
	 * @param path                a path
	 * @param ignoreHeadingSlash  true to ignore heading slash
	 * @param ignoreTrailingSlash true to ignore trailing slash
	 *
	 * @return a list of segment components or an empty list
	 */
	public static List<SegmentComponent> fromPath(URIFlags flags, Charset charset, String path, boolean ignoreHeadingSlash, boolean ignoreTrailingSlash) {
		LinkedList<SegmentComponent> segments = new LinkedList<>();
		if(StringUtils.isNotBlank(path)) {
			String currentPath = path;
			if(currentPath.charAt(0) == '/') {
				currentPath = currentPath.substring(1);
				if(!ignoreHeadingSlash) {
					segments.add(new SegmentComponent(flags, charset, ""));
				}
			}
			
			if(ignoreTrailingSlash) {
				if(currentPath.isEmpty()) {
					return segments;
				}
				else if(currentPath.charAt(currentPath.length() - 1) == '/') {
					currentPath = currentPath.substring(0, currentPath.length() - 1);
				}
			}
			
			SegmentComponent nextSegment = null;
			do {
				if(nextSegment != null) {
					currentPath = currentPath.substring(nextSegment.getRawValue().length() + 1);
				}
				nextSegment = new SegmentComponent(flags, charset, currentPath, true);
				
				if(nextSegment.isDirectoriesPattern()) {
					// we have **, it can't be preceded by **
					if(!segments.isEmpty() && segments.peekLast().isDirectoriesPattern()) {
						throw new URIBuilderException("Invalid path: **/**");
					}
					segments.add(nextSegment);
				}
				else if(flags.isNormalized()) {
					// Note that, this doesn't apply to parameterized segment that might be set to ../ or ./ as  a result, normalization will also take place during the build of a parameterized URI
					String nextSegmentValue = nextSegment.getRawValue();
					if(nextSegmentValue.equals(".")) {
						// Segment is ignored
					}
					else if(nextSegmentValue.equals("..")) {
						// We have to remove the previous segment if it is not '..' OR keep that segment if there's no previous segment
						if(!segments.isEmpty()) {
							SegmentComponent lastSegment = segments.peekLast();
							String lastSegmentValue = lastSegment.getRawValue();
							if(lastSegmentValue.equals("..") || (segments.size() == 1 && lastSegmentValue.equals(""))) {
								segments.add(nextSegment);
							}
							else {
								segments.removeLast();
							}
						}
						else {
							segments.add(nextSegment);
						}
					}
					else {
						if(!segments.isEmpty()) {
							SegmentComponent lastSegment = segments.peekLast();
							if(lastSegment.isTerminal()) {
								throw new URIBuilderException("Invalid path: **/* is terminal");
							}
							lastSegment.setNextSegment(nextSegment);
						}
						segments.add(nextSegment);
					}
				}
				else {
					if(!segments.isEmpty()) {
						SegmentComponent lastSegment = segments.peekLast();
						if(lastSegment.isTerminal()) {
							throw new URIBuilderException("Invalid path: **/* is terminal");
						}
						lastSegment.setNextSegment(nextSegment);
					}
					segments.add(nextSegment);
				}
			} while(currentPath.length() >= nextSegment.getRawValue().length() + 1);
		}
		return segments;
	}
	
	@Override
	public String getRawValue() {
		return this.rawValue;
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
	
	private String getUnnamedNonCapturingPattern() {
		if(this.rawValue == null) {
			return "";
		}
		else {
			StringBuilder patternBuilder = new StringBuilder();
			int valueIndex = 0;
			for(URIParameter parameter : this.parameters) {
				if(parameter.getOffset() > valueIndex) {
					patternBuilder.append("(?:").append(Pattern.quote(this.rawValue.substring(valueIndex, parameter.getOffset()))).append(")");
				}
				patternBuilder.append(parameter.getUnnamedNonCapturingPattern());
				valueIndex = parameter.getOffset() + parameter.getLength();
			}
			if(valueIndex < this.rawValue.length()) {
				patternBuilder.append("(?:").append(Pattern.quote(this.rawValue.substring(valueIndex))).append(")");
			}
			return patternBuilder.toString();
		}
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
		return this.getValue(values, true);
	}
	
	@Override
	public String getValue(Map<String, ?> values) {
		return this.getValue(values, true);
	}
	
	/**
	 * <p>
	 * Returns the segment component value after replacing the parameters with the string representation of the specified values escaping or not slash it might contain.
	 * </p>
	 *
	 * <p>
	 * Note that the resulting value is percent encoded as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-2.1">RFC 3986 Section 2.1</a>.
	 * </p>
	 *
	 * @param values      an array of values to replace the component's parameters
	 * @param escapeSlash true to escape the slash contained in the segment
	 *
	 * @return the segment value
	 *
	 * @throws IllegalArgumentException if there's not enough values to replace all parameters
	 */
	public String getValue(Object[] values, boolean escapeSlash) throws IllegalArgumentException {
		if(this.parameters.isEmpty()) {
			return URIs.encodeURIComponent(this.rawValue, SegmentComponent.ESCAPED_CHARACTERS_SLASH, this.charset);
		}
		if(values.length != this.parameters.size()) {
			throw new IllegalArgumentException("Missing values to generate segment: " + this.parameters.stream().map(URIParameter::getName).skip(values.length).collect(Collectors.joining(", ")));
		}
		
		Predicate<Integer> allowedCharacters = escapeSlash ? SegmentComponent.ESCAPED_CHARACTERS_SLASH : SegmentComponent.ESCAPED_CHARACTERS_NO_SLASH;
		
		StringBuilder result = new StringBuilder();
		int valueIndex = 0;
		for(int i = 0;i<this.parameters.size();i++) {
			URIParameter parameter = this.parameters.get(i);
			String parameterValue = parameter.checkValue(values[i].toString());
			if(parameter.getOffset() > valueIndex) {
				result.append(URIs.encodeURIComponent(this.rawValue.substring(valueIndex, parameter.getOffset()), SegmentComponent.ESCAPED_CHARACTERS_SLASH, this.charset));
			}
			result.append(URIs.encodeURIComponent(parameterValue, allowedCharacters, this.charset));
			valueIndex = parameter.getOffset() + parameter.getLength();
		}
		if(valueIndex < this.rawValue.length()) {
			result.append(URIs.encodeURIComponent(this.rawValue.substring(valueIndex), SegmentComponent.ESCAPED_CHARACTERS_SLASH, this.charset));
		}
		return result.toString();
	}
	
	/**
	 * <p>
	 * Returns the segment component value after replacing the parameters with the string representation of the specified values escaping or not slash it might contain.
	 * </p>
	 *
	 * <p>
	 * Note that the resulting value is percent encoded as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-2.1">RFC 3986 Section 2.1</a>.
	 * </p>
	 *
	 * @param values      a map of values to replace the component's parameters
	 * @param escapeSlash true to escape the slash contained in the segment
	 *
	 * @return the segment value
	 *
	 * @throws IllegalArgumentException if there are missing values
	 */
	public String getValue(Map<String, ?> values, boolean escapeSlash) throws IllegalArgumentException {
		if(this.parameters.isEmpty()) {
			return URIs.encodeURIComponent(this.rawValue, SegmentComponent.ESCAPED_CHARACTERS_SLASH, this.charset);
		}
		String missingValues = this.parameters.stream().map(URIParameter::getName).filter(name -> !values.containsKey(name)).collect(Collectors.joining(", "));
		if(!StringUtils.isEmpty(missingValues)) {
			throw new IllegalArgumentException("Missing values to generate segment: " + missingValues);
		}
		
		Predicate<Integer> allowedCharacters = escapeSlash ? SegmentComponent.ESCAPED_CHARACTERS_SLASH : SegmentComponent.ESCAPED_CHARACTERS_NO_SLASH;
		
		StringBuilder result = new StringBuilder();
		int valueIndex = 0;
		for(URIParameter parameter : this.parameters) {
			String parameterValue = parameter.checkValue(values.get(parameter.getName()).toString());
			if(parameter.getOffset() > valueIndex) {
				result.append(URIs.encodeURIComponent(this.rawValue.substring(valueIndex, parameter.getOffset()), SegmentComponent.ESCAPED_CHARACTERS_SLASH, this.charset));
			}
			result.append(URIs.encodeURIComponent(parameterValue, allowedCharacters, this.charset));
			valueIndex = parameter.getOffset() + parameter.getLength();
		}
		if(valueIndex < this.rawValue.length()) {
			result.append(URIs.encodeURIComponent(this.rawValue.substring(valueIndex), SegmentComponent.ESCAPED_CHARACTERS_SLASH, this.charset));
		}
		return result.toString();
	}
}

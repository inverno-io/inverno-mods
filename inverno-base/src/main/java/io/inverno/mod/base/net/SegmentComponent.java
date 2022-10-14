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
import java.util.ArrayList;
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

	public static final Predicate<Integer> ESCAPED_CHARACTERS_SLASH =  b -> {
		return !(Character.isLetterOrDigit(b) || b == '-' || b == '.' || b == '_' || b == '~' || b == '!' || b == '$' || b == '&' || b == '\'' || b == '(' || b == ')' || b == '*' || b == '+' || b == ',' || b == ';' || b == '=' || b == ':' || b == '@');
	};
	
	public static final Predicate<Integer> ESCAPED_CHARACTERS_NO_SLASH =  b -> {
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
	
	private boolean directories;
	private Boolean wildcard;
	private Boolean custom;
	
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
				if(directories && nextByte != '/') {
					throw new URIBuilderException("Invalid usage of path pattern '**' which is exclusive: /" + path.substring(0, i+1));
				}
				if(nextByte == '?') {
					if(this.previousWildcard) {
						this.parameters.add(new URIParameter(i-1, 1, null, "[^/]*", charset));
						this.previousWildcard = false;
					}
					this.parameters.add(new URIParameter(i, 1, null, "[^/]", charset));
				}
				else if(nextByte == '*') {
					if(this.previousWildcard) {
						if(i - 1 > 0) {
							throw new URIBuilderException("Invalid usage of path pattern '**' which is exclusive: /" + path.substring(0, i+1));
						}
						this.parameters.add(new URIParameter(i-1, 2, null, ".*", charset));
						this.previousWildcard = false;
						this.directories = true;
						this.wildcard = this.custom = false;
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
	 * @return true if the segment represent a directories path pattern, false otherwise
	 */
	public boolean isDirectories() {
		return this.directories;
	}
	
	/**
	 * <p>
	 * Returns true if this segment represents a wildcard pattern.
	 * </p>
	 * 
	 * <p>
	 * A wildcard segment contains only wildcard pattern parameters (see {@link URIParameter#isWildcardPattern() }) and no static part.
	 * </p>
	 *
	 * @return true if the segment represent a wildcard pattern, false otherwise
	 */
	public boolean isWildcard() {
		if(this.wildcard == null) {
			int index = 0;
			for(URIParameter parameter : this.parameters) {
				if(!parameter.isWildcard() || parameter.getOffset() != index) {
					this.wildcard = false;
					break;
				}
				index += parameter.getLength();
			}
			this.wildcard = !this.rawValue.isEmpty() && index == this.rawValue.length();
		}
		return this.wildcard;
	}
	
	/**
	 * <p>
	 * Returns true if this segment represents a custom pattern.
	 * </p>
	 * 
	 * <p>
	 * A custom segment must contain at least one custom pattern parameter (see {@link URIParameter#isCustomPattern()}).
	 * </p>
	 *
	 * @return true if the segment represents a custom pattern, false otherwise
	 */
	public boolean isCustom() {
		if(this.custom == null) {
			this.custom = false;
			for(URIParameter parameter : this.parameters) {
				if(parameter.isCustom()) {
					this.custom = true;
					break;
				}
			}
		}
		return this.custom;
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
				
				if(nextSegment.isDirectories()) {
					// we have **, it can't be preceded by **
					if(!segments.isEmpty() && segments.peekLast().isDirectories()) {
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
						segments.add(nextSegment);
					}
				}
				else {
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
		return this.getValue(List.of());
	}
	
	@Override
	public String getPattern() {
		if(this.pattern == null) {
			if(this.rawValue == null) {
				this.patternGroupNames = List.of();
				this.pattern = "";
			}
			else if(this.directories) {
				this.patternGroupNames = new LinkedList<>();
				this.patternGroupNames.add(null);
				this.pattern = "((?:/[^/]*)*)";
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
	
	/**
	 * <p>
	 * Returns an unnamed non-capturing pattern matching the segment.
	 * </p>
	 * 
	 * @return an unnamed non-capturing pattern
	 */
	public String getUnnamedNonCapturingPattern() {
		if(this.rawValue == null) {
			return "";
		}
		else if(this.directories) {
			return "(?:(?:/[^/]*)*)";
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
	public String getValue(List<Object> values) {
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
	 * @param values      a list of values to replace the component's parameters
	 * @param escapeSlash true to escape the slash contained in the segment
	 *
	 * @return the segment value
	 *
	 * @throws IllegalArgumentException if there's not enough values to replace all parameters
	 */
	public String getValue(List<Object> values, boolean escapeSlash) throws IllegalArgumentException {
		if(this.parameters.isEmpty()) {
			return URIs.encodeURIComponent(this.rawValue, SegmentComponent.ESCAPED_CHARACTERS_SLASH, this.charset);
		}
		if(values.size() != this.parameters.size()) {
			throw new IllegalArgumentException("Missing values to generate segment: " + this.parameters.stream().map(URIParameter::getName).skip(values.size()).collect(Collectors.joining(", ")));
		}
		
		Predicate<Integer> allowedCharacters = escapeSlash ? SegmentComponent.ESCAPED_CHARACTERS_SLASH : SegmentComponent.ESCAPED_CHARACTERS_NO_SLASH;
		
		StringBuilder result = new StringBuilder();
		int valueIndex = 0;
		for(int i = 0;i<this.parameters.size();i++) {
			URIParameter parameter = this.parameters.get(i);
			String parameterValue = parameter.checkValue(values.get(i).toString());
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
	
	/**
	 * <p>
	 * Represents a static part in a segment component.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.3
	 */
	private static class StaticSegmentPart implements URIComponentPart {

		private String value;
		
		public StaticSegmentPart(String value) {
			this.value = value;
		}
		
		@Override
		public boolean isStatic() {
			return true;
		}

		@Override
		public boolean isQuestionMark() {
			return false;
		}

		@Override
		public boolean isWildcard() {
			return false;
		}

		@Override
		public boolean isCustom() {
			return false;
		}

		@Override
		public String getValue() {
			return value;
		}

		/**
		 * <p>
		 * Sets the value of the part.
		 * </p>
		 * 
		 * @param value a new value
		 */
		public void setValue(String value) {
			this.value = value;
		}
	}
	
	/**
	 * <p>
	 * Splits this segment component into parts.
	 * </p>
	 * 
	 * @return a list of URI component parts
	 */
	private List<URIComponentPart> splitSegment() {
		List<URIComponentPart> split = new ArrayList<>();
		int valueIndex = 0;
		for(URIParameter parameter : this.parameters) {
			if(parameter.getOffset() > valueIndex) {
				split.add(new StaticSegmentPart(this.rawValue.substring(valueIndex, parameter.getOffset())));
			}
			split.add(parameter);
			valueIndex = parameter.getOffset() + parameter.getLength();
		}
		if(valueIndex < this.rawValue.length()) {
			split.add(new StaticSegmentPart(this.rawValue.substring(valueIndex)));
		}
		return split;
	}
	
	/**
	 * <p>
	 * Determines whether the other path segment is included into this path segment.
	 * </p>
	 *
	 * @param other a path segment component
	 *
	 * @return {@link URIPattern.Inclusion#INCLUDED} if the segment is included, {@link URIPattern.Inclusion#DISJOINT} if segments are disjoint, {@link URIPattern.Inclusion#INDETERMINATE} otherwise
	 */
	public URIPattern.Inclusion includes(SegmentComponent other) {
		// * and *, we have a match, how to determine this?
		// if s1 has parameters they must be * (safe) or ? (in which case s2 must have ? at the same position)
		// we can do this with parsing well, not so fast we don't know if path pattern is supported here
		// we can find \Q and \S
		// a pattern is (?:\Q<static>\S)(?:<dynamic>)...
		// <static> must match, <dynamic> can be: * and ?, ? and ?
		// if there's not the same number of groups, is it a no match? or indeterminate
		// Actually if static differs it's a no match, then it's indeterminate, we can't determine things any further
		
		// Extract the groups of each segment
		List<URIComponentPart> s1Parts = this.splitSegment();
		List<URIComponentPart> s2Parts = other.splitSegment();

		// Let's compare them
		int i=0;
		int j=0;
		while(i < s1Parts.size() && j < s2Parts.size()) {
			URIComponentPart s1Part = s1Parts.get(i);
			URIComponentPart s2Part = s2Parts.get(j);
			
			if(s1Part.isStatic()) {
				// s1Part is static
				if(s2Part.isStatic()) {
					// s2Part is static
					int value1Length = s1Part.getValue().length();
					int value2Length = s2Part.getValue().length();
					
					if(value1Length < value2Length && s2Part.getValue().startsWith(s1Part.getValue())) {
						// s2Part starts with s1Part
						i++;
						((StaticSegmentPart)s2Part).setValue(s2Part.getValue().substring(value1Length));
					}
					else if(value1Length > value2Length && s1Part.getValue().startsWith(s2Part.getValue())) {
						// s1Part starts with s2Part
						((StaticSegmentPart)s1Part).setValue(s1Part.getValue().substring(value2Length));
						j++;
					}
					else if(value1Length == value2Length && s1Part.getValue().equals(s2Part.getValue())) {
						// s1Part == s2Part
						i++;
						j++;
					}
					else {
						return URIPattern.Inclusion.DISJOINT;
					}
				}
				else {
					// SegmentGroup.QUESTION_MARK:
					// we could check whether next s2 group matches s1.substring(1), if it doesn't, there's no match otherwise it is indeterminate
					// but where do we stop, this also applies to *, let's keep it simple for now
					
					// SegmentGroup.WILDCARD:
					// s2Part is * so we have a match but s2 matches more than s1
					
					// s2Part.equals(SegmentGroup.OTHER_PATTERN:
					// s2Part is another pattern we can't do better
					
					return URIPattern.Inclusion.INDETERMINATE;
				}
			}
			else if(s1Part.isCustom()) {
				// s1Part is a custom regex
				// We can't determine what to do here since we don't know what is matched by s1Part unless patterns are equals
				if(s2Part.isCustom() && s1Part.getValue().equals(s2Part.getValue())) {
					i++;
					j++;
				}
				else {
					return URIPattern.Inclusion.INDETERMINATE;
				}
			}
			else if(s1Part.isQuestionMark()) {
				// s1Part is ?
				if(s2Part.isStatic()) {
					// s2Part is static
					// if s2Part is a single character s1 matches more than s2, otherwise we have to look forward => we must remove the first character and check next group in s1
					if(s2Part.getValue().length() == 1) {
						// if s2Part is a single character s1 matches more than s2
						i++;
						j++;
					}
					else {
						// otherwise we can consider s2Part without the first character and consider next s1group
						((StaticSegmentPart)s2Part).setValue(s2Part.getValue().substring(1));
						i++;
					}
				}
				else if(s2Part.isQuestionMark()) {
					// s1Part == s2Part
					i++;
					j++;
				}
				else {
					// SegmentGroup.WILDCARD:
					// We can continue if the next s1Part is * (ie. ?*)
					// This should hopefully barely happen, in any case returning INDETERMINATE is safe here since s2 is most likely to match more than s1
					
					// SegmentGroup.OTHER_PATTERN:
					// s2Part is another pattern we can't do better
					
					return URIPattern.Inclusion.INDETERMINATE;
				}
			}
			else {
				// s1Part is *
				// It can be followed by other * or ? (eg. {param1}?{param2}) in which case we can ignore them since they are not relevant
				i++;
				for(;i<s1Parts.size();i++) {
					s1Part = s1Parts.get(i);
					if(s1Part.isStatic()) {
						break;
					}
					else if(s1Part.isCustom()) {
						// We can stop here
						return URIPattern.Inclusion.INDETERMINATE;
					}
				}
				if(i == s1Parts.size()) {
					// s1 ends with * => it matches everything unless s2 has a custom pattern group in which case outcome is indeterminate since it can consume segments
					for(;j<s2Parts.size();j++) {
						if(s2Part.isCustom()) {
							 return URIPattern.Inclusion.INDETERMINATE;
						}
					}
					return URIPattern.Inclusion.INCLUDED;
				}

				// we should advanced s2 as long as its groups are * or ?
				for(;j<s2Parts.size();j++) {
					s2Part = s2Parts.get(j);
					if(s2Part.isStatic()) {
						break;
					}
					else if(s2Part.isCustom()) {
						// This other pattern can match s1 exit group: 
						// s1 is of the form: ...([^/]*)(\Qexit\Q)...
						// s2 must be of the form: ...([^/]*)(\Qexit\S)..., 
						// For instance the following sequence is indeterminate since we can't guess what's inside the custom regex we can't see that it actually contain s1's exit group ...([^/]*)([^/]*\Qexit\S[^/]*)([^/]*)(\Qexit\S), 
						return URIPattern.Inclusion.INDETERMINATE; 
					}
				}
				if(j == s2Parts.size()) {
					// s2 ends with * as well
					return URIPattern.Inclusion.INCLUDED;
				}
				// s1Part and s2Part should now be both static
				if(!s1Part.getValue().equals(s2Part.getValue())) {
					return URIPattern.Inclusion.DISJOINT;
				}
				i++;
				j++;
			}
		}
		// If we get there, it means we consumed s1, s2 or both
		if(i == s1Parts.size() && j == s2Parts.size()) {
			// We have consumed both, we must have inclusion
			return URIPattern.Inclusion.INCLUDED;
		}
		else if(i == s1Parts.size()) {
			// s1 is the consumed segment, it doesn't end with *
			for(;j<s2Parts.size();j++) {
				URIComponentPart s2Part = s2Parts.get(j);
				if(s2Part.isStatic() || s2Part.isQuestionMark()) {
					// we know for sure s2 matches more data so we are disjointed
					return URIPattern.Inclusion.DISJOINT; 
				}
			}
			// s2 has only * or regex groups remaining inclusion is indeterminate
			return URIPattern.Inclusion.INDETERMINATE;
		}
		else {
			//  s2 is the consumed segment, it doesn't end with *
			for(;i<s1Parts.size();i++) {
				URIComponentPart s1Part = s1Parts.get(i);
				if(s1Part.isStatic() || s1Part.isQuestionMark()) {
					// we know for sure s1 matches more data so we are disjointed
					return URIPattern.Inclusion.DISJOINT; 
				}
				if(s1Part.isCustom()) {
					// we have a custom regex so we can't determine wheter s1 matches more data
					return URIPattern.Inclusion.INDETERMINATE; 
				}
			}
			// s1 has only * groups remaining, it matches more than s2
			return URIPattern.Inclusion.INCLUDED;
		}
	}
}

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

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import io.inverno.mod.base.Charsets;

/**
 * <p>
 * Utility methods for URIs manipulation.
 * </p>
 *
 * <p>
 * A URI can be created fluently:
 * </p>
 *
 * <pre>{@code
 * // http://localhost/foo/bar/123
 * URI uri = URIs.uri()
 *     .scheme("http")
 *     .host("localhost")
 *     .path("/foo/bar/123")
 *     .build();
 * }</pre>
 *
 * <p>
 * A URI can be automatically normalized by enabling the {@link Option#NORMALIZED} option:
 * </p>
 *
 * <pre>{@code
 * // http://localhost/123
 * URI uri = URIs.uri(URIs.Option.NORMALIZED)
 *     .scheme("http")
 *     .host("localhost")
 *     .path("/foo/../123")
 *     .build();
 * }</pre>
 *
 * <p>
 * URI templates can be created by enabling the {@link Option#PARAMETERIZED} option and specifying parameters of the form {@code {<name>[:<pattern>]}} in the URI components:
 * </p>
 *
 * <pre>{@code
 * URIBuilder uriTemplate = URIs.uri(URIs.Option.PARAMETERIZED)
 *     .scheme("{scheme}")
 *     .host("localhost")
 *     .path("/static/{custom_path}");
 *
 * // https://localhost/static/resource1
 * URI uri1 = uriTemplate.build("https", "resource1");
 *
 * // http://localhost/static/resource2
 * URI uri2 = uriTemplate.build("http", "resource2");
 * }</pre>
 * 
 * <p>
 * URI matchers can be created by enabling the {@link Option#PARAMETERIZED} and/or {@link Option#PATH_PATTERN} options and specifying unnamed parameters of the form
 * {@code {[:<pattern>]}} and/or path patterns using {@code ?}, {@code *} and {@code **} in the URI components:
 * </p>
 * 
 * <pre>{@code
 * URIBuilder uriBuilder = URIs.uri("/static/**" + "/*.png", URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);
 * URIPattern uriPattern = uriBuilder.buildPathPattern(false);
 * 
 * // true
 * uriPattern.matcher("/static/path/to/image.png").matches();
 * 
 * // false
 * uriPattern.matcher("/static/image.jpg").matches();
 * }</pre>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see URIBuilder
 */
public final class URIs {

	/**
	 * <p>
	 * Defines the options used to create a URI builder.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	public enum Option {
		/**
		 * <p>
		 * Normalizes the URI
		 * </p>
		 */
		NORMALIZED,
		/**
		 * <p>
		 * Enables parameterized URI
		 * </p>
		 */
		PARAMETERIZED,
		/**
		 * <p>
		 * Enables path pattern support using the following rules:
		 * </p>
		 * 
		 * <ul>
		 *   <li>? matches one character</li>
		 *   <li>* matches zero or more characters</li>
		 *   <li>** matches zero or more directories in path</li>
		 * </ul>
		 * 
		 * <p>
		 * <code>**&#47;*</code> is a terminal combination which means that no more path segment can be added after.
		 * </p>
		 * 
		 */
		PATH_PATTERN,
	}
	
	/**
	 * <p>
	 * Defines the forms supported when building a URI from a request-target as defined by <a href="https://tools.ietf.org/html/rfc7230#section-5.3">RFC 7230 Section 5.3</a>.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.3
	 */
	public enum RequestTargetForm {
		/**
		 * <p>
		 * Designates an absolute path request target (i.e. {@code absolute-path}) as defined by <a href="https://tools.ietf.org/html/rfc7230">RFC 7230</a>
		 * </p>
		 */
		PATH,
		/**
		 * <p>
		 * Designates an absolute path request target with query (i.e. {@code absolute-path [ "?" query ]}) as defined by <a href="https://tools.ietf.org/html/rfc7230">RFC 7230</a>
		 * </p>
		 */
		PATH_QUERY,
		/**
		 * <p>
		 * Designates an absolute path request target with query and fragment (i.e. {@code absolute-path [ "?" query ] [ "#" fragment ]}) as defined by
		 * <a href="https://tools.ietf.org/html/rfc7230">RFC 7230</a>.
		 * </p>
		 */
		PATH_QUERY_FRAGMENT,
	}

	/**
	 * <p>
	 * Creates a URI builder with the specified options and default charset.
	 * </p>
	 * 
	 * @param options a list of options
	 * 
	 * @return a URI builder
	 */
	public static URIBuilder uri(URIs.Option... options) {
		return new GenericURIBuilder(Charsets.DEFAULT, options);
	}

	/**
	 * <p>
	 * Creates a URI builder from the specified {@link URIs.RequestTargetForm#PATH_QUERY_FRAGMENT} request-target ignoring trailing slash with the specified options and default charset.
	 * </p>
	 *
	 * <p>
	 * The request-target corresponds to the origin form as defined by
	 * <a href="https://tools.ietf.org/html/rfc7230#section-5.3.1">RFC 7230 Section 5.3.1</a> with the addition of the fragment component, as such it may contain query and/or a fragment URI
	 * components.
	 * </p>
	 *
	 * <p>
	 * The origin-form request is incompatible with {@link URIs.Option#PATH_PATTERN} option since query component delimiter {@code ?} is conflicting with path pattern character {@code ?}.
	 * </p>
	 * 
	 * <p>
	 * Note that the different URI components composing the specified request-target are decoded individually after parsing the initial parsing of the request-target.
	 * </p>
	 *
	 * @param requestTarget an extended origin-form request-target
	 * @param options       a list of options
	 *
	 * @return a URI builder
	 *
	 * @throws URIBuilderException if option {@link URIs.Option#PATH_PATTERN} is specified which is incompatible with origin-form request
	 */
	public static URIBuilder uri(String requestTarget, URIs.Option... options) throws URIBuilderException {
		return new GenericURIBuilder(requestTarget, URIs.RequestTargetForm.PATH_QUERY_FRAGMENT, true, Charsets.DEFAULT, options);
	}
	
	/**
	 * <p>
	 * Creates a URI builder from the specified request-target of the specified form ignoring trailing slash with the specified options and default charset.
	 * </p>
	 *
	 * <p>
	 * The request-target must be of the specified form as defined by <a href="https://tools.ietf.org/html/rfc7230#section-5.3">RFC 7230 Section 5.3</a>.
	 * </p>
	 * 
	 * <p>
	 * The origin-form request is incompatible with {@link URIs.Option#PATH_PATTERN} option since query component delimiter {@code ?} is conflicting with path pattern character {@code ?}.
	 * </p>
	 * 
	 * <p>
	 * Note that the different URI components composing the specified request-target are decoded individually after parsing the initial parsing of the request-target.
	 * </p>
	 *
	 * @param requestTarget     a request-target
	 * @param requestTargetForm the form of the request-target
	 * @param options           a list of options
	 *
	 * @return a URI builder
	 *
	 * @throws URIBuilderException if option {@link URIs.Option#PATH_PATTERN} is specified which is incompatible with origin-form request
	 */
	public static URIBuilder uri(String requestTarget, RequestTargetForm requestTargetForm, URIs.Option... options) throws URIBuilderException {
		return new GenericURIBuilder(requestTarget, requestTargetForm, true, Charsets.DEFAULT, options);
	}

	/**
	 * <p>
	 * Creates a URI builder from the specified {@link URIs.RequestTargetForm#PATH_QUERY_FRAGMENT} request-target ignoring or not trailing slash with the specified options and default charset.
	 * </p>
	 *
	 * <p>
	 * The request-target corresponds to the origin form as defined by
	 * <a href="https://tools.ietf.org/html/rfc7230#section-5.3.1">RFC 7230 Section 5.3.1</a> with the addition of the fragment component, as such it may contain query and/or a fragment URI
	 * components.
	 * </p>
	 *
	 * <p>
	 * The origin-form request is incompatible with {@link URIs.Option#PATH_PATTERN} option since query component delimiter {@code ?} is conflicting with path pattern character {@code ?}.
	 * </p>
	 * 
	 * <p>
	 * Note that the different URI components composing the specified request-target are decoded individually after parsing the initial parsing of the request-target.
	 * </p>
	 *
	 * @param requestTarget       an extended origin-form request-target
	 * @param ignoreTrailingSlash true to ignore trailing slash in the path
	 * @param options             a list of options
	 *
	 * @return a URI builder
	 *
	 * @throws URIBuilderException if option {@link URIs.Option#PATH_PATTERN} is specified which is incompatible with origin-form request
	 */
	public static URIBuilder uri(String requestTarget, boolean ignoreTrailingSlash, URIs.Option... options) throws URIBuilderException {
		return new GenericURIBuilder(requestTarget, URIs.RequestTargetForm.PATH_QUERY_FRAGMENT, ignoreTrailingSlash, Charsets.DEFAULT, options);
	}
	
	/**
	 * <p>
	 * Creates a URI builder from the specified request-target of the specified form ignoring or not trailing slash with the specified options and default charset.
	 * </p>
	 * 
	 * <p>
	 * The request-target must be of the specified form as defined by <a href="https://tools.ietf.org/html/rfc7230#section-5.3">RFC 7230 Section 5.3</a>.
	 * </p>
	 * 
	 * <p>
	 * The origin-form request is incompatible with {@link URIs.Option#PATH_PATTERN} option since query component delimiter {@code ?} is conflicting with path pattern character {@code ?}.
	 * </p>
	 * 
	 * <p>
	 * Note that the different URI components composing the specified request-target are decoded individually after parsing the initial parsing of the request-target.
	 * </p>
	 * 
	 * @param requestTarget     a request-target
	 * @param requestTargetForm the form of the request-target
	 * @param ignoreTrailingSlash true to ignore trailing slash in the path
	 * @param options           a list of options
	 * 
	 * @return a URI builder
	 * 
	 * @throws URIBuilderException if option {@link URIs.Option#PATH_PATTERN} is specified which is incompatible with origin-form request
	 */
	public static URIBuilder uri(String requestTarget, RequestTargetForm requestTargetForm, boolean ignoreTrailingSlash, URIs.Option... options) throws URIBuilderException {
		return new GenericURIBuilder(requestTarget, requestTargetForm, ignoreTrailingSlash, Charsets.DEFAULT, options);
	}

	/**
	 * <p>
	 * Creates a URI builder from the specified {@link URIs.RequestTargetForm#PATH_QUERY_FRAGMENT} request-target ignoring or not trailing slash with the specified options and default charset.
	 * </p>
	 *
	 * <p>
	 * The request-target corresponds to the origin form as defined by
	 * <a href="https://tools.ietf.org/html/rfc7230#section-5.3.1">RFC 7230 Section 5.3.1</a> with the addition of the fragment component, as such it may contain query and/or a fragment URI
	 * components.
	 * </p>
	 *
	 * <p>
	 * The origin-form request is incompatible with {@link URIs.Option#PATH_PATTERN} option since query component delimiter {@code ?} is conflicting with path pattern character {@code ?}.
	 * </p>
	 * 
	 * <p>
	 * Note that the different URI components composing the specified request-target are decoded individually after parsing the initial parsing of the request-target.
	 * </p>
	 *
	 * @param requestTarget       an extended origin-form request-target
	 * @param ignoreTrailingSlash true to ignore trailing slash in the path
	 * @param charset             a charset
	 * @param options             a list of options
	 *
	 * @return a URI builder
	 *
	 * @throws URIBuilderException if option {@link URIs.Option#PATH_PATTERN} is specified which is incompatible with origin-form request
	 */
	public static URIBuilder uri(String requestTarget, boolean ignoreTrailingSlash, Charset charset, URIs.Option... options) throws URIBuilderException {
		return new GenericURIBuilder(requestTarget, URIs.RequestTargetForm.PATH_QUERY_FRAGMENT, ignoreTrailingSlash, charset, options);
	}
	
	/**
	 * <p>
	 * Creates a URI builder from the specified {@link URIs.RequestTargetForm#PATH_QUERY_FRAGMENT} request-target ignoring or not trailing slash with the specified options and default charset.
	 * </p>
	 *
	 * <p>
	 * The request-target must be of the specified form as defined by <a href="https://tools.ietf.org/html/rfc7230#section-5.3">RFC 7230 Section 5.3</a>.
	 * </p>
	 *
	 * <p>
	 * The origin-form request is incompatible with {@link URIs.Option#PATH_PATTERN} option since query component delimiter {@code ?} is conflicting with path pattern character {@code ?}.
	 * </p>
	 * 
	 * <p>
	 * Note that the different URI components composing the specified request-target are decoded individually after parsing the initial parsing of the request-target.
	 * </p>
	 *
	 * @param requestTarget       a request-target
	 * @param requestTargetForm   the form of the request-target
	 * @param ignoreTrailingSlash true to ignore trailing slash in the path
	 * @param charset             a charset
	 * @param options             a list of options
	 *
	 * @return a URI builder
	 *
	 * @throws URIBuilderException if option {@link URIs.Option#PATH_PATTERN} is specified which is incompatible with origin-form request
	 */
	public static URIBuilder uri(String requestTarget, RequestTargetForm requestTargetForm, boolean ignoreTrailingSlash, Charset charset, URIs.Option... options) throws URIBuilderException {
		return new GenericURIBuilder(requestTarget, requestTargetForm, ignoreTrailingSlash, charset, options);
	}

	/**
	 * <p>
	 * Creates a URI builder from the specified URI ignoring trailing slash with the specified options and default charset.
	 * </p>
	 *
	 * @param uri     a URI
	 * @param options a list of options
	 *
	 * @return a URI builder
	 */
	public static URIBuilder uri(URI uri, URIs.Option... options) {
		return new GenericURIBuilder(uri, true, Charsets.DEFAULT, options);
	}

	/**
	 * <p>
	 * Creates a URI builder from the specified URI ignoring or not trailing slash with the specified options and default charset.
	 * </p>
	 *
	 * @param uri                 a URI
	 * @param ignoreTrailingSlash true to ignore trailing slash in the path
	 * @param options             a list of options
	 *
	 * @return a URI builder
	 */
	public static URIBuilder uri(URI uri, boolean ignoreTrailingSlash, URIs.Option... options) {
		return new GenericURIBuilder(uri, ignoreTrailingSlash, Charsets.DEFAULT, options);
	}

	/**
	 * <p>
	 * Creates a URI builder from the specified path ignoring or not trailing slash with the specified options and default charset.
	 * </p>
	 *
	 * @param uri                 a URI
	 * @param ignoreTrailingSlash true to ignore trailing slash in the path
	 * @param charset             a charset
	 * @param options             a list of options
	 *
	 * @return a URI builder
	 */
	public static URIBuilder uri(URI uri, boolean ignoreTrailingSlash, Charset charset, URIs.Option... options) {
		return new GenericURIBuilder(uri, ignoreTrailingSlash, charset, options);
	}

	/**
	 * <p>
	 * Decodes a percent encoded URI component as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-2.1">RFC 3986 Section 2.1</a>.
	 * </p>
	 *
	 * @param component the URI component to decode
	 * @param charset   a charset
	 *
	 * @return a decoded component
	 * 
	 * @throws URIBuilderException if there was an error decoding the component
	 */
	static String decodeURIComponent(String component, Charset charset) throws URIBuilderException {
		Objects.requireNonNull(charset, "charset");
		boolean needToChange = false;
		int numChars = component.length();
		StringBuilder result = new StringBuilder(numChars > 500 ? numChars / 2 : numChars);

		int i = 0;
		while (i < numChars) {
			char nextByte = component.charAt(i);
			if (nextByte == '%') {
				try {
					byte[] bytes = new byte[(numChars - i) / 3];
					// (numChars-i)/3 is an upper bound for the number
					// of remaining bytes
					int pos = 0;

					while (i + 2 < numChars && nextByte == '%') {
						int v = Integer.parseInt(component, i + 1, i + 3, 16);
						if (v < 0) {
							throw new URIBuilderException(
									"Illegal hex characters in escape (%) pattern - negative value");
						}
						bytes[pos++] = (byte) v;
						i += 3;
						if (i < numChars) {
							nextByte = component.charAt(i);
						}
					}

					// A trailing, incomplete byte encoding such as
					// "%x" will cause an exception to be thrown
					if (i < numChars && nextByte == '%') {
						throw new URIBuilderException("Incomplete trailing escape (%) pattern");
					}
					result.append(new String(bytes, 0, pos, charset));
				} catch (NumberFormatException e) {
					throw new URIBuilderException("Illegal hex characters in escape (%) pattern - " + e.getMessage());
				}
				needToChange = true;
			} else {
				result.append(nextByte);
				i++;
			}
		}
		return (needToChange ? result.toString() : component);
	}

	/**
	 * <p>
	 * Percent encodes a URI component as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-2.1">RFC 3986 Section 2.1</a> escaping the characters matching the specified escaped characters predicate.
	 * </p>
	 *
	 * @param component         the URI component to decode
	 * @param escapedCharacters an escaped characters predicate
	 * @param charset           a charset
	 *
	 * @return an encoded URI component if there was an error encoding the component
	 */
	static String encodeURIComponent(String component, Predicate<Integer> escapedCharacters, Charset charset) throws URIBuilderException {
		if(escapedCharacters == null) {
			return component;
		}
		Objects.requireNonNull(charset, "charset");
		if(StringUtils.isEmpty(component)) {
			return component;
		}
		byte[] bytes = component.getBytes(charset);
		ByteArrayOutputStream encodedOutput = null;

		for(int i = 0; i < bytes.length; i++) {
			byte b = bytes[i];

			if(!escapedCharacters.test((int) b)) {
				if (encodedOutput != null) {
					encodedOutput.write(b);
				}
			} 
			else {
				if(encodedOutput == null) {
					encodedOutput = new ByteArrayOutputStream(bytes.length);
					encodedOutput.write(bytes, 0, i);
				}
				encodedOutput.write('%');
				char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
				char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
				encodedOutput.write(hex1);
				encodedOutput.write(hex2);
			}
		}

		if(encodedOutput != null) {
			return encodedOutput.toString(charset);
		}
		return component;
	}
	
	/**
	 * <p>
	 * Percent encodes a query parameter as defined by <a href="https://tools.ietf.org/html/rfc3986#section-2.1">RFC 3986 Section 2.1</a> escaping non-query characters as defined by
	 * <a href="https://datatracker.ietf.org/doc/html/rfc3986#section-3.4">RFC 3986 Section 3.4</a>.
	 * </p>
	 *
	 * @param name    the parameter name to encode
	 * @param value   the parameter value to encode
	 * @param charset a charset
	 *
	 * @return an encoded query parameter
	 */
	public static String encodeQueryParameter(String name, String value, Charset charset) {
		return encodeURIComponent(name, QueryParameterComponent.ESCAPED_CHARACTERS, charset) + "=" + encodeURIComponent(value, QueryParameterComponent.ESCAPED_CHARACTERS, charset);
	}
	
	/**
	 * <p>
	 * Percent encodes a query string as defined by <a href="https://tools.ietf.org/html/rfc3986#section-2.1">RFC 3986 Section 2.1</a> escaping non-query characters as defined by
	 * <a href="https://datatracker.ietf.org/doc/html/rfc3986#section-3.4">RFC 3986 Section 3.4</a>.
	 * </p>
	 *
	 * @param query    the query to encode
	 * @param charset a charset
	 *
	 * @return an encoded query
	 */
	public static String encodeQuery(String query, Charset charset) {
		return encodeURIComponent(query, QueryComponent.ESCAPED_CHARACTERS, charset);
	}
	
	/**
	 * <p>
	 * Percent encodes a path segment as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-2.1">RFC 3986 Section 2.1</a> escaping non-segment characters (including slash) as defined by
	 * <a href="https://datatracker.ietf.org/doc/html/rfc3986#section-3.3">RFC 3986 Section 3.3</a>.
	 * </p>
	 *
	 * @param component the path segment to encode
	 * @param charset   a charset
	 *
	 * @return an encoded path segment
	 */
	public static String encodeSegment(String component, Charset charset) {
		return encodeURIComponent(component, SegmentComponent.ESCAPED_CHARACTERS_NO_SLASH, charset);
	}

	/**
	 * <p>
	 * Checks that the specified component is valid against the specified allowed characters predicate.
	 * </p>
	 *
	 * @param component         the component to check
	 * @param allowedCharacters an allowed character predicate
	 * @param charset           a charset
	 *
	 * @return the component if it is valid
	 * 
	 * @throws URIBuilderException if the component is invalid
	 */
	static String checkURIComponent(String component, Predicate<Integer> allowedCharacters, Charset charset) throws URIBuilderException {
		if(allowedCharacters == null) {
			return component;
		}
		Objects.requireNonNull(charset, "charset");
		byte[] bytes = component.getBytes(charset);
		for(byte b : bytes) {
			if(!allowedCharacters.test((int) b)) {
				throw new URIBuilderException("Invalid character " + (char) b + " found in URI component");
			}
		}
		return component;
	}
	
	/**
	 * URI parameter extract "default" status.
	 */
	private static final byte STATUS_DEFAULT = 0;
	/**
	 * URI parameter extract "in name" status.
	 */
	private static final byte STATUS_IN_NAME = 1;
	/**
	 * URI parameter extract "in pattern" status.
	 */
	private static final byte STATUS_IN_PATTERN = 2;
	
	/**
	 * <p>
	 * Scans the specified URI component in order to extracts parameters while a break predicate is not matched.
	 * </p>
	 *
	 * <p>
	 * URI parameters are extracted from the specified value when a parameter handler is specified.
	 * </p>
	 *
	 * <p>
	 * The break predicate is used to stop scanning when a particular index is reached or a particular character is matched outside parameter definitions when parameters are extracted.
	 * </p>
	 *
	 * @param component         the component value to scan
	 * @param allowedCharacters the allowed characters predicate
	 * @param charset           the charset
	 * @param parameterHandler  the parameter handler, null to disable parameter extraction
	 * @param breakPredicate    the break predicate, null to never break the scan
	 *
	 * @throws URIBuilderException if an invalid parameter is found
	 */
	static void scanURIComponent(String component, Predicate<Integer> allowedCharacters, Charset charset, Consumer<URIParameter> parameterHandler, BiPredicate<Integer, Byte> breakPredicate) throws URIBuilderException {
		if(StringUtils.isNotBlank(component)) {
			boolean escape = false;
			byte patternBraceDepth = 0;
			
			byte[] valueBytes = component.getBytes(charset);
			byte status = STATUS_DEFAULT;
			int parameterIndex = -1;
			int parameterPatternIndex = -1;
			loop:
			for(int i=0;i<valueBytes.length;i++) {
				byte nextByte = valueBytes[i];
				
				if(nextByte == '\\') {
					escape = !escape;
					continue;
				}
				
				switch(status) {
					case STATUS_IN_NAME: {
						if(nextByte == '}') {
							String parameterName = new String(valueBytes, parameterIndex + 1, i - (parameterIndex + 1));
							parameterHandler.accept(new URIParameter(parameterIndex, i - parameterIndex + 1, parameterName, allowedCharacters, charset));
							parameterIndex = parameterPatternIndex = -1;
							status = STATUS_DEFAULT;
						}
						else if(nextByte == ':') {
							parameterPatternIndex = i;
							status = STATUS_IN_PATTERN;
						}
						else if( (i == parameterIndex + 1 && !Character.isJavaIdentifierStart(nextByte)) || (i > parameterIndex + 1 && !Character.isJavaIdentifierPart(nextByte))) {
							throw new URIBuilderException("Invalid parameter name at position " + parameterIndex + ": " + component);
						}
						break;
					}
					case STATUS_IN_PATTERN: {
						if(!escape) {
							if(nextByte == '{') {
								patternBraceDepth++;
							}
							else if(nextByte == '}') {
								if(patternBraceDepth == 0) {
									// close parameter
									String parameterName = new String(valueBytes, parameterIndex + 1, parameterPatternIndex - (parameterIndex + 1));
									String parameterPattern = new String(valueBytes, parameterPatternIndex + 1, i - (parameterPatternIndex + 1));
									parameterHandler.accept(new URIParameter(parameterIndex, i - parameterIndex + 1, parameterName, parameterPattern, allowedCharacters, charset));
									parameterIndex = parameterPatternIndex = -1;
									status = STATUS_DEFAULT;
								}
								else {
									patternBraceDepth--;
								}
							}
						}
						break;
					}
					default: {
						if(nextByte == '{' && parameterHandler != null) {
							parameterIndex = i;
							status = STATUS_IN_NAME;
						}
						else if(allowedCharacters != null && !allowedCharacters.test((int)nextByte)) {
							throw new URIBuilderException("Invalid character " + (char)nextByte + " found in URI component");
						}
						else if(breakPredicate != null && breakPredicate.test(i, nextByte)) {
							break loop;
						}
						break;
					}
				}
				escape = false;
			}
		}
	}
}

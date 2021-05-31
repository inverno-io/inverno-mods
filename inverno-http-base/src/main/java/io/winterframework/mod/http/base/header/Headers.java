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
package io.inverno.mod.http.base.header;

import java.nio.charset.Charset;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.inverno.mod.http.base.header.Headers.Accept.MediaRange;
import io.inverno.mod.http.base.internal.header.AcceptCodec;
import io.inverno.mod.http.base.internal.header.AcceptLanguageCodec;

/**
 * <p>
 * Defines standard HTTP header types.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * 
 * @see Header
 */
public final class Headers {

	/**
	 * <a href="https://datatracker.ietf.org/doc/html/rfc5322#section-3.3">RFC 5322 Section 3.3</a>
	 */
	public static final DateTimeFormatter FORMATTER_RFC_1123_DATE_TIME = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId.of("GMT"));
	
	/* Header Names */
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.2</a>
	 */
	public static final String NAME_ACCEPT = "accept";
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-5.3.4">RFC 7231 Section 5.3.4</a>
	 */
	public static final String NAME_ACCEPT_ENCODING = "accept-encoding";
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-5.3.5">RFC 7231 Section 5.3.5</a>
	 */
	public static final String NAME_ACCEPT_LANGUAGE = "accept-language";
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-7.4.1">RFC 7231 Section 7.4.1</a>
	 */
	public static final String NAME_ALLOW = "allow";
	/**
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7234#section-5.2">RFC 7234 Section 5.2</a>
	 */
	public static final String NAME_CACHE_CONTROL = "cache-control";
	/**
	 * <a href="https://tools.ietf.org/html/rfc7230#section-6.1">RFC 7230 Section 6.1</a>
	 */
	public static final String NAME_CONNECTION = "connection";
	/**
	 * <a href="https://tools.ietf.org/html/rfc6266">RFC 6266</a>
	 */
	public static final String NAME_CONTENT_DISPOSITION = "content-disposition";
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-3.1.2.2">RFC 7231 Section 3.1.2.2</a>
	 */
	public static final String NAME_CONTENT_ENCODING = "content-encoding";
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-3.1.1.5">RFC 7231 Section 3.1.1.5</a>
	 */
	public static final String NAME_CONTENT_TYPE = "content-type";
	/**
	 * <a href="https://tools.ietf.org/html/rfc7230#section-3.3.2">RFC 7230 Section 3.3.2</a>
	 */
	public static final String NAME_CONTENT_LENGTH = "content-length";
	/**
	 * <a href="https://tools.ietf.org/html/rfc6265#section-4.2">RFC 6265 Section 4.2</a>
	 */
	public static final String NAME_COOKIE = "cookie";
	/**
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7231#section-7.1.1.2">RFC 7231 Section 7.1.1.2</a>
	 */
	public static final String NAME_DATE = "date";
	/**
	 * <a href="https://tools.ietf.org/html/rfc7230#section-5.4">RFC 7230 Section 5.4</a>
	 */
	public static final String NAME_HOST = "host";
	/**
	 * <a href="https://tools.ietf.org/html/rfc7540#section-3.2.1">RFC 7540 Section 3.2.1</a>
	 */
	public static final String NAME_HTTP2_SETTINGS = "http2-settings";
	/**
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7232#section-2.2">RFC 7232 Section 2.2</a>
	 */
	public static final String NAME_LAST_MODIFIED = "last-modified";
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-7.1.2">RFC 7231 Section 7.1.2</a>
	 */
	public static final String NAME_LOCATION = "location";
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-5.5.2">RFC 7231 Section 5.5.2</a>
	 */
	public static final String NAME_REFERER = "referer";
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-7.1.3">RFC 7231 Section 7.1.3</a>
	 */
	public static final String NAME_RETRY_AFTER = "retry-after";
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-7.4.2">RFC 7231 Section 7.4.2</a>
	 */
	public static final String NAME_SERVER = "server";
	/**
	 * <a href="https://tools.ietf.org/html/rfc6265#section-4.1">RFC 6265 Section 4.1</a>
	 */
	public static final String NAME_SET_COOKIE = "set-cookie";
	/**
	 * <a href="https://tools.ietf.org/html/rfc7230#section-4.3">RFC 7230 Section 4.3</a>
	 */
	public static final String NAME_TE = "te";
	/**
	 * <a href="https://tools.ietf.org/html/rfc7230#section-4.4">RFC 7230 Section 4.4</a>
	 */
	public static final String NAME_TRAILER = "trailer";
	/**
	 * <a href="https://tools.ietf.org/html/rfc7230#section-3.3.1">RFC 7230 Section 3.3.1</a>
	 */
	public static final String NAME_TRANSFER_ENCODING = "transfer-encoding";
	/**
	 * <a href="https://tools.ietf.org/html/rfc7230#section-6.7">RFC 7230 Section 6.7</a>
	 */
	public static final String NAME_UPGRADE = "upgrade";
	
	/* HTTP/2 pseudo headers */
	/**
	 * <a href="https://tools.ietf.org/html/rfc7540#section-8.1.2.3">RFC 7540 Section 8.1.2.3</a>
	 */
	public static final String NAME_PSEUDO_AUTHORITY = ":authority";
	/**
	 * <a href="https://tools.ietf.org/html/rfc7540#section-8.1.2.3">RFC 7540 Section 8.1.2.3</a>
	 */
	public static final String NAME_PSEUDO_METHOD = ":method";
	/**
	 * <a href="https://tools.ietf.org/html/rfc7540#section-8.1.2.3">RFC 7540 Section 8.1.2.3</a>
	 */
	public static final String NAME_PSEUDO_PATH = ":path";
	/**
	 * <a href="https://tools.ietf.org/html/rfc7540#section-8.1.2.3">RFC 7540 Section 8.1.2.3</a>
	 */
	public static final String NAME_PSEUDO_SCHEME = ":scheme";
	/**
	 * <a href="https://tools.ietf.org/html/rfc7540#section-8.1.2.4">RFC 7540 Section 8.1.2.4</a>
	 */
	public static final String NAME_PSEUDO_STATUS = ":status";
	
	/* Header Values */
	/**
	 * <a href="https://tools.ietf.org/html/rfc7230#section-3.3.1">RFC 7230 Section 3.3.1</a>
	 */
	public static final String VALUE_CHUNKED = "chunked";
	/**
	 * <a href="https://tools.ietf.org/html/rfc7230#section-6.1">RFC 7230 Section 6.1</a>
	 */
	public static final String VALUE_CLOSE = "close";
	/**
	 * deflate content encoding as defined by <a href="https://www.ietf.org/rfc/rfc1951.txt">RFC 1951</a>
	 */
	public static final String VALUE_DEFLATE = "deflate";
	/**
	 * gzip content encoding as defined by <a href="https://tools.ietf.org/html/rfc1952">RFC 1952</a>
	 */
	public static final String VALUE_GZIP = "gzip";
	/**
	 * <a href="https://tools.ietf.org/html/rfc7230#section-4.3">RFC 7230 Section 4.3</a>
	 */
	public static final String VALUE_TRAILERS = "trailers";
	/**
	 * x-deflate content encoding as defined by <a href="https://www.ietf.org/rfc/rfc1951.txt">RFC 1951</a>
	 */
	public static final String VALUE_X_DEFLATE = "x-deflate";
	/**
	 * x-gzip content encoding as defined by <a href="https://tools.ietf.org/html/rfc1952">RFC 1952</a>
	 */
	public static final String VALUE_X_GZIP = "x-gzip";
	
	private Headers() {}
	
	/**
	 * <p>
	 * Content-type HTTP header as defined by
	 * <a href="https://tools.ietf.org/html/rfc7231#section-3.1.1.5">RFC 7231
	 * Section 3.1.1.5</a>.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	public static interface ContentType extends Header {
		
		/**
		 * Boundary parameter
		 */
		public static final String BOUNDARY = "boundary";
		
		/**
		 * Charset parameter
		 */
		public static final String CHARSET = "charset";
		
		/**
		 * <p>
		 * Returns the media type composed of type and sub-type parts without parameters.
		 * </p>
		 * 
		 * @return a raw media type
		 */
		String getMediaType();
		
		/**
		 * <p>
		 * Returns the type part.
		 * </p>
		 * 
		 * @return the type part
		 */
		String getType();
		
		/**
		 * <p>
		 * Returns the sub-type part.
		 * </p>
		 * 
		 * @return the sub-type part
		 */
		String getSubType();
		
		/**
		 * <p>
		 * Returns the boundary.
		 * </p>
		 * 
		 * @return the boundary or null
		 */
		String getBoundary();

		/**
		 * <p>
		 * Returns the charset.
		 * </p>
		 * 
		 * @return the charset of null
		 */
		Charset getCharset();
		
		/**
		 * <p>
		 * Returns the content type parameters.
		 * </p>
		 * 
		 * @return a map of parameters
		 */
		Map<String, String> getParameters();
		
		/**
		 * <p>
		 * Converts the content type to a media range.
		 * </p>
		 * 
		 * @return the media range corresponding to the content type
		 */
		Accept.MediaRange toMediaRange();
	}
	
	/**
	 * <p>
	 * Content-disposition HTTP header as defined by
	 * <a href="https://tools.ietf.org/html/rfc6266#section-4.1">RFC 6266 Section
	 * 4.1</a>.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	public static interface ContentDisposition extends Header {

		/**
		 * Name parameter
		 */
		public static final String PART_NAME = "name";
		/**
		 * Filename parameter
		 */
		public static final String FILENAME = "filename";
		/**
		 * Creation-date parameter
		 */
		public static final String CREATION_DATE = "creation-date";
		/**
		 * Modification-date parameter
		 */
		public static final String MODIFICATION_DATE = "modification-date";
		/**
		 * Read-state parameter
		 */
		public static final String READ_DATE = "read-date";
		/**
		 * Size parameter
		 */
		public static final String SIZE = "size";
		
		/**
		 * <p>
		 * Returns the disposition type.
		 * </p>
		 * 
		 * @return the disposition type
		 */
		String getDispositionType();

		/**
		 * <p>
		 * Returns the part name.
		 * </p>
		 * 
		 * @return the name or null
		 */
		String getPartName();

		/**
		 * <p>
		 * Returns the part file name.
		 * </p>
		 * 
		 * @return the file name or null
		 */
		String getFilename();

		/**
		 * <p>
		 * Returns the part creation datetime.
		 * </p>
		 * 
		 * @return the creation datetime or null
		 */
		String getCreationDateTime();

		/**
		 * <p>
		 * Returns the part modification datetime.
		 * </p>
		 * 
		 * @return the modification datetime or null
		 */
		String getModificationDatetime();

		/**
		 * <p>
		 * Returns the part read datetime.
		 * </p>
		 * 
		 * @return the read datetime or null
		 */
		String getReadDateTime();

		/**
		 * <p>
		 * Returns the part size.
		 * </p>
		 * 
		 * @return the size or null
		 */
		Integer getSize();
	}
	
	/**
	 * <p>
	 * Set-cookie HTTP header as defined by
	 * <a href="https://tools.ietf.org/html/rfc6265#section-4.1">RFC 6265 Section
	 * 4.1</a>.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	public static interface SetCookie extends Header {
		
		/**
		 * Expires parameter
		 */
		public static final String EXPIRES = "Expires";
		/**
		 * Max-Age parameter
		 */
		public static final String MAX_AGE = "Max-Age";
		/**
		 * Domain parameter
		 */
		public static final String DOMAIN = "Domain";
		/**
		 * Path parameter
		 */
		public static final String PATH = "Path";
		/**
		 * Secure parameter
		 */
		public static final String SECURE = "Secure";
		/**
		 * HttpOnly parameter
		 */
		public static final String HTTPONLY = "HttpOnly";
		
		/**
		 * <p>
		 * Returns the cookie name.
		 * </p>
		 * 
		 * @return the cookie name or null
		 */
		String getName();

		/**
		 * <p>
		 * Returns the cookie value.
		 * </p>
		 * 
		 * @return the cookie value or null
		 */
		String getValue();

		/**
		 * <p>
		 * Returns the cookie expires value.
		 * </p>
		 * 
		 * @return the cookie expires value or null
		 */
		String getExpires();

		/**
		 * <p>
		 * Returns the cookie max age.
		 * </p>
		 * 
		 * @return the cookie max age or null
		 */
		Integer getMaxAge();

		/**
		 * <p>
		 * Returns the cookie max age.
		 * </p>
		 * 
		 * @return the cookie max age or null
		 */
		String getDomain();

		/**
		 * <p>
		 * Returns the cookie path.
		 * </p>
		 * 
		 * @return the cookie path or null
		 */
		String getPath();

		/**
		 * <p>
		 * Returns the cookie secure flag.
		 * </p>
		 * 
		 * @return the cookie secure flag or null
		 */
		Boolean isSecure();

		/**
		 * <p>
		 * Returns the cookie HTTP only flag.
		 * </p>
		 * 
		 * @return the cookie HTTP only flag or null
		 */
		Boolean isHttpOnly();
	}
	
	/**
	 * <p>
	 * Cookie HTTP header as defined by
	 * <a href="https://tools.ietf.org/html/rfc6265#section-4.2">RFC 6265 Section
	 * 4.2</a>.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	public static interface Cookie extends Header {
		
		/**
		 * <p>
		 * Returns the list of cookie pairs.
		 * </p>
		 * 
		 * @return a map associating a cookie name to a list of cookie parameter
		 */
		public Map<String, List<CookieParameter>> getPairs();
	}
	
	/**
	 * <p>
	 * Accept HTTP header as defined by
	 * <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section
	 * 5.3.2</a>.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	public static interface Accept extends Header {
		
		/**
		 * Accept all header: *{@literal /*}
		 */
		public static final Headers.Accept ALL = new AcceptCodec.Accept(null);
		
		/**
		 * <p>
		 * Returns the list of media ranges defined in the accept header.
		 * </p>
		 * 
		 * @return a list of media ranges
		 */
		List<MediaRange> getMediaRanges();
		
		/**
		 * <p>
		 * Returns the content type in the specified collection that best matches the
		 * accept header.
		 * </p>
		 * 
		 * @param contentTypes a collection of content types
		 * 
		 * @return an optional returning an accept match or an empty optional if no
		 *         match was found
		 */
		default Optional<AcceptMatch<MediaRange, Headers.ContentType>> findBestMatch(Collection<Headers.ContentType> contentTypes) {
			return this.findBestMatch(contentTypes, Function.identity());
		}
		
		/**
		 * <p>
		 * Returns the item from the specified collection whose content type best
		 * matches the accept header.
		 * </p>
		 * 
		 * @param <T>                  the type of the item
		 * @param items                a collection items
		 * @param contentTypeExtractor a function that extracts the content type of an
		 *                             item
		 * 
		 * @return an optional returning an accept match or an empty optional if no
		 *         match was found
		 */
		default <T> Optional<AcceptMatch<MediaRange, T>> findBestMatch(Collection<T> items, Function<T, Headers.ContentType> contentTypeExtractor) {
			for(MediaRange mediaRange : this.getMediaRanges()) {
				for(T item : items) {
					if(mediaRange.matches(contentTypeExtractor.apply(item))) {
						return Optional.of(new AcceptMatch<>(mediaRange, item));
					}
				}
			}
			return Optional.empty();
		}
		
		/**
		 * <p>
		 * Returns all the content types in the specified collection that matches the
		 * accept header sorted from best to worst.
		 * </p>
		 * 
		 * @param contentTypes a collection of content types
		 * 
		 * @return a collection of accept matches
		 */
		default Collection<AcceptMatch<MediaRange, Headers.ContentType>> findAllMatch(Collection<Headers.ContentType> contentTypes) {
			return this.findAllMatch(contentTypes, Function.identity());
		}
		
		/**
		 * <p>
		 * Returns all the items in the specified collection whose content type matches
		 * the accept header sorted from best to worst.
		 * </p>
		 * 
		 * @param <T>                  the type of the item
		 * @param items                a collection of items
		 * @param contentTypeExtractor a function that extracts the content type of an
		 *                             item
		 * 
		 * @return a collection of accept matches
		 */
		default <T> Collection<AcceptMatch<MediaRange, T>> findAllMatch(Collection<T> items, Function<T, Headers.ContentType> contentTypeExtractor) {
			List<AcceptMatch<MediaRange, T>> result = new ArrayList<>();
			// This works because items are content type ie. with no wild cards
			for(MediaRange mediaRange : this.getMediaRanges()) {
				for(T item : items) {
					if(mediaRange.matches(contentTypeExtractor.apply(item))) {
						result.add(new AcceptMatch<>(mediaRange, item));
					}
				}
			}
			return result;
		}
		
		/**
		 * <p>
		 * Merges multiple accept headers into one.
		 * </p>
		 * 
		 * @param acceptHeaders a list of accept headers.
		 * 
		 * @return an optional returning an accept header or an empty optional if the
		 *         specified list was null or empty
		 */
		static Optional<Accept> merge(List<Accept> acceptHeaders) {
			if(acceptHeaders == null || acceptHeaders.isEmpty()) {
				return Optional.empty();
			}
			else if(acceptHeaders.size() == 1) {
				return Optional.of(acceptHeaders.get(0));
			}
			else {
				return Optional.of(new Accept() {

					@Override
					public String getHeaderName() {
						return Headers.NAME_ACCEPT;
					}

					@Override
					public String getHeaderValue() {
						return acceptHeaders.stream().map(Accept::getHeaderValue).collect(Collectors.joining(", "));
					}

					@Override
					public List<MediaRange> getMediaRanges() {
						return acceptHeaders.stream()
							.flatMap(accept -> accept.getMediaRanges().stream())
							.distinct()
							.sorted(MediaRange.COMPARATOR)
							.collect(Collectors.toList());
					}
				});
			}
		}
		
		/**
		 * <p>
		 * Accept HTTP header media range as defined by
		 * <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section
		 * 5.3.2</a>.
		 * </p>
		 * 
		 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
		 * @since 1.0
		 */
		public static interface MediaRange {
			
			/**
			 * The media range comparator based on media range scores.
			 */
			public static final Comparator<MediaRange> COMPARATOR = (r1, r2) -> r2.getScore() - r1.getScore();
			
			/**
			 * <p>
			 * Returns the media range raw value.
			 * </p>
			 * 
			 * @return the raw media range
			 */
			String getMediaType();
			
			/**
			 * <p>
			 * Returns the media range type.
			 * </p>
			 * 
			 * @return the media range type
			 */
			String getType();
			
			/**
			 * <p>
			 * Returns the media range sub-type.
			 * </p>
			 * 
			 * @return the media range sub-type
			 */
			String getSubType();
			
			/**
			 * <p>
			 * Returns the media range quality value as defined by
			 * <a href="https://tools.ietf.org/html/rfc7231#section-5.3.1">RFC 7231 Section
			 * 5.3.1</a>.
			 * </p>
			 * 
			 * @return the media range quality value
			 */
			float getWeight();
			
			/**
			 * <p>
			 * Returns media range parameters.
			 * </p>
			 * 
			 * @return the media range parameters
			 */
			Map<String, String> getParameters();
			
			/**
			 * <p>
			 * Determines whether the specified content type matches the media range.
			 * </p>
			 * 
			 * @param contentType the content type to test
			 * 
			 * @return true if the content type matches the range, false otherwise
			 */
			// When this range doesn't define any parameters, this method returns true as
			// soon as the media types are compatible.
			// The best match is then the most precise one.
			default boolean matches(Headers.ContentType contentType) {
				String requestType = contentType.getType();
				String requestSubType = contentType.getSubType();
				Map<String, String> requestParameters = contentType.getParameters();

				String consumeType = this.getType();
				String consumeSubType = this.getSubType();
				Map<String, String> consumeParameters = this.getParameters();

				if(requestType.equals("*")) {
					if(requestSubType.equals("*")) {
						// we can stop here: any match as long as parameters match
						return consumeParameters.isEmpty() || consumeParameters.equals(requestParameters);
					}
					else {
						return (consumeSubType.equals("*") || consumeSubType.equals(requestSubType)) && 
							(consumeParameters.isEmpty() || consumeParameters.equals(requestParameters));
					}
				}
				else {
					if(requestSubType.equals("*")) {
						return (consumeType.equals("*") || consumeType.equals(requestType)) && 
							(consumeParameters.isEmpty() || consumeParameters.equals(requestParameters));
					}
					else {
						return (consumeType.equals("*") || consumeType.equals(requestType)) && 
							(consumeSubType.equals("*") || consumeSubType.equals(requestSubType)) && 
							(consumeParameters.isEmpty() || consumeParameters.equals(requestParameters));
					}
				}
			}
			
			/**
			 * <p>
			 * Calculates and returns the score or the media range used for sorting.
			 * </p>
			 * 
			 * <p>
			 * The score is calculated by assigning a score to the media range part and add
			 * all:
			 * </p>
			 * 
			 * <ul>
			 * <li>the range quality value is multiplied by 1000</li>
			 * <li>*{@literal /}* is worth 0</li>
			 * <li>*{@literal /}x is worth 10</li>
			 * <li>x{@literal /}* is worth 20</li>
			 * <li>x{@literal /}x is worth 30</li>
			 * <li>a null parameter is worth 1</li>
			 * <li>a non-null parameter is worth 2</li>
			 * </ul>
			 * 
			 * @return the score of the media range
			 */
			default int getScore() {
				String type = this.getType();
				String subType = this.getSubType();
				Map<String, String> parameters = this.getParameters();
				float weight = this.getWeight();
				int score = 0;
				
				// 1. weight
				score += 1000 * weight;
				
				// 2. wildcards
				if(type.equals("*")) {
					if(subType.equals("*")) {
						// */*
						score += 0 * 10;
					}
					else {
						// */b
						score += 1 * 10;
					}
				}
				else {
					if(subType.equals("*")) {
						// a/*
						score += 2 * 10;
					}
					else {
						// a/b
						score += 3 * 10;
					}
				}
				
				// 3. parameters
				for(Entry<String, String> e : parameters.entrySet()) {
					if(e.getValue() == null) {
						score += 1 * 1;
					}
					else {
						score += 2 * 1;
					}
				}
				
				return score;
			}
			
			/**
			 * <p>
			 * Returns the first media range in the specified list that matches the
			 * specified content type.
			 * </p>
			 * 
			 * @param contentType a content type
			 * @param mediaRanges a list of media ranges
			 * 
			 * @return an optional returning the first match or an empty optional if no
			 *         match was found
			 */
			static Optional<AcceptMatch<MediaRange, Headers.ContentType>> findFirstMatch(Headers.ContentType contentType, List<MediaRange> mediaRanges) {
				return findFirstMatch(contentType, mediaRanges, Function.identity());
			}
			
			/**
			 * <p>
			 * Returns the first item in the specified collection whose media range matches
			 * the specified content type.
			 * </p>
			 * 
			 * @param <T>                 the type of the item
			 * @param contentType         a content type
			 * @param items               a collection of items
			 * @param mediaRangeExtractor a function that extracts the media type of an item
			 * 
			 * @return an optional returning the first match or an empty optional if no
			 *         match was found
			 */
			static <T> Optional<AcceptMatch<T, Headers.ContentType>> findFirstMatch(Headers.ContentType contentType, Collection<T> items, Function<T, MediaRange> mediaRangeExtractor) {
				String requestType = contentType.getType();
				String requestSubType = contentType.getSubType();
				Map<String, String> requestParameters = contentType.getParameters();

				return items.stream()
					.filter(item -> {
						MediaRange range = mediaRangeExtractor.apply(item);
						String type = range.getType();
						String subType = range.getSubType();
						Map<String, String> consumeParameters = range.getParameters();

						if(requestType.equals("*")) {
							if(requestSubType.equals("*")) {
								// we can stop here: any match as long as parameters match
								return consumeParameters.isEmpty() || consumeParameters.equals(requestParameters);
							}
							else {
								return (type.equals("*") || subType.equals(requestSubType)) && 
									(consumeParameters.isEmpty() || consumeParameters.equals(requestParameters));
							}
						}
						else {
							if(requestSubType.equals("*")) {
								return (type.equals("*") || type.equals(requestType)) && 
									(consumeParameters.isEmpty() || consumeParameters.equals(requestParameters));
							}
							else {
								return (type.equals("*") || type.equals(requestType)) && 
									(subType.equals("*") || subType.equals(requestSubType)) && 
									(consumeParameters.isEmpty() || consumeParameters.equals(requestParameters));
							}
						}
					})
					.findFirst()
					.map(item -> new AcceptMatch<>(item, contentType));
			}
		}
	}
	
	/**
	 * <p>
	 * An accept match respresents a match between a source item and a target item.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see Accept#findBestMatch(Collection)
	 * @see Accept#findBestMatch(Collection, Function)
	 * @see Accept#findAllMatch(Collection)
	 * @see Accept#findAllMatch(Collection, Function)
	 * @see MediaRange#findFirstMatch(ContentType, List)
	 * @see MediaRange#findFirstMatch(ContentType, Collection, Function)
	 * 
	 * @param <A> the source item
	 * @param <B> the target item
	 */
	public static class AcceptMatch<A, B> {
		
		private final A source;
		
		private final B target;
		
		private AcceptMatch(A source, B target) {
			this.source = source;
			this.target = target;
		}

		/**
		 * <p>
		 * Returns the source item.
		 * </p>
		 * 
		 * @return the source item
		 */
		public A getSource() {
			return this.source;
		}

		/**
		 * <p>
		 * Returns the target item.
		 * </p>
		 * 
		 * @return the target item
		 */
		public B getTarget() {
			return this.target;
		}
	}

	/**
	 * <p>
	 * Accept language match with a match score.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 *
	 * @param <A> the source item
	 * @param <B> the target item
	 */
	private static class AcceptLanguageMatch<A, B> extends AcceptMatch<A, B> {
		
		private final int score;
		
		private AcceptLanguageMatch(A source, B target, int score) {
			super(source, target);
			this.score = score;
		}
	
		/**
		 * <p>
		 * Returns the match score.
		 * </p>
		 * 
		 * @return the match score
		 */
		public int getScore() {
			return score;
		}
	}
	
	/**
	 * <p>
	 * Accept-language HTTP header as defined by
	 * <a href="https://tools.ietf.org/html/rfc7231#section-5.3.5">RFC 7231 Section
	 * 5.3.5</a>.
	 * </p>
	 * 
	 * https://tools.ietf.org/html/rfc4647#section-3.3.1
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	public static interface AcceptLanguage extends Header {
		
		/**
		 * Accept all header: *
		 */
		public static final Headers.AcceptLanguage ALL = new AcceptLanguageCodec.AcceptLanguage(null);
		
		/**
		 * <p>
		 * Returns the list of language ranges defined in the accept language header.
		 * </p>
		 * 
		 * @return a list of language ranges
		 */
		List<LanguageRange> getLanguageRanges();
		
		/**
		 * <p>
		 * Returns the language range in the specified collection that best matches the
		 * accept language header.
		 * </p>
		 * 
		 * @param languageRanges a collection of language ranges
		 * 
		 * @return an optional returning an accept match or an empty optional if no
		 *         match was found
		 */
		default Optional<AcceptMatch<LanguageRange, LanguageRange>> findBestMatch(Collection<Headers.AcceptLanguage.LanguageRange> languageRanges) {
			return this.findBestMatch(languageRanges, Function.identity());
		}
		
		/**
		 * <p>
		 * Returns the item from the specified collection whose language range best
		 * matches the accept language header.
		 * </p>
		 * 
		 * @param <T>                    the type of the item
		 * @param items                  a collection of items
		 * @param languageRangeExtractor a function that extracts the language range of
		 *                               an item
		 * 
		 * @return an optional returning an accept match or an empty optional if no
		 *         match was found
		 */
		default <T> Optional<AcceptMatch<LanguageRange,T>> findBestMatch(Collection<T> items, Function<T, Headers.AcceptLanguage.LanguageRange> languageRangeExtractor) {
			return this.findAllMatch(items, languageRangeExtractor).stream().findFirst();
		}
		
		/**
		 * <p>
		 * Returns all the language ranges in the specified collection that matches the
		 * accept language header sorted from best to worst.
		 * </p>
		 * 
		 * @param languageRanges a collection of language ranges
		 * 
		 * @return a collection of accept matches
		 */
		default Collection<AcceptMatch<LanguageRange, LanguageRange>> findAllMatch(Collection<Headers.AcceptLanguage.LanguageRange> languageRanges) {
			return this.findAllMatch(languageRanges, Function.identity());
		}
		
		/**
		 * <p>
		 * Returns all the items in the specified collection whose language range
		 * matches the accept language header sorted from best to worst.
		 * </p>
		 * 
		 * @param <T>                    the type of the item
		 * @param items                  a collection of items
		 * @param languageRangeExtractor a function that extracts the language range of
		 *                               an item
		 * 
		 * @return a collection of accept matches
		 */
		default <T> Collection<AcceptMatch<LanguageRange,T>> findAllMatch(Collection<T> items, Function<T, Headers.AcceptLanguage.LanguageRange> languageRangeExtractor) {
			return this.getLanguageRanges().stream()
				.flatMap(languageRange -> items.stream()
					.map(item -> {
						LanguageRange itemLanguageRange = languageRangeExtractor.apply(item);
						if(languageRange.matches(itemLanguageRange)) {
							int score;
							if(languageRange.getLanguageTag().equals(itemLanguageRange.getLanguageTag())) {
								// exact match
								score = 100000 + itemLanguageRange.getScore();
							}
							else {
								score = 10000 + itemLanguageRange.getScore();
							}
							return new AcceptLanguageMatch<>(languageRange, item, score);
						}
						return null;
					})
					.filter(Objects::nonNull)
				)
				.sorted((o1, o2) -> o2.getScore() - o1.getScore())
				.collect(Collectors.toList());
		}
		
		/**
		 * <p>
		 * Merges multiple accept language headers into one.
		 * </p>
		 * 
		 * @param acceptLanguageHeaders a list of accept language headers.
		 * 
		 * @return an optional returning an accept language header or an empty optional
		 *         if the specified list was null or empty
		 */
		static Optional<AcceptLanguage> merge(List<AcceptLanguage> acceptLanguageHeaders) {
			if(acceptLanguageHeaders.isEmpty()) {
				return Optional.empty();
			}
			else if(acceptLanguageHeaders.size() == 1) {
				return Optional.of(acceptLanguageHeaders.get(0));
			}
			else {
				return Optional.of(new AcceptLanguage() {

					@Override
					public String getHeaderName() {
						return Headers.NAME_ACCEPT_LANGUAGE;
					}

					@Override
					public String getHeaderValue() {
						return acceptLanguageHeaders.stream().map(AcceptLanguage::getHeaderValue).collect(Collectors.joining(", "));
					}

					@Override
					public List<LanguageRange> getLanguageRanges() {
						return acceptLanguageHeaders.stream()
							.flatMap(accept -> accept.getLanguageRanges().stream())
							.distinct()
							.sorted(LanguageRange.COMPARATOR)
							.collect(Collectors.toList());
					}
				});
			}
		}
		
		/**
		 * <p>
		 * Accept language HTTP header language range as defined by
		 * <a href="https://tools.ietf.org/html/rfc7231#section-5.3.5">RFC 7231 Section
		 * 5.3.5</a>.
		 * </p>
		 * 
		 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
		 * @since 1.0
		 */
		public static interface LanguageRange {
			
			/**
			 * The language range comparator based on language range scores.
			 */
			public static final Comparator<LanguageRange> COMPARATOR = (r1, r2) -> r2.getScore() - r1.getScore();
			
			/**
			 * <p>
			 * Returns the language range tag.
			 * </p>
			 * 
			 * @return the language range tag
			 */
			String getLanguageTag();
			
			/**
			 * <p>
			 * Returns the language range primary sub-tag.
			 * </p>
			 * 
			 * @return the language range primary sub-tag
			 */
			String getPrimarySubTag();

			/**
			 * <p>
			 * Returns the language range secondary sub-tag.
			 * </p>
			 * 
			 * @return the language range secondary sub-tag
			 */
			String getSecondarySubTag();
			
			/**
			 * <p>
			 * Returns the language range quality value as defined by
			 * <a href="https://tools.ietf.org/html/rfc7231#section-5.3.1">RFC 7231 Section
			 * 5.3.1</a>.
			 * </p>
			 * 
			 * @return the language range quality value
			 */
			float getWeight();
			
			/**
			 * <p>
			 * Determines whether the specified language range matches the language range.
			 * </p>
			 * 
			 * @param languageRange the language range to test
			 * 
			 * @return true if the language range matches the range, false otherwise
			 */
			default boolean matches(LanguageRange languageRange) {
				String requestPrimarySubTag = languageRange.getPrimarySubTag();
				String requestSecondarySubTag = languageRange.getSecondarySubTag();
				
				String primarySubType = this.getPrimarySubTag();
				String secondarySubType = this.getSecondarySubTag();

				if(requestPrimarySubTag.equals("*")) {
					// *
					return true;
				}
				else {
					if(requestSecondarySubTag == null) {
						// xx
						return (primarySubType.equals("*") || primarySubType.equals(requestPrimarySubTag)); // could be startsWith(requestPrimarySubTag + "-") with see https://tools.ietf.org/html/rfc4647#section-3.3.1
					}
					else {
						// xx-xx
						return (primarySubType.equals("*") || primarySubType.equals(requestPrimarySubTag)) && 
							(secondarySubType == null || secondarySubType.equals(requestSecondarySubTag)); // could be startsWith(requestSecondarySubTag + "-") with see https://tools.ietf.org/html/rfc4647#section-3.3.1
					}
				}
			}
			
			/**
			 * <p>
			 * Calculates and returns the score or the language range used for sorting.
			 * </p>
			 * 
			 * <p>
			 * The score is calculated by assigning a score to the media range part and add
			 * all:
			 * </p>
			 * 
			 * <ul>
			 * <li>the range quality value is multiplied by 1000</li>
			 * <li>* is worth 0</li>
			 * <li>xx is worth 10</li>
			 * <li>xx-xx is worth 20</li>
			 * </ul>
			 * 
			 * @return the score of the language range
			 */
			default int getScore() {
				String primarySubTag = this.getPrimarySubTag();
				String secondarySubTag = this.getSecondarySubTag();
				float weight = this.getWeight();
				int score = 0;
				
				// 1. weight
				score += 1000 * weight;
				
				// 2. wildcards
				if(!primarySubTag.equals("*")) {
					if(secondarySubTag == null) {
						// xx
						score += 1 * 10;
					}
					else {
						// xx-xx
						score += 2 * 10;
					}
				}
				return score;
			}
			
			/**
			 * <p>
			 * Returns the first language range in the specified list that matches the
			 * specified language range.
			 * </p>
			 * 
			 * @param languageRange  a language range
			 * @param languageRanges a list of language ranges
			 * 
			 * @return an optional returning the first match or an empty optional if no
			 *         match was found
			 */
			static Optional<AcceptMatch<LanguageRange, LanguageRange>> findFirstMatch(LanguageRange languageRange, List<LanguageRange> languageRanges) {
				return findFirstMatch(languageRange, languageRanges, Function.identity());
			}

			/**
			 * <p>
			 * Returns the first item in the specified collection whose language range
			 * matches the specified language range.
			 * </p>
			 * 
			 * @param <T>                    the type of the item
			 * @param languageRange          a language range
			 * @param items                  a collection of items
			 * @param languageRangeExtractor a function that extracts the language range of
			 *                               an item
			 * 
			 * @return an optional returning the first match or an empty optional if no
			 *         match was found
			 */
			static <T> Optional<AcceptMatch<T, LanguageRange>> findFirstMatch(LanguageRange languageRange, Collection<T> items, Function<T, LanguageRange> languageRangeExtractor) {
				String requestPrimarySubTag = languageRange.getPrimarySubTag();
				String requestSecondarySubTag = languageRange.getSecondarySubTag();

				return items.stream()
					.filter(item -> {
						LanguageRange range = languageRangeExtractor.apply(item);
						String primarySubType = range.getPrimarySubTag();
						String secondarySubType = range.getSecondarySubTag();

						if(requestPrimarySubTag.equals("*")) {
							// *
							return true;
						}
						else {
							if(requestSecondarySubTag == null) {
								// xx
								return (primarySubType.equals("*") || primarySubType.equals(requestPrimarySubTag)); // could be startsWith(requestPrimarySubTag + "-") with see https://tools.ietf.org/html/rfc4647#section-3.3.1
							}
							else {
								// xx-xx
								return (primarySubType.equals("*") || primarySubType.equals(requestPrimarySubTag)) && 
									(secondarySubType.equals("*") || secondarySubType.equals(requestSecondarySubTag)); // could be startsWith(requestSecondarySubTag + "-") with see https://tools.ietf.org/html/rfc4647#section-3.3.1
							}
						}
					})
					.findFirst()
					.map(item -> new AcceptMatch<>(item, languageRange));
			}
		}
	}
}

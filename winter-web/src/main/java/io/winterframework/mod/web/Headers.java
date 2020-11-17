/**
 * 
 */
package io.winterframework.mod.web;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author jkuhn
 *
 */
public final class Headers {

	public static final String ACCEPT = "accept";
	public static final String CONTENT_DISPOSITION = "content-disposition";
	public static final String CONTENT_TYPE = "content-type";
	public static final String CONTENT_LENGTH = "content-length";
	public static final String COOKIE = "cookie";
	public static final String HOST = "host";
	public static final String SET_COOKIE = "set-cookie";
	public static final String TRANSFER_ENCODING = "transfer-encoding";
	
	// HTTP/2 pseudo headers
	public static final String PSEUDO_AUTHORITY = ":authority";
	public static final String PSEUDO_METHOD = ":method";
	public static final String PSEUDO_PATH = ":path";
	public static final String PSEUDO_SCHEME = ":scheme";
	public static final String PSEUDO_STATUS = ":status";
	
	private Headers() {}
	
	/**
	 * https://tools.ietf.org/html/rfc7231#section-3.1.1.5
	 * 
	 * @author jkuhn
	 *
	 */
	public static interface ContentType extends Header {
		
		public static final String BOUNDARY = "boundary";
		
		public static final String CHARSET = "charset";
		
		String getMediaType();
		
		String getType();
		
		String getSubType();
		
		String getBoundary();

		Charset getCharset();
		
		Map<String, String> getParameters();
		
		MediaRange toMediaRange();
	}
	
	/**
	 * https://tools.ietf.org/html/rfc6266#section-4.1
	 * 
	 * @author jkuhn
	 *
	 */
	public static interface ContentDisposition extends Header {

		public static final String PART_NAME = "name";
		public static final String FILENAME = "filename";
		public static final String CREATION_DATE = "creation-date";
		public static final String MODIFICATION_DATE = "modification-date";
		public static final String READ_DATE = "read-date";
		public static final String SIZE = "size";
		
		String getDispositionType();
		
		String getPartName();
		
		String getFilename();
		
		String getCreationDateTime();
		
		String getModificationDatetime();
		
		String getReadDateTime();
		
		Integer getSize();
	}
	
	/**
	 * https://tools.ietf.org/html/rfc6265#section-4.1
	 * 
	 * @author jkuhn
	 * 
	 */
	public static interface SetCookie extends Header {
		
		public static final String EXPIRES = "Expires";
		public static final String MAX_AGE = "Max-Age";
		public static final String DOMAIN = "Domain";
		public static final String PATH = "Path";
		public static final String SECURE = "Secure";
		public static final String HTTPONLY = "HttpOnly";
		
		String getName();
		
		String getValue();
		
		String getExpires();
		
		Integer getMaxAge();
		
		String getDomain();
		
		String getPath();
		
		Boolean isSecure();
		
		Boolean isHttpOnly();
	}
	
	/**
	 * https://tools.ietf.org/html/rfc6265#section-4.2
	 * 
	 * @author jkuhn
	 *
	 */
	public static interface Cookie extends Header {
		
		public Map<String, List<io.winterframework.mod.web.Cookie>> getPairs();
	}
	
	/**
	 * https://tools.ietf.org/html/rfc7231#section-5.3.2
	 * 
	 * @author jkuhn
	 *
	 */
	public static interface Accept extends Header {
		
		List<MediaRange> getMediaRanges();
		
		default Optional<Headers.ContentType> findBestMatch(Collection<Headers.ContentType> contentTypes) {
			return this.findBestMatch(contentTypes, Function.identity());
		}
		
		default <T> Optional<T> findBestMatch(Collection<T> items, Function<T, Headers.ContentType> contentTypeExtractor) {
			for(MediaRange mediaRange : this.getMediaRanges()) {
				for(T item : items) {
					if(mediaRange.matches(contentTypeExtractor.apply(item))) {
						return Optional.of(item);
					}
				}
			}
			return Optional.empty();
		}
	}
	
	public static interface MediaRange {
		
		public static final Comparator<MediaRange> COMPARATOR = (r1, r2) -> r2.getScore() - r1.getScore();
		
		String getMediaType();
		
		String getType();
		
		String getSubType();
		
		float getWeight();
		
		Map<String, String> getParameters();
		
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
		
		static Optional<MediaRange> findFirstMatch(Headers.ContentType contentType, List<MediaRange> mediaRanges) {
			return findFirstMatch(contentType, mediaRanges, Function.identity());
		}
		
		static <T> Optional<T> findFirstMatch(Headers.ContentType contentType, Collection<T> items, Function<T, MediaRange> mediaRangeExtractor) {
			String requestType = contentType.getType();
			String requestSubType = contentType.getSubType();
			Map<String, String> requestParameters = contentType.getParameters();

			return items.stream()
				.filter(item -> {
					MediaRange range = mediaRangeExtractor.apply(item);
					String consumeType = range.getType();
					String consumeSubType = range.getSubType();
					Map<String, String> consumeParameters = range.getParameters();

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
				})
				.findFirst();
		}
	}
}

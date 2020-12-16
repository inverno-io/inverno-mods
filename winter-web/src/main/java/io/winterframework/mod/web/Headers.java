/**
 * 
 */
package io.winterframework.mod.web;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author jkuhn
 *
 */
public final class Headers {

	public static final String ACCEPT = "accept";
	public static final String ACCEPT_LANGUAGE = "accept-language";
	public static final String ALLOW = "allow";
	public static final String CONNECTION = "connection";
	public static final String CONTENT_DISPOSITION = "content-disposition";
	public static final String CONTENT_TYPE = "content-type";
	public static final String CONTENT_LENGTH = "content-length";
	public static final String COOKIE = "cookie";
	public static final String HOST = "host";
	public static final String RETRY_AFTER = "retry-after";
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
		
		Accept.MediaRange toMediaRange();
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
		
		default Optional<AcceptMatch<MediaRange, Headers.ContentType>> findBestMatch(Collection<Headers.ContentType> contentTypes) {
			return this.findBestMatch(contentTypes, Function.identity());
		}
		
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
		
		default List<AcceptMatch<MediaRange, Headers.ContentType>> findAllMatch(Collection<Headers.ContentType> contentTypes) {
			return this.findAllMatch(contentTypes, Function.identity());
		}
		
		default <T> List<AcceptMatch<MediaRange, T>> findAllMatch(Collection<T> items, Function<T, Headers.ContentType> contentTypeExtractor) {
			List<AcceptMatch<MediaRange, T>> result = new ArrayList<>();
			for(MediaRange mediaRange : this.getMediaRanges()) {
				for(T item : items) {
					if(mediaRange.matches(contentTypeExtractor.apply(item))) {
						result.add(new AcceptMatch<>(mediaRange, item));
					}
				}
			}
			return result;
		}
		
		static Optional<Accept> merge(List<Accept> acceptHeaders) {
			if(acceptHeaders.isEmpty()) {
				return Optional.empty();
			}
			else if(acceptHeaders.size() == 1) {
				return Optional.of(acceptHeaders.get(0));
			}
			else {
				return Optional.of(new Accept() {

					@Override
					public String getHeaderName() {
						return Headers.ACCEPT;
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
			
			static Optional<AcceptMatch<MediaRange, Headers.ContentType>> findFirstMatch(Headers.ContentType contentType, List<MediaRange> mediaRanges) {
				return findFirstMatch(contentType, mediaRanges, Function.identity());
			}
			
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
	
	public static class AcceptMatch<A, B> {
		
		private A source;
		
		private B target;
		
		private AcceptMatch(A source, B target) {
			this.source = source;
			this.target = target;
		}
		
		public A getSource() {
			return this.source;
		}
		
		public B getTarget() {
			return this.target;
		}
	}
	
	/**
	 * https://tools.ietf.org/html/rfc7231#section-5.3.5
	 * https://tools.ietf.org/html/rfc4647#section-3.3.1
	 * 
	 * @author jkuhn
	 *
	 */
	public static interface AcceptLanguage extends Header {
		
		List<LanguageRange> getLanguageRanges();
		
		default Optional<AcceptMatch<LanguageRange, LanguageRange>> findBestMatch(Collection<Headers.AcceptLanguage.LanguageRange> languageRanges) {
			return this.findBestMatch(languageRanges, Function.identity());
		}
		
		default <T> Optional<AcceptMatch<LanguageRange,T>> findBestMatch(Collection<T> items, Function<T, Headers.AcceptLanguage.LanguageRange> languageRangeExtractor) {
			for(LanguageRange languageRange : this.getLanguageRanges()) {
				for(T item : items) {
					if(languageRange.matches(languageRangeExtractor.apply(item))) {
						return Optional.of(new AcceptMatch<>(languageRange, item));
					}
				}
			}
			return Optional.empty();
		}
		
		default List<AcceptMatch<LanguageRange, LanguageRange>> findAllMatch(Collection<Headers.AcceptLanguage.LanguageRange> languageRanges) {
			return this.findAllMatch(languageRanges, Function.identity());
		}
		
		default <T> List<AcceptMatch<LanguageRange,T>> findAllMatch(Collection<T> items, Function<T, Headers.AcceptLanguage.LanguageRange> languageRangeExtractor) {
			List<AcceptMatch<LanguageRange,T>> result = new ArrayList<>();
			for(LanguageRange languageRange : this.getLanguageRanges()) {
				for(T item : items) {
					if(languageRange.matches(languageRangeExtractor.apply(item))) {
						result.add(new AcceptMatch<>(languageRange, item));
					}
				}
			}
			return result;
		}
		
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
						return Headers.ACCEPT;
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
		
		public static interface LanguageRange {
			
			public static final Comparator<LanguageRange> COMPARATOR = (r1, r2) -> r2.getScore() - r1.getScore();
			
			String getLanguageTag();
			
			String getPrimarySubTag();
			
			String getSecondarySubTag();
			
			float getWeight();
			
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
							(secondarySubType.equals("*") || secondarySubType.equals(requestSecondarySubTag)); // could be startsWith(requestSecondarySubTag + "-") with see https://tools.ietf.org/html/rfc4647#section-3.3.1
					}
				}
			}
			
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
			
			static Optional<AcceptMatch<LanguageRange, LanguageRange>> findFirstMatch(LanguageRange languageRange, List<LanguageRange> languageRanges) {
				return findFirstMatch(languageRange, languageRanges, Function.identity());
			}
			
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

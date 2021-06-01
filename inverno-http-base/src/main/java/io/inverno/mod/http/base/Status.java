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
package io.inverno.mod.http.base;

/**
 * <p>
 * Enumeration of HTTP statuses as defined by
 * <a href="https://tools.ietf.org/html/rfc7231#section-6">RFC 7231 Section
 * 6</a>.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public enum Status {

	/**
	 *  <a href="https://tools.ietf.org/html/rfc7231#section-6.2.1">RFC 7231 Section 6.2.1</a>
	 */
	CONTINUE(100, "Continue"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.2.2">RFC 7231 Section 6.2.2</a>
	 */
	SWITCHING_PROTOCOLS(101, "Switching Protocols"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.3.1">RFC 7231 Section 6.3.1</a>
	 */
	OK(200, "OK"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.3.2">RFC 7231 Section 6.3.2</a>
	 */
	CREATED(201, "Created"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.3.3">RFC 7231 Section 6.3.3</a>
	 */
	ACCEPTED(202, "Accepted"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.3.4">RFC 7231 Section 6.3.4</a>
	 */
	NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.3.5">RFC 7231 Section 6.3.5</a>
	 */
	NO_CONTENT(204, "No Content"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.3.6">RFC 7231 Section 6.3.6</a>
	 */
	RESET_CONTENT(205, "Reset Content"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7233#section-4.1">RFC 7231 Section 4.1</a>
	 */
	PARTIAL_CONTENT(206, "Partial Content"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.4.1">RFC 7231 Section 6.4.1</a>
	 */
	MULTIPLE_CHOICES(300, "Multiple Choices"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.4.2">RFC 7231 Section 6.4.2</a>
	 */
	MOVED_PERMANENTLY(301, "Moved Permanently"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.4.3">RFC 7231 Section 6.4.3</a>
	 */
	FOUND(302, "Found"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.4.4">RFC 7231 Section 6.4.4</a>
	 */
	SEE_OTHER(303, "See Other"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7232#section-4.1">RFC 7231 Section 4.1</a>
	 */
	NOT_MODIFIED(304, "Not Modified"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.4.5">RFC 7231 Section 6.4.5</a>
	 */
	USE_PROXY(305, "Use Proxy"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.4.7">RFC 7231 Section 6.4.7</a>
	 */
	TEMPORARY_REDIRECT(307, "Temporary Redirect"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.5.1">RFC 7231 Section 6.5.1</a>
	 */
	BAD_REQUEST(400, "Bad Request"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7235#section-3.1">RFC 7231 Section 3.1</a>
	 */
	UNAUTHORIZED(401, "Unauthorized"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.5.2">RFC 7231 Section 6.5.2</a>
	 */
	PAYMENT_REQUIRED(402, "Payment Required"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.5.3">RFC 7231 Section 6.5.3</a>
	 */
	FORBIDDEN(403, "Forbidden"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.5.4">RFC 7231 Section 6.5.4</a>
	 */
	NOT_FOUND(404, "Not Found"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.5.5">RFC 7231 Section 6.5.5</a>
	 */
	METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.5.6">RFC 7231 Section 6.5.6</a>
	 */
	NOT_ACCEPTABLE(406, "Not Acceptable"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7235#section-3.2">RFC 7231 Section 3.2</a>
	 */
	PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.5.7">RFC 7231 Section 6.5.7</a>
	 */
	REQUEST_TIMEOUT(408, "Request Timeout"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.5.8">RFC 7231 Section 6.5.8</a>
	 */
	CONFLICT(409, "Conflict"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.5.9">RFC 7231 Section 6.5.9</a>
	 */
	GONE(410, "Gone"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.5.1">RFC 7231 Section 6.5.1</a>
	 */
	LENGTH_REQUIRED(411, "Length Required"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7232#section-4.2">RFC 7231 Section 4.2</a>
	 */
	PRECONDITION_FAILED(412, "Precondition Failed"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.5.1">RFC 7231 Section 6.5.1</a>
	 */
	PAYLOAD_TOO_LARGE(413, "Payload Too Large"), 
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.5.1">RFC 7231 Section 6.5.1</a>
	 */
	URI_TOO_LONG(414, "URI Too Long"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.5.1">RFC 7231 Section 6.5.1</a>
	 */
	UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7233#section-4.4">RFC 7231 Section 4.4</a>
	 */
	RANGE_NOT_SATISFIABLE(416, "Range Not Satisfiable"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.5.1">RFC 7231 Section 6.5.1</a>
	 */
	EXPECTATION_FAILED(417, "Expectation Failed"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.5.1">RFC 7231 Section 6.5.1</a>
	 */
	UPGRADE_REQUIRED(426, "Upgrade Required"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.6.1">RFC 7231 Section 6.6.1</a>
	 */
	INTERNAL_SERVER_ERROR(500, "Internal Server Error"), 
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.6.2">RFC 7231 Section 6.6.2</a>
	 */
	NOT_IMPLEMENTED(501, "Not Implemented"), 
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.6.3">RFC 7231 Section 6.6.3</a>
	 */
	BAD_GATEWAY(502, "Bad Gateway"), 
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.6.4">RFC 7231 Section 6.6.4</a>
	 */
	SERVICE_UNAVAILABLE(503, "Service Unavailable"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.6.5">RFC 7231 Section 6.6.5</a>
	 */
	GATEWAY_TIMEOUT(504, "Gateway Timeout"),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6.6.6">RFC 7231 Section 6.6.6</a>
	 */
	HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version Not Supported");
	
	private int code;
	
	private String reasonPhrase;
	
	private Category category;
	
	private Status(int code, String reasonPhrase) {
		this.code = code;
		this.reasonPhrase = reasonPhrase;
	}
	
	/**
	 * <p>
	 * Returns the code of the HTTP status.
	 * </p>
	 * 
	 * @return a HTTP status code
	 */
	public int getCode() {
		return this.code;
	}

	/**
	 * <p>
	 * Returns the reason phrase associated to the HTTP status.
	 * </p>
	 * 
	 * @return a reason phrase
	 */
	public String getReasonPhrase() {
		return this.reasonPhrase;
	}
	
	/**
	 * <p>
	 * Returns the category of the HTTP status.
	 * </p>
	 * 
	 * @return the HTTP status category
	 */
	public Category getCategory() {
		return this.category;
	}
	
	/**
	 * <p>
	 * Returns the HTTP status corresponding to the specified code.
	 * </p>
	 * 
	 * @param code an HTTP code
	 * 
	 * @return a HTTP status
	 * @throws IllegalArgumentException if the specified status doesn't correspond
	 *                                  to a known HTTP status
	 */
	public static Status valueOf(int code) {
		for(Status status : values()) {
			if(status.getCode() == code) {
				return status;
			}
		}
		throw new IllegalArgumentException("No enum constant for status code: " + code);
	}
	
	/**
	 * <p>
	 * Describes the category of an HTTP status as defined by
	 * <a href="https://tools.ietf.org/html/rfc7231#section-6">RFC 7231 Section
	 * 6</a>.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	public static enum Category {
		
		/**
		 * <a href="https://tools.ietf.org/html/rfc7231#section-6.2">RFC 7231 Section 6.2</a>
		 */
		INFORMATIONAL((byte)1),
		/**
		 * <a href="https://tools.ietf.org/html/rfc7231#section-6.3">RFC 7231 Section 6.3</a>
		 */
		SUCCESSUL((byte)2),
		/**
		 * <a href="https://tools.ietf.org/html/rfc7231#section-6.4">RFC 7231 Section 6.4</a>
		 */
		REDIRECTION((byte)3),
		/**
		 * <a href="https://tools.ietf.org/html/rfc7231#section-6.5">RFC 7231 Section 6.5</a>
		 */
		CLIENT_ERROR((byte)4),
		/**
		 * <a href="https://tools.ietf.org/html/rfc7231#section-6.6">RFC 7231 Section 6.6</a>
		 */
		SERVER_ERROR((byte)5);
		
		private byte categoryId;
		
		/**
		 * <p>
		 * Returns the HTTP status category corresponding to the specified category id.
		 * </p>
		 * 
		 * @param categoryId a category id
		 * 
		 * @return a HTTP status category
		 * @throws IllegalArgumentException if the specified id doesn't correspond to a
		 *                                  known HTTP status category
		 */
		private Category(byte categoryId) {
			this.categoryId = categoryId;
		}
		
		/**
		 * <p>
		 * Returns the HTTP status corresponding to the specified HTTP status code.
		 * </p>
		 * 
		 * @param statusCode an HTTP status code
		 * 
		 * @return a HTTP status category
		 * @throws IllegalArgumentException if the specified status doesn't correspond
		 *                                  to a known HTTP status
		 */
		public static Category valueOf(int statusCode) {
			int statusCategoryId = statusCode / 100;
			for (Category category : values()) {
				if (category.categoryId == statusCategoryId) {
					return category;
				}
			}
			throw new IllegalArgumentException("No enum constant for status code: " + statusCode);
		}
		
		/**
		 * <p>
		 * Returns the HTTP status corresponding to the specified HTTP status.
		 * </p>
		 * 
		 * @param status an HTTP status
		 * 
		 * @return a HTTP status category
		 */
		public static Category valueOf(Status status) {
			return valueOf(status.getCode());
		}
	}
}

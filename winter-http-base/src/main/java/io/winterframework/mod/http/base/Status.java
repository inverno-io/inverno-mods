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
package io.winterframework.mod.http.base;

/**
 * https://tools.ietf.org/html/rfc7231#section-6
 * 
 * @author jkuhn
 *
 */
public enum Status {

	/**
	 *  Https://tools.ietf.org/html/rfc7231#section 6.2.1
	 */
	CONTINUE(100, "Continue"),
	/**
	 * // https://tools.ietf.org/html/rfc7231#section 6.2.2
	 */
	SWITCHING_PROTOCOLS(101, "Switching Protocols"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.3.1
	 */
	OK(200, "OK"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.3.2
	 */
	CREATED(201, "Created"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.3.3
	 */
	ACCEPTED(202, "Accepted"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.3.4
	 */
	NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.3.5
	 */
	NO_CONTENT(204, "No Content"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.3.6
	 */
	RESET_CONTENT(205, "Reset Content"),
	/**
	 * https://tools.ietf.org/html/rfc7233#section-4.1
	 */
	PARTIAL_CONTENT(206, "Partial Content"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.4.1
	 */
	MULTIPLE_CHOICES(300, "Multiple Choices"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.4.2
	 */
	MOVED_PERMANENTLY(301, "Moved Permanently"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.4.3
	 */
	FOUND(302, "Found"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.4.4
	 */
	SEE_OTHER(303, "See Other"),
	/**
	 * https://tools.ietf.org/html/rfc7232#section-4.1
	 */
	NOT_MODIFIED(304, "Not Modified"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.4.5
	 */
	USE_PROXY(305, "Use Proxy"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.4.7
	 */
	TEMPORARY_REDIRECT(307, "Temporary Redirect"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.5.1
	 */
	BAD_REQUEST(400, "Bad Request"),
	/**
	 * https://tools.ietf.org/html/rfc7235#section-3.1
	 */
	UNAUTHORIZED(401, "Unauthorized"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.5.2
	 */
	PAYMENT_REQUIRED(402, "Payment Required"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.5.3
	 */
	FORBIDDEN(403, "Forbidden"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.5.4
	 */
	NOT_FOUND(404, "Not Found"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.5.5
	 */
	METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.5.6
	 */
	NOT_ACCEPTABLE(406, "Not Acceptable"),
	/**
	 * https://tools.ietf.org/html/rfc7235#section-3.2
	 */
	PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.5.7
	 */
	REQUEST_TIMEOUT(408, "Request Timeout"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.5.8
	 */
	CONFLICT(409, "Conflict"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.5.9
	 */
	GONE(410, "Gone"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.5.1
	 */
	LENGTH_REQUIRED(411, "Length Required"),
	/**
	 * https://tools.ietf.org/html/rfc7232#section-4.2
	 */
	PRECONDITION_FAILED(412, "Precondition Failed"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.5.1
	 */
	PAYLOAD_TOO_LARGE(413, "Payload Too Large"), 
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.5.1
	 */
	URI_TOO_LONG(414, "URI Too Long"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.5.1
	 */
	UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
	/**
	 * https://tools.ietf.org/html/rfc7233#section-4.4
	 */
	RANGE_NOT_SATISFIABLE(416, "Range Not Satisfiable"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.5.1
	 */
	EXPECTATION_FAILED(417, "Expectation Failed"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.5.1
	 */
	UPGRADE_REQUIRED(426, "Upgrade Required"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.6.1
	 */
	INTERNAL_SERVER_ERROR(500, "Internal Server Error"), 
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.6.2
	 */
	NOT_IMPLEMENTED(501, "Not Implemented"), 
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.6.3
	 */
	BAD_GATEWAY(502, "Bad Gateway"), 
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.6.4
	 */
	SERVICE_UNAVAILABLE(503, "Service Unavailable"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.6.5
	 */
	GATEWAY_TIMEOUT(504, "Gateway Timeout"),
	/**
	 * https://tools.ietf.org/html/rfc7231#section 6.6.6
	 */
	HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version Not Supported");
	
	private int code;
	
	private String reasonPhrase;
	
	private Category category;
	
	private Status(int code, String reasonPhrase) {
		this.code = code;
		this.reasonPhrase = reasonPhrase;
	}
	
	public int getCode() {
		return this.code;
	}
	
	public String getReasonPhrase() {
		return this.reasonPhrase;
	}
	
	public Category getCategory() {
		return this.category;
	}
	
	public static Status valueOf(int code) {
		for(Status status : values()) {
			if(status.getCode() == code) {
				return status;
			}
		}
		throw new IllegalArgumentException("No enum constant for status code: " + code);
	}
	
	public static enum Category {
		
		INFORMATIONAL((byte)1),
		SUCCESSUL((byte)2),
		REDIRECTION((byte)3),
		CLIENT_ERROR((byte)4),
		SERVER_ERROR((byte)5);
		
		private byte categoryId;
		
		private Category(byte categoryId) {
			this.categoryId = categoryId;
		}
		
		public static Category valueOf(int statusCode) {
			int statusCategoryId = statusCode / 100;
			for (Category category : values()) {
				if (category.categoryId == statusCategoryId) {
					return category;
				}
			}
			throw new IllegalArgumentException("No enum constant for status code: " + statusCode);
		}
		
		public static Category valueOf(Status status) {
			return valueOf(status.getCode());
		}
	}
}

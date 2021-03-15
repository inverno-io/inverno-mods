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

import io.winterframework.mod.http.base.Status.Category;

/**
 * <p>
 * Base exception class used to report web application errors.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class WebException extends RuntimeException {

	private static final long serialVersionUID = 2847460450871488615L;

	/**
	 * The HTTP status code.
	 */
	private int statusCode;
	
	/**
	 * The HTTP status reason phrase.
	 */
	private String statusReasonPhrase;
	
	/**
	 * The HTTP status category.
	 */
	private Status.Category statusCategory;
	
	/**
	 * <p>
	 * Creates a web exception with default status
	 * {@link Status#INTERNAL_SERVER_ERROR Internal Server Error (500)}.
	 * </p>
	 */
	public WebException() {
		this(500);
	}

	/**
	 * <p>
	 * Creates a web exception with default status
	 * {@link Status#INTERNAL_SERVER_ERROR Internal Server Error (500)} and
	 * specified message.
	 * </p>
	 * 
	 * @param message a message
	 */
	public WebException(String message) {
		this(500, message);
	}

	/**
	 * <p>
	 * Creates a web exception with default status
	 * {@link Status#INTERNAL_SERVER_ERROR Internal Server Error (500)} and
	 * specified cause.
	 * </p>
	 * 
	 * @param cause a cause
	 */
	public WebException(Throwable cause) {
		this(500, cause);
	}

	/**
	 * <p>
	 * Creates a web exception with default status
	 * {@link Status#INTERNAL_SERVER_ERROR Internal Server Error (500)}, specified
	 * message and cause
	 * </p>
	 * 
	 * @param message a message
	 * @param cause   a cause
	 */
	public WebException(String message, Throwable cause) {
		this(500, message, cause);
	}
	
	/**
	 * <p>
	 * Creates a web exception with specified HTTP status code.
	 * </p>
	 * 
	 * @param statusCode a HTTP status code
	 * 
	 * @throws IllegalArgumentException if the specified status doesn't correspond
	 *                                  to a known HTTP status
	 */
	public WebException(int statusCode) throws IllegalArgumentException {
		this.setStatusCode(statusCode);
	}

	/**
	 * <p>
	 * Creates a web exception with specified HTTP status code and message.
	 * </p>
	 * 
	 * @param statusCode a HTTP status code
	 * @param message    a message
	 * 
	 * @throws IllegalArgumentException if the specified status doesn't correspond
	 *                                  to a known HTTP status
	 */
	public WebException(int statusCode, String message) throws IllegalArgumentException {
		super(message);
		this.setStatusCode(statusCode);
	}

	/**
	 * <p>
	 * Creates a web exception with specified HTTP status code and cause.
	 * </p>
	 * 
	 * @param statusCode a HTTP status code
	 * @param cause      a cause
	 * 
	 * @throws IllegalArgumentException if the specified status doesn't correspond
	 *                                  to a known HTTP status
	 */
	public WebException(int statusCode, Throwable cause) throws IllegalArgumentException {
		super(cause);
		this.setStatusCode(statusCode);
	}

	/**
	 * <p>
	 * Creates a web exception with specified HTTP status code, message and cause.
	 * </p>
	 * 
	 * @param statusCode a HTTP status code
	 * @param message    a message
	 * @param cause      a cause
	 * 
	 * @throws IllegalArgumentException if the specified status doesn't correspond
	 *                                  to a known HTTP status
	 */
	public WebException(int statusCode, String message, Throwable cause) throws IllegalArgumentException {
		super(message, cause);
		this.setStatusCode(statusCode);
	}
	
	/**
	 * <p>
	 * Creates a web exception with specified HTTP status.
	 * </p>
	 * 
	 * @param status a HTTP status
	 */
	public WebException(Status status) {
		this.setStatus(status);
	}

	/**
	 * <p>
	 * Creates a web exception with specified HTTP status and message.
	 * </p>
	 * 
	 * @param status  a HTTP status
	 * @param message a message
	 */
	public WebException(Status status, String message) {
		super(message);
		this.setStatus(status);
	}

	/**
	 * <p>
	 * Creates a web exception with specified HTTP status and cause.
	 * </p>
	 * 
	 * @param status a HTTP status
	 * @param cause  a cause
	 */
	public WebException(Status status, Throwable cause) {
		super(cause);
		this.setStatus(status);
	}

	/**
	 * <p>
	 * Creates a web exception with specified HTTP status, message and cause.
	 * </p>
	 * 
	 * @param status  a HTTP status
	 * @param message a message
	 * @param cause   a cause
	 */
	public WebException(Status status, String message, Throwable cause) {
		super(message, cause);
		this.setStatus(status);
	}
	
	private void setStatus(Status status) {
		this.statusCode = status.getCode();
		this.statusReasonPhrase = status.getReasonPhrase();
		this.statusCategory = status.getCategory();
	}
	
	private void setStatusCode(int statusCode) {
		try {
			Status status = Status.valueOf(statusCode);
			this.statusCode = status.getCode();
			this.statusReasonPhrase = status.getReasonPhrase();
		}
		catch(IllegalArgumentException e) {
			this.statusCode = statusCode;
			this.statusCategory = Status.Category.valueOf(statusCode);
		}
	}
	
	/**
	 * <p>
	 * Returns the HTTP status code.
	 * </p>
	 * 
	 * @return a HTTP status code
	 */
	public int getStatusCode() {
		return this.statusCode;
	}
	
	/**
	 * <p>
	 * Returns the HTTP status reason phrase.
	 * </p>
	 * 
	 * @return a reason phrase
	 */
	public String getStatusReasonPhrase() {
		return this.statusReasonPhrase;
	}
	
	/**
	 * <p>
	 * Returns the HTTP status category.
	 * </p>
	 * 
	 * @return a HTTP status category
	 */
	public Category getStatusCategory() {
		return this.statusCategory;
	}
}

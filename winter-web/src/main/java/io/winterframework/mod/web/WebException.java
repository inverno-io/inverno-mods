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
package io.winterframework.mod.web;

import io.winterframework.mod.web.Status.Category;

/**
 * @author jkuhn
 *
 */
public class WebException extends RuntimeException {

	private static final long serialVersionUID = 2847460450871488615L;

	private int statusCode;
	
	private String statusReasonPhrase;
	
	private Status.Category statusCategory;
	
	public WebException() {
		this(500);
	}

	public WebException(String message) {
		this(500, message);
	}

	public WebException(Throwable cause) {
		this(500, cause);
	}

	public WebException(String message, Throwable cause) {
		this(500, message, cause);
	}
	
	public WebException(int statusCode) {
		this.setStatusCode(statusCode);
	}

	public WebException(int statusCode, String message) {
		super(message);
		this.setStatusCode(statusCode);
	}

	public WebException(int statusCode, Throwable cause) {
		super(cause);
		this.setStatusCode(statusCode);
	}

	public WebException(int statusCode, String message, Throwable cause) {
		super(message, cause);
		this.setStatusCode(statusCode);
	}
	
	public WebException(Status status) {
		this.setStatus(status);
	}

	public WebException(Status status, String message) {
		super(message);
		this.setStatus(status);
	}

	public WebException(Status status, Throwable cause) {
		super(cause);
		this.setStatus(status);
	}

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
	
	public int getStatusCode() {
		return this.statusCode;
	}
	
	public String getStatusReasonPhrase() {
		return this.statusReasonPhrase;
	}
	
	public Category getStatusCategory() {
		return this.statusCategory;
	}
}

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

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * @author jkuhn
 *
 */
public class ServiceUnavailableException extends WebException {

	private static final long serialVersionUID = -7765924553702627055L;

	private Optional<ZonedDateTime> retryAfter;
	
	public ServiceUnavailableException() {
		super(Status.SERVICE_UNAVAILABLE);
		this.retryAfter = Optional.empty();
	}

	public ServiceUnavailableException(String message) {
		super(Status.SERVICE_UNAVAILABLE, message);
		this.retryAfter = Optional.empty();
	}

	public ServiceUnavailableException(Throwable cause) {
		super(Status.SERVICE_UNAVAILABLE, cause);
		this.retryAfter = Optional.empty();
	}

	public ServiceUnavailableException(String message, Throwable cause) {
		super(Status.SERVICE_UNAVAILABLE, message, cause);
		this.retryAfter = Optional.empty();
	}
	
	public ServiceUnavailableException(long retryAfter) {
		super(Status.SERVICE_UNAVAILABLE);
		this.setRetryAfter(retryAfter);
	}

	public ServiceUnavailableException(long retryAfter, String message) {
		super(Status.SERVICE_UNAVAILABLE, message);
		this.setRetryAfter(retryAfter);
	}

	public ServiceUnavailableException(long retryAfter, Throwable cause) {
		super(Status.SERVICE_UNAVAILABLE, cause);
		this.setRetryAfter(retryAfter);
	}

	public ServiceUnavailableException(long retryAfter, String message, Throwable cause) {
		super(Status.SERVICE_UNAVAILABLE, message, cause);
		this.setRetryAfter(retryAfter);
	}
	
	public ServiceUnavailableException(ZonedDateTime retryAfter) {
		super(Status.SERVICE_UNAVAILABLE);
		this.setRetryAfter(retryAfter);
	}

	public ServiceUnavailableException(ZonedDateTime retryAfter, String message) {
		super(Status.SERVICE_UNAVAILABLE, message);
		this.setRetryAfter(retryAfter);
	}

	public ServiceUnavailableException(ZonedDateTime retryAfter, Throwable cause) {
		super(Status.SERVICE_UNAVAILABLE, cause);
		this.setRetryAfter(retryAfter);
	}

	public ServiceUnavailableException(ZonedDateTime retryAfter, String message, Throwable cause) {
		super(Status.SERVICE_UNAVAILABLE, message, cause);
		this.setRetryAfter(retryAfter);
	}
	
	private void setRetryAfter(long retryAfter) {
		this.retryAfter = Optional.of(ZonedDateTime.now().plus(retryAfter, ChronoUnit.SECONDS));
	}
	
	private void setRetryAfter(ZonedDateTime retryAfter) {
		if(retryAfter.isBefore(ZonedDateTime.now())) {
			throw new IllegalArgumentException("Can't retry in the past: " + retryAfter);
		}
		this.retryAfter = Optional.of(retryAfter);
	}
	
	public Optional<ZonedDateTime> getRetryAfter() {
		return this.retryAfter;
	}
}

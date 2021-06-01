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

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * <p>
 * A web exception that indicates that the requested resource is
 * {@link Status#SERVICE_UNAVAILABLE Service Unavailable (503)}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see HttpException
 */
public class ServiceUnavailableException extends HttpException {

	private static final long serialVersionUID = -7765924553702627055L;

	/**
	 * The datetime after which the failed request may be retried.
	 */
	private Optional<ZonedDateTime> retryAfter;
	
	/**
	 * <p>
	 * Creates a service unavailable exception.
	 * </p>
	 */
	public ServiceUnavailableException() {
		super(Status.SERVICE_UNAVAILABLE);
		this.retryAfter = Optional.empty();
	}

	/**
	 * <p>
	 * Creates a service unavailable exception with the specified message.
	 * </p>
	 * 
	 * @param message a message
	 */
	public ServiceUnavailableException(String message) {
		super(Status.SERVICE_UNAVAILABLE, message);
		this.retryAfter = Optional.empty();
	}

	/**
	 * <p>
	 * Creates a service unavailable exception with the specified cause.
	 * </p>
	 * 
	 * @param cause a cause
	 */
	public ServiceUnavailableException(Throwable cause) {
		super(Status.SERVICE_UNAVAILABLE, cause);
		this.retryAfter = Optional.empty();
	}

	/**
	 * <p>
	 * Creates a service unavailable exception with the specified message and cause.
	 * </p>
	 * 
	 * @param message a message
	 * @param cause   a cause
	 */
	public ServiceUnavailableException(String message, Throwable cause) {
		super(Status.SERVICE_UNAVAILABLE, message, cause);
		this.retryAfter = Optional.empty();
	}

	/**
	 * <p>
	 * Creates a service unavailable exception with the specified interval in
	 * seconds after which the failed request may be retried.
	 * </p>
	 * 
	 * @param retryAfter interval in seconds after which the failed request may be
	 *                   retried
	 */
	public ServiceUnavailableException(long retryAfter) {
		super(Status.SERVICE_UNAVAILABLE);
		this.setRetryAfter(retryAfter);
	}

	/**
	 * <p>
	 * Creates a service unavailable exception with the specified interval in
	 * seconds after which the failed request may be retried and message.
	 * </p>
	 * 
	 * @param retryAfter interval in seconds after which the failed request may be
	 *                   retried
	 * @param message    a message
	 */
	public ServiceUnavailableException(long retryAfter, String message) {
		super(Status.SERVICE_UNAVAILABLE, message);
		this.setRetryAfter(retryAfter);
	}
	
	/**
	 * <p>
	 * Creates a service unavailable exception with the specified interval in
	 * seconds after which the failed request may be retried and cause.
	 * </p>
	 * 
	 * @param retryAfter interval in seconds after which the failed request may be
	 *                   retried
	 * @param cause      a cause
	 */
	public ServiceUnavailableException(long retryAfter, Throwable cause) {
		super(Status.SERVICE_UNAVAILABLE, cause);
		this.setRetryAfter(retryAfter);
	}

	/**
	 * <p>
	 * Creates a service unavailable exception with the specified interval in
	 * seconds after which the failed request may be retried, message and cause.
	 * </p>
	 * 
	 * @param retryAfter interval in seconds after which the failed request may be
	 *                   retried
	 * @param message    a message
	 * @param cause      a cause
	 */
	public ServiceUnavailableException(long retryAfter, String message, Throwable cause) {
		super(Status.SERVICE_UNAVAILABLE, message, cause);
		this.setRetryAfter(retryAfter);
	}
	
	/**
	 * <p>
	 * Creates a service unavailable exception with the specified date time after
	 * which the failed request may be retried.
	 * </p>
	 * 
	 * @param retryAfter a date time after which the failed request may be retried
	 */
	public ServiceUnavailableException(ZonedDateTime retryAfter) {
		super(Status.SERVICE_UNAVAILABLE);
		this.setRetryAfter(retryAfter);
	}

	/**
	 * <p>
	 * Creates a service unavailable exception with the specified date time after
	 * which the failed request may be retried and message.
	 * </p>
	 * 
	 * @param retryAfter a date time after which the failed request may be retried
	 * @param message    a message
	 */
	public ServiceUnavailableException(ZonedDateTime retryAfter, String message) {
		super(Status.SERVICE_UNAVAILABLE, message);
		this.setRetryAfter(retryAfter);
	}

	/**
	 * <p>
	 * Creates a service unavailable exception with the specified date time after
	 * which the failed request may be retried and cause.
	 * </p>
	 * 
	 * @param retryAfter a date time after which the failed request may be retried
	 * @param cause      a cause
	 */
	public ServiceUnavailableException(ZonedDateTime retryAfter, Throwable cause) {
		super(Status.SERVICE_UNAVAILABLE, cause);
		this.setRetryAfter(retryAfter);
	}

	/**
	 * <p>
	 * Creates a service unavailable exception with the specified date time after
	 * which the failed request may be retried, message and cause.
	 * </p>
	 * 
	 * @param retryAfter a date time after which the failed request may be retried
	 * @param message    a message
	 * @param cause      a cause
	 */
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
	
	/**
	 * <p>
	 * Returns the datetime after which the failed request may be retried.
	 * </p>
	 * 
	 * @return a zoned datetime
	 */
	public Optional<ZonedDateTime> getRetryAfter() {
		return this.retryAfter;
	}
}

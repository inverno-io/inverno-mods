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
package io.inverno.mod.configuration;

/**
 * <p>
 * Thrown to indicate that an error occured in a configuration source.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see ConfigurationSource
 * @see ConfigurationQueryResult
 * @see ConfigurationUpdateResult
 */
public class ConfigurationSourceException extends RuntimeException {

	private static final long serialVersionUID = 2438241833256744554L;

	/**
	 * The configuration source at the origin of the error.
	 */
	private ConfigurationSource<?,?,?> source;

	/**
	 * <p>
	 * Creates a configuration source exception for the specified source.
	 * </p>
	 * 
	 * @param source the configuration source
	 */
	public ConfigurationSourceException(ConfigurationSource<?,?,?> source) {
		super();
		this.source = source;
	}

	/**
	 * <p>
	 * Creates a configuration source exception for the specified source and with the specified message.
	 * </p>
	 *
	 * @param source  the configuration source
	 * @param message the message
	 */
	public ConfigurationSourceException(ConfigurationSource<?,?,?> source, String message) {
		super(message);
	}

	/**
	 * <p>
	 * Creates a configuration source exception for the specified source and with the specified cause.
	 * </p>
	 *
	 * @param source the configuration source
	 * @param cause  the cause
	 */
	public ConfigurationSourceException(ConfigurationSource<?,?,?> source, Throwable cause) {
		super(cause);
	}
	
	/**
	 * <p>
	 * Creates a configuration source exception for the specified source and with the specified message and cause.
	 * </p>
	 *
	 * @param source  the configuration source
	 * @param message the message
	 * @param cause   the cause
	 */
	public ConfigurationSourceException(ConfigurationSource<?,?,?> source, String message, Throwable cause) {
		super(message, cause);
	}
	
	/**
	 * <p>
	 * Creates a configuration source exception for the specified source and with the specified message, cause, suppression enabled or disabled and writable stack trace enabled or disabled.
	 * </p>
	 *
	 * @param source             the configuration source
	 * @param message            the message
	 * @param cause              the cause
	 * @param enableSuppression  true to enable suppression, false otherwise
	 * @param writableStackTrace true to make the stack trace writable, false otherwise
	 */
	public ConfigurationSourceException(ConfigurationSource<?,?,?> source, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * <p>
	 * Returns the configuration source at the origin of the error.
	 * </p>
	 *
	 * @return the configuration source
	 */
	public ConfigurationSource<?,?,?> getSource() {
		return source;
	}
}

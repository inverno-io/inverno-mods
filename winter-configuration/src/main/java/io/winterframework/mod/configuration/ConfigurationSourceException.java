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
package io.winterframework.mod.configuration;

/**
 * @author jkuhn
 *
 */
public class ConfigurationSourceException extends RuntimeException {

	private static final long serialVersionUID = 2438241833256744554L;

	private ConfigurationSource<?,?,?> source;

	public ConfigurationSourceException(ConfigurationSource<?,?,?> source) {
		super();
		this.source = source;
	}

	public ConfigurationSourceException(ConfigurationSource<?,?,?> source, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ConfigurationSourceException(ConfigurationSource<?,?,?> source, String message, Throwable cause) {
		super(message, cause);
	}

	public ConfigurationSourceException(ConfigurationSource<?,?,?> source, String message) {
		super(message);
	}

	public ConfigurationSourceException(ConfigurationSource<?,?,?> source, Throwable cause) {
		super(cause);
	}
	
	public ConfigurationSource<?, ?, ?> getSource() {
		return source;
	}
}

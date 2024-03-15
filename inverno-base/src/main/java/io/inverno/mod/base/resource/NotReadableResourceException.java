/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.base.resource;

/**
 * <p>
 * 
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.8
 */
public class NotReadableResourceException extends ResourceException {

	public NotReadableResourceException() {
	}

	public NotReadableResourceException(String message) {
		super(message);
	}

	public NotReadableResourceException(Throwable cause) {
		super(cause);
	}

	public NotReadableResourceException(String message, Throwable cause) {
		super(message, cause);
	}

	public NotReadableResourceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}

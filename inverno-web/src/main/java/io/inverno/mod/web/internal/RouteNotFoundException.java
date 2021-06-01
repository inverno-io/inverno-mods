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
package io.inverno.mod.web.internal;

import io.inverno.mod.http.base.NotFoundException;

/**
 * <p>
 * Thrown by a routing link to indicate that not route was found to process a
 * request.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class RouteNotFoundException extends NotFoundException {

	private static final long serialVersionUID = 5440809816503439777L;

	public RouteNotFoundException() {
	}

	public RouteNotFoundException(String message) {
		super(message);
	}

	public RouteNotFoundException(Throwable cause) {
		super(cause);
	}

	public RouteNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}

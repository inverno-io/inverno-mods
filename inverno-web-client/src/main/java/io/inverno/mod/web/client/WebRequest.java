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
package io.inverno.mod.web.client;

import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.OutboundRequestHeaders;
import io.inverno.mod.http.client.Request;
import java.net.URI;
import java.util.function.Consumer;

/**
 * <p>
 * A request with supports for body encoding based on the request content type.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public interface WebRequest extends Request {

	/**
	 * <p>
	 * Returns the Web request URI.
	 * </p>
	 *
	 * @return the request URI
	 */
	URI getUri();
	
	@Override
	WebRequest headers(Consumer<OutboundRequestHeaders> headersConfigurer) throws IllegalStateException;

	@Override
	WebRequest path(String path) throws IllegalStateException;

	@Override
	WebRequest authority(String authority) throws IllegalStateException;

	@Override
	WebRequest method(Method method) throws IllegalStateException;

	@Override
	WebRequestBody body() throws IllegalStateException;
}

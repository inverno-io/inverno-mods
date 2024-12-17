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
import io.inverno.mod.http.client.InterceptedRequest;
import java.net.URI;
import java.util.function.Consumer;

/**
 * <p>
 * An intercepted Web request extending HTTP client {@link InterceptedRequest}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public interface InterceptedWebRequest extends InterceptedRequest {

	/**
	 * <p>
	 * Returns the Web exchange URI.
	 * </p>
	 *
	 * @return the Web exchange URI
	 */
	URI getUri();

	@Override
	InterceptedWebRequest headers(Consumer<OutboundRequestHeaders> headersConfigurer) throws IllegalStateException;

	@Override
	InterceptedWebRequest path(String path) throws IllegalStateException;

	@Override
	InterceptedWebRequest authority(String authority) throws IllegalStateException;

	@Override
	InterceptedWebRequest method(Method method) throws IllegalStateException;
}

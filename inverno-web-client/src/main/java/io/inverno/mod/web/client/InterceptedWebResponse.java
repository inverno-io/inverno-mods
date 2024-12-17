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

import io.inverno.mod.http.base.OutboundHeaders;
import io.inverno.mod.http.base.OutboundResponseHeaders;
import io.inverno.mod.http.client.InterceptedResponse;
import java.util.function.Consumer;

/**
 * <p>
 * An intercepted Web response extending HTTP client {@link InterceptedResponse}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public interface InterceptedWebResponse extends InterceptedResponse {

	@Override
	InterceptedWebResponse trailers(Consumer<OutboundHeaders<?>> trailersConfigurer) throws IllegalStateException;

	@Override
	InterceptedWebResponse headers(Consumer<OutboundResponseHeaders> headersConfigurer) throws IllegalStateException;

	@Override
	InterceptedWebResponseBody body();
}

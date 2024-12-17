/*
 * Copyright 2021 Jeremy Kuhn
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
package io.inverno.mod.web.server;

import io.inverno.mod.http.base.OutboundHeaders;
import io.inverno.mod.http.base.OutboundResponseHeaders;
import io.inverno.mod.http.server.Response;
import java.util.function.Consumer;

/**
 * <p>
 * A response with supports for body encoding based on the response content type.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see WebExchange
 */
public interface WebResponse extends Response {

	@Override
	WebResponse headers(Consumer<OutboundResponseHeaders> headersConfigurer) throws IllegalStateException;

	@Override
	WebResponse sendContinue() throws IllegalStateException;

	@Override
	WebResponseBody body();

	@Override
	WebResponse trailers(Consumer<OutboundHeaders<?>> trailersConfigurer);
}

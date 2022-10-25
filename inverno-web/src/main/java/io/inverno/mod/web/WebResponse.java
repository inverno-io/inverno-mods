/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.web;

import io.inverno.mod.http.base.OutboundHeaders;
import io.inverno.mod.http.base.OutboundResponseHeaders;
import io.inverno.mod.http.base.OutboundSetCookies;
import io.inverno.mod.http.server.Response;
import java.util.function.Consumer;

/**
 * <p>
 * A request with supports for body encoding based on the response content type.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see WebExchange
 */
public interface WebResponse extends Response {

	@Override
	WebResponseBody body();
	
	@Override
	public WebResponse headers(Consumer<OutboundResponseHeaders> headersConfigurer) throws IllegalStateException;

	@Override
	public WebResponse trailers(Consumer<OutboundHeaders<?>> trailersConfigurer);

	@Override
	@Deprecated
	public WebResponse cookies(Consumer<OutboundSetCookies> cookiesConfigurer) throws IllegalStateException;
}

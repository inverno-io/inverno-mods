/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.http.client;

import io.inverno.mod.http.base.BaseRequest;
import io.inverno.mod.http.base.OutboundRequestHeaders;
import java.util.function.Consumer;

/**
 * <p>
 * Base client request which allows to specify headers and authority.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
interface AbstractRequest extends BaseRequest {

	/**
	 * <p>
	 * Configures the HTTP headers to send in the response.
	 * </p>
	 * 
	 * @param headersConfigurer an outbound headers configurer
	 * 
	 * @return the request
	 * 
	 * @throws IllegalStateException if headers have already been sent
	 */
	AbstractRequest headers(Consumer<OutboundRequestHeaders> headersConfigurer) throws IllegalStateException;
	
	/**
	 * <p>
	 * Sets the request's authority.
	 * </p>
	 * 
	 * @param authority the authority
	 * 
	 * @return the request
	 */
	AbstractRequest authority(String authority);
}

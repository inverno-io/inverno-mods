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
package io.inverno.mod.http.client.internal;

import io.inverno.mod.http.base.OutboundRequestHeaders;

/**
 * <p>
 * The HTTP connection request headers resulting from sending the request to the endpoint.
 * </p>
 * 
 * <p>
 * Implementations shall depend on the protocol version.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.8
 */
public interface HttpConnectionRequestHeaders extends OutboundRequestHeaders {

	/**
	 * <p>
	 * Flags the headers to have been written.
	 * </p>
	 */
	void setWritten();
}

/*
 * Copyright 2023 Jeremy KUHN
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

import io.inverno.mod.http.client.Request;
import io.inverno.mod.http.client.InterceptableRequest;

/**
 * <p>
 * Base client request for {@link InterceptableRequest} and {@link Request}.
 * </p>
 *
 * <p>
 * It allows to set the actual request into the interceptable request once it has been sent to the endpoint. At this point, the interceptable request becomes immutable.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public interface BaseClientRequest extends InterceptableRequest, Request {

	/**
	 * <p>
	 * Sets the actual request sent to the endpoint.
	 * </p>
	 * 
	 * <p>
	 * At this point, the interceptable request becomes immutable.
	 * </p>
	 * 
	 * @param sentRequest the sent request
	 */
	void setSentRequest(AbstractRequest sentRequest);
}

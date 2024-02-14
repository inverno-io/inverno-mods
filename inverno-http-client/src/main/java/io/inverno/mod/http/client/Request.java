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

/**
 * <p>
 * Represents a client request in a client exchange.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @see Exchange
 */
public interface Request extends BaseRequest {
	
	/**
	 * <p>
	 * Determines whether the request headers have been sent to the endpoint.
	 * </p>
	 *
	 * @return true if headers have been sent, false otherwise
	 */
	boolean isHeadersWritten();
	
	/**
	 * <p>
	 * Returns the name of the scheme used to send the request (eg. http, https...).
	 * </p>
	 * 
	 * @return the name of the scheme
	 */
	String getScheme();
}

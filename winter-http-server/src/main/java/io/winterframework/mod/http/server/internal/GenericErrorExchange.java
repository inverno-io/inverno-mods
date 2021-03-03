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
package io.winterframework.mod.http.server.internal;

import io.winterframework.mod.http.server.ErrorExchange;
import io.winterframework.mod.http.server.Request;
import io.winterframework.mod.http.server.Response;

/**
 * @author jkuhn
 *
 */
public class GenericErrorExchange implements ErrorExchange<Throwable> {

	private final Request request;
	private final AbstractResponse response;
	private final Throwable error;
	
	public GenericErrorExchange(AbstractRequest request, AbstractResponse response, Throwable error) {
		this.request = request;
		this.response = response;
		this.error = error;
	}

	@Override
	public Request request() {
		return this.request;
	}

	@Override
	public Response response() {
		return this.response;
	}

	@Override
	public Throwable getError() {
		return this.error;
	}
}

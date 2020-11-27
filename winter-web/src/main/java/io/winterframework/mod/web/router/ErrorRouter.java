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
package io.winterframework.mod.web.router;

import io.winterframework.mod.web.Request;
import io.winterframework.mod.web.Response;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.WebException;

/**
 * @author jkuhn
 *
 */
public interface ErrorRouter extends Router<Void, ResponseBody, Throwable, ErrorRouter, ErrorRouteManager, ErrorRoute, Void, ResponseBody, Throwable> {

	@Override
	default void handle(Request<Void, Throwable> request, Response<ResponseBody> response) throws WebException {
		if(response.isHeadersWritten()) {
			// TODO exchange interrupted exception?
			throw new RuntimeException(request.context());
		}
	}
}
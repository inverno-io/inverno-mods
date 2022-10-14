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

package io.inverno.mod.http.client.internal.http1x;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.internal.InternalRequestHeaders;
import io.inverno.mod.http.client.internal.GenericOutboundHeaders;
import java.util.List;
import java.util.Map;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
class Http1xRequestHeaders extends GenericOutboundHeaders<Http1xRequestHeaders> implements InternalRequestHeaders {

	public Http1xRequestHeaders(HeaderService headerService, ObjectConverter<String> parameterConverter) {
		super(headerService, parameterConverter);
	}

	public Http1xRequestHeaders(HeaderService headerService, ObjectConverter<String> parameterConverter, List<Map.Entry<String, String>> entries) {
		super(headerService, parameterConverter, entries);
	}
}

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
package io.winterframework.mod.web.internal.server.http1x;

import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpVersion;
import io.winterframework.mod.web.internal.netty.LinkedHttpHeaders;

/**
 * @author jkuhn
 *
 */
public class Http1xRequestDecoder extends HttpRequestDecoder {
	
	// TODO make these values customizable or at least in static variables
	public Http1xRequestDecoder() {
		super(4096, 8192, 8192, true, 128);
	}

	@Override
	protected HttpMessage createMessage(String[] initialLine) throws Exception {
		return new DefaultHttpRequest(
		      HttpVersion.valueOf(initialLine[2]),
		      HttpMethod.valueOf(initialLine[0]),
		      initialLine[1],
		      new LinkedHttpHeaders());
	}
}

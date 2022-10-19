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
package io.inverno.mod.http.server.internal.http1x;

import io.inverno.mod.http.base.internal.netty.LinkedHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpVersion;

/**
 * <p>
 * HTTP1.x {@link HttpRequestDecoder} implementation.
 * </p>
 * 
 * <p>
 * This implementation basically substitutes a {@link LinkedHttpHeaders} for
 * Netty's {@link DefaultHttpHeaders} in order to increase performances.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class Http1xRequestDecoder extends HttpRequestDecoder {
	
	private static final int MAX_INITIAL_LINE_LENGTH = 4096;
	
	private static final int MAX_HEADER_SIZE = 8192;
	
	private static final int MAX_CHUNK_SIZE = 8192;
	
	private static final boolean VALIDATE_HEADER = true;
	
	private static final int INITIAL_BUFFER_SIZE = 128;
	
	public Http1xRequestDecoder() {
		super(MAX_INITIAL_LINE_LENGTH, MAX_HEADER_SIZE, MAX_CHUNK_SIZE, VALIDATE_HEADER, INITIAL_BUFFER_SIZE);
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

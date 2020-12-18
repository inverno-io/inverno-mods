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
package io.winterframework.mod.web.internal.server.http2;

import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import io.winterframework.mod.web.Header;
import io.winterframework.mod.web.ResponseTrailers;

/**
 * @author jkuhn
 *
 */
public class Http2ResponseTrailers implements ResponseTrailers {

	private final Http2Headers internalTrailers;
	
	public Http2ResponseTrailers() {
		this.internalTrailers = new DefaultHttp2Headers();
	}

	Http2Headers getInternalTrailers() {
		return this.internalTrailers;
	}
	
	@Override
	public ResponseTrailers add(String name, String value) {
		this.internalTrailers.add(name, value);
		return this;
	}

	@Override
	public ResponseTrailers add(CharSequence name, CharSequence value) {
		this.internalTrailers.add(name, value);
		return this;
	}

	@Override
	public ResponseTrailers add(Header... trailers) {
		for(Header trailer : trailers) {
			this.internalTrailers.add(trailer.getHeaderName(), trailer.getHeaderValue());
		}
		return this;
	}
}

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

import io.netty.handler.codec.http.HttpHeaders;
import io.winterframework.mod.web.Header;
import io.winterframework.mod.web.ResponseTrailers;
import io.winterframework.mod.web.internal.netty.LinkedHttpHeaders;

/**
 * @author jkuhn
 *
 */
public class Http1xResponseTrailers implements ResponseTrailers {

	private final LinkedHttpHeaders internalTrailers;
	
	public Http1xResponseTrailers() {
		this.internalTrailers = new LinkedHttpHeaders();
	}

	HttpHeaders getInternalTrailers() {
		return this.internalTrailers;
	}
	
	@Override
	public ResponseTrailers add(String name, String value) {
		this.internalTrailers.addCharSequence((CharSequence)name, (CharSequence)value);
		return this;
	}

	@Override
	public ResponseTrailers add(CharSequence name, CharSequence value) {
		this.internalTrailers.addCharSequence(name, value);
		return this;
	}

	@Override
	public ResponseTrailers add(Header... trailers) {
		for(Header trailer : trailers) {
			this.internalTrailers.addCharSequence((CharSequence)trailer.getHeaderName(), (CharSequence)trailer.getHeaderValue());
		}
		return this;
	}

}

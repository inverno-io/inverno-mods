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
package io.inverno.mod.http.client.internal.multipart;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.Charsets;
import io.inverno.mod.base.net.URIs;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.OutboundDataSequencer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.Charset;
import java.util.function.Function;
import reactor.core.publisher.Flux;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@Bean(visibility = Bean.Visibility.PRIVATE)
public class UrlEncodedBodyEncoder implements MultipartEncoder<Parameter> {

	private final OutboundDataSequencer dataSequencer;

	public UrlEncodedBodyEncoder() {
		this.dataSequencer = new OutboundDataSequencer();
	}
	
	@Override
	public Flux<ByteBuf> encode(Flux<Parameter> data, Headers.ContentType contentType) {
		if(contentType == null || !contentType.getMediaType().equalsIgnoreCase(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)) {
			throw new IllegalArgumentException("Content type is not " + MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED);
		}
		
		return this.dataSequencer.sequence(data.map(new ParameterMapper(contentType.getCharset())));
	}
	
	private class ParameterMapper implements Function<Parameter, ByteBuf> {

		private final Charset charset;
		
		private boolean amp;

		public ParameterMapper(Charset charset) {
			this.charset = Charsets.orDefault(charset);
		}
		
		@Override
		public ByteBuf apply(Parameter parameter) {
			String encodeParameter = (this.amp ? "&" : "") + URIs.encodeQueryParameter(parameter.getName(), parameter.getValue(), this.charset);
			this.amp = true;
			return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(encodeParameter, this.charset));
		}
	}
}

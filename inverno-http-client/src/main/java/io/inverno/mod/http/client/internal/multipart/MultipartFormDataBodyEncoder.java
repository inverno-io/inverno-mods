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
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.OutboundDataSequencer;
import io.inverno.mod.http.client.Part;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpConstants;
import java.nio.charset.Charset;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A multipart/form-data payload encoder implementation as defined by <a href="https://tools.ietf.org/html/rfc7578">RFC 7578</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
@Bean( visibility = Bean.Visibility.PRIVATE )
public class MultipartFormDataBodyEncoder implements MultipartEncoder<Part<?>> {
	
	@Override
	public Flux<ByteBuf> encode(Flux<Part<?>> data, Headers.ContentType contentType) {
		if(contentType == null || !contentType.getMediaType().equalsIgnoreCase(MediaTypes.MULTIPART_FORM_DATA)) {
			throw new IllegalArgumentException("Content type is not " + MediaTypes.MULTIPART_FORM_DATA);
		}
		if(contentType.getBoundary() == null) {
			throw new IllegalArgumentException("Missing multipart form data boundary");
		}
		final PartMapper partMapper = new PartMapper(contentType.getBoundary(), Charsets.orDefault(contentType.getCharset()));
		
		return data
			.concatMap(part -> {
				if(part instanceof ResourcePart) {
					return Flux.from(((ResourcePart)part).getFileParts()).concatMap(partMapper);
				}
				return partMapper.apply(part);
			})
			.concatWith(partMapper.apply(ClosingPart.INSTANCE))
			.transform(new OutboundDataSequencer());
	}
	
	/**
	 * <p>
	 * A part mapper used to map a part to a payload data publisher.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	private static class PartMapper implements Function<Part<?>, Publisher<ByteBuf>> {

		private static final int CRLF_SHORT = (HttpConstants.CR << 8) | HttpConstants.LF;
		
		private static final int DASH_BOUNDARY_SHORT = '-' << 8 | '-';
		
		private final Charset charset;
		
		private final ByteBuf delimiter;

		private boolean encapsulation;
		
		/**
		 * <p>
		 * Creates a part mapper.
		 * </p>
		 * 
		 * @param boundary the multipart form data boundary
		 * @param charset  the charset
		 */
		public PartMapper(String boundary, Charset charset) {
			this.charset = Charsets.orDefault(charset);
			this.delimiter = Unpooled.wrappedBuffer(("--" + boundary).getBytes(this.charset));
		}
		
		@Override
		public Publisher<ByteBuf> apply(Part<?> part) {
			if(part == ClosingPart.INSTANCE) {
				return Mono.fromSupplier(() -> {
					ByteBuf buffer = Unpooled.buffer();
					
					ByteBufUtil.writeShortBE(buffer, CRLF_SHORT);
					buffer.writeBytes(this.delimiter.duplicate());
					ByteBufUtil.writeShortBE(buffer, DASH_BOUNDARY_SHORT);
					ByteBufUtil.writeShortBE(buffer, CRLF_SHORT);
					
					return buffer;
				});
			}
			else if(part instanceof AbstractDataPart) {
				AbstractDataPart<?> dataPart = (AbstractDataPart<?>)part;
				return Flux.concat(Mono.fromSupplier(() -> {
					ByteBuf buffer = Unpooled.buffer();
					if(this.encapsulation) {
						ByteBufUtil.writeShortBE(buffer, CRLF_SHORT);
					}
					else {
						this.encapsulation = true;
					}
					buffer.writeBytes(this.delimiter.duplicate());
					ByteBufUtil.writeShortBE(buffer, CRLF_SHORT);

					PartHeaders headers = dataPart.headers();
					if(!headers.contains(Headers.NAME_CONTENT_DISPOSITION)) {
						headers.set(Headers.NAME_CONTENT_DISPOSITION, dataPart.getContentDisposition(this.charset));
					}
					dataPart.headers().encode(buffer);
					headers.remove(Headers.NAME_CONTENT_DISPOSITION);
					ByteBufUtil.writeShortBE(buffer, CRLF_SHORT);
					return buffer;
				}), dataPart.getData());
			}
			else {
				throw new IllegalArgumentException("Invalid part which was not created with provided factory");
			}
		}
	}
}

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
package io.winterframework.mod.http.server.internal.multipart;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import io.winterframework.mod.base.converter.ObjectConverter;
import io.winterframework.mod.http.base.header.Header;
import io.winterframework.mod.http.server.Part;
import io.winterframework.mod.http.server.PartHeaders;
import io.winterframework.mod.http.server.RequestData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * <p>
 * Generic {@link Part} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class GenericPart implements Part {

	private String name;
	
	private String filename;

	private PartHeaders partHeaders;
	
	private Flux<ByteBuf> data;
	private FluxSink<ByteBuf> dataEmitter;
	
	private RequestData<ByteBuf> rawData;
	
	/**
	 * <p>
	 * Creates a part.
	 * </p>
	 * 
	 * @param parameterConverter a string object converter
	 * @param name               the part's name
	 * @param headers            the part's headers
	 */
	public GenericPart(ObjectConverter<String> parameterConverter, String name, Map<String, List<Header>> headers) {
		this(parameterConverter, name, null, headers);
	}
	
	/**
	 * <p>
	 * creates a file part.
	 * </p>
	 * 
	 * @param parameterConverter a string object converter
	 * @param name               the part's name
	 * @param filename           the part's file name
	 * @param headers            the part's headers
	 * @param contentType          the part's media type
	 * @param charset            the part's charset
	 * @param contentLength      the part's content length
	 */
	public GenericPart(ObjectConverter<String> parameterConverter, String name, String filename, Map<String, List<Header>> headers) {
		this.name = name;
		this.filename = filename;
		this.partHeaders = new GenericPartHeaders(headers, parameterConverter);
		
		this.data = Flux.<ByteBuf>create(emitter -> {
			this.dataEmitter = emitter;
		}).doOnDiscard(ByteBuf.class, ByteBuf::release);
	}
	
	/**
	 * <p>
	 * Returns the part's data sink.
	 * </p>
	 * 
	 * @return an optional returning the payload data sink or an empty optional if
	 *         the part has no body
	 */
	public Optional<FluxSink<ByteBuf>> getData() {
		return Optional.ofNullable(this.dataEmitter);
	}
	
	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public Optional<String> getFilename() {
		return Optional.ofNullable(this.filename);
	}
	
	@Override
	public PartHeaders headers() {
		return this.partHeaders;
	}
	
	// Emitted ByteBuf must be released 
	// But if nobody subscribe they are released
	@Override
	public RequestData<ByteBuf> raw() {
		if(this.rawData == null) {
			this.rawData = new GenericPartRawData();
		}
		return this.rawData;
	}
	
	private class GenericPartRawData implements RequestData<ByteBuf> {

		@Override
		public Publisher<ByteBuf> stream() {
			return GenericPart.this.data;
		}
	}
}

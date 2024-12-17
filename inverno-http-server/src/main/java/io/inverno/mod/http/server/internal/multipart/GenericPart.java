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
package io.inverno.mod.http.server.internal.multipart;

import io.inverno.mod.base.Charsets;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.InboundData;
import io.inverno.mod.http.base.header.Header;
import io.inverno.mod.http.server.Part;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * <p>
 * Generic {@link Part} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class GenericPart implements Part {

	private final String name;
	private final String filename;
	private final PartHeaders partHeaders;
	
	private final Sinks.Many<ByteBuf> dataSink;
	private final Flux<ByteBuf> data;

	private boolean subscribed;
	private boolean disposed;
	private InboundData<ByteBuf> rawData;
	private InboundData<CharSequence> stringData;
	
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
	 * @param parameterConverter the parameter converter
	 * @param name               the part's name
	 * @param filename           the part's file name
	 * @param headers            the part's headers
	 */
	public GenericPart(ObjectConverter<String> parameterConverter, String name, String filename, Map<String, List<Header>> headers) {
		this.name = name;
		this.filename = filename;
		this.partHeaders = new PartHeaders(headers, parameterConverter);
		
		this.dataSink = Sinks.many().unicast().onBackpressureBuffer();
		this.data = Flux.defer(() -> {
			if(this.disposed) {
				return Mono.error(new IllegalStateException("Part was disposed"));
			}
			return this.dataSink.asFlux()
				.doOnSubscribe(ign -> this.subscribed = true)
				.doOnDiscard(ByteBuf.class, ByteBuf::release);
		});
	}
	
	/**
	 * <p>
	 * Disposes the part.
	 * </p>
	 * 
	 * <p>
	 * This method delegates to {@link #dispose(java.lang.Throwable) } with a null error.
	 * </p>
	 */
	void dispose() {
		this.dispose(null);
	}
	
	/**
	 * <p>
	 * Disposes the part with the specified error.
	 * </p>
	 * 
	 * <p>
	 * This method drains received data if the part data publisher hasn't been subscribed.
	 * </p>
	 * 
	 * <p>
	 * A non-null error indicates that the enclosing exchange did not complete successfully and that the error should be emitted by the part data publisher.
	 * </p>
	 * 
	 * @param error an error or null
	 */
	void dispose(Throwable error) {
		if(!this.subscribed) {
			// Try to drain and release buffered data 
			// when the datasink was already subscribed data are released in doOnDiscard
			this.dataSink.asFlux().subscribe(
				ReferenceCounted::release,
				ex -> {
					// TODO Should be ignored but can be logged as debug or trace log
				}
			);
		}
		else if(error != null) {
			this.dataSink.tryEmitError(error);
		}
		else {
			this.dataSink.tryEmitComplete();
		}
		this.disposed = true;
	}
	
	/**
	 * <p>
	 * Returns the part data sink.
	 * </p>
	 *
	 * @return the part data sink
	 */
	Sinks.Many<ByteBuf> data() {
		return this.dataSink;
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
	public InboundData<ByteBuf> raw() {
		if(this.rawData == null) {
			this.rawData = new RawInboundData();
		}
		return this.rawData;
	}

	@Override
	public InboundData<CharSequence> string() {
		if(this.stringData == null) {
			this.stringData = new StringInboundData();
		}
		return this.stringData;
	}
	
	/**
	 * <p>
	 * Generic Part raw data.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	private class RawInboundData implements InboundData<ByteBuf> {

		@Override
		public Publisher<ByteBuf> stream() {
			return GenericPart.this.data;
		}
	}
	
	/**
	 * <p>
	 * Generic Part String data.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	private class StringInboundData implements InboundData<CharSequence> {

		@Override
		public Publisher<CharSequence> stream() {
			return GenericPart.this.data.map(buf -> {
				try {
					return buf.toString(Charsets.DEFAULT);
				}
				finally {
					buf.release();
				}
			});
		}
	}
}

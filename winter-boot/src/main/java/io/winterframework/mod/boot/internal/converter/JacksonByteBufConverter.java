/*
 * Copyright 2021 Jeremy KUHN
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
package io.winterframework.mod.boot.internal.converter;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.reactivestreams.Publisher;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.util.TokenBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Provide;
import io.winterframework.mod.base.converter.ConverterException;
import io.winterframework.mod.base.converter.JoinableEncoder;
import io.winterframework.mod.base.converter.ReactiveConverter;
import io.winterframework.mod.base.converter.SplittableDecoder;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
@Bean( name = "jsonByteBufConverter")
public class JacksonByteBufConverter implements @Provide ReactiveConverter<ByteBuf, Object>, SplittableDecoder<ByteBuf, Object>, JoinableEncoder<Object, ByteBuf> {

	private static final ByteBuf EMPTY_LAST_CHUNK = Unpooled.unreleasableBuffer(Unpooled.EMPTY_BUFFER);
	
	private static final Mono<ByteBuf> LAST_CHUNK_PUBLISHER = Mono.just(EMPTY_LAST_CHUNK);
	
	private ObjectMapper mapper;

	public JacksonByteBufConverter(ObjectMapper mapper) {
		this.mapper = mapper;
	}
	
	@Override
	public <T extends Object> Publisher<ByteBuf> encodeOne(Mono<T> data) {
		return data.map(t -> {
				try {
					return Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(this.mapper.writeValueAsBytes(t)));
				} 
				catch (JsonProcessingException e) {
					throw Exceptions.propagate(e);
				}
			});
	}
	
	@Override
	public <T extends Object> Publisher<ByteBuf> encodeMany(Flux<T> data) {
		return data.map(t -> {
				try {
					return Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(this.mapper.writeValueAsBytes(t)));
				} 
				catch (JsonProcessingException e) {
					throw Exceptions.propagate(e);
				}
			});
	}

	@Override
	public ByteBuf encode(Object data) {
		try {
			return Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(this.mapper.writeValueAsBytes(data)));
		} 
		catch (JsonProcessingException e) {
			throw new ConverterException("Error encoding data", e);
		}
	}

	@Override
	public <T extends Object> ByteBuf encodeList(List<T> data) {
		return this.encode(data);
	}

	@Override
	public <T extends Object> ByteBuf encodeSet(Set<T> data) {
		return this.encode(data);
	}

	@Override
	public <T extends Object> ByteBuf encodeArray(T[] data) {
		return this.encode(data);
	}
	
	@Override
	public <T> Mono<T> decodeOne(Publisher<ByteBuf> data, Class<T> type) {
		return this.decodeMany(data, type, true).single();
	}

	@Override
	public <T> Flux<T> decodeMany(Publisher<ByteBuf> data, Class<T> type) {
		return this.decodeMany(data, type, true);
	}

	private <T> Flux<T> decodeMany(Publisher<ByteBuf> data, Class<T> type, boolean scanRootArray) {
		// Performance wise, this might not be ideal because creating a flux is resource consuming
		// TODO assess performance and see whether it is interesting to optimize this
		return Flux.concat(data, LAST_CHUNK_PUBLISHER).scanWith(
				() -> {
					try {
						return new ObjectScanner<>(type, this.mapper, scanRootArray);
					}
					catch(IOException e) {
						throw Exceptions.propagate(e);
					}
				},
				(scanner, chunk) -> {
					try {
						if(chunk == EMPTY_LAST_CHUNK) {
							scanner.endOfInput();
						}
						else {
							scanner.feedInput(chunk);
						}
						return scanner;
					}
					catch (IOException e) {
						throw Exceptions.propagate(e);
					}
				}
			)
			.skip(1)
			.concatMap(scanner -> {
				try {
					List<T> objects = new LinkedList<>();
					T object = null;
					while( (object = scanner.nextObject()) != null) {
						objects.add(object);
					}
					return Flux.fromIterable(objects);
				} 
				catch (IOException e) {
					throw Exceptions.propagate(e);
				}
			});
	}
	
	@Override
	public <T> T decode(ByteBuf data, Class<T> type) {
		try {
			return this.mapper.readValue(ByteBufUtil.getBytes(data), type);
		} 
		catch (IOException e) {
			throw new ConverterException("Error decoding data", e);
		}
		finally {
			data.release();
		}
	}

	@Override
	public <T> List<T> decodeToList(ByteBuf data, Class<T> type) {
		try {
			ObjectScanner<T> scanner = new ObjectScanner<>(type, this.mapper, true);
			scanner.feedInput(data);
			scanner.endOfInput();
			
			List<T> objects = new LinkedList<>();
			T object = null;
			while( (object = scanner.nextObject()) != null) {
				objects.add(object);
			}
			return objects;
		} 
		catch (IOException e) {
			throw new ConverterException("Error decoding data", e);
		}
	}

	@Override
	public <T> Set<T> decodeToSet(ByteBuf data, Class<T> type) {
		try {
			ObjectScanner<T> scanner = new ObjectScanner<>(type, this.mapper, true);
			scanner.feedInput(data);
			scanner.endOfInput();
			
			Set<T> objects = new HashSet<>();
			T object = null;
			while( (object = scanner.nextObject()) != null) {
				objects.add(object);
			}
			return objects;
		} 
		catch (IOException e) {
			throw new ConverterException("Error decoding data", e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] decodeToArray(ByteBuf data, Class<T> type) {
		List<T> objects = this.decodeToList(data, type);
		return objects.toArray((T[])Array.newInstance(type, objects.size()));
	}
	
	private static class ObjectScanner<T> {
		
		private Class<T> type;
		
		private ObjectMapper mapper;
		
		private boolean scanRootArray;
		
		private JsonParser parser;
		
		private ByteArrayFeeder feeder;
		
		private DeserializationContext deserializationContext;

		private TokenBuffer tokenBuffer;
		
		public ObjectScanner(Class<T> type, ObjectMapper mapper, boolean scanRootArray) throws IOException {
			this.type = type;
			this.mapper = mapper;
			this.scanRootArray = scanRootArray;
			this.parser = this.mapper.getFactory().createNonBlockingByteArrayParser();
			this.feeder = (ByteArrayFeeder)this.parser.getNonBlockingInputFeeder();
			this.deserializationContext = this.mapper.getDeserializationContext();
			if (this.deserializationContext instanceof DefaultDeserializationContext) {
				this.deserializationContext = ((DefaultDeserializationContext) this.deserializationContext).createInstance(this.mapper.getDeserializationConfig(), this.parser, this.mapper.getInjectableValues());
			}
		}
		
		protected TokenBuffer getTokenBuffer() {
			if(this.tokenBuffer == null) {
				this.tokenBuffer = new TokenBuffer(this.parser, this.deserializationContext);
				this.tokenBuffer.forceUseOfBigDecimal(this.mapper.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS));
			}
			return this.tokenBuffer;
		}

		public void feedInput(ByteBuf chunk) throws IOException {
			try {
				if(chunk.hasArray()) {
					int length = chunk.readableBytes();
					int offset = chunk.arrayOffset() + chunk.readerIndex();
					this.feeder.feedInput(chunk.array(), offset, offset + length);
				}
				else {
					int length = chunk.readableBytes();
					byte[] chunkBytes = new byte[length];
					chunk.getBytes(chunk.readerIndex(), chunkBytes);
					this.feeder.feedInput(chunkBytes, 0, length);
				}
			}
			finally {
				chunk.release();
			}
		}
		
		public void endOfInput() {
			this.feeder.endOfInput();
		}
		
		public T nextObject() throws IOException {
			while (!this.parser.isClosed()) {
				JsonToken token = this.parser.nextToken();
				// TODO smile data format uses null to separate document
				// we actually know in advanced that we are dealing with that format so maybe we can provide another scanner implementation to make things explicit
				if(token == null || token == JsonToken.NOT_AVAILABLE) {
					// end of input
					break;
				}
				
				JsonStreamContext context = this.parser.getParsingContext();
				TokenBuffer currentTokenBuffer = this.getTokenBuffer();
				if(this.scanRootArray && context.inArray() && context.getParent().inRoot() && (token == JsonToken.START_ARRAY || token == JsonToken.END_ARRAY)) {
					continue;
				}
				
				currentTokenBuffer.copyCurrentEvent(this.parser);
				if( (context.inRoot() || (this.scanRootArray && context.inArray() && context.getParent().inRoot())) && (token.isScalarValue() || token.isStructEnd())) {
					try {
						return this.mapper.readValue(currentTokenBuffer.asParser(), this.type);
					}
					finally {
						this.tokenBuffer = null;
					}
				}
			}
			return null;
		}
	}
}

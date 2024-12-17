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
package io.inverno.mod.boot.converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.base.converter.ConverterException;
import io.inverno.mod.base.converter.JoinableEncoder;
import io.inverno.mod.base.converter.ReactiveConverter;
import io.inverno.mod.base.converter.SplittableDecoder;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.reactivestreams.Publisher;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * JSON String to Object converter backed by an {@link ObjectMapper}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see io.inverno.mod.base.converter.Converter Converter
 * @see ReactiveConverter
 * @see ObjectMapper
 */
@Bean( name = "jsonStringConverter" )
public class JacksonStringConverter implements @Provide ReactiveConverter<String, Object>, SplittableDecoder<String, Object>, JoinableEncoder<Object, String> {

	/**
	 * Unique String reference identifying the last chunk in a stream.
	 */
	@SuppressWarnings("StringOperationCanBeSimplified")
	private static final String LAST_CHUNK = new String();
	
	private static final Mono<String> LAST_CHUNK_PUBLISHER = Mono.just(LAST_CHUNK);
	
	private final ObjectMapper mapper;

	/**
	 * <p>
	 * Creates a JSON String converter.
	 * </p>
	 * 
	 * @param mapper a Jackson object mapper
	 */
	public JacksonStringConverter(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public <T> Publisher<String> encodeOne(Mono<T> value) {
		return value.map(this::encode);
	}

	@Override
	public <T> Publisher<String> encodeOne(Mono<T> value, Class<T> type) {
		return this.encodeOne(value, (Type)type);
	}

	@Override
	public <T> Publisher<String> encodeOne(Mono<T> value, Type type) {
		return value.map(t -> this.encode(t, type));
	}

	@Override
	public <T> Publisher<String> encodeMany(Flux<T> value) {
		return value.map(this::encode);
	}

	@Override
	public <T> Publisher<String> encodeMany(Flux<T> value, Class<T> type) {
		return this.encodeMany(value, (Type)type);
	}

	@Override
	public <T> Publisher<String> encodeMany(Flux<T> value, Type type) {
		return value.map(t -> this.encode(t, type));
	}
	
	@Override
	public <T> String encode(T value) throws ConverterException {
		try {
			return this.mapper.writeValueAsString(value);
		} 
		catch (JsonProcessingException e) {
			throw new ConverterException("Error encoding value", e);
		}
	}

	@Override
	public <T> String encode(T value, Class<T> type) throws ConverterException {
		return this.encode(value, (Type)type);
	}

	@Override
	public <T> String encode(T value, Type type) throws ConverterException {
		try {
			return this.mapper.writerFor(this.mapper.constructType(type)).writeValueAsString(value);
		} 
		catch (JsonProcessingException e) {
			throw new ConverterException("Error encoding value", e);
		}
	}

	@Override
	public <T> String encodeList(List<T> value) throws ConverterException {
		return this.encode(value);
	}

	@Override
	public <T> String encodeList(List<T> value, Class<T> type) throws ConverterException {
		return this.encode(value);
	}

	@Override
	public <T> String encodeList(List<T> value, Type type) throws ConverterException {
		return this.encode(value);
	}

	@Override
	public <T> String encodeSet(Set<T> value) throws ConverterException {
		return this.encode(value);
	}

	@Override
	public <T> String encodeSet(Set<T> value, Class<T> type) throws ConverterException {
		return this.encode(value);
	}

	@Override
	public <T> String encodeSet(Set<T> value, Type type) throws ConverterException {
		return this.encode(value);
	}

	@Override
	public <T> String encodeArray(T[] value) throws ConverterException {
		return this.encode(value);
	}

	@Override
	public <T> String encodeArray(T[] value, Class<T> type) throws ConverterException {
		return this.encode(value);
	}

	@Override
	public <T> String encodeArray(T[] value, Type type) throws ConverterException {
		return this.encode(value);
	}

	@Override
	public <T> Mono<T> decodeOne(Publisher<String> value, Class<T> type) {
		return this.<T>decodeMany(value, type, false).single();
	}

	@Override
	public <T> Mono<T> decodeOne(Publisher<String> value, Type type) {
		return this.<T>decodeMany(value, type, false).single();
	}

	@Override
	public <T> Flux<T> decodeMany(Publisher<String> value, Class<T> type) {
		return this.decodeMany(value, type, true);
	}

	@Override
	public <T> Flux<T> decodeMany(Publisher<String> value, Type type) {
		return this.decodeMany(value, type, true);
	}

	private <T> Flux<T> decodeMany(Publisher<String> value, Type type, boolean scanRootArray) {
		// Performance wise, this might not be ideal because creating a flux is resource consuming
		// TODO assess performance and see whether it is interesting to optimize this
		return Flux.concat(value, LAST_CHUNK_PUBLISHER).scanWith(
				() -> {
					try {
						return new ObjectScanner<T>(type, this.mapper, scanRootArray);
					}
					catch(IOException e) {
						throw Exceptions.propagate(e);
					}
				},
				(scanner, chunk) -> {
					try {
						// This is on purpose we want to compare the instance so we can differentiate it from the last chunk reference
						//noinspection StringEquality
						if(chunk == LAST_CHUNK) {
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
					T object;
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
	public <T> T decode(String value, Class<T> type) throws ConverterException {
		return this.decode(value, (Type)type);
	}

	@Override
	public <T> T decode(String value, Type type) throws ConverterException {
		try {
			return this.mapper.readerFor(this.mapper.constructType(type)).readValue(value);
		} 
		catch (IOException e) {
			throw new ConverterException("Error decoding value", e);
		}
	}

	@Override
	public <T> List<T> decodeToList(String value, Class<T> type) {
		return this.decodeToList(value, (Type)type);
	}

	@Override
	public <T> List<T> decodeToList(String value, Type type) {
		try {
			ObjectScanner<T> scanner = new ObjectScanner<>(type, this.mapper, true);
			scanner.feedInput(value);
			scanner.endOfInput();
			
			List<T> objects = new LinkedList<>();
			T object;
			while( (object = scanner.nextObject()) != null) {
				objects.add(object);
			}
			return objects;
		} 
		catch (IOException e) {
			throw new ConverterException("Error decoding value", e);
		}
	}

	@Override
	public <T> Set<T> decodeToSet(String value, Class<T> type) {
		return this.decodeToSet(value, (Type)type);
	}

	@Override
	public <T> Set<T> decodeToSet(String value, Type type) {
		try {
			ObjectScanner<T> scanner = new ObjectScanner<>(type, this.mapper, true);
			scanner.feedInput(value);
			scanner.endOfInput();
			
			Set<T> objects = new HashSet<>();
			T object;
			while( (object = scanner.nextObject()) != null) {
				objects.add(object);
			}
			return objects;
		} 
		catch (IOException e) {
			throw new ConverterException("Error decoding value", e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] decodeToArray(String value, Class<T> type) {
		List<T> objects = this.decodeToList(value, type);
		return objects.toArray((T[])Array.newInstance(type, objects.size()));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] decodeToArray(String value, Type type) {
		List<T> objects = this.decodeToList(value, type);
		
		if(type instanceof Class) {
			return objects.toArray((T[]) Array.newInstance((Class<T>)type, objects.size()));
		}
		else if(type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType)type;
			return objects.toArray((T[]) Array.newInstance((Class<T>)parameterizedType.getRawType(), objects.size()));
		}
		else {
			throw new ConverterException("Can't decode " + String.class.getCanonicalName() + " to array of " + type.getTypeName());
		}
	}

	/**
	 * <p>
	 * Object scanner used to decode objects from a stream of {@code ByteBufs}.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 *
	 * @param <T> the object type
	 */
	private static class ObjectScanner<T> {
		
		private final ObjectMapper mapper;
		private final ObjectReader reader;
		
		private final boolean scanRootArray;
		
		private final JsonParser parser;
		
		private final ByteArrayFeeder feeder;
		
		private DeserializationContext deserializationContext;

		private TokenBuffer tokenBuffer;
		
		public ObjectScanner(Type type, ObjectMapper mapper, boolean scanRootArray) throws IOException {
			this.mapper = mapper;
			this.reader = this.mapper.readerFor(this.mapper.constructType(type));
			this.scanRootArray = scanRootArray;
			this.parser = this.mapper.getFactory().createNonBlockingByteArrayParser();
			this.feeder = (ByteArrayFeeder)this.parser.getNonBlockingInputFeeder();
			this.deserializationContext = this.mapper.getDeserializationContext();
			if (this.deserializationContext instanceof DefaultDeserializationContext) {
				this.deserializationContext = ((DefaultDeserializationContext) this.deserializationContext).createInstance(this.mapper.getDeserializationConfig(), this.parser, this.mapper.getInjectableValues());
			}
		}
		
		@SuppressWarnings("unused")
		public ObjectScanner(Class<T> type, ObjectMapper mapper, boolean scanRootArray) throws IOException {
			this((Type)type, mapper, scanRootArray);
		}
		
		private TokenBuffer getTokenBuffer() {
			if(this.tokenBuffer == null) {
				this.tokenBuffer = new TokenBuffer(this.parser, this.deserializationContext);
				this.tokenBuffer.forceUseOfBigDecimal(this.mapper.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS));
			}
			return this.tokenBuffer;
		}

		public void feedInput(String chunk) throws IOException {
			byte[] bytes = chunk.getBytes();
			this.feeder.feedInput(bytes, 0, bytes.length);
		}
		
		public void endOfInput() {
			this.feeder.endOfInput();
		}
		
		public T nextObject() throws IOException {
			while (!this.parser.isClosed()) {
				JsonToken token = this.parser.nextToken();
				// TODO smile value format uses null to separate document
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
						return this.reader.readValue(currentTokenBuffer.asParser());
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

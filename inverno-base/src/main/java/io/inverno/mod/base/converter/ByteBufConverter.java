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
package io.inverno.mod.base.converter;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.inverno.mod.base.Charsets;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A {@link Converter} that encodes objects to {@link ByteBuf} and decodes
 * {@link ByteBuf} to objects.
 * </p>
 * 
 * <p>
 * This implementation relies on a String {@link ObjectConverter} to convert the
 * string representation of an object from/to ByteBufs.
 * </p>
 * 
 * <p>
 * This converter is an object converter and as such it can convert collection
 * of objects using a customizable separator.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ObjectConverter
 * @see ReactiveConverter
 */
public class ByteBufConverter implements ReactiveConverter<ByteBuf, Object>, ObjectConverter<ByteBuf> {

	/**
	 * The default array/list separator
	 */
	public static final byte DEFAULT_ARRAY_LIST_SEPARATOR = ',';
	
	private static final ByteBuf EMPTY_LAST_CHUNK = Unpooled.unreleasableBuffer(Unpooled.EMPTY_BUFFER);
	
	private static final Mono<ByteBuf> LAST_CHUNK_PUBLISHER = Mono.just(EMPTY_LAST_CHUNK);
	
	private final ObjectConverter<String> stringConverter;
	
	private byte arrayListSeparator;
	
	private Charset charset;

	/**
	 * <p>
	 * Creates a ByteBuf converter backed by the specified string converter, with default charset and array/list separator.
	 * </p>
	 * 
	 * @param stringConverter a string converter
	 */
	public ByteBufConverter(ObjectConverter<String> stringConverter) {
		this(stringConverter, Charsets.DEFAULT, DEFAULT_ARRAY_LIST_SEPARATOR);
	}

	/**
	 * <p>
	 * Creates a ByteBuf converter backed by the specified string converter, with
	 * specified charset and array/list separator.
	 * </p>
	 * 
	 * @param stringConverter    A string converter
	 * @param charset            the charset
	 * @param arrayListSeparator the array/list separator
	 */
	public ByteBufConverter(ObjectConverter<String> stringConverter, Charset charset, byte arrayListSeparator) {
		this.stringConverter = stringConverter;
		this.charset = charset;
		this.arrayListSeparator = arrayListSeparator;
	}

	/**
	 * <p>
	 * Returns the charset used to serialize/deserialize to/from ByteBuf.
	 * </p>
	 * 
	 * @return a charset
	 */
	public Charset getCharset() {
		return charset;
	}

	/**
	 * <p>
	 * Sets the charset used to serialize/deserialize to/from ByteBuf.
	 * </p>
	 * 
	 * @param charset a charset
	 */
	public void setCharset(Charset charset) {
		this.charset = charset;
	}
	
	/**
	 * <p>
	 * Returns the array/list separator used to convert lists and arrays.
	 * </p>
	 * 
	 * @return an array/list separator
	 */
	public byte getArrayListSeparator() {
		return arrayListSeparator;
	}
	
	/**
	 * <p>
	 * Sets the array/list separator used to convert lists and arrays.
	 * </p>
	 * 
	 * @param arrayListSeparator an array/list separator
	 */
	public void setArrayListSeparator(byte arrayListSeparator) {
		this.arrayListSeparator = arrayListSeparator;
	}
	
	@Override
	public <T> Mono<T> decodeOne(Publisher<ByteBuf> value, Class<T> type) {
		return this.decodeOne(value, (Type)type);
	}
	
	@Override
	public <T> Mono<T> decodeOne(Publisher<ByteBuf> value, Type type) {
		return Flux.from(value).reduceWith(() -> Unpooled.unreleasableBuffer(Unpooled.buffer()), (acc, chunk) -> {
			try {
				return acc.writeBytes(chunk);
			}
			finally {
				chunk.release();
			}
		}).map(buffer -> this.decode(buffer, type));
	}

	@Override
	public <T> Flux<T> decodeMany(Publisher<ByteBuf> value, Class<T> type) {
		return this.decodeMany(value, (Type)type);
	}

	@Override
	public <T> Flux<T> decodeMany(Publisher<ByteBuf> value, Type type) {
		return Flux.concat(value, LAST_CHUNK_PUBLISHER).scanWith(
				() -> new ObjectScanner<T>(type),
				(scanner, chunk) -> {
					if(chunk == EMPTY_LAST_CHUNK) {
						scanner.endOfInput();
					}
					else {
						scanner.feedInput(chunk);
					}
					return scanner;
				}
			)
			.skip(1)
			.concatMap(scanner -> {
				List<T> objects = new LinkedList<>();
				T object = null;
				while( (object = scanner.nextObject()) != null) {
					objects.add(object);
				}
				return Flux.fromIterable(objects);
			});
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T decode(ByteBuf value, Class<T> type) {
		if(value == null) {
			return null;
		}
		if(ByteBuf.class.equals(type)) {
			return (T) value;
		}
		if(type.isArray()) {
			return (T)this.decodeToArray(value, type.getComponentType());
		}
		try {
			return this.stringConverter.decode(value.toString(this.charset), type);
		}
		finally {
			value.release();
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T decode(ByteBuf value, Type type) throws ConverterException {
		if(value == null) {
			return null;
		}
		
		if(type instanceof Class) {
			return this.decode(value, (Class<T>)type);
		}
		else if(type instanceof GenericArrayType) {
			return (T) this.decodeToArray(value, ((GenericArrayType)type).getGenericComponentType());
		}
		else if(type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = ((ParameterizedType)type);
			if(parameterizedType.getActualTypeArguments().length == 1) {
				if(parameterizedType.getRawType() instanceof Class) {
					Class<?> rawType = (Class<?>)parameterizedType.getRawType();
					Type typeArgument = parameterizedType.getActualTypeArguments()[0];
					if(rawType.equals(Collection.class) || rawType.equals(List.class)) {
						return (T) this.decodeToList(value, typeArgument);
					}
					else if(rawType.equals(Set.class)) {
						return (T) this.decodeToSet(value, typeArgument);
					}
				}
			}
		}
		try {
			return this.stringConverter.decode(value.toString(this.charset), type);
		}
		finally {
			value.release();
		}
	}

	private <T> void decodeToCollection(ByteBuf value, Type type, Collection<T> result) {
		ObjectScanner<T> scanner = new ObjectScanner<>(type);
		scanner.feedInput(value);
		scanner.endOfInput();
		
		T object = null;
		while( (object = scanner.nextObject()) != null) {
			result.add(object);
		}
	}
	
	@Override
	public <T> List<T> decodeToList(ByteBuf value, Class<T> type) {
		return this.decodeToList(value, (Type)type);
	}
	
	@Override
	public <T> List<T> decodeToList(ByteBuf value, Type type) {
		List<T> result = new ArrayList<>();
		this.decodeToCollection(value, type, result);
		return result;
	}

	@Override
	public <T> Set<T> decodeToSet(ByteBuf value, Class<T> type) {
		return this.decodeToSet(value, (Type)type);
	}
	
	@Override
	public <T> Set<T> decodeToSet(ByteBuf value, Type type) {
		Set<T> result = new HashSet<>();
		this.decodeToCollection(value, type, result);
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] decodeToArray(ByteBuf value, Class<T> type) {
		List<T> result = new LinkedList<>();
		this.decodeToCollection(value, type, result);
		return result.toArray((T[]) Array.newInstance(type, result.size()));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] decodeToArray(ByteBuf value, Type type) {
		List<T> result = new LinkedList<>();
		this.decodeToCollection(value, type, result);
		if(type instanceof Class) {
			return result.toArray((T[]) Array.newInstance((Class<T>)type, result.size()));
		}
		else if(type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType)type;
			return result.toArray((T[]) Array.newInstance((Class<T>)parameterizedType.getRawType(), result.size()));
		}
		else {
			throw new ConverterException("Can't decode " + String.class.getCanonicalName() + " to array of " + type.getTypeName());
		}
	}
	
	@Override
	public Byte decodeByte(ByteBuf value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return this.stringConverter.decodeByte(value.toString(this.charset));
	}

	@Override
	public Short decodeShort(ByteBuf value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return this.stringConverter.decodeShort(value.toString(this.charset));
	}

	@Override
	public Integer decodeInteger(ByteBuf value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return this.stringConverter.decodeInteger(value.toString(this.charset));
	}

	@Override
	public Long decodeLong(ByteBuf value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return this.stringConverter.decodeLong(value.toString(this.charset));
	}

	@Override
	public Float decodeFloat(ByteBuf value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return this.stringConverter.decodeFloat(value.toString(this.charset));
	}

	@Override
	public Double decodeDouble(ByteBuf value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return this.stringConverter.decodeDouble(value.toString(this.charset));
	}

	@Override
	public Character decodeCharacter(ByteBuf value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return this.stringConverter.decodeCharacter(value.toString(this.charset));
	}

	@Override
	public Boolean decodeBoolean(ByteBuf value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return this.stringConverter.decodeBoolean(value.toString(this.charset));
	}

	@Override
	public String decodeString(ByteBuf value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return this.stringConverter.decodeString(value.toString(this.charset));
	}

	@Override
	public BigInteger decodeBigInteger(ByteBuf value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return this.stringConverter.decodeBigInteger(value.toString(this.charset));
	}

	@Override
	public BigDecimal decodeBigDecimal(ByteBuf value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return this.stringConverter.decodeBigDecimal(value.toString(this.charset));
	}

	@Override
	public LocalDate decodeLocalDate(ByteBuf value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return this.stringConverter.decodeLocalDate(value.toString(this.charset));
	}

	@Override
	public LocalDateTime decodeLocalDateTime(ByteBuf value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return this.stringConverter.decodeLocalDateTime(value.toString(this.charset));
	}

	@Override
	public ZonedDateTime decodeZonedDateTime(ByteBuf value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return this.stringConverter.decodeZonedDateTime(value.toString(this.charset));
	}

	@Override
	public Currency decodeCurrency(ByteBuf value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return this.stringConverter.decodeCurrency(value.toString(this.charset));
	}

	@Override
	public Locale decodeLocale(ByteBuf value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return this.stringConverter.decodeLocale(value.toString(this.charset));
	}

	@Override
	public File decodeFile(ByteBuf value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return this.stringConverter.decodeFile(value.toString(this.charset));
	}

	@Override
	public Path decodePath(ByteBuf value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return this.stringConverter.decodePath(value.toString(this.charset));
	}

	@Override
	public URI decodeURI(ByteBuf value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return this.stringConverter.decodeURI(value.toString(this.charset));
	}

	@Override
	public URL decodeURL(ByteBuf value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return this.stringConverter.decodeURL(value.toString(this.charset));
	}

	@Override
	public Pattern decodePattern(ByteBuf value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return this.stringConverter.decodePattern(value.toString(this.charset));
	}

	@Override
	public InetAddress decodeInetAddress(ByteBuf value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return this.stringConverter.decodeInetAddress(value.toString(this.charset));
	}

	@Override
	public Class<?> decodeClass(ByteBuf value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return this.stringConverter.decodeClass(value.toString(this.charset));
	}

	@Override
	public <T> Publisher<ByteBuf> encodeOne(Mono<T> value) {
		return value.map(this::encode);
	}
	
	@Override
	public <T> Publisher<ByteBuf> encodeOne(Mono<T> value, Class<T> type) {
		return this.encodeOne(value, (Type)type);
	}
	
	@Override
	public <T> Publisher<ByteBuf> encodeOne(Mono<T> value, Type type) {
		return value.map(element -> this.encode(element, type));
	}

	@Override
	public <T> Publisher<ByteBuf> encodeMany(Flux<T> value) {
		return value.map(this::encode);
	}
	
	@Override
	public <T> Publisher<ByteBuf> encodeMany(Flux<T> value, Class<T> type) {
		return this.encodeMany(value, (Type)type);
	}
	
	@Override
	public <T> Publisher<ByteBuf> encodeMany(Flux<T> value, Type type) {
		return value.map(element -> this.encode(element, type));
	}

	@Override
	public <T> ByteBuf encode(T value) {
		if(value == null) {
			return null;
		}
		if(ByteBuf.class.isAssignableFrom(value.getClass())) {
			return (ByteBuf)value;
		}
		if(value.getClass().isArray()) {
			return this.encodeArray((Object[])value);
		}
		if(Collection.class.isAssignableFrom(value.getClass())) {
			return this.encodeCollection((Collection<?>)value);
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value), this.charset));
	}
	
	@Override
	public <T> ByteBuf encode(T value, Class<T> type) throws ConverterException {
		if(value == null) {
			return null;
		}
		if(ByteBuf.class.equals(type)) {
			return (ByteBuf) value;
		}
		if(type.isArray()) {
			return this.encodeArray((Object[])value, type.getComponentType());
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value, type), this.charset));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> ByteBuf encode(T value, Type type) throws ConverterException {
		if(value == null) {
			return null;
		}
		if(type instanceof Class) {
			return this.encode(value, (Class<T>)type);
		}
		else if(type instanceof GenericArrayType) {
			return this.encodeArray((Object[])value, ((GenericArrayType)type).getGenericComponentType());
		}
		else if(type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = ((ParameterizedType)type);
			if(parameterizedType.getActualTypeArguments().length == 1) {
				if(parameterizedType.getRawType() instanceof Class) {
					Class<?> rawType = (Class<?>)parameterizedType.getRawType();
					Type typeArgument = parameterizedType.getActualTypeArguments()[0];
					if(rawType.equals(Collection.class) || rawType.equals(List.class)) {
						return this.encodeList((List<T>)value, typeArgument);
					}
					else if(rawType.equals(Set.class)) {
						return this.encodeSet((Set<T>)value, typeArgument);
					}
				}
			}
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value), this.charset));
		
	}
	
	private <T> ByteBuf encodeCollection(Collection<T> value) {
		ByteBuf[] buffers = new ByteBuf[value.size()];
		int i = 0;
		for(Iterator<T> valueIterator = value.iterator();valueIterator.hasNext();i++) {
			buffers[i] = this.encode(valueIterator.next());
			if(valueIterator.hasNext()) {
				buffers[i].writeByte(this.arrayListSeparator);
			}
		}
		return Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(buffers));
	}
	
	private <T> ByteBuf encodeCollection(Collection<T> value, Type type) {
		ByteBuf[] buffers = new ByteBuf[value.size()];
		int i = 0;
		for(Iterator<T> valueIterator = value.iterator();valueIterator.hasNext();i++) {
			buffers[i] = this.encode(valueIterator.next(), type);
			if(valueIterator.hasNext()) {
				buffers[i].writeByte(this.arrayListSeparator);
			}
		}
		return Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(buffers));
	}
	
	@Override
	public <T> ByteBuf encodeList(List<T> value) {
		return this.encodeCollection(value);
	}
	
	@Override
	public <T> ByteBuf encodeList(List<T> value, Class<T> type) {
		return this.encodeList(value, type);
	}
	
	@Override
	public <T> ByteBuf encodeList(List<T> value, Type type) {
		return this.encodeCollection(value, type);
	}

	@Override
	public <T> ByteBuf encodeSet(Set<T> value) {
		return this.encodeCollection(value);
	}

	@Override
	public <T> ByteBuf encodeSet(Set<T> value, Class<T> type) {
		return this.encodeCollection(value, type);
	}
	
	@Override
	public <T> ByteBuf encodeSet(Set<T> value, Type type) {
		return this.encodeCollection(value, type);
	}
	
	@Override
	public <T> ByteBuf encodeArray(T[] value) {
		ByteBuf[] buffers = new ByteBuf[value.length];
		for(int i = 0;i<value.length;i++) {
			buffers[i] = this.encode(value[i]);
			if(i < value.length - 1) {
				buffers[i].writeByte(this.arrayListSeparator);
			}
		}
		return Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(buffers));
	}
	
	@Override
	public <T> ByteBuf encodeArray(T[] value, Class<T> type) {
		return this.encodeArray(value, (Type)type);
	}
	
	@Override
	public <T> ByteBuf encodeArray(T[] value, Type type) {
		ByteBuf[] buffers = new ByteBuf[value.length];
		for(int i = 0;i<value.length;i++) {
			buffers[i] = this.encode(value[i], type);
			if(i < value.length - 1) {
				buffers[i].writeByte(this.arrayListSeparator);
			}
		}
		return Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(buffers));
	}
	
	@Override
	public ByteBuf encode(Byte value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value), this.charset));
	}

	@Override
	public ByteBuf encode(Short value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value), this.charset));
	}

	@Override
	public ByteBuf encode(Integer value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value), this.charset));
	}

	@Override
	public ByteBuf encode(Long value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value), this.charset));
	}

	@Override
	public ByteBuf encode(Float value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value), this.charset));
	}

	@Override
	public ByteBuf encode(Double value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value), this.charset));
	}

	@Override
	public ByteBuf encode(Character value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value), this.charset));
	}

	@Override
	public ByteBuf encode(Boolean value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value), this.charset));
	}

	@Override
	public ByteBuf encode(String value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value), this.charset));
	}

	@Override
	public ByteBuf encode(BigInteger value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value), this.charset));
	}

	@Override
	public ByteBuf encode(BigDecimal value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value), this.charset));
	}

	@Override
	public ByteBuf encode(LocalDate value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value), this.charset));
	}

	@Override
	public ByteBuf encode(LocalDateTime value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value), this.charset));
	}

	@Override
	public ByteBuf encode(ZonedDateTime value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value), this.charset));
	}

	@Override
	public ByteBuf encode(Currency value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value), this.charset));
	}

	@Override
	public ByteBuf encode(Locale value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value), this.charset));
	}

	@Override
	public ByteBuf encode(File value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value), this.charset));
	}

	@Override
	public ByteBuf encode(Path value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value), this.charset));
	}

	@Override
	public ByteBuf encode(URI value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value), this.charset));
	}

	@Override
	public ByteBuf encode(URL value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value), this.charset));
	}

	@Override
	public ByteBuf encode(Pattern value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value), this.charset));
	}

	@Override
	public ByteBuf encode(InetAddress value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value), this.charset));
	}

	@Override
	public ByteBuf encode(Class<?> value) throws ConverterException {
		if(value == null) {
			return null;
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(value), this.charset));
	}

	private class ObjectScanner<T> {
		
		private Type type;
		
		private boolean endOfInput;
		
		private int inputIndex;
		private ByteBuf inputBuffer;
		private ByteBuf keepBuffer;
		
		public ObjectScanner(Type type) {
			this.type = type;
		}
		
		@SuppressWarnings("unused")
		public ObjectScanner(Class<T> type) {
			this((Type)type);
		}
		
		public void feedInput(ByteBuf chunk) {
			if(this.inputBuffer != null) {
				throw new IllegalStateException("Undecoded bytes remaining");
			}
			
			if(this.keepBuffer != null && this.keepBuffer.isReadable()) {
				this.inputBuffer = Unpooled.wrappedBuffer(this.keepBuffer, chunk);
			}
			else {
				this.inputBuffer = chunk;
			}
			this.inputIndex= this.inputBuffer.readerIndex();
		}
		
		public void endOfInput() {
			this.endOfInput = true;
		}
		
		public T nextObject() {
			if(this.inputBuffer == null) {
				if(this.endOfInput && this.keepBuffer != null && this.keepBuffer.isReadable()) {
					try {
						return ByteBufConverter.this.decode(this.keepBuffer, this.type);
					}
					finally {
						this.keepBuffer.release();
						this.keepBuffer = null;
					}
				}
				return null;
			}
			
			while(this.inputBuffer.isReadable()) {
				byte nextByte = this.inputBuffer.readByte();
				if(nextByte == ByteBufConverter.this.arrayListSeparator) {
					T object = ByteBufConverter.this.decode(this.inputBuffer.retainedSlice(this.inputIndex, this.inputBuffer.readerIndex() - this.inputIndex -1), this.type);
					this.inputIndex = this.inputBuffer.readerIndex();
					return object;
				}
			}
			
			try {
				if(this.inputIndex < this.inputBuffer.readerIndex()) {
					if(this.endOfInput) {
						try {
							return ByteBufConverter.this.decode(this.inputBuffer.retainedSlice(this.inputIndex, this.inputBuffer.readerIndex() - this.inputIndex), this.type);
						}
						finally {
							if(this.keepBuffer != null) {
								this.keepBuffer.release();
								this.keepBuffer = null;
							}
						}
					}
					else {
						if(this.keepBuffer != null) {
							this.keepBuffer.discardReadBytes();
						}
						else {
							this.keepBuffer = Unpooled.unreleasableBuffer(Unpooled.buffer(this.inputBuffer.readerIndex() - this.inputIndex));
						}
						this.keepBuffer.writeBytes(this.inputBuffer.slice(this.inputIndex, this.inputBuffer.readerIndex() - this.inputIndex));
					}
				}
				else {
					this.keepBuffer.clear();
				}
				return null;
			}
			finally {
				this.inputBuffer.release();
				this.inputBuffer = null;
			}
		}
	}
}

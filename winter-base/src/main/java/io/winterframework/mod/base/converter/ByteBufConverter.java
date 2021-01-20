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
package io.winterframework.mod.base.converter;

import java.io.File;
import java.lang.reflect.Array;
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
import io.winterframework.mod.base.Charsets;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Decode/Encode primitive objects from/to their respective String representation serialized in ByteBuf.
 * 
 * @author jkuhn
 *
 */
public class ByteBufConverter implements ReactiveConverter<ByteBuf, Object>, ObjectConverter<ByteBuf> {

	private static final ByteBuf EMPTY_LAST_CHUNK = Unpooled.unreleasableBuffer(Unpooled.EMPTY_BUFFER);
	
	private static final Mono<ByteBuf> LAST_CHUNK_PUBLISHER = Mono.just(EMPTY_LAST_CHUNK);
	
	private static final byte DEFAULT_ARRAY_LIST_SEPARATOR = ',';
	
	private ObjectConverter<String> stringConverter;
	
	private byte arrayListSeparator;
	
	private Charset charset;
	
	public ByteBufConverter(ObjectConverter<String> stringConverter) {
		this(stringConverter, Charsets.DEFAULT, DEFAULT_ARRAY_LIST_SEPARATOR);
	}
	
	public ByteBufConverter(ObjectConverter<String> stringConverter, Charset charset, byte arrayListSeparator) {
		this.stringConverter = stringConverter;
		this.charset = charset;
		this.arrayListSeparator = arrayListSeparator;
	}

	public Charset getCharset() {
		return charset;
	}
	
	public void setCharset(Charset charset) {
		this.charset = charset;
	}
	
	public byte getArrayListSeparator() {
		return arrayListSeparator;
	}
	
	public void setArrayListSeparator(byte arrayListSeparator) {
		this.arrayListSeparator = arrayListSeparator;
	}
	
	@Override
	public <T> Mono<T> decodeOne(Publisher<ByteBuf> data, Class<T> type) {
		return Flux.from(data).reduceWith(() -> Unpooled.unreleasableBuffer(Unpooled.buffer()), (acc, chunk) -> {
			try {
				return acc.writeBytes(chunk);
			}
			finally {
				chunk.release();
			}
		}).map(buffer -> this.decode(buffer, type));
	}

	@Override
	public <T> Flux<T> decodeMany(Publisher<ByteBuf> data, Class<T> type) {
		return Flux.concat(data, LAST_CHUNK_PUBLISHER).scanWith(
				() -> new ObjectScanner<>(type),
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
	public <T> T decode(ByteBuf data, Class<T> type) {
		if(data == null) {
			return null;
		}
		if (ByteBuf.class.equals(type)) {
			return (T) data;
		}
		if(type.isArray()) {
			return (T)this.decodeToArray(data, type.getComponentType());
		}
		try {
			return this.stringConverter.decode(data.toString(this.charset), type);
		}
		finally {
			data.release();
		}
	}

	private <T> void decodeToCollection(ByteBuf data, Class<T> type, Collection<T> result) {
		ObjectScanner<T> scanner = new ObjectScanner<>(type);
		scanner.feedInput(data);
		scanner.endOfInput();
		
		T object = null;
		while( (object = scanner.nextObject()) != null) {
			result.add(object);
		}
	}
	
	@Override
	public <T> List<T> decodeToList(ByteBuf data, Class<T> type) {
		List<T> result = new LinkedList<>();
		this.decodeToCollection(data, type, result);
		return result;
	}

	@Override
	public <T> Set<T> decodeToSet(ByteBuf data, Class<T> type) {
		Set<T> result = new HashSet<>();
		this.decodeToCollection(data, type, result);
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] decodeToArray(ByteBuf data, Class<T> type) {
		List<T> result = new LinkedList<>();
		this.decodeToCollection(data, type, result);
		return result.toArray((T[]) Array.newInstance(type, result.size()));
	}
	
	@Override
	public Byte decodeByte(ByteBuf data) throws ConverterException {
		if(data == null) {
			return null;
		}
		return this.stringConverter.decodeByte(data.toString(this.charset));
	}

	@Override
	public Short decodeShort(ByteBuf data) throws ConverterException {
		if(data == null) {
			return null;
		}
		return this.stringConverter.decodeShort(data.toString(this.charset));
	}

	@Override
	public Integer decodeInteger(ByteBuf data) throws ConverterException {
		if(data == null) {
			return null;
		}
		return this.stringConverter.decodeInteger(data.toString(this.charset));
	}

	@Override
	public Long decodeLong(ByteBuf data) throws ConverterException {
		if(data == null) {
			return null;
		}
		return this.stringConverter.decodeLong(data.toString(this.charset));
	}

	@Override
	public Float decodeFloat(ByteBuf data) throws ConverterException {
		if(data == null) {
			return null;
		}
		return this.stringConverter.decodeFloat(data.toString(this.charset));
	}

	@Override
	public Double decodeDouble(ByteBuf data) throws ConverterException {
		if(data == null) {
			return null;
		}
		return this.stringConverter.decodeDouble(data.toString(this.charset));
	}

	@Override
	public Character decodeCharacter(ByteBuf data) throws ConverterException {
		if(data == null) {
			return null;
		}
		return this.stringConverter.decodeCharacter(data.toString(this.charset));
	}

	@Override
	public Boolean decodeBoolean(ByteBuf data) throws ConverterException {
		if(data == null) {
			return null;
		}
		return this.stringConverter.decodeBoolean(data.toString(this.charset));
	}

	@Override
	public String decodeString(ByteBuf data) throws ConverterException {
		if(data == null) {
			return null;
		}
		return this.stringConverter.decodeString(data.toString(this.charset));
	}

	@Override
	public BigInteger decodeBigInteger(ByteBuf data) throws ConverterException {
		if(data == null) {
			return null;
		}
		return this.stringConverter.decodeBigInteger(data.toString(this.charset));
	}

	@Override
	public BigDecimal decodeBigDecimal(ByteBuf data) throws ConverterException {
		if(data == null) {
			return null;
		}
		return this.stringConverter.decodeBigDecimal(data.toString(this.charset));
	}

	@Override
	public LocalDate decodeLocalDate(ByteBuf data) throws ConverterException {
		if(data == null) {
			return null;
		}
		return this.stringConverter.decodeLocalDate(data.toString(this.charset));
	}

	@Override
	public LocalDateTime decodeLocalDateTime(ByteBuf data) throws ConverterException {
		if(data == null) {
			return null;
		}
		return this.stringConverter.decodeLocalDateTime(data.toString(this.charset));
	}

	@Override
	public ZonedDateTime decodeZonedDateTime(ByteBuf data) throws ConverterException {
		if(data == null) {
			return null;
		}
		return this.stringConverter.decodeZonedDateTime(data.toString(this.charset));
	}

	@Override
	public Currency decodeCurrency(ByteBuf data) throws ConverterException {
		if(data == null) {
			return null;
		}
		return this.stringConverter.decodeCurrency(data.toString(this.charset));
	}

	@Override
	public Locale decodeLocale(ByteBuf data) throws ConverterException {
		if(data == null) {
			return null;
		}
		return this.stringConverter.decodeLocale(data.toString(this.charset));
	}

	@Override
	public File decodeFile(ByteBuf data) throws ConverterException {
		if(data == null) {
			return null;
		}
		return this.stringConverter.decodeFile(data.toString(this.charset));
	}

	@Override
	public Path decodePath(ByteBuf data) throws ConverterException {
		if(data == null) {
			return null;
		}
		return this.stringConverter.decodePath(data.toString(this.charset));
	}

	@Override
	public URI decodeURI(ByteBuf data) throws ConverterException {
		if(data == null) {
			return null;
		}
		return this.stringConverter.decodeURI(data.toString(this.charset));
	}

	@Override
	public URL decodeURL(ByteBuf data) throws ConverterException {
		if(data == null) {
			return null;
		}
		return this.stringConverter.decodeURL(data.toString(this.charset));
	}

	@Override
	public Pattern decodePattern(ByteBuf data) throws ConverterException {
		if(data == null) {
			return null;
		}
		return this.stringConverter.decodePattern(data.toString(this.charset));
	}

	@Override
	public InetAddress decodeInetAddress(ByteBuf data) throws ConverterException {
		if(data == null) {
			return null;
		}
		return this.stringConverter.decodeInetAddress(data.toString(this.charset));
	}

	@Override
	public Class<?> decodeClass(ByteBuf data) throws ConverterException {
		if(data == null) {
			return null;
		}
		return this.stringConverter.decodeClass(data.toString(this.charset));
	}

	@Override
	public <T> Publisher<ByteBuf> encodeOne(Mono<T> data) {
		return data.map(this::encode);
	}

	@Override
	public <T> Publisher<ByteBuf> encodeMany(Flux<T> data) {
		return data.map(this::encode);
	}

	@Override
	public <T> ByteBuf encode(T data) {
		if(data == null) {
			return null;
		}
		if(ByteBuf.class.isAssignableFrom(data.getClass())) {
			return (ByteBuf)data;
		}
		if(data.getClass().isArray()) {
			return this.encodeArray((Object[])data);
		}
		if(Collection.class.isAssignableFrom(data.getClass())) {
			return this.encodeCollection((Collection<?>)data);
		}
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.stringConverter.encode(data), this.charset));
	}
	
	private <T> ByteBuf encodeCollection(Collection<T> data) {
		ByteBuf[] buffers = new ByteBuf[data.size()];
		int i = 0;
		for(Iterator<T> dataIterator = data.iterator();dataIterator.hasNext();i++) {
			buffers[i] = this.encode(dataIterator.next());
			if(dataIterator.hasNext()) {
				buffers[i].writeByte(this.arrayListSeparator);
			}
		}
		return Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(buffers));
	}

	@Override
	public <T> ByteBuf encodeList(List<T> data) {
		return this.encodeCollection(data);
	}

	@Override
	public <T> ByteBuf encodeSet(Set<T> data) {
		return this.encodeCollection(data);
	}

	@Override
	public <T> ByteBuf encodeArray(T[] data) {
		ByteBuf[] buffers = new ByteBuf[data.length];
		for(int i = 0;i<data.length;i++) {
			buffers[i] = this.encode(data[i]);
			if(i < data.length - 1) {
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
		
		private Class<T> type;
		
		private boolean endOfInput;
		
		private int inputIndex;
		private ByteBuf inputBuffer;
		private ByteBuf keepBuffer;
		
		public ObjectScanner(Class<T> type) {
			this.type = type;
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

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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Currency;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.winterframework.mod.base.Charsets;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
public class ByteBufConverter implements Converter<ByteBuf, Object>, PrimitiveDecoder<ByteBuf>, PrimitiveEncoder<ByteBuf> {

	private static final byte DEFAULT_ARRAY_LIST_SEPARATOR = ',';
	
	private byte arrayListSeparator;
	
	public ByteBufConverter() {
		this(DEFAULT_ARRAY_LIST_SEPARATOR);
	}
	
	public ByteBufConverter(byte arrayListSeparator) {
		this.arrayListSeparator = arrayListSeparator;
	}
	
	public byte getArrayListSeparator() {
		return arrayListSeparator;
	}
	
	public void setArrayListSeparator(byte arrayListSeparator) {
		this.arrayListSeparator = arrayListSeparator;
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
		if(data.getClass().isArray()) {
			return this.encodeArray((Object[])data);
		}
		if(Collection.class.isAssignableFrom(data.getClass())) {
			return this.encodeCollection((Collection<?>)data);
		}
		if(ByteBuf.class.isAssignableFrom(data.getClass())) {
			return (ByteBuf)data;
		}
		if(Byte.class.equals(data.getClass())) {
			return this.encode((Byte)data);
		}
		if(Short.class.equals(data.getClass())) {
			return this.encode((Short)data);
		}
		if(Integer.class.equals(data.getClass())) {
			return this.encode((Integer)data);
		}
		if(Long.class.equals(data.getClass())) {
			return this.encode((Long)data);
		}
		if(Float.class.equals(data.getClass())) {
			return this.encode((Float)data);
		}
		if(Double.class.equals(data.getClass())) {
			return this.encode((Double)data);
		}
		if(Character.class.equals(data.getClass())) {
			return this.encode((Character)data);
		}
		if(Boolean.class.equals(data.getClass())) {
			return this.encode((Boolean)data);
		}
		if(String.class.isAssignableFrom(data.getClass())) {
			return this.encode((String)data);
		}
		if(BigInteger.class.equals(data.getClass())) {
			return this.encode((BigInteger)data);
		}
		if(BigDecimal.class.equals(data.getClass())) {
			return this.encode((BigDecimal)data);
		}
		if(LocalDate.class.equals(data.getClass())) {
			return this.encode((LocalDate)data);
		}
		if(LocalDateTime.class.equals(data.getClass())) {
			return this.encode((LocalDateTime)data);
		}
		if(ZonedDateTime.class.equals(data.getClass())) {
			return this.encode((ZonedDateTime)data);
		}
		if(Currency.class.equals(data.getClass())) {
			return this.encode((Currency)data);
		}		
		if(Locale.class.equals(data.getClass())) {
			return this.encode((Locale)data);
		}
		if(File.class.equals(data.getClass())) {
			return this.encode((File)data);
		}
		if(Path.class.equals(data.getClass())) {
			return this.encode((Path)data);
		}
		if(URI.class.equals(data.getClass())) {
			return this.encode((URI)data);
		}
		if(URL.class.equals(data.getClass())) {
			return this.encode((URL)data);
		}
		if(Pattern.class.equals(data.getClass())) {
			return this.encode((Pattern)data);
		}
		if(InetAddress.class.equals(data.getClass())) {
			return this.encode((InetAddress)data);
		}
		if(Class.class.equals(data.getClass())) {
			return this.encode((Class<?>)data);
		}
		throw new ConverterException("Data can't be encoded");
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
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value.toString(), Charsets.DEFAULT));
	}

	@Override
	public ByteBuf encode(Short value) throws ConverterException {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value.toString(), Charsets.DEFAULT));
	}

	@Override
	public ByteBuf encode(Integer value) throws ConverterException {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value.toString(), Charsets.DEFAULT));
	}

	@Override
	public ByteBuf encode(Long value) throws ConverterException {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value.toString(), Charsets.DEFAULT));
	}

	@Override
	public ByteBuf encode(Float value) throws ConverterException {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value.toString(), Charsets.DEFAULT));
	}

	@Override
	public ByteBuf encode(Double value) throws ConverterException {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value.toString(), Charsets.DEFAULT));
	}

	@Override
	public ByteBuf encode(Character value) throws ConverterException {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value.toString(), Charsets.DEFAULT));
	}

	@Override
	public ByteBuf encode(Boolean value) throws ConverterException {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value.toString(), Charsets.DEFAULT));
	}

	@Override
	public ByteBuf encode(String value) throws ConverterException {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value.toString(), Charsets.DEFAULT));
	}

	@Override
	public ByteBuf encode(BigInteger value) throws ConverterException {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value.toString(), Charsets.DEFAULT));
	}

	@Override
	public ByteBuf encode(BigDecimal value) throws ConverterException {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value.toString(), Charsets.DEFAULT));
	}

	@Override
	public ByteBuf encode(LocalDate value) throws ConverterException {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value.toString(), Charsets.DEFAULT));
	}

	@Override
	public ByteBuf encode(LocalDateTime value) throws ConverterException {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value.toString(), Charsets.DEFAULT));
	}

	@Override
	public ByteBuf encode(ZonedDateTime value) throws ConverterException {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value.toString(), Charsets.DEFAULT));
	}

	@Override
	public ByteBuf encode(Currency value) throws ConverterException {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value.toString(), Charsets.DEFAULT));
	}

	@Override
	public ByteBuf encode(Locale value) throws ConverterException {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value.toString(), Charsets.DEFAULT));
	}

	@Override
	public ByteBuf encode(File value) throws ConverterException {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value.getPath(), Charsets.DEFAULT));
	}

	@Override
	public ByteBuf encode(Path value) throws ConverterException {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value.toString(), Charsets.DEFAULT));
	}

	@Override
	public ByteBuf encode(URI value) throws ConverterException {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value.toString(), Charsets.DEFAULT));
	}

	@Override
	public ByteBuf encode(URL value) throws ConverterException {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value.toString(), Charsets.DEFAULT));
	}

	@Override
	public ByteBuf encode(Pattern value) throws ConverterException {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value.pattern(), Charsets.DEFAULT));
	}

	@Override
	public ByteBuf encode(InetAddress value) throws ConverterException {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value.getCanonicalHostName(), Charsets.DEFAULT));
	}

	@Override
	public ByteBuf encode(Class<?> value) throws ConverterException {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value.getCanonicalName(), Charsets.DEFAULT));
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
		return Flux.from(data).flatMapIterable(s -> this.decodeToList(s, type));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T decode(ByteBuf data, Class<T> type) {
		if (type.isInstance(data)) {
			return (T) data;
		}
		if(type.isArray()) {
			return (T)this.decodeToArray(data, type.getComponentType());
		}
		if(String.class.equals(type)) {
			return (T) data;
		}
		if(Boolean.class.equals(type) || Boolean.TYPE.equals(type)) {
			return (T) this.decodeBoolean(data);
		} 
		if(Character.class.equals(type) || Character.TYPE.equals(type)) {
			return (T) this.decodeCharacter(data);
		} 
		if(Integer.class.equals(type) || Integer.TYPE.equals(type)) {
			return (T) this.decodeInteger(data);
		} 
		if(Long.class.equals(type) || Long.TYPE.equals(type)) {
			return (T) this.decodeLong(data);
		} 
		if(Byte.class.equals(type) || Byte.TYPE.equals(type)) {
			return (T) this.decodeByte(data);
		} 
		if(Short.class.equals(type) || Short.TYPE.equals(type)) {
			return (T) this.decodeShort(data);
		} 
		if(Float.class.equals(type) || Float.TYPE.equals(type)) {
			return (T) this.decodeFloat(data);
		} 
		if(Double.class.equals(type) || Double.TYPE.equals(type)) {
			return (T) this.decodeDouble(data);
		} 
		if(BigInteger.class.equals(type)) {
			return (T) this.decodeBigInteger(data);
		} 
		if(BigDecimal.class.equals(type)) {
			return (T) this.decodeBigDecimal(data);
		} 
		if(LocalDate.class.equals(type)) {
			return (T) this.decodeLocalDate(data);
		} 
		if(LocalDateTime.class.equals(type)) {
			return (T) this.decodeLocalDateTime(data);
		} 
		if(ZonedDateTime.class.equals(type)) {
			return (T) this.decodeZonedDateTime(data);
		} 
		if(File.class.equals(type)) {
			return (T) this.decodeFile(data);
		} 
		if(Path.class.equals(type)) {
			return (T) this.decodePath(data);
		} 
		if(URI.class.equals(type)) {
			return (T) this.decodeURI(data);
		} 
		if(URL.class.equals(type)) {
			return (T) this.decodeURL(data);
		} 
		if(Pattern.class.equals(type)) {
			return (T) this.decodePattern(data);
		} 
		if(Locale.class.equals(type)) {
			return (T) this.decodeLocale(data);
		} 
		if(type.isEnum()) {
			return (T) this.decodeEnum(data, type.asSubclass(Enum.class));
		} 
		if(type.equals(Class.class)) {
			return (T) this.decodeClass(data);
		} 
		if(InetAddress.class.isAssignableFrom(type)) {
			return (T) this.decodeInetAddress(data);
		}
		
		throw new ConverterException(data + " can't be decoded to the requested type: " + type.getCanonicalName());
	}

	@Override
	public <T> List<T> decodeToList(ByteBuf data, Class<T> type) {
		// We must split the buffer base on the separator
		
		
		
		return null;
	}

	@Override
	public <T> Set<T> decodeToSet(ByteBuf data, Class<T> type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T[] decodeToArray(ByteBuf data, Class<T> type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Byte decodeByte(ByteBuf data) throws ConverterException {
		try {
			return Byte.valueOf(data.toString(Charsets.DEFAULT));
		}
		catch (NumberFormatException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
		finally {
			data.release();
		}
	}

	@Override
	public Short decodeShort(ByteBuf data) throws ConverterException {
		try {
			return Short.valueOf(data.toString(Charsets.DEFAULT));
		}
		catch (NumberFormatException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
		finally {
			data.release();
		}
	}

	@Override
	public Integer decodeInteger(ByteBuf data) throws ConverterException {
		try {
			return Integer.valueOf(data.toString(Charsets.DEFAULT));
		}
		catch (NumberFormatException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
		finally {
			data.release();
		}
	}

	@Override
	public Long decodeLong(ByteBuf data) throws ConverterException {
		try {
			return Long.valueOf(data.toString(Charsets.DEFAULT));
		}
		catch (NumberFormatException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
		finally {
			data.release();
		}
	}

	@Override
	public Float decodeFloat(ByteBuf data) throws ConverterException {
		try {
			return Float.valueOf(data.toString(Charsets.DEFAULT));
		}
		catch (NumberFormatException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
		finally {
			data.release();
		}
	}

	@Override
	public Double decodeDouble(ByteBuf data) throws ConverterException {
		try {
			return Double.valueOf(data.toString(Charsets.DEFAULT));
		} 
		catch (NumberFormatException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
		finally {
			data.release();
		}
	}

	@Override
	public Character decodeCharacter(ByteBuf data) throws ConverterException {
		try {
			String value = data.toString(Charsets.DEFAULT);
			if(value.length() == 1) {
				return value.charAt(0);
			}
			throw new ConverterException(data + " can't be decoded to the requested type");
		}
		finally {
			data.release();
		}
	}

	@Override
	public Boolean decodeBoolean(ByteBuf data) throws ConverterException {
		try {
			return Boolean.valueOf(data.toString(Charsets.DEFAULT));
		}
		finally {
			data.release();
		}
	}

	@Override
	public String decodeString(ByteBuf data) throws ConverterException {
		try {
			return data.toString(Charsets.DEFAULT);
		}
		finally {
			data.release();
		}
	}

	@Override
	public BigInteger decodeBigInteger(ByteBuf data) throws ConverterException {
		try {
			return new BigInteger(data.toString(Charsets.DEFAULT));
		} 
		catch (NumberFormatException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
		finally {
			data.release();
		}
	}

	@Override
	public BigDecimal decodeBigDecimal(ByteBuf data) throws ConverterException {
		try {
			return new BigDecimal(data.toString(Charsets.DEFAULT));
		}
		catch (NumberFormatException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
		finally {
			data.release();
		}
	}

	@Override
	public LocalDate decodeLocalDate(ByteBuf data) throws ConverterException {
		try {
			return LocalDate.parse(data.toString(Charsets.DEFAULT));
		} 
		catch (Exception e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
		finally {
			data.release();
		}
	}

	@Override
	public LocalDateTime decodeLocalDateTime(ByteBuf data) throws ConverterException {
		try {
			return LocalDateTime.parse(data.toString(Charsets.DEFAULT));
		} 
		catch (DateTimeParseException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
		finally {
			data.release();
		}
	}

	@Override
	public ZonedDateTime decodeZonedDateTime(ByteBuf data) throws ConverterException {
		try {
			return ZonedDateTime.parse(data.toString(Charsets.DEFAULT));
		} 
		catch (DateTimeParseException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
		finally {
			data.release();
		}
	}

	@Override
	public Currency decodeCurrency(ByteBuf data) throws ConverterException {
		try {
			return Currency.getInstance(data.toString(Charsets.DEFAULT));
		} 
		catch (IllegalArgumentException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
		finally {
			data.release();
		}
	}

	@Override
	public Locale decodeLocale(ByteBuf data) throws ConverterException {
		try {
			String[] elements = data.toString(Charsets.DEFAULT).split("_");
	        if(elements.length >= 1 && (elements[0].length() == 2 || elements[0].length() == 0)) {
	            return new Locale(elements[0], elements.length >= 2 ? elements[1] : "", elements.length >= 3 ? elements[2] : "");
	        }
	        throw new ConverterException(data + " can't be decoded to the requested type");
		}
		finally {
			data.release();
		}
	}

	@Override
	public File decodeFile(ByteBuf data) throws ConverterException {
		try {
			return new File(data.toString(Charsets.DEFAULT));
		}
		finally {
			data.release();
		}
	}

	@Override
	public Path decodePath(ByteBuf data) throws ConverterException {
		try {
			return Paths.get(data.toString(Charsets.DEFAULT));
		} 
		catch (InvalidPathException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
		finally {
			data.release();
		}
	}

	@Override
	public URI decodeURI(ByteBuf data) throws ConverterException {
		try {
			return new URI(data.toString(Charsets.DEFAULT));
		} 
		catch (URISyntaxException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
		finally {
			data.release();
		}
	}

	@Override
	public URL decodeURL(ByteBuf data) throws ConverterException {
		try {
			return new URL(data.toString(Charsets.DEFAULT));
		} 
		catch (MalformedURLException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
		finally {
			data.release();
		}
	}

	@Override
	public Pattern decodePattern(ByteBuf data) throws ConverterException {
		try {
			return Pattern.compile(data.toString(Charsets.DEFAULT));
		} 
		catch (PatternSyntaxException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
		finally {
			data.release();
		}
	}

	@Override
	public InetAddress decodeInetAddress(ByteBuf data) throws ConverterException {
		try {
			return InetAddress.getByName(data.toString(Charsets.DEFAULT));
		} 
		catch (UnknownHostException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
		finally {
			data.release();
		}
	}

	@Override
	public Class<?> decodeClass(ByteBuf data) throws ConverterException {
		try {
			return Class.forName(data.toString(Charsets.DEFAULT));
		} 
		catch (ClassNotFoundException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
		finally {
			data.release();
		}
	}
	
	private <T extends Enum<T>> T decodeEnum(ByteBuf data, Class<T> type) {
		try {
			return Enum.valueOf(type, data.toString(Charsets.DEFAULT));
		} 
		catch (IllegalArgumentException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
		finally {
			data.release();
		}
	}
}

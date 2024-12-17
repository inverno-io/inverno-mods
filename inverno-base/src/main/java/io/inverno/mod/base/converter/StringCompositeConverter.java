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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>
 * An extensible string converter.
 * </p>
 * 
 * <p>
 * This converter is backed by a {@link StringConverter} which can convert primitive and common objects, it can be extended to be able to convert other types of objects by injecting specific compound
 * decoders and encoders.
 * </p>
 * 
 * <p>
 * This converter is an object converter and as such it can convert collection of objects using a customizable separator.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see CompositeConverter
 * @see CompoundDecoder
 * @see CompoundEncoder
 * @see StringConverter
 */
public class StringCompositeConverter extends CompositeConverter<String> implements ObjectConverter<String> {

	private final StringConverter defaultConverter;
	
	private String arrayListSeparator;

	/**
	 * <p>
	 * Creates a string composite converter with the default array/list separator ({@link StringConverter#DEFAULT_ARRAY_LIST_SEPARATOR}.
	 * </p>
	 */
	public StringCompositeConverter() {
		this(StringConverter.DEFAULT_ARRAY_LIST_SEPARATOR);
	}
	
	/**
	 * <p>
	 * Creates a string composite converter with the specified array/list separator.
	 * </p>
	 * 
	 * @param arrayListSeparator an array/list separator
	 */
	public StringCompositeConverter(String arrayListSeparator) {
		this.arrayListSeparator = arrayListSeparator;
		this.defaultConverter = new StringConverter(this.arrayListSeparator);
		this.setDefaultDecoder(this.defaultConverter);
		this.setDefaultEncoder(this.defaultConverter);
	}
	
	/**
	 * <p>
	 * Returns the array/list separator used to convert lists and arrays.
	 * </p>
	 * 
	 * @return an array/list separator
	 */
	public String getArrayListSeparator() {
		return this.arrayListSeparator;
	}
	
	/**
	 * <p>
	 * Sets the array/list separator used to convert lists and arrays.
	 * </p>
	 * 
	 * @param arrayListSeparator an array/list separator
	 */
	public void setArrayListSeparator(String arrayListSeparator) {
		this.arrayListSeparator = arrayListSeparator;
		this.defaultConverter.setArrayListSeparator(this.arrayListSeparator);
	}
	
	@Override
	public void setDecoders(List<CompoundDecoder<String, ?>> decoders) {
		super.setDecoders(decoders);
	}
	
	@Override
	public void setEncoders(List<CompoundEncoder<?, String>> encoders) {
		super.setEncoders(encoders);
	}
	
	@Override
	public <T> List<T> decodeToList(String value, Class<T> type) {
		return this.decodeToList(value, (Type)type);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> decodeToList(String value, Type type) {
		if(value == null) {
			return null;
		}
		return Arrays.stream(value.split(this.arrayListSeparator)).map(String::trim).map(item -> (T)this.decode(item, type))
				.collect(Collectors.toList());
	}
	
	@Override
	public <T> Set<T> decodeToSet(String value, Class<T> type) {
		return this.decodeToSet(value, (Type)type);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> Set<T> decodeToSet(String value, Type type) {
		if(value == null) {
			return null;
		}
		return Arrays.stream(value.split(this.arrayListSeparator)).map(String::trim).map(item -> (T)this.decode(item, type))
				.collect(Collectors.toSet());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] decodeToArray(String value, Class<T> type) {
		if(value == null) {
			return null;
		}
		List<T> objects = this.decodeToList(value, type);
		return objects.toArray((T[]) Array.newInstance(type, objects.size()));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] decodeToArray(String value, Type type) {
		if(value == null) {
			return null;
		}
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

	@Override
	public Byte decodeByte(String value) throws ConverterException {
		return super.decode(value, Byte.class);
	}

	@Override
	public Short decodeShort(String value) throws ConverterException {
		return super.decode(value, Short.class);
	}

	@Override
	public Integer decodeInteger(String value) throws ConverterException {
		return super.decode(value, Integer.class);
	}

	@Override
	public Long decodeLong(String value) throws ConverterException {
		return super.decode(value, Long.class);
	}

	@Override
	public Float decodeFloat(String value) throws ConverterException {
		return super.decode(value, Float.class);
	}

	@Override
	public Double decodeDouble(String value) throws ConverterException {
		return super.decode(value, Double.class);
	}

	@Override
	public Character decodeCharacter(String value) throws ConverterException {
		return super.decode(value, Character.class);
	}

	@Override
	public Boolean decodeBoolean(String value) throws ConverterException {
		return super.decode(value, Boolean.class);
	}

	@Override
	public String decodeString(String value) throws ConverterException {
		return super.decode(value, String.class);
	}

	@Override
	public BigInteger decodeBigInteger(String value) throws ConverterException {
		return super.decode(value, BigInteger.class);
	}

	@Override
	public BigDecimal decodeBigDecimal(String value) throws ConverterException {
		return super.decode(value, BigDecimal.class);
	}

	@Override
	public LocalDate decodeLocalDate(String value) throws ConverterException {
		return super.decode(value, LocalDate.class);
	}

	@Override
	public LocalDateTime decodeLocalDateTime(String value) throws ConverterException {
		return super.decode(value, LocalDateTime.class);
	}

	@Override
	public ZonedDateTime decodeZonedDateTime(String value) throws ConverterException {
		return super.decode(value, ZonedDateTime.class);
	}

	@Override
	public Currency decodeCurrency(String value) throws ConverterException {
		return super.decode(value, Currency.class);
	}

	@Override
	public Locale decodeLocale(String value) throws ConverterException {
		return super.decode(value, Locale.class);
	}

	@Override
	public File decodeFile(String value) throws ConverterException {
		return super.decode(value, File.class);
	}

	@Override
	public Path decodePath(String value) throws ConverterException {
		return super.decode(value, Path.class);
	}

	@Override
	public URI decodeURI(String value) throws ConverterException {
		return super.decode(value, URI.class);
	}

	@Override
	public URL decodeURL(String value) throws ConverterException {
		return super.decode(value, URL.class);
	}

	@Override
	public Pattern decodePattern(String value) throws ConverterException {
		return super.decode(value, Pattern.class);
	}

	@Override
	public InetAddress decodeInetAddress(String value) throws ConverterException {
		return super.decode(value, InetAddress.class);
	}

	@Override
	public InetSocketAddress decodeInetSocketAddress(String value) throws ConverterException {
		return super.decode(value, InetSocketAddress.class);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Class<T> decodeClass(String value) throws ConverterException {
		return super.decode(value, Class.class);
	}
	
	private <T> String encodeCollection(Collection<T> value) {
		return value.stream().map(this::encode).collect(Collectors.joining(String.valueOf(this.arrayListSeparator)));
	}
	
	private <T> String encodeCollection(Collection<T> value, Type type) {
		return value.stream().map(element -> this.encode(element, type)).collect(Collectors.joining(String.valueOf(this.arrayListSeparator)));
	}
	
	@Override
	public <T> String encodeList(List<T> value) {
		return this.encodeCollection(value);
	}

	@Override
	public <T> String encodeList(List<T> value, Class<T> type) {
		return this.encodeCollection(value, type);
	}
	
	@Override
	public <T> String encodeList(List<T> value, Type type) {
		return this.encodeCollection(value, type);
	}
	
	@Override
	public <T> String encodeSet(Set<T> value) {
		return this.encodeCollection(value);
	}

	@Override
	public <T> String encodeSet(Set<T> value, Class<T> type) {
		return this.encodeCollection(value, type);
	}
	
	@Override
	public <T> String encodeSet(Set<T> value, Type type) {
		return this.encodeCollection(value, type);
	}
	
	@Override
	public <T> String encodeArray(T[] value) {
		StringBuilder result = new StringBuilder();
		for(int i = 0;i<value.length;i++) {
			result.append(this.encode(value[i]));
			if(i < value.length - 1) {
				result.append(this.arrayListSeparator);
			}
		}
		return result.toString();
	}
	
	@Override
	public <T> String encodeArray(T[] value, Class<T> type) {
		return this.encodeArray(value, (Type)type);
	}

	@Override
	public <T> String encodeArray(T[] value, Type type) {
		StringBuilder result = new StringBuilder();
		for(int i = 0;i<value.length;i++) {
			result.append(this.encode(value[i], type));
			if(i < value.length - 1) {
				result.append(this.arrayListSeparator);
			}
		}
		return result.toString();
	}
	
	@Override
	public String encode(Byte value) throws ConverterException {
		return super.encode(value);
	}

	@Override
	public String encode(Short value) throws ConverterException {
		return super.encode(value);
	}

	@Override
	public String encode(Integer value) throws ConverterException {
		return super.encode(value);
	}

	@Override
	public String encode(Long value) throws ConverterException {
		return super.encode(value);
	}

	@Override
	public String encode(Float value) throws ConverterException {
		return super.encode(value);
	}

	@Override
	public String encode(Double value) throws ConverterException {
		return super.encode(value);
	}

	@Override
	public String encode(Character value) throws ConverterException {
		return super.encode(value);
	}

	@Override
	public String encode(Boolean value) throws ConverterException {
		return super.encode(value);
	}

	@Override
	public String encode(String value) throws ConverterException {
		return super.encode(value);
	}

	@Override
	public String encode(BigInteger value) throws ConverterException {
		return super.encode(value);
	}

	@Override
	public String encode(BigDecimal value) throws ConverterException {
		return super.encode(value);
	}

	@Override
	public String encode(LocalDate value) throws ConverterException {
		return super.encode(value);
	}

	@Override
	public String encode(LocalDateTime value) throws ConverterException {
		return super.encode(value);
	}

	@Override
	public String encode(ZonedDateTime value) throws ConverterException {
		return super.encode(value);
	}

	@Override
	public String encode(Currency value) throws ConverterException {
		return super.encode(value);
	}

	@Override
	public String encode(Locale value) throws ConverterException {
		return super.encode(value);
	}

	@Override
	public String encode(File value) throws ConverterException {
		return super.encode(value);
	}

	@Override
	public String encode(Path value) throws ConverterException {
		return super.encode(value);
	}

	@Override
	public String encode(URI value) throws ConverterException {
		return super.encode(value);
	}

	@Override
	public String encode(URL value) throws ConverterException {
		return super.encode(value);
	}

	@Override
	public String encode(Pattern value) throws ConverterException {
		return super.encode(value);
	}

	@Override
	public String encode(InetAddress value) throws ConverterException {
		return super.encode(value);
	}

	@Override
	public String encode(InetSocketAddress value) throws ConverterException {
		return super.encode(value);
	}
	
	@Override
	public String encode(Class<?> value) throws ConverterException {
		return super.encode(value);
	}
}

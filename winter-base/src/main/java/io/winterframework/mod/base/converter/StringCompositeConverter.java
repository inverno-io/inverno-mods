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
 * @author jkuhn
 *
 */
public class StringCompositeConverter extends CompositeConverter<String> implements ObjectConverter<String> {

	private StringConverter defaultConverter;
	
	private String arrayListSeparator;

	public StringCompositeConverter() {
		this(StringConverter.DEFAULT_ARRAY_LIST_SEPARATOR);
	}
	
	public StringCompositeConverter(String arrayListSeparator) {
		this.arrayListSeparator = arrayListSeparator;
		this.defaultConverter = new StringConverter(this.arrayListSeparator);
		this.setDefaultDecoder(this.defaultConverter);
		this.setDefaultEncoder(this.defaultConverter);
	}
	
	public String getArrayListSeparator() {
		return this.arrayListSeparator;
	}
	
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
	public <T> List<T> decodeToList(String data, Class<T> type) {
		if(data == null) {
			return null;
		}
		return Arrays.stream(data.split(this.arrayListSeparator)).map(String::trim).map(item -> this.decode(item, type))
				.collect(Collectors.toList());
	}

	@Override
	public <T> Set<T> decodeToSet(String data, Class<T> type) {
		if(data == null) {
			return null;
		}
		return Arrays.stream(data.split(this.arrayListSeparator)).map(String::trim).map(item -> this.decode(item, type))
				.collect(Collectors.toSet());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] decodeToArray(String data, Class<T> type) {
		if(data == null) {
			return null;
		}
		List<T> objects = this.decodeToList(data, type);
		return objects.toArray((T[]) Array.newInstance(type, objects.size()));
	}

	@Override
	public Byte decodeByte(String data) throws ConverterException {
		return super.decode(data, Byte.class);
	}

	@Override
	public Short decodeShort(String data) throws ConverterException {
		return super.decode(data, Short.class);
	}

	@Override
	public Integer decodeInteger(String data) throws ConverterException {
		return super.decode(data, Integer.class);
	}

	@Override
	public Long decodeLong(String data) throws ConverterException {
		return super.decode(data, Long.class);
	}

	@Override
	public Float decodeFloat(String data) throws ConverterException {
		return super.decode(data, Float.class);
	}

	@Override
	public Double decodeDouble(String data) throws ConverterException {
		return super.decode(data, Double.class);
	}

	@Override
	public Character decodeCharacter(String data) throws ConverterException {
		return super.decode(data, Character.class);
	}

	@Override
	public Boolean decodeBoolean(String data) throws ConverterException {
		return super.decode(data, Boolean.class);
	}

	@Override
	public String decodeString(String data) throws ConverterException {
		return super.decode(data, String.class);
	}

	@Override
	public BigInteger decodeBigInteger(String data) throws ConverterException {
		return super.decode(data, BigInteger.class);
	}

	@Override
	public BigDecimal decodeBigDecimal(String data) throws ConverterException {
		return super.decode(data, BigDecimal.class);
	}

	@Override
	public LocalDate decodeLocalDate(String data) throws ConverterException {
		return super.decode(data, LocalDate.class);
	}

	@Override
	public LocalDateTime decodeLocalDateTime(String data) throws ConverterException {
		return super.decode(data, LocalDateTime.class);
	}

	@Override
	public ZonedDateTime decodeZonedDateTime(String data) throws ConverterException {
		return super.decode(data, ZonedDateTime.class);
	}

	@Override
	public Currency decodeCurrency(String data) throws ConverterException {
		return super.decode(data, Currency.class);
	}

	@Override
	public Locale decodeLocale(String data) throws ConverterException {
		return super.decode(data, Locale.class);
	}

	@Override
	public File decodeFile(String data) throws ConverterException {
		return super.decode(data, File.class);
	}

	@Override
	public Path decodePath(String data) throws ConverterException {
		return super.decode(data, Path.class);
	}

	@Override
	public URI decodeURI(String data) throws ConverterException {
		return super.decode(data, URI.class);
	}

	@Override
	public URL decodeURL(String data) throws ConverterException {
		return super.decode(data, URL.class);
	}

	@Override
	public Pattern decodePattern(String data) throws ConverterException {
		return super.decode(data, Pattern.class);
	}

	@Override
	public InetAddress decodeInetAddress(String data) throws ConverterException {
		return super.decode(data, InetAddress.class);
	}

	@Override
	public Class<?> decodeClass(String data) throws ConverterException {
		return super.decode(data, Class.class);
	}
	
	private <T> String encodeCollection(Collection<T> data) {
		return data.stream().map(this::encode).collect(Collectors.joining(String.valueOf(this.arrayListSeparator)));
	}
	
	@Override
	public <T> String encodeList(List<T> data) {
		return this.encodeCollection(data);
	}

	@Override
	public <T> String encodeSet(Set<T> data) {
		return this.encodeCollection(data);
	}

	@Override
	public <T> String encodeArray(T[] data) {
		StringBuilder result = new StringBuilder();
		for(int i = 0;i<data.length;i++) {
			result.append(this.encode(data[i]));
			if(i < data.length - 1) {
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
	public String encode(Class<?> value) throws ConverterException {
		return super.encode(value);
	}
}

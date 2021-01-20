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
package io.winterframework.mod.configuration.internal;

import java.io.File;
import java.lang.reflect.Array;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;

import io.winterframework.mod.base.converter.ConverterException;
import io.winterframework.mod.base.converter.ObjectConverter;

/**
 * <p>
 * Decode/Encode primitive objects from/to Java String literals (ie. escaping
 * characters in a String using Java String rules).
 * </p>
 * 
 * @author jkuhn
 *
 */
public class JavaStringConverter implements ObjectConverter<String> {

	private static final String DEFAULT_ARRAY_LIST_SEPARATOR = ",";

	private String arrayListSeparator;

	public JavaStringConverter() {
		this(DEFAULT_ARRAY_LIST_SEPARATOR);
	}

	public JavaStringConverter(String arrayListSeparator) {
		this.arrayListSeparator = arrayListSeparator;
	}

	public String getArrayListSeparator() {
		return arrayListSeparator;
	}

	public void setArrayListSeparator(String arrayListSeparator) {
		this.arrayListSeparator = arrayListSeparator;
	}

	@Override
	public String encode(Object data) {
		if(data == null) {
			return null;
		}
		if(data.getClass().isArray()) {
			return this.encodeArray((Object[])data);
		}
		if(Collection.class.isAssignableFrom(data.getClass())) {
			return this.encodeCollection((Collection<?>)data);
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
		if(String.class.equals(data.getClass())) {
			return this.encode((String)data);
		}
		if(BigInteger.class.isAssignableFrom(data.getClass())) {
			return this.encode((BigInteger)data);
		}
		if(BigDecimal.class.isAssignableFrom(data.getClass())) {
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
		if(File.class.isAssignableFrom(data.getClass())) {
			return this.encode((File)data);
		}
		if(Path.class.isAssignableFrom(data.getClass())) {
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
		if(InetAddress.class.isAssignableFrom(data.getClass())) {
			return this.encode((InetAddress)data);
		}
		if(Class.class.equals(data.getClass())) {
			return this.encode((Class<?>)data);
		}
		throw new ConverterException("Data can't be encoded");
	}

	private <T extends Object> String encodeCollection(Collection<T> data) {
		return data != null ? data.stream().map(this::encode).collect(Collectors.joining(this.arrayListSeparator))
				: null;
	}

	@Override
	public <T extends Object> String encodeList(List<T> data) {
		return this.encodeCollection(data);
	}

	@Override
	public <T extends Object> String encodeSet(Set<T> data) {
		return this.encodeCollection(data);
	}

	@Override
	public <T extends Object> String encodeArray(T[] data) {
		return data != null ? Arrays.stream(data).map(this::encode).collect(Collectors.joining(this.arrayListSeparator))
				: null;
	}

	@Override
	public String encode(Byte value) throws ConverterException {
		return value != null ? value.toString() : null;
	}

	@Override
	public String encode(Short value) throws ConverterException {
		return value != null ? value.toString() : null;
	}

	@Override
	public String encode(Integer value) throws ConverterException {
		return value != null ? value.toString() : null;
	}

	@Override
	public String encode(Long value) throws ConverterException {
		return value != null ? value.toString() : null;
	}

	@Override
	public String encode(Float value) throws ConverterException {
		return value != null ? value.toString() : null;
	}

	@Override
	public String encode(Double value) throws ConverterException {
		return value != null ? value.toString() : null;
	}

	@Override
	public String encode(Character value) throws ConverterException {
		return value != null ? StringEscapeUtils.escapeJava(value.toString()) : null;
	}

	@Override
	public String encode(Boolean value) throws ConverterException {
		return value != null ? value.toString() : null;
	}

	@Override
	public String encode(String value) throws ConverterException {
		return value != null ? StringEscapeUtils.escapeJava(value.toString()) : null;
	}

	@Override
	public String encode(BigInteger value) throws ConverterException {
		return value != null ? value.toString() : null;
	}

	@Override
	public String encode(BigDecimal value) throws ConverterException {
		return value != null ? value.toString() : null;
	}

	@Override
	public String encode(LocalDate value) throws ConverterException {
		return value != null ? StringEscapeUtils.escapeJava(value.toString()) : null;
	}

	@Override
	public String encode(LocalDateTime value) throws ConverterException {
		return value != null ? StringEscapeUtils.escapeJava(value.toString()) : null;
	}

	@Override
	public String encode(ZonedDateTime value) throws ConverterException {
		return value != null ? StringEscapeUtils.escapeJava(value.toString()) : null;
	}

	@Override
	public String encode(Currency value) throws ConverterException {
		return value != null ? StringEscapeUtils.escapeJava(value.toString()) : null;
	}

	@Override
	public String encode(Locale value) throws ConverterException {
		return value != null ? StringEscapeUtils.escapeJava(value.toString()) : null;
	}

	@Override
	public String encode(File value) throws ConverterException {
		return value != null ? StringEscapeUtils.escapeJava(value.getPath()) : null;
	}

	@Override
	public String encode(Path value) throws ConverterException {
		return value != null ? StringEscapeUtils.escapeJava(value.toString()) : null;
	}

	@Override
	public String encode(URI value) throws ConverterException {
		return value != null ? StringEscapeUtils.escapeJava(value.toString()) : null;
	}

	@Override
	public String encode(URL value) throws ConverterException {
		return value != null ? StringEscapeUtils.escapeJava(value.toString()) : null;
	}

	@Override
	public String encode(Pattern value) throws ConverterException {
		return value != null ? StringEscapeUtils.escapeJava(value.pattern()) : null;
	}

	@Override
	public String encode(InetAddress value) throws ConverterException {
		return value != null ? StringEscapeUtils.escapeJava(value.getCanonicalHostName()) : null;
	}

	@Override
	public String encode(Class<?> value) throws ConverterException {
		return value != null ? value.getCanonicalName() : null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T decode(String data, Class<T> type) {
		if(data == null) {
			return null;
		}
		if (CharSequence.class.isAssignableFrom(type)) {
			return (T) this.decodeString(data);
		}
		if (type.isArray()) {
			return (T) this.decodeToArray(data, type.getComponentType());
		}
		if (Boolean.class.equals(type) || Boolean.TYPE.equals(type)) {
			return (T) this.decodeBoolean(data);
		}
		if (Character.class.equals(type) || Character.TYPE.equals(type)) {
			return (T) this.decodeCharacter(data);
		}
		if (Integer.class.equals(type) || Integer.TYPE.equals(type)) {
			return (T) this.decodeInteger(data);
		}
		if (Long.class.equals(type) || Long.TYPE.equals(type)) {
			return (T) this.decodeLong(data);
		}
		if (Byte.class.equals(type) || Byte.TYPE.equals(type)) {
			return (T) this.decodeByte(data);
		}
		if (Short.class.equals(type) || Short.TYPE.equals(type)) {
			return (T) this.decodeShort(data);
		}
		if (Float.class.equals(type) || Float.TYPE.equals(type)) {
			return (T) this.decodeFloat(data);
		}
		if (Double.class.equals(type) || Double.TYPE.equals(type)) {
			return (T) this.decodeDouble(data);
		}
		if (BigInteger.class.equals(type)) {
			return (T) this.decodeBigInteger(data);
		}
		if (BigDecimal.class.equals(type)) {
			return (T) this.decodeBigDecimal(data);
		}
		if (LocalDate.class.equals(type)) {
			return (T) this.decodeLocalDate(data);
		}
		if (LocalDateTime.class.equals(type)) {
			return (T) this.decodeLocalDateTime(data);
		}
		if (ZonedDateTime.class.equals(type)) {
			return (T) this.decodeZonedDateTime(data);
		}
		if (Currency.class.equals(type)) {
			return (T) this.decodeCurrency(data);
		}
		if (File.class.equals(type)) {
			return (T) this.decodeFile(data);
		}
		if (Path.class.equals(type)) {
			return (T) this.decodePath(data);
		}
		if (URI.class.equals(type)) {
			return (T) this.decodeURI(data);
		}
		if (URL.class.equals(type)) {
			return (T) this.decodeURL(data);
		}
		if (Pattern.class.equals(type)) {
			return (T) this.decodePattern(data);
		}
		if (Locale.class.equals(type)) {
			return (T) this.decodeLocale(data);
		}
		if (type.isEnum()) {
			return (T) this.decodeEnum(data, type.asSubclass(Enum.class));
		}
		if (type.equals(Class.class)) {
			return (T) this.decodeClass(data);
		}
		if (InetAddress.class.isAssignableFrom(type)) {
			return (T) this.decodeInetAddress(data);
		}
		// TODO we could inject another String to object decoder based on json, xml...
		// to convert other types

		throw new ConverterException(data + " can't be decoded to the requested type: " + type.getCanonicalName());
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
		try {
			return data != null ? Byte.valueOf(data) : null;
		} 
		catch (NumberFormatException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public Short decodeShort(String data) throws ConverterException {
		try {
			return data != null ? Short.valueOf(data) : null;
		} 
		catch (NumberFormatException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public Integer decodeInteger(String data) throws ConverterException {
		try {
			return data != null ? Integer.valueOf(data) : null;
		}
		catch (NumberFormatException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public Long decodeLong(String data) throws ConverterException {
		try {
			return data != null ? Long.valueOf(data) : null;
		} 
		catch (NumberFormatException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public Float decodeFloat(String data) throws ConverterException {
		try {
			return data != null ? Float.valueOf(data) : null;
		}
		catch (NumberFormatException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public Double decodeDouble(String data) throws ConverterException {
		try {
			return data != null ? Double.valueOf(data) : null;
		} 
		catch (NumberFormatException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public Character decodeCharacter(String data) throws ConverterException {
		if(data == null) {
			return null;
		}
		data = StringEscapeUtils.unescapeJava(data);
		if (data.length() == 1) {
			return data.charAt(0);
		}
		throw new ConverterException(data + " can't be decoded to the requested type");
	}

	@Override
	public Boolean decodeBoolean(String data) throws ConverterException {
		return data != null ? Boolean.valueOf(data) : null;
	}

	@Override
	public String decodeString(String data) throws ConverterException {
		return StringEscapeUtils.unescapeJava(data);
	}

	@Override
	public BigInteger decodeBigInteger(String data) throws ConverterException {
		try {
			return data != null ? new BigInteger(data) : null;
		} 
		catch (NumberFormatException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public BigDecimal decodeBigDecimal(String data) throws ConverterException {
		try {
			return data != null ? new BigDecimal(data) : null;
		} 
		catch (NumberFormatException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public LocalDate decodeLocalDate(String data) throws ConverterException {
		try {
			return data != null ? LocalDate.parse(StringEscapeUtils.unescapeJava(data)) : null;
		} 
		catch (Exception e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public LocalDateTime decodeLocalDateTime(String data) throws ConverterException {
		try {
			return data != null ? LocalDateTime.parse(StringEscapeUtils.unescapeJava(data)) : null;
		} 
		catch (DateTimeParseException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public ZonedDateTime decodeZonedDateTime(String data) throws ConverterException {
		try {
			return data != null ? ZonedDateTime.parse(StringEscapeUtils.unescapeJava(data)) : null;
		} 
		catch (DateTimeParseException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public Currency decodeCurrency(String data) throws ConverterException {
		try {
			return Currency.getInstance(StringEscapeUtils.unescapeJava(data));
		} 
		catch (IllegalArgumentException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public Locale decodeLocale(String data) throws ConverterException {
		if(data == null) {
			return null;
		}
		String[] elements = StringEscapeUtils.unescapeJava(data).split("_");
		if (elements.length >= 1 && (elements[0].length() == 2 || elements[0].length() == 0)) {
			return new Locale(elements[0], elements.length >= 2 ? elements[1] : "",
					elements.length >= 3 ? elements[2] : "");
		}
		throw new ConverterException(data + " can't be decoded to the requested type");
	}

	@Override
	public File decodeFile(String data) throws ConverterException {
		return data != null ? new File(StringEscapeUtils.unescapeJava(data)) : null;
	}

	@Override
	public Path decodePath(String data) throws ConverterException {
		try {
			return data != null ? Paths.get(StringEscapeUtils.unescapeJava(data)) : null;
		} catch (InvalidPathException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public URI decodeURI(String data) throws ConverterException {
		try {
			return data != null ? new URI(StringEscapeUtils.unescapeJava(data)) : null;
		} catch (URISyntaxException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public URL decodeURL(String data) throws ConverterException {
		try {
			return data != null ? new URL(StringEscapeUtils.unescapeJava(data)) : null;
		} catch (MalformedURLException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public Pattern decodePattern(String data) throws ConverterException {
		try {
			return data != null ? Pattern.compile(StringEscapeUtils.unescapeJava(data)) : null;
		} catch (PatternSyntaxException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public InetAddress decodeInetAddress(String data) throws ConverterException {
		try {
			return data != null ? InetAddress.getByName(StringEscapeUtils.unescapeJava(data)) : null;
		} catch (UnknownHostException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public Class<?> decodeClass(String data) throws ConverterException {
		try {
			return data != null ? Class.forName(data) : null;
		} catch (ClassNotFoundException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
	}

	private <T extends Enum<T>> T decodeEnum(String data, Class<T> type) {
		try {
			return data != null ? Enum.valueOf(type, data) : null;
		} catch (IllegalArgumentException e) {
			throw new ConverterException(data + " can't be decoded to the requested type", e);
		}
	}
}

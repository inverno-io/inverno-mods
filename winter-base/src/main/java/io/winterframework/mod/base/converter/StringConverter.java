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
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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

/**
 * @author jkuhn
 *
 */
public class StringConverter implements ObjectConverter<String> {

	public static final String DEFAULT_ARRAY_LIST_SEPARATOR = ",";
	private String arrayListSeparator;
	
	public StringConverter() {
		this(DEFAULT_ARRAY_LIST_SEPARATOR);
	}
	
	public StringConverter(String arrayListSeparator) {
		this.arrayListSeparator = arrayListSeparator;
	}
	
	public String getArrayListSeparator() {
		return arrayListSeparator;
	}
	
	public void setArrayListSeparator(String arrayListSeparator) {
		this.arrayListSeparator = arrayListSeparator;
	}
	
	@Override
	public <T> String encode(T value) {
		return this.encode(value, value.getClass());
	}
	
	@Override
	public <T> String encode(T value, Class<T> type) throws ConverterException {
		if(value == null) {
			return null;
		}
		if(type.isArray()) {
			return this.encodeArray((Object[])value, type.getComponentType());
		}
		if(Collection.class.isAssignableFrom(type)) {
			return this.encodeCollection((Collection<?>)value);
		}
		if(Byte.class.equals(type)) {
			return this.encode((Byte)value);
		}
		if(Short.class.equals(type)) {
			return this.encode((Short)value);
		}
		if(Integer.class.equals(type)) {
			return this.encode((Integer)value);
		}
		if(Long.class.equals(type)) {
			return this.encode((Long)value);
		}
		if(Float.class.equals(type)) {
			return this.encode((Float)value);
		}
		if(Double.class.equals(type)) {
			return this.encode((Double)value);
		}
		if(Character.class.equals(type)) {
			return this.encode((Character)value);
		}
		if(Boolean.class.equals(type)) {
			return this.encode((Boolean)value);
		}
		if(String.class.equals(type)) {
			return this.encode((String)value);
		}
		if(BigInteger.class.isAssignableFrom(type)) {
			return this.encode((BigInteger)value);
		}
		if(BigDecimal.class.isAssignableFrom(type)) {
			return this.encode((BigDecimal)value);
		}
		if(LocalDate.class.equals(type)) {
			return this.encode((LocalDate)value);
		}
		if(LocalDateTime.class.equals(type)) {
			return this.encode((LocalDateTime)value);
		}
		if(ZonedDateTime.class.equals(type)) {
			return this.encode((ZonedDateTime)value);
		}
		if(Currency.class.equals(type)) {
			return this.encode((Currency)value);
		}		
		if(Locale.class.equals(type)) {
			return this.encode((Locale)value);
		}
		if(File.class.isAssignableFrom(type)) {
			return this.encode((File)value);
		}
		if(Path.class.isAssignableFrom(type)) {
			return this.encode((Path)value);
		}
		if(URI.class.equals(type)) {
			return this.encode((URI)value);
		}
		if(URL.class.equals(type)) {
			return this.encode((URL)value);
		}
		if(Pattern.class.equals(type)) {
			return this.encode((Pattern)value);
		}
		if(InetAddress.class.isAssignableFrom(type)) {
			return this.encode((InetAddress)value);
		}
		if(Class.class.equals(type)) {
			return this.encode((Class<?>)value);
		}
		throw new ConverterException("Can't encode " + type.getCanonicalName() + " to " + String.class.getCanonicalName());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> String encode(T value, Type type) throws ConverterException {
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
		throw new ConverterException("Can't encode " + type.getTypeName() + " to " + String.class.getCanonicalName());
	}
	
	private <T> String encodeCollection(Collection<T> value) {
		return value != null ?value.stream().map(this::encode).collect(Collectors.joining(String.valueOf(this.arrayListSeparator))) : null;
	}
	
	private <T> String encodeCollection(Collection<T> value, Type type) {
		return value != null ? value.stream().map(element -> this.encode(element, type)).collect(Collectors.joining(String.valueOf(this.arrayListSeparator))) : null;
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
		if(value == null) {
			return null;
		}
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
		if(value == null) {
			return null;
		}
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
		return value != null ? value.toString() : null;
	}

	@Override
	public String encode(Boolean value) throws ConverterException {
		return value != null ? value.toString() : null;
	}

	@Override
	public String encode(String value) throws ConverterException {
		return value;
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
		return value != null ? value.toString() : null;
	}

	@Override
	public String encode(LocalDateTime value) throws ConverterException {
		return value != null ? value.toString() : null;
	}

	@Override
	public String encode(ZonedDateTime value) throws ConverterException {
		return value != null ? value.toString() : null;
	}

	@Override
	public String encode(Currency value) throws ConverterException {
		return value != null ? value.toString() : null;
	}

	@Override
	public String encode(Locale value) throws ConverterException {
		return value != null ? value.toString() : null;
	}

	@Override
	public String encode(File value) throws ConverterException {
		return value != null ? value.getPath() : null;
	}

	@Override
	public String encode(Path value) throws ConverterException {
		return value != null ? value.toString() : null;
	}

	@Override
	public String encode(URI value) throws ConverterException {
		return value != null ? value.toString() : null;
	}

	@Override
	public String encode(URL value) throws ConverterException {
		return value != null ? value.toString() : null;
	}

	@Override
	public String encode(Pattern value) throws ConverterException {
		return value != null ? value.pattern() : null;
	}

	@Override
	public String encode(InetAddress value) throws ConverterException {
		return value != null ? value.getCanonicalHostName() : null;
	}

	@Override
	public String encode(Class<?> value) throws ConverterException {
		return value != null ? value.getCanonicalName() : null;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T decode(String value, Class<T> type) {
		if(value == null) {
			return null;
		}
		if (String.class.equals(type)) {
			return (T) value;
		}
		if(type.isArray()) {
			return (T)this.decodeToArray(value, type.getComponentType());
		}
		if(Boolean.class.equals(type) || Boolean.TYPE.equals(type)) {
			return (T) this.decodeBoolean(value);
		} 
		if(Character.class.equals(type) || Character.TYPE.equals(type)) {
			return (T) this.decodeCharacter(value);
		} 
		if(Integer.class.equals(type) || Integer.TYPE.equals(type)) {
			return (T) this.decodeInteger(value);
		} 
		if(Long.class.equals(type) || Long.TYPE.equals(type)) {
			return (T) this.decodeLong(value);
		} 
		if(Byte.class.equals(type) || Byte.TYPE.equals(type)) {
			return (T) this.decodeByte(value);
		} 
		if(Short.class.equals(type) || Short.TYPE.equals(type)) {
			return (T) this.decodeShort(value);
		} 
		if(Float.class.equals(type) || Float.TYPE.equals(type)) {
			return (T) this.decodeFloat(value);
		} 
		if(Double.class.equals(type) || Double.TYPE.equals(type)) {
			return (T) this.decodeDouble(value);
		} 
		if(BigInteger.class.equals(type)) {
			return (T) this.decodeBigInteger(value);
		} 
		if(BigDecimal.class.equals(type)) {
			return (T) this.decodeBigDecimal(value);
		} 
		if(LocalDate.class.equals(type)) {
			return (T) this.decodeLocalDate(value);
		} 
		if(LocalDateTime.class.equals(type)) {
			return (T) this.decodeLocalDateTime(value);
		} 
		if(ZonedDateTime.class.equals(type)) {
			return (T) this.decodeZonedDateTime(value);
		}
		if (Currency.class.equals(type)) {
			return (T) this.decodeCurrency(value);
		}
		if(File.class.equals(type)) {
			return (T) this.decodeFile(value);
		} 
		if(Path.class.equals(type)) {
			return (T) this.decodePath(value);
		} 
		if(URI.class.equals(type)) {
			return (T) this.decodeURI(value);
		} 
		if(URL.class.equals(type)) {
			return (T) this.decodeURL(value);
		} 
		if(Pattern.class.equals(type)) {
			return (T) this.decodePattern(value);
		} 
		if(Locale.class.equals(type)) {
			return (T) this.decodeLocale(value);
		} 
		if(type.isEnum()) {
			return (T) this.decodeEnum(value, type.asSubclass(Enum.class));
		} 
		if(type.equals(Class.class)) {
			return (T) this.decodeClass(value);
		} 
		if(InetAddress.class.isAssignableFrom(type)) {
			return (T) this.decodeInetAddress(value);
		}
		throw new ConverterException("Can't decode " + String.class.getCanonicalName() + " to " + type.getCanonicalName());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T decode(String value, Type type) throws ConverterException {
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
		throw new ConverterException("Can't decode " + String.class.getCanonicalName() + " to " + type.getTypeName());
	}
	
	@Override
	public <T> List<T> decodeToList(String value, Class<T> type) throws ConverterException {
		return value != null ? this.decodeToList(value, (Type)type) : null;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> decodeToList(String value, Type type) throws ConverterException {
		if(value == null) {
			return null;
		}
		return Arrays.stream(value.split(this.arrayListSeparator)).map(String::trim).map(item -> (T)this.decode(item, type))
				.collect(Collectors.toList());
	}

	@Override
	public <T> Set<T> decodeToSet(String value, Class<T> type) throws ConverterException {
		return this.decodeToSet(value, (Type)type);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> Set<T> decodeToSet(String value, Type type) throws ConverterException {
		if(value == null) {
			return null;
		}
		return Arrays.stream(value.split(this.arrayListSeparator)).map(String::trim).map(item -> (T)this.decode(item, type))
				.collect(Collectors.toSet());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] decodeToArray(String value, Class<T> type) throws ConverterException {
		if(value == null) {
			return null;
		}
		List<T> objects = this.decodeToList(value, type);
		return objects.toArray((T[]) Array.newInstance(type, objects.size()));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] decodeToArray(String value, Type type) throws ConverterException {
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
		try {
			return value != null ? Byte.valueOf(value) : null;
		}
		catch (NumberFormatException e) {
			throw new ConverterException(value + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public Short decodeShort(String value) throws ConverterException {
		try {
			return value != null ? Short.valueOf(value) : null;
		}
		catch (NumberFormatException e) {
			throw new ConverterException(value + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public Integer decodeInteger(String value) throws ConverterException {
		try {
			return value != null ? Integer.valueOf(value) : null;
		}
		catch (NumberFormatException e) {
			throw new ConverterException(value + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public Long decodeLong(String value) throws ConverterException {
		try {
			return value != null ? Long.valueOf(value) : null;
		}
		catch (NumberFormatException e) {
			throw new ConverterException(value + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public Float decodeFloat(String value) throws ConverterException {
		try {
			return value != null ? Float.valueOf(value) : null;
		}
		catch (NumberFormatException e) {
			throw new ConverterException(value + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public Double decodeDouble(String value) throws ConverterException {
		try {
			return value !=null ? Double.valueOf(value) : null;
		} 
		catch (NumberFormatException e) {
			throw new ConverterException(value + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public Character decodeCharacter(String value) throws ConverterException {
		if(value.length() == 1) {
			return value.charAt(0);
		}
		throw new ConverterException(value + " can't be decoded to the requested type");
	}

	@Override
	public Boolean decodeBoolean(String value) throws ConverterException {
		return value != null ? Boolean.valueOf(value) : null;
	}

	@Override
	public String decodeString(String value) throws ConverterException {
		return value;
	}

	@Override
	public BigInteger decodeBigInteger(String value) throws ConverterException {
		try {
			return value != null ? new BigInteger(value) : null;
		} 
		catch (NumberFormatException e) {
			throw new ConverterException(value + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public BigDecimal decodeBigDecimal(String value) throws ConverterException {
		try {
			return value != null ? new BigDecimal(value) : null;
		}
		catch (NumberFormatException e) {
			throw new ConverterException(value + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public LocalDate decodeLocalDate(String value) throws ConverterException {
		try {
			return value != null ? LocalDate.parse(value) : null;
		} 
		catch (Exception e) {
			throw new ConverterException(value + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public LocalDateTime decodeLocalDateTime(String value) throws ConverterException {
		try {
			return value != null ? LocalDateTime.parse(value) : null;
		} 
		catch (DateTimeParseException e) {
			throw new ConverterException(value + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public ZonedDateTime decodeZonedDateTime(String value) throws ConverterException {
		try {
			return value != null ? ZonedDateTime.parse(value) : null;
		} 
		catch (DateTimeParseException e) {
			throw new ConverterException(value + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public Currency decodeCurrency(String value) throws ConverterException {
		try {
			return value != null ? Currency.getInstance(value) : null;
		} 
		catch (IllegalArgumentException e) {
			throw new ConverterException(value + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public Locale decodeLocale(String value) throws ConverterException {
		if(value == null) {
			return null;
		}
		String[] elements = value.split("_");
        if(elements.length >= 1 && (elements[0].length() == 2 || elements[0].length() == 0)) {
            return new Locale(elements[0], elements.length >= 2 ? elements[1] : "", elements.length >= 3 ? elements[2] : "");
        }
        throw new ConverterException(value + " can't be decoded to the requested type");
	}

	@Override
	public File decodeFile(String value) throws ConverterException {
		return value != null ? new File(value) : null;
	}

	@Override
	public Path decodePath(String value) throws ConverterException {
		try {
			return value != null ? Paths.get(value) : null;
		} 
		catch (InvalidPathException e) {
			throw new ConverterException(value + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public URI decodeURI(String value) throws ConverterException {
		try {
			return value != null ? new URI(value) : null;
		} 
		catch (URISyntaxException e) {
			throw new ConverterException(value + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public URL decodeURL(String value) throws ConverterException {
		try {
			return value != null ? new URL(value) : null;
		} 
		catch (MalformedURLException e) {
			throw new ConverterException(value + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public Pattern decodePattern(String value) throws ConverterException {
		try {
			return value != null ? Pattern.compile(value) : null;
		} 
		catch (PatternSyntaxException e) {
			throw new ConverterException(value + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public InetAddress decodeInetAddress(String value) throws ConverterException {
		try {
			return value != null ? InetAddress.getByName(value) : null;
		} 
		catch (UnknownHostException e) {
			throw new ConverterException(value + " can't be decoded to the requested type", e);
		}
	}

	@Override
	public Class<?> decodeClass(String value) throws ConverterException {
		try {
			return value != null ? Class.forName(value) : null;
		} 
		catch (ClassNotFoundException e) {
			throw new ConverterException(value + " can't be decoded to the requested type", e);
		}
	}
	
	private <T extends Enum<T>> T decodeEnum(String value, Class<T> type) {
		try {
			return Enum.valueOf(type, value);
		} 
		catch (IllegalArgumentException e) {
			throw new ConverterException(value + " can't be decoded to the requested type", e);
		}
	}
}

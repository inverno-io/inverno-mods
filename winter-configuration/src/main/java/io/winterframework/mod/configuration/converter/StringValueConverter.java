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
package io.winterframework.mod.configuration.converter;

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

import io.winterframework.mod.configuration.ConversionException;
import io.winterframework.mod.configuration.ValueConverter;

/**
 * @author jkuhn
 *
 */
public class StringValueConverter implements ValueConverter<String> {

	private static final String ARRAY_LIST_SEPARATOR = ",";
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T to(String value, Class<T> type) throws ConversionException {
		if (type.isInstance(value)) {
			return (T) value;
		}
		
		if(type.isArray()) {
			return (T)this.toArrayOf(value, type.getComponentType());
		}
		if (String.class.equals(type)) {
			return (T) value;
		}
		if (Boolean.class.equals(type) || Boolean.TYPE.equals(type)) {
			return (T) this.toBoolean(value);
		} 
		else if (Character.class.equals(type) || Character.TYPE.equals(type)) {
			return (T) this.toCharacter(value);
		} 
		else if (Integer.class.equals(type) || Integer.TYPE.equals(type)) {
			return (T) this.toInteger(value);
		} 
		else if (Long.class.equals(type) || Long.TYPE.equals(type)) {
			return (T) this.toLong(value);
		} 
		else if (Byte.class.equals(type) || Byte.TYPE.equals(type)) {
			return (T) this.toByte(value);
		} 
		else if (Short.class.equals(type) || Short.TYPE.equals(type)) {
			return (T) this.toShort(value);
		} 
		else if (Float.class.equals(type) || Float.TYPE.equals(type)) {
			return (T) this.toFloat(value);
		} 
		else if (Double.class.equals(type) || Double.TYPE.equals(type)) {
			return (T) this.toDouble(value);
		} 
		else if (BigInteger.class.equals(type)) {
			return (T) this.toBigInteger(value);
		} 
		else if (BigDecimal.class.equals(type)) {
			return (T) this.toBigDecimal(value);
		} 
		else if (LocalDate.class.equals(type)) {
			return (T) this.toLocalDate(value);
		} 
		else if (LocalDateTime.class.equals(type)) {
			return (T) this.toLocalDateTime(value);
		} 
		else if (ZonedDateTime.class.equals(type)) {
			return (T) this.toZonedDateTime(value);
		} 
		else if (File.class.equals(type)) {
			return (T) this.toFile(value);
		} 
		else if (Path.class.equals(type)) {
			return (T) this.toPath(value);
		} 
		else if (URI.class.equals(type)) {
			return (T) this.toURI(value);
		} 
		else if (URL.class.equals(type)) {
			return (T) this.toURL(value);
		} 
		else if (Pattern.class.equals(type)) {
			return (T) this.toPattern(value);
		} 
		else if (Locale.class.equals(type)) {
			return (T) this.toLocale(value);
		} 
		else if (type.isEnum()) {
			return (T) this.toEnum(value, type.asSubclass(Enum.class));
		} 
		else if (type.equals(Class.class)) {
			return (T) this.toClass(value);
		} 
		else if (InetAddress.class.isAssignableFrom(type)) {
			return (T) this.toInetAddress(value);
		}
		throw new ConversionException(value + " can't be converted to the requested type: " + type.getCanonicalName());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArrayOf(String value, Class<T> type) throws ConversionException {
		return this.toListOf(value, type).toArray((T[])Array.newInstance(type, 0));
	}
	
	@Override
	public <T> Collection<T> toCollectionOf(String value, Class<T> type) throws ConversionException {
		return this.toListOf(value, type);
	}
	
	@Override
	public <T> List<T> toListOf(String value, Class<T> type) throws ConversionException {
		return Arrays.stream(value.split(ARRAY_LIST_SEPARATOR))
			.map(String::trim)
			.map(item -> this.to(item, type))
			.collect(Collectors.toList());
	}

	@Override
	public <T> Set<T> toSetOf(String value, Class<T> type) throws ConversionException {
		return Arrays.stream(value.split(ARRAY_LIST_SEPARATOR))
			.map(String::trim)
			.map(item -> this.to(item, type))
			.collect(Collectors.toSet());
	}
	
	@Override
	public Byte toByte(String value) throws ConversionException {
		try {
			return Byte.valueOf(value);
		}
		catch (NumberFormatException e) {
			throw new ConversionException(value + " can't be converted to the requested type", e);
		}
	}
	
	@Override
	public Short toShort(String value) throws ConversionException {
		try {
			return Short.valueOf(value);
		}
		catch (NumberFormatException e) {
			throw new ConversionException(value + " can't be converted to the requested type", e);
		}
	}
	
	@Override
	public Integer toInteger(String value) throws ConversionException {
		try {
			return Integer.valueOf(value);
		}
		catch (NumberFormatException e) {
			throw new ConversionException(value + " can't be converted to the requested type", e);
		}
	}
	
	@Override
	public Long toLong(String value) throws ConversionException {
		try {
			return Long.valueOf(value);
		}
		catch (NumberFormatException e) {
			throw new ConversionException(value + " can't be converted to the requested type", e);
		}
	}

	@Override
	public Float toFloat(String value) throws ConversionException {
		try {
			return Float.valueOf(value);
		}
		catch (NumberFormatException e) {
			throw new ConversionException(value + " can't be converted to the requested type", e);
		}
	}

	@Override
	public Double toDouble(String value) throws ConversionException {
		try {
			return Double.valueOf(value);
		} 
		catch (NumberFormatException e) {
			throw new ConversionException(value + " can't be converted to the requested type", e);
		}
	}

	@Override
	public Character toCharacter(String value) throws ConversionException {
		if(value.length() == 1) {
			return value.charAt(0);
		}
		throw new ConversionException(value + " can't be converted to the requested type");
	}

	@Override
	public Boolean toBoolean(String value) throws ConversionException {
		return Boolean.valueOf(value);
	}

	@Override
	public BigInteger toBigInteger(String value) throws ConversionException {
		try {
			return new BigInteger(value);
		} 
		catch (NumberFormatException e) {
			throw new ConversionException(value + " can't be converted to the requested type", e);
		}
	}

	@Override
	public BigDecimal toBigDecimal(String value) throws ConversionException {
		try {
			return new BigDecimal(value);
		}
		catch (NumberFormatException e) {
			throw new ConversionException(value + " can't be converted to the requested type", e);
		}
	}

	@Override
	public LocalDate toLocalDate(String value) throws ConversionException {
		try {
			return LocalDate.parse(value);
		} 
		catch (Exception e) {
			throw new ConversionException(value + " can't be converted to the requested type", e);
		}
	}

	@Override
	public LocalDateTime toLocalDateTime(String value) throws ConversionException {
		try {
			return LocalDateTime.parse(value);
		} 
		catch (DateTimeParseException e) {
			throw new ConversionException(value + " can't be converted to the requested type", e);
		}
	}

	@Override
	public ZonedDateTime toZonedDateTime(String value) throws ConversionException {
		try {
			return ZonedDateTime.parse(value);
		} 
		catch (DateTimeParseException e) {
			throw new ConversionException(value + " can't be converted to the requested type", e);
		}
	}

	@Override
	public Currency toCurrency(String value) throws ConversionException {
		try {
			return Currency.getInstance(value);
		} 
		catch (IllegalArgumentException e) {
			throw new ConversionException(value + " can't be converted to the requested type", e);
		}
	}

	@Override
	public Locale toLocale(String value) throws ConversionException {
		String[] elements = ((String) value).split("_");
        if(elements.length >= 1 && (elements[0].length() == 2 || elements[0].length() == 0)) {
            return new Locale(elements[0], elements.length >= 2 ? elements[1] : "", elements.length >= 3 ? elements[2] : "");
        }
        throw new ConversionException(value + " can't be converted to the requested type");
	}

	@Override
	public File toFile(String value) throws ConversionException {
		return new File(value);
	}

	@Override
	public Path toPath(String value) throws ConversionException {
		try {
			return Paths.get(value);
		} 
		catch (InvalidPathException e) {
			throw new ConversionException(value + " can't be converted to the requested type", e);
		}
	}

	@Override
	public URI toURI(String value) throws ConversionException {
		try {
			return new URI(value);
		} 
		catch (URISyntaxException e) {
			throw new ConversionException(value + " can't be converted to the requested type", e);
		}
	}

	@Override
	public URL toURL(String value) throws ConversionException {
		try {
			return new URL(value);
		} 
		catch (MalformedURLException e) {
			throw new ConversionException(value + " can't be converted to the requested type", e);
		}
	}

	@Override
	public Pattern toPattern(String value) throws ConversionException {
		try {
			return Pattern.compile(value);
		} catch (PatternSyntaxException e) {
			throw new ConversionException(value + " can't be converted to the requested type", e);
		}
	}

	@Override
	public InetAddress toInetAddress(String value) throws ConversionException {
		try {
			return InetAddress.getByName(value);
		} 
		catch (UnknownHostException e) {
			throw new ConversionException(value + " can't be converted to the requested type", e);
		}
	}

	@Override
	public Class<?> toClass(String value) throws ConversionException {
		try {
			return Class.forName(value);
		} 
		catch (ClassNotFoundException e) {
			throw new ConversionException(value + " can't be converted to the requested type", e);
		}
	}
	
	private <T extends Enum<T>> T toEnum(String value, Class<T> type) {
		try {
			return Enum.valueOf(type, value);
		} 
		catch (IllegalArgumentException e) {
			throw new ConversionException(value + " can't be converted to the requested type", e);
		}
	}
}

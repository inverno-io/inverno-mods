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
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>
 * Object to object {@link Decoder} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ObjectConverter
 */
public class ObjectDecoder implements SplittablePrimitiveDecoder<Object> {
	
	private final StringConverter stringConverter;

	/**
	 * <p>
	 * Creates a new Object decoder.
	 * </p>
	 */
	public ObjectDecoder() {
		this.stringConverter = new StringConverter();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T decode(Object value, Class<T> type) {
		if(type.isAssignableFrom(value.getClass())) {
			return (T)value;
		}
		else if(Number.class.isAssignableFrom(type) && value instanceof Number) {
			if(type.equals(Byte.class)) {
				return (T)(Byte)(((Number)value).byteValue());
			}
			else if(type.equals(Double.class)) {
				return (T)(Double)(((Number)value).doubleValue());
			}
			else if(type.equals(Float.class)) {
				return (T)(Float)(((Number)value).floatValue());
			}
			else if(type.equals(Integer.class)) {
				return (T)(Integer)(((Number)value).intValue());
			}
			else if(type.equals(Long.class)) {
				return (T)(Long)(((Number)value).longValue());
			}
			else if(type.equals(Short.class)) {
				return (T)(Short)(((Number)value).shortValue());
			}
		}
		else if(value instanceof CharSequence) {
			return this.stringConverter.decode(value.toString(), type);
		}
		throw new ConverterException(value + " can't be decoded to the requested type: " + type.getCanonicalName());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T decode(Object value, Type type) throws ConverterException {
		if(type instanceof Class) {
			return this.decode(value, (Class<T>)type);
		}
		else if(type instanceof GenericArrayType) {
			return (T) this.decodeToArray(value, ((GenericArrayType)type).getGenericComponentType());
		}
		else if(type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = ((ParameterizedType)type);
			Type rawType = parameterizedType.getRawType();
			if(rawType instanceof Class) {
				if(rawType.equals(Collection.class) || rawType.equals(List.class)) {
					return (T) this.decodeToList(value, parameterizedType.getActualTypeArguments()[0]);
				}
				else if(rawType.equals(Set.class)) {
					return (T) this.decodeToSet(value, parameterizedType.getActualTypeArguments()[0]);
				}
				else {
					// this is best effort some class cast exception might be thrown later due to type erasure
					return this.decode(value, (Class<T>)rawType);
				}
			}
		}
		throw new ConverterException("Can't decode " + String.class.getCanonicalName() + " to " + type.getTypeName());
	}
	
	@SuppressWarnings("unchecked")
	private <T> Collection<T> decodeToCollection(Object value, Class<T> type) {
		if(Collection.class.isAssignableFrom(value.getClass())) {
			Iterator<?> valueIterator = ((Collection<?>)value).iterator();
			if(valueIterator.hasNext()) {
				if(type.isAssignableFrom(valueIterator.next().getClass())) {
					return (Collection<T>)value;
				}
				else {
					throw new ConverterException(value + " is not a collection of " + type.getCanonicalName());
				}
			}
			return (Collection<T>)value;
		}
		else {
			throw new ConverterException(value + " is not a collection");
		}
	}
	
	private <T> Collection<T> decodeToCollection(Object value, Type type, Collection<T> result) {
		if(Collection.class.isAssignableFrom(value.getClass())) {
			Iterator<?> valueIterator = ((Collection<?>)value).iterator();
			if(valueIterator.hasNext()) {
				result.add(this.decode(valueIterator.next(), type));
			}
			return result;
		}
		else {
			throw new ConverterException(value + " is not a collection");
		}
	}
	

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> decodeToList(Object value, Class<T> type) {
		Collection<T> valueCollection = this.decodeToCollection(value, type);
		if(List.class.isAssignableFrom(valueCollection.getClass())) {
			return (List<T>)value;
		}
		else {
			return valueCollection.stream().collect(Collectors.toList());
		}
	}

	@Override
	public <T> List<T> decodeToList(Object value, Type type) {
		List<T> result = new ArrayList<>();
		this.decodeToCollection(value, type, result);
		return result;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> Set<T> decodeToSet(Object value, Class<T> type) {
		Collection<T> valueCollection = this.decodeToCollection(value, type);
		if(List.class.isAssignableFrom(valueCollection.getClass())) {
			return (Set<T>)value;
		}
		else {
			return valueCollection.stream().collect(Collectors.toSet());
		}
	}
	
	@Override
	public <T> Set<T> decodeToSet(Object value, Type type) {
		Set<T> result = new HashSet<>();
		this.decodeToCollection(value, type, result);
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] decodeToArray(Object value, Class<T> type) {
		if(value.getClass().isArray()) {
			if(value.getClass().getComponentType().equals(type)) {
				return (T[])value;
			}
			else {
				T[] array = (T[])Array.newInstance(type, Array.getLength(value));
				for(int i=0;i<array.length;i++) {
					array[i] = this.<T>decode(Array.get(value, i), value.getClass().getComponentType());
				}
				return array;
			}
		}
		else {
			throw new ConverterException(value + " is not an array");
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] decodeToArray(Object value, Type type) {
		if(value.getClass().isArray()) {
			T[] result;
			if(type instanceof Class) {
				result = (T[]) Array.newInstance((Class<T>)type, Array.getLength(value));
			}
			else if(type instanceof ParameterizedType) {
				ParameterizedType parameterizedType = (ParameterizedType)type;
				result = (T[]) Array.newInstance((Class<T>)parameterizedType.getRawType(), Array.getLength(value));
			}
			else {
				throw new ConverterException("Can't decode " + String.class.getCanonicalName() + " to array of " + type.getTypeName());
			}
			
			for(int i = 0;i<result.length;i++) {
				result[i] = this.decode(Array.get(value, i), type);
			}
			return result;
		}
		else {
			throw new ConverterException(value + " is not an array");
		}
	}
	
	@Override
	public Byte decodeByte(Object value) throws ConverterException {
		return this.decode(value, Byte.class);
	}

	@Override
	public Short decodeShort(Object value) throws ConverterException {
		return this.decode(value, Short.class);
	}

	@Override
	public Integer decodeInteger(Object value) throws ConverterException {
		return this.decode(value, Integer.class);
	}

	@Override
	public Long decodeLong(Object value) throws ConverterException {
		return this.decode(value, Long.class);
	}

	@Override
	public Float decodeFloat(Object value) throws ConverterException {
		return this.decode(value, Float.class);
	}

	@Override
	public Double decodeDouble(Object value) throws ConverterException {
		return this.decode(value, Double.class);
	}

	@Override
	public Character decodeCharacter(Object value) throws ConverterException {
		return this.decode(value, Character.class);
	}

	@Override
	public Boolean decodeBoolean(Object value) throws ConverterException {
		return this.decode(value, Boolean.class);
	}

	@Override
	public String decodeString(Object value) throws ConverterException {
		return this.decode(value, String.class);
	}

	@Override
	public BigInteger decodeBigInteger(Object value) throws ConverterException {
		return this.decode(value, BigInteger.class);
	}

	@Override
	public BigDecimal decodeBigDecimal(Object value) throws ConverterException {
		return this.decode(value, BigDecimal.class);
	}

	@Override
	public LocalDate decodeLocalDate(Object value) throws ConverterException {
		return this.decode(value, LocalDate.class);
	}

	@Override
	public LocalDateTime decodeLocalDateTime(Object value) throws ConverterException {
		return this.decode(value, LocalDateTime.class);
	}

	@Override
	public ZonedDateTime decodeZonedDateTime(Object value) throws ConverterException {
		return this.decode(value, ZonedDateTime.class);
	}

	@Override
	public Currency decodeCurrency(Object value) throws ConverterException {
		return this.decode(value, Currency.class);
	}

	@Override
	public Locale decodeLocale(Object value) throws ConverterException {
		return this.decode(value, Locale.class);
	}

	@Override
	public File decodeFile(Object value) throws ConverterException {
		return this.decode(value, File.class);
	}

	@Override
	public Path decodePath(Object value) throws ConverterException {
		return this.decode(value, Path.class);
	}

	@Override
	public URI decodeURI(Object value) throws ConverterException {
		return this.decode(value, URI.class);
	}

	@Override
	public URL decodeURL(Object value) throws ConverterException {
		return this.decode(value, URL.class);
	}

	@Override
	public Pattern decodePattern(Object value) throws ConverterException {
		return this.decode(value, Pattern.class);
	}

	@Override
	public InetAddress decodeInetAddress(Object value) throws ConverterException {
		return this.decode(value, InetAddress.class);
	}

	@Override
	public Class<?> decodeClass(Object value) throws ConverterException {
		return this.decode(value, Class.class);
	}
}

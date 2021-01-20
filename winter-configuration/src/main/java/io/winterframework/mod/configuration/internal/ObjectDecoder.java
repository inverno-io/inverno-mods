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
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Currency;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.winterframework.mod.base.converter.ConverterException;
import io.winterframework.mod.base.converter.SplittablePrimitiveDecoder;

/**
 * @author jkuhn
 *
 */
public class ObjectDecoder implements SplittablePrimitiveDecoder<Object> {

	@SuppressWarnings("unchecked")
	@Override
	public <T> T decode(Object data, Class<T> type) {
		if(type.isAssignableFrom(data.getClass())) {
			return (T)data;
		}
		else {
			throw new ConverterException(data + " can't be decoded to the requested type: " + type.getCanonicalName());
		}
	}
	
	@SuppressWarnings("unchecked")
	private <T> Collection<T> decodeToCollection(Object data, Class<T> type) {
		if(Collection.class.isAssignableFrom(data.getClass())) {
			Iterator<?> valueIterator = ((Collection<?>)data).iterator();
			if(valueIterator.hasNext()) {
				if(type.isAssignableFrom(valueIterator.next().getClass())) {
					return (Collection<T>)data;
				}
				else {
					throw new ConverterException(data + " is not a collection of " + type.getCanonicalName());
				}
			}
			return (Collection<T>)data;
		}
		else {
			throw new ConverterException(data + " is not a collection");
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> decodeToList(Object data, Class<T> type) {
		Collection<T> valueCollection = this.decodeToCollection(data, type);
		if(List.class.isAssignableFrom(valueCollection.getClass())) {
			return (List<T>)data;
		}
		else {
			return valueCollection.stream().collect(Collectors.toList());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Set<T> decodeToSet(Object data, Class<T> type) {
		Collection<T> valueCollection = this.decodeToCollection(data, type);
		if(List.class.isAssignableFrom(valueCollection.getClass())) {
			return (Set<T>)data;
		}
		else {
			return valueCollection.stream().collect(Collectors.toSet());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] decodeToArray(Object data, Class<T> type) {
		if(data.getClass().isArray()) {
			if(data.getClass().getComponentType().equals(type)) {
				return (T[])data;
			}
			else if(type.isAssignableFrom(data.getClass().getComponentType())) {
				T[] array = (T[])Array.newInstance(type, Array.getLength(data));
				for(int i=0;i<array.length;i++) {
					array[i] = (T)Array.get(data, i);
				}
				return array;
			}
			else {
				throw new ConverterException(data + " can't be decoded to an array of " + type.getCanonicalName());
			}
		}
		else {
			throw new ConverterException(data + " is not an array");
		}
	}

	@Override
	public Byte decodeByte(Object data) throws ConverterException {
		return this.decode(data, Byte.class);
	}

	@Override
	public Short decodeShort(Object data) throws ConverterException {
		return this.decode(data, Short.class);
	}

	@Override
	public Integer decodeInteger(Object data) throws ConverterException {
		return this.decode(data, Integer.class);
	}

	@Override
	public Long decodeLong(Object data) throws ConverterException {
		return this.decode(data, Long.class);
	}

	@Override
	public Float decodeFloat(Object data) throws ConverterException {
		return this.decode(data, Float.class);
	}

	@Override
	public Double decodeDouble(Object data) throws ConverterException {
		return this.decode(data, Double.class);
	}

	@Override
	public Character decodeCharacter(Object data) throws ConverterException {
		return this.decode(data, Character.class);
	}

	@Override
	public Boolean decodeBoolean(Object data) throws ConverterException {
		return this.decode(data, Boolean.class);
	}

	@Override
	public String decodeString(Object data) throws ConverterException {
		return this.decode(data, String.class);
	}

	@Override
	public BigInteger decodeBigInteger(Object data) throws ConverterException {
		return this.decode(data, BigInteger.class);
	}

	@Override
	public BigDecimal decodeBigDecimal(Object data) throws ConverterException {
		return this.decode(data, BigDecimal.class);
	}

	@Override
	public LocalDate decodeLocalDate(Object data) throws ConverterException {
		return this.decode(data, LocalDate.class);
	}

	@Override
	public LocalDateTime decodeLocalDateTime(Object data) throws ConverterException {
		return this.decode(data, LocalDateTime.class);
	}

	@Override
	public ZonedDateTime decodeZonedDateTime(Object data) throws ConverterException {
		return this.decode(data, ZonedDateTime.class);
	}

	@Override
	public Currency decodeCurrency(Object data) throws ConverterException {
		return this.decode(data, Currency.class);
	}

	@Override
	public Locale decodeLocale(Object data) throws ConverterException {
		return this.decode(data, Locale.class);
	}

	@Override
	public File decodeFile(Object data) throws ConverterException {
		return this.decode(data, File.class);
	}

	@Override
	public Path decodePath(Object data) throws ConverterException {
		return this.decode(data, Path.class);
	}

	@Override
	public URI decodeURI(Object data) throws ConverterException {
		return this.decode(data, URI.class);
	}

	@Override
	public URL decodeURL(Object data) throws ConverterException {
		return this.decode(data, URL.class);
	}

	@Override
	public Pattern decodePattern(Object data) throws ConverterException {
		return this.decode(data, Pattern.class);
	}

	@Override
	public InetAddress decodeInetAddress(Object data) throws ConverterException {
		return this.decode(data, InetAddress.class);
	}

	@Override
	public Class<?> decodeClass(Object data) throws ConverterException {
		return this.decode(data, Class.class);
	}

}

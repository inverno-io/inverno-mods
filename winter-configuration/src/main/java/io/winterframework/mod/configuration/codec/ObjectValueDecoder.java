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
package io.winterframework.mod.configuration.codec;

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

import io.winterframework.mod.configuration.ValueCodecException;
import io.winterframework.mod.configuration.ValueDecoder;

/**
 * @author jkuhn
 *
 */
public class ObjectValueDecoder implements ValueDecoder<Object> {

	@SuppressWarnings("unchecked")
	@Override
	public <T> T to(Object value, Class<T> type) throws ValueCodecException {
		if(type.isAssignableFrom(value.getClass())) {
			return (T)value;
		}
		else {
			throw new ValueCodecException(value + " can't be decoded to the requested type: " + type.getCanonicalName());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Collection<T> toCollectionOf(Object value, Class<T> type) throws ValueCodecException {
		if(Collection.class.isAssignableFrom(value.getClass())) {
			Iterator<?> valueIterator = ((Collection<?>)value).iterator();
			if(valueIterator.hasNext()) {
				if(type.isAssignableFrom(valueIterator.next().getClass())) {
					return (Collection<T>)value;
				}
				else {
					throw new ValueCodecException(value + " is not a collection of " + type.getCanonicalName());
				}
			}
			return (Collection<T>)value;
		}
		else {
			throw new ValueCodecException(value + " is not a collection");
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> toListOf(Object value, Class<T> type) throws ValueCodecException {
		Collection<T> valueCollection = this.toCollectionOf(value, type);
		if(List.class.isAssignableFrom(valueCollection.getClass())) {
			return (List<T>)value;
		}
		else {
			return valueCollection.stream().collect(Collectors.toList());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Set<T> toSetOf(Object value, Class<T> type) throws ValueCodecException {
		Collection<T> valueCollection = this.toCollectionOf(value, type);
		if(List.class.isAssignableFrom(valueCollection.getClass())) {
			return (Set<T>)value;
		}
		else {
			return valueCollection.stream().collect(Collectors.toSet());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArrayOf(Object value, Class<T> type) throws ValueCodecException {
		if(value.getClass().isArray()) {
			if(value.getClass().getComponentType().equals(type)) {
				return (T[])value;
			}
			else if(type.isAssignableFrom(value.getClass().getComponentType())) {
				T[] array = (T[])Array.newInstance(type, Array.getLength(value));
				for(int i=0;i<array.length;i++) {
					array[i] = (T)Array.get(value, i);
				}
				return array;
			}
			else {
				throw new ValueCodecException(value + " can't be decoded to an array of " + type.getCanonicalName());
			}
		}
		else {
			throw new ValueCodecException(value + " is not an array");
		}
	}

	@Override
	public Byte toByte(Object value) throws ValueCodecException {
		return this.to(value, Byte.class);
	}

	@Override
	public Short toShort(Object value) throws ValueCodecException {
		return this.to(value, Short.class);
	}

	@Override
	public Integer toInteger(Object value) throws ValueCodecException {
		return this.to(value, Integer.class);
	}

	@Override
	public Long toLong(Object value) throws ValueCodecException {
		return this.to(value, Long.class);
	}

	@Override
	public Float toFloat(Object value) throws ValueCodecException {
		return this.to(value, Float.class);
	}

	@Override
	public Double toDouble(Object value) throws ValueCodecException {
		return this.to(value, Double.class);
	}

	@Override
	public Character toCharacter(Object value) throws ValueCodecException {
		return this.to(value, Character.class);
	}

	@Override
	public Boolean toBoolean(Object value) throws ValueCodecException {
		return this.to(value, Boolean.class);
	}
	
	@Override
	public String toString(Object value) throws ValueCodecException {
		return this.to(value, String.class);
	}

	@Override
	public BigInteger toBigInteger(Object value) throws ValueCodecException {
		return this.to(value, BigInteger.class);
	}

	@Override
	public BigDecimal toBigDecimal(Object value) throws ValueCodecException {
		return this.to(value, BigDecimal.class);
	}

	@Override
	public LocalDate toLocalDate(Object value) throws ValueCodecException {
		return this.to(value, LocalDate.class);
	}

	@Override
	public LocalDateTime toLocalDateTime(Object value) throws ValueCodecException {
		return this.to(value, LocalDateTime.class);
	}

	@Override
	public ZonedDateTime toZonedDateTime(Object value) throws ValueCodecException {
		return this.to(value, ZonedDateTime.class);
	}

	@Override
	public Currency toCurrency(Object value) throws ValueCodecException {
		return this.to(value, Currency.class);
	}

	@Override
	public Locale toLocale(Object value) throws ValueCodecException {
		return this.to(value, Locale.class);
	}

	@Override
	public File toFile(Object value) throws ValueCodecException {
		return this.to(value, File.class);
	}

	@Override
	public Path toPath(Object value) throws ValueCodecException {
		return this.to(value, Path.class);
	}

	@Override
	public URI toURI(Object value) throws ValueCodecException {
		return this.to(value, URI.class);
	}

	@Override
	public URL toURL(Object value) throws ValueCodecException {
		return this.to(value, URL.class);
	}

	@Override
	public Pattern toPattern(Object value) throws ValueCodecException {
		return this.to(value, Pattern.class);
	}

	@Override
	public InetAddress toInetAddress(Object value) throws ValueCodecException {
		return this.to(value, InetAddress.class);
	}

	@Override
	public Class<?> toClass(Object value) throws ValueCodecException {
		return this.to(value, Class.class);
	}

}

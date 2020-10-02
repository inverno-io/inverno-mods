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

import org.apache.commons.text.StringEscapeUtils;

import io.winterframework.mod.configuration.ValueCodecException;
import io.winterframework.mod.configuration.ValueEncoder;

/**
 * @author jkuhn
 *
 */
public class StringValueEncoder implements ValueEncoder<String> {

	private static final String ARRAY_LIST_SEPARATOR = ",";
	
	@Override
	public String from(Object value) throws ValueCodecException {
		if(value == null) {
			return null;
		}
		if(value.getClass().isArray()) {
			return this.from((Object[])value);
		}
		if(Collection.class.isAssignableFrom(value.getClass())) {
			return this.from((Collection<?>)value);
		}
		if(Byte.class.equals(value.getClass())) {
			return this.from((Byte)value);
		}
		if(Short.class.equals(value.getClass())) {
			return this.from((Short)value);
		}
		if(Integer.class.equals(value.getClass())) {
			return this.from((Integer)value);
		}
		if(Long.class.equals(value.getClass())) {
			return this.from((Long)value);
		}
		if(Float.class.equals(value.getClass())) {
			return this.from((Float)value);
		}
		if(Double.class.equals(value.getClass())) {
			return this.from((Double)value);
		}
		if(Character.class.equals(value.getClass())) {
			return this.from((Character)value);
		}
		if(Boolean.class.equals(value.getClass())) {
			return this.from((Boolean)value);
		}
		if(String.class.isAssignableFrom(value.getClass())) {
			return this.from((String)value);
		}
		if(BigInteger.class.equals(value.getClass())) {
			return this.from((BigInteger)value);
		}
		if(BigDecimal.class.equals(value.getClass())) {
			return this.from((BigDecimal)value);
		}
		if(LocalDate.class.equals(value.getClass())) {
			return this.from((LocalDate)value);
		}
		if(LocalDateTime.class.equals(value.getClass())) {
			return this.from((LocalDateTime)value);
		}
		if(ZonedDateTime.class.equals(value.getClass())) {
			return this.from((ZonedDateTime)value);
		}
		if(Currency.class.equals(value.getClass())) {
			return this.from((Currency)value);
		}		
		if(Locale.class.equals(value.getClass())) {
			return this.from((Locale)value);
		}
		if(File.class.equals(value.getClass())) {
			return this.from((File)value);
		}
		if(Path.class.equals(value.getClass())) {
			return this.from((Path)value);
		}
		if(URI.class.equals(value.getClass())) {
			return this.from((URI)value);
		}
		if(URL.class.equals(value.getClass())) {
			return this.from((URL)value);
		}
		if(Pattern.class.equals(value.getClass())) {
			return this.from((Pattern)value);
		}
		if(InetAddress.class.equals(value.getClass())) {
			return this.from((InetAddress)value);
		}
		if(Class.class.equals(value.getClass())) {
			return this.from((Class<?>)value);
		}
		throw new ValueCodecException("Value can't be encoded");
	}

	@Override
	public String from(Collection<?> value) throws ValueCodecException {
		return value != null ? value.stream().map(this::from).collect(Collectors.joining(ARRAY_LIST_SEPARATOR)) : null;
	}

	@Override
	public String from(List<?> value) throws ValueCodecException {
		return this.from((Collection<?>)value);
	}

	@Override
	public String from(Set<?> value) throws ValueCodecException {
		return this.from((Collection<?>)value);
	}

	@Override
	public String from(Object[] value) throws ValueCodecException {
		return value != null ? Arrays.stream(value).map(this::from).collect(Collectors.joining(ARRAY_LIST_SEPARATOR)) : null;
	}

	@Override
	public String from(Byte value) throws ValueCodecException {
		return value != null ? value.toString() : null;
	}

	@Override
	public String from(Short value) throws ValueCodecException {
		return value != null ? value.toString() : null;
	}

	@Override
	public String from(Integer value) throws ValueCodecException {
		return value != null ?  value.toString() : null;
	}

	@Override
	public String from(Long value) throws ValueCodecException {
		return value != null ? value.toString() : null;
	}

	@Override
	public String from(Float value) throws ValueCodecException {
		return value != null ? value.toString() : null;
	}

	@Override
	public String from(Double value) throws ValueCodecException {
		return value != null ? value.toString() : null;
	}

	@Override
	public String from(Character value) throws ValueCodecException {
		return value != null ? StringEscapeUtils.escapeJava(value.toString()) : null;
	}

	@Override
	public String from(Boolean value) throws ValueCodecException {
		return value != null ? value.toString() : null;
	}
	
	@Override
	public String from(String value) throws ValueCodecException {
		return value != null ? StringEscapeUtils.escapeJava(value.toString()) : null;
	}

	@Override
	public String from(BigInteger value) throws ValueCodecException {
		return value != null ? value.toString() : null;
	}

	@Override
	public String from(BigDecimal value) throws ValueCodecException {
		return value != null ? value.toString() : null;
	}

	@Override
	public String from(LocalDate value) throws ValueCodecException {
		return value != null ? StringEscapeUtils.escapeJava(value.toString()) : null;
	}

	@Override
	public String from(LocalDateTime value) throws ValueCodecException {
		return value != null ? StringEscapeUtils.escapeJava(value.toString()) : null;
	}

	@Override
	public String from(ZonedDateTime value) throws ValueCodecException {
		return value != null ? StringEscapeUtils.escapeJava(value.toString()) : null;
	}

	@Override
	public String from(Currency value) throws ValueCodecException {
		return value != null ? StringEscapeUtils.escapeJava(value.toString()) : null;
	}

	@Override
	public String from(Locale value) throws ValueCodecException {
		return value != null ? StringEscapeUtils.escapeJava(value.toString()) : null;
	}

	@Override
	public String from(File value) throws ValueCodecException {
		return value != null ? StringEscapeUtils.escapeJava(value.getPath()) : null;
	}

	@Override
	public String from(Path value) throws ValueCodecException {
		return value != null ? StringEscapeUtils.escapeJava(value.toString()) : null;
	}

	@Override
	public String from(URI value) throws ValueCodecException {
		return value != null ? StringEscapeUtils.escapeJava(value.toString()) : null;
	}

	@Override
	public String from(URL value) throws ValueCodecException {
		return value != null ? StringEscapeUtils.escapeJava(value.toString()) : null;
	}

	@Override
	public String from(Pattern value) throws ValueCodecException {
		return value != null ? StringEscapeUtils.escapeJava(value.pattern()) : null;
	}

	@Override
	public String from(InetAddress value) throws ValueCodecException {
		return value != null ? StringEscapeUtils.escapeJava(value.getCanonicalHostName()) : null;
	}

	@Override
	public String from(Class<?> value) throws ValueCodecException {
		return value != null ? value.getCanonicalName() : null;
	}
}

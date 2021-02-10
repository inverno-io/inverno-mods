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
package io.winterframework.mod.web.internal.server;

import java.io.File;
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
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import io.winterframework.mod.base.converter.ObjectConverter;
import io.winterframework.mod.web.Parameter;

/**
 * @author jkuhn
 *
 */
public class GenericParameter implements Parameter {

	protected final ObjectConverter<String> parameterConverter;
	
	protected final String name;
	
	protected final String value;
	
	public GenericParameter(ObjectConverter<String> parameterConverter, String name, String value) {
		this.parameterConverter = parameterConverter;
		this.name = name;
		this.value = value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getValue() {
		return this.value;
	}

	@Override
	public <T> T as(Class<T> type) {
		return this.parameterConverter.decode(this.value, type);
	}
	
	@Override
	public <T> T as(Type type) {
		return this.parameterConverter.decode(this.value, type);
	}

	@Override
	public <T> T[] asArrayOf(Class<T> type) {
		return this.parameterConverter.decodeToArray(this.value, type);
	}
	
	@Override
	public <T> T[] asArrayOf(Type type) {
		return this.parameterConverter.decodeToArray(this.value, type);
	}

	@Override
	public <T> List<T> asListOf(Class<T> type) {
		return this.parameterConverter.decodeToList(this.value, type);
	}
	
	@Override
	public <T> List<T> asListOf(Type type) {
		return this.parameterConverter.decodeToList(this.value, type);
	}

	@Override
	public <T> Set<T> asSetOf(Class<T> type) {
		return this.parameterConverter.decodeToSet(this.value, type);
	}
	
	@Override
	public <T> Set<T> asSetOf(Type type) {
		return this.parameterConverter.decodeToSet(this.value, type);
	}

	@Override
	public Byte asByte() {
		return this.parameterConverter.decodeByte(this.value);
	}

	@Override
	public Short asShort() {
		return this.parameterConverter.decodeShort(this.value);
	}

	@Override
	public Integer asInteger() {
		return this.parameterConverter.decodeInteger(this.value);
	}

	@Override
	public Long asLong() {
		return this.parameterConverter.decodeLong(this.value);
	}

	@Override
	public Float asFloat() {
		return this.parameterConverter.decodeFloat(this.value);
	}

	@Override
	public Double asDouble() {
		return this.parameterConverter.decodeDouble(this.value);
	}

	@Override
	public Character asCharacter() {
		return this.parameterConverter.decodeCharacter(this.value);
	}

	@Override
	public String asString() {
		return this.parameterConverter.decodeString(this.value);
	}

	@Override
	public Boolean asBoolean() {
		return this.parameterConverter.decodeBoolean(this.value);
	}

	@Override
	public BigInteger asBigInteger() {
		return this.parameterConverter.decodeBigInteger(this.value);
	}

	@Override
	public BigDecimal asBigDecimal() {
		return this.parameterConverter.decodeBigDecimal(this.value);
	}

	@Override
	public LocalDate asLocalDate() {
		return this.parameterConverter.decodeLocalDate(this.value);
	}

	@Override
	public LocalDateTime asLocalDateTime() {
		return this.parameterConverter.decodeLocalDateTime(this.value);
	}

	@Override
	public ZonedDateTime asZonedDateTime() {
		return this.parameterConverter.decodeZonedDateTime(this.value);
	}

	@Override
	public Currency asCurrency() {
		return this.parameterConverter.decodeCurrency(this.value);
	}

	@Override
	public Locale asLocale() {
		return this.parameterConverter.decodeLocale(this.value);
	}

	@Override
	public File asFile() {
		return this.parameterConverter.decodeFile(this.value);
	}

	@Override
	public Path asPath() {
		return this.parameterConverter.decodePath(this.value);
	}

	@Override
	public URI asURI() {
		return this.parameterConverter.decodeURI(this.value);
	}

	@Override
	public URL asURL() {
		return this.parameterConverter.decodeURL(this.value);
	}

	@Override
	public Pattern asPattern() {
		return this.parameterConverter.decodePattern(this.value);
	}

	@Override
	public InetAddress asInetAddress() {
		return this.parameterConverter.decodeInetAddress(this.value);
	}

	@Override
	public Class<?> asClass() {
		return this.parameterConverter.decodeClass(this.value);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GenericParameter other = (GenericParameter) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
}

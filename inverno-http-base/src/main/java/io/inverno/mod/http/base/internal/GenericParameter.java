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
package io.inverno.mod.http.base.internal;

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
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.Parameter;

/**
 * <p>
 * Generic {@link Parameter} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Parameter
 */
public class GenericParameter implements Parameter {

	/**
	 * The parameter value converter. 
	 */
	protected final ObjectConverter<String> parameterConverter;
	
	/**
	 * The parameter name.
	 */
	protected final String name;
	
	/**
	 * The parameter value.
	 */
	protected final String value;
	
	/**
	 * <p>
	 * Creates a generic parameter with the specified parameter name, parameter value and parameter value converter.
	 * </p>
	 *
	 * @param name               a parameter name
	 * @param value              a parameter value
	 * @param parameterConverter a string object converter
	 */
	public GenericParameter(String name, String value, ObjectConverter<String> parameterConverter) {
		this.parameterConverter = parameterConverter;
		this.name = name;
		this.value = value;
	}
	
	/**
	 * <p>
	 * Creates a generic parameter with the specified parameter name, parameter value and parameter value converter.
	 * </p>
	 *
	 * @param name               a parameter name
	 * @param value              a parameter value
	 * @param parameterConverter a string object converter
	 */
	public GenericParameter(String name, Object value, ObjectConverter<String> parameterConverter) {
		this.parameterConverter = parameterConverter;
		this.name = name;
		this.value = parameterConverter.encode(value);
	}

	/**
	 * <p>
	 * Creates a generic parameter with the specified parameter name, parameter value and parameter value converter.
	 * </p>
	 *
	 * @param name               a parameter name
	 * @param value              a parameter value
	 * @param parameterConverter a string object converter
	 * @param type               the value type
	 */
	public GenericParameter(String name, Object value, ObjectConverter<String> parameterConverter, Class<?> type) {
		this(name, value, parameterConverter, (Type)type);
	}

	/**
	 * <p>
	 * Creates a generic parameter with the specified parameter name, parameter value and parameter value converter.
	 * </p>
	 *
	 * @param name               a parameter name
	 * @param value              a parameter value
	 * @param parameterConverter a string object converter
	 * @param type               the value type
	 */
	public GenericParameter(String name, Object value, ObjectConverter<String> parameterConverter, Type type) {
		this.parameterConverter = parameterConverter;
		this.name = name;
		this.value = parameterConverter.encode(value, type);
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
	public boolean equals(Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;
		GenericParameter that = (GenericParameter) o;
		return Objects.equals(name, that.name) && Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, value);
	}
}

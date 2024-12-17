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
package io.inverno.mod.web.server.internal;

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
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.web.server.PathParameters;
import io.inverno.mod.web.server.WebRequest;

/**
 * <p>
 * Generic {@link PathParameters} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see WebRequest
 */
public class GenericPathParameters implements PathParameters {

	private final ObjectConverter<String> parameterConverter;
	
	private final Map<String, Parameter> parameters;
	
	/**
	 * <p>
	 * Creates generic path parameters.
	 * </p>
	 * 
	 * @param parameterConverter a string object converter
	 */
	public GenericPathParameters(ObjectConverter<String> parameterConverter) {
		this.parameterConverter = parameterConverter;
		this.parameters = new HashMap<>();
	}

	/**
	 * <p>
	 * Creates generic path parameters initialized with the specified parameters.
	 * </p>
	 *
	 * @param parameterConverter a string object converter
	 * @param parameters         the initial parameters
	 */
	public GenericPathParameters(ObjectConverter<String> parameterConverter, Map<String, String> parameters) {
		this.parameterConverter = parameterConverter;
		this.parameters = new HashMap<>();
		this.putAll(parameters);
	}

	/**
	 * <p>
	 * Sets the specified parameter.
	 * </p>
	 *
	 * <p>
	 * When the specified value is empty, it is considered null (i.e. missing).
	 * </p>
	 *
	 * @param name  the parameter name
	 * @param value the parameter value
	 */
	public void put(String name, String value) {
		this.parameters.put(name, value != null && !value.isEmpty() ? new GenericPathParameter(name, value) : null);
	}

	/**
	 * <p>
	 * Sets all specified parameters.
	 * </p>
	 *
	 * <p>
	 * When a specified value is empty, it is considered null (i.e. missing).
	 * </p>
	 *
	 * @param parameters a map of parameters
	 */
	public void putAll(Map<String, String> parameters) {
		for(Map.Entry<String, String> e : parameters.entrySet()) {
			String name = e.getKey();
			String value = e.getValue();
			this.parameters.put(name, value != null && !value.isEmpty() ? new GenericPathParameter(name, value) : null);
		}
	}

	/**
	 * <p>
	 * Removes the parameter with the specified name.
	 * </p>
	 *
	 * @param name the parameter name
	 *
	 * @return the removed value or null if no value was removed
	 */
	public String remove(String name) {
		Parameter removedParameter = this.parameters.remove(name);
		return removedParameter != null ? removedParameter.getValue() : null;
	}

	@Override
	public Set<String> getNames() {
		return this.parameters.keySet();
	}

	@Override
	public Optional<Parameter> get(String name) {
		return Optional.ofNullable(this.parameters.get(name));
	}

	@Override
	public Map<String, Parameter> getAll() {
		return Collections.unmodifiableMap(this.parameters);
	}

	/**
	 * <p>
	 * Generic path {@link Parameter} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	private class GenericPathParameter implements Parameter {

		private final String name;
		
		private final String value;
		
		/**
		 * <p>
		 * Creates a generic path parameter with the specified name and value.
		 * </p>
		 * 
		 * @param name  the parameter name
		 * @param value the parameter value
		 */
		public GenericPathParameter(String name, String value) {
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
			return GenericPathParameters.this.parameterConverter.decode(this.value, type);
		}
		
		@Override
		public <T> T as(Type type) {
			return GenericPathParameters.this.parameterConverter.decode(this.value, type);
		}

		@Override
		public <T> T[] asArrayOf(Class<T> type) {
			return GenericPathParameters.this.parameterConverter.decodeToArray(this.value, type);
		}
		
		@Override
		public <T> T[] asArrayOf(Type type) {
			return GenericPathParameters.this.parameterConverter.decodeToArray(this.value, type);
		}

		@Override
		public <T> List<T> asListOf(Class<T> type) {
			return GenericPathParameters.this.parameterConverter.decodeToList(this.value, type);
		}

		@Override
		public <T> List<T> asListOf(Type type) {
			return GenericPathParameters.this.parameterConverter.decodeToList(this.value, type);
		}
		
		@Override
		public <T> Set<T> asSetOf(Class<T> type) {
			return GenericPathParameters.this.parameterConverter.decodeToSet(this.value, type);
		}
		
		@Override
		public <T> Set<T> asSetOf(Type type) {
			return GenericPathParameters.this.parameterConverter.decodeToSet(this.value, type);
		}

		@Override
		public Byte asByte() {
			return GenericPathParameters.this.parameterConverter.decodeByte(this.value);
		}

		@Override
		public Short asShort() {
			return GenericPathParameters.this.parameterConverter.decodeShort(this.value);
		}

		@Override
		public Integer asInteger() {
			return GenericPathParameters.this.parameterConverter.decodeInteger(this.value);
		}

		@Override
		public Long asLong() {
			return GenericPathParameters.this.parameterConverter.decodeLong(this.value);
		}

		@Override
		public Float asFloat() {
			return GenericPathParameters.this.parameterConverter.decodeFloat(this.value);
		}

		@Override
		public Double asDouble() {
			return GenericPathParameters.this.parameterConverter.decodeDouble(this.value);
		}

		@Override
		public Character asCharacter() {
			return GenericPathParameters.this.parameterConverter.decodeCharacter(this.value);
		}

		@Override
		public String asString() {
			return GenericPathParameters.this.parameterConverter.decodeString(this.value);
		}

		@Override
		public Boolean asBoolean() {
			return GenericPathParameters.this.parameterConverter.decodeBoolean(this.value);
		}

		@Override
		public BigInteger asBigInteger() {
			return GenericPathParameters.this.parameterConverter.decodeBigInteger(this.value);
		}

		@Override
		public BigDecimal asBigDecimal() {
			return GenericPathParameters.this.parameterConverter.decodeBigDecimal(this.value);
		}

		@Override
		public LocalDate asLocalDate() {
			return GenericPathParameters.this.parameterConverter.decodeLocalDate(this.value);
		}

		@Override
		public LocalDateTime asLocalDateTime() {
			return GenericPathParameters.this.parameterConverter.decodeLocalDateTime(this.value);
		}

		@Override
		public ZonedDateTime asZonedDateTime() {
			return GenericPathParameters.this.parameterConverter.decodeZonedDateTime(this.value);
		}

		@Override
		public Currency asCurrency() {
			return GenericPathParameters.this.parameterConverter.decodeCurrency(this.value);
		}

		@Override
		public Locale asLocale() {
			return GenericPathParameters.this.parameterConverter.decodeLocale(this.value);
		}

		@Override
		public File asFile() {
			return GenericPathParameters.this.parameterConverter.decodeFile(this.value);
		}

		@Override
		public Path asPath() {
			return GenericPathParameters.this.parameterConverter.decodePath(this.value);
		}

		@Override
		public URI asURI() {
			return GenericPathParameters.this.parameterConverter.decodeURI(this.value);
		}

		@Override
		public URL asURL() {
			return GenericPathParameters.this.parameterConverter.decodeURL(this.value);
		}

		@Override
		public Pattern asPattern() {
			return GenericPathParameters.this.parameterConverter.decodePattern(this.value);
		}

		@Override
		public InetAddress asInetAddress() {
			return GenericPathParameters.this.parameterConverter.decodeInetAddress(this.value);
		}

		@Override
		public Class<?> asClass() {
			return GenericPathParameters.this.parameterConverter.decodeClass(this.value);
		}
	}
}

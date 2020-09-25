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
package io.winterframework.mod.configuration.internal;

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
import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import io.winterframework.mod.configuration.ConfigurationEntry;
import io.winterframework.mod.configuration.ConfigurationKey;
import io.winterframework.mod.configuration.ConfigurationSource;
import io.winterframework.mod.configuration.ValueConverter;

/**
 * @author jkuhn
 *
 */
public class GenericConfigurationEntry<A extends ConfigurationKey, B extends ConfigurationSource<?,?,?>, C> implements ConfigurationEntry<A, B> {

	protected A key;

	protected B source;
	
	protected C value;
	
	protected ValueConverter<C> converter;
	
	protected boolean unset;
	
	public GenericConfigurationEntry(A key, C value, B source, ValueConverter<C> converter) {
		if(converter == null) {
			throw new NullPointerException("Converter can't be null");
		}
		this.key = key;
		this.value = value;
		this.source = source;
		this.converter = converter;
	}
	
	public GenericConfigurationEntry(A key, B source, boolean unset) {
		this.key = key;
		this.source = source;
		this.unset = unset;
	}
	
	@Override
	public A getKey() {
		return this.key;
	}

	@Override
	public B getSource() {
		return this.source;
	}

	@Override
	public boolean isPresent() {
		return this.value != null;
	}

	@Override
	public boolean isEmpty() {
		return this.value == null;
	}
	
	@Override
	public boolean isUnset() {
		return this.unset;
	}
	
	@Override
	public <T> Optional<T> valueAs(Class<T> type) {
		return Optional.ofNullable(this.value != null ? this.converter.to(this.value, type) : null);
	}
	
	@Override
	public <T> Optional<Collection<T>> valueAsCollectionOf(Class<T> type) {
		return Optional.ofNullable(this.value != null ? this.converter.toCollectionOf(this.value, type) : null);
	}
	
	@Override
	public <T> Optional<List<T>> valueAsListOf(Class<T> type) {
		return Optional.ofNullable(this.value != null ? this.converter.toListOf(this.value, type) : null);
	}

	@Override
	public <T> Optional<Set<T>> valueAsSetOf(Class<T> type) {
		return Optional.ofNullable(this.value != null ? this.converter.toSetOf(this.value, type) : null);
	}
	
	@Override
	public <T> Optional<T[]> valueAsArrayOf(Class<T> type) {
		return Optional.ofNullable(this.value != null ? this.converter.toArrayOf(this.value, type) : null);
	}

	@Override
	public Optional<Byte> valueAsByte() {
		return Optional.ofNullable(this.value != null ? this.converter.toByte(this.value) : null);
	}

	@Override
	public Optional<Short> valueAsShort() {
		return Optional.ofNullable(this.value != null ? this.converter.toShort(this.value) : null);
	}

	@Override
	public Optional<Integer> valueAsInteger() {
		return Optional.ofNullable(this.value != null ? this.converter.toInteger(this.value) : null);
	}

	@Override
	public Optional<Long> valueAsLong() {
		return Optional.ofNullable(this.value != null ? this.converter.toLong(this.value) : null);
	}

	@Override
	public Optional<Float> valueAsFloat() {
		return Optional.ofNullable(this.value != null ? this.converter.toFloat(this.value) : null);
	}

	@Override
	public Optional<Double> valueAsDouble() {
		return Optional.ofNullable(this.value != null ? this.converter.toDouble(this.value) : null);
	}

	@Override
	public Optional<Character> valueAsCharacter() {
		return Optional.ofNullable(this.value != null ? this.converter.toCharacter(this.value) : null);
	}

	@Override
	public Optional<String> valueAsString() {
		return Optional.ofNullable(this.value != null ? this.value.toString() : null);
	}

	@Override
	public Optional<Boolean> valueAsBoolean() {
		return Optional.ofNullable(this.value != null ? this.converter.toBoolean(this.value) : null);
	}

	@Override
	public Optional<BigInteger> valueAsBigInteger() {
		return Optional.ofNullable(this.value != null ? this.converter.toBigInteger(this.value) : null);
	}

	@Override
	public Optional<BigDecimal> valueAsBigDecimal() {
		return Optional.ofNullable(this.value != null ? this.converter.toBigDecimal(this.value) : null);
	}

	@Override
	public Optional<LocalDate> valueAsLocalDate() {
		return Optional.ofNullable(this.value != null ? this.converter.toLocalDate(this.value) : null);
	}

	@Override
	public Optional<LocalDateTime> valueAsLocalDateTime() {
		return Optional.ofNullable(this.value != null ? this.converter.toLocalDateTime(this.value) : null);
	}

	@Override
	public Optional<ZonedDateTime> valueAsZonedDateTime() {
		return Optional.ofNullable(this.value != null ? this.converter.toZonedDateTime(this.value) : null);
	}

	@Override
	public Optional<Currency> valueAsCurrency() {
		return Optional.ofNullable(this.value != null ? this.converter.toCurrency(this.value) : null);
	}

	@Override
	public Optional<Locale> valueAsLocale() {
		return Optional.ofNullable(this.value != null ? this.converter.toLocale(this.value) : null);
	}

	@Override
	public Optional<File> valueAsFile() {
		return Optional.ofNullable(this.value != null ? this.converter.toFile(this.value) : null);
	}

	@Override
	public Optional<Path> valueAsPath() {
		return Optional.ofNullable(this.value != null ? this.converter.toPath(this.value) : null);
	}

	@Override
	public Optional<URI> valueAsURI() {
		return Optional.ofNullable(this.value != null ? this.converter.toURI(this.value) : null);
	}

	@Override
	public Optional<URL> valueAsURL() {
		return Optional.ofNullable(this.value != null ? this.converter.toURL(this.value) : null);
	}

	@Override
	public Optional<Pattern> valueAsPattern() {
		return Optional.ofNullable(this.value != null ? this.converter.toPattern(this.value) : null);
	}

	@Override
	public Optional<InetAddress> valueAsInetAddress() {
		return Optional.ofNullable(this.value != null ? this.converter.toInetAddress(this.value) : null);
	}

	@Override
	public Optional<Class<?>> valueAsClass() {
		return Optional.ofNullable(this.value != null ? this.converter.toClass(this.value) : null);
	}
	
	@Override
	public String toString() {
		return this.key.toString() + " = " + this.value;
	}
}

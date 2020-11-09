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

import io.winterframework.mod.configuration.AbstractConfigurationSource;
import io.winterframework.mod.configuration.ConfigurationProperty;
import io.winterframework.mod.configuration.ConfigurationKey;

/**
 * @author jkuhn
 *
 */
public class GenericConfigurationProperty<A extends ConfigurationKey, B extends AbstractConfigurationSource<?,?,?, C>, C> implements ConfigurationProperty<A, B> {

	protected A key;

	protected B source;
	
	protected C value;
	
	protected boolean unset;
	
	public GenericConfigurationProperty(A key, C value, B source) {
		this.key = key;
		this.value = value;
		this.source = source;
	}
	
	public GenericConfigurationProperty(A key, B source) {
		this.key = key;
		this.source = source;
		this.unset = true;
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
	public <T> Optional<T> as(Class<T> type) {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().to(this.value, type) : null);
	}
	
	@Override
	public <T> Optional<Collection<T>> asCollectionOf(Class<T> type) {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toCollectionOf(this.value, type) : null);
	}
	
	@Override
	public <T> Optional<List<T>> asListOf(Class<T> type) {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toListOf(this.value, type) : null);
	}

	@Override
	public <T> Optional<Set<T>> asSetOf(Class<T> type) {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toSetOf(this.value, type) : null);
	}
	
	@Override
	public <T> Optional<T[]> asArrayOf(Class<T> type) {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toArrayOf(this.value, type) : null);
	}

	@Override
	public Optional<Byte> asByte() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toByte(this.value) : null);
	}

	@Override
	public Optional<Short> asShort() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toShort(this.value) : null);
	}

	@Override
	public Optional<Integer> asInteger() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toInteger(this.value) : null);
	}

	@Override
	public Optional<Long> asLong() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toLong(this.value) : null);
	}

	@Override
	public Optional<Float> asFloat() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toFloat(this.value) : null);
	}

	@Override
	public Optional<Double> asDouble() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toDouble(this.value) : null);
	}

	@Override
	public Optional<Character> asCharacter() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toCharacter(this.value) : null);
	}

	@Override
	public Optional<String> asString() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toString(this.value) : null);
	}

	@Override
	public Optional<Boolean> asBoolean() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toBoolean(this.value) : null);
	}

	@Override
	public Optional<BigInteger> asBigInteger() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toBigInteger(this.value) : null);
	}

	@Override
	public Optional<BigDecimal> asBigDecimal() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toBigDecimal(this.value) : null);
	}

	@Override
	public Optional<LocalDate> asLocalDate() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toLocalDate(this.value) : null);
	}

	@Override
	public Optional<LocalDateTime> asLocalDateTime() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toLocalDateTime(this.value) : null);
	}

	@Override
	public Optional<ZonedDateTime> asZonedDateTime() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toZonedDateTime(this.value) : null);
	}

	@Override
	public Optional<Currency> asCurrency() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toCurrency(this.value) : null);
	}

	@Override
	public Optional<Locale> asLocale() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toLocale(this.value) : null);
	}

	@Override
	public Optional<File> asFile() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toFile(this.value) : null);
	}

	@Override
	public Optional<Path> asPath() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toPath(this.value) : null);
	}

	@Override
	public Optional<URI> asURI() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toURI(this.value) : null);
	}

	@Override
	public Optional<URL> asURL() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toURL(this.value) : null);
	}

	@Override
	public Optional<Pattern> asPattern() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toPattern(this.value) : null);
	}

	@Override
	public Optional<InetAddress> asInetAddress() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toInetAddress(this.value) : null);
	}

	@Override
	public Optional<Class<?>> asClass() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().toClass(this.value) : null);
	}
	
	@Override
	public String toString() {
		return this.key.toString() + " = " + this.value;
	}
}

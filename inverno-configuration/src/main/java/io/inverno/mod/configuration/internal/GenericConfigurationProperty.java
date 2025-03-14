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
package io.inverno.mod.configuration.internal;

import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.ConfigurationProperty;
import java.io.File;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * <p>
 * Generic {@link ConfigurationProperty} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ConfigurationProperty
 *
 * @param <A> the key type
 * @param <B> the source type
 * @param <C> the raw value type
 */
public class GenericConfigurationProperty<A extends ConfigurationKey, B extends AbstractConfigurationSource<?, ?, ?, C, ?>, C> implements ConfigurationProperty {

	protected final A key;

	protected final B source;
	
	protected C value;
	
	protected boolean unset;
	
	/**
	 * <p>
	 * Creates a generic configuration property with the specified key, raw value and source.
	 * </p>
	 *
	 * @param key    the property key
	 * @param value  the property raw value
	 * @param source the property source
	 */
	public GenericConfigurationProperty(A key, C value, B source) {
		this.key = key;
		this.value = value;
		this.source = source;
	}
	
	/**
	 * <p>
	 * Creates a generic unset configuration property with the specified key and source.
	 * </p>
	 *
	 * @param key    the property key
	 * @param source the property source
	 */
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
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decode(this.value, type) : null);
	}

	@Override
	public <T> T as(Class<T> type, T defaultValue) {
		return this.value != null ? this.source.getDecoder().decode(this.value, type) : defaultValue;
	}

	@Override
	public <T> Optional<T> as(Type type) {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decode(this.value, type) : null);
	}

	@Override
	public <T> T as(Type type, T defaultValue) {
		return this.value != null ? this.source.getDecoder().decode(this.value, type) : defaultValue;
	}

	@Override
	public <T> Optional<T[]> asArrayOf(Class<T> type) {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeToArray(this.value, type) : null);
	}

	@Override
	public <T> T[] asArrayOf(Class<T> type, T[] defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeToArray(this.value, type) : defaultValue;
	}

	@Override
	public <T> Optional<T[]> asArrayOf(Type type) {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeToArray(this.value, type) : null);
	}

	@Override
	public <T> T[] asArrayOf(Type type, T[] defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeToArray(this.value, type) : defaultValue;
	}

	@Override
	public <T> Optional<List<T>> asListOf(Class<T> type) {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeToList(this.value, type) : null);
	}

	@Override
	public <T> List<T> asListOf(Class<T> type, List<T> defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeToList(this.value, type) : defaultValue;
	}

	@Override
	public <T> Optional<List<T>> asListOf(Type type) {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeToList(this.value, type) : null);
	}

	@Override
	public <T> List<T> asListOf(Type type, List<T> defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeToList(this.value, type) : defaultValue;
	}

	@Override
	public <T> Optional<Set<T>> asSetOf(Class<T> type) {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeToSet(this.value, type) : null);
	}

	@Override
	public <T> Set<T> asSetOf(Class<T> type, Set<T> defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeToSet(this.value, type) : defaultValue;
	}

	@Override
	public <T> Optional<Set<T>> asSetOf(Type type) {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeToSet(this.value, type) : null);
	}

	@Override
	public <T> Set<T> asSetOf(Type type, Set<T> defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeToSet(this.value, type) : defaultValue;
	}

	@Override
	public Optional<Byte> asByte() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeByte(this.value) : null);
	}

	@Override
	public byte asByte(byte defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeByte(this.value) : defaultValue;
	}

	@Override
	public Optional<Short> asShort() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeShort(this.value) : null);
	}

	@Override
	public short asShort(short defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeShort(this.value) : defaultValue;
	}

	@Override
	public Optional<Integer> asInteger() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeInteger(this.value) : null);
	}

	@Override
	public int asInteger(int defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeInteger(this.value) : defaultValue;
	}

	@Override
	public Optional<Long> asLong() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeLong(this.value) : null);
	}

	@Override
	public long asLong(long defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeLong(this.value) : defaultValue;
	}

	@Override
	public Optional<Float> asFloat() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeFloat(this.value) : null);
	}

	@Override
	public float asFloat(float defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeFloat(this.value) : defaultValue;
	}

	@Override
	public Optional<Double> asDouble() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeDouble(this.value) : null);
	}

	@Override
	public double asDouble(double defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeDouble(this.value) : defaultValue;
	}

	@Override
	public Optional<Character> asCharacter() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeCharacter(this.value) : null);
	}

	@Override
	public char asCharacter(char defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeCharacter(this.value) : defaultValue;
	}

	@Override
	public Optional<String> asString() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeString(this.value) : null);
	}

	@Override
	public String asString(String defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeString(this.value) : defaultValue;
	}

	@Override
	public Optional<Boolean> asBoolean() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeBoolean(this.value) : null);
	}

	@Override
	public boolean asBoolean(boolean defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeBoolean(this.value) : defaultValue;
	}

	@Override
	public Optional<BigInteger> asBigInteger() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeBigInteger(this.value) : null);
	}

	@Override
	public BigInteger asBigInteger(BigInteger defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeBigInteger(this.value) : defaultValue;
	}

	@Override
	public Optional<BigDecimal> asBigDecimal() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeBigDecimal(this.value) : null);
	}

	@Override
	public BigDecimal asBigDecimal(BigDecimal defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeBigDecimal(this.value) : defaultValue;
	}

	@Override
	public Optional<LocalDate> asLocalDate() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeLocalDate(this.value) : null);
	}

	@Override
	public LocalDate asLocalDate(LocalDate defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeLocalDate(this.value) : defaultValue;
	}

	@Override
	public Optional<LocalDateTime> asLocalDateTime() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeLocalDateTime(this.value) : null);
	}

	@Override
	public LocalDateTime asLocalDateTime(LocalDateTime defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeLocalDateTime(this.value) : defaultValue;
	}

	@Override
	public Optional<ZonedDateTime> asZonedDateTime() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeZonedDateTime(this.value) : null);
	}

	@Override
	public ZonedDateTime asZonedDateTime(ZonedDateTime defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeZonedDateTime(this.value) : defaultValue;
	}

	@Override
	public Optional<Currency> asCurrency() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeCurrency(this.value) : null);
	}

	@Override
	public Currency asCurrency(Currency defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeCurrency(this.value) : defaultValue;
	}

	@Override
	public Optional<Locale> asLocale() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeLocale(this.value) : null);
	}

	@Override
	public Locale asLocale(Locale defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeLocale(this.value) : defaultValue;
	}

	@Override
	public Optional<File> asFile() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeFile(this.value) : null);
	}

	@Override
	public File asFile(File defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeFile(this.value) : defaultValue;
	}

	@Override
	public Optional<Path> asPath() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodePath(this.value) : null);
	}

	@Override
	public Path asPath(Path defaultValue) {
		return this.value != null ? this.source.getDecoder().decodePath(this.value) : defaultValue;
	}

	@Override
	public Optional<URI> asURI() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeURI(this.value) : null);
	}

	@Override
	public URI asURI(URI defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeURI(this.value) : defaultValue;
	}

	@Override
	public Optional<URL> asURL() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeURL(this.value) : null);
	}

	@Override
	public URL asURL(URL defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeURL(this.value) : defaultValue;
	}

	@Override
	public Optional<Pattern> asPattern() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodePattern(this.value) : null);
	}

	@Override
	public Pattern asPattern(Pattern defaultValue) {
		return this.value != null ? this.source.getDecoder().decodePattern(this.value) : defaultValue;
	}

	@Override
	public Optional<InetAddress> asInetAddress() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeInetAddress(this.value) : null);
	}

	@Override
	public InetAddress asInetAddress(InetAddress defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeInetAddress(this.value) : defaultValue;
	}

	@Override
	public Optional<InetSocketAddress> asInetSocketAddress() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeInetSocketAddress(this.value) : null);
	}

	@Override
	public InetSocketAddress asInetSocketAddress(InetSocketAddress defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeInetSocketAddress(this.value) : defaultValue;
	}

	@Override
	public <T> Optional<Class<T>> asClass() {
		return Optional.ofNullable(this.value != null ? this.source.getDecoder().decodeClass(this.value) : null);
	}

	@Override
	public <T> Class<T> asClass(Class<T> defaultValue) {
		return this.value != null ? this.source.getDecoder().decodeClass(this.value) : defaultValue;
	}
	
	@Override
	public String toString() {
		return this.key.toString() + " = " + this.value;
	}
}

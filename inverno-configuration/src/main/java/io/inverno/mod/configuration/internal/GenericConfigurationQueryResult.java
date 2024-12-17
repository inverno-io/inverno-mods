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
import java.util.NoSuchElementException;
import java.util.Optional;

import io.inverno.mod.configuration.ConfigurationProperty;
import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.ConfigurationQueryResult;
import io.inverno.mod.configuration.ConfigurationSource;
import io.inverno.mod.configuration.ConfigurationSourceException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * <p>
 * Generic {@link ConfigurationQueryResult} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ConfigurationQueryResult
 */
public class GenericConfigurationQueryResult implements ConfigurationQueryResult {

	protected ConfigurationKey queryKey;
	protected ConfigurationProperty queryResult;
	protected Throwable error;
	protected ConfigurationSource errorSource;
	
	/**
	 * <p>
	 * Creates a generic successful configuration query result with the specified query key and result property.
	 * </p>
	 *
	 * @param queryKey    the query key
	 * @param queryResult the result property
	 */
	public GenericConfigurationQueryResult(ConfigurationKey queryKey, ConfigurationProperty queryResult) {
		this.queryKey = queryKey;
		this.queryResult = queryResult;
	}
	
	/**
	 * <p>
	 * Creates a generic faulty configuration query result with the specified query key, configuration source and error.
	 * </p>
	 *
	 * @param queryKey the query key
	 * @param source   the configuration source
	 * @param error    the error
	 */
	public GenericConfigurationQueryResult(ConfigurationKey queryKey, ConfigurationSource source, Throwable error) {
		this.queryKey = queryKey;
		this.errorSource = source;
		this.error = error;
	}
	
	@Override
	public ConfigurationKey getQueryKey() {
		return this.queryKey;
	}

	@Override
	public boolean isPresent() {
		return this.queryResult != null;
	}

	@Override
	public boolean isEmpty() {
		return this.queryResult == null;
	}

	@Override
	public void ifPresent(Consumer<? super ConfigurationProperty> action) {
		if(this.queryResult != null) {
			action.accept(this.queryResult);
		}
	}

	@Override
	public void ifPresentOrElse(Consumer<? super ConfigurationProperty> action, Runnable emptyAction) {
		if(this.queryResult != null) {
			action.accept(this.queryResult);
		}
		else {
			emptyAction.run();
		}
	}

	@Override
	public ConfigurationProperty orElseThrow() throws NoSuchElementException {
		if(this.queryResult == null) {
			throw new NoSuchElementException("Query " + this.queryKey + " returned no result");
		}
		return this.queryResult;
	}

	@Override
	public <X extends Throwable> ConfigurationProperty orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
		if(this.queryResult == null) {
			throw exceptionSupplier.get();
		}
		return this.queryResult;
	}

	@Override
	public ConfigurationProperty get() throws ConfigurationSourceException, NoSuchElementException {
		if(this.error != null) {
			throw new ConfigurationSourceException(this.errorSource, this.error);
		}
		if(this.queryResult == null) {
			throw new NoSuchElementException("Query " + this.queryKey + " returned no result");
		}
		return this.queryResult;
	}

	@Override
	public Optional<ConfigurationProperty> toOptional() throws ConfigurationSourceException {
		if(this.error != null) {
			throw new ConfigurationSourceException(this.errorSource, this.error);
		}
		return Optional.ofNullable(this.queryResult);
	}

	@Override
	public <T> T as(Class<T> type, T defaultValue) {
		return this.queryResult != null ? this.queryResult.as(type, defaultValue) : defaultValue;
	}

	@Override
	public <T> T as(Type type, T defaultValue) {
		return this.queryResult != null ? this.queryResult.as(type, defaultValue) : defaultValue;
	}

	@Override
	public <T> T[] asArrayOf(Class<T> type, T[] defaultValue) {
		return this.queryResult != null ? this.queryResult.asArrayOf(type, defaultValue) : defaultValue;
	}

	@Override
	public <T> T[] asArrayOf(Type type, T[] defaultValue) {
		return this.queryResult != null ? this.queryResult.asArrayOf(type, defaultValue) : defaultValue;
	}

	@Override
	public <T> List<T> asListOf(Class<T> type, List<T> defaultValue) {
		return this.queryResult != null ? this.queryResult.asListOf(type, defaultValue) : defaultValue;
	}

	@Override
	public <T> List<T> asListOf(Type type, List<T> defaultValue) {
		return this.queryResult != null ? this.queryResult.asListOf(type, defaultValue) : defaultValue;
	}

	@Override
	public <T> Set<T> asSetOf(Class<T> type, Set<T> defaultValue) {
		return this.queryResult != null ? this.queryResult.asSetOf(type, defaultValue) : defaultValue;
	}

	@Override
	public <T> Set<T> asSetOf(Type type, Set<T> defaultValue) {
		return this.queryResult != null ? this.queryResult.asSetOf(type, defaultValue) : defaultValue;
	}

	@Override
	public byte asByte(byte defaultValue) {
		return this.queryResult != null ? this.queryResult.asByte(defaultValue) : defaultValue;
	}

	@Override
	public short asShort(short defaultValue) {
		return this.queryResult != null ? this.queryResult.asShort(defaultValue) : defaultValue;
	}

	@Override
	public int asInteger(int defaultValue) {
		return this.queryResult != null ? this.queryResult.asInteger(defaultValue) : defaultValue;
	}

	@Override
	public long asLong(long defaultValue) {
		return this.queryResult != null ? this.queryResult.asLong(defaultValue) : defaultValue;
	}

	@Override
	public float asFloat(float defaultValue) {
		return this.queryResult != null ? this.queryResult.asFloat(defaultValue) : defaultValue;
	}

	@Override
	public double asDouble(double defaultValue) {
		return this.queryResult != null ? this.queryResult.asDouble(defaultValue) : defaultValue;
	}

	@Override
	public char asCharacter(char defaultValue) {
		return this.queryResult != null ? this.queryResult.asCharacter(defaultValue) : defaultValue;
	}

	@Override
	public String asString(String defaultValue) {
		return this.queryResult != null ? this.queryResult.asString(defaultValue) : defaultValue;
	}

	@Override
	public boolean asBoolean(boolean defaultValue) {
		return this.queryResult != null ? this.queryResult.asBoolean(defaultValue) : defaultValue;
	}

	@Override
	public BigInteger asBigInteger(BigInteger defaultValue) {
		return this.queryResult != null ? this.queryResult.asBigInteger(defaultValue) : defaultValue;
	}

	@Override
	public BigDecimal asBigDecimal(BigDecimal defaultValue) {
		return this.queryResult != null ? this.queryResult.asBigDecimal(defaultValue) : defaultValue;
	}

	@Override
	public LocalDate asLocalDate(LocalDate defaultValue) {
		return this.queryResult != null ? this.queryResult.asLocalDate(defaultValue) : defaultValue;
	}

	@Override
	public LocalDateTime asLocalDateTime(LocalDateTime defaultValue) {
		return this.queryResult != null ? this.queryResult.asLocalDateTime(defaultValue) : defaultValue;
	}

	@Override
	public ZonedDateTime asZonedDateTime(ZonedDateTime defaultValue) {
		return this.queryResult != null ? this.queryResult.asZonedDateTime(defaultValue) : defaultValue;
	}

	@Override
	public Currency asCurrency(Currency defaultValue) {
		return this.queryResult != null ? this.queryResult.asCurrency(defaultValue) : defaultValue;
	}

	@Override
	public Locale asLocale(Locale defaultValue) {
		return this.queryResult != null ? this.queryResult.asLocale(defaultValue) : defaultValue;
	}

	@Override
	public File asFile(File defaultValue) {
		return this.queryResult != null ? this.queryResult.asFile(defaultValue) : defaultValue;
	}

	@Override
	public Path asPath(Path defaultValue) {
		return this.queryResult != null ? this.queryResult.asPath(defaultValue) : defaultValue;
	}

	@Override
	public URI asURI(URI defaultValue) {
		return this.queryResult != null ? this.queryResult.asURI(defaultValue) : defaultValue;
	}

	@Override
	public URL asURL(URL defaultValue) {
		return this.queryResult != null ? this.queryResult.asURL(defaultValue) : defaultValue;
	}

	@Override
	public Pattern asPattern(Pattern defaultValue) {
		return this.queryResult != null ? this.queryResult.asPattern(defaultValue) : defaultValue;
	}

	@Override
	public InetAddress asInetAddress(InetAddress defaultValue) {
		return this.queryResult != null ? this.queryResult.asInetAddress(defaultValue) : defaultValue;
	}

	@Override
	public InetSocketAddress asInetSocketAddress(InetSocketAddress defaultValue) {
		return this.queryResult != null ? this.queryResult.asInetSocketAddress(defaultValue) : defaultValue;
	}

	@Override
	public <T> Class<T> asClass(Class<T> defaultValue) {
		return this.queryResult != null ? this.queryResult.asClass(defaultValue) : defaultValue;
	}
}

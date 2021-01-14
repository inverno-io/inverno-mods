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
package io.winterframework.mod.configuration;

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
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author jkuhn
 *
 */
public interface ConfigurationProperty<A extends ConfigurationKey, B extends ConfigurationSource<?, ?, ?>> {
	
	A getKey();
	
	B getSource();
	
	boolean isUnset();
	
	boolean isPresent();
	
	boolean isEmpty();
	
	<T> Optional<T> as(Class<T> type);
	
	<T> Optional<T[]> asArrayOf(Class<T> type);
	
	<T> Optional<List<T>> asListOf(Class<T> type);
	
	<T> Optional<Set<T>> asSetOf(Class<T> type);
	
	Optional<Byte> asByte();
	
	Optional<Short> asShort();
	
	Optional<Integer> asInteger();
	
	Optional<Long> asLong();
	
	Optional<Float> asFloat();
	
	Optional<Double> asDouble();
	
	Optional<Character> asCharacter();
	
	Optional<String> asString();
	
	Optional<Boolean> asBoolean();
	
	Optional<BigInteger> asBigInteger();
	
	Optional<BigDecimal> asBigDecimal();
	
	Optional<LocalDate> asLocalDate();
	
	Optional<LocalDateTime> asLocalDateTime();
	
	Optional<ZonedDateTime> asZonedDateTime();
	
	Optional<Currency> asCurrency();
	
	Optional<Locale> asLocale();
	
	Optional<File> asFile();
	
	Optional<Path> asPath();
	
	Optional<URI> asURI();
	
	Optional<URL> asURL();
	
	Optional<Pattern> asPattern();
	
	Optional<InetAddress> asInetAddress();
	
	Optional<Class<?>> asClass();
}
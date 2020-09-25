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
import java.util.Collection;
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
public interface ConfigurationEntry<A extends ConfigurationKey, B extends ConfigurationSource<?, ?, ?>> {
	
	A getKey();
	
	B getSource();
	
	boolean isUnset();
	
	boolean isPresent();
	
	boolean isEmpty();
	
	<T> Optional<T> valueAs(Class<T> type);
	
	<T> Optional<T[]> valueAsArrayOf(Class<T> type);
	
	<T> Optional<Collection<T>> valueAsCollectionOf(Class<T> type);
	
	<T> Optional<List<T>> valueAsListOf(Class<T> type);
	
	<T> Optional<Set<T>> valueAsSetOf(Class<T> type);
	
	Optional<Byte> valueAsByte();
	
	Optional<Short> valueAsShort();
	
	Optional<Integer> valueAsInteger();
	
	Optional<Long> valueAsLong();
	
	Optional<Float> valueAsFloat();
	
	Optional<Double> valueAsDouble();
	
	Optional<Character> valueAsCharacter();
	
	Optional<String> valueAsString();
	
	Optional<Boolean> valueAsBoolean();
	
	Optional<BigInteger> valueAsBigInteger();
	
	Optional<BigDecimal> valueAsBigDecimal();
	
	Optional<LocalDate> valueAsLocalDate();
	
	Optional<LocalDateTime> valueAsLocalDateTime();
	
	Optional<ZonedDateTime> valueAsZonedDateTime();
	
	Optional<Currency> valueAsCurrency();
	
	Optional<Locale> valueAsLocale();
	
	Optional<File> valueAsFile();
	
	Optional<Path> valueAsPath();
	
	Optional<URI> valueAsURI();
	
	Optional<URL> valueAsURL();
	
	Optional<Pattern> valueAsPattern();
	
	Optional<InetAddress> valueAsInetAddress();
	
	Optional<Class<?>> valueAsClass();
}
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
package io.inverno.mod.configuration;

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
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import io.inverno.mod.configuration.source.CompositeConfigurationSource;

/**
 * <p>
 * A configuration property.
 * </p>
 *
 * <p>
 * A configuration property is identified by a configuration key which is composed of a name and a collection of parameters defining the context for which a property value is defined. This means that
 * for a given property name, multiple values can be defined in a configuration source for different contexts.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 * @see ConfigurationKey
 * @see ConfigurationQuery
 * @see ConfigurationQueryResult
 */
public interface ConfigurationProperty {
	
	/**
	 * <p>
	 * Returns the key identifying the property and the context in which it has been defined.
	 * </p>
	 *
	 * @return a configuration key
	 */
	ConfigurationKey getKey();
	
	/**
	 * <p>
	 * Returns the configuration source that loaded the property.
	 * </p>
	 *
	 * @return a configuration source.
	 */
	ConfigurationSource<?,?,?> getSource();
	
	/**
	 * <p>
	 * Determines whether this property is unset.
	 * </p>
	 *
	 * <p>
	 * Unset properties are always empty, they are used in a {@link CompositeConfigurationSource} to cancel non-empty properties retrieved from sources with lower priority.
	 * </p>
	 *
	 * @return true if the property is unset, false otherwise
	 */
	boolean isUnset();
	
	/**
	 * <p>
	 * Determines whether the value is present (ie. value is not null).
	 * </p>
	 *
	 * @return true if the property value is not null, false otherwise
	 */
	boolean isPresent();
	
	/**
	 * <p>
	 * Determines whether the value is empty (ie. value is null).
	 * </p>
	 * 
	 * @return true if the property value is null, false otherwise
	 */
	boolean isEmpty();
	
	/**
	 * <p>
	 * Converts the property value to the specified type.
	 * </p>
	 *
	 * @param <T>  the target type
	 * @param type a class of type T
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	<T> Optional<T> as(Class<T> type);
	
	/**
	 * <p>
	 * Converts the property value to the specified type.
	 * </p>
	 *
	 * @param <T>  the target type
	 * @param type the target type
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	<T> Optional<T> as(Type type);
	
	/**
	 * <p>
	 * Converts the property value to an array of the specified type.
	 * </p>
	 *
	 * @param <T>  the target component type
	 * @param type a class of type T
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	<T> Optional<T[]> asArrayOf(Class<T> type);
	
	/**
	 * <p>
	 * Converts the property value to an array of the specified type.
	 * </p>
	 *
	 * @param <T>  the target component type
	 * @param type the target component type
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	<T> Optional<T[]> asArrayOf(Type type);
	
	/**
	 * <p>
	 * Converts the property value to a list of the specified type.
	 * </p>
	 *
	 * @param <T>  the target list argument type
	 * @param type a class of type T
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	<T> Optional<List<T>> asListOf(Class<T> type);
	
	/**
	 * <p>
	 * Converts the property value to a list of the specified type.
	 * </p>
	 *
	 * @param <T>  the target list argument type
	 * @param type the target list argument type
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	<T> Optional<List<T>> asListOf(Type type);
	
	/**
	 * <p>
	 * Converts the property value to a set of the specified type.
	 * </p>
	 *
	 * @param <T>  the target set argument type
	 * @param type a class of type T
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	<T> Optional<Set<T>> asSetOf(Class<T> type);
	
	/**
	 * <p>
	 * Converts the property value to a set of the specified type.
	 * </p>
	 *
	 * @param <T>  the target set argument type
	 * @param type the target set argument type
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	<T> Optional<Set<T>> asSetOf(Type type);
	
	/**
	 * <p>
	 * Converts the property value to a byte.
	 * </p>
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	Optional<Byte> asByte();

	/**
	 * <p>
	 * Converts the property value to a short.
	 * </p>
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	Optional<Short> asShort();

	/**
	 * <p>
	 * Converts the property value to an integer.
	 * </p>
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	Optional<Integer> asInteger();

	/**
	 * <p>
	 * Converts the property value to a long.
	 * </p>
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	Optional<Long> asLong();

	/**
	 * <p>
	 * Converts the property value to a float.
	 * </p>
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	Optional<Float> asFloat();

	/**
	 * <p>
	 * Converts the property value to a double.
	 * </p>
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	Optional<Double> asDouble();

	/**
	 * <p>
	 * Converts the property value to a character.
	 * </p>
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	Optional<Character> asCharacter();

	/**
	 * <p>
	 * Converts the property value to a string.
	 * </p>
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	Optional<String> asString();

	/**
	 * <p>
	 * Converts the property value to a boolean.
	 * </p>
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	Optional<Boolean> asBoolean();

	/**
	 * <p>
	 * Converts the property value to a big integer.
	 * </p>
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	Optional<BigInteger> asBigInteger();

	/**
	 * <p>
	 * Converts the property value to a big decimal.
	 * </p>
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	Optional<BigDecimal> asBigDecimal();

	/**
	 * <p>
	 * Converts the property value to a local date.
	 * </p>
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	Optional<LocalDate> asLocalDate();

	/**
	 * <p>
	 * Converts the property value to a local date time.
	 * </p>
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	Optional<LocalDateTime> asLocalDateTime();

	/**
	 * <p>
	 * Converts the property value to a zoned date time.
	 * </p>
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	Optional<ZonedDateTime> asZonedDateTime();

	/**
	 * <p>
	 * Converts the property value to a currency.
	 * </p>
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	Optional<Currency> asCurrency();

	/**
	 * <p>
	 * Converts the property value to a locale.
	 * </p>
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	Optional<Locale> asLocale();

	/**
	 * <p>
	 * Converts the property value to a file.
	 * </p>
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	Optional<File> asFile();

	/**
	 * <p>
	 * Converts the property value to a path.
	 * </p>
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	Optional<Path> asPath();

	/**
	 * <p>
	 * Converts the property value to a URI.
	 * </p>
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	Optional<URI> asURI();

	/**
	 * <p>
	 * Converts the property value to a URL.
	 * </p>
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	Optional<URL> asURL();

	/**
	 * <p>
	 * Converts the property value to a pattern.
	 * </p>
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	Optional<Pattern> asPattern();

	/**
	 * <p>
	 * Converts the property value to an inet address.
	 * </p>
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	Optional<InetAddress> asInetAddress();

	/**
	 * <p>
	 * Converts the property value to a class.
	 * </p>
	 *
	 * @return an optional returning the converted value or an empty optional if the property is empty
	 */
	Optional<Class<?>> asClass();
}
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
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * <p>
 * Represents a single query result providing the configuration property retrieved from a configuration source with a query key.
 * </p>
 *
 * <p>
 * Note that the query key and the property key may differs if the configuration source uses a defaulting mechanism to return the value that best matches the context specified in the query key.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see ConfigurationQuery
 * @see ConfigurationSource
 */
public interface ConfigurationQueryResult {

	/**
	 * <p>
	 * Returns the configuration key corresponding to the query that was executed.
	 * </p>
	 *
	 * @return a configuration key
	 */
	ConfigurationKey getQueryKey();

	/**
	 * <p>
	 * Determines whether the result is present.
	 * </p>
	 *
	 * <p>
	 * An actual result is returned when a property has been defined in the target source for the queried key. Note that this doesn't mean the property has a non-null value since null values
	 * are supported.
	 * </p>
	 *
	 * @return true if there is a result, false otherwise
	 */
	boolean isPresent();

	/**
	 * <p>
	 * Determines whether the result is empty.
	 * </p>
	 *
	 * <p>
	 * No result is returned when no property has been defined in the target source for the queried key.
	 * </p>
	 *
	 * @return true if there is no result, false otherwise
	 */
	boolean isEmpty();

	/**
	 * <p>
	 * Performs the given action with the configuration property if a result is present, otherwise does nothing.
	 * </p>
	 *
	 * @param action the action to be performed if a result is present
	 */
	void ifPresent(Consumer<? super ConfigurationProperty> action);

	/**
	 * <p>
	 * Performs the given action with the configuration property if a result is present, otherwise performs the given empty-based action.
	 * </p>
	 *
	 * @param action      the action to be performed if a result is present
	 * @param emptyAction the empty-based action to be performed if no result is present
	 */
	void ifPresentOrElse(Consumer<? super ConfigurationProperty> action, Runnable emptyAction);

	/**
	 * <p>
	 * Returns the configuration property if a result is present, otherwise throws {@code NoSuchElementException}.
	 * </p>
	 *
	 * @return the configuration property
	 *
	 * @throws NoSuchElementException if no result is present
	 */
	ConfigurationProperty orElseThrow() throws NoSuchElementException;

	/**
	 * <p>
	 * Returns the configuration property if a result is present, otherwise throws an exception produced by the exception supplying function.
	 * </p>
	 *
	 * @param <X>               the type of the exception to be thrown
	 * @param exceptionSupplier the exception supplier
	 *
	 * @return the configuration property
	 *
	 * @throws X if no result is present
	 */
	<X extends Throwable> ConfigurationProperty orElseThrow(Supplier<? extends X> exceptionSupplier) throws X;

	/**
	 * <p>
	 * Returns the resulting configuration property.
	 * </p>
	 *
	 * @return the configuration property
	 *
	 * @throws ConfigurationSourceException if there was an error retrieving the configuration property
	 * @throws NoSuchElementException       if the result is empty
	 */
	ConfigurationProperty get() throws ConfigurationSourceException, NoSuchElementException;

	/**
	 * <p>
	 * Returns the configuration property value converted to the specified type or a default value.
	 * </p>
	 *
	 * @param <T>          the target type
	 * @param type         a class of type T
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	<T> T as(Class<T> type, T defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value converted to the specified type or a default value.
	 * </p>
	 *
	 * @param <T>          the target type
	 * @param type         a class of type T
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	<T> T as(Type type, T defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value converted to an array of the specified type or a default array.
	 * </p>
	 *
	 * @param <T>          the target component type
	 * @param type         a class of type T
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default array
	 */
	<T> T[] asArrayOf(Class<T> type, T[] defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value converted to an array of the specified type or a default array.
	 * </p>
	 *
	 * @param <T>          the target component type
	 * @param type         a class of type T
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default array
	 */
	<T> T[] asArrayOf(Type type, T[] defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value converted to a list of the specified type or a default list.
	 * </p>
	 *
	 * @param <T>          the target component type
	 * @param type         a class of type T
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default list
	 */
	<T> List<T> asListOf(Class<T> type, List<T> defaultValue);

	/**
	 * <p>
	 * Returns the property value converted to a list of the specified type or a default list.
	 * </p>
	 *
	 * @param <T>          the target component type
	 * @param type         a class of type T
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default list
	 */
	<T> List<T> asListOf(Type type, List<T> defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value converted to a set of the specified type or a default set.
	 * </p>
	 *
	 * @param <T>          the target component type
	 * @param type         a class of type T
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default set
	 */
	<T> Set<T> asSetOf(Class<T> type, Set<T> defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value converted to a set of the specified type or a default set.
	 * </p>
	 *
	 * @param <T>          the target component type
	 * @param type         a class of type T
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default set
	 */
	<T> Set<T> asSetOf(Type type, Set<T> defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value as a byte or a default value.
	 * </p>
	 *
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	byte asByte(byte defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value as a short or a default value.
	 * </p>
	 *
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	short asShort(short defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value as an integer or a default value.
	 * </p>
	 *
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	int asInteger(int defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value as a long or a default value.
	 * </p>
	 *
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	long asLong(long defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value as a float or a default value.
	 * </p>
	 *
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	float asFloat(float defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value as a double or a default value.
	 * </p>
	 *
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	double asDouble(double defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value as a character or a default value.
	 * </p>
	 *
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	char asCharacter(char defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value as a string or a default value.
	 * </p>
	 *
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	String asString(String defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value as a boolean or a default value.
	 * </p>
	 *
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	boolean asBoolean(boolean defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value as a big integer or a default value.
	 * </p>
	 *
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	BigInteger asBigInteger(BigInteger defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value as a big decimal or a default value.
	 * </p>
	 *
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	BigDecimal asBigDecimal(BigDecimal defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value as a local date or a default value.
	 * </p>
	 *
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	LocalDate asLocalDate(LocalDate defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value as a local date time or a default value.
	 * </p>
	 *
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	LocalDateTime asLocalDateTime(LocalDateTime defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value as a zoned date time or a default value.
	 * </p>
	 *
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	ZonedDateTime asZonedDateTime(ZonedDateTime defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value as a currency or a default value.
	 * </p>
	 *
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	Currency asCurrency(Currency defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value as a locale or a default value.
	 * </p>
	 *
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	Locale asLocale(Locale defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value as a file or a default value.
	 * </p>
	 *
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	File asFile(File defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value as a path or a default value.
	 * </p>
	 *
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	Path asPath(Path defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value as a URI or a default value.
	 * </p>
	 *
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	URI asURI(URI defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value as a URL or a default value.
	 * </p>
	 *
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	URL asURL(URL defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value as a pattern or a default value.
	 * </p>
	 *
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	Pattern asPattern(Pattern defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value as an inet address or a default value.
	 * </p>
	 *
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	InetAddress asInetAddress(InetAddress defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value as an inet socket address or a default value.
	 * </p>
	 *
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	InetSocketAddress asInetSocketAddress(InetSocketAddress defaultValue);

	/**
	 * <p>
	 * Returns the configuration property value as a class or a default value.
	 * </p>
	 *
	 * @param <T>          the target type
	 * @param defaultValue a default value
	 *
	 * @return the configuration property value or the specified default value
	 */
	<T> Class<T> asClass(Class<T> defaultValue);

	/**
	 * <p>
	 * Returns the query result as configuration property {@code Optional}.
	 * </p>
	 *
	 * <p>
	 * This method allows to map the query result as an {@link Optional}.
	 * </p>
	 *
	 * @return an optional returning the configuration property or an empty optional if the configuration returned no value for the property
	 *
	 * @throws ConfigurationSourceException if there was an error retrieving the configuration property
	 */
	Optional<ConfigurationProperty> toOptional() throws ConfigurationSourceException;
}

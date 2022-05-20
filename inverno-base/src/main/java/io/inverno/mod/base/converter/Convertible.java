/*
 * Copyright 2022 Jeremy Kuhn
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
package io.inverno.mod.base.converter;

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
import java.util.Set;
import java.util.regex.Pattern;

/**
 * <p>
 * Represents a value of a given type that can be converted. 
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface Convertible<A> {

	/**
	 * <p>
	 * Returns the name.
	 * </p>
	 *
	 * @return a name
	 */
	String getName();

	/**
	 * <p>
	 * Returns the raw value.
	 * </p>
	 *
	 * @return a raw value
	 */
	A getValue();

	/**
	 * <p>
	 * Converts the value to the specified type.
	 * </p>
	 *
	 * @param <T>  the target type
	 * @param type a class of type T
	 *
	 * @return the converted value or null
	 */
	<T> T as(Class<T> type);

	/**
	 * <p>
	 * Converts the value to the specified type.
	 * </p>
	 *
	 * @param <T>  the target type
	 * @param type the target type
	 *
	 * @return the converted value or null
	 */
	<T> T as(Type type);

	/**
	 * <p>
	 * Converts the value to an array of the specified type.
	 * </p>
	 *
	 * @param <T>  the target component type
	 * @param type a class of type T
	 *
	 * @return the value converted to an array of T or null
	 */
	<T> T[] asArrayOf(Class<T> type);

	/**
	 * <p>
	 * Converts the value to an array of the specified type.
	 * </p>
	 *
	 * @param <T>  the target component type
	 * @param type the target component type
	 *
	 * @return the value converted to an array of T or null
	 */
	<T> T[] asArrayOf(Type type);

	/**
	 * <p>
	 * Converts the value to a list of the specified type.
	 * </p>
	 *
	 * @param <T>  the target list argument type
	 * @param type a class of type T
	 *
	 * @return the value converted to a list of T or null
	 */
	<T> List<T> asListOf(Class<T> type);

	/**
	 * <p>
	 * Converts the value to a list of the specified type.
	 * </p>
	 *
	 * @param <T>  the target list argument type
	 * @param type the target list argument type
	 *
	 * @return the value converted to a list of T or null
	 */
	<T> List<T> asListOf(Type type);

	/**
	 * <p>
	 * Converts the value to a set of the specified type.
	 * </p>
	 *
	 * @param <T>  the target set argument type
	 * @param type a class of type T
	 *
	 * @return the value converted to a set of T or null
	 */
	<T> Set<T> asSetOf(Class<T> type);

	/**
	 * <p>
	 * Converts the value to a set of the specified type.
	 * </p>
	 *
	 * @param <T>  the target set argument type
	 * @param type the target set argument type
	 *
	 * @return the value converted to a set of T or null
	 */
	<T> Set<T> asSetOf(Type type);

	/**
	 * <p>
	 * Converts the value to a byte.
	 * </p>
	 *
	 * @return the converted value or null
	 */
	Byte asByte();

	/**
	 * <p>
	 * Converts the value to a short.
	 * </p>
	 *
	 * @return the converted value or null
	 */
	Short asShort();

	/**
	 * <p>
	 * Converts the value to an integer.
	 * </p>
	 *
	 * @return the converted value or null
	 */
	Integer asInteger();

	/**
	 * <p>
	 * Converts the value to a long.
	 * </p>
	 *
	 * @return the converted value or null
	 */
	Long asLong();

	/**
	 * <p>
	 * Converts the value to a float.
	 * </p>
	 *
	 * @return the converted value or null
	 */
	Float asFloat();

	/**
	 * <p>
	 * Converts the value to a double.
	 * </p>
	 *
	 * @return the converted value or null
	 */
	Double asDouble();

	/**
	 * <p>
	 * Converts the value to a character.
	 * </p>
	 *
	 * @return the converted value or null
	 */
	Character asCharacter();

	/**
	 * <p>
	 * Converts the value to a string.
	 * </p>
	 *
	 * @return the converted value or null
	 */
	String asString();

	/**
	 * <p>
	 * Converts the value to a boolean.
	 * </p>
	 *
	 * @return the converted value or null
	 */
	Boolean asBoolean();

	/**
	 * <p>
	 * Converts the value to a big integer.
	 * </p>
	 *
	 * @return the converted value or null
	 */
	BigInteger asBigInteger();

	/**
	 * <p>
	 * Converts the value to a big decimal.
	 * </p>
	 *
	 * @return the converted value or null
	 */
	BigDecimal asBigDecimal();

	/**
	 * <p>
	 * Converts the value to a local date.
	 * </p>
	 *
	 * @return the converted value or null
	 */
	LocalDate asLocalDate();

	/**
	 * <p>
	 * Converts the value to a loca date time.
	 * </p>
	 *
	 * @return the converted value or null
	 */
	LocalDateTime asLocalDateTime();

	/**
	 * <p>
	 * Converts the value to a zoned date time.
	 * </p>
	 *
	 * @return the converted value or null
	 */
	ZonedDateTime asZonedDateTime();

	/**
	 * <p>
	 * Converts the value to a currency.
	 * </p>
	 *
	 * @return the converted value or null
	 */
	Currency asCurrency();

	/**
	 * <p>
	 * Converts the value to a locale.
	 * </p>
	 *
	 * @return the converted value or null
	 */
	Locale asLocale();

	/**
	 * <p>
	 * Converts the value to a file.
	 * </p>
	 *
	 * @return the converted value or null
	 */
	File asFile();

	/**
	 * <p>
	 * Converts the value to a path.
	 * </p>
	 *
	 * @return the converted value or null
	 */
	Path asPath();

	/**
	 * <p>
	 * Converts the value to a URI.
	 * </p>
	 *
	 * @return the converted value or null
	 */
	URI asURI();

	/**
	 * <p>
	 * Converts the value to a URL.
	 * </p>
	 *
	 * @return the converted value or null
	 */
	URL asURL();

	/**
	 * <p>
	 * Converts the value to a pattern.
	 * </p>
	 *
	 * @return the converted value or null
	 */
	Pattern asPattern();

	/**
	 * <p>
	 * Converts the value to an inet address.
	 * </p>
	 *
	 * @return the converted value or null
	 */
	InetAddress asInetAddress();

	/**
	 * <p>
	 * Converts the value to a class.
	 * </p>
	 *
	 * @return the converted value or null
	 */
	Class<?> asClass();
}

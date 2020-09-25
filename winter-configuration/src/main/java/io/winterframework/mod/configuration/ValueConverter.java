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
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author jkuhn
 *
 */
public interface ValueConverter<E> {

	<T> T to(E value, Class<T> type) throws ConversionException;
	
	<T> Collection<T> toCollectionOf(E value, Class<T> type) throws ConversionException;
	
	<T> List<T> toListOf(E value, Class<T> type) throws ConversionException;
	
	<T> Set<T> toSetOf(E value, Class<T> type) throws ConversionException;
	
	<T> T[] toArrayOf(E value, Class<T> type) throws ConversionException;
	
	Byte toByte(E value) throws ConversionException;
	
	Short toShort(E value) throws ConversionException;
	
	Integer toInteger(E value) throws ConversionException;
	
	Long toLong(E value) throws ConversionException;

	Float toFloat(E value) throws ConversionException;
	
	Double toDouble(E value) throws ConversionException;
	
	Character toCharacter(E value) throws ConversionException;
	
	Boolean toBoolean(E value) throws ConversionException;
	
	BigInteger toBigInteger(E value) throws ConversionException;
	
	BigDecimal toBigDecimal(E value) throws ConversionException;
	
	LocalDate toLocalDate(E value) throws ConversionException;
	
	LocalDateTime toLocalDateTime(E value) throws ConversionException;
	
	ZonedDateTime toZonedDateTime(E value) throws ConversionException;
	
	Currency toCurrency(E value) throws ConversionException;
	
	Locale toLocale(E value) throws ConversionException;
	
	File toFile(E value) throws ConversionException;
	
	Path toPath(E value) throws ConversionException;
	
	URI toURI(E value) throws ConversionException;
	
	URL toURL(E value) throws ConversionException;
	
	Pattern toPattern(E value) throws ConversionException;
	
	InetAddress toInetAddress(E value) throws ConversionException;
	
	Class<?> toClass(E value) throws ConversionException;
}

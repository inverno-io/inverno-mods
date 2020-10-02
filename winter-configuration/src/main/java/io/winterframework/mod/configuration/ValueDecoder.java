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
public interface ValueDecoder<E> {

	<T> T to(E value, Class<T> type) throws ValueCodecException;
	
	<T> Collection<T> toCollectionOf(E value, Class<T> type) throws ValueCodecException;
	
	<T> List<T> toListOf(E value, Class<T> type) throws ValueCodecException;
	
	<T> Set<T> toSetOf(E value, Class<T> type) throws ValueCodecException;
	
	<T> T[] toArrayOf(E value, Class<T> type) throws ValueCodecException;
	
	Byte toByte(E value) throws ValueCodecException;
	
	Short toShort(E value) throws ValueCodecException;
	
	Integer toInteger(E value) throws ValueCodecException;
	
	Long toLong(E value) throws ValueCodecException;

	Float toFloat(E value) throws ValueCodecException;
	
	Double toDouble(E value) throws ValueCodecException;
	
	Character toCharacter(E value) throws ValueCodecException;
	
	Boolean toBoolean(E value) throws ValueCodecException;
	
	String toString(E value) throws ValueCodecException;
	
	BigInteger toBigInteger(E value) throws ValueCodecException;
	
	BigDecimal toBigDecimal(E value) throws ValueCodecException;
	
	LocalDate toLocalDate(E value) throws ValueCodecException;
	
	LocalDateTime toLocalDateTime(E value) throws ValueCodecException;
	
	ZonedDateTime toZonedDateTime(E value) throws ValueCodecException;
	
	Currency toCurrency(E value) throws ValueCodecException;
	
	Locale toLocale(E value) throws ValueCodecException;
	
	File toFile(E value) throws ValueCodecException;
	
	Path toPath(E value) throws ValueCodecException;
	
	URI toURI(E value) throws ValueCodecException;
	
	URL toURL(E value) throws ValueCodecException;
	
	Pattern toPattern(E value) throws ValueCodecException;
	
	InetAddress toInetAddress(E value) throws ValueCodecException;
	
	Class<?> toClass(E value) throws ValueCodecException;
}

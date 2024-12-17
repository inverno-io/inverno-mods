/*
 * Copyright 2021 Jeremy KUHN
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
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * <p>
 * An object decoder providing primitive bindings to decode to primitive and common types.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Decoder
 * 
 * @param <From> the encoded type
 */
public interface PrimitiveDecoder<From> extends Decoder<From, Object> {

	/**
	 * <p>
	 * Decodes to byte.
	 * </p>
	 * 
	 * @param value the encoded value to decode
	 * 
	 * @return a decoded value
	 *
	 * @throws ConverterException if there was an error decoding the value
	 */
	Byte decodeByte(From value) throws ConverterException;
	
	/**
	 * <p>
	 * Decodes to short.
	 * </p>
	 * 
	 * @param value the encoded value to decode
	 * 
	 * @return a decoded value
	 *
	 * @throws ConverterException if there was an error decoding the value
	 */
	Short decodeShort(From value) throws ConverterException;

	/**
	 * <p>
	 * Decodes to integer.
	 * </p>
	 * 
	 * @param value the encoded value to decode
	 * 
	 * @return a decoded value
	 *
	 * @throws ConverterException if there was an error decoding the value
	 */
	Integer decodeInteger(From value) throws ConverterException;

	/**
	 * <p>
	 * Decodes to long.
	 * </p>
	 * 
	 * @param value the encoded value to decode
	 * 
	 * @return a decoded value
	 *
	 * @throws ConverterException if there was an error decoding the value
	 */
	Long decodeLong(From value) throws ConverterException;

	/**
	 * <p>
	 * Decodes to float.
	 * </p>
	 * 
	 * @param value the encoded value to decode
	 * 
	 * @return a decoded value
	 *
	 * @throws ConverterException if there was an error decoding the value
	 */
	Float decodeFloat(From value) throws ConverterException;

	/**
	 * <p>
	 * Decodes to double.
	 * </p>
	 * 
	 * @param value the encoded value to decode
	 * 
	 * @return a decoded value
	 *
	 * @throws ConverterException if there was an error decoding the value
	 */
	Double decodeDouble(From value) throws ConverterException;

	/**
	 * <p>
	 * Decodes to character.
	 * </p>
	 * 
	 * @param value the encoded value to decode
	 * 
	 * @return a decoded value
	 *
	 * @throws ConverterException if there was an error decoding the value
	 */
	Character decodeCharacter(From value) throws ConverterException;

	/**
	 * <p>
	 * Decodes to boolean.
	 * </p>
	 * 
	 * @param value the encoded value to decode
	 * 
	 * @return a decoded value
	 *
	 * @throws ConverterException if there was an error decoding the value
	 */
	Boolean decodeBoolean(From value) throws ConverterException;

	/**
	 * <p>
	 * Decodes to string.
	 * </p>
	 * 
	 * @param value the encoded value to decode
	 * 
	 * @return a decoded value
	 *
	 * @throws ConverterException if there was an error decoding the value
	 */
	String decodeString(From value) throws ConverterException;

	/**
	 * <p>
	 * Decodes to big integer.
	 * </p>
	 * 
	 * @param value the encoded value to decode
	 * 
	 * @return a decoded value
	 *
	 * @throws ConverterException if there was an error decoding the value
	 */
	BigInteger decodeBigInteger(From value) throws ConverterException;

	/**
	 * <p>
	 * Decodes to big decimal.
	 * </p>
	 * 
	 * @param value the encoded value to decode
	 * 
	 * @return a decoded value
	 *
	 * @throws ConverterException if there was an error decoding the value
	 */
	BigDecimal decodeBigDecimal(From value) throws ConverterException;

	/**
	 * <p>
	 * Decodes to local date.
	 * </p>
	 * 
	 * @param value the encoded value to decode
	 * 
	 * @return a decoded value
	 *
	 * @throws ConverterException if there was an error decoding the value
	 */
	LocalDate decodeLocalDate(From value) throws ConverterException;

	/**
	 * <p>
	 * Decodes to local datetime.
	 * </p>
	 * 
	 * @param value the encoded value to decode
	 * 
	 * @return a decoded value
	 *
	 * @throws ConverterException if there was an error decoding the value
	 */
	LocalDateTime decodeLocalDateTime(From value) throws ConverterException;

	/**
	 * <p>
	 * Decodes to zoned datetime.
	 * </p>
	 * 
	 * @param value the encoded value to decode
	 * 
	 * @return a decoded value
	 *
	 * @throws ConverterException if there was an error decoding the value
	 */
	ZonedDateTime decodeZonedDateTime(From value) throws ConverterException;

	/**
	 * <p>
	 * Decodes to currency.
	 * </p>
	 * 
	 * @param value the encoded value to decode
	 * 
	 * @return a decoded value
	 *
	 * @throws ConverterException if there was an error decoding the value
	 */
	Currency decodeCurrency(From value) throws ConverterException;

	/**
	 * <p>
	 * Decodes to locale.
	 * </p>
	 * 
	 * @param value the encoded value to decode
	 * 
	 * @return a decoded value
	 *
	 * @throws ConverterException if there was an error decoding the value
	 */
	Locale decodeLocale(From value) throws ConverterException;

	/**
	 * <p>
	 * Decodes to file.
	 * </p>
	 * 
	 * @param value the encoded value to decode
	 * 
	 * @return a decoded value
	 *
	 * @throws ConverterException if there was an error decoding the value
	 */
	File decodeFile(From value) throws ConverterException;

	/**
	 * <p>
	 * Decodes to path.
	 * </p>
	 * 
	 * @param value the encoded value to decode
	 * 
	 * @return a decoded value
	 *
	 * @throws ConverterException if there was an error decoding the value
	 */
	Path decodePath(From value) throws ConverterException;

	/**
	 * <p>
	 * Decodes to URI.
	 * </p>
	 * 
	 * @param value the encoded value to decode
	 * 
	 * @return a decoded value
	 *
	 * @throws ConverterException if there was an error decoding the value
	 */
	URI decodeURI(From value) throws ConverterException;

	/**
	 * <p>
	 * Decodes to URL.
	 * </p>
	 * 
	 * @param value the encoded value to decode
	 * 
	 * @return a decoded value
	 *
	 * @throws ConverterException if there was an error decoding the value
	 */
	URL decodeURL(From value) throws ConverterException;

	/**
	 * <p>
	 * Decodes to pattern.
	 * </p>
	 * 
	 * @param value the encoded value to decode
	 * 
	 * @return a decoded value
	 *
	 * @throws ConverterException if there was an error decoding the value
	 */
	Pattern decodePattern(From value) throws ConverterException;

	/**
	 * <p>
	 * Decodes to inet address.
	 * </p>
	 * 
	 * @param value the encoded value to decode
	 * 
	 * @return a decoded value
	 *
	 * @throws ConverterException if there was an error decoding the value
	 */
	InetAddress decodeInetAddress(From value) throws ConverterException;
	
	/**
	 * <p>
	 * Decodes to inet socket address.
	 * </p>
	 * 
	 * @param value the encoded value to decode
	 * 
	 * @return a decoded value
	 *
	 * @throws ConverterException if there was an error decoding the value
	 */
	InetSocketAddress decodeInetSocketAddress(From value) throws ConverterException;

	/**
	 * <p>
	 * Decodes to class.
	 * </p>
	 *
	 * @param <T>   the target class type
	 * @param value the encoded value to decode
	 * 
	 * @return a decoded value
	 *
	 * @throws ConverterException if there was an error decoding the value
	 */
	<T> Class<T> decodeClass(From value) throws ConverterException;
}

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
 * An object encoder providing primitive bindings to encode primitive and common types.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Encoder
 * 
 * @param <To> the encoded type
 */
public interface PrimitiveEncoder<To> extends Encoder<Object, To> {

	/**
	 * <p>
	 * Encodes byte.
	 * </p>
	 * 
	 * @param value the value to encode
	 * 
	 * @return an encoded value
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	To encode(Byte value) throws ConverterException;

	/**
	 * <p>
	 * Encodes short.
	 * </p>
	 * 
	 * @param value the value to encode
	 * 
	 * @return an encoded value
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	To encode(Short value) throws ConverterException;

	/**
	 * <p>
	 * Encodes integer.
	 * </p>
	 * 
	 * @param value the value to encode
	 * 
	 * @return an encoded value
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	To encode(Integer value) throws ConverterException;

	/**
	 * <p>
	 * Encodes long.
	 * </p>
	 * 
	 * @param value the value to encode
	 * 
	 * @return an encoded value
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	To encode(Long value) throws ConverterException;

	/**
	 * <p>
	 * Encodes float.
	 * </p>
	 * 
	 * @param value the value to encode
	 * 
	 * @return an encoded value
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	To encode(Float value) throws ConverterException;

	/**
	 * <p>
	 * Encodes double.
	 * </p>
	 * 
	 * @param value the value to encode
	 * 
	 * @return an encoded value
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	To encode(Double value) throws ConverterException;

	/**
	 * <p>
	 * Encodes character.
	 * </p>
	 * 
	 * @param value the value to encode
	 * 
	 * @return an encoded value
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	To encode(Character value) throws ConverterException;

	/**
	 * <p>
	 * Encodes boolean.
	 * </p>
	 * 
	 * @param value the value to encode
	 * 
	 * @return an encoded value
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	To encode(Boolean value) throws ConverterException;

	/**
	 * <p>
	 * Encodes string.
	 * </p>
	 * 
	 * @param value the value to encode
	 * 
	 * @return an encoded value
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	To encode(String value) throws ConverterException;

	/**
	 * <p>
	 * Encodes big integer.
	 * </p>
	 * 
	 * @param value the value to encode
	 * 
	 * @return an encoded value
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	To encode(BigInteger value) throws ConverterException;

	/**
	 * <p>
	 * Encodes big decimal.
	 * </p>
	 * 
	 * @param value the value to encode
	 * 
	 * @return an encoded value
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	To encode(BigDecimal value) throws ConverterException;

	/**
	 * <p>
	 * Encodes local date.
	 * </p>
	 * 
	 * @param value the value to encode
	 * 
	 * @return an encoded value
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	To encode(LocalDate value) throws ConverterException;

	/**
	 * <p>
	 * Encodes local datetime.
	 * </p>
	 * 
	 * @param value the value to encode
	 * 
	 * @return an encoded value
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	To encode(LocalDateTime value) throws ConverterException;

	/**
	 * <p>
	 * Encodes zoned datetime.
	 * </p>
	 * 
	 * @param value the value to encode
	 * 
	 * @return an encoded value
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	To encode(ZonedDateTime value) throws ConverterException;

	/**
	 * <p>
	 * Encodes currency.
	 * </p>
	 * 
	 * @param value the value to encode
	 * 
	 * @return an encoded value
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	To encode(Currency value) throws ConverterException;

	/**
	 * <p>
	 * Encodes locale.
	 * </p>
	 * 
	 * @param value the value to encode
	 * 
	 * @return an encoded value
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	To encode(Locale value) throws ConverterException;

	/**
	 * <p>
	 * Encodes file.
	 * </p>
	 * 
	 * @param value the value to encode
	 * 
	 * @return an encoded value
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	To encode(File value) throws ConverterException;

	/**
	 * <p>
	 * Encodes path.
	 * </p>
	 * 
	 * @param value the value to encode
	 * 
	 * @return an encoded value
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	To encode(Path value) throws ConverterException;

	/**
	 * <p>
	 * Encodes URI.
	 * </p>
	 * 
	 * @param value the value to encode
	 * 
	 * @return an encoded value
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	To encode(URI value) throws ConverterException;

	/**
	 * <p>
	 * Encodes URL.
	 * </p>
	 * 
	 * @param value the value to encode
	 * 
	 * @return an encoded value
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	To encode(URL value) throws ConverterException;

	/**
	 * <p>
	 * Encodes pattern.
	 * </p>
	 * 
	 * @param value the value to encode
	 * 
	 * @return an encoded value
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	To encode(Pattern value) throws ConverterException;

	/**
	 * <p>
	 * Encodes inet address.
	 * </p>
	 * 
	 * @param value the value to encode
	 * 
	 * @return an encoded value
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	To encode(InetAddress value) throws ConverterException;
	
	/**
	 * <p>
	 * Encodes inet socket address.
	 * </p>
	 * 
	 * @param value the value to encode
	 * 
	 * @return an encoded value
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	To encode(InetSocketAddress value) throws ConverterException;

	/**
	 * <p>
	 * Encodes class.
	 * </p>
	 * 
	 * @param value the value to encode
	 * 
	 * @return an encoded value
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	To encode(Class<?> value) throws ConverterException;
}

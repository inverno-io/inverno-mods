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
package io.winterframework.mod.commons.converter;

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
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * @author jkuhn
 *
 */
public interface GeneralDecoder<A, B extends GeneralDecoder<A, B>> extends Decoder<A, B> {

	Byte toByte(A value) throws ValueCodecException;
	
	Short toShort(A value) throws ValueCodecException;
	
	Integer toInteger(A value) throws ValueCodecException;
	
	Long toLong(A value) throws ValueCodecException;

	Float toFloat(A value) throws ValueCodecException;
	
	Double toDouble(A value) throws ValueCodecException;
	
	Character toCharacter(A value) throws ValueCodecException;
	
	Boolean toBoolean(A value) throws ValueCodecException;
	
	String toString(A value) throws ValueCodecException;
	
	BigInteger toBigInteger(A value) throws ValueCodecException;
	
	BigDecimal toBigDecimal(A value) throws ValueCodecException;
	
	LocalDate toLocalDate(A value) throws ValueCodecException;
	
	LocalDateTime toLocalDateTime(A value) throws ValueCodecException;
	
	ZonedDateTime toZonedDateTime(A value) throws ValueCodecException;
	
	Currency toCurrency(A value) throws ValueCodecException;
	
	Locale toLocale(A value) throws ValueCodecException;
	
	File toFile(A value) throws ValueCodecException;
	
	Path toPath(A value) throws ValueCodecException;
	
	URI toURI(A value) throws ValueCodecException;
	
	URL toURL(A value) throws ValueCodecException;
	
	Pattern toPattern(A value) throws ValueCodecException;
	
	InetAddress toInetAddress(A value) throws ValueCodecException;
	
	Class<?> toClass(A value) throws ValueCodecException;
}

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
package io.winterframework.mod.base.converter;

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
public interface PrimitiveDecoder<From> extends Decoder<From, Object> {

	Byte decodeByte(From data) throws ConverterException;
	
	Short decodeShort(From data) throws ConverterException;
	
	Integer decodeInteger(From data) throws ConverterException;
	
	Long decodeLong(From data) throws ConverterException;

	Float decodeFloat(From data) throws ConverterException;
	
	Double decodeDouble(From data) throws ConverterException;
	
	Character decodeCharacter(From data) throws ConverterException;
	
	Boolean decodeBoolean(From data) throws ConverterException;
	
	String decodeString(From data) throws ConverterException;
	
	BigInteger decodeBigInteger(From data) throws ConverterException;
	
	BigDecimal decodeBigDecimal(From data) throws ConverterException;
	
	LocalDate decodeLocalDate(From data) throws ConverterException;
	
	LocalDateTime decodeLocalDateTime(From data) throws ConverterException;

	ZonedDateTime decodeZonedDateTime(From data) throws ConverterException;
	
	Currency decodeCurrency(From data) throws ConverterException;

	Locale decodeLocale(From data) throws ConverterException;
	
	File decodeFile(From data) throws ConverterException;
	
	Path decodePath(From data) throws ConverterException;
	
	URI decodeURI(From data) throws ConverterException;

	URL decodeURL(From data) throws ConverterException;

	Pattern decodePattern(From data) throws ConverterException;

	InetAddress decodeInetAddress(From data) throws ConverterException;
	
	Class<?> decodeClass(From data) throws ConverterException;
}

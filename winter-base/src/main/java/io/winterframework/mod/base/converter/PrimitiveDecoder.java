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

	Byte decodeByte(From value) throws ConverterException;
	
	Short decodeShort(From value) throws ConverterException;
	
	Integer decodeInteger(From value) throws ConverterException;
	
	Long decodeLong(From value) throws ConverterException;

	Float decodeFloat(From value) throws ConverterException;
	
	Double decodeDouble(From value) throws ConverterException;
	
	Character decodeCharacter(From value) throws ConverterException;
	
	Boolean decodeBoolean(From value) throws ConverterException;
	
	String decodeString(From value) throws ConverterException;
	
	BigInteger decodeBigInteger(From value) throws ConverterException;
	
	BigDecimal decodeBigDecimal(From value) throws ConverterException;
	
	LocalDate decodeLocalDate(From value) throws ConverterException;
	
	LocalDateTime decodeLocalDateTime(From value) throws ConverterException;

	ZonedDateTime decodeZonedDateTime(From value) throws ConverterException;
	
	Currency decodeCurrency(From value) throws ConverterException;

	Locale decodeLocale(From value) throws ConverterException;
	
	File decodeFile(From value) throws ConverterException;
	
	Path decodePath(From value) throws ConverterException;
	
	URI decodeURI(From value) throws ConverterException;

	URL decodeURL(From value) throws ConverterException;

	Pattern decodePattern(From value) throws ConverterException;

	InetAddress decodeInetAddress(From value) throws ConverterException;
	
	Class<?> decodeClass(From value) throws ConverterException;
}

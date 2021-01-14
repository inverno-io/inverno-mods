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
public interface PrimitiveEncoder<To> extends Encoder<Object, To> {

	To encode(Byte value) throws ConverterException;

	To encode(Short value) throws ConverterException;

	To encode(Integer value) throws ConverterException;

	To encode(Long value) throws ConverterException;

	To encode(Float value) throws ConverterException;

	To encode(Double value) throws ConverterException;

	To encode(Character value) throws ConverterException;

	To encode(Boolean value) throws ConverterException;
	
	To encode(String value) throws ConverterException;

	To encode(BigInteger value) throws ConverterException;

	To encode(BigDecimal value) throws ConverterException;

	To encode(LocalDate value) throws ConverterException;

	To encode(LocalDateTime value) throws ConverterException;

	To encode(ZonedDateTime value) throws ConverterException;

	To encode(Currency value) throws ConverterException;

	To encode(Locale value) throws ConverterException;

	To encode(File value) throws ConverterException;

	To encode(Path value) throws ConverterException;

	To encode(URI value) throws ConverterException;

	To encode(URL value) throws ConverterException;

	To encode(Pattern value) throws ConverterException;

	To encode(InetAddress value) throws ConverterException;

	To encode(Class<?> value) throws ConverterException;
}

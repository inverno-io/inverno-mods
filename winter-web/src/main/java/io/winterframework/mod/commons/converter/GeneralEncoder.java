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
public interface GeneralEncoder<A, B extends GeneralEncoder<A, B>> extends Encoder<A, B> {

	A from(Byte value) throws ValueCodecException;
	
	A from(Short value) throws ValueCodecException;
	
	A from(Integer value) throws ValueCodecException;
	
	A from(Long value) throws ValueCodecException;

	A from(Float value) throws ValueCodecException;
	
	A from(Double value) throws ValueCodecException;
	
	A from(Character value) throws ValueCodecException;
	
	A from(Boolean value) throws ValueCodecException;
	
	A from(String value) throws ValueCodecException;
	
	A from(BigInteger value) throws ValueCodecException;
	
	A from(BigDecimal value) throws ValueCodecException;
	
	A from(LocalDate value) throws ValueCodecException;
	
	A from(LocalDateTime value) throws ValueCodecException;
	
	A from(ZonedDateTime value) throws ValueCodecException;
	
	A from(Currency value) throws ValueCodecException;
	
	A from(Locale value) throws ValueCodecException;
	
	A from(File value) throws ValueCodecException;
	
	A from(Path value) throws ValueCodecException;
	
	A from(URI value) throws ValueCodecException;
	
	A from(URL value) throws ValueCodecException;
	
	A from(Pattern value) throws ValueCodecException;
	
	A from(InetAddress value) throws ValueCodecException;
	
	A from(Class<?> value) throws ValueCodecException;
}

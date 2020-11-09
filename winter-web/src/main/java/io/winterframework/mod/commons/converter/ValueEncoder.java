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
public interface ValueEncoder<E> {

	E from(Object value) throws ValueCodecException;

	E from(Collection<?> value) throws ValueCodecException;
	
	E from(List<?> value) throws ValueCodecException;
	
	E from(Set<?> value) throws ValueCodecException;
	
	E from(Object[] value) throws ValueCodecException;
	
	E from(Byte value) throws ValueCodecException;
	
	E from(Short value) throws ValueCodecException;
	
	E from(Integer value) throws ValueCodecException;
	
	E from(Long value) throws ValueCodecException;

	E from(Float value) throws ValueCodecException;
	
	E from(Double value) throws ValueCodecException;
	
	E from(Character value) throws ValueCodecException;
	
	E from(Boolean value) throws ValueCodecException;
	
	E from(String value) throws ValueCodecException;
	
	E from(BigInteger value) throws ValueCodecException;
	
	E from(BigDecimal value) throws ValueCodecException;
	
	E from(LocalDate value) throws ValueCodecException;
	
	E from(LocalDateTime value) throws ValueCodecException;
	
	E from(ZonedDateTime value) throws ValueCodecException;
	
	E from(Currency value) throws ValueCodecException;
	
	E from(Locale value) throws ValueCodecException;
	
	E from(File value) throws ValueCodecException;
	
	E from(Path value) throws ValueCodecException;
	
	E from(URI value) throws ValueCodecException;
	
	E from(URL value) throws ValueCodecException;
	
	E from(Pattern value) throws ValueCodecException;
	
	E from(InetAddress value) throws ValueCodecException;
	
	E from(Class<?> value) throws ValueCodecException;
}

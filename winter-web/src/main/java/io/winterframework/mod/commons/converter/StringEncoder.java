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
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * @author jkuhn
 *
 */
public class StringEncoder implements GeneralEncoder<String, StringEncoder>, SplittableEncoder<String, StringEncoder> {

	public StringEncoder() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public <T> StringEncoder register(Class<T> type, Function<T, String> converter) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public <T> String from(T value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(Collection<?> value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(List<?> value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(Set<?> value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(Object[] value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(Byte value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(Short value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(Integer value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(Long value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(Float value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(Double value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(Character value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(Boolean value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(String value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(BigInteger value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(BigDecimal value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(LocalDate value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(LocalDateTime value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(ZonedDateTime value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(Currency value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(Locale value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(File value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(Path value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(URI value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(URL value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(Pattern value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(InetAddress value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String from(Class<?> value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

}

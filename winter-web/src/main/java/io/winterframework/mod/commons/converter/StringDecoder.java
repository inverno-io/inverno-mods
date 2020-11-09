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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * @author jkuhn
 *
 */
public class StringDecoder implements GeneralDecoder<String, StringDecoder>, SplittableDecoder<String, StringDecoder> {

	private Map<Class<?>, Function<String, ?>> converters;
	
	public StringDecoder() {
		this.converters = new HashMap<>();
	}
	
	@Override
	public <T> StringDecoder register(Class<T> type, Function<String, T> converter) {
		// TODO at some point what happens if a converter can convert everything: eg. Json
		// We should find a way to default converters:
		// - we start from a string that we want to convert to an int
		// - do we have a converter that match the actual type
		// - if yes take this converter if no look for the super type
		// - ...
		// finally we end up with a String to Object converter (json whatever)
		// Note regarding rest services we must to take the mimetype into account so basically we'll probably create adhoc codec associated to mimetype for this
		this.converters.put(type, converter);
		return this;
	}
	
	private Optional<Function<String, ?>> resolveConverter(Class<?> type) {
		return null;
	}
	
	@Override
	public <T> T to(String value, Class<T> type) {
		
		return null;
	}
	
	@Override
	public <T> Collection<T> toCollectionOf(String value, Class<T> type) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> List<T> toListOf(String value, Class<T> type) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> Set<T> toSetOf(String value, Class<T> type) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T[] toArrayOf(String value, Class<T> type) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Byte toByte(String value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Short toShort(String value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer toInteger(String value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long toLong(String value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Float toFloat(String value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Double toDouble(String value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Character toCharacter(String value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean toBoolean(String value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString(String value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BigInteger toBigInteger(String value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BigDecimal toBigDecimal(String value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LocalDate toLocalDate(String value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LocalDateTime toLocalDateTime(String value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ZonedDateTime toZonedDateTime(String value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Currency toCurrency(String value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Locale toLocale(String value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File toFile(String value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path toPath(String value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URI toURI(String value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URL toURL(String value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Pattern toPattern(String value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InetAddress toInetAddress(String value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<?> toClass(String value) throws ValueCodecException {
		// TODO Auto-generated method stub
		return null;
	}

}

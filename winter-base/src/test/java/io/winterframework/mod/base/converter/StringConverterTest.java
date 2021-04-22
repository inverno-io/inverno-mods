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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.winterframework.mod.base.reflect.Types;

/**
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 *
 */
public class StringConverterTest {

	@Test
	public void testStringConverter() throws URISyntaxException, MalformedURLException, UnknownHostException {
		StringConverter converter = new StringConverter();
		
		Byte byte_value = Byte.valueOf("5");
		Assertions.assertEquals(byte_value, converter.decodeByte(converter.encode(byte_value)));
		
		Short short_value = Short.valueOf("6");
		Assertions.assertEquals(short_value, converter.decodeShort(converter.encode(short_value)));
		
		Integer integer_value = Integer.valueOf("55656");
		Assertions.assertEquals(integer_value, converter.decodeInteger(converter.encode(integer_value)));
		
		Long long_value = Long.valueOf("456654894984");
		Assertions.assertEquals(long_value, converter.decodeLong(converter.encode(long_value)));
		
		Float float_value = Float.valueOf("52.36");
		Assertions.assertEquals(float_value, converter.decodeFloat(converter.encode(float_value)));
		
		Double double_value = Double.valueOf("5654842.36");
		Assertions.assertEquals(double_value, converter.decodeDouble(converter.encode(double_value)));
		
		Character character_value = Character.valueOf('\n');
		Assertions.assertEquals(character_value, converter.decodeCharacter(converter.encode(character_value)));
		
		Boolean boolean_value = Boolean.valueOf(true);
		Assertions.assertEquals(boolean_value, converter.decodeBoolean(converter.encode(boolean_value)));
		
		String string_value = "dslkfdgf\nsfkjdfhd\t\"fdlgf";
		Assertions.assertEquals(string_value, converter.decodeString(converter.encode(string_value)));
		
		BigInteger bigInteger_value = new BigInteger("464984894894894894894651321564794135168798745318964651564894651684841654894651468974651518946168916198764165479797");
		Assertions.assertEquals(bigInteger_value, converter.decodeBigInteger(converter.encode(bigInteger_value)));
		
		BigDecimal bigDecimal_value = new BigDecimal("464984894894894894894651321564794135168798745318964651564894651684841654894651468974651518946168916198764165479797.26554946400000000");
		Assertions.assertEquals(bigDecimal_value, converter.decodeBigDecimal(converter.encode(bigDecimal_value)));
		
		LocalDate localDate_value = LocalDate.now();
		Assertions.assertEquals(localDate_value, converter.decodeLocalDate(converter.encode(localDate_value)));
		
		LocalDateTime localDateTime_value = LocalDateTime.now();
		Assertions.assertEquals(localDateTime_value, converter.decodeLocalDateTime(converter.encode(localDateTime_value)));
		
		ZonedDateTime zonedDateTime_value = ZonedDateTime.now();
		Assertions.assertEquals(zonedDateTime_value, converter.decodeZonedDateTime(converter.encode(zonedDateTime_value)));
		
		Currency currency_value = Currency.getInstance("GBP");
		Assertions.assertEquals(currency_value, converter.decodeCurrency(converter.encode(currency_value)));
		
		Locale locale_value = Locale.getDefault();
		Assertions.assertEquals(locale_value, converter.decodeLocale(converter.encode(locale_value)));
		
		File file_value = new File(".");
		Assertions.assertEquals(file_value, converter.decodeFile(converter.encode(file_value)));
		
		Path path_value = Paths.get(".");
		Assertions.assertEquals(path_value, converter.decodePath(converter.encode(path_value)));
		
		URI uri_value = new URI("file:/tmp");
		Assertions.assertEquals(uri_value, converter.decodeURI(converter.encode(uri_value)));
		
		URL url_value = new URL("file:/tmp");
		Assertions.assertEquals(url_value, converter.decodeURL(converter.encode(url_value)));
		
		Pattern pattern_value = Pattern.compile("\\d+\\{.*\\}");
		Assertions.assertEquals(pattern_value.pattern(), converter.decodePattern(converter.encode(pattern_value)).pattern());
		
		InetAddress inetAddress_value = InetAddress.getLocalHost();
		Assertions.assertEquals(inetAddress_value, converter.decodeInetAddress(converter.encode(inetAddress_value)));
		
		Class<?> class_value = List.class;
		Assertions.assertEquals(class_value, converter.decodeClass(converter.encode(class_value)));
	}
	
	@Test
	public void testDecodeType() throws NoSuchFieldException, SecurityException {
		StringConverter converter = new StringConverter();
		
		Collection<Integer> data = converter.decode("4,5,6,7", Types.type(Collection.class).type(Integer.class).and().build());
		
		Assertions.assertEquals(4, data.size());
		
		Assertions.assertEquals(List.of(4,5,6,7), new ArrayList<>(data));
	}
}

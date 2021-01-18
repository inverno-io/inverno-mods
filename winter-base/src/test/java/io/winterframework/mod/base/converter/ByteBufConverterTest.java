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
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.winterframework.mod.base.Charsets;

/**
 * @author jkuhn
 *
 */
class ByteBufConverterTest {

	@Test
	void testEncode() throws ConverterException, URISyntaxException, MalformedURLException, UnknownHostException {
		ByteBufConverter converter = new ByteBufConverter();
		
		ByteBuf buffer = converter.encode(List.of("a","b","c"));
		Assertions.assertEquals("a,b,c", buffer.toString(Charsets.DEFAULT));
		
		buffer = converter.encodeList(List.of("a","b","c"));
		Assertions.assertEquals("a,b,c", buffer.toString(Charsets.DEFAULT));

		buffer = converter.encode(new LinkedHashSet<>(List.of("a","b","c")));
		Assertions.assertEquals("a,b,c", buffer.toString(Charsets.DEFAULT));
		
		buffer = converter.encodeSet(new LinkedHashSet<>(List.of("a","b","c")));
		Assertions.assertEquals("a,b,c", buffer.toString(Charsets.DEFAULT));
		
		buffer = converter.encode(new String[]{"a","b","c"});
		Assertions.assertEquals("a,b,c", buffer.toString(Charsets.DEFAULT));
		
		buffer = converter.encodeArray(new String[]{"a","b","c"});
		Assertions.assertEquals("a,b,c", buffer.toString(Charsets.DEFAULT));
		
		buffer = converter.encode((byte)64);
		Assertions.assertEquals("64", buffer.toString(Charsets.DEFAULT));
		
		buffer = converter.encode((short)256);
		Assertions.assertEquals("256", buffer.toString(Charsets.DEFAULT));
		
		buffer = converter.encode((int)1024);
		Assertions.assertEquals("1024", buffer.toString(Charsets.DEFAULT));
		
		buffer = converter.encode((long)2048);
		Assertions.assertEquals("2048", buffer.toString(Charsets.DEFAULT));
		
		buffer = converter.encode((float)12345.5);
		Assertions.assertEquals("12345.5", buffer.toString(Charsets.DEFAULT));
		
		buffer = converter.encode((double)123456789.54321);
		Assertions.assertEquals("1.2345678954321E8", buffer.toString(Charsets.DEFAULT));
		
		buffer = converter.encode('a');
		Assertions.assertEquals("a", buffer.toString(Charsets.DEFAULT));
	
		buffer = converter.encode(true);
		Assertions.assertEquals("true", buffer.toString(Charsets.DEFAULT));
	
		buffer = converter.encode("abcdef");
		Assertions.assertEquals("abcdef", buffer.toString(Charsets.DEFAULT));
	
		buffer = converter.encode(new BigInteger("123457891011121314151617181920212223242526"));
		Assertions.assertEquals("123457891011121314151617181920212223242526", buffer.toString(Charsets.DEFAULT));
		
		buffer = converter.encode(new BigDecimal("123457891011121314151617181920212223242526.987654321"));
		Assertions.assertEquals("123457891011121314151617181920212223242526.987654321", buffer.toString(Charsets.DEFAULT));
	
		buffer = converter.encode(LocalDate.of(2021, 1, 15));
		Assertions.assertEquals("2021-01-15", buffer.toString(Charsets.DEFAULT));
		
		buffer = converter.encode(LocalDateTime.of(2021, 1, 15, 18, 0, 52));
		Assertions.assertEquals("2021-01-15T18:00:52", buffer.toString(Charsets.DEFAULT));
		
		buffer = converter.encode(ZonedDateTime.of(LocalDate.of(2021, 1, 15), LocalTime.of(18, 0, 52), ZoneId.of("UTC")));
		Assertions.assertEquals("2021-01-15T18:00:52Z[UTC]", buffer.toString(Charsets.DEFAULT));
		
		buffer = converter.encode(Currency.getInstance("EUR"));
		Assertions.assertEquals("EUR", buffer.toString(Charsets.DEFAULT));
		
		buffer = converter.encode(Locale.FRANCE);
		Assertions.assertEquals("fr_FR", buffer.toString(Charsets.DEFAULT));
		
		buffer = converter.encode(new File("/abc.txt"));
		Assertions.assertEquals("/abc.txt", buffer.toString(Charsets.DEFAULT));
		
		buffer = converter.encode(Paths.get("/abc.txt"));
		Assertions.assertEquals("/abc.txt", buffer.toString(Charsets.DEFAULT));
		
		buffer = converter.encode(new URI("http://127.0.0.1:8080/abc"));
		Assertions.assertEquals("http://127.0.0.1:8080/abc", buffer.toString(Charsets.DEFAULT));
		
		buffer = converter.encode(new URL("http://127.0.0.1:8080/abc"));
		Assertions.assertEquals("http://127.0.0.1:8080/abc", buffer.toString(Charsets.DEFAULT));
		
		buffer = converter.encode(Pattern.compile("[a-b]+.*"));
		Assertions.assertEquals("[a-b]+.*", buffer.toString(Charsets.DEFAULT));
		
		buffer = converter.encode(InetAddress.getByName("localhost"));
		Assertions.assertEquals("localhost", buffer.toString(Charsets.DEFAULT));
		
		buffer = converter.encode(String.class);
		Assertions.assertEquals("java.lang.String", buffer.toString(Charsets.DEFAULT));
	}

	@Test
	public void testByteBufConverter() throws URISyntaxException, MalformedURLException, UnknownHostException {
		ByteBufConverter converter = new ByteBufConverter();
		
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
}

package io.winterframework.mod.configuration.codec;

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
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StringValueCodecTest {

	@Test
	public void test() throws URISyntaxException, MalformedURLException, UnknownHostException {
		StringValueEncoder encoder = new StringValueEncoder();
		StringValueDecoder decoder = new StringValueDecoder();
		
		Byte byte_value = Byte.valueOf("5");
		Assertions.assertEquals(byte_value, decoder.toByte(encoder.from(byte_value)));
		
		Short short_value = Short.valueOf("6");
		Assertions.assertEquals(short_value, decoder.toShort(encoder.from(short_value)));
		
		Integer integer_value = Integer.valueOf("55656");
		Assertions.assertEquals(integer_value, decoder.toInteger(encoder.from(integer_value)));
		
		Long long_value = Long.valueOf("456654894984");
		Assertions.assertEquals(long_value, decoder.toLong(encoder.from(long_value)));
		
		Float float_value = Float.valueOf("52.36");
		Assertions.assertEquals(float_value, decoder.toFloat(encoder.from(float_value)));
		
		Double double_value = Double.valueOf("5654842.36");
		Assertions.assertEquals(double_value, decoder.toDouble(encoder.from(double_value)));
		
		Character character_value = Character.valueOf('\n');
		Assertions.assertEquals(character_value, decoder.toCharacter(encoder.from(character_value)));
		
		Boolean boolean_value = Boolean.valueOf(true);
		Assertions.assertEquals(boolean_value, decoder.toBoolean(encoder.from(boolean_value)));
		
		String string_value = "dslkfdgf\nsfkjdfhd\t\"fdlgf";
		Assertions.assertEquals(string_value, decoder.toString(encoder.from(string_value)));
		
		BigInteger bigInteger_value = new BigInteger("464984894894894894894651321564794135168798745318964651564894651684841654894651468974651518946168916198764165479797");
		Assertions.assertEquals(bigInteger_value, decoder.toBigInteger(encoder.from(bigInteger_value)));
		
		BigDecimal bigDecimal_value = new BigDecimal("464984894894894894894651321564794135168798745318964651564894651684841654894651468974651518946168916198764165479797.26554946400000000");
		Assertions.assertEquals(bigDecimal_value, decoder.toBigDecimal(encoder.from(bigDecimal_value)));
		
		LocalDate localDate_value = LocalDate.now();
		Assertions.assertEquals(localDate_value, decoder.toLocalDate(encoder.from(localDate_value)));
		
		LocalDateTime localDateTime_value = LocalDateTime.now();
		Assertions.assertEquals(localDateTime_value, decoder.toLocalDateTime(encoder.from(localDateTime_value)));
		
		ZonedDateTime zonedDateTime_value = ZonedDateTime.now();
		Assertions.assertEquals(zonedDateTime_value, decoder.toZonedDateTime(encoder.from(zonedDateTime_value)));
		
		Currency currency_value = Currency.getInstance("GBP");
		Assertions.assertEquals(currency_value, decoder.toCurrency(encoder.from(currency_value)));
		
		Locale locale_value = Locale.getDefault();
		Assertions.assertEquals(locale_value, decoder.toLocale(encoder.from(locale_value)));
		
		File file_value = new File(".");
		Assertions.assertEquals(file_value, decoder.toFile(encoder.from(file_value)));
		
		Path path_value = Paths.get(".");
		Assertions.assertEquals(path_value, decoder.toPath(encoder.from(path_value)));
		
		URI uri_value = new URI("file:/tmp");
		Assertions.assertEquals(uri_value, decoder.toURI(encoder.from(uri_value)));
		
		URL url_value = new URL("file:/tmp");
		Assertions.assertEquals(url_value, decoder.toURL(encoder.from(url_value)));
		
		Pattern pattern_value = Pattern.compile("\\d+\\{.*\\}");
		Assertions.assertEquals(pattern_value.pattern(), decoder.toPattern(encoder.from(pattern_value)).pattern());
		
		InetAddress inetAddress_value = InetAddress.getLocalHost();
		Assertions.assertEquals(inetAddress_value, decoder.toInetAddress(encoder.from(inetAddress_value)));
		
		Class<?> class_value = List.class;
		Assertions.assertEquals(class_value, decoder.toClass(encoder.from(class_value)));
		
	}

}

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
package io.inverno.mod.irt;

import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.Currency;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

/**
 * <p>
 * A collection of pipes used to transform basic values including: strings transformation, date formatting, number formatting, escaping...
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 */
public final class Pipes {
	
	/* Strings */
	/**
	 * <p>
	 * Returns a pipe which converts all characters in a string to upper case.
	 * </p>
	 * 
	 * @return a pipe
	 */
	public static Pipe<String, String> uppercase() {
		return String::toUpperCase;
	}
	
	/**
	 * <p>
	 * Returns a pipe which converts all characters in a string to lower case.
	 * </p>
	 * 
	 * @return a pipe
	 */
	public static Pipe<String, String> lowercase() {
		return String::toLowerCase;
	}
	
	/**
	 * <p>
	 * Returns a pipe which changes the first character in a string to title case.
	 * </p>
	 * 
	 * @return a pipe
	 */
	public static Pipe<String, String> titlecase() {
		return StringUtils::capitalize;
	}
	
	/* Date pipes */
	
	/**
	 * <p>
	 * Returns a pipe which formats a temporal object (date, time, date-time...) using the specified formatter.
	 * </p>
	 * 
	 * @param formatter a date time formatter
	 * 
	 * @return a pipe
	 */
	public static Pipe<TemporalAccessor, String> dateTime(DateTimeFormatter formatter) {
		return formatter::format;
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a temporal object (date, time, date-time...) as a date using the specified format style.
	 * </p>
	 * 
	 * @param dateStyle a date format style
	 * 
	 * @return a pipe
	 */
	// TODO This is not performant as we might create a lot of datetimeformater
	public static Pipe<TemporalAccessor, String> date(FormatStyle dateStyle) {
		return source -> DateTimeFormatter.ofLocalizedDate(dateStyle).format(source);
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a temporal object (date, time, date-time...) as a date using the specified format style and locale.
	 * </p>
	 * 
	 * @param dateStyle a date format style
	 * @param locale    a locale
	 * 
	 * @return a pipe
	 */
	public static Pipe<TemporalAccessor, String> date(FormatStyle dateStyle, Locale locale) {
		return source -> DateTimeFormatter.ofLocalizedDate(dateStyle).withLocale(locale).format(source);
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a temporal object (date, time, date-time...) as a date-time using the specified format style.
	 * </p>
	 * 
	 * @param dateTimeStyle a date-time format style
	 * 
	 * @return a pipe
	 */
	public static Pipe<TemporalAccessor, String> dateTime(FormatStyle dateTimeStyle) {
		return source -> DateTimeFormatter.ofLocalizedDateTime(dateTimeStyle).format(source);
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a temporal object (date, time, date-time...) as a date-time using the specified format style and zone.
	 * </p>
	 * 
	 * @param dateTimeStyle a date-time format style
	 * @param zone          a zone
	 * 
	 * @return a pipe
	 */
	public static Pipe<TemporalAccessor, String> dateTime(FormatStyle dateTimeStyle, ZoneId zone) {
		return source -> DateTimeFormatter.ofLocalizedDateTime(dateTimeStyle).withZone(zone).format(source);
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a temporal object (date, time, date-time...) as a date-time using the specified format style and locale.
	 * </p>
	 * 
	 * @param dateTimeStyle a date-time format style
	 * @param locale        a locale
	 * 
	 * @return a pipe
	 */
	public static Pipe<TemporalAccessor, String> dateTime(FormatStyle dateTimeStyle, Locale locale) {
		return source -> DateTimeFormatter.ofLocalizedDateTime(dateTimeStyle).withLocale(locale).format(source);
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a temporal object (date, time, date-time...) as a date-time using the specified format style, zone and locale.
	 * </p>
	 * 
	 * @param dateTimeStyle a date-time format style
	 * @param zone          a zone
	 * @param locale        a locale
	 * 
	 * @return a pipe
	 */
	public static Pipe<TemporalAccessor, String> dateTime(FormatStyle dateTimeStyle, ZoneId zone, Locale locale) {
		return source -> DateTimeFormatter.ofLocalizedDateTime(dateTimeStyle).withZone(zone).withLocale(locale).format(source);
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a temporal object (date, time, date-time...) as a date-time using the specified pattern.
	 * </p>
	 * 
	 * @param pattern a date-time pattern
	 * 
	 * @return a pipe
	 */
	public static Pipe<TemporalAccessor, String> dateTime(String pattern) {
		return source -> DateTimeFormatter.ofPattern(pattern).format(source);
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a temporal object (date, time, date-time...) as a date-time using the specified pattern and zone.
	 * </p>
	 * 
	 * @param pattern a date-time pattern
	 * @param zone          a zone
	 * 
	 * @return a pipe
	 */
	public static Pipe<TemporalAccessor, String> dateTime(String pattern, ZoneId zone) {
		return source -> DateTimeFormatter.ofPattern(pattern).withZone(zone).format(source);
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a temporal object (date, time, date-time...) as a date-time using the specified pattern and locale.
	 * </p>
	 * 
	 * @param pattern a date-time pattern
	 * @param locale        a locale
	 * 
	 * @return a pipe
	 */
	public static Pipe<TemporalAccessor, String> dateTime(String pattern, Locale locale) {
		return source -> DateTimeFormatter.ofPattern(pattern).withLocale(locale).format(source);
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a temporal object (date, time, date-time...) as a date-time using the specified pattern, zone and locale.
	 * </p>
	 * 
	 * @param pattern a date-time pattern
	 * @param zone          a zone
	 * @param locale        a locale
	 * 
	 * @return a pipe
	 */
	public static Pipe<TemporalAccessor, String> dateTime(String pattern, ZoneId zone, Locale locale) {
		return source -> DateTimeFormatter.ofPattern(pattern).withZone(zone).withLocale(locale).format(source);
	}
	
	/* Number format */
	
	/**
	 * <p>
	 * Returns a pipe which formats a number using the specified format.
	 * </p>
	 * 
	 * @param formatter a number format
	 * 
	 * @return a pipe
	 */
	public static Pipe<Number, String> number(NumberFormat formatter) {
		return formatter::format;
	}

	/**
	 * <p>
	 * Returns a pipe which formats a number using the default format.
	 * </p>
	 * 
	 * @return a pipe
	 */
	public static Pipe<Number, String> number() {
		return NumberFormat.getNumberInstance()::format;
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a number using the format for the specified
	 * locale.
	 * </p>
	 * 
 	 * @param locale a locale 
	 * 
	 * @return a pipe
	 */
	public static Pipe<Number, String> number(Locale locale) {
		return NumberFormat.getNumberInstance(locale)::format;
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a number according to the specified limits.
	 * </p>
	 * 
	 * @param minIntegerDigits  the minimum number of digits allowed in the integer portion
	 * @param minFractionDigits the minimum number of digits allowed in the fraction portion
	 * @param maxFractionDigits the maximum number of digits allowed in the fraction portion
	 * 
	 * @return a pipe
	 */
	public static Pipe<Number, String> number(int minIntegerDigits, int minFractionDigits, int maxFractionDigits) {
		NumberFormat formatter = NumberFormat.getNumberInstance();
		formatter.setMinimumIntegerDigits(minIntegerDigits);
		formatter.setMinimumFractionDigits(minFractionDigits);
		formatter.setMaximumFractionDigits(maxFractionDigits);
		return formatter::format;
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a number for the specified locale according to the specified limits.
	 * </p>
	 * 
	 * @param minIntegerDigits  the minimum number of digits allowed in the integer portion
	 * @param minFractionDigits the minimum number of digits allowed in the fraction portion
	 * @param maxFractionDigits the maximum number of digits allowed in the fraction portion
	 * @param locale            a locale
	 * 
	 * @return a pipe
	 */
	public static Pipe<Number, String> number(int minIntegerDigits, int minFractionDigits, int maxFractionDigits, Locale locale) {
		NumberFormat formatter = NumberFormat.getNumberInstance(locale);
		formatter.setMinimumIntegerDigits(minIntegerDigits);
		formatter.setMinimumFractionDigits(minFractionDigits);
		formatter.setMaximumFractionDigits(maxFractionDigits);
		return formatter::format;
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a number as an integer.
	 * </p>
	 * 
	 * @return a pipe
	 */
	public static Pipe<Number, String> integer() {
		return NumberFormat.getIntegerInstance()::format;
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a number as an integer for the specified locale.
	 * </p>
	 * 
	 * @param locale a locale
	 * 
	 * @return a pipe
	 */
	public static Pipe<Number, String> integer(Locale locale) {
		return NumberFormat.getIntegerInstance(locale)::format;
	}
	
	/* currency */
	
	/**
	 * <p>
	 * Returns a pipe which formats a number as an amount in the default locale currency.
	 * </p>
	 * 
	 * @return a pipe
	 */
	public static Pipe<Number, String> currency() {
		return NumberFormat.getCurrencyInstance()::format;
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a number as an amount in the specified locale currency.
	 * </p>
	 * 
	 * @param locale a locale
	 * 
	 * @return a pipe
	 */
	public static Pipe<Number, String> currency(Locale locale) {
		return NumberFormat.getCurrencyInstance(locale)::format;
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a number as an amount in the default locale currency according to the specified limits.
	 * </p>
	 * 
	 * @param minIntegerDigits  the minimum number of digits allowed in the integer portion
	 * @param minFractionDigits the minimum number of digits allowed in the fraction portion
	 * @param maxFractionDigits the maximum number of digits allowed in the fraction portion
	 * 
	 * @return a pipe
	 */
	public static Pipe<Number, String> currency(int minIntegerDigits, int minFractionDigits, int maxFractionDigits) {
		NumberFormat formatter = NumberFormat.getCurrencyInstance();
		formatter.setMinimumIntegerDigits(minIntegerDigits);
		formatter.setMinimumFractionDigits(minFractionDigits);
		formatter.setMaximumFractionDigits(maxFractionDigits);
		return formatter::format;
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a number as an amount in the specified locale currency according to the specified limits.
	 * </p>
	 * 
	 * @param minIntegerDigits  the minimum number of digits allowed in the integer portion
	 * @param minFractionDigits the minimum number of digits allowed in the fraction portion
	 * @param maxFractionDigits the maximum number of digits allowed in the fraction portion
	 * @param locale            a locale
	 * 
	 * @return a pipe
	 */
	public static Pipe<Number, String> currency(int minIntegerDigits, int minFractionDigits, int maxFractionDigits, Locale locale) {
		NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
		formatter.setMinimumIntegerDigits(minIntegerDigits);
		formatter.setMinimumFractionDigits(minFractionDigits);
		formatter.setMaximumFractionDigits(maxFractionDigits);
		return formatter::format;
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a number as an amount in the specified currency using default locale format.
	 * </p>
	 * 
	 * @param currencyCode a currency code
	 * 
	 * @return a pipe
	 */
	public static Pipe<Number, String> currency(String currencyCode) {
		NumberFormat formatter = NumberFormat.getCurrencyInstance();
		formatter.setCurrency(Currency.getInstance(currencyCode));
		
		return formatter::format;
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a number as an amount in the specified currency using default locale format according to the specified limits.
	 * </p>
	 * 
	 * @param currencyCode      a currency code
	 * @param minIntegerDigits  the minimum number of digits allowed in the integer portion
	 * @param minFractionDigits the minimum number of digits allowed in the fraction portion
	 * @param maxFractionDigits the maximum number of digits allowed in the fraction portion
	 * 
	 * @return a pipe
	 */
	public static Pipe<Number, String> currency(String currencyCode, int minIntegerDigits, int minFractionDigits, int maxFractionDigits) {
		NumberFormat formatter = NumberFormat.getCurrencyInstance();
		formatter.setMinimumIntegerDigits(minIntegerDigits);
		formatter.setMinimumFractionDigits(minFractionDigits);
		formatter.setMaximumFractionDigits(maxFractionDigits);
		formatter.setCurrency(Currency.getInstance(currencyCode));
		return formatter::format;
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a number as an amount in the specified currency using specified locale format.
	 * </p>
	 * 
	 * @param currencyCode a currency code
	 * @param locale       a locale
	 * 
	 * @return a pipe
	 */
	public static Pipe<Number, String> currency(String currencyCode, Locale locale) {
		NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
		formatter.setCurrency(Currency.getInstance(currencyCode));
		
		return formatter::format;
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a number as an amount in the specified currency using the specified locale format according to the specified limits.
	 * </p>
	 * 
	 * @param currencyCode      a currency code
	 * @param minIntegerDigits  the minimum number of digits allowed in the integer portion
	 * @param minFractionDigits the minimum number of digits allowed in the fraction portion
	 * @param maxFractionDigits the maximum number of digits allowed in the fraction portion
	 * @param locale            a locale
	 * 
	 * @return a pipe
	 */
	public static Pipe<Number, String> currency(String currencyCode,  int minIntegerDigits, int minFractionDigits, int maxFractionDigits, Locale locale) {
		NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
		formatter.setMinimumIntegerDigits(minIntegerDigits);
		formatter.setMinimumFractionDigits(minFractionDigits);
		formatter.setMaximumFractionDigits(maxFractionDigits);
		formatter.setCurrency(Currency.getInstance(currencyCode));
		return formatter::format;
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a number as an amount in the specified currency using default locale format.
	 * </p>
	 * 
	 * @param currency a currency
	 * 
	 * @return a pipe
	 */
	public static Pipe<Number, String> currency(Currency currency) {
		NumberFormat formatter = NumberFormat.getCurrencyInstance();
		formatter.setCurrency(currency);
		return formatter::format;
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a number as an amount in the specified currency using default locale format according to the specified limits.
	 * </p>
	 * 
	 * @param currency          a currency
	 * @param minIntegerDigits  the minimum number of digits allowed in the integer portion
	 * @param minFractionDigits the minimum number of digits allowed in the fraction portion
	 * @param maxFractionDigits the maximum number of digits allowed in the fraction portion
	 * 
	 * @return a pipe
	 */
	public static Pipe<Number, String> currency(Currency currency, int minIntegerDigits, int minFractionDigits, int maxFractionDigits) {
		NumberFormat formatter = NumberFormat.getCurrencyInstance();
		formatter.setMinimumIntegerDigits(minIntegerDigits);
		formatter.setMinimumFractionDigits(minFractionDigits);
		formatter.setMaximumFractionDigits(maxFractionDigits);
		formatter.setCurrency(currency);
		return formatter::format;
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a number as an amount in the specified currency using the specified locale format.
	 * </p>
	 * 
	 * @param currency a currency
	 * @param locale a locale
	 * 
	 * @return a pipe
	 */
	public static Pipe<Number, String> currency(Currency currency, Locale locale) {
		NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
		formatter.setCurrency(currency);
		return formatter::format;
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a number as an amount in the specified currency using the specified locale format according to the specified limits.
	 * </p>
	 * 
	 * @param currency          a currency
	 * @param minIntegerDigits  the minimum number of digits allowed in the integer portion
	 * @param minFractionDigits the minimum number of digits allowed in the fraction portion
	 * @param maxFractionDigits the maximum number of digits allowed in the fraction portion
	 * @param locale            a locale
	 * 
	 * @return a pipe
	 */
	public static Pipe<Number, String> currency(Currency currency, int minIntegerDigits, int minFractionDigits, int maxFractionDigits, Locale locale) {
		NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
		formatter.setMinimumIntegerDigits(minIntegerDigits);
		formatter.setMinimumFractionDigits(minFractionDigits);
		formatter.setMaximumFractionDigits(maxFractionDigits);
		formatter.setCurrency(currency);
		return formatter::format;
	}
	
	/* percent */
	
	/**
	 * <p>
	 * Returns a pipe which formats a number as a percentage using default locale format.
	 * </p>
	 * 
	 * @return a pipe
	 */
	public static Pipe<Number, String> percent() {
		return NumberFormat.getPercentInstance()::format;
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a number as a percentage using the specified locale format.
	 * </p>
	 * 
	 * @param locale a locale
	 * 
	 * @return a pipe
	 */
	public static Pipe<Number, String> percent(Locale locale) {
		return NumberFormat.getPercentInstance(locale)::format;
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a number as a percentage using default locale format according to the specified limits.
	 * </p>
	 * 
	 * @param minIntegerDigits  the minimum number of digits allowed in the integer portion
	 * @param minFractionDigits the minimum number of digits allowed in the fraction portion
	 * @param maxFractionDigits the maximum number of digits allowed in the fraction portion
	 * 
	 * @return a pipe
	 */
	public static Pipe<Number, String> percent(int minIntegerDigits, int minFractionDigits, int maxFractionDigits) {
		NumberFormat formatter = NumberFormat.getPercentInstance();
		formatter.setMinimumIntegerDigits(minIntegerDigits);
		formatter.setMinimumFractionDigits(minFractionDigits);
		formatter.setMaximumFractionDigits(maxFractionDigits);
		return formatter::format;
	}
	
	/**
	 * <p>
	 * Returns a pipe which formats a number as a percentage using the specified locale format according to the specified limits.
	 * </p>
	 * 
	 * @param minIntegerDigits  the minimum number of digits allowed in the integer portion
	 * @param minFractionDigits the minimum number of digits allowed in the fraction portion
	 * @param maxFractionDigits the maximum number of digits allowed in the fraction portion
	 * @param locale            a locale
	 * 
	 * @return a pipe
	 */
	public static Pipe<Number, String> percent(int minIntegerDigits, int minFractionDigits, int maxFractionDigits, Locale locale) {
		NumberFormat formatter = NumberFormat.getPercentInstance(locale);
		formatter.setMinimumIntegerDigits(minIntegerDigits);
		formatter.setMinimumFractionDigits(minFractionDigits);
		formatter.setMaximumFractionDigits(maxFractionDigits);
		return formatter::format;
	}
	
	/* escape */
	
	/**
	 * <p>
	 * Returns a pipe which escapes a string using HTML entities.
	 * </p>
	 * 
	 * @return a pipe
	 */
	public static Pipe<String, String> escapeHtml() {
		return StringEscapeUtils::escapeHtml4;
	}
	
	/**
	 * <p>
	 * Returns a pipe which escapes a string using Json string rules rules.
	 * </p>
	 * 
	 * @return a pipe
	 */
	public static Pipe<String, String> escapeJson() {
		return StringEscapeUtils::escapeJson;
	}
	
	/**
	 * <p>
	 * Returns a pipe which escapes a string using XML entities.
	 * </p>
	 * 
	 * @return a pipe
	 */
	public static Pipe<String, String> escapeXml() {
		return StringEscapeUtils::escapeXml11;
	}
}

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
package io.winterframework.mod.web;

import java.io.File;
import java.lang.reflect.Type;
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
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author jkuhn
 *
 */
public interface Parameter {

	String getName();
	
	String getValue();
	
	<T> T as(Class<T> type);
	
	<T> T as(Type type);
	
	<T> T[] asArrayOf(Class<T> type);
	
	<T> T[] asArrayOf(Type type);
	
	<T> List<T> asListOf(Class<T> type);
	
	<T> List<T> asListOf(Type type);
	
	<T> Set<T> asSetOf(Class<T> type);
	
	<T> Set<T> asSetOf(Type type);
	
	Byte asByte();
	
	Short asShort();
	
	Integer asInteger();
	
	Long asLong();
	
	Float asFloat();
	
	Double asDouble();
	
	Character asCharacter();
	
	String asString();
	
	Boolean asBoolean();
	
	BigInteger asBigInteger();
	
	BigDecimal asBigDecimal();
	
	LocalDate asLocalDate();
	
	LocalDateTime asLocalDateTime();
	
	ZonedDateTime asZonedDateTime();
	
	Currency asCurrency();
	
	Locale asLocale();
	
	File asFile();
	
	Path asPath();
	
	URI asURI();
	
	URL asURL();
	
	Pattern asPattern();
	
	InetAddress asInetAddress();
	
	Class<?> asClass();
}

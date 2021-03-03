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
package io.winterframework.mod.web.internal.mock;

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

import io.winterframework.mod.http.base.Parameter;

/**
 * @author jkuhn
 *
 */
public class MockParameter implements Parameter {

	private final String name;
	
	private final String value;
	
	/**
	 * 
	 */
	public MockParameter(String name, String value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getValue() {
		return this.value;
	}

	@Override
	public <T> T as(Class<T> type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T as(Type type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T[] asArrayOf(Class<T> type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T[] asArrayOf(Type type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> List<T> asListOf(Class<T> type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> List<T> asListOf(Type type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> Set<T> asSetOf(Class<T> type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> Set<T> asSetOf(Type type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Byte asByte() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Short asShort() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Integer asInteger() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Long asLong() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Float asFloat() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Double asDouble() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Character asCharacter() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String asString() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Boolean asBoolean() {
		throw new UnsupportedOperationException();
	}

	@Override
	public BigInteger asBigInteger() {
		throw new UnsupportedOperationException();
	}

	@Override
	public BigDecimal asBigDecimal() {
		throw new UnsupportedOperationException();
	}

	@Override
	public LocalDate asLocalDate() {
		throw new UnsupportedOperationException();
	}

	@Override
	public LocalDateTime asLocalDateTime() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ZonedDateTime asZonedDateTime() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Currency asCurrency() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Locale asLocale() {
		throw new UnsupportedOperationException();
	}

	@Override
	public File asFile() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Path asPath() {
		throw new UnsupportedOperationException();
	}

	@Override
	public URI asURI() {
		throw new UnsupportedOperationException();
	}

	@Override
	public URL asURL() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Pattern asPattern() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InetAddress asInetAddress() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Class<?> asClass() {
		throw new UnsupportedOperationException();
	}

}

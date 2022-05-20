/*
 * Copyright 2022 Jeremy Kuhn
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
package io.inverno.mod.ldap.internal;

import io.inverno.mod.base.converter.ObjectDecoder;
import io.inverno.mod.ldap.LDAPAttribute;
import io.inverno.mod.ldap.LDAPException;
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
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;

/**
 * <p>
 * Generic {@link LDAPAttribute} implementation based on the JDK.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericLDAPAttribute implements LDAPAttribute {

	/**
	 * The Object decoder instance.
	 */
	private static final ObjectDecoder OBJECT_DECODER = new ObjectDecoder();
	
	/**
	 * The attribute name.
	 */
	private final String name;
	
	/**
	 * The attribute value.
	 */
	private final Object value;

	/**
	 * <p>
	 * Creates a generic LDAP attribute with the specified name and value.
	 * </p>
	 *
	 * @param name the attribute name
	 * @param name the attribute value
	 */
	public GenericLDAPAttribute(String name, Object value) {
		this.name = name;
		this.value = value;
	}
	
	/**
	 * <p>
	 * Creates a generic Ldap attribute with the specified attribute.
	 * </p>
	 *
	 * @param attribute the underlying attribute
	 * 
	 * @throws LDAPException if there was an error retrieving the value
	 */
	public GenericLDAPAttribute(Attribute attribute) throws LDAPException {
		this.name = attribute.getID();
		try {
			this.value = attribute.get();
		}
		catch (NamingException e) {
			throw new JdkLDAPException(e);
		}
	}
	
	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Object getValue() {
		return this.value;
	}
	
	@Override
	public <T> T as(Class<T> type) {
		return OBJECT_DECODER.decode(this.value, type);
	}

	@Override
	public <T> T as(Type type) {
		return OBJECT_DECODER.decode(this.value, type);
	}

	@Override
	public <T> T[] asArrayOf(Class<T> type) {
		return OBJECT_DECODER.decodeToArray(this.value, type);
	}

	@Override
	public <T> T[] asArrayOf(Type type) {
		return OBJECT_DECODER.decodeToArray(this.value, type);
	}

	@Override
	public <T> List<T> asListOf(Class<T> type) {
		return OBJECT_DECODER.decodeToList(this.value, type);
	}

	@Override
	public <T> List<T> asListOf(Type type) {
		return OBJECT_DECODER.decodeToList(this.value, type);
	}

	@Override
	public <T> Set<T> asSetOf(Class<T> type) {
		return OBJECT_DECODER.decodeToSet(this.value, type);
	}

	@Override
	public <T> Set<T> asSetOf(Type type) {
		return OBJECT_DECODER.decodeToSet(this.value, type);
	}

	@Override
	public Byte asByte() {
		return OBJECT_DECODER.decodeByte(this.value);
	}

	@Override
	public Short asShort() {
		return OBJECT_DECODER.decodeShort(this.value);
	}

	@Override
	public Integer asInteger() {
		return OBJECT_DECODER.decodeInteger(this.value);
	}

	@Override
	public Long asLong() {
		return OBJECT_DECODER.decodeLong(this.value);
	}

	@Override
	public Float asFloat() {
		return OBJECT_DECODER.decodeFloat(this.value);
	}

	@Override
	public Double asDouble() {
		return OBJECT_DECODER.decodeDouble(this.value);
	}

	@Override
	public Character asCharacter() {
		return OBJECT_DECODER.decodeCharacter(this.value);
	}

	@Override
	public String asString() {
		return OBJECT_DECODER.decodeString(this.value);
	}

	@Override
	public Boolean asBoolean() {
		return OBJECT_DECODER.decodeBoolean(this.value);
	}

	@Override
	public BigInteger asBigInteger() {
		return OBJECT_DECODER.decodeBigInteger(this.value);
	}

	@Override
	public BigDecimal asBigDecimal() {
		return OBJECT_DECODER.decodeBigDecimal(this.value);
	}

	@Override
	public LocalDate asLocalDate() {
		return OBJECT_DECODER.decodeLocalDate(this.value);
	}

	@Override
	public LocalDateTime asLocalDateTime() {
		return OBJECT_DECODER.decodeLocalDateTime(this.value);
	}

	@Override
	public ZonedDateTime asZonedDateTime() {
		return OBJECT_DECODER.decodeZonedDateTime(this.value);
	}

	@Override
	public Currency asCurrency() {
		return OBJECT_DECODER.decodeCurrency(this.value);
	}

	@Override
	public Locale asLocale() {
		return OBJECT_DECODER.decodeLocale(this.value);
	}

	@Override
	public File asFile() {
		return OBJECT_DECODER.decodeFile(this.value);
	}

	@Override
	public Path asPath() {
		return OBJECT_DECODER.decodePath(this.value);
	}

	@Override
	public URI asURI() {
		return OBJECT_DECODER.decodeURI(this.value);
	}

	@Override
	public URL asURL() {
		return OBJECT_DECODER.decodeURL(this.value);
	}

	@Override
	public Pattern asPattern() {
		return OBJECT_DECODER.decodePattern(this.value);
	}

	@Override
	public InetAddress asInetAddress() {
		return OBJECT_DECODER.decodeInetAddress(this.value);
	}

	@Override
	public Class<?> asClass() {
		return OBJECT_DECODER.decodeClass(this.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GenericLDAPAttribute other = (GenericLDAPAttribute) obj;
		return Objects.equals(name, other.name) && Objects.equals(value, other.value);
	}
}

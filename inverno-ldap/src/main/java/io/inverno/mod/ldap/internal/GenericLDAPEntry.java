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

import io.inverno.mod.ldap.LDAPAttribute;
import io.inverno.mod.ldap.LDAPEntry;
import io.inverno.mod.ldap.LDAPException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

/**
 * <p>
 * Generic {@link LDAPEntry} implementation based on the JDK.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericLDAPEntry implements LDAPEntry {

	/**
	 * The DN.
	 */
	private final String dn;
	
	/**
	 * The underlying attributes.
	 */
	private final Attributes attributes;
	
	/**
	 * <p>
	 * Creates a generic LDAP entry with the specified DN and attributes.
	 * </p>
	 * 
	 * @param dn         the entry DN
	 * @param attributes the entry attributes
	 */
	public GenericLDAPEntry(String dn, Attributes attributes) {
		this.dn = dn;
		this.attributes = attributes;
	}
	
	/**
	 * <p>
	 * Creates a generic LDAP entry with the specified search result.
	 * </p>
	 * 
	 * @param searchResult a search result
	 */
	public GenericLDAPEntry(SearchResult searchResult) {
		this.dn = searchResult.getNameInNamespace();
		this.attributes = searchResult.getAttributes();
	}

	@Override
	public String getDN() {
		return this.dn;
	}

	@Override
	public Optional<Object> get(String name) throws LDAPException {
		return Optional.ofNullable(this.attributes.get(name))
			.map(attr -> {
				try {
					return attr.get();
				}
				catch(NamingException e) {
					throw new JdkLDAPException(e);
				}
			});
	}

	@Override
	public List<Object> getAll(String name) throws LDAPException  {
		try {
			Attribute attribute = this.attributes.get(name);
			if(attribute != null) {
				List<Object> result = new ArrayList<>();
				NamingEnumeration<?> all = this.attributes.get(name).getAll();
				while(all.hasMore()) {
					result.add(all.next());
				}
				return result;
			}
			return List.of();
		}
		catch(NamingException e) {
			throw new JdkLDAPException(e);
		}
	}

	@Override
	public List<Map.Entry<String, Object>> getAll() throws LDAPException {
		try {
			List<Map.Entry<String, Object>> result = new ArrayList<>();
			NamingEnumeration<? extends Attribute> allAttributes = this.attributes.getAll();
			while(allAttributes.hasMore()) {
				Attribute attribute = allAttributes.next();
				NamingEnumeration<?> all = attribute.getAll();
				while(all.hasMore()) {
					result.add(Map.entry(attribute.getID(), all.next()));
				}
			}
			return result;
		}
		catch(NamingException e) {
			throw new JdkLDAPException(e);
		}
	}

	@Override
	public Optional<LDAPAttribute> getAttribute(String name) throws LDAPException {
		return Optional.ofNullable(this.attributes.get(name))
			.map(GenericLDAPAttribute::new);
	}

	@Override
	public List<LDAPAttribute> getAllAttribute(String name) throws LDAPException {
		try {
			Attribute attribute = this.attributes.get(name);
			if(attribute != null) {
				List<LDAPAttribute> result = new ArrayList<>();
				NamingEnumeration<?> all = this.attributes.get(name).getAll();
				while(all.hasMore()) {
					result.add(new GenericLDAPAttribute(name, all.next()));
				}
				return result;
			}
			return List.of();
		}
		catch(NamingException e) {
			throw new JdkLDAPException(e);
		}
	}

	@Override
	public List<LDAPAttribute> getAllAttribute() throws LDAPException {
		try {
			List<LDAPAttribute> result = new ArrayList<>();
			NamingEnumeration<? extends Attribute> allAttributes = this.attributes.getAll();
			while(allAttributes.hasMore()) {
				Attribute attribute = allAttributes.next();
				NamingEnumeration<?> all = attribute.getAll();
				while(all.hasMore()) {
					result.add(new GenericLDAPAttribute(attribute.getID(), all.next()));
				}
			}
			return result;
		}
		catch(NamingException e) {
			throw new JdkLDAPException(e);
		}
	}
}

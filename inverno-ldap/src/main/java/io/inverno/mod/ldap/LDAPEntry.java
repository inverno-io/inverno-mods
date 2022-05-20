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
package io.inverno.mod.ldap;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <p>I
 * Represents an LDAP entry resulting from an LDAP search operation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface LDAPEntry {

	/**
	 * <p>
	 * Returns the entry DN.
	 * </p>
	 * 
	 * @return a DN
	 */
	String getDN();
	
	/**
	 * <p>
	 * Returns the value of the specified attribute.
	 * </p>
	 * 
	 * <p>
	 * If multiple values are defined for that attribute, this method returns one of them in an non-deterministic way.
	 * </p>
	 * 
	 * @param name the name of the attribute
	 * 
	 * @return an optional returning the attribute value or an empty optional if no attribute exists with that name
	 * 
	 * @throws LDAPException if there was an error accessing the attribute
	 */
	Optional<Object> get(String name) throws LDAPException;

	/**
	 * <p>
	 * Returns all values defined for the specifies attribute.
	 * </p>
	 * 
	 * @param name the name of the attribute
	 * 
	 * @return a list of values or an empty list if no attribute exists with that name
	 * 
	 * @throws LDAPException if there was an error accessing the attribute
	 */
	List<Object> getAll(String name) throws LDAPException;
	
	/**
	 * <p>
	 * Returns all the attributes defined in the entry.
	 * </p>
	 * 
	 * @return a list of entries with attribute name as key and attribute value as value
	 * 
	 * @throws LDAPException if there was an error accessing the attributes
	 */
	List<Map.Entry<String, Object>> getAll() throws LDAPException;
	
	/**
	 * <p>
	 * Returns the specified attribute.
	 * </p>
	 * 
	 * <p>
	 * If multiple values are defined for that attribute, this method returns one of them in an non-deterministic way.
	 * </p>
	 * 
	 * @param name the name of the attribute
	 * 
	 * @return an optional returning the attribute or an empty optional if no attribute exists with that name
	 * 
	 * @throws LDAPException if there was an error accessing the attribute
	 */
	Optional<LDAPAttribute> getAttribute(String name) throws LDAPException;
	
	/**
	 * <p>
	 * Returns all attributes defined for the specifies name.
	 * </p>
	 * 
	 * @param name the name of the attribute
	 * 
	 * @return a list of attributes or an empty list if no attribute exists with that name
	 * 
	 * @throws LDAPException if there was an error accessing the attribute
	 */
	List<LDAPAttribute> getAllAttribute(String name) throws LDAPException;
	
	/**
	 * <p>
	 * Returns all the attributes defined in the entry.
	 * </p>
	 * 
	 * @return a list attributes
	 * 
	 * @throws LDAPException if there was an error accessing the attributes
	 */
	List<LDAPAttribute> getAllAttribute() throws LDAPException;
}

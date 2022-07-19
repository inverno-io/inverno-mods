/*
 * Copyright 2022 Jeremy KUHN
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
import io.inverno.mod.ldap.LDAPClient;
import io.inverno.mod.ldap.LDAPEntry;
import io.inverno.mod.ldap.LDAPOperations;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class Readme {
	
	public void doc() {
		
		LDAPClient client = null;
		
		LDAPEntry jsmithEntry = client.get("cn=jsmith,ou=users,dc=inverno,dc=io").block();
		
		
		jsmithEntry = client.get("cn={0},ou=users,dc=inverno,dc=io", "jsmith").block();
		
		jsmithEntry = client.get("cn={0},ou=users,dc=inverno,dc=io", new String[] {"cn", "uid", "mail", "userPassword"}, "jsmith").block();
		
		// Gets the value of attribute 'mail' or null
		Object mail = jsmithEntry.get("mail").orElse(null);
		
		// Gets all values for attribute 'mail' or an empty list
		List<Object> allMail = jsmithEntry.getAll("mail");
		
		// Get all attributes 
		List<Map.Entry<String, Object>> all = jsmithEntry.getAll();
		
		// Gets the value of attribute 'birthDate' as a local date or null
		LocalDate birthDate = jsmithEntry.getAttribute("birthDate").map(LDAPAttribute::asLocalDate).orElse(null);
		
		// Gets all values for attribute 'address' as strings or an empty list
		List<String> addresses = jsmithEntry.getAllAttribute("address").stream().map(LDAPAttribute::asString).collect(Collectors.toList());
		
		// Get all attributes
		List<LDAPAttribute> allAttribute = jsmithEntry.getAllAttribute();
		
		client.search("dc=inverno,dc=io", new String[]{ "cn" }, "(&(objectClass=groupOfNames)(member={0}))", "cn=jsmith,ou=users,dc=inverno,dc=io").collectList().block();
		
		client.search().scope(LDAPOperations.SearchScope.WHOLE_SUBTREE).build("ou=users,dc=inverno,dc=io", new String[] {"cn", "uid"}, "(objectClass=inetOrgPerson)");
		
	}
	
}

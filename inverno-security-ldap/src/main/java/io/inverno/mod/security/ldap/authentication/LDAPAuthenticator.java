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
package io.inverno.mod.security.ldap.authentication;

import io.inverno.mod.ldap.LDAPAttribute;
import io.inverno.mod.ldap.LDAPClient;
import io.inverno.mod.ldap.LDAPException;
import io.inverno.mod.security.authentication.AuthenticationException;
import io.inverno.mod.security.authentication.Authenticator;
import io.inverno.mod.security.authentication.InvalidCredentialsException;
import io.inverno.mod.security.authentication.UserCredentials;
import io.inverno.mod.security.ldap.internal.authentication.GenericLDAPAuthentication;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

/**
 * <p>
 * General LDAP authenticator.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class LDAPAuthenticator implements Authenticator<UserCredentials, LDAPAuthentication> {

	/**
	 * Default bind DN format.
	 */
	public static final String DEFAULT_BIND_NAME_FORMAT = "cn={0},ou=users";
	
	/**
	 * Default search group filter.
	 */
	public static final String DEFAULT_SEARCH_GROUP_FILTER = "(&(objectClass=groupOfNames)(member={0}))";
	
	/**
	 * The underlying LDAP client.
	 */
	private final LDAPClient ldapClient;
	
	/**
	 * The base DN where to search for groups.
	 */
	private final String base;
	
	/**
	 * The search group filter.
	 */
	private final String searchGroupFilter;
	
	/**
	 * The bind DN format.
	 */
	private final String bindNameFormat;
	
	/**
	 * <p>
	 * Creates an LDAP authenticator with the specified LDAP client and base DN.
	 * </p>
	 * 
	 * @param ldapClient the LDAP client
	 * @param base       the base DN where to search for groups
	 */
	public LDAPAuthenticator(LDAPClient ldapClient, String base) {
		this(ldapClient, base, DEFAULT_SEARCH_GROUP_FILTER, StringUtils.isBlank(base) ? DEFAULT_BIND_NAME_FORMAT : DEFAULT_BIND_NAME_FORMAT + "," + base);
	}
	
	/**
	 * <p>
	 * Creates an LDAP authenticator with the specified LDAP client, base DN and search group filter.
	 * </p>
	 * 
	 * <p>
	 * The search group filter is parameterized and is formatted using the user's DN and the uid.
	 * </p>
	 * 
	 * @param ldapClient        the LDAP client
	 * @param base              the base DN where to search for groups
	 * @param searchGroupFilter the search group filter
	 */
	public LDAPAuthenticator(LDAPClient ldapClient, String base, String searchGroupFilter) {
		this(ldapClient, base, searchGroupFilter, StringUtils.isBlank(base) ? DEFAULT_BIND_NAME_FORMAT : DEFAULT_BIND_NAME_FORMAT + "," + base);
	}
	
	/**
	 * <p>
	 * Creates an LDAP authenticator with the specified LDAP client, base DN and search group filter.
	 * </p>
	 * 
	 * <p>
	 * The search group filter is formatted using the user's DN and the uid.
	 * </p>
	 * 
	 * <p>
	 * The bind name formatted using the uid.
	 * </p>
	 * 
	 * @param ldapClient        the LDAP client
	 * @param base              the base DN where to search for groups
	 * @param searchGroupFilter the search group filter
	 * @param bindNameFormat    the bind DN format
	 */
	public LDAPAuthenticator(LDAPClient ldapClient, String base, String searchGroupFilter, String bindNameFormat) {
		this.ldapClient = ldapClient;
		this.base = base;
		this.searchGroupFilter = searchGroupFilter;
		this.bindNameFormat = bindNameFormat;
	}
	
	/**
	 * <p>
	 * Returns the base DN where to search for groups.
	 * </p>
	 * 
	 * @return the base DN
	 */
	public String getBase() {
		return base;
	}
	
	/**
	 * <p>
	 * Returns the bind DN format used when binding a user.
	 * </p>
	 * 
	 * @return the bind DN format
	 */
	public String getBindNameFormat() {
		return bindNameFormat;
	}
	
	/**
	 * <p>
	 * Returns the search group filter.
	 * </p>
	 * 
	 * @return a filter
	 */
	public String getSearchGroupFilter() {
		return searchGroupFilter;
	}

	@Override
	public Mono<LDAPAuthentication> authenticate(UserCredentials credentials) throws AuthenticationException {
		return Mono.from(this.ldapClient.bind(
			this.bindNameFormat,
			new Object[] { credentials.getUsername() },
			credentials.getPassword(), 
			ops -> ops.search(this.base, new String[]{ "cn" }, this.searchGroupFilter, ops.getBoundDN(), credentials.getUsername())
				.map(groupEntry -> groupEntry.getAttribute("cn").map(LDAPAttribute::asString).get())
				.collect(Collectors.toSet())
				.map(groups -> (LDAPAuthentication)new GenericLDAPAuthentication(credentials.getUsername(), ops.getBoundDN().orElse(null), groups, true))
			))
			.onErrorMap(this::mapError);
	}
	
	/**
	 * <p>
	 * Maps the error to an authentication exception.
	 * </p>
	 * 
	 * @param t an error
	 * 
	 * @return an authentication exception
	 */
	private AuthenticationException mapError(Throwable t) {
		if(t instanceof LDAPException) {
			LDAPException e = (LDAPException)t;
			if(e.getErrorCode() != null) {
				String message = "LDAP " + (e.getErrorCode() != null ? " (" + ((LDAPException)e).getErrorCode() + "): " : ": ") + (e.getErrorDescription() != null ? ((LDAPException)e).getErrorDescription() : "");
				switch(e.getErrorCode()) {
					case LDAPClient.CODE_INVALID_CREDENTIALS: 
						return new InvalidCredentialsException(message, e);
					default: 
						return new AuthenticationException(e.getErrorDescription(), e);
				}
			}
			else if(e.getErrorDescription() != null) {
				return new AuthenticationException(e.getErrorDescription(), e);
			}
			else {
				return new AuthenticationException(e.getMessage(), e);
			}
		}
		return new AuthenticationException(t.getMessage(), t);
	}
}

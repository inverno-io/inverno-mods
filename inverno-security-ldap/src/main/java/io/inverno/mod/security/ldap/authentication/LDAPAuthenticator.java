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
import io.inverno.mod.security.authentication.LoginCredentials;
import io.inverno.mod.security.authentication.password.RawPassword;
import io.inverno.mod.security.ldap.internal.authentication.GenericLDAPAuthentication;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic LDAP authenticator.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class LDAPAuthenticator implements Authenticator<LoginCredentials, LDAPAuthentication> {

	private static final Logger LOGGER = LogManager.getLogger(LDAPAuthenticator.class);
	
	/**
	 * Default bind DN format.
	 */
	public static final String DEFAULT_USER_NAME_FORMAT = "cn={0},ou=users";
	
	/**
	 * Default search group filter.
	 */
	public static final String DEFAULT_SEARCH_GROUP_FILTER = "(&(objectClass=groupOfNames)(member={0}))";
	
	/**
	 * The default search user filter. 
	 */
	public static final String DEFAULT_SEARCH_USER_FILTER = "(&(objectClass=inetOrgPerson)(uid={0}))";
	
	/**
	 * The user password attribute as defined by <a href="https://datatracker.ietf.org/doc/html/rfc2256#section-5.36">RFC2256 Section 5.36</a>.
	 */
	public static final String ATTRIBUTE_PASSWORD = "userPassword";
	
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
	 * The search user filter.
	 */
	private final String searchUserFilter;
	
	/**
	 * The user DN format.
	 */
	private final String usernameFormat;
	
	/**
	 * The password attribute name for password comparison.
	 */
	private final String passwordAttribute;
	
	/**
	 * The password comparison filter to search for user entries by matching username and password.
	 */
	private final String searchPasswordComparisonFilter;
	
	/**
	 * Indicates whether an empty Mono should be returned on {@link AuthenticationException}.
	 */
	private boolean terminal;
	
	/**
	 * <p>
	 * Creates an LDAP authenticator with the specified LDAP client and base DN.
	 * </p>
	 * 
	 * <p>
	 * The resulting authenticator is terminal and returns denied authentication on failed authentication.
	 * </p>
	 * 
	 * @param ldapClient the LDAP client
	 * @param base       the base DN where to search for groups
	 */
	public LDAPAuthenticator(LDAPClient ldapClient, String base) {
		this(ldapClient, base, DEFAULT_SEARCH_GROUP_FILTER, StringUtils.isBlank(base) ? DEFAULT_USER_NAME_FORMAT : DEFAULT_USER_NAME_FORMAT + "," + base, DEFAULT_SEARCH_USER_FILTER, ATTRIBUTE_PASSWORD);
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
	 * <p>
	 * The resulting authenticator is terminal and returns denied authentication on failed authentication.
	 * </p>
	 * 
	 * @param ldapClient        the LDAP client
	 * @param base              the base DN where to search for groups
	 * @param searchGroupFilter the search group filter
	 */
	public LDAPAuthenticator(LDAPClient ldapClient, String base, String searchGroupFilter) {
		this(ldapClient, base, searchGroupFilter, StringUtils.isBlank(base) ? DEFAULT_USER_NAME_FORMAT : DEFAULT_USER_NAME_FORMAT + "," + base, DEFAULT_SEARCH_USER_FILTER, ATTRIBUTE_PASSWORD);
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
	 * <p>
	 * The resulting authenticator is terminal and returns denied authentication on failed authentication.
	 * </p>
	 * 
	 * @param ldapClient        the LDAP client
	 * @param base              the base DN where to search for groups
	 * @param searchGroupFilter the search group filter
	 * @param usernameFormat    the user DN format
	 */
	public LDAPAuthenticator(LDAPClient ldapClient, String base, String searchGroupFilter, String usernameFormat) {
		this(ldapClient, base, searchGroupFilter, usernameFormat, DEFAULT_SEARCH_USER_FILTER, ATTRIBUTE_PASSWORD);
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
	 * <p>
	 * The resulting authenticator is terminal and returns denied authentication on failed authentication.
	 * </p>
	 * 
	 * @param ldapClient        the LDAP client
	 * @param base              the base DN where to search for groups
	 * @param searchGroupFilter the search group filter
	 * @param usernameFormat    the user DN format
	 * @param searchUserFilter  the search user filter
	 */
	public LDAPAuthenticator(LDAPClient ldapClient, String base, String searchGroupFilter, String usernameFormat, String searchUserFilter) {
		this(ldapClient, base, searchGroupFilter, usernameFormat, searchUserFilter, ATTRIBUTE_PASSWORD);
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
	 * <p>
	 * The resulting authenticator is terminal and returns denied authentication on failed authentication.
	 * </p>
	 * 
	 * @param ldapClient        the LDAP client
	 * @param base              the base DN where to search for groups
	 * @param searchGroupFilter the search group filter
	 * @param searchUserFilter  the search user filter
	 * @param usernameFormat    the user DN format
	 * @param passwordAttribute the id of the password attribute
	 */
	public LDAPAuthenticator(LDAPClient ldapClient, String base, String searchGroupFilter, String usernameFormat, String searchUserFilter, String passwordAttribute) {
		this.ldapClient = ldapClient;
		this.base = base;
		this.searchGroupFilter = searchGroupFilter;
		this.usernameFormat = usernameFormat;
		this.searchUserFilter = searchUserFilter;
		this.passwordAttribute = passwordAttribute;
		this.searchPasswordComparisonFilter = "(&" + this.searchUserFilter + "(" + this.passwordAttribute + "={1}))";
		this.terminal = true;
	}
	
	/**
	 * <p>
	 * Sets whether the authenticator is terminal and should return denied authentication on failed authentication or no authentication to indicate it was not able to authenticate credentials.
	 * </p>
	 * 
	 * @param terminal true to terminate authentication, false otherwise
	 */
	public void setTerminal(boolean terminal) {
		this.terminal = terminal;
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
	 * Returns the user DN format used when binding a user or fetching a user entry.
	 * </p>
	 * 
	 * @return a user DN format
	 */
	public String getUsernameFormat() {
		return usernameFormat;
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

	/**
	 * <p>
	 * Returns the search user filter.
	 * </p>
	 * 
	 * @return a filter
	 */
	public String getSearchUserFilter() {
		return searchUserFilter;
	}

	/**
	 * <p>
	 * Returns the name of the password attribute.
	 * </p>
	 * 
	 * @return an attribute name
	 */
	public String getPasswordAttribute() {
		return passwordAttribute;
	}

	@Override
	public Mono<LDAPAuthentication> authenticate(LoginCredentials credentials) {
		Mono<LDAPAuthentication> authentication;
		if(credentials.getPassword() instanceof RawPassword) {
			// If password is raw we can do a bind operation to authenticate
			// This is normally the case for credentials extracted in a basic or form authentication
			authentication = Mono.from(this.ldapClient.bind(this.usernameFormat,
				new Object[] { credentials.getUsername() },
				credentials.getPassword().getValue(), 
				ops -> ops.search(this.base, new String[]{ "cn" }, this.searchGroupFilter, ops.getBoundDN().orElseThrow(() -> new IllegalStateException("Bound user has no DN")), credentials.getUsername())
					.map(groupEntry -> groupEntry.getAttribute("cn").map(LDAPAttribute::asString).get())
					.collect(Collectors.toSet())
					.map(groups -> (LDAPAuthentication)new GenericLDAPAuthentication(credentials.getUsername(), ops.getBoundDN().orElse(null), groups, true))
				));
		}
		else {
			// This can happen when we actually do not have access to the raw password (digest authentication...)
			// we then need to compare the password from the credentials to the password in the user LDAP entry which is expected to be encoded using the same password encoder
			authentication = this.ldapClient.get(this.usernameFormat, new String[] {this.passwordAttribute}, credentials.getUsername())
				.flatMap(userEntry -> {
					Optional<String> passwordAttributeOpt = userEntry.getAttribute(this.passwordAttribute).map(LDAPAttribute::asString);
					if(passwordAttributeOpt.isPresent()) {
						// The encoded password attribute has been retrieved
						if(credentials.getPassword().getValue().equals(passwordAttributeOpt.get())) {
							// it matches the encoded password from user credentials
							return Mono.just(userEntry);
						}
						return Mono.empty();
					}
					else {
						// We were not able to resolve the password attribute, we must compare encoded passwords in LDAP server
						return this.ldapClient.search(this.base, this.searchPasswordComparisonFilter, credentials.getUsername(), credentials.getPassword().getValue()).single();
					}
				})
				.flatMap(userEntry -> this.ldapClient.search(this.base, new String[]{ "cn" }, this.searchGroupFilter, userEntry.getDN(), credentials.getUsername())
					.map(groupEntry -> groupEntry.getAttribute("cn").map(LDAPAttribute::asString).get())
					.collect(Collectors.toSet())
					.map(groups -> (LDAPAuthentication)new GenericLDAPAuthentication(credentials.getUsername(), userEntry.getDN(), groups, true))
				);
		}
		
		return authentication.onErrorMap(LDAPException.class, e -> {
			if(e.getErrorCode() != null) {
				String message = "LDAP " + (e.getErrorCode() != null ? " (" + e.getErrorCode() + "): " : ": ") + (e.getErrorDescription() != null ? e.getErrorDescription() : "");
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
		})
		.onErrorResume(AuthenticationException.class, e -> {
			if(!terminal) {
				LOGGER.error("Failed to authenticate", e);
				return Mono.empty();
			}
			return Mono.fromSupplier(() -> {
				GenericLDAPAuthentication ldapAuthentication = new GenericLDAPAuthentication(credentials.getUsername(), null, Set.of(), false);
				ldapAuthentication.setCause(e);
				return ldapAuthentication;
			});
		});
	}
}

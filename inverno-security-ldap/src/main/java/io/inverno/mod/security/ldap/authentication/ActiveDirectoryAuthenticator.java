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
import java.util.Arrays;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Specialized LDAP authenticator that uses Active Directory semantic.
 * </p>
 * 
 * <p>
 * The {@code domain} is appended to the username before authentication unless it already provides the domain. If not specified, it is assumed that the username provides the domain.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class ActiveDirectoryAuthenticator implements Authenticator<LoginCredentials, LDAPAuthentication> {

	/**
	 * The pattern used to extract error code from Active Directory error messages.
	 */
	private static final Pattern HEX_ERROR_CODE_PATTERN = Pattern.compile(".*data\\s([0-9a-f]{3,4}).*");
	
	/**
	 * The default search user filter.
	 */
	public static final String DEFAULT_SEARCH_USER_FILTER = "(&(objectClass=user)(userPrincipalName={0}))";
	
	/**
	 * The underlying LDAP client.
	 */
	private final LDAPClient ldapClient;
	
	/**
	 * The Active Directory domain.
	 */
	private final String domain;
	
	/**
	 * The base DN where to search for users.
	 */
	private final String base;
	
	/**
	 * The search user filter.
	 */
	private final String searchUserFilter;
	
	/*
	 * https://ldapwiki.com/wiki/Common%20Active%20Directory%20Bind%20Errors
	 */
	/**
	 * Entry does not exist.
	 */
	private static final int CODE_LDAP_NO_SUCH_OBJECT = 0x525;

	/**
	 * Returns when username is valid but password/credential is invalid.
	 */
	private static final int CODE_LOGON_FAILURE = 0x52e;
	
	/**
	 * Account Restrictions are preventing this user from signing in. 
	 */
	private static final int CODE_ACCOUNT_RESTRICTION = 0x52f;

	/**
	 * Time Restriction:Entry logon time restriction violation
	 */
	private static final int CODE_INVALID_LOGON_HOURS = 0x530;
	
	/**
	 * Device Restriction:Entry not allowed to log on to this computer.
	 */
	private static final int CODE_INVALID_WORKSTATION = 0x531;

	/**
	 * Password Expiration: Entry password has expired LDAP User-Account-Control Attribute - ERROR_PASSWORD_EXPIRED
	 */
	private static final int CODE_PASSWORD_EXPIRED = 0x532;

	/**
	 * Administratively Disabled: LDAP User-Account-Control Attribute - ACCOUNTDISABLE
	 */
	private static final int CODE_ACCOUNT_DISABLED = 0x533;
	
	/**
	 * During a logon attempt, the user's security context accumulated too many security Identifiers. (ie Group-AD)
	 */
	private static final int CODE_TOO_MANY_CONTEXT_IDS = 0x568;

	/**
	 * 	LDAP Password Expiration: User-Account-Control Attribute - ACCOUNTEXPIRED
	 */
	private static final int CODE_ACCOUNT_EXPIRED = 0x701;

	/**
	 * Password Expiration: Entry's password must be changed before logging on LDAP pwdLastSet: value of 0 indicates admin-required password change - MUST_CHANGE_PASSWD
	 */
	private static final int CODE_PASSWORD_MUST_CHANGE = 0x773;

	/**
	 * Intruder Detection:Entry is currently locked out and may not be logged on to LDAP User-Account-Control Attribute - LOCKOUT
	 */
	private static final int CODE_ACCOUNT_LOCKED_OUT = 0x775;
	
	/**
	 * <p>
	 * Creates an Active Directory authenticator with the specified LDAP client and domain.
	 * </p>
	 * 
	 * @param ldapClient the LDAP client
	 * @param domain     the domain
	 */
	public ActiveDirectoryAuthenticator(LDAPClient ldapClient, String domain) {
		this(ldapClient, domain, null, DEFAULT_SEARCH_USER_FILTER);
	}
	
	/**
	 * <p>
	 * Creates an Active Directory authenticator with the specified LDAP client, domain and base DN.
	 * </p>
	 * 
	 * @param ldapClient the LDAP client
	 * @param domain     the domain
	 * @param base       the base DN
	 */
	public ActiveDirectoryAuthenticator(LDAPClient ldapClient, String domain, String base) {
		this(ldapClient, domain, base, DEFAULT_SEARCH_USER_FILTER);
	}
	
	/**
	 * <p>
	 * Creates an Active Directory authenticator with the specified LDAP client, domain, base DN and search user filter.
	 * </p>
	 * 
	 * @param ldapClient       the LDAP client
	 * @param domain           the domain
	 * @param base             the base DN
	 * @param searchUserFilter the search user filter
	 */
	public ActiveDirectoryAuthenticator(LDAPClient ldapClient, String domain, String base, String searchUserFilter) {
		this.ldapClient = ldapClient;
		this.domain = domain;
		this.base = base;
		this.searchUserFilter = searchUserFilter;
	}

	/**
	 * <p>
	 * Returns the domain.
	 * </p>
	 * 
	 * @return the domain
	 */
	public String getDomain() {
		return domain;
	}
	
	/**
	 * <p>
	 * Returns the base DN where to search for users.
	 * </p>
	 * 
	 * @return the base DN
	 */
	public String getBase() {
		return base;
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
	
	@Override
	public Mono<LDAPAuthentication> authenticate(LoginCredentials credentials) throws AuthenticationException {
		if(credentials.getPassword() instanceof RawPassword) {
			String boundDN = this.getBindDN(credentials.getUsername(), this.domain);
			String baseDN = this.base != null ? this.base : this.boundDNToBaseDN(boundDN);
			return Mono.from(this.ldapClient.bind(
				boundDN,
				credentials.getPassword().getValue(), 
				ops -> ops.search(baseDN, this.searchUserFilter, boundDN, credentials.getUsername())
					.single()
					.mapNotNull(userEntry -> userEntry.getAllAttribute("memberOf"))
					.flatMapIterable(Function.identity())
					.map(LDAPAttribute::asString)
					.collect(Collectors.toSet())
					.map(groups -> (LDAPAuthentication)new GenericLDAPAuthentication(credentials.getUsername(), ops.getBoundDN().orElse(null), groups, true))
				))
				.onErrorMap(this::mapError);
		}
		else {
			throw new AuthenticationException("Unsupported password type: " + credentials.getPassword().getClass().getCanonicalName());
		}
	}

	/**
	 * <p>
	 * Resolves the bind operation DN from the specified username and domain.
	 * </p>
	 * 
	 * <p>
	 * This method basically appends the domain to the username if it doesn't already provides a domain to obtain an Active Directory login name of the form: {@code <username>@<domain>}.
	 * </p>
	 * 
	 * @param username the name of the user to authenticate
	 * @param domain   the domain
	 * 
	 * @return bind operation DN
	 */
	protected String getBindDN(String username, String domain) {
		if (domain == null || username.toLowerCase().endsWith("@" + domain)) {
			return username;
		}
		return username + "@" + this.domain;
	}
	
	/**
	 * <p>
	 * Extracts the base DN from bind operation DN.
	 * </p>
	 * 
	 * <p>
	 * This basically converts the domain to a valid LDAP DN (e.g. {@code example.com} to {@code dc=example,dc=com}).
	 * </p>
	 * 
	 * @param boundDN the bind operation DN
	 * 
	 * @return a base DN
	 * 
	 * @throws InvalidCredentialsException if the bind operation DN is invalid (i.e. it does not provides a domain)
	 */
	protected String boundDNToBaseDN(String boundDN) throws InvalidCredentialsException {
		int domainIndex = boundDN.lastIndexOf('@');
		if (domainIndex < 0) {
			throw new InvalidCredentialsException("Missing domain");
		}
		return Arrays.stream(boundDN.substring(domainIndex + 1).split("\\."))
			.map(s -> "dc=" + s)
			.collect(Collectors.joining(","));
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
				String message = "LDAP " + (((LDAPException)e).getErrorCode() != null ? " (" + ((LDAPException)e).getErrorCode() + "): " : ": ") + this.mapErrorDescription(e.getErrorDescription());
				switch(e.getErrorCode()) {
					case LDAPClient.CODE_INVALID_CREDENTIALS: 
						return new InvalidCredentialsException(message, e);
					default: 
						return new AuthenticationException(e.getErrorDescription(), e);
				}
			}
			else if(e.getErrorDescription() != null) {
				return new AuthenticationException(this.mapErrorDescription(e.getErrorDescription()), e);
			}
			else {
				return new AuthenticationException(e.getMessage(), e);
			}
		}
		return new AuthenticationException(t.getMessage(), t);
	}
	
	/**
	 * <p>
	 * Maps an LDAP error description to Active Directory error message.
	 * </p>
	 * 
	 * <p>
	 * This methods parses Active Directory error message to extract an error code and returns the corresponding error message.
	 * </p>
	 * 
	 * <p>
	 * <a href="https://ldapwiki.com/wiki/Common%20Active%20Directory%20Bind%20Errors">Microsoft Active Directory LDAP Result Codes sub-codes for Bind Response</a>
	 * </p>
	 * 
	 * @param errorDescription an LDAP error description (i.e. the actual error message returned by the Active Directory server)
	 * 
	 * @return an error message
	 */
	private String mapErrorDescription(String errorDescription) {
		if(errorDescription == null) {
			return "";
		}
		Matcher matcher = HEX_ERROR_CODE_PATTERN.matcher(errorDescription);
		if (matcher.matches()) {
			int errorCode = Integer.parseInt(matcher.group(1), 16);
			switch (errorCode) {
				case CODE_LDAP_NO_SUCH_OBJECT:
					return "User not found";
				case CODE_LOGON_FAILURE:
					return "Invalid password";
				case CODE_ACCOUNT_RESTRICTION:
					return "Account restriction";
				case CODE_INVALID_LOGON_HOURS:
					return "Invalid login hours";
				case CODE_INVALID_WORKSTATION:
					return "Invalid workstation";
				case CODE_PASSWORD_EXPIRED:
					return "Password expired";
				case CODE_ACCOUNT_DISABLED:
					return "Account disabled";
				case CODE_TOO_MANY_CONTEXT_IDS:
					return "Too many security identifiers";
				case CODE_ACCOUNT_EXPIRED:
					return "Account expired";
				case CODE_PASSWORD_MUST_CHANGE:
					return "Password must be changed";
				case CODE_ACCOUNT_LOCKED_OUT:
					return "Account locked";
				default:
					return errorDescription;
			}
		}
		return errorDescription;
	}
}

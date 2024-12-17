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
package io.inverno.mod.security.ldap.identity;

import io.inverno.mod.ldap.LDAPAttribute;
import io.inverno.mod.ldap.LDAPClient;
import io.inverno.mod.security.identity.IdentityException;
import io.inverno.mod.security.identity.IdentityResolver;
import io.inverno.mod.security.ldap.authentication.LDAPAuthentication;
import io.inverno.mod.security.ldap.internal.identity.GenericLDAPIdentity;
import java.util.List;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * <p>
 * LDAP identity resolver used to resolve the identity of an authenticated user from an LDAP server.
 * </p>
 * 
 * <p>
 * It searches user entries in an LDAP server based on the DN and uid provided by the LDAP authentication. The attributes to retrieve can be specified and the resulting {@link LDAPIdentity} shall only
 * contains these ones. By default it resolves: uid, sn, cn, givenName, displayName, mail, title, telephoneNumber, mobile and jpegPhoto.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class LDAPIdentityResolver implements IdentityResolver<LDAPAuthentication, LDAPIdentity> {

	/**
	 * The default search user filter. 
	 */
	public static final String DEFAULT_SEARCH_USER_FILTER = "(&(objectClass=inetOrgPerson)(uid={0}))";
	
	/**
	 * The default attributes to resolve.
	 */
	public static final String[] DEFAULT_SEARCH_ATTRIBUTES = new String[] {
		LDAPIdentity.ATTRIBUTE_UID,
		LDAPIdentity.ATTRIBUTE_SN,
		LDAPIdentity.ATTRIBUTE_CN,
		LDAPIdentity.ATTRIBUTE_GIVEN_NAME,
		LDAPIdentity.ATTRIBUTE_DISPLAY_NAME,
		LDAPIdentity.ATTRIBUTE_MAIL,
		LDAPIdentity.ATTRIBUTE_TITLE,
		LDAPIdentity.ATTRIBUTE_TELEPHONE_NUMBER,
		LDAPIdentity.ATTRIBUTE_MOBILE,
		LDAPIdentity.ATTRIBUTE_JPEG_PHOTO,
	};
	
	/**
	 * The underlying LDAP client.
	 */
	private final LDAPClient ldapClient;
	
	/**
	 * The search user filter.
	 */
	private final String searchUserFilter;
	
	/**
	 * The attributes to resolve.
	 */
	private final String[] attributes;
	
	/**
	 * <p>
	 * Creates an LDAP identity resolver.
	 * </p>
	 * 
	 * @param ldapClient the LDAP client
	 */
	public LDAPIdentityResolver(LDAPClient ldapClient) {
		this(ldapClient, DEFAULT_SEARCH_USER_FILTER, DEFAULT_SEARCH_ATTRIBUTES);
	}
	
	/**
	 * <p>
	 * Creates an LDAP identity resolver that resolves the specified attributes.
	 * </p>
	 * 
	 * @param ldapClient the LDAP client
	 * @param attributes the attributes to resolve
	 */
	public LDAPIdentityResolver(LDAPClient ldapClient, String... attributes) {
		this(ldapClient, DEFAULT_SEARCH_USER_FILTER, attributes);
	}

	/**
	 * <p>
	 * Creates an LDAP identity resolver with the specified search user filter.
	 * </p>
	 * 
	 * @param ldapClient       the LDAP client
	 * @param searchUserFilter a filter
	 */
	public LDAPIdentityResolver(LDAPClient ldapClient, String searchUserFilter) {
		this(ldapClient, searchUserFilter, DEFAULT_SEARCH_ATTRIBUTES);
	}

	/**
	 * <p>
	 * Creates an LDAP identity resolver with the specified search user filter that resolves the specified attributes.
	 * </p>
	 * 
	 * @param ldapClient       the LDAP client
	 * @param searchUserFilter a filter
	 * @param attributes       the attributes to resolve
	 */
	public LDAPIdentityResolver(LDAPClient ldapClient, String searchUserFilter, String... attributes) {
		this.ldapClient = ldapClient;
		this.searchUserFilter = searchUserFilter;
		this.attributes = attributes;
	}
	
	/**
	 * <p>
	 * Returns the attributes resolved by the resolver. 
	 * </p>
	 * 
	 * @return the attributes to resolve
	 */
	public String[] getAttributes() {
		return attributes;
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
	public Mono<LDAPIdentity> resolveIdentity(LDAPAuthentication authentication) throws IdentityException {
		return this.ldapClient.search(authentication.getDN(), this.attributes, DEFAULT_SEARCH_USER_FILTER, authentication.getUsername())
			.single()
			.map(userEntry -> {
				String uid = userEntry.getAttribute(LDAPIdentity.ATTRIBUTE_UID)
					.map(LDAPAttribute::asString)
					.orElseThrow(() -> new IdentityException("Missing uid attribute"));
				
				List<String> sn = userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_SN).stream().map(LDAPAttribute::asString).collect(Collectors.toList());
				List<String> cn = userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_CN).stream().map(LDAPAttribute::asString).collect(Collectors.toList());
				
				GenericLDAPIdentity ldapIdentity = new GenericLDAPIdentity(uid, sn, cn);
				
				ldapIdentity.setTelephoneNumber(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_TELEPHONE_NUMBER).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setDescription(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_DESCRIPTION).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setTitle(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_TITLE).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setRegisteredAddress(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_REGISTERED_ADDRESS).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setDestinationIndicator(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_DESTINATION_INDICATOR).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setPreferredDeliveryMethod(userEntry.getAttribute(LDAPIdentity.ATTRIBUTE_PREFERRED_DELIVERY_METHOD).map(LDAPAttribute::asString).orElse(null));
				ldapIdentity.setFacsimileTelephoneNumber(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_FACSIMILE_TELEPHONE_NUMBER).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setStreet(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_STREET).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setPostOfficeBox(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_POST_OFFICE_BOX).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setPostalCode(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_POSTAL_CODE).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setPostalAddress(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_POSTAL_ADDRESS).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setPhysicalDeliveryOfficeName(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_PHYSICAL_DELIVERY_OFFICE_NAME).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setOrganizationalUnit(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_OU).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setState(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_ST).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setLocalityName(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_L).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setCarLicense(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_CAR_LICENSE).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setDepartmentNumber(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_DEPARTMENT_NUMBER).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setDisplayName(userEntry.getAttribute(LDAPIdentity.ATTRIBUTE_DISPLAY_NAME).map(LDAPAttribute::asString).orElse(null));
				ldapIdentity.setEmployeeNumber(userEntry.getAttribute(LDAPIdentity.ATTRIBUTE_EMPLOYEE_NUMBER).map(LDAPAttribute::asString).orElse(null));
				ldapIdentity.setEmployeeType(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_EMPLOYEE_TYPE).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setGivenName(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_GIVEN_NAME).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setHomePhone(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_HOME_PHONE).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setHomePostalAddress(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_HOME_POSTAL_ADDRESS).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setInitials(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_INITIALS).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setJpegPhoto(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_JPEG_PHOTO).stream().map(attribute -> attribute.as(byte[].class)).collect(Collectors.toList()));
				ldapIdentity.setLabeledURI(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_LABELED_URI).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setMail(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_MAIL).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setManager(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_MANAGER).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setMobile(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_MOBILE).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setOrganizationName(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_O).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setRoomNumber(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_ROOM_NUMBER).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setSecretary(userEntry.getAllAttribute(LDAPIdentity.ATTRIBUTE_SECRETARY).stream().map(LDAPAttribute::asString).collect(Collectors.toList()));
				ldapIdentity.setUserCertificate(userEntry.getAttribute(LDAPIdentity.ATTRIBUTE_USER_CERTIFICATE).map(attribute -> attribute.as(byte[].class)).orElse(null));
				ldapIdentity.setPreferredLanguage(userEntry.getAttribute(LDAPIdentity.ATTRIBUTE_PREFERRED_LANGUAGE).map(LDAPAttribute::asString).orElse(null));
				
				return ldapIdentity;
			});
	}
}

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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.inverno.mod.security.identity.Identity;
import java.util.List;

/**
 * <p>
 * Represents the identity of a user in an LDAP server.
 * </p>
 * 
 * <p>
 * An LDAP user is typically represented as a {@code person}, an {@code organizationalPerson} or a {@code inetOrgPerson} as defined by 
 * <a href="https://datatracker.ietf.org/doc/html/rfc2256">RFC 2256</a> and <a href="https://datatracker.ietf.org/doc/html/rfc2798">RFC2798</a>.
 * </p>
 * 
 * <p>
 * The LDAP identity compiles these attributes to represent the full identity of a user previously authenticated on an LDAP server.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@JsonInclude( JsonInclude.Include.NON_EMPTY )
public interface LDAPIdentity extends Identity {
	
	/**
	 * <p>
	 * Strings for the family names of a person.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4519#section-2.32">RFC4519 Section 2.32</a>
	 */
	String ATTRIBUTE_SN = "sn";
	/**
	 * <p>
	 * Names of an object.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4519#section-2.3">RFC4519 Section 2.3</a>
	 */
	String ATTRIBUTE_CN = "cn";
	/**
	 * <p>
	 * Telephone numbers that comply with the ITU Recommendation E.123.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4519#section-2.35">RFC4519 Section 2.35</a>
	 */
	String ATTRIBUTE_TELEPHONE_NUMBER = "telephoneNumber";
	/**
	 * <p>
	 * Human-readable descriptive phrases about the object.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4519#section-2.5">RFC4519 Section 2.5</a>
	 */
	String ATTRIBUTE_DESCRIPTION = "description";
	/**
	 * <p>
	 * Title of a person in their organizational context.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4519#section-2.38">RFC4519 Section 2.38</a>
	 */
	String ATTRIBUTE_TITLE = "title";
	/**
	 * <p>
	 * Postal addresses suitable for reception of telegrams or expedited documents, where it is necessary to have the recipient accept delivery.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4519#section-2.27">RFC4519 Section 2.27</a>
	 */
	String ATTRIBUTE_REGISTERED_ADDRESS = "registeredAddress"; // AD: -
	/**
	 * <p>
	 * Country and city strings associated with the object (the addressee) needed to provide the Public Telegram Service.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4519#section-2.6">RFC4519 Section 2.6</a>
	 */
	String ATTRIBUTE_DESTINATION_INDICATOR = "destinationIndicator"; // AD: -
	/**
	 * <p>
	 * An indication of the preferred method of getting a message to the object.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4519#section-2.26">RFC4519 Section 2.26</a>
	 */
	String ATTRIBUTE_PREFERRED_DELIVERY_METHOD = "preferredDeliveryMethod"; // AD: -
	/**
	 * <p>
	 * Telephone numbers (and, optionally, the parameters) for facsimile terminals.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4519#section-2.10">RFC4519 Section 2.10</a>
	 */
	String ATTRIBUTE_FACSIMILE_TELEPHONE_NUMBER = "facsimileTelephoneNumber";
	/**
	 * <p>
	 * Site information from a postal address (i.e., the street name, place, avenue, and the house number).
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4519#section-2.34">RFC4519 Section 2.34</a>
	 */
	String ATTRIBUTE_STREET = "street"; // AD: streetAddress
	/**
	 * <p>
	 * Postal box identifiers that a Postal Service uses when a customer arranges to receive mail at a box on the premises of the Postal Service.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4519#section-2.25">RFC4519 Section 2.25</a>
	 */
	String ATTRIBUTE_POST_OFFICE_BOX = "postOfficeBox";
	/**
	 * <p>
	 * Codes used by a Postal Service to identify postal service zones.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4519#section-2.24">RFC4519 Section 2.24</a>
	 */
	String ATTRIBUTE_POSTAL_CODE = "postalCode";
	/**
	 * <p>
	 * Addresses used by a Postal Service to perform services for the object.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4519#section-2.23">RFC4519 Section 2.23</a>
	 */
	String ATTRIBUTE_POSTAL_ADDRESS = "postalAddress"; // AD: -
	/**
	 * <p>
	 * Names that a Postal Service uses to identify a post office.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4519#section-2.22">RFC4519 Section 2.22</a>
	 */
	String ATTRIBUTE_PHYSICAL_DELIVERY_OFFICE_NAME = "physicalDeliveryOfficeName";
	/**
	 * <p>
	 * Names of an organizational unit.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4519#section-2.20">RFC4519 Section 2.20</a>
	 */
	String ATTRIBUTE_OU = "ou";
	/**
	 * <p>
	 * Full names of states or provinces.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4519#section-2.33">RFC4519 Section 2.33</a>
	 */
	String ATTRIBUTE_ST = "st";
	/**
	 * <p>
	 * Names of a locality or place, such as a city, county, or other geographic region.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4519#section-2.16">RFC4519 Section 2.16</a>
	 */
	String ATTRIBUTE_L = "l";
	/**
	 * <p>
	 * Values of the license or registration plate associated with an individual.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc2798#section-2.1">RFC2798 Section 2.1</a>
	 */
	String ATTRIBUTE_CAR_LICENSE = "carLicense";
	/**
	 * <p>
	 * Department to which a person belongs.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc2798#section-2.1">RFC2798 Section 2.3</a>
	 */
	String ATTRIBUTE_DEPARTMENT_NUMBER = "departmentNumber";
	/**
	 * <p>
	 * Preferred name of a person to be used when displaying entries.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc2798#section-2.3">RFC2798 Section 2.3</a>
	 */
	String ATTRIBUTE_DISPLAY_NAME = "displayName";
	/**
	 * <p>
	 * Numeric or alphanumeric identifier assigned to a person.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc2798#section-2.4">RFC2798 Section 2.4</a>
	 */
	String ATTRIBUTE_EMPLOYEE_NUMBER = "employeeNumber";
	/**
	 * <p>
	 * Used to identify the employer to employee relationship.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc2798#section-2.5">RFC2798 Section 2.5</a>
	 */
	String ATTRIBUTE_EMPLOYEE_TYPE = "employeeType";
	/**
	 * <p>
	 * Strings that are the part of a person's name that is not their surname.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4519#section-2.12">RFC4519 Section 2.12</a>
	 */
	String ATTRIBUTE_GIVEN_NAME = "givenName";
	/**
	 * <p>
	 * A home telephone number associated with a person.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc1274#section-9.3.16">RFC1274 Section 9.3.16</a>
	 */
	String ATTRIBUTE_HOME_PHONE = "homePhone";
	/**
	 * <p>
	 * A home postal address for an object.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc1274#section-9.3.29">RFC1274 Section 9.3.29</a>
	 */
	String ATTRIBUTE_HOME_POSTAL_ADDRESS = "homePostalAddress"; // AD: -
	/**
	 * <p>
	 * Strings of initials of some or all of an individual's names, except the surname(s).
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4519#section-2.14">RFC4519 Section 2.14</a>
	 */
	String ATTRIBUTE_INITIALS = "initials";
	/**
	 * <p>
	 * Images of a person using the JPEG File Interchange Format
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc2798#section-2.6">RFC2798 Section 2.6</a>
	 */
	String ATTRIBUTE_JPEG_PHOTO = "jpegPhoto";
	/**
	 * <p>
	 * Uniform Resource Identifiers with optional label.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc2079">RFC2079</a>
	 */
	String ATTRIBUTE_LABELED_URI = "labeledURI";
	/**
	 * <p>
	 * An electronic mailbox.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc1274#section-9.3.3">RFC1274 Section 9.3.3</a>
	 */
	String ATTRIBUTE_MAIL = "mail";
    /**
	 * <p>
	 * The manager of an object represented by an entry.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc1274#section-9.3.10">RFC1274 Section 9.3.10</a>
	 */
	String ATTRIBUTE_MANAGER = "manager";
	/**
	 * <p>
	 * A mobile telephone number associated with a person.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc1274#section-9.3.31">RFC1274 Section 9.3.31</a>
	 */
	String ATTRIBUTE_MOBILE = "mobile";
	/**
	 * <p>
	 * Names of an organization.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4519#section-2.19">RFC4519 Section 2.19</a>
	 */
	String ATTRIBUTE_O = "o";
	/**
	 * <p>
	 * The room number of an object.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc1274#section-9.3.6">RFC1274 Section 9.3.6</a>
	 */
	String ATTRIBUTE_ROOM_NUMBER = "roomNumber";
	/**
	 * <p>
	 * The secretary of a person.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc1274#section-9.3.17">RFC1274 Section 9.3.17</a>
	 */
	String ATTRIBUTE_SECRETARY = "secretary";
	/**
	 * <p>
	 * Computer system login names associated with the object.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4519#section-2.39">RFC4519 Section 2.39</a>
	 */
	String ATTRIBUTE_UID = "uid";
	/**
	 * <p>
	 * The certificate (public key) of a person.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc2256#section-5.37">RFC2256 Section 5.37</a>
	 */
	String ATTRIBUTE_USER_CERTIFICATE = "userCertificate"; // AD: -
	/**
	 * <p>
	 * Preferred written or spoken language.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc2798#section-2.7">RFC2798 Section 2.7</a>
	 */
	String ATTRIBUTE_PREFERRED_LANGUAGE = "preferredLanguage";

	/**
	 * <p>
	 * Returns the list of surnames.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getSurname();
	
	/**
	 * <p>
	 * Returns the list of common names.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getCommonName();
	
	/**
	 * <p>
	 * Returns the list of telephone numbers.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getTelephoneNumber();
	
	/**
	 * <p>
	 * Returns the list of descriptions.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getDescription();
	
	/**
	 * <p>
	 * Returns the list of titles.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getTitle();
	
	/**
	 * <p>
	 * Returns the list of registered addresses.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getRegisteredAddress();
	
	/**
	 * <p>
	 * Returns the list of destination indicators.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getDestinationIndicator();
	
	/**
	 * <p>
	 * Returns the preferred delivery method.
	 * </p>
	 * 
	 * @return a string or null
	 */
	String getPreferredDeliveryMethod();
	
	/**
	 * <p>
	 * Returns the list of facsimile telephone numbers.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getFacsimileTelephoneNumber();
	
	/**
	 * <p>
	 * Returns the list of streets.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getStreet();
	
	/**
	 * <p>
	 * Returns the list of post office boxes.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getPostOfficeBox();
	
	/**
	 * <p>
	 * Returns the list of postal codes.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getPostalCode();
	
	/**
	 * <p>
	 * Returns the list of postal addresses.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getPostalAddress();
	
	/**
	 * <p>
	 * Returns the list of physical delivery office name.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getPhysicalDeliveryOfficeName();
	
	/**
	 * <p>
	 * Returns the list of organizational units.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getOrganizationalUnit();
	
	/**
	 * <p>
	 * Returns the list of states.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getState();
	
	/**
	 * <p>
	 * Returns the list of locality names.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getLocalityName();
	
	/**
	 * <p>
	 * Returns the list of car licenses.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getCarLicense();
	
	/**
	 * <p>
	 * Returns the list of department numbers.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getDepartmentNumber();
	
	/**
	 * <p>
	 * Returns the list of display names.
	 * </p>
	 * 
	 * @return a list
	 */
	String getDisplayName();
	
	/**
	 * <p>
	 * Returns the list of employee numbers.
	 * </p>
	 * 
	 * @return a list
	 */
	String getEmployeeNumber();
	
	/**
	 * <p>
	 * Returns the list of employee types.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getEmployeeType();
	
	/**
	 * <p>
	 * Returns the list of given names.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getGivenName();
	
	/**
	 * <p>
	 * Returns the list of home telephone numbers.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getHomePhone();
	
	/**
	 * <p>
	 * Returns the list of home postal addresses.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getHomePostalAddress();
	
	/**
	 * <p>
	 * Returns the list of initials.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getInitials();
	
	/**
	 * <p>
	 * Returns the list of JPEG photos.
	 * </p>
	 * 
	 * @return a list
	 */
	List<byte[]> getJpegPhoto();
	
	/**
	 * <p>
	 * Returns the list of labeled URIs.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getLabeledURI();
	
	/**
	 * <p>
	 * Returns the list of email addresses.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getMail();
	
	/**
	 * <p>
	 * Returns the list of post office boxes.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getManager();
	
	/**
	 * <p>
	 * Returns the list of mobile telephone numbers. 
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getMobile();
	
	/**
	 * <p>
	 * Returns the list of organization names.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getOrganizationName();
	
	/**
	 * <p>
	 * Returns the list of room numbers.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getRoomNumber();
	
	/**
	 * <p>
	 * Returns the list of secretaries.
	 * </p>
	 * 
	 * @return a list
	 */
	List<String> getSecretary();
	
	/**
	 * <p>
	 * Returns the user certificate
	 * </p>
	 * 
	 * @return a certificate or null
	 */
	byte[] getUserCertificate();
	
	/**
	 * <p>
	 * Returns the preferred language.
	 * </p>
	 * 
	 * @return a string or null
	 */
	String getPreferredLanguage();
}

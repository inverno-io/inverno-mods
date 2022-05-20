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
package io.inverno.mod.security.ldap.internal.identity;

import io.inverno.mod.security.ldap.identity.LDAPIdentity;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Generic LDAP identity implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericLDAPIdentity implements LDAPIdentity {

	private final String uid;
	private final List<String> sn;
	private final List<String> cn;
	
	private List<String> telephoneNumber;
	private List<String> description;
	private List<String> title;
	private List<String> registeredAddress;
	private List<String> destinationIndicator;
	private String preferredDeliveryMethod;
	private List<String> facsimileTelephoneNumber;
	private List<String> street;
	private List<String> postOfficeBox;
	private List<String> postalCode;
	private List<String> postalAddress;
	private List<String> physicalDeliveryOfficeName;
	private List<String> ou;
	private List<String> st;
	private List<String> l;
	private List<String> carLicense;
	private List<String> departmentNumber;
	private String displayName;
	private String employeeNumber;
	private List<String> employeeType;
	private List<String> givenName;
	private List<String> homePhone;
	private List<String> homePostalAddress;
	private List<String> initials;
	private List<byte[]> jpegPhoto;
	private List<String> labeledURI;
	private List<String> mail;
	private List<String> manager;
	private List<String> mobile;
	private List<String> o;
	private List<String> roomNumber;
	private List<String> secretary;
	
	private byte[] userCertificate;
	private String preferredLanguage;
	
	/**
	 * <p>
	 * Creates a generic LDAP identity.
	 * </p>
	 * 
	 * @param uid the uid
	 * @param sn  the list of surnames
	 * @param cn  the list of common names
	 */
	public GenericLDAPIdentity(String uid, List<String> sn, List<String> cn) {
		this.uid = uid;
		this.sn = Collections.unmodifiableList(sn);
		this.cn = Collections.unmodifiableList(cn);
	}
	
	@Override
	public List<String> getSurname() {
		return sn;
	}

	@Override
	public List<String> getCommonName() {
		return cn;
	}

	@Override
	public List<String> getTelephoneNumber() {
		return telephoneNumber;
	}
	
	/**
	 * <p>
	 * Sets the list of telephone numbers.
	 * </p>
	 * 
	 * @param telephoneNumber a list
	 */
	public void setTelephoneNumber(List<String> telephoneNumber) {
		this.telephoneNumber = Collections.unmodifiableList(telephoneNumber);
	}
	
	@Override
	public List<String> getDescription() {
		return description;
	}
	
	/**
	 * <p>
	 * Sets the list of descriptions.
	 * </p>
	 * 
	 * @param description a list
	 */
	public void setDescription(List<String> description) {
		this.description = Collections.unmodifiableList(description);
	}
	
	@Override
	public List<String> getTitle() {
		return title;
	}
	
	/**
	 * <p>
	 * Sets the list of titles.
	 * </p>
	 * 
	 * @param title a list
	 */
	public void setTitle(List<String> title) {
		this.title = Collections.unmodifiableList(title);
	}
	
	@Override
	public List<String> getRegisteredAddress() {
		return registeredAddress;
	}
	
	/**
	 * <p>
	 * Sets the list of registered addresses.
	 * </p>
	 * 
	 * @param registeredAddress a list
	 */
	public void setRegisteredAddress(List<String> registeredAddress) {
		this.registeredAddress = Collections.unmodifiableList(registeredAddress);
	}
	
	@Override
	public List<String> getDestinationIndicator() {
		return destinationIndicator;
	}
	
	/**
	 * <p>
	 * Sets the list of destination indicators.
	 * </p>
	 * 
	 * @param destinationIndicator a list
	 */
	public void setDestinationIndicator(List<String> destinationIndicator) {
		this.destinationIndicator = Collections.unmodifiableList(destinationIndicator);
	}

	@Override
	public String getPreferredDeliveryMethod() {
		return preferredDeliveryMethod;
	}
	
	/**
	 * <p>
	 * Sets the preferred delivery method.
	 * </p>
	 * 
	 * @param preferredDeliveryMethod a string
	 */
	public void setPreferredDeliveryMethod(String preferredDeliveryMethod) {
		this.preferredDeliveryMethod = preferredDeliveryMethod;
	}
	
	@Override
	public List<String> getFacsimileTelephoneNumber() {
		return facsimileTelephoneNumber;
	}
	
	/**
	 * <p>
	 * Sets the list of facsimile telephone numbers.
	 * </p>
	 * 
	 * @param facsimileTelephoneNumber a list
	 */
	public void setFacsimileTelephoneNumber(List<String> facsimileTelephoneNumber) {
		this.facsimileTelephoneNumber = Collections.unmodifiableList(facsimileTelephoneNumber);
	}
	
	@Override
	public List<String> getStreet() {
		return street;
	}
	
	/**
	 * <p>
	 * Sets the list of streets.
	 * </p>
	 * 
	 * @param street a list
	 */
	public void setStreet(List<String> street) {
		this.street = Collections.unmodifiableList(street);
	}
	
	@Override
	public List<String> getPostOfficeBox() {
		return postOfficeBox;
	}
	
	/**
	 * <p>
	 * Sets the list of post office boxes.
	 * </p>
	 * 
	 * @param postOfficeBox a list
	 */
	public void setPostOfficeBox(List<String> postOfficeBox) {
		this.postOfficeBox = Collections.unmodifiableList(postOfficeBox);
	}
	
	@Override
	public List<String> getPostalCode() {
		return postalCode;
	}
	
	/**
	 * <p>
	 * Sets the list of postal codes.
	 * </p>
	 * 
	 * @param postalCode a list
	 */
	public void setPostalCode(List<String> postalCode) {
		this.postalCode = Collections.unmodifiableList(postalCode);
	}

	@Override
	public List<String> getPostalAddress() {
		return postalAddress;
	}
	
	/**
	 * <p>
	 * Sets the list of postal addresses.
	 * </p>
	 * 
	 * @param postalAddress a list
	 */
	public void setPostalAddress(List<String> postalAddress) {
		this.postalAddress = Collections.unmodifiableList(postalAddress);
	}
	
	@Override
	public List<String> getPhysicalDeliveryOfficeName() {
		return physicalDeliveryOfficeName;
	}

	/**
	 * <p>
	 * Sets the list of physical delivery office names.
	 * </p>
	 * 
	 * @param physicalDeliveryOfficeName a list
	 */
	public void setPhysicalDeliveryOfficeName(List<String> physicalDeliveryOfficeName) {
		this.physicalDeliveryOfficeName = Collections.unmodifiableList(physicalDeliveryOfficeName);
	}
	
	@Override
	public List<String> getOrganizationalUnit() {
		return ou;
	}
	
	/**
	 * <p>
	 * Sets the list of organizational units.
	 * </p>
	 * 
	 * @param ou a list
	 */
	public void setOrganizationalUnit(List<String> ou) {
		this.ou = Collections.unmodifiableList(ou);
	}
	
	@Override
	public List<String> getState() {
		return st;
	}
	
	/**
	 * <p>
	 * Sets the list of states.
	 * </p>
	 * 
	 * @param st a list
	 */
	public void setState(List<String> st) {
		this.st = Collections.unmodifiableList(st);
	}
	
	@Override
	public List<String> getLocalityName() {
		return l;
	}
	
	/**
	 * <p>
	 * Sets the list of locality names.
	 * </p>
	 * 
	 * @param l a list
	 */
	public void setLocalityName(List<String> l) {
		this.l = Collections.unmodifiableList(l);
	}
	
	@Override
	public List<String> getCarLicense() {
		return carLicense;
	}
	
	/**
	 * <p>
	 * Sets the list of car licenses.
	 * </p>
	 * 
	 * @param carLicense a list
	 */
	public void setCarLicense(List<String> carLicense) {
		this.carLicense = Collections.unmodifiableList(carLicense);
	}
	
	@Override
	public List<String> getDepartmentNumber() {
		return departmentNumber;
	}
	
	/**
	 * <p>
	 * Sets the list of department numbers.
	 * </p>
	 * 
	 * @param departmentNumber a list
	 */
	public void setDepartmentNumber(List<String> departmentNumber) {
		this.departmentNumber = Collections.unmodifiableList(departmentNumber);
	}
	
	@Override
	public String getDisplayName() {
		return displayName;
	}
	
	/**
	 * <p>
	 * Sets the display name.
	 * </p>
	 * 
	 * @param displayName a string
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	@Override
	public String getEmployeeNumber() {
		return employeeNumber;
	}
	
	/**
	 * <p>
	 * Sets the employee number.
	 * </p>
	 * 
	 * @param employeeNumber a string
	 */
	public void setEmployeeNumber(String employeeNumber) {
		this.employeeNumber = employeeNumber;
	}
	
	@Override
	public List<String> getEmployeeType() {
		return employeeType;
	}
	
	/**
	 * <p>
	 * Sets the list of employee types.
	 * </p>
	 * 
	 * @param employeeType a list
	 */
	public void setEmployeeType(List<String> employeeType) {
		this.employeeType = Collections.unmodifiableList(employeeType);
	}
	
	@Override
	public List<String> getGivenName() {
		return givenName;
	}
	
	/**
	 * <p>
	 * Sets the list of given names.
	 * </p>
	 * 
	 * @param givenName a list
	 */
	public void setGivenName(List<String> givenName) {
		this.givenName = Collections.unmodifiableList(givenName);
	}
	
	@Override
	public List<String> getHomePhone() {
		return homePhone;
	}
	
	/**
	 * <p>
	 * Sets the list of home telephone numbers.
	 * </p>
	 * 
	 * @param homePhone a list
	 */
	public void setHomePhone(List<String> homePhone) {
		this.homePhone = Collections.unmodifiableList(homePhone);
	}
	
	@Override
	public List<String> getHomePostalAddress() {
		return homePostalAddress;
	}
	
	/**
	 * <p>
	 * Sets the list of home postal addresses.
	 * </p>
	 * 
	 * @param homePostalAddress a list
	 */
	public void setHomePostalAddress(List<String> homePostalAddress) {
		this.homePostalAddress = Collections.unmodifiableList(homePostalAddress);
	}
	
	@Override
	public List<String> getInitials() {
		return initials;
	}
	
	/**
	 * <p>
	 * Sets the list of initials.
	 * </p>
	 * 
	 * @param initials a list
	 */
	public void setInitials(List<String> initials) {
		this.initials = Collections.unmodifiableList(initials);
	}
	
	@Override
	public List<byte[]> getJpegPhoto() {
		return jpegPhoto;
	}
	
	/**
	 * <p>
	 * Sets the list of JPEG photos.
	 * </p>
	 * 
	 * @param jpegPhoto a list
	 */
	public void setJpegPhoto(List<byte[]> jpegPhoto) {
		this.jpegPhoto = Collections.unmodifiableList(jpegPhoto);
	}
	
	@Override
	public List<String> getLabeledURI() {
		return labeledURI;
	}
	
	/**
	 * <p>
	 * Sets the list of labeled URIs.
	 * </p>
	 * 
	 * @param description a list
	 */
	public void setLabeledURI(List<String> labeledURI) {
		this.labeledURI = Collections.unmodifiableList(labeledURI);
	}
	
	@Override
	public List<String> getMail() {
		return mail;
	}
	
	/**
	 * <p>
	 * Sets the list of email addresses.
	 * </p>
	 * 
	 * @param mail a list
	 */
	public void setMail(List<String> mail) {
		this.mail = Collections.unmodifiableList(mail);
	}
	
	@Override
	public List<String> getManager() {
		return manager;
	}
	
	/**
	 * <p>
	 * Sets the list of managers.
	 * </p>
	 * 
	 * @param manager a list
	 */
	public void setManager(List<String> manager) {
		this.manager = Collections.unmodifiableList(manager);
	}
	
	@Override
	public List<String> getMobile() {
		return mobile;
	}
	
	/**
	 * <p>
	 * Sets the list of mobile telephone numbers.
	 * </p>
	 * 
	 * @param mobile a list
	 */
	public void setMobile(List<String> mobile) {
		this.mobile = Collections.unmodifiableList(mobile);
	}
	
	@Override
	public List<String> getOrganizationName() {
		return o;
	}
	
	/**
	 * <p>
	 * Sets the list of organization names.
	 * </p>
	 * 
	 * @param o a list
	 */
	public void setOrganizationName(List<String> o) {
		this.o = Collections.unmodifiableList(o);
	}
	
	@Override
	public List<String> getRoomNumber() {
		return roomNumber;
	}
	
	/**
	 * <p>
	 * Sets the list of room numbers.
	 * </p>
	 * 
	 * @param roomNumber a list
	 */
	public void setRoomNumber(List<String> roomNumber) {
		this.roomNumber = Collections.unmodifiableList(roomNumber);
	}
	
	@Override
	public List<String> getSecretary() {
		return secretary;
	}
	
	/**
	 * <p>
	 * Sets the list of secretaries.
	 * </p>
	 * 
	 * @param secretary a list
	 */
	public void setSecretary(List<String> secretary) {
		this.secretary = Collections.unmodifiableList(secretary);
	}
	
	@Override
	public String getUid() {
		return uid;
	}
	
	@Override
	public byte[] getUserCertificate() {
		return userCertificate;
	}
	
	/**
	 * <p>
	 * Sets the user certificate.
	 * </p>
	 * 
	 * @param userCertificate a byte array
	 */
	public void setUserCertificate(byte[] userCertificate) {
		this.userCertificate = userCertificate;
	}
	
	@Override
	public String getPreferredLanguage() {
		return preferredLanguage;
	}
	
	/**
	 * <p>
	 * Sets the preferred language.
	 * </p>
	 * 
	 * @param preferredLanguage a string
	 */
	public void setPreferredLanguage(String preferredLanguage) {
		this.preferredLanguage = preferredLanguage;
	}
}

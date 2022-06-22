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
package io.inverno.mod.security.identity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.inverno.mod.security.authentication.user.User;
import java.util.Objects;

/**
 * <p>
 * A basic identity implementation exposing basic personal information.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see User
 */
public class PersonIdentity implements Identity {
	
	/**
	 * The unique identifier of the person.
	 */
	@JsonIgnore
	private final String uid;
	
	/**
	 * The person's first name.
	 */
	@JsonIgnore
	private String firstName;
	
	/**
	 * The person's last name.
	 */
	@JsonIgnore
	private String lastName;
	
	/**
	 * The person's email address.
	 */
	@JsonIgnore
	private String email;

	/**
	 * <p>
	 * Creates a person identity.
	 * </p>
	 *
	 * @param uid       the unique identifier of the person
	 * @param firstName the person's first name
	 * @param lastName  the person's last name
	 * @param email     the person's email address
	 */
	@JsonCreator
	public PersonIdentity(@JsonProperty(value = "uid", required = true) String uid, @JsonProperty(value = "firstName", required = true) String firstName, @JsonProperty(value = "lastName", required = true) String lastName, @JsonProperty(value = "email", required = true) String email) {
		this.uid = Objects.requireNonNull(uid);
		this.firstName = Objects.requireNonNull(firstName);
		this.lastName = Objects.requireNonNull(lastName);
		this.email = Objects.requireNonNull(email);
	}

	/**
	 * <p>
	 * Returns the unique identifier that identifies the person.
	 * </p>
	 * 
	 * @return the unique identifier
	 */
	@Override
	public String getUid() {
		return this.uid;
	}
	
	/**
	 * <p>
	 * Returns the person's first name.
	 * </p>
	 * 
	 * @return the first name
	 */
	@JsonProperty("firstName")
	public String getFirstName() {
		return firstName;
	}

	/**
	 * <p>
	 * Sets the person's first name.
	 * </p>
	 * 
	 * @param firstName the first name to set
	 */
	@JsonIgnore
	public void setFirstName(String firstName) {
		this.firstName = Objects.requireNonNull(firstName);
	}

	/**
	 * <p>
	 * Returns the person's last name.
	 * </p>
	 * 
	 * @return the last name
	 */
	@JsonProperty("lastName")
	public String getLastName() {
		return lastName;
	}

	/**
	 * <p>
	 * Sets the person's last name.
	 * </p>
	 * 
	 * @param lastName the last name to set
	 */
	@JsonIgnore
	public void setLastName(String lastName) {
		this.lastName = Objects.requireNonNull(lastName);
	}

	/**
	 * <p>
	 * Returns the person's email address.
	 * </p>
	 * 
	 * @return the email address
	 */
	@JsonProperty("email")
	public String getEmail() {
		return email;
	}

	/**
	 * <p>
	 * Sets the person's email address.
	 * </p>
	 * 
	 * @param email the email address to set
	 */
	@JsonIgnore
	public void setEmail(String email) {
		this.email = Objects.requireNonNull(email);
	}
}

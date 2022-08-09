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
package io.inverno.mod.security.jose.jwt;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.inverno.mod.base.converter.Convertible;
import io.inverno.mod.base.converter.ObjectDecoder;
import io.inverno.mod.security.jose.internal.jwt.StringOrURI;
import java.io.File;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * <p>
 * A JWT Claims set contains the claims conveyed by a JSON Web Token as specified by <a href="https://datatracker.ietf.org/doc/html/rfc7519#section-4">RFC7519 Section 4</a>.
 * </p>
 * 
 * <p>
 * A JWT Claims set can be validated in various ways using methods: {@link #isValid() }, {@link #ifValid(java.lang.Runnable) }... JWT claims validation is performed by a list of
 * {@link JWTClaimsSetValidator} which can be set using {@link #validate(io.inverno.mod.security.jose.jwt.JWTClaimsSetValidator)} or {@link #setValidators(java.util.List)} methods. By default,
 * expiration time and not before time are validated.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JWTClaimsSet {
	
	/**
	 * The issuer claim.
	 */
	private final StringOrURI iss;
	/**
	 * The subject claim.
	 */
	private final StringOrURI sub;
	/**
	 * The audience claim.
	 */
	private final String aud;
	/**
	 * The expiration time claim.
	 */
	private final ZonedDateTime exp;
	/**
	 * The not before claim.
	 */
	private final ZonedDateTime nbf;
	/**
	 * The issued at claim.
	 */
	private final ZonedDateTime iat;
	/**
	 * The JWT id claim.
	 */
	private final String jti;
	
	/**
	 * Custom claims map.
	 */
	/*
	 * Unfortunately We can't use @JsonAnySetter on a constructor parameter
	 * https://github.com/FasterXML/jackson-databind/issues/562
	 *
	 * When/if this is supported, we must move this to the @JsonCreator constructor
	 */
	@JsonAnySetter
	private final Map<String, Object> customClaims;
	
	/**
	 * The list of validators to use to validate the JWT
	 */
	@JsonIgnore
	private final List<JWTClaimsSetValidator> validators;
	
	/**
	 * <p>
	 * Creates a JWT Claims set.
	 * </p>
	 * 
	 * @param iss the issuer claim
	 * @param sub the subject claim
	 * @param aud the audience claim
	 * @param exp the expiration time claim
	 * @param nbf the not before claim
	 * @param iat the issue at claim
	 * @param jti the JWT id claim
	 * 
	 * @throws JWTBuildException if there was an error building the JWT claims set
	 */
	@JsonCreator
	public JWTClaimsSet(@JsonProperty("iss") String iss, @JsonProperty("sub") String sub, @JsonProperty("aud") String aud, @JsonProperty("exp") Long exp, @JsonProperty("nbf") Long nbf, @JsonProperty("iat") Long iat, @JsonProperty("jti") String jti) throws JWTBuildException {
		this.iss = iss != null ? new StringOrURI(iss) : null;
		this.sub = sub != null ? new StringOrURI(sub) : null;
		this.aud = aud;
		this.exp = exp != null ? ZonedDateTime.ofInstant(Instant.ofEpochSecond(exp), ZoneOffset.UTC) : null;
		this.nbf = nbf != null ? ZonedDateTime.ofInstant(Instant.ofEpochSecond(nbf), ZoneOffset.UTC) : null;
		this.iat = iat != null ? ZonedDateTime.ofInstant(Instant.ofEpochSecond(iat), ZoneOffset.UTC) : null;
		this.jti =jti;
		this.customClaims = new HashMap<>();
		this.validators = new LinkedList<>();
		this.validate(JWTClaimsSetValidator.expiration()).validate(JWTClaimsSetValidator.notBefore());
	}
	
	/**
	 * <p>
	 * Creates a JWT Claims set.
	 * </p>
	 *
	 * @param iss          the issuer claim
	 * @param sub          the subject claim
	 * @param aud          the audience claim
	 * @param exp          the expiration time claim
	 * @param nbf          the not before claim
	 * @param iat          the issue at claim
	 * @param jti          the JWT id claim
	 * @param customClaims custom claims map
	 *
	 * @throws JWTBuildException if there was an error building the JWT claims set
	 */
	public JWTClaimsSet(@JsonProperty("iss") String iss, @JsonProperty("sub") String sub, @JsonProperty("aud") String aud, @JsonProperty("exp") Long exp, @JsonProperty("nbf") Long nbf, @JsonProperty("iat") Long iat, @JsonProperty("jti") String jti, Map<String, Object> customClaims) throws JWTBuildException {
		this.iss = iss != null ? new StringOrURI(iss) : null;
		this.sub = sub != null ? new StringOrURI(sub) : null;
		this.aud = aud;
		this.exp = exp != null ? ZonedDateTime.ofInstant(Instant.ofEpochSecond(exp), ZoneOffset.UTC) : null;
		this.nbf = nbf != null ? ZonedDateTime.ofInstant(Instant.ofEpochSecond(nbf), ZoneOffset.UTC) : null;
		this.iat = iat != null ? ZonedDateTime.ofInstant(Instant.ofEpochSecond(iat), ZoneOffset.UTC) : null;
		this.jti =jti;
		this.customClaims = customClaims;
		this.validators = new LinkedList<>();
		this.validate(JWTClaimsSetValidator.expiration()).validate(JWTClaimsSetValidator.notBefore());
	}
	
	/**
	 * <p>
	 * Creates a JWT Claims set.
	 * </p>
	 *
	 * @param iss          the issuer claim as string
	 * @param iss_uri      the issuer claim as URI
	 * @param sub          the subject claim as string
	 * @param sub_uri      the issuer claim as URI
	 * @param aud          the audience claim
	 * @param exp          the expiration time claim
	 * @param nbf          the not before claim
	 * @param iat          the issue at claim
	 * @param jti          the JWT id claim
	 * @param customClaims custom claims map
	 *
	 * @throws JWTBuildException if there was an error building the JWT claims set
	 */
	protected JWTClaimsSet(String iss, URI iss_uri, String sub, URI sub_uri, String aud, ZonedDateTime exp, ZonedDateTime nbf, ZonedDateTime iat, String jti, Map<String, Object> customClaims) throws JWTBuildException {
		if(iss != null && iss_uri != null) {
			throw new JWTBuildException("Cannot define issuer as both String and URI");
		}
		if(sub != null && sub_uri != null) {
			throw new JWTBuildException("Cannot define subject as both String and URI");
		}
		
		if(iss != null) {
			this.iss = new StringOrURI(iss);
		}
		else if(iss_uri != null) {
			this.iss = new StringOrURI(iss_uri);
		}
		else {
			this.iss = null;
		}
		
		if(sub != null) {
			this.sub = new StringOrURI(sub);
		}
		else if(iss_uri != null) {
			this.sub = new StringOrURI(sub_uri);
		}
		else {
			this.sub = null;
		}
		this.aud = aud;
		this.exp = exp;
		this.nbf = nbf;
		this.iat = iat;
		this.jti = jti;
		this.customClaims = customClaims != null ? Collections.unmodifiableMap(customClaims) : null;
		this.validators = new LinkedList<>();
		this.validate(JWTClaimsSetValidator.expiration()).validate(JWTClaimsSetValidator.notBefore());
	}
	
	/**
	 * <p>
	 * Returns the issuer claim as String.
	 * </p>
	 * 
	 * @return the issuer claim as String
	 */
	@JsonProperty("iss")
	public String getIssuer() {
		return this.iss != null ? this.iss.asString() : null;
	}
	
	/**
	 * <p>
	 * Returns the issuer claim as URI.
	 * </p>
	 * 
	 * @return the issuer claim as URI
	 */
	@JsonIgnore
	public URI getIssuerAsURI() {
		return this.iss != null ? this.iss.asURI() : null;
	}
	
	/**
	 * <p>
	 * Returns the subject claim as String.
	 * </p>
	 * 
	 * @return the subject claim as String
	 */
	@JsonProperty("sub")
	public String getSubject() {
		return this.sub != null ? this.sub.asString() : null;
	}
	
	/**
	 * <p>
	 * Returns the subject claim as URI.
	 * </p>
	 * 
	 * @return the subject claim as URI
	 */
	@JsonIgnore
	public URI getSubjectAsURI() {
		return this.sub != null ? this.sub.asURI() : null;
	}
	
	/**
	 * <p>
	 * Returns the audience claim.
	 * </p>
	 * 
	 * @return the audience claim
	 */
	@JsonProperty("aud")
	public String getAudience() {
		return this.aud;
	}
	
	/**
	 * <p>
	 * Returns the expiration time claim as seconds since epoch.
	 * </p>
	 * 
	 * @return the expiration time claim as seconds since epoch
	 */
	@JsonProperty("exp")
	public Long getExpirationTime() {
		return this.exp != null ? this.exp.toEpochSecond() : null;
	}
	
	/**
	 * <p>
	 * Returns the expiration time claim as zoned date time.
	 * </p>
	 * 
	 * @return the expiration time claim as zoned date time
	 */
	@JsonIgnore
	public ZonedDateTime getExpirationTimeAsDateTime() {
		return this.exp;
	}
	
	/**
	 * <p>
	 * Returns the not before claim as seconds since epoch.
	 * </p>
	 * 
	 * @return the not before claim as seconds since epoch
	 */
	@JsonProperty("nbf")
	public Long getNotBefore() {
		return this.nbf != null ? this.nbf.toEpochSecond() : null;
	}
	
	/**
	 * <p>
	 * Returns the not before claim as zoned date time.
	 * </p>
	 * 
	 * @return the not before claim as zoned date time
	 */
	@JsonIgnore
	public ZonedDateTime getNotBeforeAsDateTime() {
		return this.nbf;
	}
	
	/**
	 * <p>
	 * Returns the issued at claim as seconds since epoch.
	 * </p>
	 * 
	 * @return the issued at claim as seconds since epoch
	 */
	@JsonProperty("iat")
	public Long getIssuedAt() {
		return this.iat != null ? this.iat.toEpochSecond() : null;
	}
	
	/**
	 * <p>
	 * Returns the issued at claim as zoned date time.
	 * </p>
	 * 
	 * @return the issued at claim as zoned date time
	 */
	@JsonIgnore
	public ZonedDateTime getIssuedAtAsDateTime() {
		return this.iat;
	}
	
	/**
	 * <p>
	 * Returns the JWT id claim.
	 * </p>
	 * 
	 * @return the JWT id claim
	 */
	@JsonProperty("jti")
	public String getJWTId() {
		return this.jti;
	}

	/**
	 * <p>
	 * Returns the map of custom claims.
	 * </p>
	 * 
	 * @return a map of custom claims
	 */
	@JsonAnyGetter
	public final Map<String, Object> getCustomClaims() {
		return this.customClaims;
	}
	
	/**
	 * <p>
	 * Returns the custom claim with the specified name.
	 * </p>
	 * 
	 * <p>
	 * The returned {@link Claim} object allows to convert the claim into various types.
	 * </p>
	 * 
	 * @param name a custom claim name
	 * 
	 * @return an optional containing the custom claim or an empty optional if the JWT claims set does not contain that claim
	 */
	public final Optional<Claim> getCustomClaim(String name) {
		return Optional.ofNullable(this.customClaims.get(name)).map(value -> new GenericJWTClaim(name, value));
	}

	/**
	 * <p>
	 * Adds the specified validator to the JWT claims set.
	 * </p>
	 * 
	 * @param validator the validator to add
	 * 
	 * @return the JWT claims set
	 */
	public final JWTClaimsSet validate(JWTClaimsSetValidator validator) {
		this.validators.add(validator);
		return this;
	}

	/**
	 * <p>
	 * Sets the JWT claims set validators.
	 * </p>
	 * 
	 * @param validators a list of validators or null to clear the validators
	 */
	public final void setValidators(List<JWTClaimsSetValidator> validators) {
		this.validators.clear();
		if(validators != null) {
			for(JWTClaimsSetValidator validator : validators) {
				this.validators.add(validator);
			}
		}
	}

	/**
	 * <p>
	 * Returns the list of JWT claims set validators.
	 * </p>
	 * 
	 * @return the JWT claims set validators
	 */
	public final List<JWTClaimsSetValidator> getValidators() {
		return Collections.unmodifiableList(validators);
	}
	
	/**
	 * <p>
	 * Validates the JWT claims set and throws an exception if it is invalid.
	 * </p>
	 * 
	 * <p>
	 * This method basically invoke the list of validators that have been set using {@link #validate(io.inverno.mod.security.jose.jwt.JWTClaimsSetValidator) }.
	 * </p>
	 * 
	 * @throws InvalidJWTException if the JWT claims set is invalid
	 */
	protected void validate() throws InvalidJWTException {
		this.validators.stream().forEach(validator -> validator.validate(this));
	}
	
	/**
	 * <p>
	 * Determines whether the JWT Claims set is valid.
	 * </p>
	 * 
	 * @return true if the JWT Claims set is valid, false otherwise
	 */
	@JsonIgnore
	public final boolean isValid() {
		try {
			this.validate();
			return true;
		}
		catch(InvalidJWTException e) {
			return false;
		}
	}

	/**
	 * <p>
	 * Executes the specified action if the JWT Claims set is valid.
	 * </p>
	 * 
	 * @param action the action to run if the JWT Claims set is valid
	 */
	public final void ifValid(Runnable action) {
		if(this.isValid()) {
			action.run();
		}
	}
	
	/**
	 * <p>
	 * Executes the specified action if the JWT Claims set is valid or the specified invalid action if it is invalid.
	 * </p>
	 *
	 * @param action        the action to run if the JWT Claims set is valid
	 * @param invalidAction the action to run if the JWT Claims set is invalid
	 */
	public final void ifValidOrElse(Runnable action, Runnable invalidAction) {
		if(this.isValid()) {
			action.run();
		}
		else {
			invalidAction.run();
		}
	}
	
	/**
	 * <p>
	 * Returns the JWT Claims set or throws an exception if it is invalid.
	 * </p>
	 * 
	 * @return the JWT Claims set
	 * 
	 * @throws InvalidJWTException if the JWT Claims set is invalid
	 */
	public final JWTClaimsSet ifInvalidThrow() throws InvalidJWTException {
		this.validate();
		return this;
	}
	
	/**
	 * <p>
	 * Returns the JWT Claims set or throws a custom exception if it is invalid.
	 * </p>
	 *
	 * @param <T>               the custom exception type
	 * @param exceptionSupplier the exception supplier
	 *
	 * @return the JWT Claims set
	 *
	 * @throws T if the JWT Claims set is invalid
	 */
	public <T extends Throwable> JWTClaimsSet ifInvalidThrow(Supplier<? extends T> exceptionSupplier) throws T {
		if(!this.isValid()) {
			throw exceptionSupplier.get();
		}
		return this;
	}
	
	/**
	 * <p>
	 * Creates a new empty JWT Claims set builder.
	 * </p>
	 * 
	 * @return a new empty JWT Claims set builder
	 */
	public static Builder<JWTClaimsSet, ?> of() {
		return new GenericJWTClaimsSetBuilder();
	}
	
	/**
	 * <p>
	 * Creates a new JWT Claims set builder with the specified issuer and expiration time claims.
	 * </p>
	 * 
	 * @param iss the issuer
	 * @param exp the expiration time in seconds since epoch
	 * 
	 * @return a new JWT Claims set builder with the specified issuer and expiration time claims.
	 */
	public static Builder<JWTClaimsSet, ?> of(String iss, long exp) {
		return new GenericJWTClaimsSetBuilder()
			.issuer(iss)
			.expirationTime(exp);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(aud, customClaims, exp, iat, iss, jti, nbf, sub);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JWTClaimsSet other = (JWTClaimsSet) obj;
		return Objects.equals(aud, other.aud) && Objects.equals(customClaims, other.customClaims)
				&& Objects.equals(exp, other.exp) && Objects.equals(iat, other.iat) && Objects.equals(iss, other.iss)
				&& Objects.equals(jti, other.jti) && Objects.equals(nbf, other.nbf) && Objects.equals(sub, other.sub);
	}

	/**
	 * <p>
	 * A JWT Claim represents a piece of information asserted about a subject.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	public interface Claim extends Convertible<Object> {
		
		/**
		 * <p>
		 * Returns the claim name.
		 * </p>
		 *
		 * @return a name
		 */
		String getName();
	}
	
	/**
	 * <p>
	 * A generic {@link JWTClaim} implementation using an {@link ObjectDecoder} to convert the value.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	private static class GenericJWTClaim implements JWTClaimsSet.Claim {

		/**
		 * The Object decoder instance.
		 */
		private static final ObjectDecoder OBJECT_DECODER = new ObjectDecoder();
		
		/**
		 * The claim name.
		 */
		private final String name;
		/**
		 * The claim value.
		 */
		private final Object value;

		/**
		 * <p>
		 * Creates a JWT claim with the specified name and value.
		 * </p>
		 *
		 * @param name  the claim name
		 * @param value the claim value
		 */
		public GenericJWTClaim(String name, Object value) {
			this.name = name;
			this.value = value;
		}
		
		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public Object getValue() {
			return this.value;
		}
		
		@Override
		public <T> T as(Class<T> type) {
			return OBJECT_DECODER.decode(this.value, type);
		}

		@Override
		public <T> T as(Type type) {
			return OBJECT_DECODER.decode(this.value, type);
		}

		@Override
		public <T> T[] asArrayOf(Class<T> type) {
			return OBJECT_DECODER.decodeToArray(this.value, type);
		}

		@Override
		public <T> T[] asArrayOf(Type type) {
			return OBJECT_DECODER.decodeToArray(this.value, type);
		}

		@Override
		public <T> List<T> asListOf(Class<T> type) {
			return OBJECT_DECODER.decodeToList(this.value, type);
		}

		@Override
		public <T> List<T> asListOf(Type type) {
			return OBJECT_DECODER.decodeToList(this.value, type);
		}

		@Override
		public <T> Set<T> asSetOf(Class<T> type) {
			return OBJECT_DECODER.decodeToSet(this.value, type);
		}

		@Override
		public <T> Set<T> asSetOf(Type type) {
			return OBJECT_DECODER.decodeToSet(this.value, type);
		}

		@Override
		public Byte asByte() {
			return OBJECT_DECODER.decodeByte(this.value);
		}

		@Override
		public Short asShort() {
			return OBJECT_DECODER.decodeShort(this.value);
		}

		@Override
		public Integer asInteger() {
			return OBJECT_DECODER.decodeInteger(this.value);
		}

		@Override
		public Long asLong() {
			return OBJECT_DECODER.decodeLong(this.value);
		}

		@Override
		public Float asFloat() {
			return OBJECT_DECODER.decodeFloat(this.value);
		}

		@Override
		public Double asDouble() {
			return OBJECT_DECODER.decodeDouble(this.value);
		}

		@Override
		public Character asCharacter() {
			return OBJECT_DECODER.decodeCharacter(this.value);
		}

		@Override
		public String asString() {
			return OBJECT_DECODER.decodeString(this.value);
		}

		@Override
		public Boolean asBoolean() {
			return OBJECT_DECODER.decodeBoolean(this.value);
		}

		@Override
		public BigInteger asBigInteger() {
			return OBJECT_DECODER.decodeBigInteger(this.value);
		}

		@Override
		public BigDecimal asBigDecimal() {
			return OBJECT_DECODER.decodeBigDecimal(this.value);
		}

		@Override
		public LocalDate asLocalDate() {
			return OBJECT_DECODER.decodeLocalDate(this.value);
		}

		@Override
		public LocalDateTime asLocalDateTime() {
			return OBJECT_DECODER.decodeLocalDateTime(this.value);
		}

		@Override
		public ZonedDateTime asZonedDateTime() {
			return OBJECT_DECODER.decodeZonedDateTime(this.value);
		}

		@Override
		public Currency asCurrency() {
			return OBJECT_DECODER.decodeCurrency(this.value);
		}

		@Override
		public Locale asLocale() {
			return OBJECT_DECODER.decodeLocale(this.value);
		}

		@Override
		public File asFile() {
			return OBJECT_DECODER.decodeFile(this.value);
		}

		@Override
		public Path asPath() {
			return OBJECT_DECODER.decodePath(this.value);
		}

		@Override
		public URI asURI() {
			return OBJECT_DECODER.decodeURI(this.value);
		}

		@Override
		public URL asURL() {
			return OBJECT_DECODER.decodeURL(this.value);
		}

		@Override
		public Pattern asPattern() {
			return OBJECT_DECODER.decodePattern(this.value);
		}

		@Override
		public InetAddress asInetAddress() {
			return OBJECT_DECODER.decodeInetAddress(this.value);
		}

		@Override
		public Class<?> asClass() {
			return OBJECT_DECODER.decodeClass(this.value);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, value);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			GenericJWTClaim other = (GenericJWTClaim) obj;
			return Objects.equals(name, other.name) && Objects.equals(value, other.value);
		}
	}
	
	/**
	 * <p>
	 * A JWT Claims set builder is used to build a JWT Claims set.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 * 
	 * @param <A> the JWT Claims set type
	 * @param <B> the JWT Claims set builder type
	 */
	public interface Builder<A extends JWTClaimsSet, B extends JWTClaimsSet.Builder<A, B>> {
		
		/**
		 * <p>
		 * Specifies the issuer claim as a String.
		 * </p>
		 * 
		 * @param iss the issuer claim value
		 * 
		 * @return this builder
		 */
		B issuer(String iss);
		
		/**
		 * <p>
		 * Specifies the issuer claim as a URI.
		 * </p>
		 * 
		 * @param iss the issuer claim value
		 * 
		 * @return this builder
		 */
		B issuer(URI iss);
		
		/**
		 * <p>
		 * Specifies the subject claim as a String.
		 * </p>
		 * 
		 * @param sub the subject claim value
		 * 
		 * @return this builder
		 */
		B subject(String sub);
		
		/**
		 * <p>
		 * Specifies the subject claim as a URI.
		 * </p>
		 * 
		 * @param sub the subject claim value
		 * 
		 * @return this builder
		 */
		B subject(URI sub);
		
		/**
		 * <p>
		 * Specifies the audience claim.
		 * </p>
		 * 
		 * @param aud the audience claim value
		 * 
		 * @return this builder
		 */
		B audience(String aud);
		
		/**
		 * <p>
		 * Specifies the expiration time claim as seconds since epoch.
		 * </p>
		 * 
		 * @param exp the expiration time claim value
		 * 
		 * @return this builder
		 */
		B expirationTime(Long exp);
		
		/**
		 * <p>
		 * Specifies the expiration time claim as zoned date time.
		 * </p>
		 * 
		 * @param exp the expiration time claim value
		 * 
		 * @return this builder
		 */
		B expirationTime(ZonedDateTime exp);
		
		/**
		 * <p>
		 * Specifies the not before claim as seconds since epoch.
		 * </p>
		 * 
		 * @param nbf the not before claim value
		 * 
		 * @return this builder
		 */
		B notBefore(Long nbf);
		
		/**
		 * <p>
		 * Specifies the not before claim as zoned date time.
		 * </p>
		 * 
		 * @param nbf the not before claim value
		 * 
		 * @return this builder
		 */
		B notBefore(ZonedDateTime nbf);
		
		/**
		 * <p>
		 * Specifies the issued at claim as seconds since epoch.
		 * </p>
		 * 
		 * @param iat the issued at claim value
		 * 
		 * @return this builder
		 */
		B issuedAt(Long iat);
		
		/**
		 * <p>
		 * Specifies the issued at claim as zoned date time.
		 * </p>
		 * 
		 * @param iat the issued at claim value
		 * 
		 * @return this builder
		 */
		B issuedAt(ZonedDateTime iat);
		
		/**
		 * <p>
		 * Specifies the JWT id claim.
		 * </p>
		 * 
		 * @param jti the JWT id claim value
		 * 
		 * @return this builder
		 */
		B jwtId(String jti);
		
		/**
		 * <p>
		 * Specifies a custom claim.
		 * </p>
		 *
		 * @param name  the claim name
		 * @param value the claim value
		 *
		 * @return this builder
		 */
		B addCustomClaim(String name, Object value);
		
		/**
		 * <p>
		 * Builds the JWT Claims set.
		 * </p>
		 * 
		 * @return a new JWT Claims set
		 * 
		 * @throws JWTBuildException if there was an error building the JWT Claims set
		 */
		A build() throws JWTBuildException;
	}
	
	/**
	 * <p>
	 * A generic {@link JWTClaimsSet.Builder} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	private static class GenericJWTClaimsSetBuilder implements JWTClaimsSet.Builder<JWTClaimsSet, GenericJWTClaimsSetBuilder> {

		/**
		 * The issuer claim value as String.
		 */
		private String iss;
		/**
		 * The issuer claim value as URI.
		 */
		private URI iss_uri;
		/**
		 * The subject claim value as String.
		 */
		private String sub;
		/**
		 * The subject claim value as URI.
		 */
		private URI sub_uri;
		/**
		 * The audience claim value.
		 */
		private String aud;
		/**
		 * The expiration time claim value.
		 */
		private ZonedDateTime exp;
		/**
		 * The not before claim value.
		 */
		private ZonedDateTime nbf;
		/**
		 * The issued at claim value.
		 */
		private ZonedDateTime iat;
		/**
		 * The JWT id claim value.
		 */
		private String jti;
		/**
		 * The map of custom claims.
		 */
		private Map<String, Object> customClaims;
		
		/**
		 * <p>
		 * Creates a generic JWT Claims set.
		 * </p>
		 */
		public GenericJWTClaimsSetBuilder() {
		}

		@Override
		public GenericJWTClaimsSetBuilder issuer(String iss) {
			this.iss = iss;
			return this;
		}

		@Override
		public GenericJWTClaimsSetBuilder issuer(URI iss) {
			this.iss_uri = iss;
			return this;
		}

		@Override
		public GenericJWTClaimsSetBuilder subject(String sub) {
			this.sub = sub;
			return this;
		}

		@Override
		public GenericJWTClaimsSetBuilder subject(URI sub) {
			this.sub_uri = sub;
			return this;
		}

		@Override
		public GenericJWTClaimsSetBuilder audience(String aud) {
			this.aud = aud;
			return this;
		}

		@Override
		public GenericJWTClaimsSetBuilder expirationTime(Long exp) {
			this.exp = ZonedDateTime.ofInstant(Instant.ofEpochSecond(exp), ZoneOffset.UTC);
			return this;
		}

		@Override
		public GenericJWTClaimsSetBuilder expirationTime(ZonedDateTime exp) {
			this.exp = exp;
			return this;
		}

		@Override
		public GenericJWTClaimsSetBuilder notBefore(Long nbf) {
			this.nbf = ZonedDateTime.ofInstant(Instant.ofEpochSecond(nbf), ZoneOffset.UTC);
			return this;
		}

		@Override
		public GenericJWTClaimsSetBuilder notBefore(ZonedDateTime nbf) {
			this.nbf = nbf;
			return this;
		}

		@Override
		public GenericJWTClaimsSetBuilder issuedAt(Long iat) {
			this.nbf = ZonedDateTime.ofInstant(Instant.ofEpochSecond(iat), ZoneOffset.UTC);
			return this;
		}

		@Override
		public GenericJWTClaimsSetBuilder issuedAt(ZonedDateTime iat) {
			this.iat = iat;
			return this;
		}

		@Override
		public GenericJWTClaimsSetBuilder jwtId(String jti) {
			this.jti = jti;
			return this;
		}

		@Override
		public GenericJWTClaimsSetBuilder addCustomClaim(String name, Object value) {
			if(this.customClaims == null) {
				this.customClaims = new HashMap<>();
			}
			this.customClaims.put(name, value);
			return this;
		}

		@Override
		public JWTClaimsSet build() throws JWTBuildException {
			return new JWTClaimsSet(this.iss, this.iss_uri, this.sub, this.sub_uri, this.aud, this.exp, this.nbf, this.iat, this.jti, this.customClaims);
		}
	}
}

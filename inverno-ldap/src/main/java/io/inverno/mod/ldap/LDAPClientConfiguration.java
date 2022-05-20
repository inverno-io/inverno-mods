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

import io.inverno.mod.configuration.Configuration;
import java.net.URI;

/**
 * <p>
 * LDAP client configuration.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Configuration( name = "configuration" )
public interface LDAPClientConfiguration {
	
	/**
	 * <p>
	 * Represents the referral policy to apply when encountering a referral.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	enum ReferralPolicy {
		/**
		 * Follow the referral.
		 */
		FOLLOW,
		/**
		 * Ignore the referral.
		 */
		IGNORE,
		/**
		 * Raise an error.
		 */
		THROW;
	}
	
	/**
	 * <p>
	 * The LDAP server URI (e.g. {@code ldap://<host>:<port>/...}
	 * </p>
	 * 
	 * @return a URI
	 */
	URI uri();
	
	/**
	 * <p>
	 * The authentication choice.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code simple}
	 * </p>
	 * 
	 * @return the authentication choice
	 */
	default String authentication() {
		return "simple";
	}
	
	/**
	 * <p>
	 * Indicates whether to follow referrals, ignore them or raise exceptions.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@link ReferralPolicy#FOLLOW}.
	 * </p>
	 * 
	 * @return the referral policy
	 */
	default ReferralPolicy referral() {
		return ReferralPolicy.FOLLOW;
	}
	
	/**
	 * <p>
	 * The admin user DN;
	 * </p>
	 * 
	 * @return the admin user DN
	 */
	String admin_dn();
	
	/**
	 * <p>
	 * The admin user credentials.
	 * </p>
	 * 
	 * @return admin credentials
	 */
	String admin_credentials();
}

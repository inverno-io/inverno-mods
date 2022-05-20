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

import java.util.Optional;
import reactor.core.publisher.Flux;

/**
 * <p>
 * LDAP reactive operations.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface LDAPOperations {
	
	/**
	 * <p>
	 * Represents the serach scope as defined by <a href="https://datatracker.ietf.org/doc/html/rfc4511#section-4.5.1.2">RFC4511 Section 4.5.1.2</a>.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	enum SearchScope {
		/**
		 * The scope is constrained to the entry named by baseObject.
		 */
		BASE_OBJECT,
		/**
		 * The scope is constrained to the immediate subordinates of the entry named by baseObject.
		 */
		SINGLE_LEVEL,
		/**
		 * The scope is constrained to the entry named by baseObject and to all its subordinates.
		 */
		WHOLE_SUBTREE
	}
	
	/**
	 * <p>
	 * Returns the DN bound to the operation.
	 * </p>
	 * 
	 * <p>
	 * This DN should be present when LDAP operations is backed by an authenticated connection (e.g. after a {@link LDAPClient#bind(String, String, java.util.function.Function)} operation).
	 * </p>
	 * 
	 * @return an optional returning the bound DN or an empty optional if the connection is anonymous.
	 */
	Optional<String> getBoundDN();

	/**
	 * <p>
	 * Executes a search operation in the specified base context using the specified filter.
	 * </p>
	 * 
	 * @param base   the base context
	 * @param filter a filter
	 * 
	 * @return a publisher of entries
	 * 
	 * @throws LDAPException if there was an error during the search operation
	 */
	Flux<LDAPEntry> search(String base, String filter) throws LDAPException;
	
	/**
	 * <p>
	 * Executes a search operation in the specified base context using the specified filter and arguments.
	 * </p>
	 * 
	 * @param base       the base context
	 * @param filter     a filter
	 * @param filterArgs the arguments to use to format the filter
	 * 
	 * @return a publisher of entries
	 * 
	 * @throws LDAPException if there was an error during the search operation
	 */
	Flux<LDAPEntry> search(String base, String filter, Object... filterArgs) throws LDAPException;
	
	/**
	 * <p>
	 * Executes a search operation in the specified base context using the specified filter returning the specified list of attributes.
	 * </p>
	 * 
	 * @param base       the base context
	 * @param attributes the list of attributes to query
	 * @param filter     a filter
	 * 
	 * @return a publisher of entries
	 * 
	 * @throws LDAPException if there was an error during the search operation
	 */
	Flux<LDAPEntry> search(String base, String[] attributes, String filter) throws LDAPException;
	
	/**
	 * <p>
	 * Executes a search operation in the specified base context using the specified filter and arguments returning the specified list of attributes.
	 * </p>
	 * 
	 * @param base       the base context
	 * @param attributes the list of attributes to query
	 * @param filter     a filter
	 * @param filterArgs the arguments to use to format the filter
	 * 
	 * @return a publisher of entries
	 * 
	 * @throws LDAPException if there was an error during the search operation
	 */
	Flux<LDAPEntry> search(String base, String[] attributes, String filter, Object... filterArgs) throws LDAPException;
	
	/**
	 * <p>
	 * Returns a builder used to build complex search operations.
	 * </p>
	 * 
	 * @return a search builder
	 */
	SearchBuilder search();
	
	/**
	 * <p>
	 * A search builder is used to build complex search operations.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	interface SearchBuilder {
		
		/**
		 * <p>
		 * Specifies the search scope.
		 * </p>
		 * 
		 * @param scope a scope
		 * 
		 * @return this builder
		 */
		SearchBuilder scope(SearchScope scope);
		
		/**
		 * <p>
		 * Specifies whether aliases should be dereferenced.
		 * </p>
		 * 
		 * @return this builder
		 */
		SearchBuilder derefAliases();
		
		/**
		 * <p>
		 * Specified the size limit.
		 * </p>
		 * 
		 * @param sizeLimit the size limit
		 * 
		 * @return this builder
		 */
		SearchBuilder sizeLimit(int sizeLimit);
		
		/**
		 * <p>
		 * specifies the time limit.
		 * </p>
		 * 
		 * @param timeLimit the time limit
		 * 
		 * @return this builder
		 */
		SearchBuilder timeLimit(int timeLimit);
		
		/**
		 * <p>
		 * Specifies whether only types should be returned.
		 * </p>
		 * 
		 * @return this builder
		 */
		SearchBuilder typesOnly();
		
		/**
		 * <p>
		 * Executes a search operation in the specified base context using the specified filter.
		 * </p>
		 * 
		 * @param base   the base context
		 * @param filter a filter
		 * 
		 * @return a publisher of entries
		 * 
		 * @throws LDAPException if there was an error during the search operation
		 */
		Flux<LDAPEntry> build(String base, String filter) throws LDAPException;
		
		/**
		 * <p>
		 * Executes a search operation in the specified base context using the specified filter and arguments.
		 * </p>
		 * 
		 * @param base       the base context
		 * @param filter     a filter
		 * @param filterArgs the arguments to use to format the filter
		 * 
		 * @return a publisher of entries
		 * 
		 * @throws LDAPException if there was an error during the search operation
		 */
		Flux<LDAPEntry> build(String base, String filter, Object... filterArgs) throws LDAPException;
		
		/**
		 * <p>
		 * Executes a search operation in the specified base context using the specified filter returning the specified list of attributes.
		 * </p>
		 * 
		 * @param base       the base context
		 * @param attributes the list of attributes to query
		 * @param filter     a filter
		 * 
		 * @return a publisher of entries
		 * 
		 * @throws LDAPException if there was an error during the search operation
		 */
		Flux<LDAPEntry> build(String base, String[] attributes, String filter) throws LDAPException;
		
		/**
		 * <p>
		 * Executes a search operation in the specified base context using the specified filter and arguments returning the specified list of attributes.
		 * </p>
		 * 
		 * @param base       the base context
		 * @param attributes the list of attributes to query
		 * @param filter     a filter
		 * @param filterArgs the arguments to use to format the filter
		 * 
		 * @return a publisher of entries
		 * 
		 * @throws LDAPException if there was an error during the search operation
		 */
		Flux<LDAPEntry> build(String base, String[] attributes, String filter, Object... filterArgs) throws LDAPException;
	}
}

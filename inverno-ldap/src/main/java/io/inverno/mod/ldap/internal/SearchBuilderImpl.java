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
package io.inverno.mod.ldap.internal;

import io.inverno.mod.ldap.LDAPEntry;
import io.inverno.mod.ldap.LDAPException;
import io.inverno.mod.ldap.LDAPOperations;
import io.inverno.mod.ldap.LDAPOperations.SearchBuilder;
import io.inverno.mod.ldap.LDAPOperations.SearchScope;
import java.util.LinkedList;
import java.util.List;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * <p>
 * {@link LDAPOperations.SearchBuilder} implementation based on the JDK.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class SearchBuilderImpl implements LDAPOperations.SearchBuilder {

	/**
	 * The underlying dir context.
	 */
	private final DirContext context;
	/**
	 * The underlying dir context when created from the {@link GenericLDAPClient}.
	 */
	private final Mono<DirContext> contextMono;
	/**
	 * The scheduler used to execute blocking operations.
	 */
	private final Scheduler scheduler;
	
	/**
	 * The search scope.
	 */
	private SearchScope scope;
	/**
	 * true to defer aliases, false or null otherwise.
	 */
	private Boolean derefAliases;
	/**
	 * The size limit.
	 */
	private Integer sizeLimit;
	/**
	 * The time limit.
	 */
	private Integer timeLimit;
	/**
	 * true to return only types, false or null otherwise.
	 */
	private Boolean typeOnly;
	
	/**
	 * <p>
	 * Creates a search builder.
	 * </p>
	 * 
	 * <p>
	 * This is used from {@link GenericLDAPOperations} where the dir context is already created.
	 * </p>
	 * 
	 * @param context   a dir context
	 * @param scheduler a scheduler
	 */
	public SearchBuilderImpl(DirContext context, Scheduler scheduler) {
		this.context = context;
		this.contextMono = null;
		this.scheduler = scheduler;
	}

	/**
	 * <p>
	 * Creates a search builder.
	 * </p>
	 * 
	 * <p>
	 * This is used from {@link GenericLDAPClient} where the dir context may not yet be created.
	 * </p>
	 * 
	 * @param context   a dir context
	 * @param scheduler a scheduler
	 */
	public SearchBuilderImpl(Mono<DirContext> context, Scheduler scheduler) {
		this.context = null;
		this.contextMono = context;
		this.scheduler = scheduler;
	}
	
	@Override
	public SearchBuilder scope(SearchScope scope) {
		this.scope = scope;
		return this;
	}

	@Override
	public SearchBuilder derefAliases() {
		this.derefAliases = true;
		return this;
	}

	@Override
	public SearchBuilder sizeLimit(int sizeLimit) {
		this.sizeLimit = sizeLimit;
		return this;
	}

	@Override
	public SearchBuilder timeLimit(int timeLimit) {
		this.timeLimit = timeLimit;
		return this;
	}

	@Override
	public SearchBuilder typesOnly() {
		this.typeOnly = true;
		return this;
	}

	/**
	 * <p>
	 * Creates the search controls.
	 * </p>
	 * 
	 * @param attributes the attributes to query
	 * 
	 * @return search controls
	 */
	protected SearchControls buildSearchControls(String[] attributes) {
		SearchControls controls = new SearchControls();
		if(this.scope != null) {
			switch(this.scope) {
				case BASE_OBJECT: controls.setSearchScope(SearchControls.OBJECT_SCOPE);
					break;
				case SINGLE_LEVEL: controls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
					break;
				case WHOLE_SUBTREE: controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
					break;
				default: throw new IllegalStateException("Unsupported scope: " + this.scope);
			}
		}
		
		if(this.derefAliases != null) {
			controls.setDerefLinkFlag(this.derefAliases);
		}
		if(this.sizeLimit != null) {
			controls.setCountLimit(this.sizeLimit);
		}
		if(this.timeLimit != null) {
			controls.setTimeLimit(this.timeLimit);
		}
		if(this.typeOnly != null) {
			controls.setReturningObjFlag(!this.typeOnly);
		}
		
		controls.setReturningAttributes(attributes);
		
		return controls;
	}
	
	@Override
	public Flux<LDAPEntry> build(String base, String filter) throws LDAPException {
		return this.build(base, null, filter, (Object[])null);
	}
	
	@Override
	public Flux<LDAPEntry> build(String base, String filter, Object... filterArgs) throws LDAPException {
		return this.build(base, null, filter, filterArgs);
	}

	@Override
	public Flux<LDAPEntry> build(String base, String[] attributes, String filter) throws LDAPException {
		return this.build(base, attributes, filter, (Object[])null);
	}
	
	@Override
	public Flux<LDAPEntry> build(String base, String[] attributes, String filter, Object... filterArgs) throws LDAPException {
		if(this.context != null) {
			return this.build(this.context, base, attributes, filter, filterArgs); 
		}
		else {
			return this.contextMono.flatMapMany(localContext -> this.build(this.context, base, attributes, filter, filterArgs));
		}
	}
	
	/**
	 * <p>
	 * Creates and return the search operation.
	 * </p>
	 * 
	 * @param localContext the dir context
	 * @param base         the base DN
	 * @param attributes   the attributes to query
	 * @param filter       the filter
	 * @param filterArgs   the filter arguments
	 * 
	 * @return a publisher of entries
	 * 
	 * @throws LDAPException if there was an error during the search operation
	 */
	private Flux<LDAPEntry> build(DirContext localContext, String base, String[] attributes, String filter, Object... filterArgs) throws LDAPException {
		return Flux.fromStream(() -> {
			try {
				List<LDAPEntry> entries = new LinkedList<>();
				
				NamingEnumeration<SearchResult> result = localContext.search(base, filter, filterArgs, this.buildSearchControls(attributes));
				while(result.hasMore()) {
					entries.add(new GenericLDAPEntry(result.next()));
				}
				
				return entries.stream();
			}
			catch (NamingException e) {
				throw new JdkLDAPException(e);
			}
		})
		.subscribeOn(this.scheduler);
	}
}

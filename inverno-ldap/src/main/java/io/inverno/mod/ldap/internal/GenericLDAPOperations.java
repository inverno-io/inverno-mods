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
import java.util.Optional;
import javax.naming.directory.DirContext;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

/**
 * <p>
 * Generic {@link LDAPOperations} implementation based on the JDK.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericLDAPOperations implements LDAPOperations {

	/**
	 * The underlying dir context.
	 */
	private final DirContext context;

	/**
	 * The scheduler used to execute blocking operations.
	 */
	private final Scheduler scheduler;
	
	/**
	 * The DN that was bound when creating the dir context. 
	 */
	private final Optional<String> boundDN;
	
	/**
	 * <p>
	 * Creates a generic LDAP operations with the specified dir context and scheduler.
	 * </p>
	 * 
	 * @param context   a dir context
	 * @param scheduler a scheduler
	 */
	public GenericLDAPOperations(DirContext context, Scheduler scheduler) {
		this(context, scheduler, null);
	}
	
	/**
	 * <p>
	 * Creates a generic LDAP operations with the specified dir context, scheduler and bound DN.
	 * </p>
	 * 
	 * @param context   a dir context
	 * @param scheduler a scheduler
	 * @param boundDN   the bound DN
	 */
	public GenericLDAPOperations(DirContext context, Scheduler scheduler, String boundDN) {
		this.context = context;
		this.scheduler = scheduler;
		this.boundDN = Optional.ofNullable(boundDN);
	}
	
	@Override
	public Optional<String> getBoundDN() {
		return boundDN;
	}

	@Override
	public Flux<LDAPEntry> search(String base, String filter) {
		return this.search().scope(SearchScope.WHOLE_SUBTREE).build(base, null, filter, (Object[])null);
	}

	@Override
	public Flux<LDAPEntry> search(String base, String filter, Object... filterArgs) throws LDAPException {
		return this.search().scope(SearchScope.WHOLE_SUBTREE).build(base, null, filter, filterArgs);
	}
	
	@Override
	public Flux<LDAPEntry> search(String base, String[] attributes, String filter) {
		return this.search().scope(SearchScope.WHOLE_SUBTREE).build(base, attributes, filter, (Object[])null);
	}

	@Override
	public Flux<LDAPEntry> search(String base, String[] attributes, String filter, Object... filterArgs) throws LDAPException {
		return this.search().scope(SearchScope.WHOLE_SUBTREE).build(base, attributes, filter, filterArgs);
	}

	@Override
	public SearchBuilder search() {
		return new SearchBuilderImpl(this.context, this.scheduler);
	}
}

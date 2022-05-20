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

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Destroy;
import io.inverno.core.annotation.Init;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.ldap.LDAPClient;
import io.inverno.mod.ldap.LDAPClientConfiguration;
import io.inverno.mod.ldap.LDAPEntry;
import io.inverno.mod.ldap.LDAPException;
import io.inverno.mod.ldap.LDAPOperations;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * <p>
 * Generic {@link LDAPClient} implemenation based on the JDK.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Bean( name = "jdkLdapClient" )
public class GenericLDAPClient implements @Provide LDAPClient {

	private final LDAPClientConfiguration configuration;
	private final Scheduler scheduler;
	
	private Map<String, String> environment;
	private Mono<DirContext> context;
	private Optional<String> boundDN;
	
	/**
	 * <p>
	 * Creates a generic LDAP client.
	 * </p>
	 * 
	 * @param configuration the LDAP module configuration
	 * @param executor an executor service
	 */
	public GenericLDAPClient(LDAPClientConfiguration configuration, ExecutorService executor) {
		this.configuration = configuration;
		this.scheduler = Schedulers.fromExecutor(executor);
	}
	
	@Init
	public void init() throws LDAPException {
		this.environment = new HashMap<>();
		this.environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		if(this.configuration.uri() == null) {
			throw new JdkLDAPException("Missing LDAP server URI");
		}
		else {
			this.environment.put(Context.PROVIDER_URL, this.configuration.uri().normalize().toString());
		}
		
		if(this.configuration.authentication() != null) {
			this.environment.put(Context.SECURITY_AUTHENTICATION, this.configuration.authentication());
		}
		
		if(this.configuration.referral() != null) {
			switch(this.configuration.referral()) {
				case FOLLOW: this.environment.put(Context.REFERRAL, "follow");
					break;
				case IGNORE: this.environment.put(Context.REFERRAL, "ignore");
					break;
				case THROW: this.environment.put(Context.REFERRAL, "throw");
					break;
				default: throw new IllegalStateException("Unsupported referral policy: " + this.configuration.referral());
			}
		}
		this.context = this.context(this.configuration.admin_dn(), this.configuration.admin_credentials())
			.doOnNext(ctx -> this.boundDN = Optional.ofNullable(this.configuration.admin_dn()))
			.cache();
	}
	
	@Destroy
	public void destroy() {
		this.close();
	}
	
	@Override
	public Optional<String> getBoundDN() {
		return this.boundDN;
	}
	
	/**
	 * <p>
	 * Returns the dir context associated to the client.
	 * </p>
	 * 
	 * @return the dir context
	 * 
	 * @throws LDAPException if the client has not been initialized
	 */
	protected Mono<DirContext> getContext() throws LDAPException {
		if(this.context == null) {
			throw new JdkLDAPException("LDAP client not initialized");
		}
		return this.context;
	}
	
	/**
	 * <p>
	 * Creates and return a dir context by authenticating the specified credentials.
	 * </p>
	 * 
	 * @param dn the user DN
	 * @param credentials the user credentials
	 * 
	 * @return an authenticated dir context
	 * 
	 * @throws LDAPException if there was an error creating the dir context
	 */
	protected Mono<DirContext> context(String dn, String credentials) throws LDAPException {
		return Mono.fromSupplier(() -> {
			try {
				Hashtable<String, String> environment = new Hashtable<>(this.environment);
				
				if(dn != null) {
					environment.put(Context.SECURITY_PRINCIPAL, dn);
				}
				if(credentials != null) {
					environment.put(Context.SECURITY_CREDENTIALS, credentials);
				}
				
				return new InitialDirContext(environment);
			}
			catch(NamingException e) {
				throw new JdkLDAPException(e);
			}
		});
	}
	
	@Override
	public Flux<LDAPEntry> search(String base, String filter) throws LDAPException {
		return this.search(base, null, filter, (Object[])null);
	}
	
	@Override
	public Flux<LDAPEntry> search(String base, String filter, Object... filterArgs) throws LDAPException {
		return this.search(base, null, filter, filterArgs);
	}

	@Override
	public Flux<LDAPEntry> search(String base, String[] attributes, String filter) throws LDAPException {
		return this.search(base, attributes, filter, (Object[])null);
	}

	@Override
	public Flux<LDAPEntry> search(String base, String[] attributes, String filter, Object... filterArgs) throws LDAPException {
		return this.context.flatMapMany(localContext -> new GenericLDAPOperations(localContext, this.scheduler).search(base, attributes, filter, filterArgs));
	}
	
	@Override
	public SearchBuilder search() {
		return new SearchBuilderImpl(this.getContext(), this.scheduler);
	}

	@Override
	public <T> Publisher<T> bind(String dn, String credentials, Function<LDAPOperations, Publisher<T>> function) throws LDAPException {
		return Flux.usingWhen(
			this.context(dn, credentials), 
			ctx -> Mono.just(new GenericLDAPOperations(ctx, this.scheduler, dn)).flatMapMany(function), 
			ctx -> Mono.fromRunnable(() -> {
				try {
					ctx.close();
				} 
				catch (NamingException e) {
					throw new JdkLDAPException(e);
				}
			})
		);
	}
	
	@Override
	public <T> Publisher<T> bind(String dn, Object[] dnArgs, String credentials, Function<LDAPOperations, Publisher<T>> function) throws LDAPException {
		String formattedDN = LDAPUtils.format(dn, dnArgs);
		return Flux.usingWhen(
			this.context(formattedDN, credentials), 
			ctx -> Mono.just(new GenericLDAPOperations(ctx, this.scheduler, formattedDN)).flatMapMany(function), 
			ctx -> Mono.fromRunnable(() -> {
				try {
					ctx.close();
				} 
				catch (NamingException e) {
					throw new JdkLDAPException(e);
				}
			})
		);
	}

	@Override
	public Mono<Void> close() throws LDAPException {
		return this.getContext()
			.doOnNext(ctx -> {
				try {
					ctx.close();
				} 
				catch (NamingException e) {
					throw new JdkLDAPException(e);
				}
			})
			.then();
	}
}

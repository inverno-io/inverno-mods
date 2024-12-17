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

/**
 * <p>
 * The Inverno framework LDAP module provides a reactive API to query an LDAP server.
 * </p>
 * 
 * <p>
 * The module also provides a basic implementation using {@link java.naming} module.
 * </p>
 * 
 * <p>
 * It defines the following sockets:
 * </p>
 * 
 * <dl>
 * <dt><b>configuration</b></dt>
 * <dd>the LDAP module configuration</dd>
 * <dt><b>workerPool</b></dt>
 * <dd>The {@link java.util.concurrent.ExecutorService} used to execute blocking operations.</dd>
 * </dl>
 * 
 * <p>
 * It exposes the following beans:
 * </p>
 * 
 * <dl>
 * <dt><b>configuration</b></dt>
 * <dd>the LDAP module configuration</dd>
 * <dt><b>jdkLdapClient</b></dt>
 * <dd>The {@link io.inverno.mod.ldap.LDAPClient} used to execute blocking operations.</dd>
 * </dl>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@io.inverno.core.annotation.Module
module io.inverno.mod.ldap {
	requires io.inverno.core;
	requires static io.inverno.core.annotation; // for javadoc...

	requires io.inverno.mod.base;
	requires transitive io.inverno.mod.configuration;
	
	requires java.naming;
	requires transitive org.reactivestreams;
	requires transitive reactor.core;
	
	exports io.inverno.mod.ldap;
}

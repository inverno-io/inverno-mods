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

/**
 * <p>
 * The Inverno framework HTTP security module provides support for securing access to HTTP endpoints.
 * </p>
 * 
 * <p>
 * This module extends the security module and provides components to secure services and resources accessed by HTTP. It provides:
 * </p>
 * 
 * <ul>
 * <li>basic HTTP authentication (<a href="https://datatracker.ietf.org/doc/html/rfc7617">RFC 7617</a>)</li>
 * <li>digest HTTP authentication (<a href="https://datatracker.ietf.org/doc/html/rfc7616">RFC 7616</a>)</li>
 * <li>token based authentication</li>
 * <li>session based authentication</li>
 * <li>Cross-origin resource sharing (CORS) as defined by <a href="https://fetch.spec.whatwg.org/#http-cors-protocol">HTTP CORS protocol</a></li>
 * <li>Cross-site request forgery attack protection</li>
 * </ul>
 * 
 * <p>
 * HTTP endpoints are protected using combinations of security interceptors. More specifically, the {@link io.inverno.mod.security.http.SecurityInterceptor} shall always be used on protected services
 * or resources, just like the {@link io.inverno.mod.security.SecurityManager}, its role is to authenticate the credentials provided in the request and creates the security context in the exchange
 * context. An {@link io.inverno.mod.security.http.AccessControlInterceptor} can then be used to control the access to the protected services or resources using the security context. An
 * {@link io.inverno.mod.security.http.AuthenticationErrorInterceptor} can be used to intercept {@code UNAUTHORIZED(401)} errors and request for authentication. For instance, such interceptor can send
 * HTTP basic or digest challenges in the response, or it can redirect the client to a login form.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@io.inverno.core.annotation.Module( excludes = { "io.inverno.mod.http.server" } )
module io.inverno.mod.security.http {
	requires io.inverno.core;
	requires static io.inverno.core.annotation; // for javadoc...
	
	requires io.inverno.mod.base;
    requires transitive io.inverno.mod.http.server;
	requires io.inverno.mod.irt;
	requires transitive io.inverno.mod.security;
	requires static io.inverno.mod.session.http;

	requires com.fasterxml.jackson.databind;
	requires org.apache.commons.codec;
    requires org.apache.commons.lang3;
    requires org.apache.commons.text;
	requires org.apache.logging.log4j;
	requires transitive org.reactivestreams;
	requires transitive reactor.core;
	
    exports io.inverno.mod.security.http;
	exports io.inverno.mod.security.http.basic;
	exports io.inverno.mod.security.http.context;
	exports io.inverno.mod.security.http.cors;
	exports io.inverno.mod.security.http.csrf;
	exports io.inverno.mod.security.http.digest;
	exports io.inverno.mod.security.http.form;
	exports io.inverno.mod.security.http.login;
	exports io.inverno.mod.security.http.session;
	exports io.inverno.mod.security.http.session.jwt;
	exports io.inverno.mod.security.http.token;
}

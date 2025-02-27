/*
 * Copyright 2025 Jeremy KUHN
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
 * The Inverno framework HTTP session module provides session support in an HTTP server.
 * </p>
 *
 * <p>
 * This module extends the session module and provides components to expose session in HTTP endpoints.
 * </p>
 *
 * <p>
 * The {@link io.inverno.mod.session.http.SessionInterceptor} shall be applied on session aware endpoints to expose the session in the {@link io.inverno.mod.session.http.context.SessionContext}. It is
 * responsible for creating the session when requested on the session context, resolving the session using a {@link io.inverno.mod.session.SessionIdGenerator} to extract the session identifier
 * from the request, injecting the session identifier into the server response using a {@link io.inverno.mod.session.http.SessionInjector} and eventually saving the session after the request has been
 * processed.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 */
module io.inverno.mod.session.http {
	requires transitive io.inverno.mod.session;
	requires transitive io.inverno.mod.http.server;

	requires transitive org.reactivestreams;
	requires transitive reactor.core;
	requires org.apache.logging.log4j;

	exports io.inverno.mod.session.http;
	exports io.inverno.mod.session.http.context;
	exports io.inverno.mod.session.http.context.jwt;
}

/*
 * Copyright 2020 Jeremy KUHN
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
 * The Inverno framework web compiler module provides an Inverno compiler plugin to generate a {@link io.inverno.mod.web.server.WebServer} bean that aggregates all web controller beans and web router
 * configurer beans defined in the module.
 * </p>
 * 
 * <p>
 * This generated web router configurer bean can then be used to configure the web router of a web server module instance.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see io.inverno.mod.web.server.annotation.WebController
 * @see io.inverno.mod.web.server.annotation.WebRoutes
 */
module io.inverno.mod.web.compiler {
	requires transitive io.inverno.core.compiler;

	requires io.inverno.mod.base;
	requires io.inverno.mod.http.base;
	requires io.inverno.mod.http.client;
	requires io.inverno.mod.http.server;
	requires io.inverno.mod.web.base;
	requires io.inverno.mod.web.client;
	requires io.inverno.mod.web.server;

	requires jdk.javadoc;
	requires org.apache.commons.lang3;
	requires org.apache.commons.text;
	requires org.reactivestreams;
	requires reactor.core;

	exports io.inverno.mod.web.compiler.spi;
	exports io.inverno.mod.web.compiler.spi.server;

	provides io.inverno.core.compiler.spi.plugin.CompilerPlugin with io.inverno.mod.web.compiler.internal.client.WebClientCompilerPlugin, io.inverno.mod.web.compiler.internal.server.WebServerCompilerPlugin;
}

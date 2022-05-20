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
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@io.inverno.core.annotation.Module( excludes = { "io.inverno.mod.http.server" } )
module io.inverno.mod.security.http {
	requires io.inverno.core;
	requires static io.inverno.core.annotation; // for javadoc...
	
	requires io.inverno.mod.base;
    requires transitive io.inverno.mod.security;
    requires transitive io.inverno.mod.http.server;
	requires io.inverno.mod.irt;

    requires transitive reactor.core;
    requires transitive org.reactivestreams;
	requires org.apache.commons.codec;
    requires org.apache.commons.lang3;
    requires org.apache.commons.text;
	requires org.apache.logging.log4j;

    exports io.inverno.mod.security.http;
	exports io.inverno.mod.security.http.basic;
	exports io.inverno.mod.security.http.context;
	exports io.inverno.mod.security.http.digest;
	exports io.inverno.mod.security.http.form;
	exports io.inverno.mod.security.http.token;
}

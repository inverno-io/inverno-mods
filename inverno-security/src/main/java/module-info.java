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
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
module io.inverno.mod.security {
	
	requires com.fasterxml.jackson.databind;
	requires org.apache.commons.lang3;
	requires transitive org.reactivestreams;
	requires transitive reactor.core;

	exports io.inverno.mod.security.internal.authentication to com.fasterxml.jackson.databind;
	exports io.inverno.mod.security.internal.identity to com.fasterxml.jackson.databind;
	
	exports io.inverno.mod.security;
	exports io.inverno.mod.security.authentication;
	exports io.inverno.mod.security.accesscontrol;
	exports io.inverno.mod.security.context;
	exports io.inverno.mod.security.identity;
}

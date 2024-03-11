/*
 * Copyright 2024 Jeremy KUHN
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
 * @since 1.8
 */
@io.inverno.core.annotation.Module
module io.inverno.grpc.server {
	requires io.inverno.core;
	requires static io.inverno.core.annotation; // for javadoc...
	
	requires transitive io.inverno.mod.base;
	requires io.inverno.mod.configuration;
	requires transitive io.inverno.mod.http.server;
	
	requires io.grpc;
	
	exports io.inverno.grpc.server;
}

/*
 * Copyright 2021 Jeremy KUHN
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
 * The Inverno framework configuration compiler module provides an Inverno compiler plugin to generate configuration loader for {@link io.inverno.mod.configuration.Configuration @Configuration}
 * annotated interfaces.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see io.inverno.mod.configuration.Configuration
 */
module io.inverno.mod.configuration.compiler {
	requires transitive io.inverno.core.compiler;

	requires io.inverno.mod.configuration;
	
	exports io.inverno.mod.configuration.compiler.spi;
	
	provides io.inverno.core.compiler.spi.plugin.CompilerPlugin with io.inverno.mod.configuration.compiler.internal.ConfigurationCompilerPlugin;
}

import io.winterframework.mod.configuration.Configuration;

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
 * The Winter framework configuration compiler module provides a Winter compiler
 * plugin to generate configuration loader for
 * {@link Configuration @Configuration} annotated interfaces.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Configuration
 */
module io.winterframework.mod.configuration.compiler {
	requires transitive io.winterframework.core.compiler;
	
	requires io.winterframework.mod.configuration;
	
	exports io.winterframework.mod.configuration.compiler.spi;
	
	provides io.winterframework.core.compiler.spi.plugin.CompilerPlugin with io.winterframework.mod.configuration.compiler.internal.ConfigurationCompilerPlugin;
}

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
 * The Inverno framework web compiler module provides an Inverno compiler plugin to compile Inverno reactive templates ({@code *.irt}).
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
module io.inverno.mod.irt.compiler {
	requires transitive io.inverno.core.compiler;

	requires io.inverno.mod.base;
	requires io.inverno.mod.irt;
	
	requires org.apache.commons.lang3;
	requires org.apache.commons.text;
	
	provides io.inverno.core.compiler.spi.plugin.CompilerPlugin with io.inverno.mod.irt.compiler.internal.IrtCompilerPlugin;
}

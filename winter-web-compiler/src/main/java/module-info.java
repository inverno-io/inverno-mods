import io.winterframework.mod.web.WebRouterConfigurer;
import io.winterframework.mod.web.annotation.WebController;
import io.winterframework.mod.web.annotation.WebRoutes;

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
 * The Winter framework web compiler module provides a Winter compiler plugin to
 * generate a {@link WebRouterConfigurer} implementation that aggregates all web
 * controller beans and web router configurer beans defined in the module.
 * </p>
 * 
 * <p>
 * This generated web router configurer bean can then be used to configure the
 * web router of a web module instance.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see WebController
 * @see WebRoutes
 */
module io.winterframework.mod.web.compiler {
	requires transitive io.winterframework.core.compiler;
	
	requires io.winterframework.mod.base;
	requires transitive io.winterframework.mod.http.base;
	requires io.winterframework.mod.http.server;
	requires io.winterframework.mod.web;
	
	requires reactor.core;
	requires org.reactivestreams;
	
	exports io.winterframework.mod.web.compiler.spi;
	
	provides io.winterframework.core.compiler.spi.plugin.CompilerPlugin with io.winterframework.mod.web.compiler.internal.WebRouterConfigurerCompilerPlugin;
}
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
package io.winterframework.mod.web.compiler.spi;

/**
 * @author jkuhn
 *
 */
public interface WebRouterConfigurerInfoVisitor<R, P> {

	R visit(WebRouterConfigurerInfo routerConfigurerInfo, P p);
	
	R visit(WebProvidedRouterConfigurerInfo providedRouterConfigurerInfo, P p);
	
	R visit(WebControllerInfo controllerInfo, P p);
	
	R visit(WebRouteInfo routeInfo, P p);
	
	R visit(WebResponseBodyInfo responseBodyInfo, P p);
	
	R visit(WebParameterInfo parameterInfo, P p);
	
	R visit(WebBasicParameterInfo basicParameterInfo, P p);
	
	R visit(WebCookieParameterInfo cookieParameterInfo, P p);
	
	R visit(WebFormParameterInfo formParameterInfo, P p);
	
	R visit(WebHeaderParameterInfo headerParameterInfo, P p);
	
	R visit(WebPathParameterInfo pathParameterInfo, P p);
	
	R visit(WebQueryParameterInfo queryParameterInfo, P p);
	
	R visit(WebRequestBodyParameterInfo bodyParameterInfo, P p);
	
	R visit(WebExchangeParameterInfo exchangeParameterInfo, P p);
	
	R visit(WebSseEventFactoryParameterInfo sseEventFactoryParameterInfo, P p);
}


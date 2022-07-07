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
package io.inverno.mod.web.compiler.spi;

/**
 * <p>
 * A web router configurer info visitor is used to process a web router
 * configurer info.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @param <R> the visitor result type
 * @param <P> the visitor parameter type
 */
public interface WebServerControllerConfigurerInfoVisitor<R, P> {

	/**
	 * <p>
	 * Visits web server controller configurer info.
	 * </p>
	 *
	 * @param controllerConfigurerInfo the info to visit
	 * @param p					       a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebServerControllerConfigurerInfo controllerConfigurerInfo, P p);

	/**
	 * <p>
	 * Visits web interceptors configurer info.
	 * </p>
	 *
	 * @param interceptorsConfigurerInfo the info to visit
	 * @param p                          a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebInterceptorsConfigurerInfo interceptorsConfigurerInfo, P p);

	/**
	 * <p>
	 * Visits web routes configurer info.
	 * </p>
	 *
	 * @param routesConfigurerInfo the info to visit
	 * @param p                    a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebRoutesConfigurerInfo routesConfigurerInfo, P p);

	/**
	 * <p>
	 * Visits web router configurer info.
	 * </p>
	 *
	 * @param routerConfigurerInfo the info to visit
	 * @param p                    a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebRouterConfigurerInfo routerConfigurerInfo, P p);
	
	/**
	 * <p>
	 * Visits error web interceptors configurer info.
	 * </p>
	 *
	 * @param errorInterceptorsConfigurerInfo the info to visit
	 * @param p                               a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(ErrorWebInterceptorsConfigurerInfo errorInterceptorsConfigurerInfo, P p);

	/**
	 * <p>
	 * Visits error web routes configurer info.
	 * </p>
	 *
	 * @param errorRoutesConfigurerInfo the info to visit
	 * @param p                         a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(ErrorWebRoutesConfigurerInfo errorRoutesConfigurerInfo, P p);

	/**
	 * <p>
	 * Visits error web router configurer info.
	 * </p>
	 *
	 * @param errorRouterConfigurerInfo the info to visit
	 * @param p                         a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(ErrorWebRouterConfigurerInfo errorRouterConfigurerInfo, P p);
	
	/**
	 * <p>
	 * Visits web controller info.
	 * </p>
	 *
	 * @param controllerInfo the info to visit
	 * @param p              a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebControllerInfo controllerInfo, P p);

	/**
	 * <p>
	 * Visits web route info.
	 * </p>
	 *
	 * @param routeInfo the info to visit
	 * @param p         a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebRouteInfo routeInfo, P p);

	/**
	 * <p>
	 * Visits web response body info.
	 * </p>
	 *
	 * @param responseBodyInfo the info to visit
	 * @param p                a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebResponseBodyInfo responseBodyInfo, P p);

	/**
	 * <p>
	 * Visits web parameter info.
	 * </p>
	 *
	 * @param parameterInfo the info to visit
	 * @param p             a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebParameterInfo parameterInfo, P p);

	/**
	 * <p>
	 * Visits web basic parameter info.
	 * </p>
	 *
	 * @param basicParameterInfo the info to visit
	 * @param p                  a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebBasicParameterInfo basicParameterInfo, P p);

	/**
	 * <p>
	 * Visits web cookie parameter info.
	 * </p>
	 *
	 * @param cookieParameterInfo the info to visit
	 * @param p                   a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebCookieParameterInfo cookieParameterInfo, P p);

	/**
	 * <p>
	 * Visits web form parameter info.
	 * </p>
	 *
	 * @param formParameterInfo the info to visit
	 * @param p                 a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebFormParameterInfo formParameterInfo, P p);

	/**
	 * <p>
	 * Visits web header parameter info.
	 * </p>
	 *
	 * @param headerParameterInfo the info to visit
	 * @param p                   a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebHeaderParameterInfo headerParameterInfo, P p);

	/**
	 * <p>
	 * Visits web path parameter info.
	 * </p>
	 *
	 * @param pathParameterInfo the info to visit
	 * @param p                 a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebPathParameterInfo pathParameterInfo, P p);

	/**
	 * <p>
	 * Visits web query parameter info.
	 * </p>
	 *
	 * @param queryParameterInfo the info to visit
	 * @param p                  a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebQueryParameterInfo queryParameterInfo, P p);

	/**
	 * <p>
	 * Visits web body parameter info.
	 * </p>
	 *
	 * @param bodyParameterInfo the info to visit
	 * @param p                 a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebRequestBodyParameterInfo bodyParameterInfo, P p);

	/**
	 * <p>
	 * Visits web exchange parameter info.
	 * </p>
	 *
	 * @param exchangeParameterInfo the info to visit
	 * @param p                     a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebExchangeParameterInfo exchangeParameterInfo, P p);

	/**
	 * <p>
	 * Visits web context exchange parameter info.
	 * </p>
	 *
	 * @param exchangeContextParameterInfo the info to visit
	 * @param p                            a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebExchangeContextParameterInfo exchangeContextParameterInfo, P p);

	/**
	 * <p>
	 * Visits web server-sent event factory parameter info.
	 * </p>
	 *
	 * @param sseEventFactoryParameterInfo the info to visit
	 * @param p                            a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebSseEventFactoryParameterInfo sseEventFactoryParameterInfo, P p);
	
	
	
	/**
	 * <p>
	 * Visits WebSocket route info.
	 * </p>
	 *
	 * @param webSocketRouteInfo the info to visit
	 * @param p                  a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebSocketRouteInfo webSocketRouteInfo, P p);
	
	/**
	 * <p>
	 * Visits WebSocket bound publisher info.
	 * </p>
	 *
	 * @param boundPublisherInfo the info to visit
	 * @param p                     a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebSocketBoundPublisherInfo boundPublisherInfo, P p);
	
	/**
	 * <p>
	 * Visits WebSocket outbound publisher info.
	 * </p>
	 *
	 * @param outboundPublisherInfo the info to visit
	 * @param p                     a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebSocketOutboundPublisherInfo outboundPublisherInfo, P p);
	
	/**
	 * <p>
	 * Visits WebSocket parameter info.
	 * </p>
	 *
	 * @param parameterInfo the info to visit
	 * @param p             a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebSocketParameterInfo parameterInfo, P p);
	
	/**
	 * <p>
	 * Visits WebSocket outbound parameter info.
	 * </p>
	 *
	 * @param outboundParameterInfo the info to visit
	 * @param p                     a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebSocketOutboundParameterInfo outboundParameterInfo, P p);
	
	/**
	 * <p>
	 * Visits WebSocket inbound publisher parameter info.
	 * </p>
	 *
	 * @param inboundPublisherParameterInfo the info to visit
	 * @param p                             a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebSocketInboundPublisherParameterInfo inboundPublisherParameterInfo, P p);
	
	/**
	 * <p>
	 * Visits WebSocket inbound parameter info.
	 * </p>
	 *
	 * @param inboundParameterInfo the info to visit
	 * @param p                    a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebSocketInboundParameterInfo inboundParameterInfo, P p);
	
	/**
	 * <p>
	 * Visits WebSocket exchange parameter info.
	 * </p>
	 *
	 * @param exchangeParameterInfo the info to visit
	 * @param p                     a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebSocketExchangeParameterInfo exchangeParameterInfo, P p);
}

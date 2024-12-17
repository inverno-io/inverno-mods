/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.web.compiler.spi.server;

import io.inverno.mod.web.compiler.spi.WebBasicParameterInfo;
import io.inverno.mod.web.compiler.spi.WebCookieParameterInfo;
import io.inverno.mod.web.compiler.spi.WebFormParameterInfo;
import io.inverno.mod.web.compiler.spi.WebHeaderParameterInfo;
import io.inverno.mod.web.compiler.spi.WebParameterInfo;
import io.inverno.mod.web.compiler.spi.WebPathParameterInfo;
import io.inverno.mod.web.compiler.spi.WebQueryParameterInfo;
import io.inverno.mod.web.compiler.spi.WebRequestBodyParameterInfo;
import io.inverno.mod.web.compiler.spi.WebExchangeContextParameterInfo;
import io.inverno.mod.web.compiler.spi.WebExchangeParameterInfo;
import io.inverno.mod.web.compiler.spi.WebSocketBoundPublisherInfo;
import io.inverno.mod.web.compiler.spi.WebSocketExchangeParameterInfo;
import io.inverno.mod.web.compiler.spi.WebSocketOutboundParameterInfo;
import io.inverno.mod.web.compiler.spi.WebSocketParameterInfo;

/**
 * <p>
 * A Web server module info visitor is used to process a Web server module info.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <R> the visitor result type
 * @param <P> the visitor parameter type
 */
public interface WebServerModuleInfoVisitor<R, P> {

	/**
	 * <p>
	 * Visits Web server module info.
	 * </p>
	 *
	 * @param serverInfo the info to visit
	 * @param p          a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebServerModuleInfo serverInfo, P p);

	/**
	 * <p>
	 * Visits Web interceptors configurer info.
	 * </p>
	 *
	 * @param interceptorsConfigurerInfo the info to visit
	 * @param p                          a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebServerRouteInterceptorConfigurerInfo interceptorsConfigurerInfo, P p);

	/**
	 * <p>
	 * Visits Web routes configurer info.
	 * </p>
	 *
	 * @param routesConfigurerInfo the info to visit
	 * @param p                    a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebServerRouterConfigurerInfo routesConfigurerInfo, P p);

	/**
	 * <p>
	 * Visits error Web interceptors configurer info.
	 * </p>
	 *
	 * @param errorInterceptorsConfigurerInfo the info to visit
	 * @param p                               a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(ErrorWebServerRouteInterceptorConfigurerInfo errorInterceptorsConfigurerInfo, P p);

	/**
	 * <p>
	 * Visits error Web routes configurer info.
	 * </p>
	 *
	 * @param errorRoutesConfigurerInfo the info to visit
	 * @param p                         a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(ErrorWebServerRouterConfigurerInfo errorRoutesConfigurerInfo, P p);

	/**
	 * <p>
	 * Visits Web server configurer info.
	 * </p>
	 *
	 * @param serverConfigurerInfo the info to visit
	 * @param p                    a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebServerConfigurerInfo serverConfigurerInfo, P p);

	/**
	 * <p>
	 * Visits Web controller info.
	 * </p>
	 *
	 * @param controllerInfo the info to visit
	 * @param p              a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebServerControllerInfo controllerInfo, P p);

	/**
	 * <p>
	 * Visits Web route info.
	 * </p>
	 *
	 * @param routeInfo the info to visit
	 * @param p         a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebServerRouteInfo routeInfo, P p);

	/**
	 * <p>
	 * Visits Web response body info.
	 * </p>
	 *
	 * @param responseBodyInfo the info to visit
	 * @param p                a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebServerResponseBodyInfo responseBodyInfo, P p);

	/**
	 * <p>
	 * Visits Web parameter info.
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
	 * Visits Web basic parameter info.
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
	 * Visits Web cookie parameter info.
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
	 * Visits Web form parameter info.
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
	 * Visits Web header parameter info.
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
	 * Visits Web path parameter info.
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
	 * Visits Web query parameter info.
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
	 * Visits Web body parameter info.
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
	 * Visits Web exchange parameter info.
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
	 * Visits Web context exchange parameter info.
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
	 * Visits Web server-sent event factory parameter info.
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
	 * @param webSocketServerRouteInfo the info to visit
	 * @param p                  a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebSocketServerRouteInfo webSocketServerRouteInfo, P p);

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
	R visit(WebSocketServerOutboundPublisherInfo outboundPublisherInfo, P p);

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
	R visit(WebSocketServerInboundPublisherParameterInfo inboundPublisherParameterInfo, P p);

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
	R visit(WebSocketServerInboundParameterInfo inboundParameterInfo, P p);

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

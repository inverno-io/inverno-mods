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
package io.inverno.mod.web.compiler.spi.client;

import io.inverno.mod.web.compiler.spi.WebCookieParameterInfo;
import io.inverno.mod.web.compiler.spi.WebExchangeParameterInfo;
import io.inverno.mod.web.compiler.spi.WebFormParameterInfo;
import io.inverno.mod.web.compiler.spi.WebHeaderParameterInfo;
import io.inverno.mod.web.compiler.spi.WebParameterInfo;
import io.inverno.mod.web.compiler.spi.WebPathParameterInfo;
import io.inverno.mod.web.compiler.spi.WebQueryParameterInfo;
import io.inverno.mod.web.compiler.spi.WebRequestBodyParameterInfo;
import io.inverno.mod.web.compiler.spi.WebSocketExchangeParameterInfo;
import io.inverno.mod.web.compiler.spi.WebSocketOutboundParameterInfo;

/**
 * <p>
 * A Web client module info visitor is used to process a Web client module info.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public interface WebClientModuleInfoVisitor<R, P> {

	/**
	 * <p>
	 * Visits Web client module info.
	 * </p>
	 *
	 * @param clientInfo the info to visit
	 * @param p          a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebClientModuleInfo clientInfo, P p);

	/**
	 * <p>
	 * Visits Web route interceptors configurer info.
	 * </p>
	 *
	 * @param interceptorsConfigurerInfo the info to visit
	 * @param p                          a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebClientRouteInterceptorConfigurerInfo interceptorsConfigurerInfo, P p);

	/**
	 * <p>
	 * Visits Web client socket info.
	 * </p>
	 *
	 * @param clientSocketInfo the info to visit
	 * @param p                a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebClientSocketInfo clientSocketInfo, P p);


	/**
	 * <p>
	 * Visits Web client stub info.
	 * </p>
	 *
	 * @param clientStubInfo the info to visit
	 * @param p              a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebClientStubInfo clientStubInfo, P p);

	/**
	 * <p>
	 * Visits Web client route info.
	 * </p>
	 *
	 * @param routeInfo the info to visit
	 * @param p         a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebClientRouteInfo routeInfo, P p);

	/**
	 * <p>
	 * Visits Web client route info.
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
	 * Visits Web part parameter info.
	 * </p>
	 *
	 * @param partParameterInfo the info to visit
	 * @param p                 a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebClientPartParameterInfo partParameterInfo, P p);

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
	 * Visits Web exchange exchange parameter info.
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
	 * Visits return info.
	 * </p>
	 *
	 * @param returnInfo the info to visit
	 * @param p          a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebClientRouteReturnInfo returnInfo, P p);

	/**
	 * <p>
	 * Visits exchange return info.
	 * </p>
	 *
	 * @param exchangeReturnInfo the info to visit
	 * @param p                  a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebClientExchangeReturnInfo exchangeReturnInfo, P p);

	/**
	 * <p>
	 * Visits exchange return info.
	 * </p>
	 *
	 * @param responseReturnInfo the info to visit
	 * @param p                  a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebClientResponseReturnInfo responseReturnInfo, P p);

	/**
	 * <p>
	 * Visits response body return info.
	 * </p>
	 *
	 * @param responseBodyInfo the info to visit
	 * @param p                a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebClientResponseBodyInfo responseBodyInfo, P p);

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
	R visit(WebSocketClientRouteInfo webSocketRouteInfo, P p);

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
	 * Visits WebSocket outbound publisher parameter info.
	 * </p>
	 *
	 * @param outboundPublisherParameterInfo the info to visit
	 * @param p                              a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebSocketClientOutboundPublisherParameterInfo outboundPublisherParameterInfo, P p);

	/**
	 * <p>
	 * Visits WebSocket return info.
	 * </p>
	 *
	 * @param returnInfo the info to visit
	 * @param p          a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebSocketClientRouteReturnInfo returnInfo, P p);

	/**
	 * <p>
	 * Visits WebSocket exchange return info.
	 * </p>
	 *
	 * @param exchangeReturnInfo the info to visit
	 * @param p                  a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebSocketClientExchangeReturnInfo exchangeReturnInfo, P p);

	/**
	 * <p>
	 * Visits WebSocket inbound return info.
	 * </p>
	 *
	 * @param inboundReturnInfo the info to visit
	 * @param p                 a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebSocketClientInboundReturnInfo inboundReturnInfo, P p);

	/**
	 * <p>
	 * Visits WebSocket inbound publisher info.
	 * </p>
	 *
	 * @param inboundPublisherInfo the info to visit
	 * @param p                    a visitor parameter
	 *
	 * @return a visitor result
	 */
	R visit(WebSocketClientInboundPublisherInfo inboundPublisherInfo, P p);
}

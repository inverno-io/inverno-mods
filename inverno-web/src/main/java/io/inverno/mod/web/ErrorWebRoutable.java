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
package io.inverno.mod.web;

import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.web.spi.Routable;

import java.util.List;

/**
 * <p>
 * An error web routable allows to defined Error Web routes.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 *
 * @see ErrorWebRouter
 *
 * @param <A> the Error Web routable type
 */
public interface ErrorWebRoutable<A extends ErrorWebRoutable<A>> extends Routable<ExchangeContext, ErrorWebExchange<Throwable>, A, ErrorWebRouteManager<A>, ErrorWebRoute> {

	/**
	 * <p>
	 * Configures error web routes using the specified configurer and returns the error web routable.
	 * </p>
	 *
	 * <p>
	 * If the specified configurer is null this method is a noop.
	 * </p>
	 *
	 * @param configurer an error web routes configurer
	 *
	 * @return the error web routable
	 */
	A configureRoutes(ErrorWebRoutesConfigurer configurer);

	/**
	 * <p>
	 * Configures error web routes using the specified configurers and returns the error web routable.
	 * </p>
	 *
	 * <p>
	 * If the specified list of configurers is null or empty this method is a noop.
	 * </p>
	 *
	 * @param configurers a list of error web routes configurers
	 *
	 * @return the error web routable
	 */
	@SuppressWarnings("unchecked")
	default A configureRoutes(List<ErrorWebRoutesConfigurer> configurers) {
		if(configurers != null) {
			for(ErrorWebRoutesConfigurer configurer : configurers) {
				this.configureRoutes(configurer);
			}
		}
		return (A)this;
	}
}

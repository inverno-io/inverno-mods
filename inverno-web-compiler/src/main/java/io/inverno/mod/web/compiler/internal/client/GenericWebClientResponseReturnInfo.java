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
package io.inverno.mod.web.compiler.internal.client;

import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientResponseReturnInfo;

/**
 * <p>
 * Generic {@link WebClientResponseReturnInfo} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class GenericWebClientResponseReturnInfo extends AbstractWebClientRouteReturnInfo implements WebClientResponseReturnInfo {

	/**
	 * <p>
	 * Creates a generic Web client response return info.
	 * </p>
	 *
	 * @param reporter a reporter info
	 */
	public GenericWebClientResponseReturnInfo(ReporterInfo reporter) {
		super(reporter);
	}
}

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

import io.inverno.core.compiler.spi.QualifiedName;
import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.core.compiler.spi.support.AbstractInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientSocketInfo;
import javax.lang.model.type.TypeMirror;

/**
 * <p>
 * Generic {@link WebClientSocketInfo} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class GenericWebClientSocketInfo extends AbstractInfo<QualifiedName> implements WebClientSocketInfo {

	private final TypeMirror contextType;

	/**
	 * <p>
	 * Creates a generic Web client socket info.
	 * </p>
	 *
	 * @param name        the Web client socket qualified name
	 * @param reporter    the reporter info
	 * @param contextType the context type
	 */
	public GenericWebClientSocketInfo(QualifiedName name, ReporterInfo reporter, TypeMirror contextType) {
		super(name, reporter);

		this.contextType = contextType;
	}

	@Override
	public TypeMirror getContextType() {
		return this.contextType;
	}
}

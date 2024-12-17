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
import io.inverno.mod.web.compiler.internal.AbstractWebParameterInfo;
import io.inverno.mod.web.compiler.spi.WebParameterQualifiedName;
import io.inverno.mod.web.compiler.spi.client.WebClientPartParameterInfo;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * <p>
 * Generic {@link WebClientPartParameterInfo} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class GenericWebClientPartParameterInfo extends AbstractWebParameterInfo implements WebClientPartParameterInfo {

	private final String filename;
	private final String contentType;
	private final PartBodyKind partBodyKind;
	private final PartBodyReactiveKind partBodyReactiveKind;

	/**
	 * <p>
	 * Creates a generic Web client part parameter info.
	 * </p>
	 *
	 * @param name                 the parameter qualified name
	 * @param reporter             the parameter reporter
	 * @param parameterElement     the parameter element
	 * @param type                 the parameter type
	 * @param filename             the part filename
	 * @param contentType          the part content type
	 * @param partBodyKind         the part body kind
	 * @param partBodyReactiveKind the part body reactive kind
	 */
	public GenericWebClientPartParameterInfo(
			WebParameterQualifiedName name,
			ReporterInfo reporter,
			VariableElement parameterElement,
			TypeMirror type,
			String filename,
			String contentType,
			PartBodyKind partBodyKind,
			PartBodyReactiveKind partBodyReactiveKind
		) {
		super(name, reporter, parameterElement, type, true);

		this.filename = filename;
		this.contentType = contentType;
		this.partBodyKind = partBodyKind;
		this.partBodyReactiveKind = partBodyReactiveKind;
	}

	@Override
	public String getFilename() {
		return this.filename;
	}

	@Override
	public String getContentType() {
		return this.contentType;
	}

	@Override
	public PartBodyKind getBodyKind() {
		return this.partBodyKind;
	}

	@Override
	public PartBodyReactiveKind getBodyReactiveKind() {
		return this.partBodyReactiveKind;
	}
}

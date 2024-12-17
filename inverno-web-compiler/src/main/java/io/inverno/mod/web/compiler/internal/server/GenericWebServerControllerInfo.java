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
package io.inverno.mod.web.compiler.internal.server;

import io.inverno.core.compiler.spi.BeanQualifiedName;
import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.core.compiler.spi.support.AbstractInfo;
import io.inverno.mod.web.compiler.internal.NoOpReporterInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerControllerInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerRouteInfo;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * <p>
 * Generic {@link WebServerControllerInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AbstractInfo
 */
public class GenericWebServerControllerInfo extends AbstractInfo<BeanQualifiedName> implements WebServerControllerInfo {

	private final TypeElement element;
	private final DeclaredType type;
	private final String rootPath;
	private final WebServerRouteInfo[] routes;
	private final TypeMirror[] typesRegistry;

	/**
	 * <p>
	 * Creates a generic Web server controller info.
	 * </p>
	 *
	 * @param element       the type element of the controller
	 * @param name          the controller qualified name
	 * @param reporter      the controller reporter
	 * @param type          the controller type
	 * @param rootPath      the root path of the routes defined in the controller
	 * @param routes        the routes defined in the controller
	 * @param typesRegistry the types registry
	 */
	public GenericWebServerControllerInfo(
			TypeElement element,
			BeanQualifiedName name,
			ReporterInfo reporter,
			DeclaredType type,
			String rootPath,
			List<GenericWebServerRouteInfo> routes,
			Set<TypeMirror> typesRegistry
		) {
		super(name, reporter instanceof NoOpReporterInfo ? ((NoOpReporterInfo)reporter).getReporter() : reporter);
		this.element = element;
		this.type = Objects.requireNonNull(type);
		this.rootPath = rootPath;
		if(routes != null) {
			this.routes = new WebServerRouteInfo[routes.size()];
			for(int i=0;i<this.routes.length;i++) {
				GenericWebServerRouteInfo route = routes.get(i);
				route.setController(this);
				this.routes[i] = route;
			}
		}
		else {
			this.routes = new WebServerRouteInfo[0];
		}
		this.typesRegistry = typesRegistry != null ? typesRegistry.toArray(TypeMirror[]::new) : new TypeMirror[0];
	}

	@Override
	public boolean hasError() {
		return super.hasError() || Arrays.stream(this.routes).anyMatch(ReporterInfo::hasError);
	}

	@Override
	public boolean hasWarning() {
		return super.hasWarning() || Arrays.stream(this.routes).anyMatch(ReporterInfo::hasWarning);
	}
	
	@Override
	public TypeElement getElement() {
		return this.element;
	}
	
	@Override
	public DeclaredType getType() {
		return this.type;
	}

	@Override
	public String getRootPath() {
		return this.rootPath;
	}
	
	@Override
	public WebServerRouteInfo[] getRoutes() {
		return this.routes;
	}

	@Override
	public TypeMirror[] getTypesRegistry() {
		return typesRegistry;
	}
}

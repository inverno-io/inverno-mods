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

import io.inverno.core.annotation.Bean;
import io.inverno.core.compiler.spi.BeanQualifiedName;
import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.core.compiler.spi.support.AbstractInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientRouteInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientStubInfo;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * <p>
 * Generic {@link WebClientStubInfo} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class GenericWebClientStubInfo extends AbstractInfo<BeanQualifiedName> implements WebClientStubInfo {

	private final TypeElement element;
	private final DeclaredType type;
	private final String uri;
	private final Bean.Visibility visibility;
	private final WebClientRouteInfo[] routes;
	private final TypeMirror[] typesRegistry;

	/**
	 * <p>
	 * Creates a generic Web client stub info.
	 * </p>
	 *
	 * @param element       the element
	 * @param name          the Web client bean name
	 * @param reporter      the reporter info
	 * @param type          the type
	 * @param uri           the URI
	 * @param visibility    the bean visibility
	 * @param routes        the stub routes
	 * @param typesRegistry the types registry
	 */
	public GenericWebClientStubInfo(TypeElement element, BeanQualifiedName name, ReporterInfo reporter, DeclaredType type, String uri, Bean.Visibility visibility, List<GenericWebClientRouteInfo> routes, Set<TypeMirror> typesRegistry) {
		super(name, reporter);
		this.element = element;
		this.type = Objects.requireNonNull(type);
		this.uri = uri;
		this.visibility = visibility;
		if(routes != null) {
			this.routes = new WebClientRouteInfo[routes.size()];
			for(int i=0;i<this.routes.length;i++) {
				GenericWebClientRouteInfo route = routes.get(i);
				route.setClientStub(this);
				this.routes[i] = route;
			}
		}
		else {
			this.routes = new WebClientRouteInfo[0];
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
	public Bean.Visibility getVisibility() {
		return this.visibility;
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
	public String getURI() {
		return this.uri;
	}

	@Override
	public WebClientRouteInfo[] getRoutes() {
		return this.routes;
	}

	@Override
	public TypeMirror[] getTypesRegistry() {
		return this.typesRegistry;
	}
}

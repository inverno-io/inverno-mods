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
package io.inverno.mod.web.compiler.internal;

import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.core.compiler.spi.plugin.CompilerPlugin;
import io.inverno.core.compiler.spi.plugin.PluginContext;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;

/**
 * <p>
 * Base Web {@link CompilerPlugin} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public abstract class AbstractWebCompilerPlugin implements CompilerPlugin {

	protected PluginContext pluginContext;
	protected TypeHierarchyExtractor typeHierarchyExtractor;

	private TypeMirror objectType;

	@Override
	public void init(PluginContext pluginContext) {
		this.pluginContext = pluginContext;
		this.typeHierarchyExtractor = new TypeHierarchyExtractor(pluginContext.getTypeUtils());

		this.objectType = this.pluginContext.getElementUtils().getTypeElement(Object.class.getCanonicalName()).asType();
	}

	/**
	 * <p>
	 * Unwilds the context type.
	 * </p>
	 *
	 * @param reporter    a reporter for errors
	 * @param contextType the context type to unwild
	 *
	 * @return the unwild context type
	 */
	protected final TypeMirror unwildContextType(ReporterInfo reporter, DeclaredType contextType) {
		if(!contextType.getTypeArguments().isEmpty()) {
			TypeElement typeElement = (TypeElement)this.pluginContext.getTypeUtils().asElement(contextType);

			List<? extends TypeMirror> typeArguments = contextType.getTypeArguments();
			TypeMirror[] unwildTypeArgs = new TypeMirror[typeArguments.size()];
			for(int i=0;i<typeArguments.size();i++) {
				TypeMirror typeArg = typeArguments.get(i);
				if(typeArg.getKind() == TypeKind.WILDCARD) {
					TypeMirror extendsBound = ((WildcardType)typeArg).getExtendsBound();
					TypeMirror superBound = ((WildcardType)typeArg).getExtendsBound();
					if(extendsBound != null) {
						unwildTypeArgs[i] = extendsBound;
					}
					else if(superBound != null) {
						unwildTypeArgs[i] = superBound;
					}
					else {
						List<? extends TypeMirror> bounds = typeElement.getTypeParameters().get(i).getBounds();
						switch(bounds.size()) {
							case 0: {
								//Object
								this.pluginContext.getElementUtils().getTypeElement(Object.class.getCanonicalName());
								break;
							}
							case 1: {
								unwildTypeArgs[i] = bounds.getFirst();
								break;
							}
							default: {
								// TODO if there are more than one bounds we have a union type: T extends Number & Runnable and we must fail because the context can't be parameterized
								reporter.error("Context type with union type bound is not allowed");
							}
						}
					}
				}
				else {
					unwildTypeArgs[i] = typeArg;
				}
			}
			return this.pluginContext.getTypeUtils().getDeclaredType(typeElement, unwildTypeArgs);
		}
		return contextType;
	}

	/**
	 * <p>
	 * Finds the super types matching the specified erased super type.
	 * </p>
	 *
	 * @param type            the type where to find the super type
	 * @param erasedSuperType the erased super type to find
	 *
	 * @return a type or null
	 *
	 * @throws IllegalStateException if no matching super type could be found
	 */
	protected final TypeMirror findSuperType(TypeMirror type, TypeMirror erasedSuperType) throws IllegalStateException {
		for(TypeMirror superType : this.pluginContext.getTypeUtils().directSupertypes(type)) {
			if(this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(superType), erasedSuperType)) {
				return superType;
			}
			else {
				TypeMirror result = this.findSuperType(superType, erasedSuperType);
				if(result != null) {
					return result;
				}
			}
		}
		return null;
	}

	/**
	 * <p>
	 * Returns all the unique super types of the specified type, Object excluded.
	 * </p>
	 *
	 * @param type a type
	 *
	 * @return a set of types
	 */
	protected final Set<TypeMirror> getAllSuperTypes(TypeMirror type) {
		Set<TypeMirror> result = new TreeSet<>( (t1, t2) -> {
			if(this.pluginContext.getTypeUtils().isSameType(t1, t2)) {
				return 0;
			}
			return t1.toString().compareTo(t2.toString());
		});

		for(TypeMirror superType : this.pluginContext.getTypeUtils().directSupertypes(type)) {
			if(!this.pluginContext.getTypeUtils().isSameType(superType, this.objectType)) {
				result.add(superType);
				result.addAll(this.getAllSuperTypes(superType));
			}
		}

		return result;
	}
}

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
package io.winterframework.mod.web.router.internal.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * @author jkuhn
 *
 */
class TypeHierarchyExtractor {

	private Types typeUtils;
	
	public TypeHierarchyExtractor(Types typeUtils) {
		this.typeUtils = typeUtils;
	}
	
	public List<TypeElement> extractTypeHierarchy(TypeElement typeElement) {
		List<TypeElement> typeHierarchy = this.getTypeElementWrappers(typeElement, 0, 0).stream()
				.sorted()
				.map(SortableTypeElementWrapper::getTypeElement)
				.collect(Collectors.toList());
		return typeHierarchy;
	}

	private List<SortableTypeElementWrapper> getTypeElementWrappers(TypeElement typeElement, int absoluteLevel, int extendsLevel) {
		List<SortableTypeElementWrapper> result = new ArrayList<>();
		result.add(new SortableTypeElementWrapper(typeElement, absoluteLevel, extendsLevel));
		TypeMirror superType = typeElement.getSuperclass();
		if(superType.getKind() == TypeKind.DECLARED) {
			TypeElement superTypeElement = (TypeElement)this.typeUtils.asElement(superType);
			result.addAll(this.getTypeElementWrappers(superTypeElement, absoluteLevel + 1, extendsLevel + 1));
		}

		for(TypeMirror interfaceType : typeElement.getInterfaces()) {
			TypeElement interfaceTypeElement = (TypeElement)this.typeUtils.asElement(interfaceType);
			result.addAll(this.getTypeElementWrappers(interfaceTypeElement, absoluteLevel + 1, extendsLevel));
		}
		return result;
	}
	
	private static class SortableTypeElementWrapper implements Comparable<SortableTypeElementWrapper> {

		private final TypeElement typeElement;
		
		private final int absoluteLevel;
		
		private final int extendsLevel;
		
		public SortableTypeElementWrapper(TypeElement typeElement, int absoluteLevel, int extendsLevel) {
			this.typeElement = typeElement;
			this.absoluteLevel = absoluteLevel;
			this.extendsLevel = extendsLevel;
		}
		
		public TypeElement getTypeElement() {
			return typeElement;
		}
		
		@Override
		public int compareTo(SortableTypeElementWrapper o) {
			if(this.absoluteLevel < o.absoluteLevel) {
				return -1;
			}
			else if(this.absoluteLevel > o.absoluteLevel) {
				return 1;
			}
			else {
				if(this.extendsLevel < o.extendsLevel) {
					return -1;
				}
				else if(this.extendsLevel > o.extendsLevel) {
					return 1;
				}
				else {
					return 0;
				}
			}
		}
	}
}

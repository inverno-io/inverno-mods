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
package io.winterframework.mod.web.compiler.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * <p>
 * A type hierarchy extractor is used to extract all the type elements
 * implemented or extended by a given type and order them according to a
 * particular distance function.
 * </p>
 * 
 * <p>
 * The distance from a type {@code T} to type {@code U} is given by the number
 * of indirection between them, that is to say the number of {@code extends} or
 * {@code implements} keywords one should go through to get to {@code U} from
 * {@code T}. If there is a tie, the type accessed with the biggest number of
 * {@code extends} wins.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class TypeHierarchyExtractor {

	private Types typeUtils;
	
	/**
	 * <p>
	 * Creates a type hierarchy extractor.
	 * <p>
	 * 
	 * @param typeUtils types utils
	 */
	public TypeHierarchyExtractor(Types typeUtils) {
		this.typeUtils = typeUtils;
	}
	
	/**
	 * <p>
	 * Extracts the hierarchy of the specified type element.
	 * </p>
	 * 
	 * @param typeElement a type element
	 * @return a list of type elements ordered from the nearest to further
	 */
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

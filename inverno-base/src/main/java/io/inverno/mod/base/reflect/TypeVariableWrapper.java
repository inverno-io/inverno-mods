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
package io.inverno.mod.base.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Objects;

/**
 * <p>
 * A wrapper around an underlying type variable used to create a specific type variable with custom name and bounds.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
class TypeVariableWrapper<D extends GenericDeclaration> implements TypeVariable<D> {

	private final TypeVariable<D> typeVariable;
	
	private final String name;
	private final Type[] bounds;
	
	/**
	 * <p>
	 * Creates a type variable wrapper based on an underlying type variable with
	 * specified name and bounds.
	 * </p>
	 * 
	 * @param typeVariable the underlying type variable
	 * @param name         a variable name
	 * @param bounds       a list of bounds
	 */
	public TypeVariableWrapper(TypeVariable<D> typeVariable, String name, Type[] bounds) {
		this.typeVariable = typeVariable;
		this.name = name;
		this.bounds = bounds;
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
		return this.typeVariable.getAnnotation(annotationClass);
	}

	@Override
	public Annotation[] getAnnotations() {
		return this.typeVariable.getAnnotations();
	}

	@Override
	public Annotation[] getDeclaredAnnotations() {
		return this.typeVariable.getDeclaredAnnotations();
	}

	@Override
	public Type[] getBounds() {
		return this.bounds;
	}

	@Override
	public D getGenericDeclaration() {
		return this.typeVariable.getGenericDeclaration();
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public AnnotatedType[] getAnnotatedBounds() {
		AnnotatedType[] annotatedBounds = new AnnotatedType[this.bounds.length];
		for(int i=0;i<this.bounds.length;i++) {
			annotatedBounds[i] = new AnnotatedTypeWrapper(this.bounds[i]);
		}
		return annotatedBounds;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(this.name);
		
		if(this.bounds != null && this.bounds.length > 0) {
			sb.append(" extends ");
			
			for(int i=0;i<this.bounds.length;i++) {
				if (i > 0) {
					sb.append(" & ");
				}
				sb.append((this.bounds[i] instanceof Class) ? ((Class<?>) this.bounds[i]).getName() : this.bounds[i].toString());
			}
		}
		return sb.toString();
	}
	
	/**
	 * <p>
	 * An annotated type wrapper that simply wrapped an existing type ignoring
	 * annotations.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 */
	private static class AnnotatedTypeWrapper implements AnnotatedType {

		private final Type type;
		
		public AnnotatedTypeWrapper(Type type) {
			this.type = type;
		}
		
		@Override
		public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
			Objects.requireNonNull(annotationClass);
			return null;
		}

		@Override
		public Annotation[] getAnnotations() {
			return new Annotation[0];
		}

		@Override
		public Annotation[] getDeclaredAnnotations() {
			return new Annotation[0];
		}

		@Override
		public Type getType() {
			return this.type;
		}
	}
}

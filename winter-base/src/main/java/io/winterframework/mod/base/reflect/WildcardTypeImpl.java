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
package io.winterframework.mod.base.reflect;

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;

/**
 * @author jkuhn
 *
 */
class WildcardTypeImpl implements WildcardType {

	private final Type[] upperBounds;
	private final Type[] lowerBounds;

	/**
	 * Creates a wildcard type with the requested bounds. Note that the array
	 * arguments are not cloned because instances of this class are never
	 * constructed from outside the containing package.
	 *
	 * @param upperBounds the array of types representing the upper bound(s) of this
	 *                    type variable
	 * @param lowerBounds the array of types representing the lower bound(s) of this
	 *                    type variable
	 */
	public WildcardTypeImpl(Type[] upperBounds, Type[] lowerBounds) {
		this.upperBounds = upperBounds;
		this.lowerBounds = lowerBounds;
	}

	/**
	 * Returns an array of {@link Type Type} objects representing the upper bound(s)
	 * of this type variable. Note that if no upper bound is explicitly declared,
	 * the upper bound is {@link Object Object}.
	 *
	 * @return an array of types representing the upper bound(s) of this type
	 *         variable
	 */
	public Type[] getUpperBounds() {
		return this.upperBounds.length == 0 ? new Type[] { Object.class } : this.upperBounds.clone();
	}

	/**
	 * Returns an array of {@link Type Type} objects representing the lower bound(s)
	 * of this type variable. Note that if no lower bound is explicitly declared,
	 * the lower bound is the type of {@code null}. In this case, a zero length
	 * array is returned.
	 *
	 * @return an array of types representing the lower bound(s) of this type
	 *         variable
	 */
	public Type[] getLowerBounds() {
		return this.lowerBounds.clone();
	}

	/**
	 * Indicates whether some other object is "equal to" this one. It is implemented
	 * compatibly with the JDK's
	 * {@link sun.reflect.generics.reflectiveObjects.WildcardTypeImpl
	 * WildcardTypeImpl}.
	 *
	 * @param object the reference object with which to compare
	 * @return {@code true} if this object is the same as the object argument;
	 *         {@code false} otherwise
	 * @see sun.reflect.generics.reflectiveObjects.WildcardTypeImpl#equals
	 */
	@Override
	public boolean equals(Object object) {
		if (object instanceof WildcardType) {
			WildcardType type = (WildcardType) object;
			return Arrays.equals(this.upperBounds, type.getUpperBounds())
					&& Arrays.equals(this.lowerBounds, type.getLowerBounds());
		}
		return false;
	}

	/**
	 * Returns a hash code value for the object. It is implemented compatibly with
	 * the JDK's {@link sun.reflect.generics.reflectiveObjects.WildcardTypeImpl
	 * WildcardTypeImpl}.
	 *
	 * @return a hash code value for this object
	 * @see sun.reflect.generics.reflectiveObjects.WildcardTypeImpl#hashCode
	 */
	@Override
	public int hashCode() {
		return Arrays.hashCode(this.upperBounds) ^ Arrays.hashCode(this.lowerBounds);
	}

	/**
	 * Returns a string representation of the object. It is implemented compatibly
	 * with the JDK's {@link sun.reflect.generics.reflectiveObjects.WildcardTypeImpl
	 * WildcardTypeImpl}.
	 *
	 * @return a string representation of the object
	 * @see sun.reflect.generics.reflectiveObjects.WildcardTypeImpl#toString
	 */
	@Override
	public String toString() {
		StringBuilder sb;
		Type[] bounds;
		if (this.lowerBounds.length == 0) {
			if (this.upperBounds.length == 0 || Object.class == this.upperBounds[0]) {
				return "?";
			}
			bounds = this.upperBounds;
			sb = new StringBuilder("? extends ");
		} else {
			bounds = this.lowerBounds;
			sb = new StringBuilder("? super ");
		}
		for (int i = 0; i < bounds.length; i++) {
			if (i > 0) {
				sb.append(" & ");
			}
			sb.append((bounds[i] instanceof Class) ? ((Class<?>) bounds[i]).getName() : bounds[i].toString());
		}
		return sb.toString();
	}
}

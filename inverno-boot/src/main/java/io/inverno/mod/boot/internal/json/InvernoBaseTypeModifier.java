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
package io.inverno.mod.boot.internal.json;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.ReferenceType;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.type.TypeModifier;
import io.inverno.mod.base.Settable;
import java.lang.reflect.Type;

/**
 * <p>
 * Jackson {@link TypeModifier} implementation that makes sure {@link Settable} is a reference type.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class InvernoBaseTypeModifier extends TypeModifier implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	@Override
	public JavaType modifyType(JavaType type, Type jdkType, TypeBindings bindings, TypeFactory typeFactory) {
		if (type.isReferenceType() || type.isContainerType()) {
			return type;
		}
		final Class<?> raw = type.getRawClass();

		JavaType refType;
		if(raw == Settable.class) {
			refType = type.containedTypeOrUnknown(0);
		}
		else {
			return type;
		}
		return ReferenceType.upgradeFrom(type, refType);
	}
}
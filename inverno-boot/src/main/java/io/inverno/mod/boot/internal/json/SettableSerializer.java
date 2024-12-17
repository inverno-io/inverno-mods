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

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.ReferenceTypeSerializer;
import com.fasterxml.jackson.databind.type.ReferenceType;
import com.fasterxml.jackson.databind.util.NameTransformer;
import io.inverno.mod.base.Settable;

/**
 * <p>
 * Jackson {@link ReferenceTypeSerializer} implementation with support for {@link Settable}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class SettableSerializer extends ReferenceTypeSerializer<Settable<?>> {

	private static final long serialVersionUID = 1L;

	protected SettableSerializer(ReferenceType fullType, boolean staticTyping, TypeSerializer vts, JsonSerializer<Object> ser) {
		super(fullType, staticTyping, vts, ser);
	}

	protected SettableSerializer(SettableSerializer base, BeanProperty property, TypeSerializer vts, JsonSerializer<?> valueSer, NameTransformer unwrapper, Object suppressableValue, boolean suppressNulls) {
		super(base, property, vts, valueSer, unwrapper, suppressableValue, suppressNulls);
	}

	@Override
	protected ReferenceTypeSerializer<Settable<?>> withResolved(BeanProperty prop, TypeSerializer vts, JsonSerializer<?> valueSer, NameTransformer unwrapper) {
		return new SettableSerializer(this, prop, vts, valueSer, unwrapper, this._suppressableValue, this._suppressNulls);
	}

	@Override
	public ReferenceTypeSerializer<Settable<?>> withContentInclusion(Object suppressableValue, boolean suppressNulls) {
		return new SettableSerializer(this, this._property, this._valueTypeSerializer, this._valueSerializer, this._unwrapper, suppressableValue, suppressNulls);
	}

	@Override
	protected boolean _isValuePresent(Settable<?> value) {
		return value.isSet();
	}

	@Override
	protected Object _getReferenced(Settable<?> value) {
		return value.get();
	}

	@Override
	protected Object _getReferencedIfPresent(Settable<?> value) {
		return value.isSet() ? value.get() : null;
	}
}
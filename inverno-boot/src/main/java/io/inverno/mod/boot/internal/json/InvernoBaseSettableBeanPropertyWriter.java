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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.util.NameTransformer;

/**
 * <p>
 * Jackson {@link BeanPropertyWriter} implementation with support for {@link io.inverno.mod.base.Settable Settable}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class InvernoBaseSettableBeanPropertyWriter extends BeanPropertyWriter {

	private static final long serialVersionUID = 1L;

	protected final Object _empty;

	protected InvernoBaseSettableBeanPropertyWriter(BeanPropertyWriter base, Object empty) {
		super(base);
		_empty = empty;
	}

	protected InvernoBaseSettableBeanPropertyWriter(InvernoBaseSettableBeanPropertyWriter base, PropertyName newName) {
		super(base, newName);
		_empty = base._empty;
	}

	@Override
	protected BeanPropertyWriter _new(PropertyName newName) {
		return new InvernoBaseSettableBeanPropertyWriter(this, newName);
	}

	@Override
	public BeanPropertyWriter unwrappingWriter(NameTransformer unwrapper) {
		return new InvernoBaseUnwrappingSettableBeanPropertyWriter(this, unwrapper, _empty);
	}

	@Override
	public void serializeAsField(Object bean, JsonGenerator g, SerializerProvider prov) throws Exception {
		if (_nullSerializer == null) {
			Object value = get(bean);
			if (value == null || value.equals(_empty)) {
				return;
			}
		}
		super.serializeAsField(bean, g, prov);
	}

}

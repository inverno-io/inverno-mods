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
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.UnwrappingBeanPropertyWriter;
import com.fasterxml.jackson.databind.util.NameTransformer;

/**
 * <p>
 * Jackson {@link UnwrappingBeanPropertyWriter} implementation with support for {@link io.inverno.mod.base.Settable Settable}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class InvernoBaseUnwrappingSettableBeanPropertyWriter extends UnwrappingBeanPropertyWriter {

	private static final long serialVersionUID = 1L;

	/**
	 * @since 2.9
	 */
	protected final Object _empty;

	public InvernoBaseUnwrappingSettableBeanPropertyWriter(BeanPropertyWriter base, NameTransformer transformer, Object empty) {
		super(base, transformer);
		_empty = empty;
	}

	protected InvernoBaseUnwrappingSettableBeanPropertyWriter(InvernoBaseUnwrappingSettableBeanPropertyWriter base, NameTransformer transformer, SerializedString name) {
		super(base, transformer, name);
		_empty = base._empty;
	}

	@Override
	protected UnwrappingBeanPropertyWriter _new(NameTransformer transformer, SerializedString newName) {
		return new InvernoBaseUnwrappingSettableBeanPropertyWriter(this, transformer, newName);
	}

	@Override
	public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception
	{
		if (_nullSerializer == null) {
			Object value = get(bean);
			if (value == null || value.equals(_empty)) {
				return;
			}
		}
		super.serializeAsField(bean, gen, prov);
	}
}

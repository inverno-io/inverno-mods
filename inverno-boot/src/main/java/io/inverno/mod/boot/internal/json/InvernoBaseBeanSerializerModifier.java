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

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import io.inverno.mod.base.Settable;
import java.util.List;

/**
 * <p>
 * A Jackson {@link BeanSerializerModifier} used to exclude "undefined" {@link Settable} values when handling of "absent as nulls" is enabled.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class InvernoBaseBeanSerializerModifier extends BeanSerializerModifier {

	@Override
	public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
		for(int i = 0; i < beanProperties.size(); ++i) {
			final BeanPropertyWriter writer = beanProperties.get(i);
			JavaType type = writer.getType();

			Object empty;
			if (type.isTypeOrSubTypeOf(Settable.class)) {
				empty = Settable.undefined();
			} else {
				continue;
			}
			beanProperties.set(i, new InvernoBaseSettableBeanPropertyWriter(writer, empty));
		}
		return beanProperties;
	}
}

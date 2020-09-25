/*
 * Copyright 2020 Jeremy KUHN
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
package io.winterframework.mod.configuration.internal;

import io.winterframework.mod.configuration.AbstractConfigurationLoader;

/**
 * @author jkuhn
 *
 */
abstract class AbstractReflectiveConfigurationLoader<A, B extends AbstractReflectiveConfigurationLoader<A, B>> extends AbstractConfigurationLoader<A, B> {

	@SuppressWarnings("unchecked")
	protected static <B> B getDefaultValue(Class<B> type) {
		Object result = null;
		if(type.equals(boolean.class)) {
			result = (new boolean[1])[0];
		}
		else if(type.equals(byte.class)) {
			result = (new byte[1])[0];
		}
		else if(type.equals(char.class)) {
			result =  (new char[1])[0];
		}
		else if(type.equals(short.class)) {
			result =  (new short[1])[0];
		}
		else if(type.equals(int.class)) {
			result = (new int[1])[0];
		}
		else if(type.equals(long.class)) {
			result = (new long[1])[0];
		}
		else if(type.equals(float.class)) {
			result = (new float[1])[0];
		}
		else if(type.equals(double.class)) {
			result = (new double[1])[0];
		}
		return (B)result;
	}
}

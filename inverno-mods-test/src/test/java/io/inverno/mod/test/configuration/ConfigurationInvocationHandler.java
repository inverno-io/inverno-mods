/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.test.configuration;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class ConfigurationInvocationHandler implements InvocationHandler {
		
	private final Class<?> configurationClass;

	private final Map<String, Object> properties;

	public ConfigurationInvocationHandler(Class<?> configurationClass, Map<String, Object> properties) {
		this.configurationClass = configurationClass;
		this.properties = new HashMap<>(properties);
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if(method.getName().equals("equals")) {
			if (args[0] == null) {
				return false;
			}
			InvocationHandler handler = Proxy.getInvocationHandler(args[0]);
			if (!(handler instanceof ConfigurationInvocationHandler)) {
				return false;
			}
			ConfigurationInvocationHandler configurationHandler = (ConfigurationInvocationHandler)handler;
			if(!configurationHandler.configurationClass.equals(this.configurationClass)) {
				return false;
			}
			return configurationHandler.properties.equals(this.properties);
		}
		if(this.properties.containsKey(method.getName())) {
			return this.properties.get(method.getName());
		}
		else if(method.isDefault()) {
			Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
			constructor.setAccessible(true);
			this.properties.put(method.getName(), constructor.newInstance(configurationClass)
				.in(configurationClass)
				.unreflectSpecial(method, configurationClass)
				.bindTo(proxy)
				.invokeWithArguments());

			/*return MethodHandles.lookup()
			.in(this.configurationClass)
			.unreflectSpecial(method, this.configurationClass)
			.bindTo(proxy)
			.invokeWithArguments();*/

//			    MethodHandle handle = MethodHandles.lookup().findSpecial(this.configurationClass, method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()), this.configurationClass).bindTo(proxy);
//			    return handle.invokeWithArguments();
		}
		return this.properties.get(method.getName());
	}
}
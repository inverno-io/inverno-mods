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
package io.inverno.mod.irt.compiler.internal;

import javax.lang.model.type.TypeMirror;

import io.inverno.mod.irt.compiler.spi.ParameterInfo;

/**
 * <p>
 * Generic {@link ParameterInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public class GenericParameterInfo extends BaseInfo implements ParameterInfo {

	private final String name;
	private final TypeMirror type;
	private final String value;
	
	/**
	 * <p>
	 * Creates a generic parameter info.
	 * </p>
	 * 
	 * @param range the range in the IRT source file where the info is defined
	 * @param name  a name
	 * @param type  a type
	 * @param value a raw Java formal parameter declaration
	 */
	public GenericParameterInfo(Range range, String name, TypeMirror type, String value) {
		super(range);
		this.name = name;
		this.type = type;
		this.value = value;
	}
	
	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public TypeMirror getType() {
		return this.type;
	}
	
	@Override
	public String getValue() {
		return this.value;
	}
}

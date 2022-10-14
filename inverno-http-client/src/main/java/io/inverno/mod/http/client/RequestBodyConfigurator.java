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

package io.inverno.mod.http.client;

import io.inverno.mod.http.base.OutboundData;
import io.inverno.mod.http.base.Parameter;
import io.netty.buffer.ByteBuf;
import java.util.function.BiConsumer;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public interface RequestBodyConfigurator {

	void empty();
	
	OutboundData<ByteBuf> raw();
	
	<T extends CharSequence> OutboundData<T> string();
	
	RequestBodyConfigurator.Resource resource();
	
	RequestBodyConfigurator.UrlEncoded<Parameter.Factory> urlEncoded();
	
	RequestBodyConfigurator.Multipart<Part.Factory, Part<?>> multipart();
	
	interface Resource {
		
		void value(io.inverno.mod.base.resource.Resource resource) throws IllegalStateException;
	}
	
	interface UrlEncoded<A extends Parameter.Factory> {
		
		void from(BiConsumer<A, OutboundData<Parameter>> data);
	}
	
	interface Multipart<A extends Part.Factory, B extends Part<?>> {
		
		void from(BiConsumer<A, OutboundData<B>> data);
	}
}

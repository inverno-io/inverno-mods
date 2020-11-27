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
package io.winterframework.mod.web;

import org.reactivestreams.Publisher;

/**
 * @author jkuhn
 *
 */
public interface ServerSentEvent<A> {
	
	String getId();
	
	String getComment();
	
	String getEvent();
	
	Publisher<A> getData();
	
	public static interface Configurator<A> {
		
		Configurator<A> id(String id);
		
		Configurator<A> comment(String comment);
		
		Configurator<A> event(String event);
		
		Configurator<A> data(Publisher<A> data);
		
		Configurator<A> data(String data);
		
		Configurator<A> data(byte[] data);
	}
}

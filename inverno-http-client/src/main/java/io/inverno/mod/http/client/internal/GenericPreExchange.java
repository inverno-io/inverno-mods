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
package io.inverno.mod.http.client.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.PreExchange;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class GenericPreExchange<A extends ExchangeContext> implements PreExchange<A>, Exchange<A> {

	private final A exchangeContext;
	
	private final GenericPreRequest request;
	
	private final GenericPreResponse response;

	public GenericPreExchange(A exchangeContext, GenericPreRequest request, GenericPreResponse response) {
		this.exchangeContext = exchangeContext;
		this.request = request;
		this.response = response;
	}
	
	@Override
	public A context() {
		return this.exchangeContext;
	}
	
	@Override
	public GenericPreRequest request() {
		return this.request;
	}

	@Override
	public GenericPreResponse response() {
		return this.response;
	}
}

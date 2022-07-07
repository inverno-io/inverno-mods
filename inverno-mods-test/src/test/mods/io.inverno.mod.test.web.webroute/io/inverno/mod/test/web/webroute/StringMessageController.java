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
package io.inverno.mod.test.web.webroute;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.net.URIs;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.test.web.webroute.dto.GenericMessage;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.annotation.FormParam;
import io.inverno.mod.web.annotation.WebController;
import io.inverno.mod.web.annotation.WebRoute;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@Bean
@WebController( path = "/message/string")
public class StringMessageController implements GenericMessageController<String> {

	private AtomicInteger index = new AtomicInteger();
	
	// Note that in real life calls to DB must be non-blocking
	private Map<Integer, GenericMessage<String>> data = new ConcurrentHashMap<>();
	
	// curl -iv -d '{"@type":"string","message":"Hello, world!"}' -H 'content-type: application/json' -X POST http://127.0.0.1:8080/message/string
	/**
	 * <p>Creates a message <a href="toto">toto</a>.</p>
	 * 
	 * <p>A message is identified by a generated id.</p>
	 * 
	 * @param message the message to create
	 * @param exchange the exchange
	 * 
	 * @return {@inverno.web.status 201} A message has been created
	 */
	@Override
	public void createMessage(GenericMessage<String> message, WebExchange<? extends ExchangeContext> exchange) {
		message.setId(this.index.getAndIncrement());
		this.data.put(message.getId(), message);
		exchange.response().headers().status(Status.CREATED).add(Headers.NAME_LOCATION, URIs.uri(exchange.request().getPathAbsolute()).segment(Integer.toString(message.getId())).buildPath());
	}

	// curl -iv http://127.0.0.1:8080/message/string/0
	/**
	 * Returns the message with the specified id.
	 * 
	 * @param id The message id 
	 * @return {@inverno.web.status 200} A message
	 * 
	 * @throws IllegalArgumentException invalid argument
	 * @throws NotFoundException not found
	 */
	@Override
	public GenericMessage<String> getMessage(int id) throws NotFoundException, IllegalStateException {
		if(!this.data.containsKey(id)) {
			throw new NotFoundException();
		}
		return this.data.get(id);
	}

	// curl -iv -d '{"@type":"string","message":"Hallo, welt!"}' -H 'content-type: application/json' -X PUT http://127.0.0.1:8080/message/string/0
	@Override
	public GenericMessage<String> updateMessage(int id, GenericMessage<String> message) {
		if(!this.data.containsKey(id)) {
			throw new NotFoundException();
		}
		GenericMessage<String> storedMessage = this.data.get(id);
		storedMessage.setMessage(message.getMessage());
		return storedMessage;
	}

	// curl -iv -X DELETE http://127.0.0.1:8080/message/string/0
	/**
	 * <p>Deletes the STRING message with the specified id.</p>
	 * 
	 * {@inheritDoc}
	 * 
	 * @return {@inverno.web.status 200} the STRING message has been deleted, {@inheritDoc}
	 */
	@Override
	public void deleteMessage(int id) throws IllegalArgumentException {
		if(!this.data.containsKey(id)) {
			throw new NotFoundException();
		}
		this.data.remove(id);
	}
	
	@WebRoute( path = "formCreate", method = Method.POST, consumes = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
	public void createMessage(@FormParam String param1, @FormParam String param2, WebExchange<? extends ExchangeContext> exchange) {
		GenericMessage<String> message = new GenericMessage<String>(this.index.getAndIncrement(), param1 + param2);
		this.data.put(message.getId(), message);
		exchange.response().headers().status(Status.CREATED).add(Headers.NAME_LOCATION, URIs.uri(exchange.request().getPathAbsolute()).segment(Integer.toString(message.getId())).buildPath());
	}
}

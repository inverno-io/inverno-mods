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

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.test.web.webroute.dto.GenericMessage;
import io.inverno.mod.web.base.annotation.Body;
import io.inverno.mod.web.base.annotation.PathParam;
import io.inverno.mod.web.server.WebExchange;
import io.inverno.mod.web.server.annotation.WebRoute;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public interface GenericMessageController<A> {

	@WebRoute( path = "", method = Method.POST, consumes = MediaTypes.APPLICATION_JSON)
	void createMessage(@Body GenericMessage<A> message, WebExchange<? extends ExchangeContext> exchange);
	
	@WebRoute( path = "{id}", method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
	GenericMessage<A> getMessage(@PathParam int id);
	
	/**
	 * <p>Updates the message with the specified id.</p>
	 * 
	 * @param id The message id.
	 * @param message The updated message
	 * @return The updated message
	 */
	@WebRoute( path = "{id}", method = Method.PUT, produces = MediaTypes.APPLICATION_JSON, consumes = MediaTypes.APPLICATION_JSON)
	GenericMessage<A> updateMessage(@PathParam int id, @Body GenericMessage<A> message);
	
	/**
	 * <p>Deletes the message with the specified id.</p>
	 * 
	 * @param id The message id to delete
	 * 
	 * @return {@inverno.web.status 200} the message has been deleted
	 */
	@WebRoute( path = "{id}", method = Method.DELETE)
	void deleteMessage(@PathParam int id);
}

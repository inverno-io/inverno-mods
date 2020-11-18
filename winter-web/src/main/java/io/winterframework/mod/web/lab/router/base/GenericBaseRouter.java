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
package io.winterframework.mod.web.lab.router.base;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.function.Function;

import io.netty.buffer.Unpooled;
import io.winterframework.mod.web.Request;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.RequestHandler;
import io.winterframework.mod.web.Response;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.internal.header.AcceptCodec;
import io.winterframework.mod.web.internal.header.AcceptLanguageCodec;
import io.winterframework.mod.web.internal.header.ContentTypeCodec;
import io.winterframework.mod.web.lab.router.BaseContext;
import io.winterframework.mod.web.lab.router.BaseRoute;
import io.winterframework.mod.web.lab.router.BaseRouter;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
public class GenericBaseRouter implements BaseRouter<RequestBody, BaseContext, ResponseBody>, RoutingChain {

	private RoutingLink<?> firstLink;
	
	private RequestHandler<RequestBody, RoutingException, ResponseBody> errorHandler;
	
	public GenericBaseRouter() {
		this.firstLink = new PathRoutingLink();
		this.firstLink
			.connect(new PathPatternRoutingLink())
			.connect(new MethodRoutingLink())
			.connect(new ConsumesRoutingLink(new AcceptCodec(false)))
			.connect(new ProducesRoutingLink(new ContentTypeCodec()))
			.connect(new LanguageRoutingLink(new AcceptLanguageCodec(false)))
			.connect(new LastRoutingLink());
		
		this.errorHandler = (request, response) -> {
			String status = Integer.toString(request.context().getStatus());
			
			ByteArrayOutputStream stackTraceOut = new ByteArrayOutputStream();
			PrintStream stackTracePrint = new PrintStream(stackTraceOut);
			stackTracePrint.append("<html><head><title>").append(status).append(" ").append(request.context().getMessage()).append("</title></head><body>");
			stackTracePrint.append("<h1><span style=\"color:red;\">").append(status).append("</span> ").append(request.context().getMessage()).append("</h1>");
			stackTracePrint.append("<hr/>");
			stackTracePrint.append("<pre>");
			request.context().printStackTrace(new PrintStream(stackTracePrint));
			stackTracePrint.append("</pre>");
			stackTracePrint.append("</body>");
			response.headers(headers -> headers.status(request.context().getStatus()).contentType("text/html")).body().data().data(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(stackTraceOut.toByteArray()))));
		};
	}

	@Override
	public GenericBaseRouteManager route() {
		return new GenericBaseRouteManager(this);
	}

	@Override
	public void addRoute(BaseRoute<RequestBody, BaseContext, ResponseBody> route) {
		this.firstLink.addRoute(route);
	}

	@Override
	public void disableRoute(BaseRoute<RequestBody, BaseContext, ResponseBody> route) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void enableRoute(BaseRoute<RequestBody, BaseContext, ResponseBody> route) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeRoute(BaseRoute<RequestBody, BaseContext, ResponseBody> route) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handle(Request<RequestBody, Void> request, Response<ResponseBody> response) {
		Request<RequestBody, BaseContext> baseRequest = request.map(Function.identity(), ign -> new GenericBaseContext());
		try {
			this.firstLink.handle(baseRequest, response);
		} 
		catch (RoutingException e) {
			// TODO we should probably report the error
			
			RequestHandler<RequestBody, Void, ResponseBody> mappedErrorHandler = this.errorHandler.map(handler -> {
				return (req, resp) -> {
					handler.handle(req.map(Function.identity(), ign -> e), resp);
				};
			});
			mappedErrorHandler.handle(request, response);
		}
		// This is not that simple, if something has already been written to the client (typically the headers) we have to terminate the response but we can't report a proper error to the client
		/*catch (Throwable t) {
			this.handler_500.handle(request, response);
		}*/
	}
}

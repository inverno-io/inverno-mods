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
package io.inverno.mod.grpc.server;

import com.google.protobuf.Message;
import io.inverno.core.v1.Application;
import io.inverno.mod.base.concurrent.Reactor;
import io.inverno.mod.base.net.NetService;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.HttpServerConfigurationLoader;
import io.inverno.mod.http.server.Server;
import io.inverno.mod.http.server.ServerController;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public class Readme {

	public void doc() {
		GrpcServer grpcServer = null;
		NetService netService = null;
		Reactor reactor = null;
		ResourceService resourceService = null;
		
		Application.with(new Server.Builder(netService, reactor, resourceService)
			.setConfiguration(HttpServerConfigurationLoader.load(conf -> conf.server_port(8080).h2_enabled(true)))
			.setController(ServerController.from(
				grpcServer.unary(
					HelloRequest.getDefaultInstance(), 
					HelloReply.getDefaultInstance(), 
					(GrpcExchange.Unary<ExchangeContext, HelloRequest, HelloReply> grpcExchange) -> {
						grpcExchange.response().value(
							grpcExchange.request().value().map(request -> HelloReply.newBuilder().setMessage("Hello " + request.getName()).build())
						);
					}
				)
			))
		).run();
	}
	
	private static interface HelloRequest extends Message {
		
		String getName();
		
		static HelloRequest getDefaultInstance() {
			return null;
		}
	}
	
	private static interface HelloReply extends Message {
		
		static HelloReply getDefaultInstance() {
			return null;
		}
		
		static Builder newBuilder() {
			return null;
		}
		
		interface Builder {
			Builder setMessage(String name);
			
			HelloReply build();
		}
	}
}

package io.inverno.mod.grpc.client;


import com.google.protobuf.Message;
import io.inverno.mod.grpc.base.GrpcServiceName;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.client.Endpoint;
import io.inverno.mod.http.client.HttpClient;
import io.inverno.mod.http.client.HttpClientConfigurationLoader;
import java.util.Set;

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
/**
 * <p>
 * 
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public class Readme {

	public void doc() {
		GrpcClient grpcClient = null;
		HttpClient httpClient = null;
		
		Endpoint<ExchangeContext> h2Endpoint = httpClient.endpoint("localhost", 8080)
			.configuration(HttpClientConfigurationLoader.load(conf -> conf.http_protocol_versions(Set.of(HttpVersion.HTTP_2_0))))
			.build();
		
		HelloReply reply = h2Endpoint.exchange()
			.map(exchange -> (GrpcExchange.Unary<ExchangeContext, HelloRequest, HelloReply>)grpcClient.unary(
					exchange,
					GrpcServiceName.of("helloworld", "Greeter"), "SayHello",
					HelloRequest.getDefaultInstance(), HelloReply.getDefaultInstance()
				)
			)
			.flatMap(grpcExchange -> {
				grpcExchange.request().value(
					HelloRequest.newBuilder()
						.setName("Bob")
						.build()
				);
				return grpcExchange.response();
			})
			.flatMap(GrpcResponse.Unary::value)
			.block();
	}
	
	private static interface HelloRequest extends Message {
		
		static HelloRequest getDefaultInstance() {
			return null;
		}
		
		static Builder newBuilder() {
			return null;
		}
		
		interface Builder {
			Builder setName(String name);
			
			HelloRequest build();
		}
	}
	
	private static interface HelloReply extends Message {
		
		static HelloReply getDefaultInstance() {
			return null;
		}
	}
}

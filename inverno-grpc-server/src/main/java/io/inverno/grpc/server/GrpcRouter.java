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
package io.inverno.grpc.server;

import io.grpc.MethodDescriptor;
import io.inverno.mod.http.base.ExchangeContext;
import java.util.List;

/**
 * <p>
 * 
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.8
 */
public interface GrpcRouter<A extends ExchangeContext> {

	@SuppressWarnings("unchecked")
	default GrpcRouter<A> configure(GrpcRouterConfigurer<? super A> configurer) {
		configurer.configure((GrpcRouter)this);
		return this;
	}
	
	default GrpcRouter<A> configure(List<GrpcRouterConfigurer<? super A>> configurers) {
		if(configurers != null) {
			for(GrpcRouterConfigurer configurer : configurers) {
				this.configure(configurer);
			}
		}
		return this;
	}
	
	<ReqT, RespT> GrpcServiceManager<A, ReqT, RespT> service(MethodDescriptor<ReqT, RespT> methodDescriptor);
}

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
package io.winterframework.mod.web.internal.router;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import io.winterframework.mod.web.Request;
import io.winterframework.mod.web.Response;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.WebException;
import io.winterframework.mod.web.router.ErrorRoute;

/**
 * @author jkuhn
 *
 */
public class ThrowableRoutingLink extends RoutingLink<Void, ResponseBody, Throwable, ThrowableRoutingLink, ErrorRoute> {

	private Map<Class<? extends Throwable>, RoutingLink<Void, ResponseBody, Throwable, ?, ErrorRoute>> handlers;
	
	private static final Comparator<Class<? extends Throwable>> CLASS_COMPARATOR = (t1, t2) -> {
		if(t1.isAssignableFrom(t2)) {
			return -1;
		}
		else if(t2.isAssignableFrom(t1)) {
			return 1;
		}
		else {
			return 0;
		}
	};
	
	public ThrowableRoutingLink() {
		super(ThrowableRoutingLink::new);
		this.handlers = new LinkedHashMap<>();
	}

	@Override
	public ThrowableRoutingLink addRoute(ErrorRoute route) {
		if(route.getErrors() != null && !route.getErrors().isEmpty()) {
			route.getErrors().stream()
				.forEach(error -> {
					if(this.handlers.containsKey(error)) {
						this.handlers.get(error).addRoute(route);
					}
					else {
						this.handlers.put(error, this.nextLink.createNextLink().addRoute(route));
					}
				});
			
			
			this.handlers = this.handlers.entrySet().stream().sorted(Comparator.comparing(Entry::getKey, CLASS_COMPARATOR)).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a,b) -> a, LinkedHashMap::new));
		}
		else {
			this.nextLink.addRoute(route);
		}
		return this;
	}

	@Override
	public void handle(Request<Void, Throwable> request, Response<ResponseBody> response) throws WebException {
		// We take the first match, or we delegate to the next link
		this.handlers.entrySet().stream()
			.filter(e -> e.getKey().isAssignableFrom(request.context().getClass()))
			.findFirst()
			.map(Entry::getValue)
			.ifPresentOrElse(
				handler -> handler.handle(request, response),
				() -> this.nextLink.handle(request, response)
			);
	}
}

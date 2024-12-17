/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.web.compiler.internal.server;

import io.inverno.mod.base.net.URIs;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.server.annotation.WebRoute;
import io.inverno.mod.web.server.annotation.WebSocketRoute;
import io.inverno.mod.web.compiler.spi.server.WebServerControllerInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerRouteInfo;
import io.inverno.mod.web.compiler.spi.server.WebSocketServerRouteInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * A duplicate route detector is used to detect duplicate in the routes defined in a module.
 * </p>
 *
 * <p>
 * Two routes are considered equals when they defined the exact same criteria, namely path, method, consume, produce and language.
 * </p>
 *
 * <p>
 * Multiple criteria can be specified in {@link WebRoute @WebRoute} or a {@link WebSocketRoute @WebSocketRoute} annotation, the detector analyzes all combinations to detect overlapping route
 * definition.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class WebServerRouteClashDetector {
	
	/**
	 * <p>
	 * Finds the duplicates in the specified list of Web routes.
	 * </p>
	 * 
	 * @param routes a list of Web routes
	 * 
	 * @return a map indicating the duplicated routes of each route
	 */
	public Map<WebServerRouteInfo, Set<WebServerRouteInfo>> findDuplicates(List<? extends WebServerRouteInfo> routes) {
		Map<WebServerRouteInfo, Set<WebServerRouteInfo>> result = new HashMap<>();
		int routeIndex = 0;
		for(WebServerRouteInfo route : routes) {
			List<WebServerRouteInfo> duplicates = this.visitRoute(route, routes.subList(++routeIndex, routes.size()));
			if(!duplicates.isEmpty()) {
				Set<WebServerRouteInfo> routeDuplicates = result.computeIfAbsent(route, k -> new HashSet<>());
				routeDuplicates.addAll(duplicates);
				for(WebServerRouteInfo duplicateRoute : routeDuplicates) {
					Set<WebServerRouteInfo> duplicateRouteDuplicates = result.computeIfAbsent(duplicateRoute, k -> new HashSet<>());
					duplicateRouteDuplicates.add(route);
					duplicateRouteDuplicates.addAll(routeDuplicates.stream().filter(duplicate -> !duplicate.equals(duplicateRoute)).collect(Collectors.toSet()));
				}
			}
		}
		return result;
	}
	
	private List<WebServerRouteInfo> visitRoute(WebServerRouteInfo route, List<? extends WebServerRouteInfo> routes) {
		if(routes.isEmpty()) {
			return List.of();
		}
		if(route.getPaths().length > 0) {
			List<WebServerRouteInfo> duplicates = new ArrayList<>();
			for(String path : route.getPaths()) {
				duplicates.addAll(this.visitPath(route, route.getController().map(WebServerControllerInfo::getRootPath).map(rootPath -> URIs.uri(rootPath, URIs.RequestTargetForm.PATH).path(path, false).buildRawPath()).orElse(path), routes));
			}
			return duplicates;
		}
		else {
			return route.getController()
				.map(WebServerControllerInfo::getRootPath)
				.map(rootPath -> URIs.uri(rootPath, URIs.RequestTargetForm.PATH, URIs.Option.PARAMETERIZED, URIs.Option.NORMALIZED, URIs.Option.PATH_PATTERN).buildRawPath())
				.map(rootPath -> this.visitPath(route, rootPath, routes))
				.orElseGet(() -> this.visitPath(route, null, routes));
		}
	}
	
	private List<WebServerRouteInfo> visitPath(WebServerRouteInfo route, String path, List<? extends WebServerRouteInfo> routes) {
		if(routes.isEmpty()) {
			return List.of();
		}
		List<? extends WebServerRouteInfo> reducedRoutes = routes.stream()
			.filter(r -> {
				if(path != null) {
					for(String p : r.getPaths()) {
						String absolutePath = r.getController().map(WebServerControllerInfo::getRootPath).map(rootPath -> URIs.uri(rootPath, URIs.RequestTargetForm.PATH).path(p, false).buildRawPath()).orElse(p);
						boolean pathMatch = absolutePath.equals(path);
						if(!pathMatch && route.isMatchTrailingSlash()) {
							String otherPath;
							if(path.endsWith("/")) {
								otherPath = path.substring(0, path.length() - 1);
							}
							else {
								otherPath = path + "/";
							}
							pathMatch = absolutePath.equals(otherPath);
						}
						return pathMatch;
					}
					return false;
				}
				else {
					return r.getPaths().length == 0;
				}
			})
			.collect(Collectors.toList());
		if(route.getMethods().length > 0) {
			List<WebServerRouteInfo> duplicates = new ArrayList<>();
			for(Method method : route.getMethods()) {
				duplicates.addAll(this.visitMethod(route, method, reducedRoutes));
			}
			return duplicates;
		}
		else {
			return this.visitMethod(route, null, reducedRoutes);
		}
	}
	
	private List<WebServerRouteInfo> visitMethod(WebServerRouteInfo route, Method method, List<? extends WebServerRouteInfo> routes) {
		if(routes.isEmpty()) {
			return List.of();
		}
		List<? extends WebServerRouteInfo> reducedRoutes = routes.stream()
			.filter(r -> {
				if(method != null) {
					return Arrays.binarySearch(r.getMethods(), method) >= 0;
				}
				else {
					return r.getMethods().length == 0;
				}
			})
			.collect(Collectors.toList());
		if(route.getConsumes().length > 0) {
			List<WebServerRouteInfo> duplicates = new ArrayList<>();
			for(String consume : route.getConsumes()) {
				duplicates.addAll(this.visitConsumes(route, consume, reducedRoutes));
			}
			return duplicates;
		}
		else {
			return this.visitConsumes(route, null, reducedRoutes);
		}
	}
	
	private List<WebServerRouteInfo> visitConsumes(WebServerRouteInfo route, String consumes, List<? extends WebServerRouteInfo> routes) {
		if(routes.isEmpty()) {
			return List.of();
		}
		List<? extends WebServerRouteInfo> reducedRoutes = routes.stream()
			.filter(r -> {
				if(consumes != null) {
					return Arrays.binarySearch(r.getConsumes(), consumes) >= 0;
				}
				else {
					return r.getConsumes().length == 0;
				}
			})
			.collect(Collectors.toList());
		if(route.getProduces().length > 0) {
			List<WebServerRouteInfo> duplicates = new ArrayList<>();
			for(String produce : route.getProduces()) {
				duplicates.addAll(this.visitProduces(route, produce, reducedRoutes));
			}
			return duplicates;
		}
		else {
			return this.visitProduces(route, null, reducedRoutes);
		}
	}
	
	private List<WebServerRouteInfo> visitProduces(WebServerRouteInfo route, String produces, List<? extends WebServerRouteInfo> routes) {
		if(routes.isEmpty()) {
			return List.of();
		}
		List<? extends WebServerRouteInfo> reducedRoutes = routes.stream()
			.filter(r -> {
				if(produces != null) {
					return Arrays.binarySearch(r.getProduces(), produces) >= 0;
				}
				else {
					return r.getProduces().length == 0;
				}
			})
			.collect(Collectors.toList());
		
		if(route.getLanguages().length > 0) {
			List<WebServerRouteInfo> duplicates = new ArrayList<>();
			for(String language : route.getLanguages()) {
				duplicates.addAll(this.visitLanguage(route, language, reducedRoutes));
			}
			return duplicates;
		}
		else {
			return this.visitLanguage(route, null, reducedRoutes);
		}
	}
	
	private List<WebServerRouteInfo> visitLanguage(WebServerRouteInfo route, String language, List<? extends WebServerRouteInfo> routes) {
		if(routes.isEmpty()) {
			return List.of();
		}
		List<WebServerRouteInfo> reducedRoutes = routes.stream()
			.filter(r -> {
				if(language != null) {
					return Arrays.binarySearch(r.getLanguages(), language) >= 0;
				}
				else {
					return r.getLanguages().length == 0;
				}
			})
			.collect(Collectors.toList());
		
		if(route instanceof WebSocketServerRouteInfo && ((WebSocketServerRouteInfo)route).getSubprotocols().length > 0) {
			List<WebServerRouteInfo> duplicates = new ArrayList<>();
			for(String subprotocol : ((WebSocketServerRouteInfo)route).getSubprotocols()) {
				duplicates.addAll(this.visitSubProtocol(((WebSocketServerRouteInfo)route), subprotocol, reducedRoutes));
			}
			return duplicates;
		}
		else {
			return reducedRoutes;
		}
	}
	
	private List<WebServerRouteInfo> visitSubProtocol(WebSocketServerRouteInfo route, String subprotocol, List<? extends WebServerRouteInfo> routes) {
		if(routes.isEmpty()) {
			return List.of();
		}
		return routes.stream()
			.filter(r -> {
				if(!(r instanceof WebSocketServerRouteInfo)) {
					return false;
				}
				if(subprotocol != null) {
					return Arrays.binarySearch(((WebSocketServerRouteInfo)r).getSubprotocols(), subprotocol) >= 0;
				}
				else {
					return ((WebSocketServerRouteInfo)r).getSubprotocols().length == 0;
				}
			})
			.collect(Collectors.toList());
	}
}

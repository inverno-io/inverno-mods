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
package io.winterframework.mod.web.compiler.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.winterframework.mod.base.net.URIs;
import io.winterframework.mod.http.base.Method;
import io.winterframework.mod.web.compiler.spi.WebControllerInfo;
import io.winterframework.mod.web.compiler.spi.WebRouteInfo;

/**
 * @author jkuhn
 *
 */
class WebRouteDuplicateDetector {
	
	public Map<WebRouteInfo, Set<WebRouteInfo>> findDuplicates(List<? extends WebRouteInfo> routes) {
		Map<WebRouteInfo, Set<WebRouteInfo>> result = new HashMap<>();
		int routeIndex = 0;
		for(WebRouteInfo route : routes) {
			List<WebRouteInfo> duplicates = this.visitRoute(route, routes.subList(++routeIndex, routes.size()));
			if(!duplicates.isEmpty()) {
				Set<WebRouteInfo> routeDuplicates = result.get(route);
				if(routeDuplicates == null) {
					routeDuplicates = new HashSet<>();
					result.put(route, routeDuplicates);
				}
				routeDuplicates.addAll(duplicates);
				for(WebRouteInfo duplicateRoute : routeDuplicates) {
					Set<WebRouteInfo> duplicateRouteDuplicates = result.get(duplicateRoute);
					if(duplicateRouteDuplicates == null) {
						duplicateRouteDuplicates = new HashSet<>();
						result.put(duplicateRoute, duplicateRouteDuplicates);
					}
					duplicateRouteDuplicates.add(route);
					duplicateRouteDuplicates.addAll(routeDuplicates.stream().filter(duplicate -> !duplicate.equals(duplicateRoute)).collect(Collectors.toSet()));
				}
			}
		}
		return result;
	}
	
	private List<WebRouteInfo> visitRoute(WebRouteInfo route, List<? extends WebRouteInfo> routes) {
		if(routes.isEmpty()) {
			return List.of();
		}
		if(route.getPaths().length > 0) {
			List<WebRouteInfo> duplicates = new ArrayList<>();
			for(String path : route.getPaths()) {
				duplicates.addAll(this.visitPath(route, route.getController().map(WebControllerInfo::getRootPath).map(rootPath -> URIs.uri(rootPath).path(path, false).buildRawPath()).orElse(path), route.isMatchTrailingSlash(), routes));
			}
			return duplicates;
		}
		else {
			return this.visitPath(route, null, false, routes);
		}
	}
	
	private List<WebRouteInfo> visitPath(WebRouteInfo route, String path, boolean matchingTrailingSlash, List<? extends WebRouteInfo> routes) {
		if(routes.isEmpty()) {
			return List.of();
		}
		List<? extends WebRouteInfo> reducedRoutes = routes.stream()
			.filter(r -> {
				if(path != null) {
					boolean pathMatch = Arrays.binarySearch(r.getPaths(), path) >= 0;
					if(route.isMatchTrailingSlash()) {
						String otherPath;
						if(path.endsWith("/")) {
							otherPath = path.substring(0, path.length() - 1);
						}
						else {
							otherPath = path + "/";
						}
						pathMatch |= Arrays.binarySearch(r.getPaths(), otherPath) >= 0;
					}
					return pathMatch;
				}
				else {
					return r.getPaths().length == 0;
				}
			})
			.collect(Collectors.toList());
		if(route.getMethods().length > 0) {
			List<WebRouteInfo> duplicates = new ArrayList<>();
			for(Method method : route.getMethods()) {
				duplicates.addAll(this.visitMethod(route, method, reducedRoutes));
			}
			return duplicates;
		}
		else {
			return this.visitMethod(route, null, reducedRoutes);
		}
	}
	
	private List<WebRouteInfo> visitMethod(WebRouteInfo route, Method method, List<? extends WebRouteInfo> routes) {
		if(routes.isEmpty()) {
			return List.of();
		}
		List<? extends WebRouteInfo> reducedRoutes = routes.stream()
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
			List<WebRouteInfo> duplicates = new ArrayList<>();
			for(String consume : route.getConsumes()) {
				duplicates.addAll(this.visitConsumes(route, consume, reducedRoutes));
			}
			return duplicates;
		}
		else {
			return this.visitConsumes(route, null, reducedRoutes);
		}
	}
	
	private List<WebRouteInfo> visitConsumes(WebRouteInfo route, String consumes, List<? extends WebRouteInfo> routes) {
		if(routes.isEmpty()) {
			return List.of();
		}
		List<? extends WebRouteInfo> reducedRoutes = routes.stream()
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
			List<WebRouteInfo> duplicates = new ArrayList<>();
			for(String produce : route.getProduces()) {
				duplicates.addAll(this.visitProduces(route, produce, reducedRoutes));
			}
			return duplicates;
		}
		else {
			return this.visitProduces(route, null, reducedRoutes);
		}
	}
	
	private List<WebRouteInfo> visitProduces(WebRouteInfo route, String produces, List<? extends WebRouteInfo> routes) {
		if(routes.isEmpty()) {
			return List.of();
		}
		List<? extends WebRouteInfo> reducedRoutes = routes.stream()
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
			List<WebRouteInfo> duplicates = new ArrayList<>();
			for(String language : route.getLanguages()) {
				duplicates.addAll(this.visitLanguage(route, language, reducedRoutes));
			}
			return duplicates;
		}
		else {
			return this.visitLanguage(route, null, reducedRoutes);
		}
	}
	
	private List<WebRouteInfo> visitLanguage(WebRouteInfo route, String language, List<? extends WebRouteInfo> routes) {
		if(routes.isEmpty()) {
			return List.of();
		}
		return routes.stream()
			.filter(r -> {
				if(language != null) {
					return Arrays.binarySearch(r.getLanguages(), language) >= 0;
				}
				else {
					return r.getLanguages().length == 0;
				}
			})
			.collect(Collectors.toList());
	}
}

package io.winterframework.mod.web.internal.router;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.winterframework.mod.commons.resource.MediaTypes;
import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.router.WebExchange;
import io.winterframework.mod.web.router.WebRoute;

class GenericWebRouterTest {

	@Test
	void testGetRoutes() {
		GenericWebRouter router = new GenericWebRouter();
		router
			.route().consumes(MediaTypes.APPLICATION_JSON).consumes(MediaTypes.TEXT_HTML).handler(exhange -> {})
			.route().method(Method.GET).method(Method.POST).language("fr-FR").language("en-US").handler(exhange -> {})
			.route().produces(MediaTypes.APPLICATION_JSON).handler(exhange -> {})
			.route().produces(MediaTypes.TEXT_HTML).handler(exhange -> {})
			.route().path("/hello", true).handler(exchange -> {})
			.route().path("/hello/{param1}").handler(exchange -> {})
			.route().path("/hello/{param1}/{param2:[a-b]*}").handler(exchange -> {});
		
		Set<WebRoute<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>>> routes = router.getRoutes();
		
		// 1. consume=JSON *
		// 2. consume=HTML *
		// 3. method=GET, language=fr-FR *
		// 4. method=GET, language=en-US *
		// 5. method=POST, language=fr-FR *
		// 6. method=POST, language=en-US *
		// 7. produce=JSON *
		// 8. produce=HTML *
		// 9. path = /hello
		// 10. path = /hello/
		// 11. path = /hello/{param1}
		// 12. path = /hello/{param1}/{param2:[a-b]*}
		
		Assertions.assertEquals(12, routes.size());
		
		GenericWebRoute route1 = new GenericWebRoute(router);
		route1.setConsumes(Set.of(MediaTypes.APPLICATION_JSON));
		
		GenericWebRoute route2 = new GenericWebRoute(router);
		route2.setConsumes(Set.of(MediaTypes.TEXT_HTML));
		
		GenericWebRoute route3 = new GenericWebRoute(router);
		route3.setMethods(Set.of(Method.GET));
		route3.setLanguages(Set.of("fr-FR"));
		
		GenericWebRoute route4 = new GenericWebRoute(router);
		route4.setMethods(Set.of(Method.GET));
		route4.setLanguages(Set.of("en-US"));
		
		GenericWebRoute route5 = new GenericWebRoute(router);
		route5.setMethods(Set.of(Method.POST));
		route5.setLanguages(Set.of("fr-FR"));
		
		GenericWebRoute route6 = new GenericWebRoute(router);
		route6.setMethods(Set.of(Method.POST));
		route6.setLanguages(Set.of("en-US"));
		
		GenericWebRoute route7 = new GenericWebRoute(router);
		route7.setProduces(Set.of(MediaTypes.APPLICATION_JSON));
		
		GenericWebRoute route8 = new GenericWebRoute(router);
		route8.setProduces(Set.of(MediaTypes.TEXT_HTML));
		
		GenericWebRoute route9 = new GenericWebRoute(router);
		route9.setPath("/hello");
		
		GenericWebRoute route10 = new GenericWebRoute(router);
		route10.setPath("/hello/");
		
		GenericWebRoute route11 = new GenericWebRoute(router);
		route11.setPath("/hello/{param1}");
		List<String> pathParams11 = new ArrayList<>();
		pathParams11.add(null);
		pathParams11.add(":param1");
		route11.setPathPattern(new GenericWebRoute.GenericPathPattern("/hello/{param1}", Pattern.compile("^(\\Q/hello/\\E)(.+)$"), pathParams11));
		
		GenericWebRoute route12 = new GenericWebRoute(router);
		route12.setPath("/hello/{param1}/{param2:[a-b]*}");
		List<String> pathParams12 = new ArrayList<>();
		pathParams11.add(null);
		pathParams11.add(":param1");
		pathParams11.add(null);
		pathParams11.add(":param2");
		route12.setPathPattern(new GenericWebRoute.GenericPathPattern("/hello/{param1}/{param2:[a-b]*}", Pattern.compile("^(\\Q/hello/\\E)(.+)(\\Q/\\E)([a-b]*)$"), pathParams12));
		
		Assertions.assertEquals(Set.of(route1, route2, route3, route4, route5, route6, route7, route8, route9, route10, route11, route12), routes);
	}
	
	@Test
	void testFindRoutes() {
		GenericWebRouter router = new GenericWebRouter();
		router
			.route().consumes(MediaTypes.APPLICATION_JSON).consumes(MediaTypes.TEXT_HTML).handler(exhange -> {})
			.route().method(Method.GET).method(Method.POST).language("fr-FR").language("en-US").handler(exhange -> {})
			.route().produces(MediaTypes.APPLICATION_JSON).handler(exhange -> {})
			.route().produces(MediaTypes.TEXT_HTML).handler(exhange -> {})
			.route().path("/hello", true).handler(exchange -> {})
			.route().path("/hello/{param1}").handler(exchange -> {})
			.route().path("/hello/{param1}/{param2:[a-b]*}").handler(exchange -> {});
		
		GenericWebRoute route1 = new GenericWebRoute(router);
		route1.setConsumes(Set.of(MediaTypes.APPLICATION_JSON));
		
		GenericWebRoute route2 = new GenericWebRoute(router);
		route2.setMethods(Set.of(Method.GET));
		route2.setLanguages(Set.of("fr-FR"));
		
		GenericWebRoute route3 = new GenericWebRoute(router);
		route3.setMethods(Set.of(Method.POST));
		route3.setLanguages(Set.of("fr-FR"));
		
		Set<WebRoute<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>>> routes = router.route().language("fr-FR").findRoutes();
		Assertions.assertEquals(2, routes.size());
		Assertions.assertEquals(Set.of(route2, route3), routes);
		
		routes = router.route().method(Method.GET).language("fr-FR").findRoutes();
		Assertions.assertEquals(1, routes.size());
		Assertions.assertEquals(Set.of(route2), routes);
		
		routes = router.route().method(Method.PUT).language("fr-FR").findRoutes();
		Assertions.assertEquals(0, routes.size());
		
		routes = router.route().consumes(MediaTypes.APPLICATION_JSON).findRoutes();
		Assertions.assertEquals(1, routes.size());
		Assertions.assertEquals(Set.of(route1), routes);
	}
	
	@Test
	void testRouteRemove() {
		GenericWebRouter router = new GenericWebRouter();
		router
			.route().consumes(MediaTypes.APPLICATION_JSON).consumes(MediaTypes.TEXT_HTML).handler(exhange -> {})
			.route().method(Method.GET).method(Method.POST).language("fr-FR").language("en-US").handler(exhange -> {})
			.route().produces(MediaTypes.APPLICATION_JSON).handler(exhange -> {})
			.route().produces(MediaTypes.TEXT_HTML).handler(exhange -> {})
			.route().path("/hello", true).handler(exchange -> {})
			.route().path("/hello/{param1}").handler(exchange -> {})
			.route().path("/hello/{param1}/{param2:[a-b]*}").handler(exchange -> {});
		
		Set<WebRoute<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>>> routes = router.getRoutes();
		Assertions.assertEquals(12, routes.size());
		
		routes = router.route().consumes(MediaTypes.APPLICATION_JSON).findRoutes();
		WebRoute<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>> removedRoute = routes.iterator().next();
		removedRoute.remove();
		
		routes = router.getRoutes();
		Assertions.assertEquals(11, routes.size());
	}

}

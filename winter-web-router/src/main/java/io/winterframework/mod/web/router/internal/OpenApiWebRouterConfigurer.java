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
package io.winterframework.mod.web.router.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.winterframework.mod.base.Charsets;
import io.winterframework.mod.base.resource.MediaTypes;
import io.winterframework.mod.base.resource.Resource;
import io.winterframework.mod.base.resource.ResourceService;
import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.NotFoundException;
import io.winterframework.mod.web.router.MissingRequiredParameterException;
import io.winterframework.mod.web.router.WebExchange;
import io.winterframework.mod.web.router.WebRouter;
import io.winterframework.mod.web.router.WebRouterConfiguration;
import io.winterframework.mod.web.router.WebRouterConfigurer;
import io.winterframework.mod.web.router.annotation.PathParam;

/**
 * @author jkuhn
 *
 */
public class OpenApiWebRouterConfigurer implements WebRouterConfigurer<WebExchange> {

	private final WebRouterConfiguration configuration;
	private final Map<String, Resource> openApiSpecs;
	
	public OpenApiWebRouterConfigurer(WebRouterConfiguration configuration, ResourceService resourceService) {
		this.configuration = configuration;
		try {
			this.openApiSpecs = new HashMap<>();
			for(Module module : this.getClass().getModule().getLayer().modules()) {
				Resource openApiSpec = resourceService.getResource(new URI("module://" + module.getName() + "/META-INF/winter/web/openapi.yml"));
				Boolean exists = openApiSpec.exists();
				if(exists != null && exists) {
					this.openApiSpecs.put(module.getName(), openApiSpec);
				}
			}
		} catch (URISyntaxException e) {
			throw new RuntimeException("Error fetching open api specifications", e);
		}
	}
	
	@Override
	public void accept(WebRouter<WebExchange> router) {
		router
			.route().path("/open-api/{moduleName}", false).method(Method.GET).handler(exchange -> {
				exchange.response().body().resource().value(this.getSpec(exchange.request().pathParameters().get("moduleName").map(parameter -> parameter.as(String.class)).orElseThrow(() -> new MissingRequiredParameterException("moduleName"))));
			})
			.route().path("/open-api", true).method(Method.GET).produces(MediaTypes.APPLICATION_JSON).handler(exchange -> {
				exchange.response().body().raw().value(this.listSpec());
			});
		
		if(this.configuration.enable_webjars()) {
			router.route().path("/open-api/{moduleName}", false).method(Method.GET).produces(MediaTypes.TEXT_HTML).handler(exchange -> {
				exchange.response().body().raw().value(this.getSpecSwaggerUI(exchange.request().pathParameters().get("moduleName").map(parameter -> parameter.as(String.class)).orElseThrow(() -> new MissingRequiredParameterException("moduleName"))));
			})
			.route().path("/open-api", true).method(Method.GET).produces(MediaTypes.TEXT_HTML).handler(exchange -> {
				exchange.response().body().raw().value(this.listSpecSwaggerUI());
			});
		}
	}
	
	public ByteBuf listSpec() {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("[" + this.openApiSpecs.entrySet().stream().map(e -> "{\"name\":\"" + e.getKey() + "\",\"url\":\"" + "/open-api/" + e.getKey()+ "\"}").collect(Collectors.joining(", ")) + "]", Charsets.DEFAULT));
	}
	
	public ByteBuf listSpecSwaggerUI() {
		StringBuilder configurationSnippet = new StringBuilder();
		configurationSnippet.append("\"layout\": \"StandaloneLayout\", ");
		configurationSnippet.append("\"urls\": [");
		configurationSnippet.append(this.openApiSpecs.entrySet().stream().map(e -> "{\"name\":\"" + e.getKey() + "\",\"url\":\"" + "/open-api/" + e.getKey()+ "\"}").collect(Collectors.joining(", ")));
		configurationSnippet.append("]");
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.buildSwaggerUI(configurationSnippet.toString()).toString(), Charsets.DEFAULT));
	}
	
	public Resource getSpec(@PathParam String moduleName) {
		Resource spec = this.openApiSpecs.get(moduleName);
		if(spec == null) {
			throw new NotFoundException();
		}
		return spec;
	}
	
	public ByteBuf getSpecSwaggerUI(@PathParam String moduleName) {
		if(!this.openApiSpecs.containsKey(moduleName)) {
			throw new NotFoundException();
		}
		StringBuilder configurationSnippet = new StringBuilder();
		configurationSnippet.append("\"layout\": \"BaseLayout\", ");
		configurationSnippet.append("\"url\": \"/open-api/").append(moduleName).append("\"");
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.buildSwaggerUI(configurationSnippet.toString()).toString(), Charsets.DEFAULT));
	}
	
	private StringBuilder buildSwaggerUI(String configurationSnippet) {
		StringBuilder result = new StringBuilder();
		
		result.append("<!DOCTYPE html>");
		result.append("<html lang=\"en\">");
		
		result.append("<head>");
		result.append("<meta charset= \"UTF-8\"/>");
		result.append("<title>Winter OpenAPI</title>");
		result.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"/webjars/swagger-ui/swagger-ui.css\" />");
		result.append("<link rel=\"icon\" type=\"image/svg+xml\" href=\"/favicon.ico\" />");
		result.append("<style>");
		result.append("html {");
		result.append("box-sizing: border-box;");
		result.append("overflow: -moz-scrollbars-vertical;");
		result.append("overflow-y: scroll;");
		result.append("}");
		result.append("*, *:before, *:after {");
		result.append("box-sizing: inherit;");
		result.append("}");
		result.append("body {");
		result.append("margin: 0;");
		result.append("background: #fafafa;");
		result.append("}");
		result.append("</style>");
		result.append("</head>");
		
		result.append("<body>");
		result.append("<div id=\"swagger-ui\"></div>");
		result.append("<script src=\"/webjars/swagger-ui/swagger-ui-bundle.js\" charset=\"UTF-8\"> </script>");
		result.append("<script src=\"/webjars/swagger-ui/swagger-ui-standalone-preset.js\" charset=\"UTF-8\"> </script>");
		result.append("<script>");
		
		result.append("window.onload = function() {");
		result.append("const ui = SwaggerUIBundle({");
		result.append("\"presets\": [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset], ");
		result.append("\"plugins\": [SwaggerUIBundle.plugins.DownloadUrl], ");
		result.append("\"dom_id\": \"#swagger-ui\", ");
		result.append("\"deepLinking\": true, ");
		result.append(configurationSnippet);
		result.append("});");
		//.append(this.objectMapper.writeValueAsString(configuration)).append(");");
		result.append("window.ui = ui;");
		result.append("}");
		
		result.append("</script>");
		result.append("</body>");
		
		result.append("</html>");
		
		return result;
	}

}

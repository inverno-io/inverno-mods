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
package io.inverno.mod.web.server;

import io.inverno.mod.base.Charsets;
import io.inverno.mod.base.ApplicationRuntime;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.web.base.MissingRequiredParameterException;
import io.inverno.mod.web.base.annotation.PathParam;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>
 * Web router configurer used to configure routes exposing generated <a href="https://www.openapis.org/">Open API</a> specifications.
 * </p>
 *
 * <p>
 * This configurer defines the following routes:
 * </p>
 *
 * <dl>
 * <dt>{@code /open-api}</dt>
 * <dd>Returns the JSON list the Open API specifications</dd>
 * <dd>If Swagger UI is enabled and request is made with {@code accept: text/html} header, a Swagger UI presenting all the specifications is returned</dd>
 * <dt>{@code /open-api/{moduleName}}</dt>
 * <dd>Returns the YAML Open API specification of the specified module</dd>
 * <dd>If Swagger UI is enabled and request is made with {@code accept: text/html} header, a Swagger UI presenting the module's specification is returned</dd>
 * </dl>
 *
 * <p>
 * Note that Swagger UI requires WebJars support and the {@code swagger-ui} WebJar.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 *
 * @see WebJarsRoutesConfigurer
 * 
 * @param <A> the exchange context type
 */
public class OpenApiRoutesConfigurer<A extends ExchangeContext> implements WebRouter.Configurer<A> {
	
	private static final Logger LOGGER = LogManager.getLogger(OpenApiRoutesConfigurer.class);
	
	private final Map<String, Resource> openApiSpecs;
	private final boolean enableSwagger;
	
	/**
	 * <p>
	 * Creates an Open API Web routes configurer with Swagger supprt and the specified resource service.
	 * </p>
	 * 
	 * @param resourceService the resource service
	 */
	public OpenApiRoutesConfigurer(ResourceService resourceService) {
		this(resourceService, true);
	}
	
	/**
	 * <p>
	 * Creates an Open API Web routes configurer with the specified resource service.
	 * </p>
	 *
	 * @param resourceService the resource service
	 * @param enableSwagger   true to enable Swagger UI routes, false otherwise
	 */
	public OpenApiRoutesConfigurer(ResourceService resourceService, boolean enableSwagger) {
		this.openApiSpecs = new HashMap<>();
		this.enableSwagger = enableSwagger;
		
		if(ApplicationRuntime.getApplicationRuntime() == ApplicationRuntime.IMAGE_NATIVE) {
			resourceService.getResources(URI.create("resource:/META-INF/inverno/web/server/"))
				.flatMap(resource -> {
					return resourceService.getResources(URI.create(resource.getURI().toString() + "/*/openapi.yml"));
				}).forEach(openApiSpec -> {
					String[] splitSpec = openApiSpec.getURI().getSchemeSpecificPart().split("/");
					final String moduleName = splitSpec[splitSpec.length - 2];
					if(!this.openApiSpecs.containsKey(moduleName)) {
						LOGGER.debug(() -> "Registered OpenAPI specification " + openApiSpec.getURI() + " for module " + moduleName);
						this.openApiSpecs.put(moduleName, openApiSpec);
					}
				});
		}
		else {
			ModuleLayer moduleLayer = this.getClass().getModule().getLayer();
			if(moduleLayer != null) {
				for(Module module : moduleLayer.modules()) {
					Resource openApiSpec = resourceService.getResource(URI.create("module://" + module.getName() + "/META-INF/inverno/web/server/" + module.getName() + "/openapi.yml"));
					if(openApiSpec.exists().orElse(false)) {
						LOGGER.debug(() -> "Registered OpenAPI specification " + openApiSpec.getURI() + " for module " + module.getName());
						this.openApiSpecs.put(module.getName(), openApiSpec);
					}
				}
			}

			resourceService.getResources(URI.create("classpath:/META-INF/inverno/web/server/"))
				.flatMap(resource -> {
					return resourceService.getResources(URI.create(resource.getURI().toString() + "/*/openapi.yml"));
				}).forEach(openApiSpec -> {
					String[] splitSpec = openApiSpec.getURI().getSchemeSpecificPart().split("/");
					final String moduleName = splitSpec[splitSpec.length - 2];
					if(!this.openApiSpecs.containsKey(moduleName)) {
						LOGGER.debug(() -> "Registered OpenAPI specification " + openApiSpec.getURI() + " for module " + moduleName);
						this.openApiSpecs.put(moduleName, openApiSpec);
					}
				});
		}
	}

	@Override
	public void configure(WebRouter<A> routes) {
		routes
			.route().path("/open-api", true).method(Method.GET).produce(MediaTypes.APPLICATION_JSON).handler(exchange -> {
				exchange.response().body().raw().value(this.listSpec());
			})
			.route().path("/open-api/{moduleName}", false).method(Method.GET).handler(exchange -> {
				exchange.response().body().resource().value(this.getSpec(exchange.request().pathParameters().get("moduleName").map(parameter -> parameter.as(String.class)).orElseThrow(() -> new MissingRequiredParameterException("moduleName"))));
			});

		if(this.enableSwagger) {
			routes
				.route().path("/open-api", true).method(Method.GET).produce(MediaTypes.TEXT_HTML).handler(exchange -> {
					exchange.response().body().raw().value(this.listSpecSwaggerUI());
				})
				.route().path("/open-api/{moduleName}", false).method(Method.GET).produce(MediaTypes.TEXT_HTML).handler(exchange -> {
					exchange.response().body().raw().value(this.getSpecSwaggerUI(exchange.request().pathParameters().get("moduleName").map(parameter -> parameter.as(String.class)).orElseThrow(() -> new MissingRequiredParameterException("moduleName"))));
				});
		}
	}

	/**
	 * <p>
	 * Returns a JSON list including all available Open API specifications.
	 * </p>
	 *
	 * @return a raw list of specifications
	 */
	public ByteBuf listSpec() {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("[" + this.openApiSpecs.entrySet().stream().map(e -> "{\"name\":\"" + e.getKey() + "\",\"url\":\"" + "/open-api/" + e.getKey()+ "\"}").collect(Collectors.joining(", ")) + "]", Charsets.DEFAULT));
	}
	
	/**
	 * <p>
	 * Returns the Swagger UI loading HTML for all available Open API specifications.
	 * </p>
	 *
	 * @return a raw Swagger UI loader
	 */
	public ByteBuf listSpecSwaggerUI() {
		StringBuilder configurationSnippet = new StringBuilder();
		configurationSnippet.append("\"layout\": \"StandaloneLayout\", ");
		configurationSnippet.append("\"urls\": [");
		configurationSnippet.append(this.openApiSpecs.entrySet().stream().map(e -> "{\"name\":\"" + e.getKey() + "\",\"url\":\"" + "/open-api/" + e.getKey()+ "\"}").collect(Collectors.joining(", ")));
		configurationSnippet.append("]");
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.buildSwaggerUI(configurationSnippet.toString()).toString(), Charsets.DEFAULT));
	}
	
	/**
	 * <p>
	 * Returns the Open API specification for the specified module usually in YAML format.
	 * </p>
	 *
	 * @param moduleName the module name
	 *
	 * @return a raw Open API specification
	 *
	 * @throws NotFoundException if there's no specification for the specified module
	 */
	public Resource getSpec(@PathParam String moduleName) throws NotFoundException {
		Resource spec = this.openApiSpecs.get(moduleName);
		if(spec == null) {
			throw new NotFoundException();
		}
		return spec;
	}
	
	/**
	 * <p>
	 * Returns the Swagger UI loading HTML for the Open API specification of the specified module
	 * </p>
	 *
	 * @param moduleName the module name
	 *
	 * @return a raw Swagger UI loader
	 *
	 * @throws NotFoundException if there's no specification for the specified module
	 */
	public ByteBuf getSpecSwaggerUI(@PathParam String moduleName) throws NotFoundException {
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
		result.append("<title>Inverno OpenAPI</title>");
		result.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"/webjars/swagger.ui/swagger-ui.css\" />");
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
		result.append("<script src=\"/webjars/swagger.ui/swagger-ui-bundle.js\" charset=\"UTF-8\"> </script>");
		result.append("<script src=\"/webjars/swagger.ui/swagger-ui-standalone-preset.js\" charset=\"UTF-8\"> </script>");
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

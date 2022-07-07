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
package io.inverno.mod.web.compiler.internal;

import io.inverno.mod.base.net.URIs;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.web.compiler.internal.WebServerControllerConfigurerOpenApiGenerationContext.GenerationMode;
import io.inverno.mod.web.compiler.internal.WebServerControllerConfigurerOpenApiGenerationContext.ResponseSpec;
import io.inverno.mod.web.compiler.spi.ErrorWebInterceptorsConfigurerInfo;
import io.inverno.mod.web.compiler.spi.ErrorWebRouterConfigurerInfo;
import io.inverno.mod.web.compiler.spi.ErrorWebRoutesConfigurerInfo;
import io.inverno.mod.web.compiler.spi.WebBasicParameterInfo;
import io.inverno.mod.web.compiler.spi.WebControllerInfo;
import io.inverno.mod.web.compiler.spi.WebCookieParameterInfo;
import io.inverno.mod.web.compiler.spi.WebExchangeContextParameterInfo;
import io.inverno.mod.web.compiler.spi.WebExchangeParameterInfo;
import io.inverno.mod.web.compiler.spi.WebFormParameterInfo;
import io.inverno.mod.web.compiler.spi.WebHeaderParameterInfo;
import io.inverno.mod.web.compiler.spi.WebInterceptorsConfigurerInfo;
import io.inverno.mod.web.compiler.spi.WebParameterInfo;
import io.inverno.mod.web.compiler.spi.WebPathParameterInfo;
import io.inverno.mod.web.compiler.spi.WebQueryParameterInfo;
import io.inverno.mod.web.compiler.spi.WebRequestBodyParameterInfo;
import io.inverno.mod.web.compiler.spi.WebRequestBodyParameterInfo.RequestBodyKind;
import io.inverno.mod.web.compiler.spi.WebResponseBodyInfo;
import io.inverno.mod.web.compiler.spi.WebRouteInfo;
import io.inverno.mod.web.compiler.spi.WebRouterConfigurerInfo;
import io.inverno.mod.web.compiler.spi.WebRoutesConfigurerInfo;
import io.inverno.mod.web.compiler.spi.WebServerControllerConfigurerInfo;
import io.inverno.mod.web.compiler.spi.WebServerControllerConfigurerInfoVisitor;
import io.inverno.mod.web.compiler.spi.WebSocketBoundPublisherInfo;
import io.inverno.mod.web.compiler.spi.WebSocketExchangeParameterInfo;
import io.inverno.mod.web.compiler.spi.WebSocketInboundParameterInfo;
import io.inverno.mod.web.compiler.spi.WebSocketInboundPublisherParameterInfo;
import io.inverno.mod.web.compiler.spi.WebSocketOutboundParameterInfo;
import io.inverno.mod.web.compiler.spi.WebSocketOutboundPublisherInfo;
import io.inverno.mod.web.compiler.spi.WebSocketParameterInfo;
import io.inverno.mod.web.compiler.spi.WebSocketRouteInfo;
import io.inverno.mod.web.compiler.spi.WebSseEventFactoryParameterInfo;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.type.TypeMirror;

/**
 * <p>
 * A {@link WebServerControllerConfigurerInfoVisitor} implementation used to generates an
 * Open API specification for the web controllers defined in an Inverno module.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class WebServerControllerConfigurerOpenApiGenerator implements WebServerControllerConfigurerInfoVisitor<StringBuilder, WebServerControllerConfigurerOpenApiGenerationContext> {
	
	private static final String OPENAPI_VERSION = "3.0.3";
	
	@Override
	public StringBuilder visit(WebServerControllerConfigurerInfo routerConfigurerInfo, WebServerControllerConfigurerOpenApiGenerationContext context) {
		if(context.getMode() == GenerationMode.ROUTER_SPEC) {
			StringBuilder result = new StringBuilder();
			
			result.append("openapi: ").append(WebServerControllerConfigurerOpenApiGenerator.OPENAPI_VERSION).append(System.lineSeparator());
			result.append("info:").append(System.lineSeparator());
			result.append(context.indent(1)).append("title: '").append(routerConfigurerInfo.getQualifiedName().getModuleQName().toString()).append("'").append(System.lineSeparator());
			
			WebServerControllerConfigurerOpenApiGenerationContext dctContext = context.withDocElement(routerConfigurerInfo.getElement());
			
			dctContext.withIndentDepthAdd(1).getDescription().ifPresent(description -> result.append(context.indent(1)).append("description: ").append(description).append(System.lineSeparator()));
			dctContext.withIndentDepthAdd(1).getContact().ifPresent(contact -> result.append(context.indent(1)).append("contact: ").append(System.lineSeparator()).append(contact).append(System.lineSeparator()));
			// TODO version is determined from @version annotation on the module, ideally we should use the version of the module provided at build time --module-version which has more value
			dctContext.withIndentDepthAdd(1).getVersion().ifPresentOrElse(version -> result.append(context.indent(1)).append("version: ").append(version).append(System.lineSeparator()), () -> result.append(context.indent(1)).append("version: ''").append(System.lineSeparator()));
			
			// TODO servers list MUST come from the environment, this has nothing to do in a spec
			// However it would be nice to provide an endpoint at runtime with these info (this is still stupid since we can be behind a router, with redirection and more...) 
//			result.append("servers: ").append(System.lineSeparator());
			
			result.append("tags: ").append(System.lineSeparator());
			result.append(Arrays.stream(routerConfigurerInfo.getControllers()).map(controller -> this.visit(controller, context.withIndentDepthAdd(1).withMode(GenerationMode.CONTROLLER_TAG))).collect(context.joining(System.lineSeparator()))).append(System.lineSeparator());

			result.append("paths: ").append(System.lineSeparator());
			// We should group by path and method, providing multiple response when applicable
			// - this happens when we specify routes with same path and different produces or language or consumes
			// - if/when we support routing based on query parameters we'll also have this problem
			// All this is kind of annoying, swagger showed once again that it is not in touch with reality
			// We'll need to merge everything we have
			// TODO Current implementation would make this quite difficult, we'll probably need to rely on a dedicated openAPI object model that would deal with such specificities
			// In the meantime, we'll try to live with this limitation
			
			Map<String, List<WebRouteInfo>> routesByPath = Arrays.stream(routerConfigurerInfo.getControllers())
				.flatMap(controller -> Arrays.stream(controller.getRoutes())
					.flatMap(route -> {
						Stream<String> routePathStream;
						if(route.getPaths().length > 0) {
							routePathStream = Arrays.stream(route.getPaths())
								.map(path -> route.getController()
									.map(WebControllerInfo::getRootPath)
									.map(rootPath -> URIs.uri(rootPath, URIs.RequestTargetForm.ABSOLUTE, URIs.Option.PARAMETERIZED, URIs.Option.NORMALIZED, URIs.Option.PATH_PATTERN).path(path, false).buildRawPath())
									.orElse(path)
								);
						}
						else {
							routePathStream = Stream.of(route.getController().map(WebControllerInfo::getRootPath).orElse("/"));
						}
						
						return routePathStream.map(path -> new Object[] {path, route});
					})
				)
				.collect(Collectors.groupingBy(pathAndRoute -> (String)pathAndRoute[0], Collectors.mapping(pathAndRoute -> (WebRouteInfo)pathAndRoute[1], Collectors.toList())));
			
			result.append(routesByPath.entrySet().stream()
				.sorted(Comparator.comparing(Map.Entry::getKey))
				.map(e -> new StringBuilder(context.indent(1)).append(e.getKey()).append(":").append(System.lineSeparator()).append(e.getValue().stream().map(route -> this.visit(route, context.withIndentDepthAdd(2).withMode(GenerationMode.ROUTE_PATH))).collect(context.joining(System.lineSeparator()))))
				.collect(context.joining(System.lineSeparator()))
			);
			
			context.withIndentDepthAdd(2).getComponentsSchemas()
				.ifPresent(componentsSchemas -> {
					result.append(System.lineSeparator()).append(context.indent(0)).append("components:").append(System.lineSeparator());
					result.append(context.indent(1)).append("schemas:").append(System.lineSeparator());
					result.append(componentsSchemas);
				});
			
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebInterceptorsConfigurerInfo interceptorsConfigurerInfo, WebServerControllerConfigurerOpenApiGenerationContext p) {
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebRoutesConfigurerInfo routesConfigurerInfo, WebServerControllerConfigurerOpenApiGenerationContext p) {
		// Does it really make sense to generate spec since we have neither access to request parameters or body nor to responses 
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebRouterConfigurerInfo providedRouterConfigurerInfo, WebServerControllerConfigurerOpenApiGenerationContext context) {
		// Does it really make sense to generate spec since we have neither access to request parameters or body nor to responses 
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(ErrorWebInterceptorsConfigurerInfo errorInterceptorsConfigurerInfo, WebServerControllerConfigurerOpenApiGenerationContext p) {
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(ErrorWebRoutesConfigurerInfo errorRoutesConfigurerInfo, WebServerControllerConfigurerOpenApiGenerationContext p) {
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(ErrorWebRouterConfigurerInfo errorRouterConfigurerInfo, WebServerControllerConfigurerOpenApiGenerationContext p) {
		return new StringBuilder();
	}
	
	@Override
	public StringBuilder visit(WebControllerInfo controllerInfo, WebServerControllerConfigurerOpenApiGenerationContext context) {
		if(context.getMode() == GenerationMode.CONTROLLER_TAG) {
			StringBuilder result = new StringBuilder();
			result.append(context.indentList(0)).append("name: '").append(controllerInfo.getQualifiedName().getSimpleValue()).append("'");
			
			WebServerControllerConfigurerOpenApiGenerationContext dctContext = context.withDocElement(controllerInfo.getElement());
			dctContext.getDescription().ifPresent(description -> result.append(System.lineSeparator()).append(context.indent(0)).append("description: ").append(description));

			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebRouteInfo routeInfo, WebServerControllerConfigurerOpenApiGenerationContext context) {
		if(routeInfo instanceof WebSocketRouteInfo) {
			return this.visit((WebSocketRouteInfo)routeInfo, context);
		}
		else {
			if(context.getMode() == GenerationMode.ROUTE_PATH) {
				// tags, operationId, summary, description, responses, parameters, requestBody
				StringBuilder operation = new StringBuilder();

				operation.append(context.indent(1)).append("tags: ");
				routeInfo.getController().ifPresent(controller -> operation.append(System.lineSeparator()).append(context.indentList(2)).append(controller.getQualifiedName().getSimpleValue()));

				WebServerControllerConfigurerOpenApiGenerationContext dctContext = routeInfo.getElement().map(context::withDocElement).orElse(context);
				dctContext.getSummary().ifPresent(summary -> operation.append(System.lineSeparator()).append(context.indent(1)).append("summary: ").append(summary));
				dctContext.getDescription().ifPresent(description -> operation.append(System.lineSeparator()).append(context.indent(1)).append("description: ").append(description));
				routeInfo.getElement().filter(element -> context.getElementUtils().isDeprecated(element)).ifPresent(element -> operation.append(System.lineSeparator()).append(context.indent(1)).append("deprecated: true"));

				StringBuilder parametersBuilder = Arrays.stream(routeInfo.getParameters())
					.map(parameter -> this.visit(parameter, dctContext.withIndentDepthAdd(2).withMode(GenerationMode.ROUTE_PARAMETER).withWebRoute(routeInfo)))
					.filter(parameterBuilder -> parameterBuilder.length() > 0)
					.collect(context.joining(System.lineSeparator()));
				if(parametersBuilder.length() > 0) {
					operation.append(System.lineSeparator()).append(context.indent(1)).append("parameters:");
					operation.append(System.lineSeparator()).append(parametersBuilder);
				}

				List<WebFormParameterInfo> formParameters = Arrays.stream(routeInfo.getParameters())
					.filter(parameter -> parameter instanceof WebFormParameterInfo)
					.map(parameter -> (WebFormParameterInfo)parameter)
					.collect(Collectors.toList());

				if(!formParameters.isEmpty()) {
					operation.append(System.lineSeparator()).append(context.indent(1)).append("requestBody: ").append(System.lineSeparator());
					operation.append(context.indent(2)).append("content: ").append(System.lineSeparator());
					operation.append(context.indent(3)).append(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED).append(":").append(System.lineSeparator());
					operation.append(context.indent(4)).append("schema:").append(System.lineSeparator());
					operation.append(context.indent(5)).append("type: object").append(System.lineSeparator());
					operation.append(context.indent(5)).append("properties:").append(System.lineSeparator());
					operation.append(formParameters.stream()
						.map(formParameter -> this.visit(formParameter, dctContext.withIndentDepthAdd(6).withMode(GenerationMode.ROUTE_BODY).withWebRoute(routeInfo)))
						.collect(context.joining(System.lineSeparator()))
					);
				}
				else {
					Arrays.stream(routeInfo.getParameters())
						.filter(parameter -> parameter instanceof WebRequestBodyParameterInfo)
						.findFirst()
						.map(parameter -> (WebRequestBodyParameterInfo)parameter)
						.ifPresent(requestBody -> {
							if(requestBody.getBodyKind() == RequestBodyKind.MULTIPART) {
								// NO idea regarding expected parts... so we can't list properties
								// In order to specify these we would have to do it in the javadoc because
								// we don't want to break reactivity we can't list the properties in the method
								// signature which must be of type Publisher<Part>

								operation.append(System.lineSeparator()).append(context.indent(1)).append("requestBody: ").append(System.lineSeparator());
								operation.append(context.indent(2)).append("content: ").append(System.lineSeparator());
								operation.append(context.indent(3)).append(MediaTypes.MULTIPART_FORM_DATA).append(":").append(System.lineSeparator());
								operation.append(context.indent(4)).append("schema:").append(System.lineSeparator());
								operation.append(context.indent(5)).append("type: object").append(System.lineSeparator());
							}
							else {
								// Regular request body 
								operation.append(System.lineSeparator()).append(context.indent(1)).append("requestBody:").append(System.lineSeparator());
								operation.append(this.visit(requestBody, dctContext.withIndentDepthAdd(2).withMode(GenerationMode.ROUTE_BODY).withWebRoute(routeInfo)));
							}
						});
				}

				operation.append(System.lineSeparator()).append(context.indent(1)).append("responses:");

				routeInfo.getElement()
					.map(routeElement -> dctContext.getResponses(routeElement, routeInfo.getResponseBody().getType()))
					.ifPresentOrElse(
						responses -> {
							operation.append(System.lineSeparator()).append(responses.stream()
								.collect(Collectors.groupingBy(ResponseSpec::getStatus)).entrySet().stream()
								.sorted(Comparator.comparing(Map.Entry::getKey))
								.map(statusEntry -> {
									StringBuilder responseBuilder = new StringBuilder();
									responseBuilder.append(context.indent(2)).append(statusEntry.getKey()).append(": ").append(System.lineSeparator());

									responseBuilder.append(context.indent(3)).append("description: '");
									responseBuilder.append(statusEntry.getValue().stream().map(ResponseSpec::getDescription)
										.map(description -> description.deleteCharAt(0).deleteCharAt(description.length() - 1))
										.filter(sb -> sb.length() > 0)
										.collect(context.joining("<br/>"))
									);
									responseBuilder.append("'");

									Collection<TypeMirror> responseTypes = statusEntry.getValue().stream().collect(Collectors.groupingBy(response -> response.getType().toString(), Collectors.collectingAndThen(Collectors.toList(), l -> l.get(0).getType()))).values();
									if(responseTypes.size() > 1) {
										responseBuilder.append(System.lineSeparator()).append(context.indent(3)).append("content:").append(System.lineSeparator());

										StringBuilder responseSchemaBuilder = new StringBuilder();
										responseSchemaBuilder.append(context.indent(5)).append("schema:").append(System.lineSeparator());
										responseSchemaBuilder.append(context.indent(6)).append("oneOf:").append(System.lineSeparator());
										responseSchemaBuilder.append(responseTypes.stream()
											.map(responseType -> context.withIndentDepthAdd(7).getSchema(responseType, true))
											.filter(Optional::isPresent)
											.map(Optional::get)
											.collect(context.joining(System.lineSeparator()))
										);

										if(routeInfo.getProduces().length > 0) {
											for(String produce : routeInfo.getProduces()) {
												responseBuilder.append(context.indent(4)).append(produce).append(":").append(System.lineSeparator());
												responseBuilder.append(responseSchemaBuilder);
											}
										}
										else {
											responseBuilder.append(context.indent(4)).append("'*/*'").append(":").append(System.lineSeparator());
											responseBuilder.append(responseSchemaBuilder);
										}
									}
									else if(responseTypes.size() > 0) {
										// We know there is only one element here						
										TypeMirror responseType = responseTypes.iterator().next();
										Optional<StringBuilder> responseSchema = context.withIndentDepthAdd(6).getSchema(responseType, false).map(schema -> schema.insert(0, new StringBuilder(context.indent(5)).append("schema:").append(System.lineSeparator())));
										if(responseSchema.isPresent()) {
											responseBuilder.append(System.lineSeparator()).append(context.indent(3)).append("content:").append(System.lineSeparator());
											if(routeInfo.getProduces().length > 0) {
												for(String produce : routeInfo.getProduces()) {
													responseBuilder.append(context.indent(4)).append(produce).append(":").append(System.lineSeparator());
													responseBuilder.append(responseSchema.get());
												}
											}
											else {
												responseBuilder.append(context.indent(4)).append("'*/*'").append(":").append(System.lineSeparator());
												responseBuilder.append(responseSchema.get());
											}
										}
									}
									return responseBuilder;
								})
								.collect(context.joining(System.lineSeparator()))
							);
						},
						() -> {
							// the route element is empty when we process a provided route coming from a
							// provided web router.
							// since we don't process these when generating a spec this should never happen.
							operation.append(System.lineSeparator()).append(context.indent(3)).append("default: ");
							operation.append(System.lineSeparator()).append(context.indent(4)).append("description: ''");
						}
					);

				return Arrays.stream(routeInfo.getMethods())
					.map(method -> {
						StringBuilder methodOperation = new StringBuilder();
						methodOperation.append(context.indent(0)).append(method.toString().toLowerCase()).append(":").append(System.lineSeparator());
						methodOperation.append(context.indent(1)).append("operationId: '").append(method.toString().toLowerCase()).append("_").append(routeInfo.getQualifiedName().getControllerQName().getSimpleValue()).append("_").append(routeInfo.getQualifiedName().getSimpleValue()).append("'").append(System.lineSeparator());
						methodOperation.append(operation);
						return methodOperation;
					})
					.collect(context.joining(System.lineSeparator()));
			}
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebResponseBodyInfo responseBodyInfo, WebServerControllerConfigurerOpenApiGenerationContext context) {
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebParameterInfo parameterInfo, WebServerControllerConfigurerOpenApiGenerationContext context) {
		if(parameterInfo instanceof WebFormParameterInfo) {
			return this.visit((WebFormParameterInfo)parameterInfo, context);
		}
		else if(parameterInfo instanceof WebBasicParameterInfo) {
			return this.visit((WebBasicParameterInfo)parameterInfo, context);
		}
		else if(parameterInfo instanceof WebExchangeParameterInfo) {
			return this.visit((WebExchangeParameterInfo)parameterInfo, context);
		}
		else if(parameterInfo instanceof WebRequestBodyParameterInfo) {
			return this.visit((WebRequestBodyParameterInfo)parameterInfo, context);
		}
		else if(parameterInfo instanceof WebSseEventFactoryParameterInfo) {
			return this.visit((WebSseEventFactoryParameterInfo)parameterInfo, context);
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebBasicParameterInfo basicParameterInfo, WebServerControllerConfigurerOpenApiGenerationContext context) {
		if(context.getMode() == GenerationMode.ROUTE_PARAMETER) {
			StringBuilder result = new StringBuilder();
			
			String parameterName = basicParameterInfo.getQualifiedName().getParameterName();
			result.append(context.indentList(0)).append("name: ").append(parameterName).append(System.lineSeparator());
			
			context.getParameterDescription(parameterName).ifPresent(description -> result.append(context.indent(0)).append("description: ").append(description).append(System.lineSeparator()));
			
			result.append(context.indent(0)).append("in: ");
			if(basicParameterInfo instanceof WebCookieParameterInfo) {
				result.append("cookie").append(System.lineSeparator());
			}
			else if(basicParameterInfo instanceof WebHeaderParameterInfo) {
				result.append("header").append(System.lineSeparator());
			}
			else if(basicParameterInfo instanceof WebPathParameterInfo) {
				result.append("path").append(System.lineSeparator());
			}
			else if(basicParameterInfo instanceof WebQueryParameterInfo) {
				result.append("query").append(System.lineSeparator());
			}
			else {
				throw new IllegalStateException("Unknown basic parameter type: " + basicParameterInfo.getClass());
			}
			result.append(context.indent(0)).append("required: ").append(basicParameterInfo.isRequired()).append(System.lineSeparator());
			context.withIndentDepthAdd(1).getSchema(basicParameterInfo.getType(), false).ifPresentOrElse(
				schema -> result.append(context.indent(0)).append("schema: ").append(System.lineSeparator()).append(schema),
				() -> {
					result.append(context.indent(0)).append("schema: ").append(System.lineSeparator());
					result.append(context.indent(1)).append("type: object");
				}
			);
			
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebCookieParameterInfo cookieParameterInfo, WebServerControllerConfigurerOpenApiGenerationContext context) {
		return this.visit((WebBasicParameterInfo)cookieParameterInfo, context);
	}

	@Override
	public StringBuilder visit(WebFormParameterInfo formParameterInfo, WebServerControllerConfigurerOpenApiGenerationContext context) {
		if(context.getMode() == GenerationMode.ROUTE_BODY) {
			StringBuilder result = new StringBuilder();
			
			result.append(context.indent(0)).append(formParameterInfo.getQualifiedName().getParameterName()).append(":").append(System.lineSeparator());
			context.withIndentDepthAdd(1).getSchema(formParameterInfo.getType(), false).ifPresentOrElse(
				schema -> result.append(schema),
				() -> result.append(context.indent(1)).append("type: object")
			);
			
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebHeaderParameterInfo headerParameterInfo, WebServerControllerConfigurerOpenApiGenerationContext context) {
		return this.visit((WebBasicParameterInfo)headerParameterInfo, context);
	}

	@Override
	public StringBuilder visit(WebPathParameterInfo pathParameterInfo, WebServerControllerConfigurerOpenApiGenerationContext context) {
		return this.visit((WebBasicParameterInfo)pathParameterInfo, context);
	}

	@Override
	public StringBuilder visit(WebQueryParameterInfo queryParameterInfo, WebServerControllerConfigurerOpenApiGenerationContext context) {
		return this.visit((WebBasicParameterInfo)queryParameterInfo, context);
	}

	@Override
	public StringBuilder visit(WebRequestBodyParameterInfo bodyParameterInfo, WebServerControllerConfigurerOpenApiGenerationContext context) {
		if(context.getMode() == GenerationMode.ROUTE_BODY) {
			StringBuilder result = new StringBuilder();
			WebRouteInfo routeInfo = context.getWebRoute();
			String parameterName = bodyParameterInfo.getQualifiedName().getParameterName();

			context.getParameterDescription(parameterName).ifPresent(description -> result.append(context.indent(0)).append("description: ").append(description).append(System.lineSeparator()));
			
			StringBuilder responseSchema = context.withIndentDepthAdd(3).getSchema(bodyParameterInfo.getType(), false)
				.map(schema -> new StringBuilder(context.indent(2)).append("schema:").append(System.lineSeparator()).append(schema))
				.orElseGet(() -> {
					StringBuilder fallbackSchema = new StringBuilder(context.indent(2)).append("schema:").append(System.lineSeparator());
					fallbackSchema.append(context.indent(3)).append("type: object");
					return fallbackSchema;
				});
			
			result.append(context.indent(0)).append("content:").append(System.lineSeparator());
			
			if(routeInfo.getConsumes().length > 0) {
				for(String produce : routeInfo.getConsumes()) {
					result.append(context.indent(1)).append(produce).append(":").append(System.lineSeparator());
					result.append(responseSchema);
				}
			}
			else {
				result.append(context.indent(1)).append("'*/*'").append(":").append(System.lineSeparator());
				result.append(responseSchema);
			}
			
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebExchangeParameterInfo exchangeParameterInfo, WebServerControllerConfigurerOpenApiGenerationContext context) {
		return new StringBuilder();
	}
	
	@Override
	public StringBuilder visit(WebExchangeContextParameterInfo exchangeContextParameterInfo, WebServerControllerConfigurerOpenApiGenerationContext p) {
		return new StringBuilder();
	}
	
	@Override
	public StringBuilder visit(WebSseEventFactoryParameterInfo sseEventFactoryParameterInfo, WebServerControllerConfigurerOpenApiGenerationContext context) {
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebSocketRouteInfo webSocketRouteInfo, WebServerControllerConfigurerOpenApiGenerationContext p) {
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebSocketBoundPublisherInfo boundPublisherInfo, WebServerControllerConfigurerOpenApiGenerationContext p) {
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebSocketOutboundPublisherInfo outboundPublisherInfo, WebServerControllerConfigurerOpenApiGenerationContext p) {
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebSocketParameterInfo parameterInfo, WebServerControllerConfigurerOpenApiGenerationContext p) {
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebSocketOutboundParameterInfo outboundParameterInfo, WebServerControllerConfigurerOpenApiGenerationContext p) {
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebSocketInboundPublisherParameterInfo inboundPublisherParameterInfo, WebServerControllerConfigurerOpenApiGenerationContext p) {
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebSocketInboundParameterInfo inboundParameterInfo, WebServerControllerConfigurerOpenApiGenerationContext p) {
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebSocketExchangeParameterInfo exchangeParameterInfo, WebServerControllerConfigurerOpenApiGenerationContext p) {
		return new StringBuilder();
	}
}
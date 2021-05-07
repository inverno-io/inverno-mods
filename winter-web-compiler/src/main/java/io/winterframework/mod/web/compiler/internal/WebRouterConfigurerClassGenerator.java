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
 * See the License for the specific language governing contextermissions and
 * limitations under the License.
 */
package io.winterframework.mod.web.compiler.internal;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import io.winterframework.core.annotation.Bean;
import io.winterframework.mod.base.net.URIs;
import io.winterframework.mod.web.WebExchange;
import io.winterframework.mod.web.WebRouter;
import io.winterframework.mod.web.WebRouterConfigurer;
import io.winterframework.mod.web.annotation.WebRoutes;
import io.winterframework.mod.web.compiler.internal.WebRouterConfigurerClassGenerationContext.GenerationMode;
import io.winterframework.mod.web.compiler.spi.WebBasicParameterInfo;
import io.winterframework.mod.web.compiler.spi.WebControllerInfo;
import io.winterframework.mod.web.compiler.spi.WebCookieParameterInfo;
import io.winterframework.mod.web.compiler.spi.WebExchangeParameterInfo;
import io.winterframework.mod.web.compiler.spi.WebFormParameterInfo;
import io.winterframework.mod.web.compiler.spi.WebHeaderParameterInfo;
import io.winterframework.mod.web.compiler.spi.WebParameterInfo;
import io.winterframework.mod.web.compiler.spi.WebPathParameterInfo;
import io.winterframework.mod.web.compiler.spi.WebProvidedRouterConfigurerInfo;
import io.winterframework.mod.web.compiler.spi.WebQueryParameterInfo;
import io.winterframework.mod.web.compiler.spi.WebRequestBodyParameterInfo;
import io.winterframework.mod.web.compiler.spi.WebRequestBodyParameterInfo.RequestBodyKind;
import io.winterframework.mod.web.compiler.spi.WebRequestBodyParameterInfo.RequestBodyReactiveKind;
import io.winterframework.mod.web.compiler.spi.WebResponseBodyInfo;
import io.winterframework.mod.web.compiler.spi.WebResponseBodyInfo.ResponseBodyKind;
import io.winterframework.mod.web.compiler.spi.WebResponseBodyInfo.ResponseBodyReactiveKind;
import io.winterframework.mod.web.compiler.spi.WebRouteInfo;
import io.winterframework.mod.web.compiler.spi.WebRouterConfigurerInfo;
import io.winterframework.mod.web.compiler.spi.WebRouterConfigurerInfoVisitor;
import io.winterframework.mod.web.compiler.spi.WebSseEventFactoryParameterInfo;

/**
 * <p>
 * A {@link WebRouterConfigurerInfoVisitor} implementation used to generates a
 * web router configurer class in a Winter module.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class WebRouterConfigurerClassGenerator implements WebRouterConfigurerInfoVisitor<StringBuilder, WebRouterConfigurerClassGenerationContext> {

	@Override
	public StringBuilder visit(WebRouterConfigurerInfo routerConfigurerInfo, WebRouterConfigurerClassGenerationContext context) {
		String configurerClassName = routerConfigurerInfo.getQualifiedName().getClassName();
		String configurerPackageName = configurerClassName.lastIndexOf(".") != -1 ? configurerClassName.substring(0, configurerClassName.lastIndexOf(".")) : "";
		configurerClassName = configurerClassName.substring(configurerPackageName.length() + 1);
		if(context.getMode() == GenerationMode.CONFIGURER_CLASS) {
			TypeMirror beanAnnotationType = context.getElementUtils().getTypeElement(Bean.class.getCanonicalName()).asType();

			TypeMirror webExchangeType = context.getElementUtils().getTypeElement(WebExchange.class.getCanonicalName()).asType();
			TypeMirror webRouterConfigurerType = context.getTypeUtils().getDeclaredType(context.getElementUtils().getTypeElement(WebRouterConfigurer.class.getCanonicalName()), webExchangeType);
			TypeMirror routerType = context.getTypeUtils().getDeclaredType(context.getElementUtils().getTypeElement(WebRouter.class.getCanonicalName()), webExchangeType);
			
			context.addImport(configurerClassName, configurerPackageName + "." + configurerClassName);
			
			StringBuilder configurerAnnotation = this.visit(routerConfigurerInfo, context.withIndentDepth(1).withMode(GenerationMode.CONFIGURER_ANNOTATION));
			
			StringBuilder configurer_controller_fields = Arrays.stream(routerConfigurerInfo.getControllers())
				.map(controllerInfo -> this.visit(controllerInfo, context.withIndentDepth(1).withMode(GenerationMode.CONTROLLER_FIELD)))
				.collect(context.joining("\n"));
				
			StringBuilder configurer_router_fields = Arrays.stream(routerConfigurerInfo.getRouters())
				.map(routerInfo -> this.visit(routerInfo, context.withIndentDepth(1).withMode(GenerationMode.CONFIGURER_FIELD)))
				.collect(context.joining("\n"));
		
			StringBuilder configurer_constructor = new StringBuilder(context.indent(1)).append("public ").append(configurerClassName).append("(");
			configurer_constructor.append(Stream.concat(
					Arrays.stream(routerConfigurerInfo.getControllers()).map(controllerInfo -> this.visit(controllerInfo, context.withIndentDepth(0).withMode(GenerationMode.CONTROLLER_PARAMETER))),
					Arrays.stream(routerConfigurerInfo.getRouters()).map(routerInfo -> this.visit(routerInfo, context.withIndentDepth(0).withMode(GenerationMode.CONFIGURER_PARAMETER)))
				)
				.collect(context.joining(", "))
			);
			configurer_constructor.append(") {\n");
			configurer_constructor.append(Stream.concat(
					Arrays.stream(routerConfigurerInfo.getControllers()).map(controllerInfo -> this.visit(controllerInfo, context.withIndentDepth(2).withMode(GenerationMode.CONTROLLER_ASSIGNMENT))),
					Arrays.stream(routerConfigurerInfo.getRouters()).map(routerInfo -> this.visit(routerInfo, context.withIndentDepth(2).withMode(GenerationMode.CONFIGURER_ASSIGNMENT)))
				)
				.collect(context.joining("\n"))
			);
			configurer_constructor.append("\n").append(context.indent(1)).append("}");
					
			StringBuilder configurer_accept = new StringBuilder(context.indent(1)).append("@Override\n");
			configurer_accept.append(context.indent(1)).append("public void accept(").append(context.getTypeName(routerType)).append(" router) {");
			if(routerConfigurerInfo.getRouters().length > 0) {
				configurer_accept.append("\n").append(Arrays.stream(routerConfigurerInfo.getRouters()).map(routerInfo -> this.visit(routerInfo, context.withIndentDepth(2).withMode(GenerationMode.CONFIGURER_INVOKE))).collect(context.joining("\n"))).append("\n");
			}
			if(routerConfigurerInfo.getControllers().length > 0) {
				configurer_accept.append("\n").append(context.indent(2)).append("router\n");
				configurer_accept.append(Arrays.stream(routerConfigurerInfo.getControllers()).map(controllerInfo -> this.visit(controllerInfo, context.withIndentDepth(3).withMode(GenerationMode.ROUTE_DECLARATION))).collect(context.joining("\n")));
				configurer_accept.append(";\n");
			}
			
			configurer_accept.append(context.indent(1)).append("}");
			
			StringBuilder configurer_class = new StringBuilder();
			
			configurer_class.append(configurerAnnotation).append("\n");
			configurer_class.append("@").append(context.getTypeName(beanAnnotationType)).append("\n");
			configurer_class.append("public final class ").append(configurerClassName).append(" implements ").append(context.getTypeName(webRouterConfigurerType)).append(" {\n\n");
			
			if(routerConfigurerInfo.getControllers().length > 0) {
				configurer_class.append(configurer_controller_fields).append("\n\n");
			}
			if(routerConfigurerInfo.getRouters().length > 0) {
				configurer_class.append(configurer_router_fields).append("\n\n");
			}
			configurer_class.append(configurer_constructor).append("\n\n");
			configurer_class.append(configurer_accept).append("\n");
			
			configurer_class.append("}");
			
			context.removeImport(configurerClassName);
			
			configurer_class.insert(0, "\n\n").insert(0, context.getImports().stream().sorted().filter(i -> i.lastIndexOf(".") > 0 && !i.substring(0, i.lastIndexOf(".")).equals(configurerPackageName)).map(i -> new StringBuilder().append("import ").append(i).append(";")).collect(context.joining("\n")));
			if(!configurerPackageName.equals("")) {
				configurer_class.insert(0, ";\n\n").insert(0, configurerPackageName).insert(0, "package ");
			}
			
			return configurer_class;
		}
		if(context.getMode() == GenerationMode.CONFIGURER_ANNOTATION) {
			TypeMirror webRoutesAnnotationType = context.getElementUtils().getTypeElement(WebRoutes.class.getCanonicalName()).asType();
			StringBuilder result = new StringBuilder();
			result.append("@").append(context.getTypeName(webRoutesAnnotationType)).append("({\n");
			result.append(Stream.concat(
					Arrays.stream(routerConfigurerInfo.getRouters()).map(routerInfo -> this.visit(routerInfo, context)),
					Arrays.stream(routerConfigurerInfo.getControllers())
						.flatMap(controllerInfo -> Arrays.stream(controllerInfo.getRoutes()).map(routeInfo -> this.visit(routeInfo, context.withWebController(controllerInfo).withMode(GenerationMode.ROUTE_ANNOTATION))))
				)
				.collect(context.joining(",\n"))
			);
			result.append("\n})");
			return result;
		}
		return new StringBuilder();
	}
	
	@Override
	public StringBuilder visit(WebProvidedRouterConfigurerInfo providedRouterConfigurerInfo, WebRouterConfigurerClassGenerationContext context) {
		if(context.getMode() == GenerationMode.CONFIGURER_ANNOTATION) {
			return Arrays.stream(providedRouterConfigurerInfo.getRoutes())
				.map(routeInfo -> this.visit(routeInfo, context.withMode(GenerationMode.ROUTE_ANNOTATION)))
				.collect(context.joining(",\n"));
		}
		else if(context.getMode() == GenerationMode.CONFIGURER_FIELD) {
			return new StringBuilder(context.indent(0)).append("private ").append(context.getTypeName(providedRouterConfigurerInfo.getType())).append(" ").append(context.getFieldName(providedRouterConfigurerInfo.getQualifiedName())).append(";");
		}
		else if(context.getMode() == GenerationMode.CONFIGURER_PARAMETER) {
			return new StringBuilder(context.indent(0)).append(context.getTypeName(providedRouterConfigurerInfo.getType())).append(" ").append(context.getFieldName(providedRouterConfigurerInfo.getQualifiedName()));
		}
		else if(context.getMode() == GenerationMode.CONFIGURER_ASSIGNMENT) {
			return new StringBuilder(context.indent(0)).append("this.").append(context.getFieldName(providedRouterConfigurerInfo.getQualifiedName())).append(" = ").append(context.getFieldName(providedRouterConfigurerInfo.getQualifiedName())).append(";");
		}
		else if(context.getMode() == GenerationMode.CONFIGURER_INVOKE) {
			return new StringBuilder(context.indent(0)).append("this.").append(context.getFieldName(providedRouterConfigurerInfo.getQualifiedName())).append(".accept(router);");
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebControllerInfo controllerInfo, WebRouterConfigurerClassGenerationContext context) {
		if(context.getMode() == GenerationMode.CONTROLLER_FIELD) {
			return new StringBuilder(context.indent(0)).append("private ").append(context.getTypeName(controllerInfo.getType())).append(" ").append(context.getFieldName(controllerInfo.getQualifiedName())).append(";");
		}
		else if(context.getMode() == GenerationMode.CONTROLLER_PARAMETER) {
			return new StringBuilder(context.indent(0)).append(context.getTypeName(controllerInfo.getType())).append(" ").append(context.getFieldName(controllerInfo.getQualifiedName()));
		}
		else if(context.getMode() == GenerationMode.CONTROLLER_ASSIGNMENT) {
			return new StringBuilder(context.indent(0)).append("this.").append(context.getFieldName(controllerInfo.getQualifiedName())).append(" = ").append(context.getFieldName(controllerInfo.getQualifiedName())).append(";");
		}
		else if(context.getMode() == GenerationMode.ROUTE_DECLARATION) {
			return Arrays.stream(controllerInfo.getRoutes()).map(routeInfo -> this.visit(routeInfo, context.withWebController(controllerInfo))).collect(context.joining("\n"));
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebRouteInfo routeInfo, WebRouterConfigurerClassGenerationContext context) {
		if(context.getMode() == GenerationMode.ROUTE_ANNOTATION) {
			StringBuilder result = new StringBuilder();
			result.append(context.indent(0)).append("@").append(context.getWebRouteAnnotationTypeName()).append("(");
			
			result.append("path = { ");
			if(routeInfo.getPaths().length > 0) {
				result.append(Arrays.stream(routeInfo.getPaths())
					.map(path -> "\"" + routeInfo.getController()
						.map(WebControllerInfo::getRootPath)
						.map(rootPath -> URIs.uri(rootPath, URIs.Option.PARAMETERIZED, URIs.Option.NORMALIZED).path(path, false).buildRawPath())
						.orElse(path) + "\""
					)
					.collect(Collectors.joining(", "))
				);	
			}
			else {
				routeInfo.getController()
					.map(WebControllerInfo::getRootPath)
					.map(rootPath -> URIs.uri(rootPath, URIs.Option.PARAMETERIZED, URIs.Option.NORMALIZED).buildRawPath())
					.ifPresent(rootPath -> result.append("\"").append(rootPath).append("\""));
			}
			result.append(" }");
			
			if(routeInfo.isMatchTrailingSlash()) {
				result.append(", matchTrailingSlash = true");
			}
			if(routeInfo.getMethods() != null && routeInfo.getMethods().length > 0) {
				result.append(", method = { ").append(Arrays.stream(routeInfo.getMethods()).map(method -> context.getMethodTypeName() + "." + method.toString()).collect(Collectors.joining(", "))).append(" }");
			}
			if(routeInfo.getConsumes() != null && routeInfo.getConsumes().length > 0) {
				result.append(", consumes = { ").append(Arrays.stream(routeInfo.getConsumes()).map(consumes -> "\"" + consumes + "\"").collect(Collectors.joining(", "))).append(" }");
			}
			if(routeInfo.getProduces() != null && routeInfo.getProduces().length > 0) {
				result.append(", produces = { ").append(Arrays.stream(routeInfo.getProduces()).map(produces -> "\"" + produces + "\"").collect(Collectors.joining(", "))).append(" }");
			}
			if(routeInfo.getLanguages() != null && routeInfo.getLanguages().length > 0) {
				result.append(", language = { ").append(Arrays.stream(routeInfo.getLanguages()).map(language -> "\"" + language + "\"").collect(Collectors.joining(", "))).append(" }");
			}
			result.append(")");
			return result;
		}
		else if(context.getMode() == GenerationMode.ROUTE_DECLARATION) {
			boolean typesMode = context.isTypeMode(routeInfo);
			GenerationMode handlerBodyMode = typesMode ? GenerationMode.ROUTE_HANDLER_BODY_TYPE : GenerationMode.ROUTE_HANDLER_BODY_CLASS;
			
			StringBuilder routeHandler = new StringBuilder("exchange -> {\n");
			routeHandler.append(this.visit(routeInfo.getResponseBody(), context.withIndentDepth(context.getIndentDepth() + (typesMode ? 2 : 1) ).withWebRoute(routeInfo).withMode(handlerBodyMode)));
			routeHandler.append(context.indent(typesMode ? 1 : 0)).append("}");
			
			StringBuilder routeManager = new StringBuilder();
			
			if(routeInfo.getPaths().length > 0) {
				routeManager.append(Arrays.stream(routeInfo.getPaths())
					.map(path -> ".path(\"" + routeInfo.getController()
						.map(WebControllerInfo::getRootPath)
						.map(rootPath -> URIs.uri(rootPath, URIs.Option.PARAMETERIZED, URIs.Option.NORMALIZED).path(path, false).buildRawPath())
						.orElse(path) + "\", " + routeInfo.isMatchTrailingSlash() + ")"
					)
					.collect(Collectors.joining())
				);
			}
			else {
				routeInfo.getController()
					.map(WebControllerInfo::getRootPath)
					.map(rootPath -> URIs.uri(rootPath, URIs.Option.PARAMETERIZED, URIs.Option.NORMALIZED).buildRawPath())
					.ifPresent(rootPath -> routeManager.append(".path(\"").append(rootPath).append("\", ").append(routeInfo.isMatchTrailingSlash()).append(")"));
			}
			if(routeInfo.getMethods() != null && routeInfo.getMethods().length > 0) {
				routeManager.append(Arrays.stream(routeInfo.getMethods()).map(method -> ".method(" + context.getMethodTypeName() + "." + method.toString() + ")").collect(Collectors.joining()));
			}
			if(routeInfo.getConsumes() != null && routeInfo.getConsumes().length > 0) {
				routeManager.append(Arrays.stream(routeInfo.getConsumes()).map(consumes -> ".consumes(\"" + consumes + "\")").collect(Collectors.joining()));
			}
			if(routeInfo.getProduces() != null && routeInfo.getProduces().length > 0) {
				routeManager.append(Arrays.stream(routeInfo.getProduces()).map(produces -> ".produces(\"" + produces + "\")").collect(Collectors.joining()));
			}
			if(routeInfo.getLanguages() != null && routeInfo.getLanguages().length > 0) {
				routeManager.append(Arrays.stream(routeInfo.getLanguages()).map(language -> ".language(\"" + language + "\")").collect(Collectors.joining()));
			}
			
			routeManager.append(".handler(").append(routeHandler).append(")");
			
			if(typesMode) {
				StringBuilder routeTypes = new StringBuilder(context.getTypeTypeName()).append("[] routeTypes = new ").append(context.getTypeTypeName()).append("[] {\n");
				routeTypes.append(Stream.concat(
						Arrays.stream(routeInfo.getParameters())
							.map(parameterInfo -> {
								if(parameterInfo instanceof WebRequestBodyParameterInfo) {
									return parameterInfo.getType();
								}
								else {
									return context.getParameterConverterType(parameterInfo.getType());
								}
							}),
						routeInfo.getResponseBody().getBodyKind() != ResponseBodyKind.EMPTY ? Stream.of(routeInfo.getResponseBody().getType()) : Stream.of()
					)
					.map(converterType -> new StringBuilder(context.indent(2)).append(context.getTypeGenerator(converterType)))
					.collect(context.joining(",\n"))
				);
				
				routeTypes.append("\n").append(context.indent(1)).append("}");
				
				StringBuilder result = new StringBuilder(context.indent(0)).append(".route(route -> {\n");
				result.append(context.indent(1)).append(routeTypes).append(";\n");
				result.append(context.indent(1)).append("route").append(routeManager).append(";\n");
				result.append(context.indent(0)).append("})");
				
				return result;
			}
			else {
				return new StringBuilder(context.indent(0)).append(".route()").append(routeManager);
			}
		}
		return new StringBuilder();
	}
	
	@Override
	public StringBuilder visit(WebResponseBodyInfo responseBodyInfo, WebRouterConfigurerClassGenerationContext context) {
		if(context.getMode() == GenerationMode.ROUTE_HANDLER_BODY_CLASS || context.getMode() == GenerationMode.ROUTE_HANDLER_BODY_TYPE) {
			boolean typesMode = context.getMode() == GenerationMode.ROUTE_HANDLER_BODY_TYPE;
			
			StringBuilder result = new StringBuilder();
			WebRouteInfo routeInfo = context.getWebRoute();
			
			StringBuilder requestParameters = new StringBuilder();
			GenerationMode parameterReferenceMode = typesMode ? GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE : GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS;
			
			Integer nonReactiveRequestBodyParameterIndex = null; 
			boolean hasFormParameters = false;
			WebRequestBodyParameterInfo requestBodyInfo = null;
			int parameterIndex = 0;
			for(Iterator<WebParameterInfo> parameterInfoIterator = Arrays.stream(routeInfo.getParameters()).iterator();parameterInfoIterator.hasNext();) {
				WebParameterInfo parameterInfo = parameterInfoIterator.next();
				if(parameterInfo instanceof WebRequestBodyParameterInfo && ((WebRequestBodyParameterInfo)parameterInfo).getBodyReactiveKind() == RequestBodyReactiveKind.NONE) {
					nonReactiveRequestBodyParameterIndex = parameterIndex;
					requestParameters.append("body");
				}
				else {
					requestParameters.append(this.visit(parameterInfo, context.withIndentDepth(0).withMode(parameterReferenceMode).withParameterIndex(parameterIndex)));
				}
				if(parameterInfo instanceof WebFormParameterInfo) {
					hasFormParameters = true;
				}
				if(parameterInfo instanceof WebRequestBodyParameterInfo) {
					requestBodyInfo = (WebRequestBodyParameterInfo)parameterInfo;
				}
				if(parameterInfoIterator.hasNext()) {
					requestParameters.append(", ");
				}
				parameterIndex++;
			}
			
			StringBuilder controllerInvoke = new StringBuilder("this.").append(context.getFieldName(context.getWebController().getQualifiedName())).append(".").append(routeInfo.getElement().get().getSimpleName().toString()).append("(").append(requestParameters).append(")");
			
			if(responseBodyInfo.getBodyKind() == ResponseBodyKind.EMPTY && responseBodyInfo.getBodyReactiveKind() == ResponseBodyReactiveKind.NONE) {
				if(hasFormParameters) {
					result.append(context.indent(0)).append("exchange.response().body().raw().stream(");
					result.append(context.getFluxTypeName()).append(".from(exchange.request().body().get().urlEncoded().stream()).collectMultimap(").append(context.getParameterTypeName()).append("::getName)").append(".flatMap(formParameters -> ");
					result.append("{ ").append(controllerInvoke).append("; return ").append(context.getMonoTypeName()).append(".empty(); }");
					result.append("));\n");
				}
				else if(nonReactiveRequestBodyParameterIndex != null) {
					result.append(context.indent(0)).append("exchange.response().body().raw().stream(");
					result.append(this.visit(routeInfo.getParameters()[nonReactiveRequestBodyParameterIndex], context.withIndentDepth(0).withMode(parameterReferenceMode).withParameterIndex(nonReactiveRequestBodyParameterIndex))).append(".flatMap(body -> ");
					result.append("{ ").append(controllerInvoke).append("; return ").append(context.getMonoTypeName()).append(".empty(); }");
					result.append("));\n");
				}
				else {
					result.append(context.indent(0)).append(controllerInvoke).append(";\n");
					result.append(context.indent(0)).append("exchange.response().body().empty();\n");
				}
			}
			else {
				if(nonReactiveRequestBodyParameterIndex != null) {
					if(responseBodyInfo.getBodyReactiveKind() == ResponseBodyReactiveKind.NONE) {
						controllerInvoke.insert(0, this.visit(routeInfo.getParameters()[nonReactiveRequestBodyParameterIndex], context.withIndentDepth(0).withMode(parameterReferenceMode).withParameterIndex(nonReactiveRequestBodyParameterIndex)).append(".map(body -> ")).append(")");
					}
					else if(responseBodyInfo.getBodyReactiveKind() == ResponseBodyReactiveKind.ONE) {
						controllerInvoke.insert(0, this.visit(routeInfo.getParameters()[nonReactiveRequestBodyParameterIndex], context.withIndentDepth(0).withMode(parameterReferenceMode).withParameterIndex(nonReactiveRequestBodyParameterIndex)).append(".flatMap(body -> ")).append(")");
					}
					else if(responseBodyInfo.getBodyReactiveKind() == ResponseBodyReactiveKind.PUBLISHER || responseBodyInfo.getBodyReactiveKind() == ResponseBodyReactiveKind.MANY) {
						controllerInvoke.insert(0, this.visit(routeInfo.getParameters()[nonReactiveRequestBodyParameterIndex], context.withIndentDepth(0).withMode(parameterReferenceMode).withParameterIndex(nonReactiveRequestBodyParameterIndex)).append(".flatMapMany(body -> ")).append(")");
					}
					else {
						throw new IllegalStateException("Unknown response body reactive kind: " + responseBodyInfo.getBodyReactiveKind());
					}
				}
				else if(hasFormParameters) {
					controllerInvoke.insert(0, new StringBuilder(context.getFluxTypeName()).append(".from(exchange.request().body().get().urlEncoded().stream()).collectMultimap(").append(context.getParameterTypeName()).append("::getName)").append(".map(formParameters -> ")).append(")");
				}
				
				if(responseBodyInfo.getBodyKind() == ResponseBodyKind.EMPTY) {
					// We know we are reactive here
					if(responseBodyInfo.getBodyReactiveKind() == ResponseBodyReactiveKind.PUBLISHER) {
						controllerInvoke.insert(0, new StringBuilder(context.getFluxTypeName()).append(".from(")).append(")");
					}
					result.append(context.indent(0)).append("exchange.response().body().raw().stream(");
					result.append(controllerInvoke).append(".then().cast(").append(context.getByteBufTypeName()).append(".class)");
					result.append(");\n");
				}
				else if(responseBodyInfo.getBodyKind() == ResponseBodyKind.RAW) {
					String responseBodyDataMethod;
					if(responseBodyInfo.getBodyReactiveKind() == ResponseBodyReactiveKind.NONE) {
						if(hasFormParameters || requestBodyInfo != null) {
							responseBodyDataMethod = "stream";
						}
						else {
							responseBodyDataMethod = "value";
						}
					}
					else {
						responseBodyDataMethod = "stream";
					}
					result.append(context.indent(0)).append("exchange.response().body().raw().").append(responseBodyDataMethod).append("(");
					result.append(controllerInvoke);
					result.append(");\n");
				}
				else if(responseBodyInfo.getBodyKind() == ResponseBodyKind.CHARSEQUENCE) {
					String responseBodyDataMethod;
					if(responseBodyInfo.getBodyReactiveKind() == ResponseBodyReactiveKind.NONE) {
						if(hasFormParameters || requestBodyInfo != null) {
							responseBodyDataMethod = "stream";
						}
						else {
							responseBodyDataMethod = "value";
						}
					}
					else {
						responseBodyDataMethod = "stream";
					}
					result.append(context.indent(0)).append("exchange.response().body().string().").append(responseBodyDataMethod).append("(");
					result.append(controllerInvoke);
					result.append(");\n");
				}
				else if(responseBodyInfo.getBodyKind() == ResponseBodyKind.ENCODED) {
					String responseBodyDataMethod;
					if(responseBodyInfo.getBodyReactiveKind() == ResponseBodyReactiveKind.NONE) {
						// if a request body is injected, we have to use one
						if(hasFormParameters || requestBodyInfo != null) {
							responseBodyDataMethod = "one";
						}
						else {
							responseBodyDataMethod = "value";
						}
					}
					else if(responseBodyInfo.getBodyKind() == ResponseBodyKind.RAW) {
						responseBodyDataMethod = "stream";
					}
					else if(responseBodyInfo.getBodyReactiveKind() == ResponseBodyReactiveKind.ONE) {
						responseBodyDataMethod = "one";
					}
					else if(responseBodyInfo.getBodyReactiveKind() == ResponseBodyReactiveKind.MANY) {
						responseBodyDataMethod = "many";
					}
					else if(responseBodyInfo.getBodyReactiveKind() == ResponseBodyReactiveKind.PUBLISHER) {
						responseBodyDataMethod = "stream";
					}
					else {
						throw new IllegalStateException("Unknown response body reactive kind: " + responseBodyInfo.getBodyReactiveKind());
					}
					
					result.append(context.indent(0)).append("exchange.response().body()");
					if(typesMode) {
						result.append(".<").append(context.getTypeName(responseBodyInfo.getType())).append(">encoder(routeTypes[").append(routeInfo.getParameters().length).append("]).");
					}
					else {
						result.append(".encoder(").append(context.getTypeName(responseBodyInfo.getType())).append(".class).");
					}
					result.append(responseBodyDataMethod).append("(");
					result.append(controllerInvoke);
					result.append(");\n");
				}
				else if(responseBodyInfo.getBodyKind() == ResponseBodyKind.RESOURCE) {
					result.append(context.indent(0)).append("exchange.response().body().resource().value(");
					result.append(controllerInvoke);
					result.append(");\n");
				}
				else if(responseBodyInfo.getBodyKind() == ResponseBodyKind.SSE_RAW) {
					result.append(context.indent(0)).append("exchange.response().body().sse().from((events, data) -> data.stream(");
					result.append(controllerInvoke);
					result.append("));\n");
				}
				else if(responseBodyInfo.getBodyKind() == ResponseBodyKind.SSE_CHARSEQUENCE) {
					result.append(context.indent(0)).append("exchange.response().body().<").append(context.getTypeName(responseBodyInfo.getType())).append(">sseString().from((events, data) -> data.stream(");
					result.append(controllerInvoke);
					result.append("));\n");
				}
				else if(responseBodyInfo.getBodyKind() == ResponseBodyKind.SSE_ENCODED) {
					WebSseEventFactoryParameterInfo sseEventFactoryParameter = (WebSseEventFactoryParameterInfo)Arrays.stream(context.getWebRoute().getParameters()).filter(parameter -> parameter instanceof WebSseEventFactoryParameterInfo).findFirst().get();
					
					result.append(context.indent(0)).append("exchange.response().body()");
					if(typesMode) {
						result.append(".<").append(context.getTypeName(responseBodyInfo.getType())).append(">sseEncoder(\"").append(sseEventFactoryParameter.getDataMediaType().orElse("text/plain")).append("\", ").append("routeTypes[").append(routeInfo.getParameters().length).append("])");
					}
					else {
						result.append(".sseEncoder(\"").append(sseEventFactoryParameter.getDataMediaType().orElse("text/plain")).append("\", ").append(context.getTypeName(responseBodyInfo.getType())).append(".class)");
					}
					result.append(".from((events, data) -> data.stream(");
					result.append(controllerInvoke);
					result.append("));\n");
				}
			}
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebParameterInfo parameterInfo, WebRouterConfigurerClassGenerationContext context) {
		if(parameterInfo instanceof WebCookieParameterInfo) {
			return this.visit((WebCookieParameterInfo)parameterInfo, context);
		}
		else if(parameterInfo instanceof WebExchangeParameterInfo) {
			return this.visit((WebExchangeParameterInfo)parameterInfo, context);
		}
		else if(parameterInfo instanceof WebFormParameterInfo) {
			return this.visit((WebFormParameterInfo)parameterInfo, context);
		}
		else if(parameterInfo instanceof WebHeaderParameterInfo) {
			return this.visit((WebHeaderParameterInfo)parameterInfo, context);
		}
		else if(parameterInfo instanceof WebPathParameterInfo) {
			return this.visit((WebPathParameterInfo)parameterInfo, context);
		}
		else if(parameterInfo instanceof WebQueryParameterInfo) {
			return this.visit((WebQueryParameterInfo)parameterInfo, context);
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
	public StringBuilder visit(WebBasicParameterInfo basicParameterInfo, WebRouterConfigurerClassGenerationContext context) {
		if(context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS || context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE) {
			boolean typesMode = context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE;
			String parameterType = typesMode ? "routeTypes[" + context.getParameterIndex() + "]" : null;
			
			StringBuilder result = new StringBuilder();
			
			TypeMirror basicParameterType = basicParameterInfo.getType();
			if(basicParameterType.getKind() == TypeKind.ARRAY) {
				if(!typesMode) {
					parameterType = context.getTypeName(((ArrayType)basicParameterType).getComponentType()) + ".class";
				}
				result.append(context.getOptionalTypeName()).append(".ofNullable(");
				result.append(this.visit(basicParameterInfo, context.withMode(GenerationMode.ROUTE_PARAMETER_REFERENCE_MANY))).append(")");
				result.append(".<").append(context.getTypeName(basicParameterType)).append(">map(parameters -> parameters.stream().flatMap(parameter -> parameter.asListOf(").append(parameterType).append(").stream())");
				result.append(".toArray(").append(context.getTypeName(((ArrayType)basicParameterType).getComponentType())).append("[]::new)).filter(l -> l.length > 0)");
			}
			else if(context.getTypeUtils().isSameType(context.getCollectionType(), context.getTypeUtils().erasure(basicParameterType))) {
				if(!typesMode) {
					parameterType = context.getTypeName(((DeclaredType)basicParameterType).getTypeArguments().get(0)) + ".class";
				}
				
				result.append(context.getOptionalTypeName()).append(".ofNullable(");
				result.append(this.visit(basicParameterInfo, context.withMode(GenerationMode.ROUTE_PARAMETER_REFERENCE_MANY))).append(")");
				result.append(".<").append(context.getTypeName(basicParameterType)).append(">map(parameters -> parameters.stream().flatMap(parameter -> parameter.asListOf(").append(parameterType).append(").stream())");
				result.append(".collect(").append(context.getCollectorsTypeName()).append(".toList())).filter(l -> !l.isEmpty())");
			}
			else if(context.getTypeUtils().isSameType(context.getListType(), context.getTypeUtils().erasure(basicParameterType))) {
				if(!typesMode) {
					parameterType = context.getTypeName(((DeclaredType)basicParameterType).getTypeArguments().get(0)) + ".class";
				}
				
				result.append(context.getOptionalTypeName()).append(".ofNullable(");
				result.append(this.visit(basicParameterInfo, context.withMode(GenerationMode.ROUTE_PARAMETER_REFERENCE_MANY))).append(")");
				result.append(".<").append(context.getTypeName(basicParameterType)).append(">map(parameters -> parameters.stream().flatMap(parameter -> parameter.asListOf(").append(parameterType).append(").stream())");
				result.append(".collect(").append(context.getCollectorsTypeName()).append(".toList())).filter(l -> !l.isEmpty())");
			}
			else if(context.getTypeUtils().isSameType(context.getSetType(), context.getTypeUtils().erasure(basicParameterType))) {
				if(!typesMode) {
					parameterType = context.getTypeName(((DeclaredType)basicParameterType).getTypeArguments().get(0)) + ".class";
				}
				
				result.append(context.getOptionalTypeName()).append(".ofNullable(");
				result.append(this.visit(basicParameterInfo, context.withMode(GenerationMode.ROUTE_PARAMETER_REFERENCE_MANY))).append(")");
				result.append(".<").append(context.getTypeName(basicParameterType)).append(">map(parameters -> parameters.stream().flatMap(parameter -> parameter.asListOf(").append(parameterType).append(").stream())");
				result.append(".collect(").append(context.getCollectorsTypeName()).append(".toSet())).filter(l -> !l.isEmpty())");
			}
			else {
				result.append(this.visit(basicParameterInfo, context.withMode(GenerationMode.ROUTE_PARAMETER_REFERENCE_ONE)));
				result.append(".map(parameter -> parameter.");
				if(basicParameterType instanceof PrimitiveType) {
					// boolean, byte, short, int, long, char, float, and double.
					switch(basicParameterType.getKind()) {
						case BOOLEAN: result.append("asBoolean())");
							break;
						case BYTE: result.append("asByte())");
							break;
						case SHORT: result.append("asShort())");
							break;
						case INT: result.append("asInteger())");
							break;
						case LONG: result.append("asLong())");
							break;
						case CHAR: result.append("asCharacter())");
							break;
						case FLOAT: result.append("asFloat())");
							break;
						case DOUBLE: result.append("asDouble())");
							break;
						default:
							throw new IllegalStateException("Unsupported primitive type: " + basicParameterType);
					}
				}
				else {
					if(!typesMode) {
						parameterType = context.getTypeName(basicParameterType) + ".class";
					}
					else {
						result.append("<").append(context.getTypeName(basicParameterType)).append(">");
					}
					result.append("as(").append(parameterType).append("))");
				}
			}
			
			if(basicParameterInfo.isRequired()) {
				result.append(".orElseThrow(() -> new ").append(context.getMissingRequiredParameterExceptionTypeName()).append("(\"").append(basicParameterInfo.getQualifiedName().getParameterName()).append("\"))");
			}
			return result;
		}
		else {
			if(basicParameterInfo instanceof WebCookieParameterInfo) {
				return this.visit((WebCookieParameterInfo)basicParameterInfo, context);
			}
			else if(basicParameterInfo instanceof WebFormParameterInfo) {
				return this.visit((WebFormParameterInfo)basicParameterInfo, context);
			}
			else if(basicParameterInfo instanceof WebHeaderParameterInfo) {
				return this.visit((WebHeaderParameterInfo)basicParameterInfo, context);
			}
			else if(basicParameterInfo instanceof WebPathParameterInfo) {
				return this.visit((WebPathParameterInfo)basicParameterInfo, context);
			}
			else if(basicParameterInfo instanceof WebQueryParameterInfo) {
				return this.visit((WebQueryParameterInfo)basicParameterInfo, context);
			}
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebCookieParameterInfo cookieParameterInfo, WebRouterConfigurerClassGenerationContext context) {
		if(context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_ONE) {
			return new StringBuilder("exchange.request().cookies().get(\"").append(cookieParameterInfo.getQualifiedName().getParameterName()).append("\")");
		}
		else if(context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_MANY) {
			return new StringBuilder("exchange.request().cookies().getAll(\"").append(cookieParameterInfo.getQualifiedName().getParameterName()).append("\")");
		}
		else if(context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS || context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE) {
			return this.visit((WebBasicParameterInfo)cookieParameterInfo, context);
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebFormParameterInfo formParameterInfo, WebRouterConfigurerClassGenerationContext context) {
		if(context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_ONE) {
			return new StringBuilder(context.getOptionalTypeName()).append(".ofNullable(formParameters.get(\"").append(formParameterInfo.getQualifiedName().getParameterName()).append("\")).flatMap(parameter -> parameter.stream().findFirst())");
		}
		else if(context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_MANY) {
			return new StringBuilder("formParameters.get(\"").append(formParameterInfo.getQualifiedName().getParameterName()).append("\")");
		}
		else if(context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS || context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE) {
			return this.visit((WebBasicParameterInfo)formParameterInfo, context);
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebHeaderParameterInfo headerParameterInfo, WebRouterConfigurerClassGenerationContext context) {
		if(context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_ONE) {
			return new StringBuilder("exchange.request().headers().getParameter(\"").append(headerParameterInfo.getQualifiedName().getParameterName()).append("\")");
		}
		else if(context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_MANY) {
			return new StringBuilder("exchange.request().headers().getAllParameter(\"").append(headerParameterInfo.getQualifiedName().getParameterName()).append("\")");
		}
		else if(context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS || context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE) {
			return this.visit((WebBasicParameterInfo)headerParameterInfo, context);
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebPathParameterInfo pathParameterInfo, WebRouterConfigurerClassGenerationContext context) {
		if(context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_ONE) {
			return new StringBuilder("exchange.request().pathParameters().get(\"").append(pathParameterInfo.getQualifiedName().getParameterName()).append("\")");
		}
		if(context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_MANY) {
			return new StringBuilder("exchange.request().pathParameters().get(\"").append(pathParameterInfo.getQualifiedName().getParameterName()).append("\").map(parameter -> ").append(context.getListTypeName()).append(".of(parameter)).orElse(").append(context.getListTypeName()).append(".of())");
		}
		else if(context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS || context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE) {
			return this.visit((WebBasicParameterInfo)pathParameterInfo, context);
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebQueryParameterInfo queryParameterInfo, WebRouterConfigurerClassGenerationContext context) {
		if(context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_ONE) {
			return new StringBuilder("exchange.request().queryParameters().get(\"").append(queryParameterInfo.getQualifiedName().getParameterName()).append("\")");
		}
		else if(context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_MANY) {
			return new StringBuilder("exchange.request().queryParameters().getAll(\"").append(queryParameterInfo.getQualifiedName().getParameterName()).append("\")");
		}
		else if(context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS || context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE) {
			return this.visit((WebBasicParameterInfo)queryParameterInfo, context);
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebRequestBodyParameterInfo bodyParameterInfo, WebRouterConfigurerClassGenerationContext context) {
		if(context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS || context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE) {
			boolean typesMode = context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE;
			String parameterType = typesMode ? "routeTypes[" + context.getParameterIndex() + "]" : null;
			
			StringBuilder result = new StringBuilder();
			
			TypeMirror bodyParameterType = bodyParameterInfo.getType();
			if(bodyParameterInfo.getBodyReactiveKind() == RequestBodyReactiveKind.NONE) {
				if(bodyParameterInfo.getBodyKind() == RequestBodyKind.RAW) {
					result.append(context.getFluxTypeName()).append(".from(exchange.request().body().get().raw().stream()).reduceWith(() -> ").append(context.getUnpooledTypeName()).append(".unreleasableBuffer(").append(context.getUnpooledTypeName()).append(".buffer()), (acc, chunk) -> { try { return acc.writeBytes(chunk); } finally { chunk.release(); } })");
				}
				else if(bodyParameterInfo.getBodyKind() == RequestBodyKind.ENCODED) {
					result.append("exchange.request().body().get().");
					if(!typesMode) {
						parameterType = context.getTypeName(bodyParameterType) + ".class";
					}
					else {
						result.append("<").append(context.getTypeName(bodyParameterType)).append(">");
					}
					result.append("decoder(").append(parameterType).append(").one()");
				}
				else {
					throw new IllegalStateException("Can't generate non-reactive body parameter of kind: " + bodyParameterInfo.getBodyKind());
				}
			}
			else {
				// Reactive
				result.append("exchange.request().body().get().");
				if(bodyParameterInfo.getBodyKind() == RequestBodyKind.ENCODED) {
					if(!typesMode) {
						parameterType = context.getTypeName(bodyParameterInfo.getType()) + ".class";
					}
					else {
						result.append("<").append(context.getTypeName(bodyParameterType)).append(">");
					}
					
					result.append("decoder(").append(parameterType).append(")");
					if(bodyParameterInfo.getBodyReactiveKind() == RequestBodyReactiveKind.ONE) {
						result.append(".one()");
					}
					else if(bodyParameterInfo.getBodyReactiveKind() == RequestBodyReactiveKind.PUBLISHER || bodyParameterInfo.getBodyReactiveKind() == RequestBodyReactiveKind.MANY) {
						result.append(".many()");
					}
					else {
						throw new IllegalStateException("Unknown request body reactive kind: " + bodyParameterInfo.getBodyReactiveKind());
					}
				}
				else {
					if(bodyParameterInfo.getBodyKind() == RequestBodyKind.RAW) {
						result.append("raw().stream()");
					}
					else if(bodyParameterInfo.getBodyKind() == RequestBodyKind.MULTIPART) {
						result.append("multipart().stream()");
					}
					else if(bodyParameterInfo.getBodyKind() == RequestBodyKind.URLENCODED) {
						result.append("urlEncoded().stream()");
					}
					else {
						throw new IllegalStateException("Unknown request body kind: " + bodyParameterInfo.getBodyKind());
					}
					
					if(bodyParameterInfo.getBodyReactiveKind() == RequestBodyReactiveKind.ONE) {
						result.insert(0, ".from(").insert(0, context.getMonoTypeName()).append(")");
					}
					else if(bodyParameterInfo.getBodyReactiveKind() == RequestBodyReactiveKind.MANY || bodyParameterInfo.getBodyReactiveKind() == RequestBodyReactiveKind.PUBLISHER) {
						result.insert(0, ".from(").insert(0, context.getFluxTypeName()).append(")");
					}
					else {
						throw new IllegalStateException("Unknown request body reactive kind: " + bodyParameterInfo.getBodyReactiveKind());
					}
				}
			}
			return result;
		}
		return new StringBuilder();
	}
	
	@Override
	public StringBuilder visit(WebExchangeParameterInfo exchangeParameterInfo, WebRouterConfigurerClassGenerationContext context) {
		if(context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS || context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE) {
			return new StringBuilder("exchange");
		}
		return new StringBuilder();
	}
	
	@Override
	public StringBuilder visit(WebSseEventFactoryParameterInfo sseEventFactoryParameterInfo, WebRouterConfigurerClassGenerationContext context) {
		if(context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS || context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE) {
			return new StringBuilder("events");
		}
		return new StringBuilder();
	}
}

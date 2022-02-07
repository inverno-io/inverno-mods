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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.net.URIs;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.web.WebInterceptorsConfigurer;
import io.inverno.mod.web.WebRouter;
import io.inverno.mod.web.WebRouterConfigurer;
import io.inverno.mod.web.WebRoutesConfigurer;
import io.inverno.mod.web.annotation.WebRoutes;
import io.inverno.mod.web.compiler.internal.WebRouterConfigurerClassGenerationContext.GenerationMode;
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
import io.inverno.mod.web.compiler.spi.WebProvidedRouterConfigurerInfo;
import io.inverno.mod.web.compiler.spi.WebQueryParameterInfo;
import io.inverno.mod.web.compiler.spi.WebRequestBodyParameterInfo;
import io.inverno.mod.web.compiler.spi.WebRequestBodyParameterInfo.RequestBodyKind;
import io.inverno.mod.web.compiler.spi.WebRequestBodyParameterInfo.RequestBodyReactiveKind;
import io.inverno.mod.web.compiler.spi.WebResponseBodyInfo;
import io.inverno.mod.web.compiler.spi.WebResponseBodyInfo.ResponseBodyKind;
import io.inverno.mod.web.compiler.spi.WebResponseBodyInfo.ResponseBodyReactiveKind;
import io.inverno.mod.web.compiler.spi.WebRouteInfo;
import io.inverno.mod.web.compiler.spi.WebRouterConfigurerInfo;
import io.inverno.mod.web.compiler.spi.WebRouterConfigurerInfoVisitor;
import io.inverno.mod.web.compiler.spi.WebRoutesConfigurerInfo;
import io.inverno.mod.web.compiler.spi.WebSseEventFactoryParameterInfo;
import java.time.ZonedDateTime;
import org.apache.commons.text.StringEscapeUtils;

/**
 * <p>
 * A {@link WebRouterConfigurerInfoVisitor} implementation used to generates a
 * web router configurer class in an Inverno module.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class WebRouterConfigurerClassGenerator implements WebRouterConfigurerInfoVisitor<StringBuilder, WebRouterConfigurerClassGenerationContext> {

	@Override
	public StringBuilder visit(WebRouterConfigurerInfo routerConfigurerInfo, WebRouterConfigurerClassGenerationContext context) {
		String configurerClassName = routerConfigurerInfo.getQualifiedName().getClassName();
		String configurerPackageName = configurerClassName.lastIndexOf(".") != -1 ? configurerClassName.substring(0, configurerClassName.lastIndexOf(".")) : "";
		configurerClassName = configurerClassName.substring(configurerPackageName.length() + 1);
		if(context.getMode() == GenerationMode.CONFIGURER_CLASS) {
			TypeMirror generatedType = context.getElementUtils().getTypeElement(context.getElementUtils().getModuleElement("java.compiler"), "javax.annotation.processing.Generated").asType();
			TypeMirror beanAnnotationType = context.getElementUtils().getTypeElement(Bean.class.getCanonicalName()).asType();
			TypeMirror webRouterConfigurerType = context.getTypeUtils().erasure(context.getElementUtils().getTypeElement(WebRouterConfigurer.class.getCanonicalName()).asType());
			TypeMirror routerType = context.getTypeUtils().erasure(context.getElementUtils().getTypeElement(WebRouter.class.getCanonicalName()).asType());
			
			context.addImport(configurerClassName, configurerPackageName + "." + configurerClassName);
			context.addImport("Context", configurerPackageName + "." + configurerClassName + ".Context");
			
			StringBuilder configurerAnnotation = this.visit(routerConfigurerInfo, context.withIndentDepth(1).withMode(GenerationMode.CONFIGURER_ANNOTATION));
			
			StringBuilder configurer_controller_fields = Arrays.stream(routerConfigurerInfo.getControllers())
				.map(controllerInfo -> this.visit(controllerInfo, context.withIndentDepth(1).withMode(GenerationMode.CONTROLLER_FIELD)))
				.collect(context.joining(System.lineSeparator()));
			
			//List<WebInterceptorsConfigurer<? super WebRouterConfigurer.Context>> routerConfs = null;
			TypeMirror interceptorsConfigurerType = context.getTypeUtils().erasure(context.getElementUtils().getTypeElement(WebInterceptorsConfigurer.class.getCanonicalName()).asType());
			//List<WebRoutesConfigurer<? super WebRouterConfigurer.Context>> routerConfs = null;
			TypeMirror routesConfigurerType = context.getTypeUtils().erasure(context.getElementUtils().getTypeElement(WebRoutesConfigurer.class.getCanonicalName()).asType());
			//List<WebRouterConfigurer<? super WebRouterConfigurer.Context>> routerConfs = null;
			TypeMirror routerConfigurerType = context.getTypeUtils().erasure(context.getElementUtils().getTypeElement(WebRouterConfigurer.class.getCanonicalName()).asType());
			
			StringBuilder interceptorsConfigurersDecl = new StringBuilder(context.getListTypeName()).append("<").append(context.getTypeName(interceptorsConfigurerType)).append("<? super ").append(configurerClassName).append(".Context>> interceptorsConfigurers");
			StringBuilder routesConfigurersDecl = new StringBuilder(context.getListTypeName()).append("<").append(context.getTypeName(routesConfigurerType)).append("<? super ").append(configurerClassName).append(".Context>> routesConfigurers");
			StringBuilder routerConfigurersDecl = new StringBuilder(context.getListTypeName()).append("<").append(context.getTypeName(routerConfigurerType)).append("<? super ").append(configurerClassName).append(".Context>> routerConfigurers");
			
			StringBuilder interceptorsConfigurersField = new StringBuilder(context.indent(1)).append("private ").append(interceptorsConfigurersDecl).append(";");
			StringBuilder routesConfigurersField = new StringBuilder(context.indent(1)).append("private ").append(routesConfigurersDecl).append(";");
			StringBuilder routerConfigurersField = new StringBuilder(context.indent(1)).append("private ").append(routerConfigurersDecl).append(";");
			
			StringBuilder interceptorsConfigurersSetter = new StringBuilder(context.indent(1)).append("public void setInterceptorsConfigurers(").append(interceptorsConfigurersDecl).append(") {").append(System.lineSeparator());
			interceptorsConfigurersSetter.append(context.indent(2)).append("this.interceptorsConfigurers = interceptorsConfigurers;").append(System.lineSeparator());
			interceptorsConfigurersSetter.append(context.indent(1)).append("}");
			
			StringBuilder routesConfigurersSetter = new StringBuilder(context.indent(1)).append("public void setRoutesConfigurers(").append(routesConfigurersDecl).append(") {").append(System.lineSeparator());
			routesConfigurersSetter.append(context.indent(2)).append("this.routesConfigurers = routesConfigurers;").append(System.lineSeparator());
			routesConfigurersSetter.append(context.indent(1)).append("}");
			
			StringBuilder routerConfigurersSetter = new StringBuilder(context.indent(1)).append("public void setRouterConfigurers(").append(routerConfigurersDecl).append(") {").append(System.lineSeparator());
			routerConfigurersSetter.append(context.indent(2)).append("this.routerConfigurers = routerConfigurers;").append(System.lineSeparator());
			routerConfigurersSetter.append(context.indent(1)).append("}");
			
			StringBuilder configurer_constructor = new StringBuilder(context.indent(1)).append("public ").append(configurerClassName).append("(");
			
			configurer_constructor.append(Arrays.stream(routerConfigurerInfo.getControllers()).map(controllerInfo -> this.visit(controllerInfo, context.withIndentDepth(0).withMode(GenerationMode.CONTROLLER_PARAMETER))).collect(context.joining(", ")));
			configurer_constructor.append(") {").append(System.lineSeparator());
			configurer_constructor.append(Arrays.stream(routerConfigurerInfo.getControllers()).map(controllerInfo -> this.visit(controllerInfo, context.withIndentDepth(2).withMode(GenerationMode.CONTROLLER_ASSIGNMENT))).collect(context.joining(System.lineSeparator())));
			configurer_constructor.append(System.lineSeparator()).append(context.indent(1)).append("}");
			
			StringBuilder configurer_accept = new StringBuilder(context.indent(1)).append("@Override").append(System.lineSeparator());
			configurer_accept.append(context.indent(1)).append("public void accept(").append(context.getTypeName(routerType)).append("<").append(configurerClassName).append(".Context> router) {");
			
			configurer_accept.append(System.lineSeparator()).append(context.indent(2)).append("router").append(System.lineSeparator());
			configurer_accept.append(context.indent(3)).append(".configureInterceptors(this.interceptorsConfigurers)").append(System.lineSeparator());
			configurer_accept.append(context.indent(3)).append(".configure(this.routerConfigurers)").append(System.lineSeparator());
			configurer_accept.append(context.indent(3)).append(".configureRoutes(this.routesConfigurers)").append(System.lineSeparator());
			if(routerConfigurerInfo.getControllers().length > 0) {
				configurer_accept.append(Arrays.stream(routerConfigurerInfo.getControllers()).map(controllerInfo -> this.visit(controllerInfo, context.withIndentDepth(3).withMode(GenerationMode.ROUTE_DECLARATION))).collect(context.joining(System.lineSeparator())));
			}
			configurer_accept.append(";").append(System.lineSeparator());
			
			configurer_accept.append(context.indent(1)).append("}");
			
			StringBuilder configurer_context_creator = this.visit(routerConfigurerInfo, context.withIndentDepth(1).withMode(GenerationMode.CONFIGURER_CONTEXT_CREATOR));
			
			StringBuilder configurer_context = this.visit(routerConfigurerInfo, context.withIndentDepth(1).withMode(GenerationMode.CONFIGURER_CONTEXT));
			
			StringBuilder configurer_class = new StringBuilder();
			
			configurer_class.append(configurerAnnotation).append(System.lineSeparator());
			configurer_class.append("@").append(context.getTypeName(beanAnnotationType)).append(System.lineSeparator());
			configurer_class.append("@").append(context.getTypeName(generatedType)).append("(value=\"").append(WebRouterConfigurerCompilerPlugin.class.getCanonicalName()).append("\", date = \"").append(ZonedDateTime.now().toString()).append("\")").append(System.lineSeparator());
			configurer_class.append("public final class ").append(configurerClassName).append(" implements ").append(context.getTypeName(webRouterConfigurerType)).append("<").append(configurerClassName).append(".Context> {").append(System.lineSeparator()).append(System.lineSeparator());
			
			configurer_class.append(interceptorsConfigurersField).append(System.lineSeparator());
			configurer_class.append(routesConfigurersField).append(System.lineSeparator());
			configurer_class.append(routerConfigurersField).append(System.lineSeparator()).append(System.lineSeparator());
			
			if(routerConfigurerInfo.getControllers().length > 0) {
				configurer_class.append(configurer_controller_fields).append(System.lineSeparator()).append(System.lineSeparator());
			}
			configurer_class.append(configurer_constructor).append(System.lineSeparator()).append(System.lineSeparator());
			configurer_class.append(configurer_accept).append(System.lineSeparator()).append(System.lineSeparator());
			
			configurer_class.append(interceptorsConfigurersSetter).append(System.lineSeparator()).append(System.lineSeparator());
			configurer_class.append(routesConfigurersSetter).append(System.lineSeparator()).append(System.lineSeparator());
			configurer_class.append(routerConfigurersSetter).append(System.lineSeparator()).append(System.lineSeparator());
			
			configurer_class.append(configurer_context_creator).append(System.lineSeparator()).append(System.lineSeparator());
			configurer_class.append(configurer_context).append(System.lineSeparator());
			
			configurer_class.append("}");
			
			context.removeImport(configurerClassName);
			context.removeImport("Context");
			
			configurer_class.insert(0, System.lineSeparator() + System.lineSeparator()).insert(0, context.getImports().stream().sorted().filter(i -> i.lastIndexOf(".") > 0 && !i.substring(0, i.lastIndexOf(".")).equals(configurerPackageName)).map(i -> new StringBuilder().append("import ").append(i).append(";")).collect(context.joining(System.lineSeparator())));
			if(!configurerPackageName.equals("")) {
				configurer_class.insert(0, ";" + System.lineSeparator() + System.lineSeparator()).insert(0, configurerPackageName).insert(0, "package ");
			}
			
			return configurer_class;
		}
		else if(context.getMode() == GenerationMode.CONFIGURER_ANNOTATION) {
			TypeMirror webRoutesAnnotationType = context.getElementUtils().getTypeElement(WebRoutes.class.getCanonicalName()).asType();
			StringBuilder result = new StringBuilder();
			result.append("@").append(context.getTypeName(webRoutesAnnotationType)).append("({").append(System.lineSeparator());
			result.append(Stream.concat(
					Arrays.stream(routerConfigurerInfo.getRouterConfigurers()).map(routerInfo -> this.visit(routerInfo, context)),
					Arrays.stream(routerConfigurerInfo.getControllers())
						.flatMap(controllerInfo -> Arrays.stream(controllerInfo.getRoutes()).map(routeInfo -> this.visit(routeInfo, context.withWebController(controllerInfo).withMode(GenerationMode.ROUTE_ANNOTATION))))
				)
				.collect(context.joining("," + System.lineSeparator()))
			);
			result.append(System.lineSeparator()).append("})");
			return result;
		}
		else if(context.getMode() == GenerationMode.CONFIGURER_CONTEXT) {
			StringBuilder result = new StringBuilder();
			result.append(context.indent(0)).append("public static interface Context extends ");
			if(routerConfigurerInfo.getContextTypes().length > 0) {
				result.append(Arrays.stream(routerConfigurerInfo.getContextTypes()).map(context::getTypeName).collect(Collectors.joining(", ")));
			}
			else {
				result.append(context.getTypeName(context.getElementUtils().getTypeElement(ExchangeContext.class.getCanonicalName()).asType()));
			}
			result.append(" {}");
			
			return result;
		}
		else if(context.getMode() == GenerationMode.CONFIGURER_CONTEXT_CREATOR) {
			// For each context type I must define the methods in order to implement the context
			// This can actually be tricky because some interface may override others
			// Let's start by listing all methods
			
			Map<String, ExecutableElement> methodsToImplement = new HashMap<>();
			Map<String, ExecutableElement> defaultMethods = new HashMap<>();
			Arrays.stream(routerConfigurerInfo.getContextTypes())
				.flatMap(type -> ElementFilter.methodsIn(context.getElementUtils().getAllMembers((TypeElement)context.getTypeUtils().asElement(type))).stream())
				.filter(element -> element.getEnclosingElement().getKind() == ElementKind.INTERFACE)
				.forEach(element -> {
					StringBuilder signatureKeyBuilder = new StringBuilder();
					signatureKeyBuilder.append(element.getSimpleName().toString());
					element.getParameters().stream().map(variableElement -> context.getTypeUtils().erasure(variableElement.asType()).toString()).forEach(signatureKeyBuilder::append);
					String signatureKey = signatureKeyBuilder.toString();
					
					if(element.isDefault()) {
						defaultMethods.put(signatureKey, element);
						if(methodsToImplement.containsKey(signatureKey)) {
							methodsToImplement.remove(signatureKey);
						}
					}
					else if(!defaultMethods.containsKey(signatureKey)) {
						methodsToImplement.put(signatureKey, element);
					}
				});
			
			Map<String, TypeMirror> contextFields = new HashMap<>();
			StringBuilder context_methods = methodsToImplement.values().stream().map(element -> {
				StringBuilder contextMethod = new StringBuilder();
				String methodName = element.getSimpleName().toString();
				if(methodName.startsWith("get")) {
					String fieldName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
					if(!element.getParameters().isEmpty() || element.getReturnType().getKind() == TypeKind.VOID) {
						// report invalid getter
					}
					contextFields.put(fieldName, element.getReturnType());
					
					contextMethod.append(context.indent(2)).append("@Override").append(System.lineSeparator());
					contextMethod.append(context.indent(2)).append("public ").append(context.getTypeName(element.getReturnType())).append(" ").append(methodName).append("() {").append(System.lineSeparator());
					contextMethod.append(context.indent(3)).append("return this.").append(fieldName).append(";").append(System.lineSeparator());
					contextMethod.append(context.indent(2)).append("}");
				}
				else if(methodName.startsWith("set")) {
					String fieldName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
					if(element.getParameters().size() != 1 || element.getReturnType().getKind() != TypeKind.VOID) {
						// report invalid setter
					}
					contextFields.put(fieldName, element.getParameters().get(0).asType());
					
					contextMethod.append(context.indent(2)).append("@Override").append(System.lineSeparator());
					contextMethod.append(context.indent(2)).append("public void ").append(methodName).append("(").append(context.getTypeName(element.getParameters().get(0).asType())).append(" ").append(fieldName).append(") {").append(System.lineSeparator());
					contextMethod.append(context.indent(3)).append("this.").append(fieldName).append(" = ").append(fieldName).append(";").append(System.lineSeparator());
					contextMethod.append(context.indent(2)).append("}");
				}
				else {
					contextMethod.append(context.indent(2)).append("public ").append(context.getTypeName(element.getReturnType())).append(" ").append(methodName).append("(").append(element.getParameters().stream().map(variableElement -> new StringBuilder().append(context.getTypeName(variableElement.asType())).append(" ").append(variableElement.getSimpleName().toString())).collect(context.joining(", "))).append(") {").append(System.lineSeparator());
					if(element.getReturnType().getKind() != TypeKind.VOID) {
						contextMethod.append(context.indent(3)).append("return ");
						switch(element.getReturnType().getKind()) {
							case BOOLEAN: contextMethod.append("false");
								break;
							case BYTE:
							case CHAR:
							case SHORT:
							case INT:
							case LONG:
							case FLOAT:
							case DOUBLE: contextMethod.append("0");
								break;
							default: contextMethod.append("null");
						}
						contextMethod.append(";").append(System.lineSeparator());
					}
					contextMethod.append(context.indent(2)).append("}");
				}
				return contextMethod;
			}).collect(context.joining(System.lineSeparator() + System.lineSeparator()));
			
			StringBuilder context_fields = contextFields.entrySet().stream().map(e -> {
				StringBuilder contextField = new StringBuilder();
				contextField.append(context.indent(2)).append("private ").append(context.getTypeName(e.getValue())).append(" ").append(e.getKey()).append(";");
				return contextField;
			}).collect(context.joining(System.lineSeparator()));
			
			StringBuilder context_creator = new StringBuilder();
			context_creator.append(context.indent(0)).append("@Override").append(System.lineSeparator());
			context_creator.append(context.indent(0)).append("public Context createContext() {").append(System.lineSeparator());
			context_creator.append(context.indent(1)).append("return new Context() {").append(System.lineSeparator());
			context_creator.append(context_fields).append(System.lineSeparator()).append(System.lineSeparator());
			context_creator.append(context_methods).append(System.lineSeparator());
			context_creator.append(context.indent(1)).append("};").append(System.lineSeparator());
			context_creator.append(context.indent(0)).append("}");
			
			return context_creator;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebInterceptorsConfigurerInfo interceptorsConfigurerInfo, WebRouterConfigurerClassGenerationContext context) {
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebRoutesConfigurerInfo routesConfigurerInfo, WebRouterConfigurerClassGenerationContext context) {
		if(context.getMode() == GenerationMode.CONFIGURER_ANNOTATION) {
			return Arrays.stream(routesConfigurerInfo.getRoutes())
				.map(routeInfo -> this.visit(routeInfo, context.withMode(GenerationMode.ROUTE_ANNOTATION)))
				.collect(context.joining("," + System.lineSeparator()));
		}
		return new StringBuilder();
	}
	
	@Override
	public StringBuilder visit(WebProvidedRouterConfigurerInfo providedRouterConfigurerInfo, WebRouterConfigurerClassGenerationContext context) {
		if(context.getMode() == GenerationMode.CONFIGURER_ANNOTATION) {
			return Arrays.stream(providedRouterConfigurerInfo.getRoutes())
				.map(routeInfo -> this.visit(routeInfo, context.withMode(GenerationMode.ROUTE_ANNOTATION)))
				.collect(context.joining("," + System.lineSeparator()));
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebControllerInfo controllerInfo, WebRouterConfigurerClassGenerationContext context) {
		if(context.getMode() == GenerationMode.CONTROLLER_FIELD) {
			return new StringBuilder(context.indent(0)).append("private final ").append(context.getTypeName(controllerInfo.getType())).append(" ").append(context.getFieldName(controllerInfo.getQualifiedName())).append(";");
		}
		else if(context.getMode() == GenerationMode.CONTROLLER_PARAMETER) {
			return new StringBuilder(context.indent(0)).append(context.getTypeName(controllerInfo.getType())).append(" ").append(context.getFieldName(controllerInfo.getQualifiedName()));
		}
		else if(context.getMode() == GenerationMode.CONTROLLER_ASSIGNMENT) {
			return new StringBuilder(context.indent(0)).append("this.").append(context.getFieldName(controllerInfo.getQualifiedName())).append(" = ").append(context.getFieldName(controllerInfo.getQualifiedName())).append(";");
		}
		else if(context.getMode() == GenerationMode.ROUTE_DECLARATION) {
			return Arrays.stream(controllerInfo.getRoutes()).map(routeInfo -> this.visit(routeInfo, context.withWebController(controllerInfo))).collect(context.joining(System.lineSeparator()));
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
					.map(path -> "\"" + StringEscapeUtils.escapeJava(routeInfo.getController()
						.map(WebControllerInfo::getRootPath)
						.map(rootPath -> URIs.uri(rootPath, URIs.RequestTargetForm.ABSOLUTE, URIs.Option.PARAMETERIZED, URIs.Option.NORMALIZED, URIs.Option.PATH_PATTERN).path(path, false).buildRawPath())
						.orElse(path)) + "\""
					)
					.collect(Collectors.joining(", "))
				);	
			}
			else {
				routeInfo.getController()
					.map(WebControllerInfo::getRootPath)
					.map(rootPath -> URIs.uri(rootPath, URIs.RequestTargetForm.ABSOLUTE, URIs.Option.PARAMETERIZED, URIs.Option.NORMALIZED, URIs.Option.PATH_PATTERN).buildRawPath())
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
			
			StringBuilder routeHandler = new StringBuilder("exchange -> {").append(System.lineSeparator());
			routeHandler.append(this.visit(routeInfo.getResponseBody(), context.withIndentDepth(context.getIndentDepth() + (typesMode ? 2 : 1) ).withWebRoute(routeInfo).withMode(handlerBodyMode)));
			routeHandler.append(context.indent(typesMode ? 1 : 0)).append("}");
			
			StringBuilder routeManager = new StringBuilder();
			
			if(routeInfo.getPaths().length > 0) {
				routeManager.append(Arrays.stream(routeInfo.getPaths())
					.map(path -> ".path(\"" + StringEscapeUtils.escapeJava(routeInfo.getController()
						.map(WebControllerInfo::getRootPath)
						.map(rootPath -> URIs.uri(rootPath, URIs.RequestTargetForm.ABSOLUTE, URIs.Option.PARAMETERIZED, URIs.Option.NORMALIZED, URIs.Option.PATH_PATTERN).path(path, false).buildRawPath())
						.orElse(path)) + "\", " + routeInfo.isMatchTrailingSlash() + ")"
					)
					.collect(Collectors.joining())
				);
			}
			else {
				routeInfo.getController()
					.map(WebControllerInfo::getRootPath)
					.map(rootPath -> URIs.uri(rootPath, URIs.RequestTargetForm.ABSOLUTE, URIs.Option.PARAMETERIZED, URIs.Option.NORMALIZED, URIs.Option.PATH_PATTERN).buildRawPath())
					.ifPresent(rootPath -> routeManager.append(".path(\"").append(StringEscapeUtils.escapeJava(rootPath)).append("\", ").append(routeInfo.isMatchTrailingSlash()).append(")"));
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
				StringBuilder routeTypes = new StringBuilder(context.getTypeTypeName()).append("[] routeTypes = new ").append(context.getTypeTypeName()).append("[] {").append(System.lineSeparator());
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
					.collect(context.joining("," + System.lineSeparator()))
				);
				
				routeTypes.append(System.lineSeparator()).append(context.indent(1)).append("}");
				
				StringBuilder result = new StringBuilder(context.indent(0)).append(".route(route -> {").append(System.lineSeparator());
				result.append(context.indent(1)).append(routeTypes).append(";").append(System.lineSeparator());
				result.append(context.indent(1)).append("route").append(routeManager).append(";").append(System.lineSeparator());
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
					result.append("));").append(System.lineSeparator());
				}
				else if(nonReactiveRequestBodyParameterIndex != null) {
					result.append(context.indent(0)).append("exchange.response().body().raw().stream(");
					result.append(this.visit(routeInfo.getParameters()[nonReactiveRequestBodyParameterIndex], context.withIndentDepth(0).withMode(parameterReferenceMode).withParameterIndex(nonReactiveRequestBodyParameterIndex))).append(".flatMap(body -> ");
					result.append("{ ").append(controllerInvoke).append("; return ").append(context.getMonoTypeName()).append(".empty(); }");
					result.append("));").append(System.lineSeparator());
				}
				else {
					result.append(context.indent(0)).append(controllerInvoke).append(";").append(System.lineSeparator());
					result.append(context.indent(0)).append("exchange.response().body().empty();").append(System.lineSeparator());
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
					String mapFunction;
					if(responseBodyInfo.getBodyReactiveKind() == ResponseBodyReactiveKind.NONE) {
						mapFunction = "map";
					}
					else if(responseBodyInfo.getBodyReactiveKind() == ResponseBodyReactiveKind.ONE) {
						if(responseBodyInfo.getBodyKind() == ResponseBodyKind.EMPTY) {
							mapFunction = "flatMap";
						}
						else {
							mapFunction = "map";
						}
					}
					else if(responseBodyInfo.getBodyReactiveKind() == ResponseBodyReactiveKind.PUBLISHER || responseBodyInfo.getBodyReactiveKind() == ResponseBodyReactiveKind.MANY) {
						mapFunction = "flatMapMany";
					}
					else {
						throw new IllegalStateException("Unknown response body reactive kind: " + responseBodyInfo.getBodyReactiveKind());
					}
					controllerInvoke.insert(0, new StringBuilder(context.getFluxTypeName()).append(".from(exchange.request().body().get().urlEncoded().stream()).collectMultimap(").append(context.getParameterTypeName()).append("::getName)").append(".").append(mapFunction).append("(formParameters -> ")).append(")");
				}
				
				if(responseBodyInfo.getBodyKind() == ResponseBodyKind.EMPTY) {
					// We know we are reactive here
					if(responseBodyInfo.getBodyReactiveKind() == ResponseBodyReactiveKind.PUBLISHER) {
						controllerInvoke.insert(0, new StringBuilder(context.getFluxTypeName()).append(".from(")).append(")");
					}
					result.append(context.indent(0)).append("exchange.response().body().raw().stream(");
					result.append(controllerInvoke).append(".then().cast(").append(context.getByteBufTypeName()).append(".class)");
					result.append(");").append(System.lineSeparator());
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
					result.append(");").append(System.lineSeparator());
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
					result.append(");").append(System.lineSeparator());
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
					result.append(");").append(System.lineSeparator());
				}
				else if(responseBodyInfo.getBodyKind() == ResponseBodyKind.RESOURCE) {
					result.append(context.indent(0)).append("exchange.response().body().resource().value(");
					result.append(controllerInvoke);
					result.append(");").append(System.lineSeparator());
				}
				else if(responseBodyInfo.getBodyKind() == ResponseBodyKind.SSE_RAW) {
					result.append(context.indent(0)).append("exchange.response().body().sse().from((events, data) -> data.stream(");
					result.append(controllerInvoke);
					result.append("));").append(System.lineSeparator());
				}
				else if(responseBodyInfo.getBodyKind() == ResponseBodyKind.SSE_CHARSEQUENCE) {
					result.append(context.indent(0)).append("exchange.response().body().<").append(context.getTypeName(responseBodyInfo.getType())).append(">sseString().from((events, data) -> data.stream(");
					result.append(controllerInvoke);
					result.append("));").append(System.lineSeparator());
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
					result.append("));").append(System.lineSeparator());
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
		else if(parameterInfo instanceof WebExchangeContextParameterInfo) {
			return this.visit((WebExchangeContextParameterInfo)parameterInfo, context);
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
	public StringBuilder visit(WebExchangeContextParameterInfo exchangeContextParameterInfo, WebRouterConfigurerClassGenerationContext context) {
		if(context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS || context.getMode() == GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE) {
			return new StringBuilder("exchange.context()");
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

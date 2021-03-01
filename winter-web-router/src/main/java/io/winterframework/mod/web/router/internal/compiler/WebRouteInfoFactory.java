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
package io.winterframework.mod.web.router.internal.compiler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import io.winterframework.core.compiler.spi.BeanInfo;
import io.winterframework.core.compiler.spi.BeanQualifiedName;
import io.winterframework.core.compiler.spi.ReporterInfo;
import io.winterframework.core.compiler.spi.plugin.PluginContext;
import io.winterframework.core.compiler.spi.plugin.PluginExecution;
import io.winterframework.mod.base.resource.Resource;
import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.router.WebResponseBody;
import io.winterframework.mod.web.router.annotation.WebController;
import io.winterframework.mod.web.router.annotation.WebRoute;
import io.winterframework.mod.web.router.annotation.WebRoutes;
import io.winterframework.mod.web.router.internal.compiler.spi.WebFormParameterInfo;
import io.winterframework.mod.web.router.internal.compiler.spi.WebParameterInfo;
import io.winterframework.mod.web.router.internal.compiler.spi.WebRequestBodyParameterInfo;
import io.winterframework.mod.web.router.internal.compiler.spi.WebResponseBodyInfo;
import io.winterframework.mod.web.router.internal.compiler.spi.WebResponseBodyInfo.ResponseBodyKind;
import io.winterframework.mod.web.router.internal.compiler.spi.WebResponseBodyInfo.ResponseBodyReactiveKind;
import io.winterframework.mod.web.router.internal.compiler.spi.WebRouteQualifiedName;
import io.winterframework.mod.web.router.internal.compiler.spi.WebSseEventFactoryParameterInfo;
import io.winterframework.mod.web.router.internal.compiler.spi.WebSseEventFactoryParameterInfo.SseEventFactoryKind;
import io.winterframework.mod.web.server.ResponseBody;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
class WebRouteInfoFactory {

	private final PluginContext pluginContext;
	private final PluginExecution pluginExecution;
	private final WebParameterInfoFactory parameterFactory;
	private final Map<ExecutableElement, GenericWebRouteInfo> routes;
	private final TypeHierarchyExtractor typeHierarchyExtractor;
	
	/* Web annotations */
	private final TypeMirror webControllerAnnotationType;
	private final TypeMirror webRoutesAnnotationType;
	private final TypeMirror webRouteAnnotationType;
	
	/* Types */
	private final TypeMirror publisherType;
	private final TypeMirror monoType;
	private final TypeMirror fluxType;
	private final TypeMirror byteBufType;
	private final TypeMirror sseRawEventType;
	private final TypeMirror SseEncoderEventType;
	private final TypeMirror resourceType;
	
	public WebRouteInfoFactory(PluginContext pluginContext, PluginExecution pluginExecution) {
		this.pluginContext = Objects.requireNonNull(pluginContext);
		this.pluginExecution = Objects.requireNonNull(pluginExecution);
		this.parameterFactory = new WebParameterInfoFactory(this.pluginContext , this.pluginExecution);
		this.routes = new HashMap<>();
		this.typeHierarchyExtractor = new TypeHierarchyExtractor(this.pluginContext.getTypeUtils());
		
		this.webControllerAnnotationType = this.pluginContext.getElementUtils().getTypeElement(WebController.class.getCanonicalName()).asType();
		this.webRoutesAnnotationType = this.pluginContext.getElementUtils().getTypeElement(WebRoutes.class.getCanonicalName()).asType();
		this.webRouteAnnotationType = this.pluginContext.getElementUtils().getTypeElement(WebRoute.class.getCanonicalName()).asType();
		
		this.publisherType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Publisher.class.getCanonicalName()).asType());
		this.monoType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Mono.class.getCanonicalName()).asType());
		this.fluxType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Flux.class.getCanonicalName()).asType());
		this.byteBufType = this.pluginContext.getElementUtils().getTypeElement(ByteBuf.class.getCanonicalName()).asType();
		this.sseRawEventType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(ResponseBody.Sse.Event.class.getCanonicalName()).asType());
		this.SseEncoderEventType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(WebResponseBody.SseEncoder.Event.class.getCanonicalName()).asType());
		this.resourceType = this.pluginContext.getElementUtils().getTypeElement(Resource.class.getCanonicalName()).asType();
	}

	public Optional<GenericWebRouteInfo> compileRoute(ExecutableElement routeElement) {
		AnnotationMirror webRouteAnnotation = null;
		for(AnnotationMirror annotation : this.pluginContext.getElementUtils().getAllAnnotationMirrors(routeElement)) {
			if(this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.webRouteAnnotationType)) {
				webRouteAnnotation = annotation;
				break;
			}
		}
		
		if(webRouteAnnotation == null) {
			return Optional.empty();
		}
		
		GenericWebRouteInfo routeInfo = this.routes.get(routeElement);
		if(routeInfo != null) {
			return Optional.of(routeInfo);
		}
		else {
			return Optional.ofNullable(this.createRoute(webRouteAnnotation, null, routeElement, routeElement, (ExecutableType)routeElement.asType(), null));
		}
	}
	
	@SuppressWarnings("unchecked")
	private GenericWebRouteInfo createRoute(AnnotationMirror webRouteAnnotation, BeanQualifiedName controllerQName, ExecutableElement routeElement, ExecutableElement routeAnnotatedElement, ExecutableType routeType, Integer discriminator) {
		ReporterInfo routeReporter;
		if(this.routes.containsKey(routeElement)) {
			// We don't want to report error multiple times on the same element
			routeReporter = new NoOpReporterInfo(this.routes.get(routeElement));
		}
		else if(routeElement == routeAnnotatedElement) {
			routeReporter = this.pluginExecution.getReporter(routeElement, webRouteAnnotation);
		}
		else {
			routeReporter = this.pluginExecution.getReporter(routeElement);
		}
		
		// Get route metadata
		Set<String> paths = new HashSet<>();
		boolean matchTrailingSlash = false;
		Set<Method> methods = new HashSet<>();
		Set<String> consumes = new HashSet<>();
		Set<String> produces = new HashSet<>();
		Set<String> languages = new HashSet<>();
		annotationLoop:
		for(Entry<? extends ExecutableElement, ? extends AnnotationValue> value : this.pluginContext.getElementUtils().getElementValuesWithDefaults(webRouteAnnotation).entrySet()) {
			switch(value.getKey().getSimpleName().toString()) {
				case "value" : paths.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> v.getValue().toString()).collect(Collectors.toSet()));
					break annotationLoop;
				case "path" : paths.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> v.getValue().toString()).collect(Collectors.toSet()));
					break;
				case "matchTrailingSlash": matchTrailingSlash = (boolean)value.getValue().getValue();
					break;
				case "method" : methods.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> Method.valueOf(v.getValue().toString())).collect(Collectors.toSet()));
					break;
				case "consumes" : consumes.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> v.getValue().toString()).collect(Collectors.toSet()));
					break;
				case "produces" : produces.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> v.getValue().toString()).collect(Collectors.toSet()));
					break;
				case "language" : languages.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> v.getValue().toString()).collect(Collectors.toSet()));
					break;
			}
		}
		
		String routeName = routeElement.getSimpleName().toString();
		if(discriminator != null) {
			routeName += "_" + discriminator; 
		}
		WebRouteQualifiedName routeQName;
		if(controllerQName != null) {
			routeQName = new WebRouteQualifiedName(controllerQName, routeName);
		}
		else {
			routeQName = new WebRouteQualifiedName(routeName);
		}
		
		// Get Response body info
		TypeMirror responseBodyType = routeElement.getReturnType();
		TypeMirror erasedResponseBodyType = this.pluginContext.getTypeUtils().erasure(responseBodyType);
		
		ResponseBodyReactiveKind responseBodyReactiveKind = null;
		if(this.pluginContext.getTypeUtils().isSameType(erasedResponseBodyType, this.publisherType)) {
			responseBodyReactiveKind = ResponseBodyReactiveKind.PUBLISHER;
		}
		else if(this.pluginContext.getTypeUtils().isSameType(erasedResponseBodyType, this.monoType)) {
			responseBodyReactiveKind = ResponseBodyReactiveKind.ONE;
		}
		else if(this.pluginContext.getTypeUtils().isSameType(erasedResponseBodyType, this.fluxType)) {
			responseBodyReactiveKind = ResponseBodyReactiveKind.MANY;
		}
		else {
			responseBodyReactiveKind = ResponseBodyReactiveKind.NONE;
		}
		
		ResponseBodyKind responseBodyKind = ResponseBodyKind.ENCODED;
		if(responseBodyReactiveKind != ResponseBodyReactiveKind.NONE) {
			responseBodyType = ((DeclaredType)responseBodyType).getTypeArguments().get(0);
			if(this.pluginContext.getTypeUtils().isSameType(responseBodyType, this.byteBufType)) {
				responseBodyKind = ResponseBodyKind.RAW;
			}
			else if(this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(responseBodyType), this.sseRawEventType)) {
				responseBodyKind = ResponseBodyKind.SSE_RAW;
				responseBodyType = ((DeclaredType)responseBodyType).getTypeArguments().get(0);
			}
			else if(this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(responseBodyType), this.SseEncoderEventType)) {
				responseBodyKind = ResponseBodyKind.SSE_ENCODED;
				responseBodyType = ((DeclaredType)responseBodyType).getTypeArguments().get(0);
			}
		}
		else if(responseBodyType.getKind() == TypeKind.VOID) {
			responseBodyKind = ResponseBodyKind.EMPTY;
		}
		else if(this.pluginContext.getTypeUtils().isSameType(responseBodyType, this.byteBufType)) {
			responseBodyKind = ResponseBodyKind.RAW;
		}
		else if(this.pluginContext.getTypeUtils().isSameType(responseBodyType, this.resourceType)) {
			responseBodyKind = ResponseBodyKind.RESOURCE;
		}
		
		WebResponseBodyInfo responseBodyInfo  = new GenericWebResponseBodyInfo(responseBodyType, responseBodyKind, responseBodyReactiveKind);
		
		// Get route parameters
		List<AbstractWebParameterInfo> parameters = new ArrayList<>();
		boolean hasFormParameters = false;
		List<WebParameterInfo> bodyParameters = new ArrayList<>();
		WebSseEventFactoryParameterInfo sseFactoryParameter = null;
		List<? extends VariableElement> routeParameters = routeElement.getParameters();
		List<? extends VariableElement> routeAnnotatedParameters = routeAnnotatedElement.getParameters();
		List<? extends TypeMirror> routeParameterTypes = routeType.getParameterTypes();
		for(int i=0;i<routeParameters.size();i++) {
			VariableElement routeParameterElement = routeParameters.get(i);
			VariableElement routeAnnotatedParameterElement = routeAnnotatedParameters.get(i);
			TypeMirror routeParameterType = routeParameterTypes.get(i);
			AbstractWebParameterInfo parameterInfo = this.parameterFactory.createParameter(routeQName, routeParameterElement, routeAnnotatedParameterElement, routeParameterType);
			if(parameterInfo instanceof WebRequestBodyParameterInfo) {
				bodyParameters.add(parameterInfo);
			}
			else if(parameterInfo instanceof WebFormParameterInfo) {
				hasFormParameters = true;
			}
			else if(parameterInfo instanceof WebSseEventFactoryParameterInfo) {
				sseFactoryParameter = (WebSseEventFactoryParameterInfo) parameterInfo;
			}
			parameters.add(parameterInfo);
		}

		if(hasFormParameters && bodyParameters.size() > 0) {
			routeReporter.error("Can't mix Body and Form parameters");
		}
		if(bodyParameters.size() > 1) {
			routeReporter.error("Multiple Body parameters");
		}
		if(sseFactoryParameter != null) {
			if(responseBodyInfo.getBodyKind() == ResponseBodyKind.SSE_RAW) {
				if(sseFactoryParameter.getEventFactoryKind() != SseEventFactoryKind.RAW) {
					routeReporter.error("SSE event factory " + sseFactoryParameter.getType() + " doesn't match sse response " + responseBodyInfo.getType());
				}
			}
			else if(responseBodyInfo.getBodyKind() == ResponseBodyKind.SSE_ENCODED) {
				if(sseFactoryParameter.getEventFactoryKind() != SseEventFactoryKind.ENCODER || !this.pluginContext.getTypeUtils().isSameType(responseBodyInfo.getType(), sseFactoryParameter.getType())) {
					routeReporter.error("SSE event factory " + sseFactoryParameter.getType() + " doesn't match sse response " + responseBodyInfo.getType());
				}
			}
			else {
				routeReporter.error("Invalid SSE route declaration which must return a publisher of SSE events and define a corresponding SSE event factory parameter");
			}
		}
		else if(responseBodyInfo.getBodyKind() == ResponseBodyKind.SSE_RAW || responseBodyInfo.getBodyKind() == ResponseBodyKind.SSE_ENCODED) {
			routeReporter.error("Invalid SSE route declaration which must return a publisher of SSE events and define a corresponding SSE event factory parameter");
		}

		GenericWebRouteInfo routeInfo = new GenericWebRouteInfo(routeElement, routeQName, routeReporter, paths, matchTrailingSlash, methods, consumes, produces, languages, parameters, responseBodyInfo);
		this.routes.putIfAbsent(routeElement, routeInfo);
		return routeInfo;
	}
	
	@SuppressWarnings("unchecked")
	public List<ProvidedWebRouteInfo> compileRouterRoutes(BeanInfo bean) {
		TypeElement beanElement = (TypeElement) this.pluginContext.getTypeUtils().asElement(bean.getType());
		
		AnnotationMirror webRoutesAnnotation = this.pluginContext.getElementUtils().getAllAnnotationMirrors(beanElement).stream()
			.filter(annotation -> this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.webRoutesAnnotationType))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("bean " + bean + " is not annotated with " + WebRoutes.class));
		
		if(webRoutesAnnotation.getElementValues().isEmpty()) {
			return List.of();
		}
		
		final AtomicInteger routeIndex = new AtomicInteger();
		return ((Collection<? extends AnnotationValue>)webRoutesAnnotation.getElementValues().values().iterator().next().getValue()).stream()
			.map(value -> (AnnotationMirror)value.getValue())
			.map(webRouteAnnotation -> {
				Set<String> paths = new HashSet<>();
				boolean matchTrailingSlash = false;
				Set<Method> methods = new HashSet<>();
				Set<String> consumes = new HashSet<>();
				Set<String> produces = new HashSet<>();
				Set<String> languages = new HashSet<>();
				for(Entry<? extends ExecutableElement, ? extends AnnotationValue> value : this.pluginContext.getElementUtils().getElementValuesWithDefaults(webRouteAnnotation).entrySet()) {
					switch(value.getKey().getSimpleName().toString()) {
						case "path" : paths.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> v.getValue().toString()).collect(Collectors.toSet()));
							break;
						case "matchTrailingSlash": matchTrailingSlash = (boolean)value.getValue().getValue();
							break;
						case "method" : methods.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> Method.valueOf(v.getValue().toString())).collect(Collectors.toSet()));
							break;
						case "consumes" : consumes.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> v.getValue().toString()).collect(Collectors.toSet()));
							break;
						case "produces" : produces.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> v.getValue().toString()).collect(Collectors.toSet()));
							break;
						case "language" : languages.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> v.getValue().toString()).collect(Collectors.toSet()));
							break;
					}
				}
				WebRouteQualifiedName routeQName = new WebRouteQualifiedName(bean.getQualifiedName(), "route_" + routeIndex.getAndIncrement());
				return new ProvidedWebRouteInfo(routeQName, bean, paths, matchTrailingSlash, methods, consumes, produces, languages);
			})
			.collect(Collectors.toList());
	}
	
	public List<GenericWebRouteInfo> compileControllerRoutes(BeanInfo bean) {
		TypeElement beanElement = (TypeElement) this.pluginContext.getTypeUtils().asElement(bean.getType());
		
		this.pluginContext.getElementUtils().getAllAnnotationMirrors(beanElement).stream()
			.filter(annotation -> this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.webControllerAnnotationType))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("bean " + bean + " is not annotated with " + WebController.class));
		
		Map<String, List<GenericWebRouteInfo>> routesByMethodName = new HashMap<>();
		for(ExecutableElement routeElement : ElementFilter.methodsIn(this.pluginContext.getElementUtils().getAllMembers(beanElement))) {
			this.getWebRouteAnnotatedElement(routeElement, beanElement)
				.ifPresent(routeAnnotatedElement -> {
					AnnotationMirror webRouteAnnotation = null;
					for(AnnotationMirror annotation : this.pluginContext.getElementUtils().getAllAnnotationMirrors(routeAnnotatedElement)) {
						if(this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.webRouteAnnotationType)) {
							webRouteAnnotation = annotation;
							break;
						}
					}
					
					ExecutableType routeElementType = (ExecutableType)this.pluginContext.getTypeUtils().asMemberOf((DeclaredType)bean.getType(), routeElement);
					
					// We have to create the route no matter what since annotations and/or types
					// might be different from what the compiler found when analyzing WebRoute
					// annotated elements.
					// eg. we can imagine an interface A external to the module exposing a WebRoute
					// annotated method, and a class B in the module overriding that particular
					// interface and implementing that particular method overriding the WebRoute
					// annotation. If a class C extends B and implements A, then we'll found the
					// route coming from B in the list of routes but we'll have to consider WebRoute
					// annotation from A and therefore we have to recreate the route for that
					// particular configuration but we must not report errors already reported on B.
					// That being said, overriding WebRoute annotation is a bad idea, it would be
					// interesting to see how JAX-RS and Spring handle this.
					
					String routeMethodName = routeElement.getSimpleName().toString();
					List<GenericWebRouteInfo> currentRoutes = routesByMethodName.get(routeMethodName);
					if(currentRoutes == null) {
						currentRoutes = new LinkedList<>();
						routesByMethodName.put(routeMethodName, currentRoutes);
					}
					currentRoutes.add(this.createRoute(webRouteAnnotation, bean.getQualifiedName(), routeElement, routeAnnotatedElement, routeElementType, currentRoutes.isEmpty() ? null : currentRoutes.size()));
				});
		}
		return routesByMethodName.values().stream().flatMap(List::stream).collect(Collectors.toList());
	}
	
	private Optional<ExecutableElement> getWebRouteAnnotatedElement(ExecutableElement executableElement, TypeElement typeElement) {
		return this.typeHierarchyExtractor.extractTypeHierarchy(typeElement).stream()
			.map(element -> ElementFilter.methodsIn(element.getEnclosedElements()).stream()
				.filter(methodElement -> methodElement.equals(executableElement) || this.pluginContext.getElementUtils().overrides(executableElement, methodElement, typeElement))
				.findFirst()
			)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.filter(overriddenElement -> overriddenElement.getAnnotationMirrors().stream().anyMatch(annotation -> this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.webRouteAnnotationType)))
			.findFirst();
	}
}

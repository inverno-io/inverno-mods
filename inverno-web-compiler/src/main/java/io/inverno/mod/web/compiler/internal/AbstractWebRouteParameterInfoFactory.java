/*
 * Copyright 2024 Jeremy Kuhn
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

import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.core.compiler.spi.plugin.PluginContext;
import io.inverno.core.compiler.spi.plugin.PluginExecution;
import io.inverno.mod.web.base.annotation.CookieParam;
import io.inverno.mod.web.base.annotation.HeaderParam;
import io.inverno.mod.web.base.annotation.PathParam;
import io.inverno.mod.web.base.annotation.QueryParam;
import io.inverno.mod.web.compiler.spi.WebParameterQualifiedName;
import io.inverno.mod.web.compiler.spi.WebRouteQualifiedName;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * Base Web route parameter info factory.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public abstract class AbstractWebRouteParameterInfoFactory {

	protected final PluginContext pluginContext;
	protected final PluginExecution pluginExecution;
	protected final Map<VariableElement, AbstractWebParameterInfo> processedParameterElements;

	/* Web annotations */
	private final TypeMirror cookieParameterAnnotationType;
	private final TypeMirror headerParameterAnnotationType;
	private final TypeMirror pathParameterAnnotationType;
	private final TypeMirror queryParameterAnnotationType;

	/* Types */
	protected final TypeMirror optionalType;

	/**
	 * <p>
	 * Creates a Web parameter info factory.
	 * </p>
	 *
	 * @param pluginContext   the Web compiler plugin context
	 * @param pluginExecution the Web compiler plugin execution
	 */
	public AbstractWebRouteParameterInfoFactory(PluginContext pluginContext, PluginExecution pluginExecution) {
		this.pluginContext = Objects.requireNonNull(pluginContext);
		this.pluginExecution = Objects.requireNonNull(pluginExecution);
		this.processedParameterElements = new HashMap<>();

		this.cookieParameterAnnotationType = this.pluginContext.getElementUtils().getTypeElement(CookieParam.class.getCanonicalName()).asType();
		this.headerParameterAnnotationType = this.pluginContext.getElementUtils().getTypeElement(HeaderParam.class.getCanonicalName()).asType();
		this.pathParameterAnnotationType = this.pluginContext.getElementUtils().getTypeElement(PathParam.class.getCanonicalName()).asType();
		this.queryParameterAnnotationType = this.pluginContext.getElementUtils().getTypeElement(QueryParam.class.getCanonicalName()).asType();

		this.optionalType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Optional.class.getCanonicalName()).asType());
	}

	public final AbstractWebParameterInfo createParameter(WebRouteQualifiedName routeQName, VariableElement parameterElement, VariableElement annotatedParameterElement, TypeMirror parameterType, Set<String> consumes, Set<String> produces) {
		AbstractWebParameterInfo result = null;

		ReporterInfo parameterReporter;
		if(this.processedParameterElements.containsKey(parameterElement)) {
			parameterReporter = new NoOpReporterInfo(this.processedParameterElements.get(parameterElement));
		}
		else {
			parameterReporter = this.pluginExecution.getReporter(parameterElement);
		}

		result = this.createContextualParameter(parameterReporter, this.getParameterQualifiedName(routeQName, annotatedParameterElement, null), parameterElement, parameterType);

		if(result == null) {
			boolean required = !this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(parameterType), this.optionalType);
			if(!required) {
				// For optional parameter consider the Optional<> argument
				parameterType = ((DeclaredType)parameterType).getTypeArguments().getFirst();
			}

			// A Web parameter can't be annotated with multiple Web parameter annotations
			for(AnnotationMirror annotation : annotatedParameterElement.getAnnotationMirrors()) {
				AbstractWebParameterInfo currentParameterInfo = null;
				if(this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.cookieParameterAnnotationType)) {
					currentParameterInfo = this.createCookieParameter(parameterReporter, this.getParameterQualifiedName(routeQName, annotatedParameterElement, annotation), parameterElement, parameterType, required);
				}
				else if(this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.headerParameterAnnotationType)) {
					currentParameterInfo = this.createHeaderParameter(parameterReporter, this.getParameterQualifiedName(routeQName, annotatedParameterElement, annotation), parameterElement, parameterType, required);
				}
				else if(this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.pathParameterAnnotationType)) {
					currentParameterInfo = this.createPathParameter(parameterReporter, this.getParameterQualifiedName(routeQName, annotatedParameterElement, annotation), parameterElement, parameterType, required);
				}
				else if(this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.queryParameterAnnotationType)) {
					currentParameterInfo = this.createQueryParameter(parameterReporter, this.getParameterQualifiedName(routeQName, annotatedParameterElement, annotation), parameterElement, parameterType, required);
				}
				else {
					currentParameterInfo = this.createParameter(parameterReporter, this.getParameterQualifiedName(routeQName, annotatedParameterElement, annotation), parameterElement, parameterType, annotation, consumes, produces, required);
				}

				if(currentParameterInfo != null) {
					if(result != null) {
						parameterReporter.error("Too many Web parameter annotations specified, only one is allowed");
						break;
					}
					result = currentParameterInfo;
				}
			}
		}

		if(result == null) {
			if(!parameterReporter.hasError()) {
				parameterReporter.error("Invalid parameter which is neither a Web parameter, nor a valid contextual parameter");
			}
			result = new InvalidWebParameterInfo(this.getParameterQualifiedName(routeQName, annotatedParameterElement, null), parameterReporter, parameterElement, false);
		}
		this.processedParameterElements.putIfAbsent(parameterElement, result);
		return result;
	}

	protected abstract AbstractWebParameterInfo createContextualParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType);

	protected abstract AbstractWebParameterInfo createParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType, AnnotationMirror annotation, Set<String> consumes, Set<String> produces, boolean required);

	/**
	 * <p>
	 * Returns the qualified name for the specified route, parameter element and parameter annotation.
	 * </p>
	 *
	 * <p>
	 * The resulting qualified name uses the name in the parameter annotation when specified or the variable name to identify the parameter.
	 * </p>
	 *
	 * @param routeQName                the qualified name of the route for which the parameter is defined
	 * @param annotatedParameterElement the variable element of the parameter in the method actually annotated with {@code WebRoute @WebRoute} annotation
	 * @param parameterAnnotation       the parameter annotation specified on the variable element
	 *
	 * @return a new Web parameter qualified name
	 */
	protected final WebParameterQualifiedName getParameterQualifiedName(WebRouteQualifiedName routeQName, VariableElement annotatedParameterElement, AnnotationMirror parameterAnnotation) {
		String name = null;
		if(parameterAnnotation != null) {
			for(Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> value : parameterAnnotation.getElementValues().entrySet()) {
				if(value.getKey().getSimpleName().toString().equals("name")) {
					name = (String) value.getValue().getValue();
				}
			}
		}
		return new WebParameterQualifiedName(routeQName, StringUtils.isNotBlank(name) ? name : annotatedParameterElement.getSimpleName().toString());
	}

	/**
	 * <p>
	 * Creates a cookie parameter info.
	 * </p>
	 *
	 * @param reporter         the parameter reporter
	 * @param parameterQName   the parameter qualified name
	 * @param parameterElement the parameter element
	 * @param parameterType    the parameter type
	 * @param required         true to indicate a required parameter, false
	 *                         otherwise
	 *
	 * @return a Web cookie parameter info
	 */
	private GenericWebCookieParameterInfo createCookieParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType, boolean required) {
		return new GenericWebCookieParameterInfo(parameterQName, reporter, parameterElement, parameterType, required);
	}

	/**
	 * <p>
	 * Creates a header parameter info.
	 * </p>
	 *
	 * @param reporter         the parameter reporter
	 * @param parameterQName   the parameter qualified name
	 * @param parameterElement the parameter element
	 * @param parameterType    the parameter type
	 * @param required         true to indicate a required parameter, false otherwise
	 *
	 * @return a Web header parameter info
	 */
	private GenericWebHeaderParameterInfo createHeaderParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType, boolean required) {
		return new GenericWebHeaderParameterInfo(parameterQName, reporter, parameterElement, parameterType, required);
	}

	/**
	 * <p>
	 * Creates a path parameter info.
	 * </p>
	 *
	 * @param reporter         the parameter reporter
	 * @param parameterQName   the parameter qualified name
	 * @param parameterElement the parameter element
	 * @param parameterType    the parameter type
	 * @param required         true to indicate a required parameter, false otherwise
	 *
	 * @return a Web path parameter info
	 */
	private GenericWebPathParameterInfo createPathParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType, boolean required) {
		return new GenericWebPathParameterInfo(parameterQName, reporter, parameterElement, parameterType, required);
	}

	/**
	 * <p>
	 * Creates a query parameter info.
	 * </p>
	 *
	 * @param reporter         the parameter reporter
	 * @param parameterQName   the parameter qualified name
	 * @param parameterElement the parameter element
	 * @param parameterType    the parameter type
	 * @param required         true to indicate a required parameter, false otherwise
	 *
	 * @return a Web query parameter info
	 */
	private GenericWebQueryParameterInfo createQueryParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType, boolean required) {
		return new GenericWebQueryParameterInfo(parameterQName, reporter, parameterElement, parameterType, required);
	}
}

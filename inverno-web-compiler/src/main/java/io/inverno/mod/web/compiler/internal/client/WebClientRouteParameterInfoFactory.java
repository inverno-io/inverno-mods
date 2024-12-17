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
package io.inverno.mod.web.compiler.internal.client;

import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.core.compiler.spi.plugin.PluginContext;
import io.inverno.core.compiler.spi.plugin.PluginExecution;
import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.web.base.annotation.Body;
import io.inverno.mod.web.base.annotation.FormParam;
import io.inverno.mod.web.client.annotation.PartParam;
import io.inverno.mod.web.compiler.internal.AbstractWebParameterInfo;
import io.inverno.mod.web.compiler.internal.GenericWebFormParameterInfo;
import io.inverno.mod.web.compiler.internal.GenericWebRequestBodyParameterInfo;
import io.inverno.mod.web.compiler.spi.RequestBodyKind;
import io.inverno.mod.web.compiler.spi.RequestBodyReactiveKind;
import io.inverno.mod.web.compiler.spi.WebParameterQualifiedName;
import io.inverno.mod.web.compiler.spi.client.WebClientPartParameterInfo;
import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Factory used to create Web client route parameter info.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class WebClientRouteParameterInfoFactory extends AbstractWebClientRouteParameterInfoFactory {

	/* Web annotations */
	private final TypeMirror formParameterAnnotationType;
	private final TypeMirror partParameterAnnotationType;
	private final TypeMirror bodyAnnotationType;

	/* Types */
	private final TypeMirror monoType;
	private final TypeMirror fluxType;
	private final TypeMirror voidType;
	private final TypeMirror publisherType;
	private final TypeMirror byteBufType;
	private final TypeMirror charSequenceType;
	private final TypeMirror resourceType;

	/**
	 * <p>
	 * Creates a Web client route parameter info factory.
	 * </p>
	 *
	 * @param pluginContext   the Web compiler plugin context
	 * @param pluginExecution the Web compiler plugin execution
	 */
	public WebClientRouteParameterInfoFactory(PluginContext pluginContext, PluginExecution pluginExecution) {
		super(pluginContext, pluginExecution);

		this.formParameterAnnotationType = this.pluginContext.getElementUtils().getTypeElement(FormParam.class.getCanonicalName()).asType();
		this.partParameterAnnotationType = this.pluginContext.getElementUtils().getTypeElement(PartParam.class.getCanonicalName()).asType();
		this.bodyAnnotationType = this.pluginContext.getElementUtils().getTypeElement(Body.class.getCanonicalName()).asType();

		this.monoType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Mono.class.getCanonicalName()).asType());
		this.fluxType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Flux.class.getCanonicalName()).asType());
		this.voidType = this.pluginContext.getElementUtils().getTypeElement(Void.class.getCanonicalName()).asType();
		this.publisherType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Publisher.class.getCanonicalName()).asType());
		this.byteBufType = this.pluginContext.getElementUtils().getTypeElement(ByteBuf.class.getCanonicalName()).asType();
		this.charSequenceType = this.pluginContext.getElementUtils().getTypeElement(CharSequence.class.getCanonicalName()).asType();
		this.resourceType = this.pluginContext.getElementUtils().getTypeElement(Resource.class.getCanonicalName()).asType();
	}

	@Override
	protected AbstractWebParameterInfo createParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType, AnnotationMirror annotation, Set<String> consumes, Set<String> produces, boolean required) {
		if(this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.formParameterAnnotationType)) {
			return this.createFormParameter(reporter, parameterQName, parameterElement, parameterType, required);
		}
		else if(this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.partParameterAnnotationType)) {
			return this.createPartParameter(reporter, parameterQName, parameterElement, parameterType, annotation);
		}
		else if(this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.bodyAnnotationType)) {
			if (!required) {
				reporter.error("Request body parameter can't be optional");
			}
			else {
				return this.createRequestBodyParameter(reporter, parameterQName, parameterElement, parameterType, consumes, produces);
			}
		}
		return null;
	}

	/**
	 * <p>
	 * Creates a form parameter info.
	 * </p>
	 *
	 * @param reporter         the parameter reporter
	 * @param parameterQName   the parameter qualified name
	 * @param parameterElement the parameter element
	 * @param parameterType    the parameter type
	 * @param required         true to indicate a required parameter, false
	 *                         otherwise
	 *
	 * @return a Web form parameter info
	 */
	private GenericWebFormParameterInfo createFormParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType, boolean required) {
		return new GenericWebFormParameterInfo(parameterQName, reporter, parameterElement, parameterType, required);
	}

	/**
	 * <p>
	 * Creates a part parameter info.
	 * </p>
	 *
	 * @param reporter         the parameter reporter
	 * @param parameterQName   the parameter qualified name
	 * @param parameterElement the parameter element
	 * @param parameterType    the parameter type
	 *
	 * @return a Web part parameter info
	 */
	private GenericWebClientPartParameterInfo createPartParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType, AnnotationMirror annotation) {
		String filename = null;
		String contentType = null;
		for(Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> value : annotation.getElementValues().entrySet()) {
			switch(value.getKey().getSimpleName().toString()) {
				case "filename": filename = (String) value.getValue().getValue();
					break;
				case "contentType" : contentType = (String) value.getValue().getValue();
					break;
			}
		}

		TypeMirror partBodyType = parameterType;
		TypeMirror erasedPartBodyType = this.pluginContext.getTypeUtils().erasure(partBodyType);

		WebClientPartParameterInfo.PartBodyReactiveKind partBodyReactiveKind;
		if(this.pluginContext.getTypeUtils().isSameType(erasedPartBodyType, this.publisherType)) {
			partBodyReactiveKind = WebClientPartParameterInfo.PartBodyReactiveKind.PUBLISHER;
		}
		else if(this.pluginContext.getTypeUtils().isSameType(erasedPartBodyType, this.monoType)) {
			partBodyReactiveKind = WebClientPartParameterInfo.PartBodyReactiveKind.ONE;
		}
		else if(this.pluginContext.getTypeUtils().isSameType(erasedPartBodyType, this.fluxType)) {
			partBodyReactiveKind = WebClientPartParameterInfo.PartBodyReactiveKind.MANY;
		}
		else {
			partBodyReactiveKind = WebClientPartParameterInfo.PartBodyReactiveKind.NONE;
		}

		WebClientPartParameterInfo.PartBodyKind partBodyKind = WebClientPartParameterInfo.PartBodyKind.ENCODED;
		if(partBodyReactiveKind != WebClientPartParameterInfo.PartBodyReactiveKind.NONE) {
			partBodyType = ((DeclaredType)partBodyType).getTypeArguments().getFirst();
			if(this.pluginContext.getTypeUtils().isSameType(partBodyType, this.byteBufType)) {
				partBodyKind = WebClientPartParameterInfo.PartBodyKind.RAW;
			}
			else if(this.pluginContext.getTypeUtils().isAssignable(partBodyType, this.charSequenceType)) {
				if(contentType == null) {
					partBodyKind = WebClientPartParameterInfo.PartBodyKind.CHARSEQUENCE;
				}
			}
		}
		else if(this.pluginContext.getTypeUtils().isSameType(partBodyType, this.byteBufType)) {
			partBodyKind = WebClientPartParameterInfo.PartBodyKind.RAW;
		}
		else if(this.pluginContext.getTypeUtils().isAssignable(partBodyType, this.charSequenceType)) {
			if(contentType == null) {
				partBodyKind = WebClientPartParameterInfo.PartBodyKind.CHARSEQUENCE;
			}
		}
		else if(this.pluginContext.getTypeUtils().isSameType(partBodyType, this.resourceType)) {
			partBodyKind = WebClientPartParameterInfo.PartBodyKind.RESOURCE;
		}

		return new GenericWebClientPartParameterInfo(parameterQName, reporter, parameterElement, partBodyType, filename, contentType, partBodyKind, partBodyReactiveKind);
	}

	/**
	 * <p>
	 * Creates a Web request body parameter info.
	 * </p>
	 *
	 * @param reporter         the parameter reporter
	 * @param parameterQName   the parameter qualified name
	 * @param parameterElement the parameter element
	 * @param parameterType    the parameter type
	 *
	 * @return a request body parameter info
	 */
	private GenericWebRequestBodyParameterInfo createRequestBodyParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType, Set<String> consumes, Set<String> produces) {
		TypeMirror requestBodyType = parameterType;
		TypeMirror erasedRequestBodyType = this.pluginContext.getTypeUtils().erasure(requestBodyType);

		RequestBodyReactiveKind requestBodyReactiveKind;
		if(this.pluginContext.getTypeUtils().isSameType(erasedRequestBodyType, this.publisherType)) {
			requestBodyReactiveKind = RequestBodyReactiveKind.PUBLISHER;
		}
		else if(this.pluginContext.getTypeUtils().isSameType(erasedRequestBodyType, this.monoType)) {
			requestBodyReactiveKind = RequestBodyReactiveKind.ONE;
		}
		else if(this.pluginContext.getTypeUtils().isSameType(erasedRequestBodyType, this.fluxType)) {
			requestBodyReactiveKind = RequestBodyReactiveKind.MANY;
		}
		else {
			requestBodyReactiveKind = RequestBodyReactiveKind.NONE;
		}

		RequestBodyKind requestBodyKind = RequestBodyKind.ENCODED;
		if(requestBodyReactiveKind != RequestBodyReactiveKind.NONE) {
			requestBodyType = ((DeclaredType)requestBodyType).getTypeArguments().getFirst();
			if(this.pluginContext.getTypeUtils().isSameType(requestBodyType, this.byteBufType)) {
				requestBodyKind = RequestBodyKind.RAW;
			}
			else if(this.pluginContext.getTypeUtils().isAssignable(requestBodyType, this.charSequenceType)) {
				if(produces.isEmpty()) {
					requestBodyKind = RequestBodyKind.CHARSEQUENCE;
				}
			}
			else if(this.pluginContext.getTypeUtils().isSameType(requestBodyType, this.voidType)) {
				reporter.error("Request body publisher can't have type argument void");
			}
		}
		else if(this.pluginContext.getTypeUtils().isSameType(requestBodyType, this.byteBufType)) {
			requestBodyKind = RequestBodyKind.RAW;
		}
		else if(this.pluginContext.getTypeUtils().isAssignable(requestBodyType, this.charSequenceType)) {
			if(produces.isEmpty()) {
				requestBodyKind = RequestBodyKind.CHARSEQUENCE;
			}
		}
		else if(this.pluginContext.getTypeUtils().isSameType(requestBodyType, this.resourceType)) {
			requestBodyKind = RequestBodyKind.RESOURCE;
		}
		return new GenericWebRequestBodyParameterInfo(parameterQName, reporter, parameterElement, requestBodyType, requestBodyKind, requestBodyReactiveKind);
	}
}

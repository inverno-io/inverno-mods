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

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.ws.WebSocketMessage;
import io.inverno.mod.web.compiler.spi.RequestBodyKind;
import io.inverno.mod.web.compiler.spi.ResponseBodyReactiveKind;
import io.inverno.mod.web.compiler.spi.WebCookieParameterInfo;
import io.inverno.mod.web.compiler.spi.WebExchangeParameterInfo;
import io.inverno.mod.web.compiler.spi.WebFormParameterInfo;
import io.inverno.mod.web.compiler.spi.WebHeaderParameterInfo;
import io.inverno.mod.web.compiler.spi.WebParameterInfo;
import io.inverno.mod.web.compiler.spi.WebPathParameterInfo;
import io.inverno.mod.web.compiler.spi.WebQueryParameterInfo;
import io.inverno.mod.web.compiler.spi.WebRequestBodyParameterInfo;
import io.inverno.mod.web.compiler.spi.WebSocketBoundPublisherInfo;
import io.inverno.mod.web.compiler.spi.WebSocketExchangeParameterInfo;
import io.inverno.mod.web.compiler.spi.WebSocketOutboundParameterInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientExchangeReturnInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientModuleInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientModuleInfoVisitor;
import io.inverno.mod.web.compiler.spi.client.WebClientPartParameterInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientResponseBodyInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientResponseReturnInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientRouteReturnInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientRouteInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientRouteInterceptorConfigurerInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientSocketInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientStubInfo;
import io.inverno.mod.web.compiler.spi.client.WebSocketClientExchangeReturnInfo;
import io.inverno.mod.web.compiler.spi.client.WebSocketClientInboundPublisherInfo;
import io.inverno.mod.web.compiler.spi.client.WebSocketClientInboundReturnInfo;
import io.inverno.mod.web.compiler.spi.client.WebSocketClientOutboundPublisherParameterInfo;
import io.inverno.mod.web.compiler.spi.client.WebSocketClientRouteInfo;
import io.inverno.mod.web.compiler.spi.client.WebSocketClientRouteReturnInfo;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;

/**
 * <p>
 * Web client generator used to generate boot and <i>cascading</i> Web client classes.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class WebClientClassGenerator implements WebClientModuleInfoVisitor<StringBuilder, WebClientClassGenerationContext> {

	private static final String VARIABLE_TYPES = "_TYPES";

	private static final String VARIABLE_EXCHANGE = "_exchange";
	private static final String VARIABLE_INBOUND = "_inbound";
	private static final String VARIABLE_HEADERS = "_headers";
	private static final String VARIABLE_COOKIES = "_cookies";
	private static final String VARIABLE_FACTORY = "_factory";
	private static final String VARIABLE_DATA = "_data";
	private static final String VARIABLE_PART = "_part";
	private static final String VARIABLE_RESPONSE = "_response";
	private static final String VARIABLE_ACC = "_acc";
	private static final String VARIABLE_CHUNK = "_chunk";

	@Override
	public StringBuilder visit(WebClientModuleInfo clientModuleInfo, WebClientClassGenerationContext context) {
		WebClientClassGenerationContext moduleContext = context.withWebClientModuleInfo(clientModuleInfo);
		String webClientClassName = clientModuleInfo.getQualifiedName().getClassName();
		String webClientPackageName = webClientClassName.lastIndexOf(".") != -1 ? webClientClassName.substring(0, webClientClassName.lastIndexOf(".")) : "";
		webClientClassName = webClientClassName.substring(webClientPackageName.length() + 1);
		if(moduleContext.getMode() == WebClientClassGenerationContext.GenerationMode.BOOT_CLIENT_CLASS) {
			moduleContext.addImport(webClientClassName, webClientPackageName + "." + webClientClassName);
			moduleContext.addImport("Context", webClientPackageName + "." + webClientClassName + ".Context");
			moduleContext.addImport("ContextImpl", webClientPackageName + "." + webClientClassName + ".ContextImpl");

			StringBuilder interceptorsConfigurersDecl = new StringBuilder(moduleContext.getTypeName(moduleContext.getListType())).append("<").append(moduleContext.getTypeName(moduleContext.getWebRouteInterceptorType())).append(".Configurer<? super ").append(webClientClassName).append(".Context>> interceptorsConfigurers");

			StringBuilder webClientBootField = new StringBuilder(moduleContext.indent(1)).append("private final ").append(moduleContext.getTypeName(moduleContext.getWebClientType())).append(".Boot webClientBoot;");
			StringBuilder webClientField = new StringBuilder(moduleContext.indent(1)).append("private ").append(moduleContext.getTypeName(moduleContext.getWebClientType())).append("<").append(webClientClassName).append(".Context> webClient;");
			StringBuilder interceptorsConfigurersField = new StringBuilder(moduleContext.indent(1)).append("private ").append(interceptorsConfigurersDecl).append(";");

			StringBuilder interceptorsConfigurersSetter = new StringBuilder(moduleContext.indent(1)).append("public void setInterceptorsConfigurers(").append(interceptorsConfigurersDecl).append(") {").append(System.lineSeparator());
			interceptorsConfigurersSetter.append(moduleContext.indent(2)).append("this.interceptorsConfigurers = interceptorsConfigurers;").append(System.lineSeparator());
			interceptorsConfigurersSetter.append(moduleContext.indent(1)).append("}");

			StringBuilder webClient_constructor = new StringBuilder(moduleContext.indent(1)).append("public ").append(webClientClassName).append("(");
			webClient_constructor.append(moduleContext.getTypeName(moduleContext.getWebClientType())).append(".Boot webClientBoot");
			webClient_constructor.append(") {").append(System.lineSeparator());
			webClient_constructor.append(moduleContext.indent(2)).append("this.webClientBoot = webClientBoot;").append(System.lineSeparator());
			webClient_constructor.append(moduleContext.indent(1)).append("}");

			StringBuilder webClient_get = new StringBuilder(moduleContext.indent(1)).append("@Override").append(System.lineSeparator());
			webClient_get.append(moduleContext.indent(1)).append("public ").append(moduleContext.getTypeName(moduleContext.getWebClientType())).append("<").append(webClientClassName).append(".Context> get() {").append(System.lineSeparator());
			webClient_get.append(moduleContext.indent(2)).append("return this.webClient;").append(System.lineSeparator());
			webClient_get.append(moduleContext.indent(1)).append("}");

			StringBuilder webClient_init = new StringBuilder(moduleContext.indent(1)).append("@").append(moduleContext.getTypeName(moduleContext.getInitType())).append(System.lineSeparator());
			webClient_init.append(moduleContext.indent(1)).append("public void init() {").append(System.lineSeparator());
			webClient_init.append(moduleContext.indent(2)).append("this.webClient = this.webClientBoot.webClient(").append(webClientClassName).append(".ContextImpl::new);").append(System.lineSeparator());
			webClient_init.append(moduleContext.indent(2)).append("this.webClient = this.webClient").append(System.lineSeparator());
			webClient_init.append(moduleContext.indent(3)).append(".configureInterceptors(this.interceptorsConfigurers);").append(System.lineSeparator());
			webClient_init.append(moduleContext.indent(1)).append("}");

			StringBuilder client_context = this.visit(clientModuleInfo, moduleContext.withIndentDepth(1).withMode(WebClientClassGenerationContext.GenerationMode.CLIENT_CONTEXT));

			StringBuilder client_context_impl = this.visit(clientModuleInfo, moduleContext.withIndentDepth(1).withMode(WebClientClassGenerationContext.GenerationMode.CLIENT_CONTEXT_IMPL));

			WebClientClassGenerationContext clientStubsContext = moduleContext.withIndentDepth(1).withMode(WebClientClassGenerationContext.GenerationMode.CLIENT_STUB_CLASS);
			StringBuilder client_stubs = Arrays.stream(clientModuleInfo.getClientStubs()).map(clientStubInfo -> this.visit(clientStubInfo, clientStubsContext)).collect(moduleContext.joining(System.lineSeparator()));

			StringBuilder webClient_class = new StringBuilder();

			webClient_class.append("@").append(moduleContext.getTypeName(moduleContext.getWrapperAnnotationType())).append(" @").append(moduleContext.getTypeName(moduleContext.getBeanAnnotationType())).append("( name = \"").append(clientModuleInfo.getQualifiedName().getBeanName()).append("\", visibility = ").append(moduleContext.getTypeName(moduleContext.getBeanAnnotationType())).append(".Visibility.PRIVATE )").append(System.lineSeparator());
			webClient_class.append("@").append(moduleContext.getTypeName(moduleContext.getGeneratedType())).append("(value=\"").append(WebClientCompilerPlugin.class.getCanonicalName()).append("\", date = \"").append(ZonedDateTime.now()).append("\")").append(System.lineSeparator());
			webClient_class.append("public final class ").append(webClientClassName).append(" implements ").append(moduleContext.getTypeName(moduleContext.getSupplierType())).append("<").append(moduleContext.getTypeName(moduleContext.getWebClientType())).append("<").append(webClientClassName).append(".Context>> {").append(System.lineSeparator()).append(System.lineSeparator());

			webClient_class.append(webClientBootField).append(System.lineSeparator());
			webClient_class.append(webClientField).append(System.lineSeparator()).append(System.lineSeparator());

			webClient_class.append(interceptorsConfigurersField).append(System.lineSeparator()).append(System.lineSeparator());

			webClient_class.append(webClient_constructor).append(System.lineSeparator()).append(System.lineSeparator());

			webClient_class.append(webClient_init).append(System.lineSeparator()).append(System.lineSeparator());

			webClient_class.append(webClient_get).append(System.lineSeparator()).append(System.lineSeparator());

			webClient_class.append(interceptorsConfigurersSetter).append(System.lineSeparator()).append(System.lineSeparator());

			webClient_class.append(client_context).append(System.lineSeparator()).append(System.lineSeparator());
			webClient_class.append(client_context_impl).append(System.lineSeparator());

			if(!client_stubs.isEmpty()) {
				webClient_class.append(System.lineSeparator()).append(client_stubs).append(System.lineSeparator());
			}

			webClient_class.append("}");

			moduleContext.removeImport(webClientClassName);
			moduleContext.removeImport("Context");
			moduleContext.removeImport("ContextImpl");

			webClient_class.insert(0, System.lineSeparator() + System.lineSeparator()).insert(0, moduleContext.getImports().stream().sorted().filter(i -> i.lastIndexOf(".") > 0 && !i.substring(0, i.lastIndexOf(".")).equals(webClientPackageName)).map(i -> new StringBuilder().append("import ").append(i).append(";")).collect(moduleContext.joining(System.lineSeparator())));
			if(!webClientPackageName.isEmpty()) {
				webClient_class.insert(0, ";" + System.lineSeparator() + System.lineSeparator()).insert(0, webClientPackageName).insert(0, "package ");
			}
			return webClient_class;
		}
		else if(moduleContext.getMode() == WebClientClassGenerationContext.GenerationMode.CLIENT_CLASS) {
			moduleContext.addImport(webClientClassName, webClientPackageName + "." + webClientClassName);
			moduleContext.addImport("Context", webClientPackageName + "." + webClientClassName + ".Context");

			StringBuilder webClient_type_argument = new StringBuilder(moduleContext.getTypeName(moduleContext.getWebClientType())).append("<").append(webClientClassName).append(".Context>");
			StringBuilder webClient_upper_type_argument = new StringBuilder(moduleContext.getTypeName(moduleContext.getWebClientType())).append("<? extends ").append(webClientClassName).append(".Context>");

			StringBuilder interceptorsConfigurersDecl = new StringBuilder(moduleContext.getTypeName(moduleContext.getListType())).append("<").append(moduleContext.getTypeName(moduleContext.getWebRouteInterceptorType())).append(".Configurer<? super ").append(webClientClassName).append(".Context>> interceptorsConfigurers");

			StringBuilder interceptorsConfigurersField = new StringBuilder(moduleContext.indent(1)).append("private ").append(interceptorsConfigurersDecl).append(";");

			StringBuilder interceptorsConfigurersSetter = new StringBuilder(moduleContext.indent(1)).append("public void setInterceptorsConfigurers(").append(interceptorsConfigurersDecl).append(") {").append(System.lineSeparator());
			interceptorsConfigurersSetter.append(moduleContext.indent(2)).append("this.interceptorsConfigurers = interceptorsConfigurers;").append(System.lineSeparator());
			interceptorsConfigurersSetter.append(moduleContext.indent(1)).append("}");

			StringBuilder webClient_apply = new StringBuilder(moduleContext.indent(1)).append("@Override").append(System.lineSeparator());
			webClient_apply.append(moduleContext.indent(1)).append("@SuppressWarnings(\"unchecked\")").append(System.lineSeparator());
			webClient_apply.append(moduleContext.indent(1)).append("public ").append(webClient_type_argument).append(" apply(").append(webClient_upper_type_argument).append(" webClient) {").append(System.lineSeparator());
			webClient_apply.append(moduleContext.indent(2)).append("return ((").append(webClient_type_argument).append(")webClient)").append(System.lineSeparator());
			webClient_apply.append(moduleContext.indent(3)).append(".configureInterceptors(this.interceptorsConfigurers);").append(System.lineSeparator());
			webClient_apply.append(moduleContext.indent(1)).append("}");

			StringBuilder client_context = this.visit(clientModuleInfo, moduleContext.withIndentDepth(1).withMode(WebClientClassGenerationContext.GenerationMode.CLIENT_CONTEXT));

			StringBuilder webClient_class = new StringBuilder();

			webClient_class.append("@").append(moduleContext.getTypeName(moduleContext.getMutatorAnnotationType())).append("(required = true) @").append(moduleContext.getTypeName(moduleContext.getBeanAnnotationType())).append("( name = \"").append(clientModuleInfo.getQualifiedName().getBeanName()).append("\" )").append(System.lineSeparator());
			webClient_class.append("@").append(moduleContext.getTypeName(moduleContext.getGeneratedType())).append("(value=\"").append(WebClientCompilerPlugin.class.getCanonicalName()).append("\", date = \"").append(ZonedDateTime.now()).append("\")").append(System.lineSeparator());
			webClient_class.append("public final class ").append(webClientClassName).append(" implements ").append(moduleContext.getTypeName(moduleContext.getFunctionType())).append("<").append(webClient_upper_type_argument).append(", ").append(webClient_type_argument).append("> {").append(System.lineSeparator()).append(System.lineSeparator());

			webClient_class.append(interceptorsConfigurersField).append(System.lineSeparator()).append(System.lineSeparator());

			webClient_class.append(webClient_apply).append(System.lineSeparator()).append(System.lineSeparator());

			webClient_class.append(interceptorsConfigurersSetter).append(System.lineSeparator()).append(System.lineSeparator());

			webClient_class.append(client_context).append(System.lineSeparator());

			webClient_class.append("}");

			moduleContext.removeImport(webClientClassName);
			moduleContext.removeImport("Context");

			webClient_class.insert(0, System.lineSeparator() + System.lineSeparator()).insert(0, moduleContext.getImports().stream().sorted().filter(i -> i.lastIndexOf(".") > 0 && !i.substring(0, i.lastIndexOf(".")).equals(webClientPackageName)).map(i -> new StringBuilder().append("import ").append(i).append(";")).collect(moduleContext.joining(System.lineSeparator())));
			if(!webClientPackageName.isEmpty()) {
				webClient_class.insert(0, ";" + System.lineSeparator() + System.lineSeparator()).insert(0, webClientPackageName).insert(0, "package ");
			}
			return webClient_class;
		}
		else if(moduleContext.getMode() == WebClientClassGenerationContext.GenerationMode.CLIENT_CONTEXT) {
			StringBuilder result = new StringBuilder();
			result.append(moduleContext.indent(0)).append("public interface Context extends ");
			if(clientModuleInfo.getContextTypes().length > 0) {
				result.append(Arrays.stream(clientModuleInfo.getContextTypes()).map(moduleContext::getTypeName).collect(Collectors.joining(", ")));
			}
			else {
				result.append(moduleContext.getTypeName(moduleContext.getElementUtils().getTypeElement(ExchangeContext.class.getCanonicalName()).asType()));
			}
			result.append(" {}");

			return result;
		}
		else if(moduleContext.getMode() == WebClientClassGenerationContext.GenerationMode.CLIENT_CONTEXT_IMPL) {
			// For each context type I must define the methods in order to implement the context
			// This can actually be tricky because some interface may override others
			// Let's start by listing all methods

			Map<String, String> context_fields = new HashMap<>();
			Map<String, String> context_methods = new HashMap<>();
			Set<String> defaultMethods = new HashSet<>();
			for(TypeMirror contextType : clientModuleInfo.getContextTypes()) {
				ElementFilter.methodsIn(moduleContext.getElementUtils().getAllMembers((TypeElement) moduleContext.getTypeUtils().asElement(contextType))).stream()
					.filter(exectuableElement -> exectuableElement.getEnclosingElement().getKind() == ElementKind.INTERFACE && !exectuableElement.getModifiers().contains(Modifier.STATIC))
					.forEach(exectuableElement -> {
						ExecutableType executableType =  (ExecutableType)moduleContext.getTypeUtils().asMemberOf((DeclaredType)contextType, exectuableElement);

						StringBuilder signatureKeyBuilder = new StringBuilder();
						signatureKeyBuilder.append(exectuableElement.getSimpleName().toString());
						exectuableElement.getParameters().stream().map(variableElement -> moduleContext.getTypeUtils().erasure(variableElement.asType()).toString()).forEach(signatureKeyBuilder::append);
						String signatureKey = signatureKeyBuilder.toString();

						if(exectuableElement.isDefault()) {
							defaultMethods.add(signatureKey);
							context_methods.remove(signatureKey);
							context_fields.remove(signatureKey);
						}
						else if(!defaultMethods.contains(signatureKey)) {
							String fieldName = null;
							TypeMirror fieldType = null;
							StringBuilder contextMethod = new StringBuilder();
							String methodName = exectuableElement.getSimpleName().toString();
							if( (methodName.startsWith("get") || methodName.startsWith("is")) && executableType.getParameterTypes().isEmpty() && executableType.getReturnType().getKind() != TypeKind.VOID) {
								int fileNameIndex = methodName.startsWith("get") ? 3 : 2;
								fieldName = Character.toLowerCase(methodName.charAt(fileNameIndex)) + methodName.substring(fileNameIndex + 1);
								fieldType = executableType.getReturnType();

								contextMethod.append(moduleContext.indent(2)).append("@Override").append(System.lineSeparator());
								contextMethod.append(moduleContext.indent(2)).append("public ").append(moduleContext.getTypeName(executableType.getReturnType())).append(" ").append(methodName).append("() {").append(System.lineSeparator());
								contextMethod.append(moduleContext.indent(3)).append("return this.").append(fieldName).append(";").append(System.lineSeparator());
								contextMethod.append(moduleContext.indent(2)).append("}");
							}
							else if(methodName.startsWith("set") && executableType.getParameterTypes().size() == 1 && executableType.getReturnType().getKind() == TypeKind.VOID) {
								fieldName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
								fieldType = executableType.getParameterTypes().getFirst();

								contextMethod.append(moduleContext.indent(2)).append("@Override").append(System.lineSeparator());
								contextMethod.append(moduleContext.indent(2)).append("public void ").append(methodName).append("(").append(moduleContext.getTypeName(executableType.getParameterTypes().getFirst())).append(" ").append(fieldName).append(") {").append(System.lineSeparator());
								contextMethod.append(moduleContext.indent(3)).append("this.").append(fieldName).append(" = ").append(fieldName).append(";").append(System.lineSeparator());
								contextMethod.append(moduleContext.indent(2)).append("}");
							}
							else {
								contextMethod.append(moduleContext.indent(2)).append("public ").append(moduleContext.getTypeName(executableType.getReturnType())).append(" ").append(methodName).append("(").append(executableType.getParameterTypes().stream().map(parameterType -> new StringBuilder().append(moduleContext.getTypeName(parameterType)).append(" ").append(methodName)).collect(moduleContext.joining(", "))).append(") {").append(System.lineSeparator());
								if(executableType.getReturnType().getKind() != TypeKind.VOID) {
									contextMethod.append(moduleContext.indent(3)).append("return ");
									switch(executableType.getReturnType().getKind()) {
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
								contextMethod.append(moduleContext.indent(2)).append("}");
							}

							if(fieldName != null && fieldType != null) {
								StringBuilder contextField = new StringBuilder();
								contextField.append(moduleContext.indent(2)).append("private ").append(moduleContext.getTypeName(fieldType)).append(" ").append(fieldName).append(";");
								context_fields.put(signatureKey, contextField.toString());
							}
							context_methods.put(signatureKey, contextMethod.toString());
						}
					});
			}

			StringBuilder context_impl = new StringBuilder();
			context_impl.append(moduleContext.indent(0)).append("private static class ContextImpl implements ").append(webClientClassName).append(".Context {").append(System.lineSeparator());
			if(!context_fields.isEmpty()) {
				context_impl.append(context_fields.values().stream().distinct().collect(Collectors.joining(System.lineSeparator()))).append(System.lineSeparator()).append(System.lineSeparator());
			}
			if(!context_methods.isEmpty()) {
				context_impl.append(context_methods.values().stream().collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()))).append(System.lineSeparator());
			}
			context_impl.append(moduleContext.indent(0)).append("}");

			return context_impl;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebClientRouteInterceptorConfigurerInfo interceptorsConfigurerInfo, WebClientClassGenerationContext context) {
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebClientSocketInfo webClientSocketInfo, WebClientClassGenerationContext context) {
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebClientStubInfo clientStubInfo, WebClientClassGenerationContext context) {
		WebClientClassGenerationContext stubContext = context.withWebClientStubInfo(clientStubInfo);
		String webClientClassName = stubContext.getWebClientModuleInfo().getQualifiedName().getClassName();
		webClientClassName = webClientClassName.substring(webClientClassName.lastIndexOf(".") + 1);

		String webClientStubImplClassName = clientStubInfo.getElement().getQualifiedName().toString() + "Impl";
		webClientStubImplClassName = webClientStubImplClassName.substring(webClientStubImplClassName.lastIndexOf(".") + 1);

		if(stubContext.getMode() == WebClientClassGenerationContext.GenerationMode.CLIENT_STUB_CLASS) {
			StringBuilder typesField = new StringBuilder(stubContext.indent(1)).append("private static final ").append(stubContext.getTypeName(stubContext.getTypeType())).append("[] ").append(VARIABLE_TYPES).append(" = new ").append(stubContext.getTypeName(stubContext.getTypeType())).append("[] {").append(System.lineSeparator());
			typesField.append(Arrays.stream(clientStubInfo.getTypesRegistry()).map(type ->  new StringBuilder(stubContext.indent(2)).append(stubContext.getTypeGenerator(type))).collect(Collectors.joining("," + System.lineSeparator()))).append(System.lineSeparator());
			typesField.append(stubContext.indent(1)).append("};");

			StringBuilder exchangeBuilderField = new StringBuilder(stubContext.indent(1)).append("private final ").append(stubContext.getTypeName(stubContext.getWebClientType())).append(".WebExchangeBuilder<").append(webClientClassName).append(".Context> exchangeBuilder;");

			StringBuilder clientStub_constructor = new StringBuilder(stubContext.indent(1)).append("public ").append(webClientStubImplClassName).append("(");
			clientStub_constructor.append(stubContext.getTypeName(stubContext.getWebClientType())).append("<").append(webClientClassName).append(".Context> webClient");
			clientStub_constructor.append(") {").append(System.lineSeparator());
			clientStub_constructor.append(stubContext.indent(2)).append("this.exchangeBuilder = webClient.exchange(\"").append(clientStubInfo.getURI()).append("\");").append(System.lineSeparator());
			clientStub_constructor.append(stubContext.indent(1)).append("}");

			WebClientClassGenerationContext routeContext = stubContext.withIndentDepthAdd(1);
			StringBuilder clientRoutes = Arrays.stream(clientStubInfo.getRoutes()).map(routeInfo -> this.visit(routeInfo, routeContext)).collect(stubContext.joining(System.lineSeparator()));

			StringBuilder clientStub_class = new StringBuilder();

			clientStub_class.append(stubContext.indent(0)).append("@").append(stubContext.getTypeName(stubContext.getBeanAnnotationType())).append("( name = \"").append(clientStubInfo.getQualifiedName().getBeanName()).append("\", visibility = ").append(stubContext.getTypeName(stubContext.getBeanAnnotationType())).append(".Visibility.").append(clientStubInfo.getVisibility().name()).append(" )").append(System.lineSeparator());
			clientStub_class.append(stubContext.indent(0)).append("public static class ").append(webClientStubImplClassName).append(" implements ").append(stubContext.getTypeName(clientStubInfo.getType())).append(" {").append(System.lineSeparator()).append(System.lineSeparator());

			if(clientStubInfo.getTypesRegistry().length > 0) {
				clientStub_class.append(typesField).append(System.lineSeparator()).append(System.lineSeparator());
			}

			clientStub_class.append(exchangeBuilderField).append(System.lineSeparator()).append(System.lineSeparator());

			clientStub_class.append(clientStub_constructor).append(System.lineSeparator()).append(System.lineSeparator());

			clientStub_class.append(clientRoutes);

			clientStub_class.append(stubContext.indent(0)).append("}");

			return clientStub_class;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebClientRouteInfo routeInfo, WebClientClassGenerationContext context) {
		WebClientClassGenerationContext routeContext = context.withWebClientRouteInfo(routeInfo);
		if(routeInfo instanceof WebSocketClientRouteInfo) {
			return this.visit((WebSocketClientRouteInfo)routeInfo, routeContext);
		}
		else {
			if(routeContext.getMode() == WebClientClassGenerationContext.GenerationMode.CLIENT_STUB_CLASS) {
				String contentType = routeInfo.getProduce().orElse("");
				String accept = String.join(",", routeInfo.getConsumes());
				String acceptLanguage = String.join(",", routeInfo.getLanguages());

				WebClientClassGenerationContext headerParametersContext = routeContext.withIndentDepthAdd(5).withMode(WebClientClassGenerationContext.GenerationMode.EXCHANGE_REQUEST_HEADERS);
				StringBuilder headerParameters = Arrays.stream(routeInfo.getParameters())
					.filter(webParameterInfo -> webParameterInfo instanceof WebHeaderParameterInfo)
					.map(webParameterInfo -> this.visit(webParameterInfo, headerParametersContext))
					.collect(routeContext.joining(", "));

				WebClientClassGenerationContext cookieParametersContext = routeContext.withIndentDepthAdd(6).withMode(WebClientClassGenerationContext.GenerationMode.EXCHANGE_REQUEST_HEADERS);
				StringBuilder cookieParameters = Arrays.stream(routeInfo.getParameters())
					.filter(webParameterInfo -> webParameterInfo instanceof WebCookieParameterInfo)
					.map(webParameterInfo -> this.visit(webParameterInfo, cookieParametersContext))
					.collect(routeContext.joining(", "));

				WebClientClassGenerationContext bodyParameterContext = routeContext.withIndentDepthAdd(4).withMode(WebClientClassGenerationContext.GenerationMode.EXCHANGE_REQUEST_BODY);
				StringBuilder bodyParameter = Arrays.stream(routeInfo.getParameters())
					.filter(webParameterInfo -> webParameterInfo instanceof WebRequestBodyParameterInfo)
					.map(webParameterInfo -> this.visit(webParameterInfo, bodyParameterContext))
					.collect(routeContext.joining());

				WebClientClassGenerationContext urlEncodedBodyParameterContext = routeContext.withIndentDepthAdd(5).withMode(WebClientClassGenerationContext.GenerationMode.EXCHANGE_REQUEST_BODY_URL_ENCODED);
				StringBuilder urlEncodedBody = Arrays.stream(routeInfo.getParameters())
					.filter(webParameterInfo -> webParameterInfo instanceof WebFormParameterInfo)
					.map(webParameterInfo -> this.visit(webParameterInfo, urlEncodedBodyParameterContext))
					.collect(routeContext.joining(", " + System.lineSeparator()));

				WebClientClassGenerationContext multipartBodyParameterContext = routeContext.withIndentDepthAdd(5).withMode(WebClientClassGenerationContext.GenerationMode.EXCHANGE_REQUEST_BODY_MULTIPART);
				StringBuilder multipartBody = Arrays.stream(routeInfo.getParameters())
					.filter(webParameterInfo -> webParameterInfo instanceof WebClientPartParameterInfo)
					.map(webParameterInfo -> this.visit(webParameterInfo, multipartBodyParameterContext))
					.collect(routeContext.joining(", " + System.lineSeparator()));

				WebClientClassGenerationContext responseBodyContext = routeContext.withIndentDepthAdd(2).withMode(WebClientClassGenerationContext.GenerationMode.EXCHANGE_RESPONSE_BODY);
				StringBuilder responseBody = this.visit(routeInfo.getReturn(), responseBodyContext);

				WebClientClassGenerationContext exchangeParameterContext = routeContext.withIndentDepthAdd(3).withMode(WebClientClassGenerationContext.GenerationMode.EXCHANGE);
				StringBuilder exchangeParameter = Arrays.stream(routeInfo.getParameters())
					.filter(webParameterInfo -> webParameterInfo instanceof WebExchangeParameterInfo)
					.map(webParameterInfo -> this.visit(webParameterInfo, exchangeParameterContext))
					.collect(routeContext.joining(", "));

				StringBuilder exchangeToResponse = new StringBuilder();

				if(contentType.isEmpty() && accept.isEmpty() && acceptLanguage.isEmpty() && exchangeParameter.isEmpty() && headerParameters.isEmpty() && cookieParameters.isEmpty() && bodyParameter.isEmpty() && urlEncodedBody.isEmpty() && multipartBody.isEmpty()) {
					if(routeInfo.getReturn() instanceof WebClientExchangeReturnInfo) {
						exchangeToResponse.append(VARIABLE_EXCHANGE).append(" -> ").append("(").append(routeContext.getTypeName(((WebClientExchangeReturnInfo)routeInfo.getReturn()).getType())).append(")").append(VARIABLE_EXCHANGE);
					}
					else {
						exchangeToResponse.append(routeContext.getTypeName(routeContext.getWebExchangeType())).append("::response");
					}
				}
				else {
					exchangeToResponse.append(VARIABLE_EXCHANGE).append(" -> {").append(System.lineSeparator());
					if(!contentType.isEmpty() || !accept.isEmpty() || !acceptLanguage.isEmpty() || !headerParameters.isEmpty() || !cookieParameters.isEmpty() || !bodyParameter.isEmpty() || !urlEncodedBody.isEmpty() || !multipartBody.isEmpty()) {
						exchangeToResponse.append(routeContext.indent(3)).append(VARIABLE_EXCHANGE).append(".request()");
						if(!contentType.isEmpty() || !accept.isEmpty() || !acceptLanguage.isEmpty() || !headerParameters.isEmpty() || !cookieParameters.isEmpty()) {
							exchangeToResponse.append(System.lineSeparator()).append(routeContext.indent(4)).append(".headers(").append(VARIABLE_HEADERS).append(" -> ").append(VARIABLE_HEADERS).append(System.lineSeparator());
							if(!contentType.isEmpty()) {
								exchangeToResponse.append(routeContext.indent(5)).append(".contentType(\"").append(contentType).append("\")").append(System.lineSeparator());
							}
							if(!accept.isEmpty()) {
								exchangeToResponse.append(routeContext.indent(5)).append(".set(").append(routeContext.getTypeName(routeContext.getHeadersType())).append(".NAME_ACCEPT, \"").append(accept).append("\")").append(System.lineSeparator());
							}
							if(!acceptLanguage.isEmpty()) {
								exchangeToResponse.append(routeContext.indent(5)).append(".set(").append(routeContext.getTypeName(routeContext.getHeadersType())).append(".NAME_ACCEPT_LANGUAGE, \"").append(acceptLanguage).append("\")").append(System.lineSeparator());
							}

							if(!headerParameters.isEmpty()) {
								exchangeToResponse.append(headerParameters).append(System.lineSeparator());
							}
							if(!cookieParameters.isEmpty()) {
								exchangeToResponse.append(routeContext.indent(5)).append(".cookies(").append(VARIABLE_COOKIES).append(" -> ").append(VARIABLE_COOKIES).append(System.lineSeparator());
								exchangeToResponse.append(cookieParameters).append(System.lineSeparator());
								exchangeToResponse.append(routeContext.indent(5)).append(")").append(System.lineSeparator());
							}
							exchangeToResponse.append(routeContext.indent(4)).append(")");
						}

						if(!bodyParameter.isEmpty()) {
							exchangeToResponse.append(System.lineSeparator()).append(bodyParameter);
						}
						else if(!urlEncodedBody.isEmpty()) {
							exchangeToResponse.append(System.lineSeparator()).append(routeContext.indent(4)).append(".body().urlEncoded().from((").append(VARIABLE_FACTORY).append(", ").append(VARIABLE_DATA).append(") -> ").append(VARIABLE_DATA).append(".stream(").append(routeContext.getTypeName(routeContext.getFluxType())).append(".just(").append(System.lineSeparator());
							exchangeToResponse.append(urlEncodedBody).append(System.lineSeparator());
							exchangeToResponse.append(routeContext.indent(4)).append(")))");
						}
						else if(!multipartBody.isEmpty()) {
							exchangeToResponse.append(System.lineSeparator()).append(routeContext.indent(4)).append(".body().multipart().from((").append(VARIABLE_FACTORY).append(", ").append(VARIABLE_DATA).append(") -> ").append(VARIABLE_DATA).append(".stream(").append(routeContext.getTypeName(routeContext.getFluxType())).append(".just(").append(System.lineSeparator());
							exchangeToResponse.append(multipartBody).append(System.lineSeparator());
							exchangeToResponse.append(routeContext.indent(4)).append(")))");
						}
						exchangeToResponse.append(";").append(System.lineSeparator());
					}
					if (!exchangeParameter.isEmpty()) {
						exchangeToResponse.append(exchangeParameter).append(System.lineSeparator());
					}

					exchangeToResponse.append(routeContext.indent(3)).append("return ");
					if(routeInfo.getReturn() instanceof WebClientExchangeReturnInfo) {
						exchangeToResponse.append("(").append(context.getTypeName(((WebClientExchangeReturnInfo)routeInfo.getReturn()).getType())).append(")").append(VARIABLE_EXCHANGE);
					}
					else {
						exchangeToResponse.append(VARIABLE_EXCHANGE).append(".response()");
					}

					exchangeToResponse.append(";").append(System.lineSeparator());
					exchangeToResponse.append(routeContext.indent(2)).append("}");
				}

				StringBuilder result = new StringBuilder();

				result.append(routeContext.indent(0)).append("@Override").append(System.lineSeparator());

				StringBuilder routeMethodSignature = new StringBuilder();
				routeMethodSignature.append("public ");
				if(!routeInfo.getElement().getTypeParameters().isEmpty()) {
					routeMethodSignature.append("<").append(routeInfo.getElement().getTypeParameters().stream().map(parameter -> routeContext.getTypeVariableName((TypeVariable)parameter.asType())).collect(Collectors.joining(", "))).append("> ");
				}
				routeMethodSignature.append(routeContext.getTypeName(routeInfo.getType().getReturnType())).append(" ").append(routeInfo.getElement().getSimpleName()).append("(");
				String[] routeParameters = new String[routeInfo.getElement().getParameters().size()];
				for(int i=0;i<routeParameters.length;i++) {
					routeParameters[i] = routeContext.getTypeName(routeInfo.getType().getParameterTypes().get(i)) + " " + routeInfo.getElement().getParameters().get(i).getSimpleName();
				}
				routeMethodSignature.append(String.join(", ", routeParameters)).append(")");

				result.append(routeContext.indent(0)).append(routeMethodSignature).append(" {").append(System.lineSeparator());

				result.append(routeContext.indent(1)).append("return this.exchangeBuilder.clone()").append(System.lineSeparator());
				result.append(routeContext.indent(2)).append(".method(").append(routeContext.getTypeName(routeContext.getMethodType())).append(".").append(routeInfo.getMethod()).append(")").append(System.lineSeparator());
				routeInfo.getPath().ifPresent(path -> result.append(routeContext.indent(2)).append(".path(\"").append(path).append("\")").append(System.lineSeparator()));

				WebClientClassGenerationContext builderParameterContext = routeContext.withIndentDepthAdd(2).withMode(WebClientClassGenerationContext.GenerationMode.EXCHANGE_BUILDER_PARAMETER);
				StringBuilder builderParameters = Arrays.stream(routeInfo.getParameters()).map(webParameterInfo -> this.visit(webParameterInfo, builderParameterContext)).filter(builderParameter -> !builderParameter.isEmpty()).collect(routeContext.joining(System.lineSeparator()));
				if(!builderParameters.isEmpty()) {
					result.append(builderParameters).append(System.lineSeparator());
				}
				result.append(routeContext.indent(2)).append(".build()").append(System.lineSeparator());

				if(routeInfo.getReturn() instanceof WebClientExchangeReturnInfo) {
					result.append(routeContext.indent(2)).append(".map(");
				}
				else {
					result.append(routeContext.indent(2)).append(".flatMap(");
				}
				result.append(exchangeToResponse).append(")");

				if(!responseBody.isEmpty()) {
					result.append(System.lineSeparator()).append(responseBody);
				}

				result.append(";").append(System.lineSeparator());
				result.append(routeContext.indent(0)).append("}").append(System.lineSeparator());

				return result;
			}
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebParameterInfo parameterInfo, WebClientClassGenerationContext context) {
		if(parameterInfo instanceof WebCookieParameterInfo) {
			return this.visit((WebCookieParameterInfo)parameterInfo, context);
		}
		else if(parameterInfo instanceof WebFormParameterInfo) {
			return this.visit((WebFormParameterInfo)parameterInfo, context);
		}
		else if(parameterInfo instanceof WebHeaderParameterInfo) {
			return this.visit((WebHeaderParameterInfo)parameterInfo, context);
		}
		else if(parameterInfo instanceof WebClientPartParameterInfo) {
			return this.visit((WebClientPartParameterInfo)parameterInfo, context);
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
		else if(parameterInfo instanceof WebExchangeParameterInfo) {
			return this.visit((WebExchangeParameterInfo)parameterInfo, context);
		}
		else if(parameterInfo instanceof WebSocketExchangeParameterInfo) {
			return this.visit((WebSocketExchangeParameterInfo)parameterInfo, context);
		}
		else if(parameterInfo instanceof WebSocketOutboundParameterInfo) {
			return this.visit((WebSocketOutboundParameterInfo)parameterInfo, context);
		}
		else if(parameterInfo instanceof WebSocketClientOutboundPublisherParameterInfo) {
			return this.visit((WebSocketClientOutboundPublisherParameterInfo)parameterInfo, context);
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebCookieParameterInfo cookieParameterInfo, WebClientClassGenerationContext context) {
		if(context.getMode() == WebClientClassGenerationContext.GenerationMode.EXCHANGE_REQUEST_HEADERS) {
			StringBuilder result = new StringBuilder();
			result.append(context.indent(0)).append(".addCookie(\"").append(cookieParameterInfo.getQualifiedName().getParameterName()).append("\", ").append(cookieParameterInfo.getElement().getSimpleName()).append(", ").append(VARIABLE_TYPES).append("[").append(context.getTypeIndex(cookieParameterInfo.getType())).append("])");
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebFormParameterInfo formParameterInfo, WebClientClassGenerationContext context) {
		if(context.getMode() == WebClientClassGenerationContext.GenerationMode.EXCHANGE_REQUEST_BODY_URL_ENCODED) {
			StringBuilder result = new StringBuilder();
			result.append(context.indent(0)).append(VARIABLE_FACTORY).append(".create(\"").append(formParameterInfo.getQualifiedName().getParameterName()).append("\", ").append(formParameterInfo.getElement().getSimpleName()).append(", ").append(VARIABLE_TYPES).append("[").append(context.getTypeIndex(formParameterInfo.getType())).append("])");
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebHeaderParameterInfo headerParameterInfo, WebClientClassGenerationContext context) {
		if(context.getMode() == WebClientClassGenerationContext.GenerationMode.EXCHANGE_REQUEST_HEADERS) {
			StringBuilder result = new StringBuilder();
			result.append(context.indent(0)).append(".setParameter(\"").append(headerParameterInfo.getQualifiedName().getParameterName()).append("\", ").append(headerParameterInfo.getElement().getSimpleName()).append(", ").append(VARIABLE_TYPES).append("[").append(context.getTypeIndex(headerParameterInfo.getType())).append("])");
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebClientPartParameterInfo partParameterInfo, WebClientClassGenerationContext context) {
		if(context.getMode() == WebClientClassGenerationContext.GenerationMode.EXCHANGE_REQUEST_BODY_MULTIPART) {
			StringBuilder result = new StringBuilder();

			result.append(context.indent(0)).append(VARIABLE_FACTORY);
			switch(partParameterInfo.getBodyKind()) {
				case RAW: {
					result.append(".raw(");
					break;
				}
				case CHARSEQUENCE: {
					result.append(".string(");
					break;
				}
				case RESOURCE: {
					result.append(".resource(");
					break;
				}
				case ENCODED: {
					result.append(".encoded(");
					break;
				}
				default: {
					throw new IllegalStateException();
				}
			}
			result.append(VARIABLE_PART).append(" -> ").append(VARIABLE_PART).append(System.lineSeparator());
			if(partParameterInfo.getContentType() != null) {
				result.append(context.indent(1)).append(".headers(").append(VARIABLE_HEADERS).append(" -> ").append(VARIABLE_HEADERS).append(".contentType(\"").append(partParameterInfo.getContentType()).append("\"))").append(System.lineSeparator());
			}
			result.append(context.indent(1)).append(".name(\"").append(partParameterInfo.getQualifiedName().getParameterName()).append("\")").append(System.lineSeparator());
			if(partParameterInfo.getFilename() != null) {
				result.append(context.indent(1)).append(".filename(\"").append(partParameterInfo.getFilename()).append("\")").append(System.lineSeparator());
			}

			switch(partParameterInfo.getBodyReactiveKind()) {
				case ONE: {
					if(partParameterInfo.getBodyKind() == WebClientPartParameterInfo.PartBodyKind.ENCODED) {
						result.append(context.indent(1)).append(".one(");
						break;
					}
				}
				case MANY: {
					if(partParameterInfo.getBodyKind() == WebClientPartParameterInfo.PartBodyKind.ENCODED) {
						result.append(context.indent(1)).append(".many(");
						break;
					}
				}
				case PUBLISHER: {
					result.append(context.indent(1)).append(".stream(");
					break;
				}
				case NONE: {
					result.append(context.indent(1)).append(".value(");
					break;
				}
				default: {
					throw new IllegalStateException();
				}
			}
			result.append(partParameterInfo.getElement().getSimpleName()).append(")");
			if(partParameterInfo.getBodyKind() == WebClientPartParameterInfo.PartBodyKind.ENCODED) {
				result.append(",").append(System.lineSeparator()).append(context.indent(1)).append(VARIABLE_TYPES).append("[").append(context.getTypeIndex(partParameterInfo.getType())).append("]");
			}
			result.append(System.lineSeparator()).append(context.indent(0)).append(")");

			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebPathParameterInfo pathParameterInfo, WebClientClassGenerationContext context) {
		if(context.getMode() == WebClientClassGenerationContext.GenerationMode.EXCHANGE_BUILDER_PARAMETER) {
			StringBuilder result = new StringBuilder();
			result.append(context.indent(0)).append(".pathParameter(\"").append(pathParameterInfo.getQualifiedName().getParameterName()).append("\", ").append(pathParameterInfo.getElement().getSimpleName()).append(", ").append(VARIABLE_TYPES).append("[").append(context.getTypeIndex(pathParameterInfo.getType())).append("])");
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebQueryParameterInfo queryParameterInfo, WebClientClassGenerationContext context) {
		if(context.getMode() == WebClientClassGenerationContext.GenerationMode.EXCHANGE_BUILDER_PARAMETER) {
			StringBuilder result = new StringBuilder();
			result.append(context.indent(0)).append(".queryParameter(\"").append(queryParameterInfo.getQualifiedName().getParameterName()).append("\", ").append(queryParameterInfo.getElement().getSimpleName()).append(", ").append(VARIABLE_TYPES).append("[").append(context.getTypeIndex(queryParameterInfo.getType())).append("])");
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebRequestBodyParameterInfo bodyParameterInfo, WebClientClassGenerationContext context) {
		if(context.getMode() == WebClientClassGenerationContext.GenerationMode.EXCHANGE_REQUEST_BODY) {
			StringBuilder result = new StringBuilder();
			result.append(context.indent(0)).append(".body()");
			switch(bodyParameterInfo.getBodyKind()) {
				case RAW: {
					result.append(".raw()");
					break;
				}
				case CHARSEQUENCE: {
					result.append(".string()");
					break;
				}
				case RESOURCE: {
					result.append(".resource()");
					break;
				}
				case ENCODED: {
					result.append(".<").append(context.getTypeName(bodyParameterInfo.getType())).append(">encoder(").append(VARIABLE_TYPES).append("[").append(context.getTypeIndex(bodyParameterInfo.getType())).append("])");
					break;
				}
				default: {
					throw new IllegalStateException();
				}
			}

			switch (bodyParameterInfo.getBodyReactiveKind()) {
				case NONE: {
					result.append(".value(").append(bodyParameterInfo.getElement().getSimpleName()).append(")");
					break;
				}
				case ONE: {
					if(bodyParameterInfo.getBodyKind() == RequestBodyKind.ENCODED) {
						result.append(".one(").append(bodyParameterInfo.getElement().getSimpleName()).append(")");
						break;
					}
				}
				case MANY: {
					if(bodyParameterInfo.getBodyKind() == RequestBodyKind.ENCODED) {
						result.append(".many(").append(bodyParameterInfo.getElement().getSimpleName()).append(")");
						break;
					}
				}
				case PUBLISHER: {
					result.append(".stream(").append(bodyParameterInfo.getElement().getSimpleName()).append(")");
					break;
				}
				default: {
					throw new IllegalStateException();
				}
			}
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebExchangeParameterInfo exchangeParameterInfo, WebClientClassGenerationContext context) {
		if(context.getMode() == WebClientClassGenerationContext.GenerationMode.EXCHANGE) {
			StringBuilder result = new StringBuilder();
			result.append(context.indent(0)).append(exchangeParameterInfo.getElement().getSimpleName()).append(".configure(");
			if(exchangeParameterInfo.getType().getKind().equals(TypeKind.TYPEVAR)) {
				result.append("(").append(context.getTypeName(context.getWebExchangeType())).append("<").append(context.getTypeName(exchangeParameterInfo.getType())).append(">)");
			}
			result.append(VARIABLE_EXCHANGE).append(");");
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebClientRouteReturnInfo returnInfo, WebClientClassGenerationContext context) {
		if(returnInfo instanceof WebClientExchangeReturnInfo) {
			return this.visit((WebClientExchangeReturnInfo)returnInfo, context);
		}
		else if(returnInfo instanceof WebClientResponseReturnInfo) {
			return this.visit((WebClientResponseReturnInfo)returnInfo, context);
		}
		else if(returnInfo instanceof WebClientResponseBodyInfo) {
			return this.visit((WebClientResponseBodyInfo)returnInfo, context);
		}
		else if(returnInfo instanceof WebSocketClientRouteReturnInfo) {
			return this.visit((WebSocketClientRouteReturnInfo)returnInfo, context);
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebClientExchangeReturnInfo exchangeReturnInfo, WebClientClassGenerationContext context) {
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebClientResponseReturnInfo responseReturnInfo, WebClientClassGenerationContext context) {
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebClientResponseBodyInfo responseBodyInfo, WebClientClassGenerationContext context) {
		if(context.getMode() == WebClientClassGenerationContext.GenerationMode.EXCHANGE_RESPONSE_BODY) {
			StringBuilder result = new StringBuilder();
			switch(responseBodyInfo.getBodyKind()) {
				case EMPTY: {
					result.append(context.indent(0)).append(".then()");
					if(responseBodyInfo.getBodyReactiveKind() == ResponseBodyReactiveKind.MANY || responseBodyInfo.getBodyReactiveKind() == ResponseBodyReactiveKind.PUBLISHER) {
						result.append(".flux()");
					}
					break;
				}
				case RAW: {
					result.append(context.indent(0)).append(".flatMapMany(").append(VARIABLE_RESPONSE).append(" -> ").append(VARIABLE_RESPONSE).append(".body().raw().stream())");
					if(responseBodyInfo.getBodyReactiveKind() == ResponseBodyReactiveKind.ONE) {
						result.append(".reduceWith(").append(context.getTypeName(context.getUnpooledType())).append("::buffer, (").append(VARIABLE_ACC).append(", ").append(VARIABLE_CHUNK).append(") -> { try { return ").append(VARIABLE_ACC).append(".writeBytes(").append(VARIABLE_CHUNK).append("); } finally { ").append(VARIABLE_CHUNK).append(".release(); } })");
					}
					break;
				}
				case CHARSEQUENCE: {
					result.append(context.indent(0)).append(".flatMapMany(").append(VARIABLE_RESPONSE).append(" -> ").append(VARIABLE_RESPONSE).append(".body().string().stream())");
					if(responseBodyInfo.getBodyReactiveKind() == ResponseBodyReactiveKind.ONE) {
						result.append(".collect(").append(context.getTypeName(context.getCollectorsType())).append(".joining())");
					}
					else if(context.getTypeUtils().isSameType(responseBodyInfo.getType(), context.getStringType())) {
						result.append(".map(").append(context.getTypeName(context.getCharSequenceType())).append("::toString)");
					}
					break;
				}
				case ENCODED: {
					result.append(context.indent(0));
					switch(responseBodyInfo.getBodyReactiveKind()) {
						case ONE: {
							result.append(".flatMap(").append(VARIABLE_RESPONSE).append(" -> ").append(VARIABLE_RESPONSE).append(".body().<").append(context.getTypeName(responseBodyInfo.getType())).append(">decoder(").append(VARIABLE_TYPES).append("[").append(context.getTypeIndex(responseBodyInfo.getType())).append("]).one())");
							break;
						}
						case PUBLISHER:
						case MANY: {
							result.append(".flatMapMany(").append(VARIABLE_RESPONSE).append(" -> ").append(VARIABLE_RESPONSE).append(".body().<").append(context.getTypeName(responseBodyInfo.getType())).append(">decoder(").append(VARIABLE_TYPES).append("[").append(context.getTypeIndex(responseBodyInfo.getType())).append("]).many())");
							break;
						}
						default: {
							throw new IllegalStateException();
						}
					}
					break;
				}
				default: {
					throw new IllegalStateException();
				}
			}
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebSocketClientRouteInfo webSocketRouteInfo, WebClientClassGenerationContext context) {
		if(context.getMode() == WebClientClassGenerationContext.GenerationMode.CLIENT_STUB_CLASS) {
			WebClientClassGenerationContext responseBodyContext = context.withIndentDepthAdd(2).withMode(WebClientClassGenerationContext.GenerationMode.EXCHANGE_RESPONSE_BODY);
			StringBuilder responseBody = this.visit(webSocketRouteInfo.getReturn(), responseBodyContext);

			String acceptLanguage = String.join(",", webSocketRouteInfo.getLanguages());

			WebClientClassGenerationContext headerParametersContext = context.withIndentDepthAdd(5).withMode(WebClientClassGenerationContext.GenerationMode.EXCHANGE_REQUEST_HEADERS);
			StringBuilder headerParameters = Arrays.stream(webSocketRouteInfo.getParameters())
				.filter(webParameterInfo -> webParameterInfo instanceof WebHeaderParameterInfo)
				.map(webParameterInfo -> this.visit(webParameterInfo, headerParametersContext))
				.collect(context.joining(", "));

			WebClientClassGenerationContext cookieParametersContext = context.withIndentDepthAdd(6).withMode(WebClientClassGenerationContext.GenerationMode.EXCHANGE_REQUEST_HEADERS);
			StringBuilder cookieParameters = Arrays.stream(webSocketRouteInfo.getParameters())
				.filter(webParameterInfo -> webParameterInfo instanceof WebCookieParameterInfo)
				.map(webParameterInfo -> this.visit(webParameterInfo, cookieParametersContext))
				.collect(context.joining(", "));

			WebClientClassGenerationContext outboundPublisherParameterContext = context.withIndentDepthAdd(3).withMode(WebClientClassGenerationContext.GenerationMode.WS_OUTBOUND_PUBLISHER);
			StringBuilder outboundPublisherParameter = Arrays.stream(webSocketRouteInfo.getParameters())
				.filter(webParameterInfo -> webParameterInfo instanceof WebSocketClientOutboundPublisherParameterInfo)
				.map(webParameterInfo -> this.visit(webParameterInfo, outboundPublisherParameterContext))
				.collect(context.joining());

			WebClientClassGenerationContext inboundPublisherContext = context.withIndentDepthAdd(2).withMode(WebClientClassGenerationContext.GenerationMode.WS_INBOUND_PUBLISHER);
			StringBuilder inboundPublisher = this.visit(webSocketRouteInfo.getReturn(), inboundPublisherContext);

			WebClientClassGenerationContext exchangeParameterContext = context.withIndentDepthAdd(3).withMode(WebClientClassGenerationContext.GenerationMode.EXCHANGE);
			StringBuilder exchangeParameter = Arrays.stream(webSocketRouteInfo.getParameters())
				.filter(webParameterInfo -> webParameterInfo instanceof WebExchangeParameterInfo)
				.map(webParameterInfo -> this.visit(webParameterInfo, exchangeParameterContext))
				.collect(context.joining(", "));

			WebClientClassGenerationContext outboundParameterContext = context.withIndentDepthAdd(3).withMode(WebClientClassGenerationContext.GenerationMode.WS_OUTBOUND);
			StringBuilder outboundParameter = Arrays.stream(webSocketRouteInfo.getParameters())
				.filter(webParameterInfo -> webParameterInfo instanceof WebSocketOutboundParameterInfo)
				.map(webParameterInfo -> this.visit(webParameterInfo, outboundParameterContext))
				.collect(context.joining());

			WebClientClassGenerationContext wsExchangeParameterContext = context.withIndentDepthAdd(3).withMode(WebClientClassGenerationContext.GenerationMode.WS_EXCHANGE);
			StringBuilder wsExchangeParameter = Arrays.stream(webSocketRouteInfo.getParameters())
				.filter(webParameterInfo -> webParameterInfo instanceof WebSocketExchangeParameterInfo)
				.map(webParameterInfo -> this.visit(webParameterInfo, wsExchangeParameterContext))
				.collect(context.joining());

			StringBuilder exchangeToWsExchange = new StringBuilder();

			if(acceptLanguage.isEmpty() && exchangeParameter.isEmpty() && headerParameters.isEmpty() && cookieParameters.isEmpty()) {
				if(webSocketRouteInfo.getSubprotocol() == null) {
					exchangeToWsExchange.append(context.getTypeName(context.getWebExchangeType())).append("::webSocket");
				}
				else {
					exchangeToWsExchange.append(VARIABLE_EXCHANGE).append(" -> ").append(VARIABLE_EXCHANGE).append(".webSocket(\"").append(webSocketRouteInfo.getSubprotocol()).append("\")");
				}
			}
			else {
				exchangeToWsExchange.append(VARIABLE_EXCHANGE).append(" -> {").append(System.lineSeparator());
				if(!acceptLanguage.isEmpty() || !headerParameters.isEmpty() || !cookieParameters.isEmpty()) {
					exchangeToWsExchange.append(context.indent(3)).append(VARIABLE_EXCHANGE).append(".request()");
					exchangeToWsExchange.append(System.lineSeparator()).append(context.indent(4)).append(".headers(").append(VARIABLE_HEADERS).append(" -> ").append(VARIABLE_HEADERS).append(System.lineSeparator());
					if (!acceptLanguage.isEmpty()) {
						exchangeToWsExchange.append(context.indent(5)).append(".set(").append(context.getTypeName(context.getHeadersType())).append(".NAME_ACCEPT_LANGUAGE, \"").append(acceptLanguage).append("\")").append(System.lineSeparator());
					}

					if (!headerParameters.isEmpty()) {
						exchangeToWsExchange.append(headerParameters).append(System.lineSeparator());
					}
					if (!cookieParameters.isEmpty()) {
						exchangeToWsExchange.append(context.indent(5)).append(".cookies(").append(VARIABLE_COOKIES).append(" -> ").append(VARIABLE_COOKIES).append(System.lineSeparator());
						exchangeToWsExchange.append(cookieParameters).append(System.lineSeparator());
						exchangeToWsExchange.append(context.indent(5)).append(")").append(System.lineSeparator());
					}
					exchangeToWsExchange.append(context.indent(4)).append(")").append(";").append(System.lineSeparator());
				}
				if(!exchangeParameter.isEmpty()) {
					exchangeToWsExchange.append(exchangeParameter).append(System.lineSeparator());
				}
				exchangeToWsExchange.append(context.indent(3)).append("return ").append(VARIABLE_EXCHANGE).append(".webSocket(");
				if(webSocketRouteInfo.getSubprotocol() != null) {
					exchangeToWsExchange.append("\"").append(webSocketRouteInfo.getSubprotocol()).append("\"");
				}
				exchangeToWsExchange.append(");").append(System.lineSeparator());
				exchangeToWsExchange.append(context.indent(2)).append("}");
			}

			StringBuilder wsExchangeToInbound = new StringBuilder();
			wsExchangeToInbound.append(VARIABLE_EXCHANGE).append(" -> {").append(System.lineSeparator());
			if(!outboundPublisherParameter.isEmpty()) {
				wsExchangeToInbound.append(outboundPublisherParameter).append(System.lineSeparator());
			}
			if(!outboundParameter.isEmpty()) {
				if(outboundPublisherParameter.isEmpty()) {
					wsExchangeToInbound.append(context.indent(3)).append(VARIABLE_EXCHANGE).append(".outbound().closeOnComplete(").append(webSocketRouteInfo.isCloseOnComplete()).append(");").append(System.lineSeparator());
				}
				wsExchangeToInbound.append(outboundParameter).append(System.lineSeparator());
			}
			if(!wsExchangeParameter.isEmpty()) {
				wsExchangeToInbound.append(wsExchangeParameter).append(System.lineSeparator());
			}

			if(webSocketRouteInfo.getReturn() instanceof WebSocketClientExchangeReturnInfo) {
				wsExchangeToInbound.append(context.indent(3)).append("return (").append(context.getTypeName(((WebSocketClientExchangeReturnInfo)webSocketRouteInfo.getReturn()).getType())).append(")").append(VARIABLE_EXCHANGE).append(";").append(System.lineSeparator());
			}
			else {
				wsExchangeToInbound.append(context.indent(3)).append("return ").append(VARIABLE_EXCHANGE).append(".inbound();").append(System.lineSeparator());
			}
			wsExchangeToInbound.append(context.indent(2)).append("}");

			StringBuilder result = new StringBuilder();

			result.append(context.indent(0)).append("@Override").append(System.lineSeparator());

			StringBuilder routeMethodSignature = new StringBuilder();
			routeMethodSignature.append("public ");
			if(!webSocketRouteInfo.getElement().getTypeParameters().isEmpty()) {
				routeMethodSignature.append("<").append(webSocketRouteInfo.getElement().getTypeParameters().stream().map(parameter -> context.getTypeVariableName((TypeVariable)parameter.asType())).collect(Collectors.joining(", "))).append("> ");
			}
			routeMethodSignature.append(context.getTypeName(webSocketRouteInfo.getType().getReturnType())).append(" ").append(webSocketRouteInfo.getElement().getSimpleName()).append("(");
			String[] routeParameters = new String[webSocketRouteInfo.getElement().getParameters().size()];
			for(int i=0;i<routeParameters.length;i++) {
				routeParameters[i] = context.getTypeName(webSocketRouteInfo.getType().getParameterTypes().get(i)) + " " + webSocketRouteInfo.getElement().getParameters().get(i).getSimpleName();
			}
			routeMethodSignature.append(String.join(", ", routeParameters)).append(")");

			result.append(context.indent(0)).append(routeMethodSignature).append(" {").append(System.lineSeparator());

			result.append(context.indent(1)).append("return this.exchangeBuilder.clone()").append(System.lineSeparator());
			result.append(context.indent(2)).append(".method(").append(context.getTypeName(context.getMethodType())).append(".").append(webSocketRouteInfo.getMethod()).append(")").append(System.lineSeparator());
			webSocketRouteInfo.getPath().ifPresent(path -> result.append(context.indent(2)).append(".path(\"").append(path).append("\")").append(System.lineSeparator()));

			WebClientClassGenerationContext builderParameterContext = context.withIndentDepthAdd(2).withMode(WebClientClassGenerationContext.GenerationMode.EXCHANGE_BUILDER_PARAMETER);
			StringBuilder builderParameters = Arrays.stream(webSocketRouteInfo.getParameters()).map(webParameterInfo -> this.visit(webParameterInfo, builderParameterContext)).filter(builderParameter -> !builderParameter.isEmpty()).collect(context.joining(System.lineSeparator()));
			if(!builderParameters.isEmpty()) {
				result.append(builderParameters).append(System.lineSeparator());
			}
			result.append(context.indent(2)).append(".build()").append(System.lineSeparator());

			result.append(context.indent(2)).append(".flatMap(").append(exchangeToWsExchange).append(")").append(System.lineSeparator());

			result.append(context.indent(2)).append(".map(").append(wsExchangeToInbound).append(")");

			if(!inboundPublisher.isEmpty()) {
				result.append(System.lineSeparator()).append(inboundPublisher);
			}

			result.append(";").append(System.lineSeparator());
			result.append(context.indent(0)).append("}").append(System.lineSeparator());

			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebSocketExchangeParameterInfo exchangeParameterInfo, WebClientClassGenerationContext context) {
		if(context.getMode() == WebClientClassGenerationContext.GenerationMode.WS_EXCHANGE) {
			StringBuilder result = new StringBuilder();
			result.append(context.indent(0)).append(exchangeParameterInfo.getElement().getSimpleName()).append(".configure(");
			if(exchangeParameterInfo.getType().getKind().equals(TypeKind.TYPEVAR)) {
				result.append("(").append(context.getTypeName(context.getWeb2SocketExchangeType())).append("<").append(context.getTypeName(exchangeParameterInfo.getType())).append(">)");
			}
			result.append(VARIABLE_EXCHANGE).append(");");
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebSocketOutboundParameterInfo outboundParameterInfo, WebClientClassGenerationContext context) {
		if(context.getMode() == WebClientClassGenerationContext.GenerationMode.WS_OUTBOUND) {
			StringBuilder result = new StringBuilder();
			result.append(context.indent(0)).append(outboundParameterInfo.getElement().getSimpleName()).append(".accept(").append(VARIABLE_EXCHANGE).append(".outbound());");
			return result;
		}

		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebSocketClientOutboundPublisherParameterInfo outboundPublisherParameterInfo, WebClientClassGenerationContext context) {
		if(context.getMode() == WebClientClassGenerationContext.GenerationMode.WS_OUTBOUND_PUBLISHER) {
			StringBuilder result = new StringBuilder();
			result.append(context.indent(0)).append(VARIABLE_EXCHANGE).append(".outbound()");
			result.append(".closeOnComplete(").append(((WebSocketClientRouteInfo)context.getWebClientRouteInfo()).isCloseOnComplete()).append(")");
			WebSocketMessage.Kind webSocketMessageKind = ((WebSocketClientRouteInfo)context.getWebClientRouteInfo()).getMessageType();
			boolean isPublisher = outboundPublisherParameterInfo.getBoundReactiveKind() == WebSocketBoundPublisherInfo.BoundReactiveKind.PUBLISHER;
			boolean isCharSequence = false;
			switch(outboundPublisherParameterInfo.getBoundKind()) {
				case CHARSEQUENCE_REDUCED:
				case CHARSEQUENCE_REDUCED_ONE:
				case CHARSEQUENCE_PUBLISHER:
				case CHARSEQUENCE_MANY: isCharSequence = true;
				case RAW_REDUCED:
				case RAW_REDUCED_ONE:
				case RAW_PUBLISHER:
				case RAW_MANY: {
					result.append(".messages(factory -> ");
					if(isPublisher) {
						result.append(context.getTypeName(context.getFluxType())).append(".from(").append(outboundPublisherParameterInfo.getElement().getSimpleName()).append(")");
					}
					else {
						result.append(outboundPublisherParameterInfo.getElement().getSimpleName());
					}
					result.append(".map(");
					switch(webSocketMessageKind) {
						case TEXT: {
							if(isCharSequence) {
								result.append("factory::text");
							}
							else {
								result.append("factory::text_raw");
							}
							break;
						}
						case BINARY: result.append("factory::binary");
							break;
						default :
							throw new IllegalStateException("Unknown WebSocket message kind");
					}
					result.append(")");
					result.append(")");
					break;
				}
				case EMPTY: {
					result.append(".messages(factory -> ");
					if(isPublisher) {
						result.append(context.getTypeName(context.getFluxType())).append(".from(").append(outboundPublisherParameterInfo.getElement().getSimpleName()).append(")");
					}
					else {
						result.append(outboundPublisherParameterInfo.getElement().getSimpleName());
					}
					result.append(".then(").append(context.getTypeName(context.getMonoType())).append(".empty()))");
					break;
				}
				case ENCODED: {
					switch(webSocketMessageKind) {
						case TEXT: result.append(".encodeTextMessages(");
							break;
						case BINARY: result.append(".encodeBinaryMessages(");
							break;
						default :
							throw new IllegalStateException("Unknown WebSocket message kind");
					}
					result.append(outboundPublisherParameterInfo.getElement().getSimpleName()).append(", ").append(VARIABLE_TYPES).append("[").append(context.getTypeIndex(outboundPublisherParameterInfo.getType())).append("])");
					break;
				}
				default: throw new IllegalStateException();
			}
			result.append(";");
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebSocketClientRouteReturnInfo returnInfo, WebClientClassGenerationContext context) {
		if(returnInfo instanceof WebSocketClientExchangeReturnInfo) {
			return this.visit((WebSocketClientExchangeReturnInfo)returnInfo, context);
		}
		else if(returnInfo instanceof WebSocketClientInboundReturnInfo) {
			return this.visit((WebSocketClientInboundReturnInfo)returnInfo, context);
		}
		else if(returnInfo instanceof WebSocketClientInboundPublisherInfo) {
			return this.visit((WebSocketClientInboundPublisherInfo)returnInfo, context);
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebSocketClientExchangeReturnInfo exchangeReturnInfo, WebClientClassGenerationContext context) {
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebSocketClientInboundReturnInfo inboundReturnInfo, WebClientClassGenerationContext context) {
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebSocketClientInboundPublisherInfo inboundPublisherInfo, WebClientClassGenerationContext context) {
		if(context.getMode() == WebClientClassGenerationContext.GenerationMode.WS_INBOUND_PUBLISHER) {
			StringBuilder result = new StringBuilder();
			result.append(context.indent(0));

			if(Objects.requireNonNull(inboundPublisherInfo.getBoundReactiveKind()) == WebSocketBoundPublisherInfo.BoundReactiveKind.ONE) {
				result.append(".flatMap(").append(VARIABLE_INBOUND).append(" -> ");
			}
			else {
				result.append(".flatMapMany(").append(VARIABLE_INBOUND).append(" -> ");
			}

			if(inboundPublisherInfo.getBoundKind() == WebSocketBoundPublisherInfo.BoundKind.EMPTY) {
				result.append("Flux.from(").append(VARIABLE_INBOUND).append(".frames()).doOnNext(").append(context.getTypeName(context.getWebSocketFrameType())).append("::release).then()");
			}
			else if(inboundPublisherInfo.getBoundKind() == WebSocketBoundPublisherInfo.BoundKind.ENCODED) {
				if(Objects.requireNonNull(inboundPublisherInfo.getBoundReactiveKind()) == WebSocketBoundPublisherInfo.BoundReactiveKind.ONE) {
					result.append(context.getTypeName(context.getMonoType())).append(".from(");
				}
				result.append(VARIABLE_INBOUND).append(".<").append(context.getTypeName(inboundPublisherInfo.getType()));
				switch(((WebSocketClientRouteInfo)context.getWebClientRouteInfo()).getMessageType()) {
					case BINARY: {
						result.append(">decodeBinaryMessages(");
						break;
					}
					case TEXT: {
						result.append(">decodeTextMessages(");
						break;
					}
					default: throw new IllegalStateException();
				}
				result.append(VARIABLE_TYPES).append("[").append(context.getTypeIndex(inboundPublisherInfo.getType())).append("])");
				if(Objects.requireNonNull(inboundPublisherInfo.getBoundReactiveKind()) == WebSocketBoundPublisherInfo.BoundReactiveKind.ONE) {
					result.append(")");
				}
			}
			else {
				if(Objects.requireNonNull(inboundPublisherInfo.getBoundReactiveKind()) == WebSocketBoundPublisherInfo.BoundReactiveKind.ONE) {
					result.append(context.getTypeName(context.getMonoType()));
				}
				else {
					result.append(context.getTypeName(context.getFluxType()));
				}
				result.append(".from(").append(VARIABLE_INBOUND);
				switch(((WebSocketClientRouteInfo)context.getWebClientRouteInfo()).getMessageType()) {
					case BINARY: {
						result.append(".binaryMessages())");
						break;
					}
					case TEXT: {
						result.append(".textMessages())");
						break;
					}
					default: throw new IllegalStateException();
				}

				if(inboundPublisherInfo.getBoundKind() == WebSocketBoundPublisherInfo.BoundKind.RAW_PUBLISHER || inboundPublisherInfo.getBoundKind() == WebSocketBoundPublisherInfo.BoundKind.RAW_MANY) {
					result.append(".map(").append(context.getTypeName(context.getWebSocketMessageType())).append("::raw)");
					if(inboundPublisherInfo.getBoundKind() == WebSocketBoundPublisherInfo.BoundKind.RAW_MANY) {
						result.append(".map(").append(context.getTypeName(context.getFluxType())).append("::from)");
					}
				}
				else if(inboundPublisherInfo.getBoundKind() == WebSocketBoundPublisherInfo.BoundKind.RAW_REDUCED_ONE) {
					result.append(".map(").append(context.getTypeName(context.getWebSocketMessageType())).append("::rawReduced)");
				}
				else if(inboundPublisherInfo.getBoundKind() == WebSocketBoundPublisherInfo.BoundKind.RAW_REDUCED) {
					result.append(".flatMap(").append(context.getTypeName(context.getWebSocketMessageType())).append("::rawReduced)");
				}
				else if(inboundPublisherInfo.getBoundKind() == WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_PUBLISHER || inboundPublisherInfo.getBoundKind() == WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_MANY) {
					result.append(".map(").append(context.getTypeName(context.getWebSocketMessageType())).append("::string)");
					if(inboundPublisherInfo.getBoundKind() == WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_MANY) {
						result.append(".map(").append(context.getTypeName(context.getFluxType())).append("::from)");
					}
				}
				else if(inboundPublisherInfo.getBoundKind() == WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_REDUCED_ONE) {
					result.append(".map(").append(context.getTypeName(context.getWebSocketMessageType())).append("::stringReduced)");
				}
				else if(inboundPublisherInfo.getBoundKind() == WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_REDUCED) {
					result.append(".flatMap(").append(context.getTypeName(context.getWebSocketMessageType())).append("::stringReduced)");
				}
			}
			result.append(")");

			/*if(inboundPublisherInfo.getBoundKind() == WebSocketBoundPublisherInfo.BoundKind.EMPTY) {
				result.append(".then()");
				if(inboundPublisherInfo.getBoundReactiveKind() == WebSocketBoundPublisherInfo.BoundReactiveKind.MANY) {
					result.append(".flux()");
				}
			}
			else {
				if(Objects.requireNonNull(inboundPublisherInfo.getBoundReactiveKind()) == WebSocketBoundPublisherInfo.BoundReactiveKind.ONE) {
					result.append(".flatMap(").append(VARIABLE_INBOUND).append(" -> ");
				}
				else {
					result.append(".flatMapMany(").append(VARIABLE_INBOUND).append(" -> ");
				}

				if(inboundPublisherInfo.getBoundKind() == WebSocketBoundPublisherInfo.BoundKind.ENCODED) {
					if(Objects.requireNonNull(inboundPublisherInfo.getBoundReactiveKind()) == WebSocketBoundPublisherInfo.BoundReactiveKind.ONE) {
						result.append(context.getTypeName(context.getMonoType())).append(".from(");
					}
					result.append(VARIABLE_INBOUND).append(".<").append(context.getTypeName(inboundPublisherInfo.getType()));
					switch(((WebSocketClientRouteInfo)context.getWebClientRouteInfo()).getMessageType()) {
						case BINARY: {
							result.append(">decodeBinaryMessages(");
							break;
						}
						case TEXT: {
							result.append(">decodeTextMessages(");
							break;
						}
						default: throw new IllegalStateException();
					}
					result.append(VARIABLE_TYPES).append("[").append(context.getTypeIndex(inboundPublisherInfo.getType())).append("])");
					if(Objects.requireNonNull(inboundPublisherInfo.getBoundReactiveKind()) == WebSocketBoundPublisherInfo.BoundReactiveKind.ONE) {
						result.append(")");
					}
				}
				else {
					if(Objects.requireNonNull(inboundPublisherInfo.getBoundReactiveKind()) == WebSocketBoundPublisherInfo.BoundReactiveKind.ONE) {
						result.append(context.getTypeName(context.getMonoType()));
					}
					else {
						result.append(context.getTypeName(context.getFluxType()));
					}
					result.append(".from(").append(VARIABLE_INBOUND);
					switch(((WebSocketClientRouteInfo)context.getWebClientRouteInfo()).getMessageType()) {
						case BINARY: {
							result.append(".binaryMessages())");
							break;
						}
						case TEXT: {
							result.append(".textMessages())");
							break;
						}
						default: throw new IllegalStateException();
					}

					if(inboundPublisherInfo.getBoundKind() == WebSocketBoundPublisherInfo.BoundKind.RAW_PUBLISHER || inboundPublisherInfo.getBoundKind() == WebSocketBoundPublisherInfo.BoundKind.RAW_MANY) {
						result.append(".map(").append(context.getTypeName(context.getWebSocketMessageType())).append("::raw)");
						if(inboundPublisherInfo.getBoundKind() == WebSocketBoundPublisherInfo.BoundKind.RAW_MANY) {
							result.append(".map(").append(context.getTypeName(context.getFluxType())).append("::from)");
						}
					}
					else if(inboundPublisherInfo.getBoundKind() == WebSocketBoundPublisherInfo.BoundKind.RAW_REDUCED_ONE) {
						result.append(".map(").append(context.getTypeName(context.getWebSocketMessageType())).append("::rawReduced)");
					}
					else if(inboundPublisherInfo.getBoundKind() == WebSocketBoundPublisherInfo.BoundKind.RAW_REDUCED) {
						result.append(".flatMap(").append(context.getTypeName(context.getWebSocketMessageType())).append("::rawReduced)");
					}
					else if(inboundPublisherInfo.getBoundKind() == WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_PUBLISHER || inboundPublisherInfo.getBoundKind() == WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_MANY) {
						result.append(".map(").append(context.getTypeName(context.getWebSocketMessageType())).append("::string)");
						if(inboundPublisherInfo.getBoundKind() == WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_MANY) {
							result.append(".map(").append(context.getTypeName(context.getFluxType())).append("::from)");
						}
					}
					else if(inboundPublisherInfo.getBoundKind() == WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_REDUCED_ONE) {
						result.append(".map(").append(context.getTypeName(context.getWebSocketMessageType())).append("::stringReduced)");
					}
					else if(inboundPublisherInfo.getBoundKind() == WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_REDUCED) {
						result.append(".flatMap(").append(context.getTypeName(context.getWebSocketMessageType())).append("::stringReduced)");
					}
				}
				result.append(")");
			}*/
			return result;
		}
		/*
		.flatMapMany(inbound -> inbound.decodeTextMessages(Message.class));
		.flatMapMany(inbound -> inbound.decodeTextMessages(Message.class));
		.flatMap(inbound -> Mono.from(inbound.decodeTextMessages(Message.class)));

		.flatMapMany(inbound -> Flux.from(inbound.textMessages()).map(WebSocketMessage::binary));
		.flatMapMany(inbound -> Flux.from(inbound.textMessages()).flatMap(WebSocketMessage::reducedBinary));
		.flatMapMany(inbound -> Flux.from(inbound.textMessages()).map(WebSocketMessage::text));
		.flatMapMany(inbound -> Flux.from(inbound.textMessages()).flatMap(WebSocketMessage::reducedText));
		.flatMapMany(inbound -> inbound.decodeTextMessages(Message.class));
		.then();
		.flatMapMany(inbound -> Flux.from(inbound.textMessages()).map(WebSocketMessage::binary).map(Flux::from));
		.flatMapMany(inbound -> Flux.from(inbound.textMessages()).flatMap(WebSocketMessage::reducedBinary));
		.flatMapMany(inbound -> Flux.from(inbound.textMessages()).map(WebSocketMessage::text).map(Flux::from));
		.flatMapMany(inbound -> Flux.from(inbound.textMessages()).flatMap(WebSocketMessage::reducedText));
		.flatMapMany(inbound -> inbound.decodeTextMessages(Message.class));
		.then().flux();
		.flatMap(inbound -> Mono.from(inbound.textMessages()).map(WebSocketMessage::reducedBinary));
		.flatMap(inbound -> Mono.from(inbound.textMessages()).flatMap(WebSocketMessage::reducedBinary));
		.flatMap(inbound -> Mono.from(inbound.textMessages()).map(WebSocketMessage::reducedText));
		.flatMap(inbound -> Mono.from(inbound.textMessages()).flatMap(WebSocketMessage::reducedText));
		.flatMap(inbound -> Mono.from(inbound.decodeTextMessages(Message.class)));
		.then();
		 */
		return new StringBuilder();
	}
}

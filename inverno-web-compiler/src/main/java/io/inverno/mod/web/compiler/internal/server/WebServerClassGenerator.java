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
package io.inverno.mod.web.compiler.internal.server;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Init;
import io.inverno.core.annotation.Mutator;
import io.inverno.core.annotation.Wrapper;
import io.inverno.mod.base.net.URIs;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.ws.WebSocketMessage;
import io.inverno.mod.web.compiler.spi.RequestBodyKind;
import io.inverno.mod.web.compiler.spi.RequestBodyReactiveKind;
import io.inverno.mod.web.compiler.spi.ResponseBodyKind;
import io.inverno.mod.web.compiler.spi.ResponseBodyReactiveKind;
import io.inverno.mod.web.compiler.spi.WebBasicParameterInfo;
import io.inverno.mod.web.compiler.spi.WebCookieParameterInfo;
import io.inverno.mod.web.compiler.spi.WebFormParameterInfo;
import io.inverno.mod.web.compiler.spi.WebHeaderParameterInfo;
import io.inverno.mod.web.compiler.spi.WebParameterInfo;
import io.inverno.mod.web.compiler.spi.WebPathParameterInfo;
import io.inverno.mod.web.compiler.spi.WebQueryParameterInfo;
import io.inverno.mod.web.compiler.spi.WebRequestBodyParameterInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerResponseBodyInfo;
import io.inverno.mod.web.compiler.spi.WebSocketBoundPublisherInfo;
import io.inverno.mod.web.compiler.spi.WebSocketExchangeParameterInfo;
import io.inverno.mod.web.compiler.spi.server.WebSocketServerInboundParameterInfo;
import io.inverno.mod.web.compiler.spi.server.WebSocketServerInboundPublisherParameterInfo;
import io.inverno.mod.web.compiler.spi.WebSocketOutboundParameterInfo;
import io.inverno.mod.web.compiler.spi.server.WebSocketServerOutboundPublisherInfo;
import io.inverno.mod.web.compiler.spi.WebSocketParameterInfo;
import io.inverno.mod.web.compiler.spi.server.ErrorWebServerRouteInterceptorConfigurerInfo;
import io.inverno.mod.web.compiler.spi.server.ErrorWebServerRouterConfigurerInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerControllerInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerConfigurerInfo;
import io.inverno.mod.web.compiler.spi.WebExchangeContextParameterInfo;
import io.inverno.mod.web.compiler.spi.WebExchangeParameterInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerModuleInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerModuleInfoVisitor;
import io.inverno.mod.web.compiler.spi.server.WebServerRouteInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerRouteInterceptorConfigurerInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerRouterConfigurerInfo;
import io.inverno.mod.web.compiler.spi.server.WebSocketServerRouteInfo;
import io.inverno.mod.web.compiler.spi.server.WebSseEventFactoryParameterInfo;
import io.inverno.mod.web.server.ErrorWebRouteInterceptor;
import io.inverno.mod.web.server.ErrorWebRouter;
import io.inverno.mod.web.server.WebRouteInterceptor;
import io.inverno.mod.web.server.WebRouter;
import io.inverno.mod.web.server.WebServer;
import io.inverno.mod.web.server.annotation.WebRoutes;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.apache.commons.text.StringEscapeUtils;

/**
 * <p>
 * Web server generator used to generate boot and <i>cascading</i> Web server classes.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class WebServerClassGenerator implements WebServerModuleInfoVisitor<StringBuilder, WebServerClassGenerationContext> {

	private static final String VARIABLE_TYPES = "_TYPES";

	@Override
	public StringBuilder visit(WebServerModuleInfo serverModuleInfo, WebServerClassGenerationContext context) {
		WebServerClassGenerationContext moduleContext = context.withWebServerModuleInfo(serverModuleInfo);
		String webServerClassName = serverModuleInfo.getQualifiedName().getClassName();
		String webServerPackageName = webServerClassName.lastIndexOf(".") != -1 ? webServerClassName.substring(0, webServerClassName.lastIndexOf(".")) : "";
		webServerClassName = webServerClassName.substring(webServerPackageName.length() + 1);
		if(moduleContext.getMode() == WebServerClassGenerationContext.GenerationMode.BOOT_SERVER_CLASS) {
			TypeMirror generatedType = moduleContext.getElementUtils().getTypeElement(moduleContext.getElementUtils().getModuleElement("java.compiler"), "javax.annotation.processing.Generated").asType();
			TypeMirror beanAnnotationType = moduleContext.getElementUtils().getTypeElement(Bean.class.getCanonicalName()).asType();
			TypeMirror wrapperAnnotationType = moduleContext.getElementUtils().getTypeElement(Wrapper.class.getCanonicalName()).asType();
			TypeMirror supplierType = moduleContext.getTypeUtils().erasure(moduleContext.getElementUtils().getTypeElement(Supplier.class.getCanonicalName()).asType());
			TypeMirror initType = moduleContext.getElementUtils().getTypeElement(Init.class.getCanonicalName()).asType();
			TypeMirror webServerType = moduleContext.getTypeUtils().erasure(moduleContext.getElementUtils().getTypeElement(WebServer.class.getCanonicalName()).asType());
			TypeMirror webRouteInterceptorType = moduleContext.getTypeUtils().erasure(moduleContext.getElementUtils().getTypeElement(WebRouteInterceptor.class.getCanonicalName()).asType());
			TypeMirror webRouterType = moduleContext.getTypeUtils().erasure(moduleContext.getElementUtils().getTypeElement(WebRouter.class.getCanonicalName()).asType());
			TypeMirror errorWebRouteInterceptorType = moduleContext.getTypeUtils().erasure(moduleContext.getElementUtils().getTypeElement(ErrorWebRouteInterceptor.class.getCanonicalName()).asType());
			TypeMirror errorWebRouterType = moduleContext.getTypeUtils().erasure(moduleContext.getElementUtils().getTypeElement(ErrorWebRouter.class.getCanonicalName()).asType());

			moduleContext.addImport(webServerClassName, webServerPackageName + "." + webServerClassName);
			moduleContext.addImport("Context", webServerPackageName + "." + webServerClassName + ".Context");
			moduleContext.addImport("ContextImpl", webServerPackageName + "." + webServerClassName + ".ContextImpl");

			StringBuilder webRoutesAnnotation = this.visit(serverModuleInfo, moduleContext.withIndentDepth(1).withMode(WebServerClassGenerationContext.GenerationMode.WEB_ROUTES_ANNOTATION));

			StringBuilder typesField = new StringBuilder(moduleContext.indent(1)).append("private static final ").append(moduleContext.getTypeName(moduleContext.getTypeType())).append("[] ").append(VARIABLE_TYPES).append(" = new ").append(moduleContext.getTypeName(moduleContext.getTypeType())).append("[] {").append(System.lineSeparator());
			typesField.append(Arrays.stream(serverModuleInfo.getTypesRegistry()).map(type ->  new StringBuilder(moduleContext.indent(2)).append(moduleContext.getTypeGenerator(type))).collect(Collectors.joining("," + System.lineSeparator()))).append(System.lineSeparator());
			typesField.append(moduleContext.indent(1)).append("};");

			StringBuilder web_controller_fields = Arrays.stream(serverModuleInfo.getControllers())
				.map(controllerInfo -> this.visit(controllerInfo, moduleContext.withIndentDepth(1).withMode(WebServerClassGenerationContext.GenerationMode.CONTROLLER_FIELD)))
				.collect(moduleContext.joining(System.lineSeparator()));

			StringBuilder webServer_parameters = Arrays.stream(serverModuleInfo.getControllers()).map(controllerInfo -> this.visit(controllerInfo, moduleContext.withIndentDepth(0).withMode(WebServerClassGenerationContext.GenerationMode.CONTROLLER_PARAMETER))).collect(moduleContext.joining(", "));

			StringBuilder interceptorsConfigurersDecl = new StringBuilder(moduleContext.getListTypeName()).append("<").append(moduleContext.getTypeName(webRouteInterceptorType)).append(".Configurer<? super ").append(webServerClassName).append(".Context>> interceptorsConfigurers");
			StringBuilder routesConfigurersDecl = new StringBuilder(moduleContext.getListTypeName()).append("<").append(moduleContext.getTypeName(webRouterType)).append(".Configurer<? super ").append(webServerClassName).append(".Context>> routesConfigurers");
			StringBuilder errorInterceptorsConfigurersDecl = new StringBuilder(moduleContext.getListTypeName()).append("<").append(moduleContext.getTypeName(errorWebRouteInterceptorType)).append(".Configurer<? super ").append(webServerClassName).append(".Context>> errorInterceptorsConfigurers");
			StringBuilder errorRoutesConfigurersDecl = new StringBuilder(moduleContext.getListTypeName()).append("<").append(moduleContext.getTypeName(errorWebRouterType)).append(".Configurer<? super ").append(webServerClassName).append(".Context>> errorRoutesConfigurers");
			StringBuilder serverConfigurersDecl = new StringBuilder(moduleContext.getListTypeName()).append("<").append(moduleContext.getTypeName(webServerType)).append(".Configurer<? super ").append(webServerClassName).append(".Context>> serverConfigurers");

			StringBuilder webServerBootField = new StringBuilder(moduleContext.indent(1)).append("private final ").append(moduleContext.getTypeName(webServerType)).append(".Boot webServerBoot;");
			StringBuilder webServerField = new StringBuilder(moduleContext.indent(1)).append("private ").append(moduleContext.getTypeName(webServerType)).append("<").append(webServerClassName).append(".Context> webServer;");
			StringBuilder interceptorsConfigurersField = new StringBuilder(moduleContext.indent(1)).append("private ").append(interceptorsConfigurersDecl).append(";");
			StringBuilder routesConfigurersField = new StringBuilder(moduleContext.indent(1)).append("private ").append(routesConfigurersDecl).append(";");
			StringBuilder errorInterceptorsConfigurersField = new StringBuilder(moduleContext.indent(1)).append("private ").append(errorInterceptorsConfigurersDecl).append(";");
			StringBuilder errorRoutesConfigurersField = new StringBuilder(moduleContext.indent(1)).append("private ").append(errorRoutesConfigurersDecl).append(";");
			StringBuilder serverConfigurersField = new StringBuilder(moduleContext.indent(1)).append("private ").append(serverConfigurersDecl).append(";");

			StringBuilder interceptorsConfigurersSetter = new StringBuilder(moduleContext.indent(1)).append("public void setInterceptorsConfigurers(").append(interceptorsConfigurersDecl).append(") {").append(System.lineSeparator());
			interceptorsConfigurersSetter.append(moduleContext.indent(2)).append("this.interceptorsConfigurers = interceptorsConfigurers;").append(System.lineSeparator());
			interceptorsConfigurersSetter.append(moduleContext.indent(1)).append("}");

			StringBuilder routesConfigurersSetter = new StringBuilder(moduleContext.indent(1)).append("public void setRoutesConfigurers(").append(routesConfigurersDecl).append(") {").append(System.lineSeparator());
			routesConfigurersSetter.append(moduleContext.indent(2)).append("this.routesConfigurers = routesConfigurers;").append(System.lineSeparator());
			routesConfigurersSetter.append(moduleContext.indent(1)).append("}");

			StringBuilder errorInterceptorsConfigurersSetter = new StringBuilder(moduleContext.indent(1)).append("public void setErrorInterceptorsConfigurers(").append(errorInterceptorsConfigurersDecl).append(") {").append(System.lineSeparator());
			errorInterceptorsConfigurersSetter.append(moduleContext.indent(2)).append("this.errorInterceptorsConfigurers = errorInterceptorsConfigurers;").append(System.lineSeparator());
			errorInterceptorsConfigurersSetter.append(moduleContext.indent(1)).append("}");

			StringBuilder errorRoutesConfigurersSetter = new StringBuilder(moduleContext.indent(1)).append("public void setErrorRoutesConfigurers(").append(errorRoutesConfigurersDecl).append(") {").append(System.lineSeparator());
			errorRoutesConfigurersSetter.append(moduleContext.indent(2)).append("this.errorRoutesConfigurers = errorRoutesConfigurers;").append(System.lineSeparator());
			errorRoutesConfigurersSetter.append(moduleContext.indent(1)).append("}");

			StringBuilder serverConfigurersSetter = new StringBuilder(moduleContext.indent(1)).append("public void setServerConfigurers(").append(serverConfigurersDecl).append(") {").append(System.lineSeparator());
			serverConfigurersSetter.append(moduleContext.indent(2)).append("this.serverConfigurers = serverConfigurers;").append(System.lineSeparator());
			serverConfigurersSetter.append(moduleContext.indent(1)).append("}");

			StringBuilder webServer_constructor = new StringBuilder(moduleContext.indent(1)).append("public ").append(webServerClassName).append("(");
			webServer_constructor.append(moduleContext.getTypeName(webServerType)).append(".Boot webServerBoot");
			if(!webServer_parameters.isEmpty()) {
				webServer_constructor.append(", ").append(webServer_parameters);
			}
			webServer_constructor.append(") {").append(System.lineSeparator());
			webServer_constructor.append(moduleContext.indent(2)).append("this.webServerBoot = webServerBoot;");
			if(serverModuleInfo.getControllers().length > 0) {
				webServer_constructor.append(System.lineSeparator()).append(Arrays.stream(serverModuleInfo.getControllers()).map(controllerInfo -> this.visit(controllerInfo, moduleContext.withIndentDepth(2).withMode(WebServerClassGenerationContext.GenerationMode.CONTROLLER_ASSIGNMENT))).collect(moduleContext.joining(System.lineSeparator())));
			}
			webServer_constructor.append(System.lineSeparator()).append(moduleContext.indent(1)).append("}");

			StringBuilder webServer_get = new StringBuilder(moduleContext.indent(1)).append("@Override").append(System.lineSeparator());
			webServer_get.append(moduleContext.indent(1)).append("public ").append(moduleContext.getTypeName(webServerType)).append("<").append(webServerClassName).append(".Context> get() {").append(System.lineSeparator());
			webServer_get.append(moduleContext.indent(2)).append("return this.webServer;").append(System.lineSeparator());
			webServer_get.append(moduleContext.indent(1)).append("}");

			StringBuilder webServer_init = new StringBuilder(moduleContext.indent(1)).append("@").append(moduleContext.getTypeName(initType)).append(System.lineSeparator());
			webServer_init.append(moduleContext.indent(1)).append("public void init() {").append(System.lineSeparator());
			webServer_init.append(moduleContext.indent(2)).append("this.webServer = this.webServerBoot.webServer(").append(webServerClassName).append(".ContextImpl::new);").append(System.lineSeparator());
			webServer_init.append(moduleContext.indent(2)).append("this.webServer = this.webServer").append(System.lineSeparator());
			webServer_init.append(moduleContext.indent(3)).append(".configureInterceptors(this.interceptorsConfigurers)").append(System.lineSeparator());
			webServer_init.append(moduleContext.indent(3)).append(".configureErrorInterceptors(this.errorInterceptorsConfigurers)").append(System.lineSeparator());
			webServer_init.append(moduleContext.indent(3)).append(".configure(this.serverConfigurers)").append(System.lineSeparator());
			webServer_init.append(moduleContext.indent(3)).append(".configureRoutes(this.routesConfigurers)").append(System.lineSeparator());
			webServer_init.append(moduleContext.indent(3)).append(".configureErrorRoutes(this.errorRoutesConfigurers)");
			if(serverModuleInfo.getControllers().length > 0) {
				webServer_init.append(System.lineSeparator()).append(Arrays.stream(serverModuleInfo.getControllers()).map(controllerInfo -> this.visit(controllerInfo, moduleContext.withIndentDepth(3).withMode(WebServerClassGenerationContext.GenerationMode.ROUTE_DECLARATION))).collect(moduleContext.joining(System.lineSeparator())));
			}
			webServer_init.append(";").append(System.lineSeparator());
			webServer_init.append(moduleContext.indent(1)).append("}");

			StringBuilder server_context = this.visit(serverModuleInfo, moduleContext.withIndentDepth(1).withMode(WebServerClassGenerationContext.GenerationMode.SERVER_CONTEXT));

			StringBuilder server_context_impl = this.visit(serverModuleInfo, moduleContext.withIndentDepth(1).withMode(WebServerClassGenerationContext.GenerationMode.SERVER_CONTEXT_IMPL));

			StringBuilder webServer_class = new StringBuilder();

			webServer_class.append(webRoutesAnnotation).append(System.lineSeparator());
			webServer_class.append("@").append(moduleContext.getTypeName(wrapperAnnotationType)).append(" @").append(moduleContext.getTypeName(beanAnnotationType)).append("( name = \"").append(serverModuleInfo.getQualifiedName().getBeanName()).append("\", visibility = ").append(moduleContext.getTypeName(beanAnnotationType)).append(".Visibility.PRIVATE )").append(System.lineSeparator());
			webServer_class.append("@").append(moduleContext.getTypeName(generatedType)).append("(value=\"").append(WebServerCompilerPlugin.class.getCanonicalName()).append("\", date = \"").append(ZonedDateTime.now()).append("\")").append(System.lineSeparator());
			webServer_class.append("public final class ").append(webServerClassName).append(" implements ").append(moduleContext.getTypeName(supplierType)).append("<").append(moduleContext.getTypeName(webServerType)).append("<").append(webServerClassName).append(".Context>> {").append(System.lineSeparator()).append(System.lineSeparator());

			webServer_class.append(typesField).append(System.lineSeparator()).append(System.lineSeparator());

			webServer_class.append(webServerBootField).append(System.lineSeparator());
			webServer_class.append(webServerField).append(System.lineSeparator()).append(System.lineSeparator());

			webServer_class.append(interceptorsConfigurersField).append(System.lineSeparator());
			webServer_class.append(routesConfigurersField).append(System.lineSeparator());
			webServer_class.append(errorInterceptorsConfigurersField).append(System.lineSeparator());
			webServer_class.append(errorRoutesConfigurersField).append(System.lineSeparator());
			webServer_class.append(serverConfigurersField).append(System.lineSeparator()).append(System.lineSeparator());
			if(serverModuleInfo.getControllers().length > 0) {
				webServer_class.append(web_controller_fields).append(System.lineSeparator()).append(System.lineSeparator());
			}

			webServer_class.append(webServer_constructor).append(System.lineSeparator()).append(System.lineSeparator());

			webServer_class.append(webServer_init).append(System.lineSeparator()).append(System.lineSeparator());

			webServer_class.append(webServer_get).append(System.lineSeparator()).append(System.lineSeparator());

			webServer_class.append(interceptorsConfigurersSetter).append(System.lineSeparator()).append(System.lineSeparator());
			webServer_class.append(routesConfigurersSetter).append(System.lineSeparator()).append(System.lineSeparator());

			webServer_class.append(errorInterceptorsConfigurersSetter).append(System.lineSeparator()).append(System.lineSeparator());
			webServer_class.append(errorRoutesConfigurersSetter).append(System.lineSeparator()).append(System.lineSeparator());

			webServer_class.append(serverConfigurersSetter).append(System.lineSeparator()).append(System.lineSeparator());

			webServer_class.append(server_context).append(System.lineSeparator()).append(System.lineSeparator());
			webServer_class.append(server_context_impl).append(System.lineSeparator());

			webServer_class.append("}");

			moduleContext.removeImport(webServerClassName);
			moduleContext.removeImport("Context");
			moduleContext.removeImport("ContextImpl");

			webServer_class.insert(0, System.lineSeparator() + System.lineSeparator()).insert(0, moduleContext.getImports().stream().sorted().filter(i -> i.lastIndexOf(".") > 0 && !i.substring(0, i.lastIndexOf(".")).equals(webServerPackageName)).map(i -> new StringBuilder().append("import ").append(i).append(";")).collect(moduleContext.joining(System.lineSeparator())));
			if(!webServerPackageName.isEmpty()) {
				webServer_class.insert(0, ";" + System.lineSeparator() + System.lineSeparator()).insert(0, webServerPackageName).insert(0, "package ");
			}
			return webServer_class;
		}
		else if(moduleContext.getMode() == WebServerClassGenerationContext.GenerationMode.SERVER_CLASS) {
			TypeMirror generatedType = moduleContext.getElementUtils().getTypeElement(moduleContext.getElementUtils().getModuleElement("java.compiler"), "javax.annotation.processing.Generated").asType();
			TypeMirror beanAnnotationType = moduleContext.getElementUtils().getTypeElement(Bean.class.getCanonicalName()).asType();
			TypeMirror mutatorAnnotationType = moduleContext.getElementUtils().getTypeElement(Mutator.class.getCanonicalName()).asType();
			TypeMirror functionType = moduleContext.getTypeUtils().erasure(moduleContext.getElementUtils().getTypeElement(Function.class.getCanonicalName()).asType());
			TypeMirror webServerType = moduleContext.getTypeUtils().erasure(moduleContext.getElementUtils().getTypeElement(WebServer.class.getCanonicalName()).asType());
			TypeMirror webRouteInterceptorType = moduleContext.getTypeUtils().erasure(moduleContext.getElementUtils().getTypeElement(WebRouteInterceptor.class.getCanonicalName()).asType());
			TypeMirror webRouterType = moduleContext.getTypeUtils().erasure(moduleContext.getElementUtils().getTypeElement(WebRouter.class.getCanonicalName()).asType());
			TypeMirror errorWebRouteInterceptorType = moduleContext.getTypeUtils().erasure(moduleContext.getElementUtils().getTypeElement(ErrorWebRouteInterceptor.class.getCanonicalName()).asType());
			TypeMirror errorWebRouterType = moduleContext.getTypeUtils().erasure(moduleContext.getElementUtils().getTypeElement(ErrorWebRouter.class.getCanonicalName()).asType());

			moduleContext.addImport(webServerClassName, webServerPackageName + "." + webServerClassName);
			moduleContext.addImport("Context", webServerPackageName + "." + webServerClassName + ".Context");

			StringBuilder webRoutesAnnotation = this.visit(serverModuleInfo, moduleContext.withIndentDepth(1).withMode(WebServerClassGenerationContext.GenerationMode.WEB_ROUTES_ANNOTATION));

			StringBuilder webServer_type_argument = new StringBuilder(moduleContext.getTypeName(webServerType)).append("<").append(webServerClassName).append(".Context>");
			StringBuilder webServer_upper_type_argument = new StringBuilder(moduleContext.getTypeName(webServerType)).append("<? extends ").append(webServerClassName).append(".Context>");

			StringBuilder typesField = new StringBuilder(moduleContext.indent(1)).append("private static final ").append(moduleContext.getTypeName(moduleContext.getTypeType())).append("[] ").append(VARIABLE_TYPES).append(" = new ").append(moduleContext.getTypeName(moduleContext.getTypeType())).append("[] {").append(System.lineSeparator());
			typesField.append(Arrays.stream(serverModuleInfo.getTypesRegistry()).map(type ->  new StringBuilder(moduleContext.indent(2)).append(moduleContext.getTypeGenerator(type))).collect(Collectors.joining("," + System.lineSeparator()))).append(System.lineSeparator());
			typesField.append(moduleContext.indent(1)).append("};");

			StringBuilder web_controller_fields = Arrays.stream(serverModuleInfo.getControllers())
				.map(controllerInfo -> this.visit(controllerInfo, moduleContext.withIndentDepth(1).withMode(WebServerClassGenerationContext.GenerationMode.CONTROLLER_FIELD)))
				.collect(moduleContext.joining(System.lineSeparator()));

			StringBuilder webServer_parameters = Arrays.stream(serverModuleInfo.getControllers()).map(controllerInfo -> this.visit(controllerInfo, moduleContext.withIndentDepth(0).withMode(WebServerClassGenerationContext.GenerationMode.CONTROLLER_PARAMETER))).collect(moduleContext.joining(", "));

			StringBuilder interceptorsConfigurersDecl = new StringBuilder(moduleContext.getListTypeName()).append("<").append(moduleContext.getTypeName(webRouteInterceptorType)).append(".Configurer<? super ").append(webServerClassName).append(".Context>> interceptorsConfigurers");
			StringBuilder routesConfigurersDecl = new StringBuilder(moduleContext.getListTypeName()).append("<").append(moduleContext.getTypeName(webRouterType)).append(".Configurer<? super ").append(webServerClassName).append(".Context>> routesConfigurers");
			StringBuilder errorInterceptorsConfigurersDecl = new StringBuilder(moduleContext.getListTypeName()).append("<").append(moduleContext.getTypeName(errorWebRouteInterceptorType)).append(".Configurer<? super ").append(webServerClassName).append(".Context>> errorInterceptorsConfigurers");
			StringBuilder errorRoutesConfigurersDecl = new StringBuilder(moduleContext.getListTypeName()).append("<").append(moduleContext.getTypeName(errorWebRouterType)).append(".Configurer<? super ").append(webServerClassName).append(".Context>> errorRoutesConfigurers");
			StringBuilder serverConfigurersDecl = new StringBuilder(moduleContext.getListTypeName()).append("<").append(moduleContext.getTypeName(webServerType)).append(".Configurer<? super ").append(webServerClassName).append(".Context>> serverConfigurers");

			StringBuilder interceptorsConfigurersField = new StringBuilder(moduleContext.indent(1)).append("private ").append(interceptorsConfigurersDecl).append(";");
			StringBuilder routesConfigurersField = new StringBuilder(moduleContext.indent(1)).append("private ").append(routesConfigurersDecl).append(";");
			StringBuilder errorInterceptorsConfigurersField = new StringBuilder(moduleContext.indent(1)).append("private ").append(errorInterceptorsConfigurersDecl).append(";");
			StringBuilder errorRoutesConfigurersField = new StringBuilder(moduleContext.indent(1)).append("private ").append(errorRoutesConfigurersDecl).append(";");
			StringBuilder serverConfigurersField = new StringBuilder(moduleContext.indent(1)).append("private ").append(serverConfigurersDecl).append(";");

			StringBuilder interceptorsConfigurersSetter = new StringBuilder(moduleContext.indent(1)).append("public void setInterceptorsConfigurers(").append(interceptorsConfigurersDecl).append(") {").append(System.lineSeparator());
			interceptorsConfigurersSetter.append(moduleContext.indent(2)).append("this.interceptorsConfigurers = interceptorsConfigurers;").append(System.lineSeparator());
			interceptorsConfigurersSetter.append(moduleContext.indent(1)).append("}");

			StringBuilder routesConfigurersSetter = new StringBuilder(moduleContext.indent(1)).append("public void setRoutesConfigurers(").append(routesConfigurersDecl).append(") {").append(System.lineSeparator());
			routesConfigurersSetter.append(moduleContext.indent(2)).append("this.routesConfigurers = routesConfigurers;").append(System.lineSeparator());
			routesConfigurersSetter.append(moduleContext.indent(1)).append("}");

			StringBuilder errorInterceptorsConfigurersSetter = new StringBuilder(moduleContext.indent(1)).append("public void setErrorInterceptorsConfigurers(").append(errorInterceptorsConfigurersDecl).append(") {").append(System.lineSeparator());
			errorInterceptorsConfigurersSetter.append(moduleContext.indent(2)).append("this.errorInterceptorsConfigurers = errorInterceptorsConfigurers;").append(System.lineSeparator());
			errorInterceptorsConfigurersSetter.append(moduleContext.indent(1)).append("}");

			StringBuilder errorRoutesConfigurersSetter = new StringBuilder(moduleContext.indent(1)).append("public void setErrorRoutesConfigurers(").append(errorRoutesConfigurersDecl).append(") {").append(System.lineSeparator());
			errorRoutesConfigurersSetter.append(moduleContext.indent(2)).append("this.errorRoutesConfigurers = errorRoutesConfigurers;").append(System.lineSeparator());
			errorRoutesConfigurersSetter.append(moduleContext.indent(1)).append("}");

			StringBuilder serverConfigurersSetter = new StringBuilder(moduleContext.indent(1)).append("public void setServerConfigurers(").append(serverConfigurersDecl).append(") {").append(System.lineSeparator());
			serverConfigurersSetter.append(moduleContext.indent(2)).append("this.serverConfigurers = serverConfigurers;").append(System.lineSeparator());
			serverConfigurersSetter.append(moduleContext.indent(1)).append("}");

			StringBuilder webServer_constructor = new StringBuilder(moduleContext.indent(1)).append("public ").append(webServerClassName).append("(").append(webServer_parameters).append(") {").append(System.lineSeparator());
			if(serverModuleInfo.getControllers().length > 0) {
				webServer_constructor.append(Arrays.stream(serverModuleInfo.getControllers()).map(controllerInfo -> this.visit(controllerInfo, moduleContext.withIndentDepth(2).withMode(WebServerClassGenerationContext.GenerationMode.CONTROLLER_ASSIGNMENT))).collect(moduleContext.joining(System.lineSeparator())));
			}
			webServer_constructor.append(System.lineSeparator()).append(moduleContext.indent(1)).append("}");

			StringBuilder webServer_apply = new StringBuilder(moduleContext.indent(1)).append("@Override").append(System.lineSeparator());
			webServer_apply.append(moduleContext.indent(1)).append("@SuppressWarnings(\"unchecked\")").append(System.lineSeparator());
			webServer_apply.append(moduleContext.indent(1)).append("public ").append(webServer_type_argument).append(" apply(").append(webServer_upper_type_argument).append(" webServer) {").append(System.lineSeparator());
			webServer_apply.append(moduleContext.indent(2)).append("return ((").append(webServer_type_argument).append(")webServer)").append(System.lineSeparator());
			webServer_apply.append(moduleContext.indent(3)).append(".configureInterceptors(this.interceptorsConfigurers)").append(System.lineSeparator());
			webServer_apply.append(moduleContext.indent(3)).append(".configureErrorInterceptors(this.errorInterceptorsConfigurers)").append(System.lineSeparator());
			webServer_apply.append(moduleContext.indent(3)).append(".configure(this.serverConfigurers)").append(System.lineSeparator());
			webServer_apply.append(moduleContext.indent(3)).append(".configureRoutes(this.routesConfigurers)").append(System.lineSeparator());
			webServer_apply.append(moduleContext.indent(3)).append(".configureErrorRoutes(this.errorRoutesConfigurers)");
			if(serverModuleInfo.getControllers().length > 0) {
				webServer_apply.append(System.lineSeparator()).append(Arrays.stream(serverModuleInfo.getControllers()).map(controllerInfo -> this.visit(controllerInfo, moduleContext.withIndentDepth(3).withMode(WebServerClassGenerationContext.GenerationMode.ROUTE_DECLARATION))).collect(moduleContext.joining(System.lineSeparator())));
			}
			webServer_apply.append(";").append(System.lineSeparator());
			webServer_apply.append(moduleContext.indent(1)).append("}");

			StringBuilder server_context = this.visit(serverModuleInfo, moduleContext.withIndentDepth(1).withMode(WebServerClassGenerationContext.GenerationMode.SERVER_CONTEXT));

			StringBuilder webServer_class = new StringBuilder();

			webServer_class.append(webRoutesAnnotation).append(System.lineSeparator());
			webServer_class.append("@").append(moduleContext.getTypeName(mutatorAnnotationType)).append("(required = true) @").append(moduleContext.getTypeName(beanAnnotationType)).append("( name = \"").append(serverModuleInfo.getQualifiedName().getBeanName()).append("\" )").append(System.lineSeparator());
			webServer_class.append("@").append(moduleContext.getTypeName(generatedType)).append("(value=\"").append(WebServerCompilerPlugin.class.getCanonicalName()).append("\", date = \"").append(ZonedDateTime.now()).append("\")").append(System.lineSeparator());
			webServer_class.append("public final class ").append(webServerClassName).append(" implements ").append(moduleContext.getTypeName(functionType)).append("<").append(webServer_upper_type_argument).append(", ").append(webServer_type_argument).append("> {").append(System.lineSeparator()).append(System.lineSeparator());

			webServer_class.append(typesField).append(System.lineSeparator()).append(System.lineSeparator());

			webServer_class.append(interceptorsConfigurersField).append(System.lineSeparator());
			webServer_class.append(routesConfigurersField).append(System.lineSeparator());
			webServer_class.append(errorInterceptorsConfigurersField).append(System.lineSeparator());
			webServer_class.append(errorRoutesConfigurersField).append(System.lineSeparator());
			webServer_class.append(serverConfigurersField).append(System.lineSeparator()).append(System.lineSeparator());

			if(serverModuleInfo.getControllers().length > 0) {
				webServer_class.append(web_controller_fields).append(System.lineSeparator()).append(System.lineSeparator());
			}

			webServer_class.append(webServer_constructor).append(System.lineSeparator()).append(System.lineSeparator());

			webServer_class.append(webServer_apply).append(System.lineSeparator()).append(System.lineSeparator());

			webServer_class.append(interceptorsConfigurersSetter).append(System.lineSeparator()).append(System.lineSeparator());
			webServer_class.append(routesConfigurersSetter).append(System.lineSeparator()).append(System.lineSeparator());

			webServer_class.append(errorInterceptorsConfigurersSetter).append(System.lineSeparator()).append(System.lineSeparator());
			webServer_class.append(errorRoutesConfigurersSetter).append(System.lineSeparator()).append(System.lineSeparator());

			webServer_class.append(serverConfigurersSetter).append(System.lineSeparator()).append(System.lineSeparator());

			webServer_class.append(server_context).append(System.lineSeparator());

			webServer_class.append("}");

			moduleContext.removeImport(webServerClassName);
			moduleContext.removeImport("Context");

			webServer_class.insert(0, System.lineSeparator() + System.lineSeparator()).insert(0, moduleContext.getImports().stream().sorted().filter(i -> i.lastIndexOf(".") > 0 && !i.substring(0, i.lastIndexOf(".")).equals(webServerPackageName)).map(i -> new StringBuilder().append("import ").append(i).append(";")).collect(moduleContext.joining(System.lineSeparator())));
			if(!webServerPackageName.isEmpty()) {
				webServer_class.insert(0, ";" + System.lineSeparator() + System.lineSeparator()).insert(0, webServerPackageName).insert(0, "package ");
			}
			return webServer_class;
		}
		else if(moduleContext.getMode() == WebServerClassGenerationContext.GenerationMode.WEB_ROUTES_ANNOTATION) {
			// all routes from: the server info, route configurer, server configurer
			TypeMirror webRoutesAnnotationType = moduleContext.getElementUtils().getTypeElement(WebRoutes.class.getCanonicalName()).asType();
			StringBuilder result = new StringBuilder();
			result.append("@").append(moduleContext.getTypeName(webRoutesAnnotationType));

			WebServerClassGenerationContext webSocketRouteAnnotationContext = moduleContext.withMode(WebServerClassGenerationContext.GenerationMode.WEBSOCKET_ROUTE_ANNOTATION).withIndentDepth(1);
			StringBuilder webSocketRoutes = Stream.of(
					Arrays.stream(serverModuleInfo.getRouterConfigurers()).map(configurerInfo -> this.visit(configurerInfo, webSocketRouteAnnotationContext)),
					Arrays.stream(serverModuleInfo.getServerConfigurers()).map(configurerInfo -> this.visit(configurerInfo, webSocketRouteAnnotationContext)),
					Arrays.stream(serverModuleInfo.getControllers())
						.flatMap(controllerInfo -> Arrays.stream(controllerInfo.getRoutes()).map(routeInfo -> this.visit(routeInfo, webSocketRouteAnnotationContext.withWebServerControllerInfo(controllerInfo))))
				)
				.flatMap(Function.identity())
				.filter(s -> !s.isEmpty())
				.collect(moduleContext.joining("," + System.lineSeparator()));

			WebServerClassGenerationContext webRouteAnnotationContext;
			if(webSocketRoutes.isEmpty()) {
				webRouteAnnotationContext = moduleContext.withMode(WebServerClassGenerationContext.GenerationMode.ROUTE_ANNOTATION);
			}
			else {
				webRouteAnnotationContext = moduleContext.withMode(WebServerClassGenerationContext.GenerationMode.ROUTE_ANNOTATION).withIndentDepthAdd(1);
			}
			StringBuilder webRoutes = Stream.of(
					Arrays.stream(serverModuleInfo.getRouterConfigurers()).map(routesConfigurerInfo -> this.visit(routesConfigurerInfo, webRouteAnnotationContext)),
					Arrays.stream(serverModuleInfo.getServerConfigurers()).map(routerConfigurerInfo -> this.visit(routerConfigurerInfo, webRouteAnnotationContext)),
					Arrays.stream(serverModuleInfo.getControllers())
						.flatMap(controllerInfo -> Arrays.stream(controllerInfo.getRoutes()).map(routeInfo -> this.visit(routeInfo, webRouteAnnotationContext.withWebServerControllerInfo(controllerInfo))))
				)
				.flatMap(Function.identity())
				.filter(s -> !s.isEmpty())
				.collect(moduleContext.joining("," + System.lineSeparator()));

			if(webSocketRoutes.isEmpty()) {
				result.append("({").append(System.lineSeparator());
				result.append(webRoutes);
				result.append(System.lineSeparator()).append("})");
			}
			else {
				result.append("(").append(System.lineSeparator());

				result.append(moduleContext.indent(0)).append("value = {").append(System.lineSeparator());
				result.append(webRoutes).append(System.lineSeparator());
				result.append(moduleContext.indent(0)).append("},").append(System.lineSeparator());

				result.append(moduleContext.indent(0)).append("webSockets = {").append(System.lineSeparator());
				result.append(webSocketRoutes).append(System.lineSeparator());
				result.append(moduleContext.indent(0)).append("}").append(System.lineSeparator());

				result.append(")");
			}
			return result;
		}
		else if(moduleContext.getMode() == WebServerClassGenerationContext.GenerationMode.SERVER_CONTEXT) {
			StringBuilder result = new StringBuilder();
			result.append(moduleContext.indent(0)).append("public interface Context extends ");
			if(serverModuleInfo.getContextTypes().length > 0) {
				result.append(Arrays.stream(serverModuleInfo.getContextTypes()).map(moduleContext::getTypeName).collect(Collectors.joining(", ")));
			}
			else {
				result.append(moduleContext.getTypeName(moduleContext.getElementUtils().getTypeElement(ExchangeContext.class.getCanonicalName()).asType()));
			}
			result.append(" {}");

			return result;
		}
		else if(moduleContext.getMode() == WebServerClassGenerationContext.GenerationMode.SERVER_CONTEXT_IMPL) {
			// For each moduleContext type I must define the methods in order to implement the moduleContext
			// This can actually be tricky because some interface may override others
			// Let's start by listing all methods

			Map<String, String> context_fields = new HashMap<>();
			Map<String, String> context_methods = new HashMap<>();
			Set<String> defaultMethods = new HashSet<>();
			for(TypeMirror contextType : serverModuleInfo.getContextTypes()) {
				ElementFilter.methodsIn(moduleContext.getElementUtils().getAllMembers((TypeElement) moduleContext.getTypeUtils().asElement(contextType))).stream()
					.filter(exectuableElement -> exectuableElement.getEnclosingElement().getKind() == ElementKind.INTERFACE && !exectuableElement.getModifiers().contains(Modifier.STATIC))
					.forEach(exectuableElement -> {
						ExecutableType executableType =  (ExecutableType) moduleContext.getTypeUtils().asMemberOf((DeclaredType)contextType, exectuableElement);

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
			context_impl.append(moduleContext.indent(0)).append("private static class ContextImpl implements ").append(webServerClassName).append(".Context {").append(System.lineSeparator());
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
	public StringBuilder visit(WebServerRouteInterceptorConfigurerInfo interceptorsConfigurerInfo, WebServerClassGenerationContext context) {
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(ErrorWebServerRouteInterceptorConfigurerInfo errorInterceptorsConfigurerInfo, WebServerClassGenerationContext context) {
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(ErrorWebServerRouterConfigurerInfo errorRoutesConfigurerInfo, WebServerClassGenerationContext context) {
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebServerConfigurerInfo serverConfigurerInfo, WebServerClassGenerationContext context) {
		if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_ANNOTATION || context.getMode() == WebServerClassGenerationContext.GenerationMode.WEBSOCKET_ROUTE_ANNOTATION) {
			return Arrays.stream(serverConfigurerInfo.getRoutes())
				.map(routeInfo -> this.visit(routeInfo, context))
				.filter(s -> !s.isEmpty())
				.collect(context.joining("," + System.lineSeparator()));
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebServerRouterConfigurerInfo routesConfigurerInfo, WebServerClassGenerationContext context) {
		if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_ANNOTATION || context.getMode() == WebServerClassGenerationContext.GenerationMode.WEBSOCKET_ROUTE_ANNOTATION) {
			return Arrays.stream(routesConfigurerInfo.getRoutes())
				.map(routeInfo -> this.visit(routeInfo, context))
				.filter(s -> !s.isEmpty())
				.collect(context.joining("," + System.lineSeparator()));
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebServerControllerInfo controllerInfo, WebServerClassGenerationContext context) {
		if(context.getMode() == WebServerClassGenerationContext.GenerationMode.CONTROLLER_FIELD) {
			return new StringBuilder(context.indent(0)).append("private final ").append(context.getTypeName(controllerInfo.getType())).append(" ").append(context.getFieldName(controllerInfo.getQualifiedName())).append(";");
		}
		else if(context.getMode() == WebServerClassGenerationContext.GenerationMode.CONTROLLER_PARAMETER) {
			return new StringBuilder(context.indent(0)).append(context.getTypeName(controllerInfo.getType())).append(" ").append(context.getFieldName(controllerInfo.getQualifiedName()));
		}
		else if(context.getMode() == WebServerClassGenerationContext.GenerationMode.CONTROLLER_ASSIGNMENT) {
			return new StringBuilder(context.indent(0)).append("this.").append(context.getFieldName(controllerInfo.getQualifiedName())).append(" = ").append(context.getFieldName(controllerInfo.getQualifiedName())).append(";");
		}
		else if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_DECLARATION) {
			return Arrays.stream(controllerInfo.getRoutes()).map(routeInfo -> this.visit(routeInfo, context.withWebServerControllerInfo(controllerInfo))).collect(context.joining(System.lineSeparator()));
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebServerRouteInfo routeInfo, WebServerClassGenerationContext context) {
		WebServerClassGenerationContext routeContext = context.withWebServerRouteInfo(routeInfo);
		if(routeInfo instanceof WebSocketServerRouteInfo) {
			return this.visit((WebSocketServerRouteInfo)routeInfo, routeContext);
		}
		else {
			if(routeContext.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_ANNOTATION) {
				StringBuilder result = new StringBuilder();
				result.append(routeContext.indent(0)).append("@").append(routeContext.getWebRouteAnnotationTypeName()).append("(");

				result.append("path = { ");
				if(routeInfo.getPaths().length > 0) {
					result.append(Arrays.stream(routeInfo.getPaths())
						.map(path -> "\"" + StringEscapeUtils.escapeJava(routeInfo.getController()
							.map(WebServerControllerInfo::getRootPath)
							.map(rootPath -> URIs.uri(rootPath, URIs.RequestTargetForm.PATH, URIs.Option.PARAMETERIZED, URIs.Option.NORMALIZED, URIs.Option.PATH_PATTERN).path(path, false).buildRawPath())
							.orElse(path)) + "\""
						)
						.collect(Collectors.joining(", "))
					);
				}
				else {
					routeInfo.getController()
						.map(WebServerControllerInfo::getRootPath)
						.map(rootPath -> URIs.uri(rootPath, URIs.RequestTargetForm.PATH, URIs.Option.PARAMETERIZED, URIs.Option.NORMALIZED, URIs.Option.PATH_PATTERN).buildRawPath())
						.ifPresent(rootPath -> result.append("\"").append(rootPath).append("\""));
				}
				result.append(" }");

				if(routeInfo.isMatchTrailingSlash()) {
					result.append(", matchTrailingSlash = true");
				}
				if(routeInfo.getMethods() != null && routeInfo.getMethods().length > 0) {
					result.append(", method = { ").append(Arrays.stream(routeInfo.getMethods()).map(method -> routeContext.getMethodTypeName() + "." + method.toString()).collect(Collectors.joining(", "))).append(" }");
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
			else if(routeContext.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_DECLARATION) {
				boolean typesMode = routeContext.isTypeMode(routeInfo);

				StringBuilder routeHandler = new StringBuilder("exchange -> {").append(System.lineSeparator());
				routeHandler.append(this.visit(routeInfo.getResponseBody(), routeContext.withIndentDepth(routeContext.getIndentDepth() + (typesMode ? 2 : 1) ).withMode(typesMode ? WebServerClassGenerationContext.GenerationMode.ROUTE_HANDLER_TYPE : WebServerClassGenerationContext.GenerationMode.ROUTE_HANDLER_CLASS)));
				routeHandler.append(routeContext.indent(typesMode ? 1 : 0)).append("}");

				StringBuilder routeManager = new StringBuilder();

				if(routeInfo.getPaths().length > 0) {
					routeManager.append(Arrays.stream(routeInfo.getPaths())
						.map(path -> ".path(\"" + StringEscapeUtils.escapeJava(routeInfo.getController()
							.map(WebServerControllerInfo::getRootPath)
							.map(rootPath -> URIs.uri(rootPath, URIs.RequestTargetForm.PATH, URIs.Option.PARAMETERIZED, URIs.Option.NORMALIZED, URIs.Option.PATH_PATTERN).path(path, false).buildRawPath())
							.orElse(path)) + "\", " + routeInfo.isMatchTrailingSlash() + ")"
						)
						.collect(Collectors.joining())
					);
				}
				else {
					routeInfo.getController()
						.map(WebServerControllerInfo::getRootPath)
						.map(rootPath -> URIs.uri(rootPath, URIs.RequestTargetForm.PATH, URIs.Option.PARAMETERIZED, URIs.Option.NORMALIZED, URIs.Option.PATH_PATTERN).buildRawPath())
						.ifPresent(rootPath -> routeManager.append(".path(\"").append(StringEscapeUtils.escapeJava(rootPath)).append("\", ").append(routeInfo.isMatchTrailingSlash()).append(")"));
				}
				if(routeInfo.getMethods() != null && routeInfo.getMethods().length > 0) {
					routeManager.append(Arrays.stream(routeInfo.getMethods()).map(method -> ".method(" + routeContext.getMethodTypeName() + "." + method.toString() + ")").collect(Collectors.joining()));
				}
				if(routeInfo.getConsumes() != null && routeInfo.getConsumes().length > 0) {
					routeManager.append(Arrays.stream(routeInfo.getConsumes()).map(consumes -> ".consume(\"" + consumes + "\")").collect(Collectors.joining()));
				}
				if(routeInfo.getProduces() != null && routeInfo.getProduces().length > 0) {
					routeManager.append(Arrays.stream(routeInfo.getProduces()).map(produces -> ".produce(\"" + produces + "\")").collect(Collectors.joining()));
				}
				if(routeInfo.getLanguages() != null && routeInfo.getLanguages().length > 0) {
					routeManager.append(Arrays.stream(routeInfo.getLanguages()).map(language -> ".language(\"" + language + "\")").collect(Collectors.joining()));
				}

				routeManager.append(".handler(").append(routeHandler).append(")");

				// TODO this causes the compiler to throw java.lang.StackOverflowError...
				//return new StringBuilder(routeContext.indent(0)).append(".route()").append(routeManager);

				if(typesMode) {
					StringBuilder result = new StringBuilder(routeContext.indent(0)).append(".route(route -> {").append(System.lineSeparator());
					result.append(routeContext.indent(1)).append("route").append(routeManager).append(";").append(System.lineSeparator());
					result.append(routeContext.indent(0)).append("})");
					return result;
				}
				else {
					return new StringBuilder(routeContext.indent(0)).append(".route()").append(routeManager);
				}
			}
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebServerResponseBodyInfo responseBodyInfo, WebServerClassGenerationContext context) {
		if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_HANDLER_CLASS || context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_HANDLER_TYPE) {
			boolean typesMode = context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_HANDLER_TYPE;

			StringBuilder result = new StringBuilder();
			WebServerRouteInfo routeInfo = context.getWebServerRouteInfo();

			StringBuilder requestParameters = new StringBuilder();
			WebServerClassGenerationContext.GenerationMode parameterReferenceMode = typesMode ? WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE : WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS;

			Integer nonReactiveRequestBodyParameterIndex = null;
			boolean hasFormParameters = false;
			WebRequestBodyParameterInfo requestBodyInfo = null;
			int parameterIndex = 0;
			for(Iterator<WebParameterInfo> parameterInfoIterator = Arrays.stream(routeInfo.getParameters()).iterator(); parameterInfoIterator.hasNext();) {
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

			StringBuilder controllerInvoke = new StringBuilder("this.").append(context.getFieldName(context.getWebServerControllerInfo().getQualifiedName())).append(".").append(routeInfo.getElement().get().getSimpleName().toString()).append("(").append(requestParameters).append(")");
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
						mapFunction = "flatMap";
						// TODO Why???
						/*if(responseBodyInfo.getBodyKind() == ResponseBodyKind.EMPTY) {
							mapFunction = "flatMap";
						}
						else {
							mapFunction = "map";
						}*/
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
						if(hasFormParameters || (requestBodyInfo != null && nonReactiveRequestBodyParameterIndex != null)) {
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
						if(hasFormParameters || (requestBodyInfo != null && nonReactiveRequestBodyParameterIndex != null)) {
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
						if(hasFormParameters || (requestBodyInfo != null && nonReactiveRequestBodyParameterIndex != null)) {
							responseBodyDataMethod = "one";
						}
						else {
							responseBodyDataMethod = "value";
						}
					}
					/*else if(responseBodyInfo.getBodyKind() == ResponseBodyKind.RAW) {
						responseBodyDataMethod = "stream";
					}*/
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
						result.append(".<").append(context.getTypeName(responseBodyInfo.getType())).append(">encoder(").append(VARIABLE_TYPES).append("[").append(context.getTypeIndex(responseBodyInfo.getType())).append("]).");
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
					WebSseEventFactoryParameterInfo sseEventFactoryParameter = (WebSseEventFactoryParameterInfo)Arrays.stream(context.getWebServerRouteInfo().getParameters()).filter(parameter -> parameter instanceof WebSseEventFactoryParameterInfo).findFirst().get();

					result.append(context.indent(0)).append("exchange.response().body()");
					if(typesMode) {
						result.append(".<").append(context.getTypeName(responseBodyInfo.getType())).append(">sseEncoder(\"").append(sseEventFactoryParameter.getDataMediaType().orElse("text/plain")).append("\", ").append(VARIABLE_TYPES).append("[").append(context.getTypeIndex(responseBodyInfo.getType())).append("])");
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
	public StringBuilder visit(WebParameterInfo parameterInfo, WebServerClassGenerationContext context) {
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
		else if(parameterInfo instanceof WebSocketParameterInfo) {
			return this.visit((WebSocketParameterInfo)parameterInfo, context);
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebBasicParameterInfo basicParameterInfo, WebServerClassGenerationContext context) {
		if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS || context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE) {
			boolean typesMode = context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE;
			String parameterRouteType = typesMode ? VARIABLE_TYPES + "[" + context.getTypeIndex(basicParameterInfo.getType()) + "]" : null;

			StringBuilder result = new StringBuilder();

			TypeMirror basicParameterType = basicParameterInfo.getType();
			if(basicParameterType.getKind() == TypeKind.ARRAY) {
				TypeMirror parameterType = ((ArrayType)basicParameterType).getComponentType();
				if(!typesMode) {
					parameterRouteType = context.getTypeName(parameterType) + ".class";
				}
				result.append(context.getOptionalTypeName()).append(".ofNullable(");
				result.append(this.visit(basicParameterInfo, context.withMode(WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_MANY))).append(")");
				result.append(".map(parameters -> parameters.stream().flatMap(parameter -> parameter.<").append(context.getTypeName(parameterType)).append(">asListOf(").append(parameterRouteType).append(").stream())");
//				result.append(".<").append(context.getTypeName(basicParameterType)).append(">map(parameters -> parameters.stream().flatMap(parameter -> parameter.asListOf(").append(parameterRouteType).append(").stream())");
				result.append(".toArray(").append(context.getTypeName(((ArrayType)basicParameterType).getComponentType())).append("[]::new)).filter(l -> l.length > 0)");
			}
			else if(context.getTypeUtils().isSameType(context.getCollectionType(), context.getTypeUtils().erasure(basicParameterType))) {
				TypeMirror parameterType = ((DeclaredType)basicParameterType).getTypeArguments().getFirst();
				if(!typesMode) {
					parameterRouteType = context.getTypeName(parameterType) + ".class";
				}
				result.append(context.getOptionalTypeName()).append(".ofNullable(");
				result.append(this.visit(basicParameterInfo, context.withMode(WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_MANY))).append(")");
				result.append(".<").append(context.getTypeName(basicParameterType)).append(">map(parameters -> parameters.stream().flatMap(parameter -> parameter.<").append(context.getTypeName(parameterType)).append(">asListOf(").append(parameterRouteType).append(").stream())");
//				result.append(".<").append(context.getTypeName(basicParameterType)).append(">map(parameters -> parameters.stream().flatMap(parameter -> parameter.asListOf(").append(parameterRouteType).append(").stream())");
				result.append(".collect(").append(context.getCollectorsTypeName()).append(".toList())).filter(l -> !l.isEmpty())");
			}
			else if(context.getTypeUtils().isSameType(context.getListType(), context.getTypeUtils().erasure(basicParameterType))) {
				TypeMirror parameterType = ((DeclaredType)basicParameterType).getTypeArguments().getFirst();
				if(!typesMode) {
					parameterRouteType = context.getTypeName(parameterType) + ".class";
				}

				result.append(context.getOptionalTypeName()).append(".ofNullable(");
				result.append(this.visit(basicParameterInfo, context.withMode(WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_MANY))).append(")");
				result.append(".map(parameters -> parameters.stream().<").append(context.getTypeName(parameterType)).append(">flatMap(parameter -> parameter.asListOf(").append(parameterRouteType).append(").stream())");
//				result.append(".<").append(context.getTypeName(basicParameterType)).append(">map(parameters -> parameters.stream().flatMap(parameter -> parameter.asListOf(").append(parameterRouteType).append(").stream())");
				result.append(".collect(").append(context.getCollectorsTypeName()).append(".toList())).filter(l -> !l.isEmpty())");
			}
			else if(context.getTypeUtils().isSameType(context.getSetType(), context.getTypeUtils().erasure(basicParameterType))) {
				TypeMirror parameterType = ((DeclaredType)basicParameterType).getTypeArguments().getFirst();
				if(!typesMode) {
					parameterRouteType = context.getTypeName(parameterType) + ".class";
				}

				result.append(context.getOptionalTypeName()).append(".ofNullable(");
				result.append(this.visit(basicParameterInfo, context.withMode(WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_MANY))).append(")");
				result.append(".map(parameters -> parameters.stream().flatMap(parameter -> parameter.<").append(context.getTypeName(parameterType)).append(">asListOf(").append(parameterRouteType).append(").stream())");
//				result.append(".<").append(context.getTypeName(basicParameterType)).append(">map(parameters -> parameters.stream().flatMap(parameter -> parameter.asListOf(").append(parameterRouteType).append(").stream())");
				result.append(".collect(").append(context.getCollectorsTypeName()).append(".toSet())).filter(l -> !l.isEmpty())");
			}
			else {
				result.append(this.visit(basicParameterInfo, context.withMode(WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_ONE)));
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
						parameterRouteType = context.getTypeName(basicParameterType) + ".class";
					}
					else {
						result.append("<").append(context.getTypeName(basicParameterType)).append(">");
					}
					result.append("as(").append(parameterRouteType).append("))");
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
	public StringBuilder visit(WebCookieParameterInfo cookieParameterInfo, WebServerClassGenerationContext context) {
		if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_ONE) {
			return new StringBuilder("exchange.request().headers().cookies().get(\"").append(cookieParameterInfo.getQualifiedName().getParameterName()).append("\")");
		}
		else if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_MANY) {
			return new StringBuilder("exchange.request().headers().cookies().getAll(\"").append(cookieParameterInfo.getQualifiedName().getParameterName()).append("\")");
		}
		else if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS || context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE) {
			return this.visit((WebBasicParameterInfo)cookieParameterInfo, context);
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebFormParameterInfo formParameterInfo, WebServerClassGenerationContext context) {
		if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_ONE) {
			return new StringBuilder(context.getOptionalTypeName()).append(".ofNullable(formParameters.get(\"").append(formParameterInfo.getQualifiedName().getParameterName()).append("\")).flatMap(parameter -> parameter.stream().findFirst())");
		}
		else if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_MANY) {
			return new StringBuilder("formParameters.get(\"").append(formParameterInfo.getQualifiedName().getParameterName()).append("\")");
		}
		else if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS || context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE) {
			return this.visit((WebBasicParameterInfo)formParameterInfo, context);
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebHeaderParameterInfo headerParameterInfo, WebServerClassGenerationContext context) {
		if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_ONE) {
			return new StringBuilder("exchange.request().headers().getParameter(\"").append(headerParameterInfo.getQualifiedName().getParameterName()).append("\")");
		}
		else if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_MANY) {
			return new StringBuilder("exchange.request().headers().getAllParameter(\"").append(headerParameterInfo.getQualifiedName().getParameterName()).append("\")");
		}
		else if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS || context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE) {
			return this.visit((WebBasicParameterInfo)headerParameterInfo, context);
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebPathParameterInfo pathParameterInfo, WebServerClassGenerationContext context) {
		if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_ONE) {
			return new StringBuilder("exchange.request().pathParameters().get(\"").append(pathParameterInfo.getQualifiedName().getParameterName()).append("\")");
		}
		if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_MANY) {
			return new StringBuilder("exchange.request().pathParameters().get(\"").append(pathParameterInfo.getQualifiedName().getParameterName()).append("\").map(parameter -> ").append(context.getListTypeName()).append(".of(parameter)).orElse(").append(context.getListTypeName()).append(".of())");
		}
		else if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS || context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE) {
			return this.visit((WebBasicParameterInfo)pathParameterInfo, context);
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebQueryParameterInfo queryParameterInfo, WebServerClassGenerationContext context) {
		if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_ONE) {
			return new StringBuilder("exchange.request().queryParameters().get(\"").append(queryParameterInfo.getQualifiedName().getParameterName()).append("\")");
		}
		else if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_MANY) {
			return new StringBuilder("exchange.request().queryParameters().getAll(\"").append(queryParameterInfo.getQualifiedName().getParameterName()).append("\")");
		}
		else if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS || context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE) {
			return this.visit((WebBasicParameterInfo)queryParameterInfo, context);
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebRequestBodyParameterInfo bodyParameterInfo, WebServerClassGenerationContext context) {
		if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS || context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE) {
			boolean typesMode = context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE;
			String parameterType = typesMode ? VARIABLE_TYPES + "[" + context.getTypeIndex(bodyParameterInfo.getType()) + "]" : null;

			StringBuilder result = new StringBuilder();

			TypeMirror bodyParameterType = bodyParameterInfo.getType();
			if(bodyParameterInfo.getBodyReactiveKind() == RequestBodyReactiveKind.NONE) {
				if(bodyParameterInfo.getBodyKind() == RequestBodyKind.RAW) {
					result.append(context.getFluxTypeName()).append(".from(exchange.request().body().get().raw().stream()).reduceWith(() -> ").append(context.getUnpooledTypeName()).append(".buffer(), (acc, chunk) -> { try { return acc.writeBytes(chunk); } finally { chunk.release(); } })");
				}
				else if(bodyParameterInfo.getBodyKind() == RequestBodyKind.CHARSEQUENCE) {
					result.append(context.getFluxTypeName()).append(".from(exchange.request().body().get().string().stream()).collect(").append(context.getCollectorsTypeName()).append(".joining())");
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
					if(bodyParameterInfo.getBodyKind() == RequestBodyKind.RAW || bodyParameterInfo.getBodyKind() == RequestBodyKind.CHARSEQUENCE) {
						result.insert(0, ".from(").insert(0, context.getFluxTypeName());
						if(bodyParameterInfo.getBodyKind() == RequestBodyKind.RAW) {
							result.append("raw().stream())");
							if(bodyParameterInfo.getBodyReactiveKind() == RequestBodyReactiveKind.ONE) {
								result.append(".reduceWith(() -> ").append(context.getUnpooledTypeName()).append(".buffer(), (acc, chunk) -> { try { return acc.writeBytes(chunk); } finally { chunk.release(); } })");
							}
						}
						else if(bodyParameterInfo.getBodyKind() == RequestBodyKind.CHARSEQUENCE) {
							result.append("string().stream())");
							if(bodyParameterInfo.getBodyReactiveKind() == RequestBodyReactiveKind.ONE) {
								result.append(".collect(").append(context.getCollectorsTypeName()).append(".joining())");
							}
							else if(context.getTypeUtils().isSameType(bodyParameterInfo.getType(), context.getStringType())) {
								result.append(".map(").append(context.getCharSequenceTypeName()).append("::toString)");
							}
						}
					}
					else {
						if(bodyParameterInfo.getBodyReactiveKind() == RequestBodyReactiveKind.ONE) {
							result.insert(0, ".from(").insert(0, context.getMonoTypeName());
						}
						else if(bodyParameterInfo.getBodyReactiveKind() == RequestBodyReactiveKind.MANY || bodyParameterInfo.getBodyReactiveKind() == RequestBodyReactiveKind.PUBLISHER) {
							result.insert(0, ".from(").insert(0, context.getFluxTypeName());
						}
						else {
							throw new IllegalStateException("Unknown request body reactive kind: " + bodyParameterInfo.getBodyReactiveKind());
						}

						if(bodyParameterInfo.getBodyKind() == RequestBodyKind.MULTIPART) {
							result.append("multipart().stream())");
						}
						else if(bodyParameterInfo.getBodyKind() == RequestBodyKind.URLENCODED) {
							result.append("urlEncoded().stream())");
						}
						else {
							throw new IllegalStateException("Unknown request body kind: " + bodyParameterInfo.getBodyKind());
						}
					}
				}
			}
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebExchangeParameterInfo exchangeParameterInfo, WebServerClassGenerationContext context) {
		if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS || context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE) {
			return new StringBuilder("exchange");
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebExchangeContextParameterInfo exchangeContextParameterInfo, WebServerClassGenerationContext context) {
		if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS || context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE) {
			return new StringBuilder("exchange.context()");
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebSseEventFactoryParameterInfo sseEventFactoryParameterInfo, WebServerClassGenerationContext context) {
		if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS || context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE) {
			return new StringBuilder("events");
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebSocketServerRouteInfo webSocketServerRouteInfo, WebServerClassGenerationContext context) {
		if(context.getMode() == WebServerClassGenerationContext.GenerationMode.WEBSOCKET_ROUTE_ANNOTATION) {
			StringBuilder result = new StringBuilder();
			result.append(context.indent(0)).append("@").append(context.getWebSocketRouteAnnotationTypeName()).append("(");

			result.append("path = { ");
			if(webSocketServerRouteInfo.getPaths().length > 0) {
				result.append(Arrays.stream(webSocketServerRouteInfo.getPaths())
					.map(path -> "\"" + StringEscapeUtils.escapeJava(webSocketServerRouteInfo.getController()
						.map(WebServerControllerInfo::getRootPath)
						.map(rootPath -> URIs.uri(rootPath, URIs.RequestTargetForm.PATH, URIs.Option.PARAMETERIZED, URIs.Option.NORMALIZED, URIs.Option.PATH_PATTERN).path(path, false).buildRawPath())
						.orElse(path)) + "\""
					)
					.collect(Collectors.joining(", "))
				);
			}
			else {
				webSocketServerRouteInfo.getController()
					.map(WebServerControllerInfo::getRootPath)
					.map(rootPath -> URIs.uri(rootPath, URIs.RequestTargetForm.PATH, URIs.Option.PARAMETERIZED, URIs.Option.NORMALIZED, URIs.Option.PATH_PATTERN).buildRawPath())
					.ifPresent(rootPath -> result.append("\"").append(rootPath).append("\""));
			}
			result.append(" }");

			if(webSocketServerRouteInfo.isMatchTrailingSlash()) {
				result.append(", matchTrailingSlash = true");
			}
			if(webSocketServerRouteInfo.getLanguages() != null && webSocketServerRouteInfo.getLanguages().length > 0) {
				result.append(", language = { ").append(Arrays.stream(webSocketServerRouteInfo.getLanguages()).map(language -> "\"" + language + "\"").collect(Collectors.joining(", "))).append(" }");
			}
			if(webSocketServerRouteInfo.getSubprotocols()!= null && webSocketServerRouteInfo.getSubprotocols().length > 0) {
				result.append(", subprotocol = { ").append(Arrays.stream(webSocketServerRouteInfo.getSubprotocols()).map(subprotocol -> "\"" + subprotocol + "\"").collect(Collectors.joining(", "))).append(" }");
			}
			if(webSocketServerRouteInfo.getMessageType() != null) {
				result.append(", messageType = ").append(context.getWebSocketMessageKindTypeName()).append(".").append(webSocketServerRouteInfo.getMessageType().toString());
			}
			result.append(")");
			return result;
		}
		else if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_DECLARATION) {
			boolean typesMode = context.isTypeMode(webSocketServerRouteInfo);
			StringBuilder routeHandler = new StringBuilder("exchange -> {").append(System.lineSeparator());
			webSocketServerRouteInfo.getOutboundPublisher().ifPresentOrElse(
				outboundPublisherInfo -> routeHandler.append(context.indent(typesMode ? 2 : 1)).append(this.visit(outboundPublisherInfo, context.withIndentDepth(context.getIndentDepth() + (typesMode ? 2 : 1) ).withMode(typesMode ? WebServerClassGenerationContext.GenerationMode.WEBSOCKET_ROUTE_HANDLER_TYPE : WebServerClassGenerationContext.GenerationMode.WEBSOCKET_ROUTE_HANDLER_CLASS))).append(";").append(System.lineSeparator()),
				() -> routeHandler.append(context.indent(typesMode ? 2 : 1)).append(this.visit(webSocketServerRouteInfo, context.withIndentDepth(context.getIndentDepth() + (typesMode ? 2 : 1) ).withMode(typesMode ? WebServerClassGenerationContext.GenerationMode.WEBSOCKET_ROUTE_HANDLER_TYPE : WebServerClassGenerationContext.GenerationMode.WEBSOCKET_ROUTE_HANDLER_CLASS))).append(";").append(System.lineSeparator())
			);
			routeHandler.append(context.indent(typesMode ? 1 : 0)).append("}");

			StringBuilder routeManager = new StringBuilder();

			if(webSocketServerRouteInfo.getPaths().length > 0) {
				routeManager.append(Arrays.stream(webSocketServerRouteInfo.getPaths())
					.map(path -> ".path(\"" + StringEscapeUtils.escapeJava(webSocketServerRouteInfo.getController()
						.map(WebServerControllerInfo::getRootPath)
						.map(rootPath -> URIs.uri(rootPath, URIs.RequestTargetForm.PATH, URIs.Option.PARAMETERIZED, URIs.Option.NORMALIZED, URIs.Option.PATH_PATTERN).path(path, false).buildRawPath())
						.orElse(path)) + "\", " + webSocketServerRouteInfo.isMatchTrailingSlash() + ")"
					)
					.collect(Collectors.joining())
				);
			}
			else {
				webSocketServerRouteInfo.getController()
					.map(WebServerControllerInfo::getRootPath)
					.map(rootPath -> URIs.uri(rootPath, URIs.RequestTargetForm.PATH, URIs.Option.PARAMETERIZED, URIs.Option.NORMALIZED, URIs.Option.PATH_PATTERN).buildRawPath())
					.ifPresent(rootPath -> routeManager.append(".path(\"").append(StringEscapeUtils.escapeJava(rootPath)).append("\", ").append(webSocketServerRouteInfo.isMatchTrailingSlash()).append(")"));
			}
			if(webSocketServerRouteInfo.getLanguages() != null && webSocketServerRouteInfo.getLanguages().length > 0) {
				routeManager.append(Arrays.stream(webSocketServerRouteInfo.getLanguages()).map(language -> ".language(\"" + language + "\")").collect(Collectors.joining()));
			}
			if(webSocketServerRouteInfo.getSubprotocols() != null && webSocketServerRouteInfo.getSubprotocols().length > 0) {
				routeManager.append(Arrays.stream(webSocketServerRouteInfo.getSubprotocols()).map(subprotocol -> ".subprotocol(\"" + subprotocol + "\")").collect(Collectors.joining()));
			}

			routeManager.append(".handler(").append(routeHandler).append(")");

			// TODO this causes the compiler to throw java.lang.StackOverflowError...
//			return new StringBuilder(context.indent(0)).append(".webSocketRoute()").append(routeManager);

			if(typesMode) {
				StringBuilder result = new StringBuilder(context.indent(0)).append(".webSocketRoute(webSocketRoute -> {").append(System.lineSeparator());
				result.append(context.indent(1)).append("webSocketRoute").append(routeManager).append(";").append(System.lineSeparator());
				result.append(context.indent(0)).append("})");
				return result;
			}
			else {
				return new StringBuilder(context.indent(0)).append(".webSocketRoute()").append(routeManager);
			}
		}
		else if(context.getMode() == WebServerClassGenerationContext.GenerationMode.WEBSOCKET_ROUTE_HANDLER_CLASS || context.getMode() == WebServerClassGenerationContext.GenerationMode.WEBSOCKET_ROUTE_HANDLER_TYPE) {
			boolean typesMode = context.getMode() == WebServerClassGenerationContext.GenerationMode.WEBSOCKET_ROUTE_HANDLER_TYPE;
			StringBuilder requestParameters = new StringBuilder();
			WebServerClassGenerationContext.GenerationMode parameterReferenceMode = typesMode ? WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE : WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS;

			int parameterIndex = 0;
			for(Iterator<WebParameterInfo> parameterInfoIterator = Arrays.stream(webSocketServerRouteInfo.getParameters()).iterator(); parameterInfoIterator.hasNext();) {
				// WebExchangeContextParameterInfo, WebSocketExchangeParameterInfo, WebSocketInboundParameterInfo, WebSocketInboundPubliserInfo, WebSocketOutboundParameterInfo
				requestParameters.append(this.visit(parameterInfoIterator.next(), context.withIndentDepth(0).withMode(parameterReferenceMode).withWebServerRouteInfo(webSocketServerRouteInfo).withParameterIndex(parameterIndex)));
				if(parameterInfoIterator.hasNext()) {
					requestParameters.append(", ");
				}
				parameterIndex++;
			}
			return new StringBuilder("this.").append(context.getFieldName(context.getWebServerControllerInfo().getQualifiedName())).append(".").append(webSocketServerRouteInfo.getElement().get().getSimpleName().toString()).append("(").append(requestParameters).append(")");
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebSocketParameterInfo parameterInfo, WebServerClassGenerationContext context) {
		if(parameterInfo instanceof WebSocketOutboundParameterInfo) {
			return this.visit((WebSocketOutboundParameterInfo)parameterInfo, context);
		}
		else if(parameterInfo instanceof WebSocketServerInboundPublisherParameterInfo) {
			return this.visit((WebSocketServerInboundPublisherParameterInfo)parameterInfo, context);
		}
		else if(parameterInfo instanceof WebSocketServerInboundParameterInfo) {
			return this.visit((WebSocketServerInboundParameterInfo)parameterInfo, context);
		}
		else if(parameterInfo instanceof WebSocketExchangeParameterInfo) {
			return this.visit((WebSocketExchangeParameterInfo)parameterInfo, context);
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebSocketBoundPublisherInfo boundPublisherInfo, WebServerClassGenerationContext context) {
		if(boundPublisherInfo instanceof WebSocketServerOutboundPublisherInfo) {
			return this.visit((WebSocketServerOutboundPublisherInfo)boundPublisherInfo, context);
		}
		else if(boundPublisherInfo instanceof WebSocketServerInboundPublisherParameterInfo) {
			return this.visit((WebSocketServerInboundPublisherParameterInfo)boundPublisherInfo, context);
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebSocketServerOutboundPublisherInfo outboundPublisherInfo, WebServerClassGenerationContext context) {
		if(context.getMode() == WebServerClassGenerationContext.GenerationMode.WEBSOCKET_ROUTE_HANDLER_CLASS || context.getMode() == WebServerClassGenerationContext.GenerationMode.WEBSOCKET_ROUTE_HANDLER_TYPE) {
			boolean typesMode = context.getMode() == WebServerClassGenerationContext.GenerationMode.WEBSOCKET_ROUTE_HANDLER_TYPE;

			WebSocketMessage.Kind webSocketMessageKind = ((WebSocketServerRouteInfo)context.getWebServerRouteInfo()).getMessageType();
			StringBuilder result = new StringBuilder();
			result.append("exchange.outbound()");

			WebSocketServerRouteInfo webSocketServerRouteInfo = (WebSocketServerRouteInfo)context.getWebServerRouteInfo();
			result.append(".closeOnComplete(").append(webSocketServerRouteInfo.isCloseOnComplete()).append(")");

			StringBuilder handlerInvoke = this.visit(webSocketServerRouteInfo, context);

			boolean isPublisher = outboundPublisherInfo.getBoundReactiveKind() == WebSocketBoundPublisherInfo.BoundReactiveKind.PUBLISHER;
			boolean isCharSequence = false;
			switch(outboundPublisherInfo.getBoundKind()) {
				case CHARSEQUENCE_REDUCED:
				case CHARSEQUENCE_REDUCED_ONE:
				case CHARSEQUENCE_PUBLISHER:
				case CHARSEQUENCE_MANY: isCharSequence = true;
				case RAW_REDUCED:
				case RAW_REDUCED_ONE:
				case RAW_PUBLISHER:
				case RAW_MANY: {
					// (Publisher|Flux|Mono)<ByteBuf>
					// exchange.outbound().messages(factory -> Flux.from(...).map(factory::text_raw))
					// exchange.outbound().messages(factory -> ....map(factory::text_raw))

					result.append(".messages(factory -> ");
					if(isPublisher) {
						result.append(context.getFluxTypeName()).append(".from(").append(handlerInvoke).append(")");
					}
					else {
						result.append(handlerInvoke);
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
					// (Publisher|Flux|Mono)<Void>
					// exchange.outbound().messages(factory -> Flux.from(...).then(Mono.empty()));
					// exchange.outbound().messages(factory -> ....then(Mono.empty()));

					result.append(".messages(factory -> ");
					if(isPublisher) {
						result.append(context.getFluxTypeName()).append(".from(").append(handlerInvoke).append(")");
					}
					else {
						result.append(handlerInvoke);
					}

					result.append(".then(").append(context.getMonoTypeName()).append(".empty())");
					result.append(")");
					break;
				}
				case ENCODED: {
					// (Publisher|Flux|Mono)<T>
					// => GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS
					// exchange.outbound().encodeTextMessages(this.wsx13(), Message.class);
					// => GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE
					// exchange.outbound().encodeTextMessages(this.wsx13(), _TYPES[0]);

					switch(webSocketMessageKind) {
						case TEXT: result.append(".encodeTextMessages(");
							break;
						case BINARY: result.append(".encodeBinaryMessages(");
							break;
						default :
							throw new IllegalStateException("Unknown WebSocket message kind");
					}

					result.append(handlerInvoke).append(", ");
					if(typesMode) {
						result.append(VARIABLE_TYPES).append("[").append(context.getTypeIndex(outboundPublisherInfo.getType())).append("]");
					}
					else {
						result.append(context.getTypeName(outboundPublisherInfo.getType())).append(".class");
					}
					result.append(")");
					break;
				}
				default :
					throw new IllegalStateException("Unknown WebSocket bound reactive kind");
			}
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebSocketOutboundParameterInfo outboundParameterInfo, WebServerClassGenerationContext context) {
		if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE || context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS) {
			StringBuilder result = new StringBuilder("exchange.outbound()");
			WebSocketServerRouteInfo webSocketServerRouteInfo = (WebSocketServerRouteInfo)context.getWebServerRouteInfo();
			result.append(".closeOnComplete(").append(webSocketServerRouteInfo.isCloseOnComplete()).append(")");
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebSocketServerInboundPublisherParameterInfo inboundPublisherParameterInfo, WebServerClassGenerationContext context) {
		if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE || context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS) {
			boolean typesMode = context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE;

			WebSocketMessage.Kind WebSocketMessageKind = ((WebSocketServerRouteInfo)context.getWebServerRouteInfo()).getMessageType();

			StringBuilder result = new StringBuilder();

			String webSocketInbound = "exchange.inbound()";

			TypeMirror boundReactiveType;
			switch(inboundPublisherParameterInfo.getBoundReactiveKind()) {
				case PUBLISHER:
				case MANY: boundReactiveType = context.getFluxType();
					break;
				case ONE: boundReactiveType = context.getMonoType();
					break;
				default:
					throw new IllegalStateException("Unknown WebSocket bound reactive kind");
			}

			boolean isCharSequence = false;
			switch(inboundPublisherParameterInfo.getBoundKind()) {
				case CHARSEQUENCE_REDUCED: isCharSequence = true;
				case RAW_REDUCED: {
					// (Publisher|Flux|Mono)<ByteBuf|String>
					// Flux.from(exchange.inbound().textMessages()).flatMap(WebSocketMessage::rawReduced)
					// Flux.from(exchange.inbound().binaryMessages()).flatMap(WebSocketMessage::rawReduced)

					result.append(context.getTypeName(boundReactiveType)).append(".from(").append(webSocketInbound);
					switch(WebSocketMessageKind) {
						case TEXT: result.append(".textMessages()");
							break;
						case BINARY: result.append(".binaryMessages()");
							break;
						default :
							throw new IllegalStateException("Unknown WebSocket message kind");
					}
					result.append(").flatMap(").append(context.getWebSocketMessageTypeName()).append("::").append(isCharSequence ? "stringReduced" : "rawReduced").append(")");
					return result;
				}
				case CHARSEQUENCE_REDUCED_ONE: isCharSequence = true;
				case RAW_REDUCED_ONE: {
					// (Publisher|Flux|Mono)<Mono<ByteBuf|String>>
					// (Flux|Mono).from(exchange.inbound().textMessages()).map(WebSocketMessage::rawReduced)
					// (Flux|Mono).from(exchange.inbound().binaryMessages()).map(WebSocketMessage::rawReduced)

					result.append(context.getTypeName(boundReactiveType)).append(".from(").append(webSocketInbound);
					switch(WebSocketMessageKind) {
						case TEXT: result.append(".textMessages()");
							break;
						case BINARY: result.append(".binaryMessages()");
							break;
						default :
							throw new IllegalStateException("Unknown WebSocket message kind");
					}
					result.append(").map(").append(context.getWebSocketMessageTypeName()).append("::").append(isCharSequence ? "stringReduced" : "rawReduced").append(")");
					return result;
				}
				case CHARSEQUENCE_PUBLISHER: isCharSequence = true;
				case RAW_PUBLISHER: {
					// (Publisher|Flux|Mono)<Publisher<ByteBuf|String>>
					// (Flux|Mono).from(exchange.inbound().textMessages()).map(WebSocketMessage::raw)
					// (Flux|Mono).from(exchange.inbound().binaryMessages()).map(WebSocketMessage::raw)
					result.append(context.getTypeName(boundReactiveType)).append(".from(").append(webSocketInbound);
					switch(WebSocketMessageKind) {
						case TEXT: result.append(".textMessages()");
							break;
						case BINARY: result.append(".binaryMessages()");
							break;
						default :
							throw new IllegalStateException("Unknown WebSocket message kind");
					}
					result.append(").map(").append(context.getWebSocketMessageTypeName()).append("::").append(isCharSequence ? "string" : "raw").append(")");
					return result;
				}
				case CHARSEQUENCE_MANY: isCharSequence = true;
				case RAW_MANY: {
					// (Publisher|Flux|Mono)<Flux<ByteBuf|String>>
					// (Flux|Mono).from(exchange.inbound().textMessages()).map(message -> Flux.from(message.raw()))
					// (Flux|Mono).from(exchange.inbound().binaryMessages()).map(message -> Flux.from(message.raw()))
					result.append(context.getTypeName(boundReactiveType)).append(".from(").append(webSocketInbound);
					switch(WebSocketMessageKind) {
						case TEXT: result.append(".textMessages()");
							break;
						case BINARY: result.append(".binaryMessages()");
							break;
						default :
							throw new IllegalStateException("Unknown WebSocket message kind");
					}
					result.append(").map(message -> ").append(context.getFluxTypeName()).append(".from(message.").append(isCharSequence ? "string" : "raw").append("()))");
					return result;
				}
				case EMPTY: {
					// (Publisher|Flux|Mono)<Void>
					// Flux.from(exchange.inbound().frames()).doOnNext(WebSocketFrame::release).then()
					// Flux.from(exchange.inbound().frames()).doOnNext(WebSocketFrame::release).thenMany(Flux.empty())

					result.append(context.getTypeName(boundReactiveType)).append(".empty()");

					// We assume the client won't send any frame so the inbound publisher will be empty, but it will only complete when the connection is closed, so injecting the actual inbound is
					// actually dangerous and may lead to situation where a user chaining the outbound publisher to the inbound publisher leading to a deadlock

					/*result.append(context.getTypeName(boundReactiveType)).append(".from(").append(webSocketInbound).append(".frames())");
					result.append(".doOnNext(").append(context.getWebSocketFrameTypeName()).append("::release)");

					if(inboundPublisherParameterInfo.getBoundReactiveKind() == WebSocketBoundPublisherInfo.BoundReactiveKind.MANY) {
						result.append(".thenMany(").append(context.getFluxTypeName()).append(".empty())");
					}
					else {
						result.append(".then()");
					}*/
					return result;
				}
				case ENCODED: {
					StringBuilder decodedMessagesPublisher = new StringBuilder();
					decodedMessagesPublisher.append(webSocketInbound);

					switch(WebSocketMessageKind) {
						case TEXT: decodedMessagesPublisher.append(".decodeTextMessages(");
							break;
						case BINARY: decodedMessagesPublisher.append(".decodeBinaryMessages(");
							break;
						default :
							throw new IllegalStateException("Unknown WebSocket message kind");
					}

					if(typesMode) {
						decodedMessagesPublisher.append(VARIABLE_TYPES).append("[").append(context.getTypeIndex(inboundPublisherParameterInfo.getType())).append("]");
					}
					else {
						decodedMessagesPublisher.append(context.getTypeName(inboundPublisherParameterInfo.getType())).append(".class");
					}
					decodedMessagesPublisher.append(")");


					if(inboundPublisherParameterInfo.getBoundReactiveKind() == WebSocketBoundPublisherInfo.BoundReactiveKind.PUBLISHER) {
						result.append(decodedMessagesPublisher);
					}
					else {
						result.append(context.getTypeName(boundReactiveType)).append(".from(").append(decodedMessagesPublisher).append(")");
					}
					return result;
				}
				default :
					throw new IllegalStateException("Unknown WebSocket bound reactive kind");
			}
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebSocketServerInboundParameterInfo inboundParameterInfo, WebServerClassGenerationContext context) {
		if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE || context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS) {
			return new StringBuilder("exchange.inbound()");
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(WebSocketExchangeParameterInfo exchangeParameterInfo, WebServerClassGenerationContext context) {
		if(context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_CLASS || context.getMode() == WebServerClassGenerationContext.GenerationMode.ROUTE_PARAMETER_REFERENCE_TYPE) {
			return new StringBuilder("exchange");
		}
		return new StringBuilder();
	}
}

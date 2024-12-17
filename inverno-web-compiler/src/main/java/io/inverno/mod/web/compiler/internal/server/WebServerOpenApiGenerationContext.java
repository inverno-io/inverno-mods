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

import com.sun.source.doctree.AttributeTree;
import com.sun.source.doctree.AuthorTree;
import com.sun.source.doctree.CommentTree;
import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocRootTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.EndElementTree;
import com.sun.source.doctree.EntityTree;
import com.sun.source.doctree.ErroneousTree;
import com.sun.source.doctree.HiddenTree;
import com.sun.source.doctree.IdentifierTree;
import com.sun.source.doctree.IndexTree;
import com.sun.source.doctree.InheritDocTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ProvidesTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.SerialDataTree;
import com.sun.source.doctree.SerialFieldTree;
import com.sun.source.doctree.SerialTree;
import com.sun.source.doctree.SinceTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.ThrowsTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.doctree.UnknownInlineTagTree;
import com.sun.source.doctree.UsesTree;
import com.sun.source.doctree.ValueTree;
import com.sun.source.doctree.VersionTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTrees;
import io.inverno.core.compiler.spi.ModuleQualifiedName;
import io.inverno.core.compiler.spi.support.AbstractSourceGenerationContext;
import io.inverno.mod.http.base.BadRequestException;
import io.inverno.mod.http.base.ForbiddenException;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.InternalServerErrorException;
import io.inverno.mod.http.base.MethodNotAllowedException;
import io.inverno.mod.http.base.NotAcceptableException;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.http.base.ServiceUnavailableException;
import io.inverno.mod.http.base.UnauthorizedException;
import io.inverno.mod.http.base.UnsupportedMediaTypeException;
import io.inverno.mod.web.compiler.internal.TypeHierarchyExtractor;
import io.inverno.mod.web.compiler.spi.server.WebServerRouteInfo;
import io.netty.buffer.ByteBuf;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * <p>
 * Web server OpenAPI specification generation context.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class WebServerOpenApiGenerationContext extends AbstractSourceGenerationContext<WebServerOpenApiGenerationContext, WebServerOpenApiGenerationContext.GenerationMode> {

	protected static final String DEFAULT_INDENT = "    ";

	public enum GenerationMode {
		ROUTER_SPEC,
		CONTROLLER_TAG,
		ROUTE_PATH,
		ROUTE_PARAMETER,
		ROUTE_BODY
	}

	private enum DocGenerationMode {
		DESCRIPTION,
		SUMMARY,
		VERSION,
		CONTACT,
		PARAMETER,
		RESPONSE,
		RICH_TEXT,
		PLAIN_TEXT
	}

	private static class SchemaGenerationOptions {
		boolean inList;
		boolean useReference;

		SchemaGenerationOptions(boolean inList, boolean useReference) {
			this.inList = inList;
			this.useReference = useReference;
		}
	}

	private final DocTrees docUtils;
	private final WebServerOpenApiGenerationContext.JavadocToOpenApi javadocToOpenApi;
	private final WebServerOpenApiGenerationContext.OpenApiSchemaGenerator openApiSchemaGenerator;
	private final Map<String, DeclaredType> componentSchemaTypes;
	private final TypeHierarchyExtractor typeHierarchyExtractor;

	private final TypeMirror webExceptionType;
	private final TypeMirror badRequestExceptionType;
	private final TypeMirror forbiddenExceptionType;
	private final TypeMirror internalServerErrorExceptionType;
	private final TypeMirror methodNotAllowedExceptionType;
	private final TypeMirror notAcceptableExceptionType;
	private final TypeMirror notFoundExceptionType;
	private final TypeMirror serviceUnavailableExceptionType;
	private final TypeMirror unauthorizedExceptionType;
	private final TypeMirror unsupportedMediaTypeExceptionType;
	private final TypeMirror classType;
	private final TypeMirror objectType;
	private final TypeMirror charSequenceType;
	private final TypeMirror localDateType;
	private final TypeMirror localDateTimeType;
	private final TypeMirror zonedDateTimeType;
	private final TypeMirror enumType;
	private final TypeMirror collectionType;
	private final TypeMirror mapType;
	private final TypeMirror byteBufType;

	private WebServerOpenApiGenerationContext.DocGenerationMode docMode;
	private WebServerOpenApiGenerationContext.DocGenerationMode docInheritMode;
	private List<DocCommentTree> docCommentTrees;
	private String docParameterName;
	private String docExceptionName;
	private WebServerOpenApiGenerationContext.SchemaGenerationOptions schemaOptions;

	private WebServerRouteInfo webRoute;

	private String indentList;

	public WebServerOpenApiGenerationContext(Types typeUtils, Elements elementUtils, DocTrees docUtils, WebServerOpenApiGenerationContext.GenerationMode mode) {
		super(typeUtils, elementUtils, mode, DEFAULT_INDENT);
		this.docUtils = docUtils;
		this.javadocToOpenApi = new WebServerOpenApiGenerationContext.JavadocToOpenApi();
		this.openApiSchemaGenerator = new WebServerOpenApiGenerationContext.OpenApiSchemaGenerator();
		this.componentSchemaTypes = new HashMap<>();
		this.typeHierarchyExtractor = new TypeHierarchyExtractor(this.typeUtils);

		this.webExceptionType = this.elementUtils.getTypeElement(HttpException.class.getCanonicalName()).asType();
		this.badRequestExceptionType = this.elementUtils.getTypeElement(BadRequestException.class.getCanonicalName()).asType();
		this.forbiddenExceptionType = this.elementUtils.getTypeElement(ForbiddenException.class.getCanonicalName()).asType();
		this.internalServerErrorExceptionType = this.elementUtils.getTypeElement(InternalServerErrorException.class.getCanonicalName()).asType();
		this.methodNotAllowedExceptionType = this.elementUtils.getTypeElement(MethodNotAllowedException.class.getCanonicalName()).asType();
		this.notAcceptableExceptionType = this.elementUtils.getTypeElement(NotAcceptableException.class.getCanonicalName()).asType();
		this.notFoundExceptionType = this.elementUtils.getTypeElement(NotFoundException.class.getCanonicalName()).asType();
		this.serviceUnavailableExceptionType = this.elementUtils.getTypeElement(ServiceUnavailableException.class.getCanonicalName()).asType();
		this.unauthorizedExceptionType = this.elementUtils.getTypeElement(UnauthorizedException.class.getCanonicalName()).asType();
		this.unsupportedMediaTypeExceptionType = this.elementUtils.getTypeElement(UnsupportedMediaTypeException.class.getCanonicalName()).asType();
		this.classType = this.typeUtils.erasure(this.elementUtils.getTypeElement(Class.class.getCanonicalName()).asType());
		this.objectType = this.elementUtils.getTypeElement(Object.class.getCanonicalName()).asType();
		this.charSequenceType = this.elementUtils.getTypeElement(CharSequence.class.getCanonicalName()).asType();
		this.localDateType = this.elementUtils.getTypeElement(LocalDate.class.getCanonicalName()).asType();
		this.localDateTimeType = this.elementUtils.getTypeElement(LocalDateTime.class.getCanonicalName()).asType();
		this.zonedDateTimeType = this.elementUtils.getTypeElement(ZonedDateTime.class.getCanonicalName()).asType();
		this.enumType = this.typeUtils.erasure(this.elementUtils.getTypeElement(Enum.class.getCanonicalName()).asType());
		this.collectionType = this.typeUtils.erasure(this.elementUtils.getTypeElement(Collection.class.getCanonicalName()).asType());
		this.mapType = this.typeUtils.erasure(this.elementUtils.getTypeElement(Map.class.getCanonicalName()).asType());
		this.byteBufType = this.elementUtils.getTypeElement(ByteBuf.class.getCanonicalName()).asType();
	}

	private WebServerOpenApiGenerationContext(WebServerOpenApiGenerationContext parentGeneration) {
		super(parentGeneration);
		this.docUtils = parentGeneration.docUtils;
		this.javadocToOpenApi = parentGeneration.javadocToOpenApi;
		this.openApiSchemaGenerator = parentGeneration.openApiSchemaGenerator;
		this.componentSchemaTypes = parentGeneration.componentSchemaTypes;
		this.typeHierarchyExtractor = parentGeneration.typeHierarchyExtractor;
		this.webRoute = parentGeneration.webRoute;
		this.docMode = parentGeneration.docMode;
		this.docInheritMode = parentGeneration.docInheritMode;
		this.docCommentTrees = parentGeneration.docCommentTrees;
		this.docParameterName = parentGeneration.docParameterName;
		this.docExceptionName = parentGeneration.docExceptionName;
		this.schemaOptions = parentGeneration.schemaOptions;
//		this.indentList = parentGeneration.indentList; // TODO this needs to be tested and will probably require some fix...

		this.webExceptionType =  parentGeneration.webExceptionType;
		this.badRequestExceptionType =  parentGeneration.badRequestExceptionType;
		this.forbiddenExceptionType =  parentGeneration.forbiddenExceptionType;
		this.internalServerErrorExceptionType =  parentGeneration.internalServerErrorExceptionType;
		this.methodNotAllowedExceptionType =  parentGeneration.methodNotAllowedExceptionType;
		this.notAcceptableExceptionType =  parentGeneration.notAcceptableExceptionType;
		this.notFoundExceptionType =  parentGeneration.notFoundExceptionType;
		this.serviceUnavailableExceptionType =  parentGeneration.serviceUnavailableExceptionType;
		this.unauthorizedExceptionType =  parentGeneration.unauthorizedExceptionType;
		this.unsupportedMediaTypeExceptionType =  parentGeneration.unsupportedMediaTypeExceptionType;
		this.classType =  parentGeneration.classType;
		this.objectType =  parentGeneration.objectType;
		this.charSequenceType =  parentGeneration.charSequenceType;
		this.localDateType =  parentGeneration.localDateType;
		this.localDateTimeType =  parentGeneration.localDateTimeType;
		this.zonedDateTimeType =  parentGeneration.zonedDateTimeType;
		this.enumType =  parentGeneration.enumType;
		this.collectionType =  parentGeneration.collectionType;
		this.mapType =  parentGeneration.mapType;
		this.byteBufType =  parentGeneration.byteBufType;
	}

	@Override
	public void setIndent(String indent) {
		super.setIndent(indent);
		if(this.indent.length() < 2) {
			throw new IllegalStateException("Can't insert '-' with a less than 2 spaces indent");
		}

		char[] indentChars = this.indent.toCharArray();
		char[] indentListChars = new char[this.indent.length()];
		for(int i=0;i<indentListChars.length;i++) {
			if(i != indentListChars.length - 2) {
				indentListChars[i] = indentChars[i];
			}
			else {
				indentListChars[i] = '-';
			}
		}
		this.indentList = new String(indentListChars);
	}

	public String indentList(int depth) {
		if(this.indentDepth + depth < 1) {
			throw new IllegalStateException("Can't insert '-' with a 0 depth");
		}
		return String.valueOf(this.indent).repeat(Math.max(0, this.indentDepth + depth - 1)) + this.indentList;
	}

	public DocTrees getDocUtils() {
		return docUtils;
	}

	@Override
	public WebServerOpenApiGenerationContext withMode(WebServerOpenApiGenerationContext.GenerationMode mode) {
		WebServerOpenApiGenerationContext context = new WebServerOpenApiGenerationContext(this);
		context.mode = mode;
		return context;
	}

	@Override
	public WebServerOpenApiGenerationContext withIndentDepth(int indentDepth) {
		WebServerOpenApiGenerationContext context = new WebServerOpenApiGenerationContext(this);
		context.indentDepth = indentDepth;
		return context;
	}

	@Override
	public WebServerOpenApiGenerationContext withModule(ModuleQualifiedName moduleQualifiedName) {
		WebServerOpenApiGenerationContext context = new WebServerOpenApiGenerationContext(this);
		context.moduleQualifiedName = moduleQualifiedName;
		return context;
	}

	public WebServerOpenApiGenerationContext withWebRoute(WebServerRouteInfo webRoute) {
		WebServerOpenApiGenerationContext context = new WebServerOpenApiGenerationContext(this);
		context.webRoute = webRoute;
		return context;
	}

	public WebServerOpenApiGenerationContext withDocElement(Element element) {
		WebServerOpenApiGenerationContext context = new WebServerOpenApiGenerationContext(this);
		if(element == null) {
			context.docCommentTrees = List.of();
		}
		else if(element.getKind() == ElementKind.METHOD) {
			// We need to get all DCT in the inheritance tree for methods
			ExecutableElement routeElement = (ExecutableElement)element;
			TypeElement controllerElement = (TypeElement)routeElement.getEnclosingElement();

			context.docCommentTrees = this.typeHierarchyExtractor.extractTypeHierarchy(controllerElement).stream()
				.map(typeElement -> ElementFilter.methodsIn(typeElement.getEnclosedElements()).stream()
					.filter(methodElement -> methodElement.equals(routeElement) || this.elementUtils.overrides(routeElement, methodElement, controllerElement))
					.findFirst()
				)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.map(this.docUtils::getDocCommentTree)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		}
		else {
			DocCommentTree docCommentTree = this.docUtils.getDocCommentTree(element);
			context.docCommentTrees = docCommentTree != null ? List.of(docCommentTree) : List.of();
		}
		return context;
	}

	private WebServerOpenApiGenerationContext withDocMode(WebServerOpenApiGenerationContext.DocGenerationMode docMode) {
		WebServerOpenApiGenerationContext context = new WebServerOpenApiGenerationContext(this);
		if(docMode == WebServerOpenApiGenerationContext.DocGenerationMode.DESCRIPTION) {
			context.docInheritMode = WebServerOpenApiGenerationContext.DocGenerationMode.DESCRIPTION;
		}
		else if(docMode == WebServerOpenApiGenerationContext.DocGenerationMode.PARAMETER) {
			context.docInheritMode = WebServerOpenApiGenerationContext.DocGenerationMode.PARAMETER;
		}
		else if(docMode == WebServerOpenApiGenerationContext.DocGenerationMode.RESPONSE) {
			context.docInheritMode = WebServerOpenApiGenerationContext.DocGenerationMode.RESPONSE;
		}
		context.docMode = docMode;
		return context;
	}

	private WebServerOpenApiGenerationContext withDocCommentTrees(List<DocCommentTree> docCommentTrees) {
		WebServerOpenApiGenerationContext context = new WebServerOpenApiGenerationContext(this);
		context.docCommentTrees = docCommentTrees;
		return context;
	}

	private WebServerOpenApiGenerationContext withDocParameterName(String parameterName) {
		WebServerOpenApiGenerationContext context = new WebServerOpenApiGenerationContext(this);
		context.docParameterName = parameterName;
		return context;
	}

	private WebServerOpenApiGenerationContext withDocExceptionName(String exceptionName) {
		WebServerOpenApiGenerationContext context = new WebServerOpenApiGenerationContext(this);
		context.docExceptionName = exceptionName;
		return context;
	}

	private WebServerOpenApiGenerationContext withSchemaOptions(WebServerOpenApiGenerationContext.SchemaGenerationOptions schemaOptions) {
		WebServerOpenApiGenerationContext context = new WebServerOpenApiGenerationContext(this);
		context.schemaOptions = schemaOptions;
		return context;
	}

	public WebServerRouteInfo getWebRoute() {
		return webRoute;
	}

	public List<DocCommentTree> getDocCommentTrees() {
		return this.docCommentTrees;
	}

	private TypeMirror getWebExceptionType() {
		return webExceptionType;
	}

	private TypeMirror getBadRequestExceptionType() {
		return badRequestExceptionType;
	}

	private TypeMirror getForbiddenExceptionType() {
		return forbiddenExceptionType;
	}

	private TypeMirror getInternalServerErrorExceptionType() {
		return internalServerErrorExceptionType;
	}

	private TypeMirror getMethodNotAllowedExceptionType() {
		return methodNotAllowedExceptionType;
	}

	private TypeMirror getNotAcceptableExceptionType() {
		return notAcceptableExceptionType;
	}

	private TypeMirror getNotFoundExceptionType() {
		return notFoundExceptionType;
	}

	private TypeMirror getServiceUnavailableExceptionType() {
		return serviceUnavailableExceptionType;
	}

	private TypeMirror getUnauthorizedExceptionType() {
		return unauthorizedExceptionType;
	}

	private TypeMirror getUnsupportedMediaTypeExceptionType() {
		return unsupportedMediaTypeExceptionType;
	}

	private TypeMirror getClassType() {
		return classType;
	}

	private TypeMirror getObjectType() {
		return objectType;
	}

	private TypeMirror getCharSequenceType() {
		return charSequenceType;
	}

	private TypeMirror getLocalDateType() {
		return localDateType;
	}

	private TypeMirror getLocalDateTimeType() {
		return localDateTimeType;
	}

	private TypeMirror getZonedDateTimeType() {
		return zonedDateTimeType;
	}

	private TypeMirror getEnumType() {
		return enumType;
	}

	private TypeMirror getCollectionType() {
		return collectionType;
	}

	private TypeMirror getMapType() {
		return mapType;
	}

	private TypeMirror getByteBufType() {
		return byteBufType;
	}

	public Optional<StringBuilder> getSummary() {
		if(this.docCommentTrees == null || this.docCommentTrees.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(this.docCommentTrees.getFirst().accept(this.javadocToOpenApi, this.withDocMode(WebServerOpenApiGenerationContext.DocGenerationMode.SUMMARY)));
	}

	public Optional<StringBuilder> getDescription() {
		if(this.docCommentTrees == null || this.docCommentTrees.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(this.docCommentTrees.getFirst().accept(this.javadocToOpenApi, this.withDocMode(WebServerOpenApiGenerationContext.DocGenerationMode.DESCRIPTION)));
	}

	public Optional<StringBuilder> getVersion() {
		if(this.docCommentTrees == null || this.docCommentTrees.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(this.docCommentTrees.getFirst().accept(this.javadocToOpenApi, this.withDocMode(WebServerOpenApiGenerationContext.DocGenerationMode.VERSION)));
	}

	public Optional<StringBuilder> getContact() {
		if(this.docCommentTrees == null || this.docCommentTrees.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(this.docCommentTrees.getFirst().accept(this.javadocToOpenApi, this.withDocMode(WebServerOpenApiGenerationContext.DocGenerationMode.CONTACT)));
	}

	public Optional<StringBuilder> getParameterDescription(String parameterName) {
		if(parameterName == null || this.docCommentTrees == null || this.docCommentTrees.isEmpty()) {
			return Optional.empty();
		}

		for(DocCommentTree dct : this.docCommentTrees) {
			Optional<ParamTree> paramTreeOptional = dct.getBlockTags().stream()
				.filter(docTree -> docTree.getKind() == DocTree.Kind.PARAM && ((ParamTree)docTree).getName().toString().equals(parameterName))
				.findFirst()
				.map(docTree -> (ParamTree)docTree);

			if(paramTreeOptional.isPresent()) {
				return paramTreeOptional.map(paramTree -> paramTree.accept(this.javadocToOpenApi, this.withDocMode(WebServerOpenApiGenerationContext.DocGenerationMode.PARAMETER)));
			}
		}
		return Optional.empty();
	}

	public List<WebServerOpenApiGenerationContext.ResponseSpec> getResponses(ExecutableElement routeElement, TypeMirror responseBodyType) {
		if(routeElement == null || responseBodyType == null) {
			return List.of();
		}
		List<WebServerOpenApiGenerationContext.ResponseSpec> returnResponses = new ArrayList<>();
		Map<String, WebServerOpenApiGenerationContext.ResponseSpec> thrownResponsesByType = new HashMap<>();

		if(this.docCommentTrees != null && !this.docCommentTrees.isEmpty()) {
			// @return
			for(DocCommentTree dct : this.docCommentTrees) {
				List<ReturnTree> returnTags = dct.getBlockTags().stream()
					.filter(docTree -> docTree.getKind() == DocTree.Kind.RETURN)
					.map(docTree -> (ReturnTree)docTree)
					.collect(Collectors.toList());

				if(!returnTags.isEmpty()) {
					for(ReturnTree returnTag: returnTags) {
						String status = returnTag.getDescription().stream()
							.filter(docTree -> docTree.getKind() == DocTree.Kind.UNKNOWN_INLINE_TAG && ((UnknownInlineTagTree)docTree).getTagName().equalsIgnoreCase("inverno.web.status"))
							.findFirst()
							.map(docTree -> docTree.accept(this.javadocToOpenApi, this.withDocMode(WebServerOpenApiGenerationContext.DocGenerationMode.RESPONSE)))
							.map(StringBuilder::toString)
							.orElse("200");

						StringBuilder description = returnTag.accept(this.javadocToOpenApi, this.withDocMode(WebServerOpenApiGenerationContext.DocGenerationMode.RESPONSE));
						returnResponses.add(new WebServerOpenApiGenerationContext.ResponseSpec(status, description, responseBodyType));
					}
					break;
				}
			}

			// @throws
			for(DocCommentTree dct : this.docCommentTrees) {
				List<ThrowsTree> throwsTags = dct.getBlockTags().stream()
					.filter(docTree -> docTree.getKind() == DocTree.Kind.THROWS)
					.map(docTree -> (ThrowsTree)docTree)
					.collect(Collectors.toList());

				if(!throwsTags.isEmpty()) {
					for(ThrowsTree throwsTag: throwsTags) {
						DocTreePath thrownPath = DocTreePath.getPath(this.docUtils.getPath(routeElement), dct, throwsTag.getExceptionName());
						TypeElement thrownElement = (TypeElement)this.docUtils.getElement(thrownPath);
						if(thrownElement != null) {
							TypeMirror thrownType = thrownElement.asType();
							String status = throwsTag.getDescription().stream()
								.filter(docTree -> docTree.getKind() == DocTree.Kind.UNKNOWN_INLINE_TAG && ((UnknownInlineTagTree)docTree).getTagName().equalsIgnoreCase("inverno.web.status"))
								.findFirst()
								.map(docTree -> docTree.accept(this.javadocToOpenApi, this.withDocMode(WebServerOpenApiGenerationContext.DocGenerationMode.RESPONSE)))
								.map(StringBuilder::toString)
								.orElse(this.getResponseStatus(thrownType));

							StringBuilder description = throwsTag.accept(this.javadocToOpenApi, this.withDocMode(WebServerOpenApiGenerationContext.DocGenerationMode.RESPONSE));
							thrownResponsesByType.put(thrownType.toString(), new WebServerOpenApiGenerationContext.ResponseSpec(status, description, thrownType));
						}
					}
					break;
				}
			}
		}

		if(returnResponses.isEmpty()) {
			returnResponses.add(new WebServerOpenApiGenerationContext.ResponseSpec("200", new StringBuilder("''"), responseBodyType));
		}

		// throws
		for(TypeMirror thrownType : routeElement.getThrownTypes()) {
			thrownResponsesByType.putIfAbsent(thrownType.toString(), new WebServerOpenApiGenerationContext.ResponseSpec(this.getResponseStatus(thrownType), new StringBuilder("''"), thrownType));
		}
		return Stream.concat(returnResponses.stream(), thrownResponsesByType.values().stream()).collect(Collectors.toList());
	}

	private String getResponseStatus(TypeMirror type) {
		String status = "500";
		if(this.typeUtils.isAssignable(type, this.getWebExceptionType())) {
			if(this.typeUtils.isAssignable(type, this.getBadRequestExceptionType())) {
				status = "400";
			}
			else if(this.typeUtils.isAssignable(type, this.getForbiddenExceptionType())) {
				status = "403";
			}
			else if(this.typeUtils.isAssignable(type, this.getInternalServerErrorExceptionType())) {
				status = "500";
			}
			else if(this.typeUtils.isAssignable(type, this.getMethodNotAllowedExceptionType())) {
				status = "405";
			}
			else if(this.typeUtils.isAssignable(type, this.getNotAcceptableExceptionType())) {
				status = "406";
			}
			else if(this.typeUtils.isAssignable(type, this.getNotFoundExceptionType())) {
				status = "404";
			}
			else if(this.typeUtils.isAssignable(type, this.getServiceUnavailableExceptionType())) {
				status = "503";
			}
			else if(this.typeUtils.isAssignable(type, this.getUnauthorizedExceptionType())) {
				status = "401";
			}
			else if(this.typeUtils.isAssignable(type, this.getUnsupportedMediaTypeExceptionType())) {
				status = "415";
			}
		}
		return status;
	}

	public Optional<StringBuilder> getSchema(TypeMirror type, boolean inList) {
		return Optional.ofNullable(type.accept(this.openApiSchemaGenerator, this.withSchemaOptions(new WebServerOpenApiGenerationContext.SchemaGenerationOptions(inList, true)))).filter(sb -> !sb.isEmpty());
	}

	public Optional<StringBuilder> getComponentsSchemas() {
		StringBuilder result = new StringBuilder();

		Set<String> generatedSchemas = new HashSet<>();
		while(generatedSchemas.size() < this.componentSchemaTypes.size()) {
			for(Map.Entry<String, DeclaredType> e : new HashMap<>(this.componentSchemaTypes).entrySet()) {
				if(generatedSchemas.add(e.getValue().toString())) {
					StringBuilder schema = e.getValue().accept(this.openApiSchemaGenerator, this.withIndentDepthAdd(1).withSchemaOptions(new WebServerOpenApiGenerationContext.SchemaGenerationOptions(false, false)));
					if(!schema.isEmpty()) {
						result.append(this.indent(0)).append(e.getKey()).append(":").append(System.lineSeparator());
						result.append(schema).append(System.lineSeparator());
					}
				}
			}
		}
		return Optional.of(result).filter(sb -> !sb.isEmpty());
	}

	private static class JavadocToOpenApi implements DocTreeVisitor<StringBuilder, WebServerOpenApiGenerationContext> {

		@Override
		public StringBuilder visitAttribute(AttributeTree node, WebServerOpenApiGenerationContext context) {
			if(context.docMode == WebServerOpenApiGenerationContext.DocGenerationMode.RICH_TEXT) {
				return new StringBuilder(node.getName().toString()).append("=\"").append(node.getValue().stream().map(docTree -> docTree.accept(this, context.withDocMode(WebServerOpenApiGenerationContext.DocGenerationMode.PLAIN_TEXT))).collect(context.joining())).append("\"");
			}
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitAuthor(AuthorTree node, WebServerOpenApiGenerationContext context) {
			if(context.docMode == WebServerOpenApiGenerationContext.DocGenerationMode.CONTACT) {
				StringBuilder result = new StringBuilder();
				StringBuilder nameBuilder = new StringBuilder(context.indent(1)).append("name: '");
				Optional<StringBuilder> linkBuilderOptional = Optional.empty();
				for(DocTree namePart : node.getName()) {
					if(linkBuilderOptional.isEmpty() && namePart.getKind() == DocTree.Kind.START_ELEMENT && ((StartElementTree)namePart).getName().toString().equalsIgnoreCase("a")) {
						linkBuilderOptional = ((StartElementTree)namePart).getAttributes().stream()
							.filter(attribute -> attribute.getKind() == DocTree.Kind.ATTRIBUTE && ((AttributeTree)attribute).getName().toString().equalsIgnoreCase("href"))
							.findFirst()
							.map(attribute -> {
								StringBuilder linkBuilder = new StringBuilder();
								String value = ((AttributeTree)attribute).getValue().stream().map(docTree -> docTree.accept(this, context.withDocMode(WebServerOpenApiGenerationContext.DocGenerationMode.PLAIN_TEXT))).collect(context.joining()).toString();
								if(value.startsWith("mailto:")) {
									linkBuilder.append(context.indent(1)).append("email: '").append(value.substring(7)).append("'");
								}
								else {
									linkBuilder.append(context.indent(1)).append("url: '").append(value).append("'");
								}
								return linkBuilder;
							});
					}
					else {
						nameBuilder.append(namePart.accept(this, context.withDocMode(WebServerOpenApiGenerationContext.DocGenerationMode.PLAIN_TEXT)));
					}
				}
				result.append(nameBuilder).append("'");
				linkBuilderOptional.ifPresent(link -> result.append(System.lineSeparator()).append(link));
				return result;
			}
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitComment(CommentTree node, WebServerOpenApiGenerationContext context) {
			// HTML comment
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitDeprecated(DeprecatedTree node, WebServerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitDocComment(DocCommentTree node, WebServerOpenApiGenerationContext context) {
			if(context.docMode == WebServerOpenApiGenerationContext.DocGenerationMode.SUMMARY) {
				return new StringBuilder().append("'").append(node.getFirstSentence().stream().map(docTree -> docTree.accept(this, context.withDocMode(WebServerOpenApiGenerationContext.DocGenerationMode.PLAIN_TEXT))).collect(context.joining())).append("'");
			}
			else if(context.docMode == WebServerOpenApiGenerationContext.DocGenerationMode.DESCRIPTION) {
				return new StringBuilder().append("'").append(node.getFullBody().stream().map(docTree -> docTree.accept(this, context.withDocMode(WebServerOpenApiGenerationContext.DocGenerationMode.RICH_TEXT))).collect(context.joining())).append("'");
			}
			else if(context.docMode == WebServerOpenApiGenerationContext.DocGenerationMode.CONTACT) {
				return node.getBlockTags().stream()
					.filter(docTree -> docTree.getKind() == DocTree.Kind.AUTHOR).findFirst()
					.map(authorTree -> authorTree.accept(this, context)).orElse(null);
			}
			else if(context.docMode == WebServerOpenApiGenerationContext.DocGenerationMode.VERSION) {
				return node.getBlockTags().stream()
					.filter(docTree -> docTree.getKind() == DocTree.Kind.VERSION).findFirst()
					.map(versionTree -> versionTree.accept(this, context)).orElse(null);
			}
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitDocRoot(DocRootTree node, WebServerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitEndElement(EndElementTree node, WebServerOpenApiGenerationContext context) {
			if(context.docMode == WebServerOpenApiGenerationContext.DocGenerationMode.RICH_TEXT) {
				return new StringBuilder("</").append(node.getName().toString()).append(">");
			}
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitEntity(EntityTree node, WebServerOpenApiGenerationContext context) {
			return new StringBuilder(node.getName().toString());
		}

		@Override
		public StringBuilder visitErroneous(ErroneousTree node, WebServerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitHidden(HiddenTree node, WebServerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitIdentifier(IdentifierTree node, WebServerOpenApiGenerationContext context) {
			// TODO seems to be the identifier after the @parameter tag for instance
			return new StringBuilder(node.getName().toString());
		}

		@Override
		public StringBuilder visitIndex(IndexTree node, WebServerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitInheritDoc(InheritDocTree node, WebServerOpenApiGenerationContext context) {
			// This basically just indicates that the doc is inherited but it doesn't provide anything...
			// We could maybe navigate here in the type hierarchy but this would require to have:
			// - the executable element for a method description
			// - the executable element AND the parameter name for a parameter description
			//   - we have it from the ParameterTree
			// - the executable element for a return description
			// - the executable element AND the exception name for a throws description
			//   - we have it from the ThrowsTree


			if(context.docCommentTrees.size() > 1) {
				if(context.docInheritMode == WebServerOpenApiGenerationContext.DocGenerationMode.DESCRIPTION) {
					StringBuilder inheritDescription = this.visitDocComment(context.docCommentTrees.get(1), context.withDocMode(WebServerOpenApiGenerationContext.DocGenerationMode.DESCRIPTION).withDocCommentTrees(context.docCommentTrees.subList(1, context.docCommentTrees.size())));
					return inheritDescription.deleteCharAt(0).deleteCharAt(inheritDescription.length() - 1);
				}
				else if(context.docInheritMode == WebServerOpenApiGenerationContext.DocGenerationMode.PARAMETER) {
					if(context.docParameterName != null) {
						for(int i=1;i<context.docCommentTrees.size();i++) {
							DocCommentTree dct = context.docCommentTrees.get(i);

							Optional<ParamTree> paramTreeOptional = dct.getBlockTags().stream()
								.filter(docTree -> docTree.getKind() == DocTree.Kind.PARAM && ((ParamTree)docTree).getName().toString().equals(context.docParameterName))
								.findFirst()
								.map(docTree -> (ParamTree)docTree);

							if(paramTreeOptional.isPresent()) {
								StringBuilder inheritParameterDescription = this.visitParam(paramTreeOptional.get(), context.withDocMode(WebServerOpenApiGenerationContext.DocGenerationMode.PARAMETER).withDocCommentTrees(context.docCommentTrees.subList(i, context.docCommentTrees.size())));
								return inheritParameterDescription.deleteCharAt(0).deleteCharAt(inheritParameterDescription.length() - 1);
							}
						}
					}
				}
				else if(context.docInheritMode == WebServerOpenApiGenerationContext.DocGenerationMode.RESPONSE) {
					if(context.docExceptionName != null) {
						// @throws
						for(int i=1;i<context.docCommentTrees.size();i++) {
							DocCommentTree dct = context.docCommentTrees.get(i);

							Optional<ThrowsTree> throwsTreeOptional = dct.getBlockTags().stream()
								.filter(docTree -> docTree.getKind() == DocTree.Kind.THROWS && ((ThrowsTree)docTree).getExceptionName().toString().equals(context.docExceptionName))
								.findFirst()
								.map(docTree -> (ThrowsTree)docTree);

							if(throwsTreeOptional.isPresent()) {
								StringBuilder inheritReponseDescription = this.visitThrows(throwsTreeOptional.get(), context.withDocMode(WebServerOpenApiGenerationContext.DocGenerationMode.RESPONSE).withDocCommentTrees(context.docCommentTrees.subList(i, context.docCommentTrees.size())));
								return inheritReponseDescription.deleteCharAt(0).deleteCharAt(inheritReponseDescription.length() - 1);
							}
						}
					}
					else {
						// @return
						// There can be more than one to specify multiple response status (xdoclint actualy breaks javadoc generation) but we consider only the first one
						for(int i=1;i<context.docCommentTrees.size();i++) {
							DocCommentTree dct = context.docCommentTrees.get(i);

							Optional<ReturnTree> returnTreeOptional = dct.getBlockTags().stream()
								.filter(docTree -> docTree.getKind() == DocTree.Kind.RETURN)
								.findFirst()
								.map(docTree -> (ReturnTree)docTree);

							if(returnTreeOptional.isPresent()) {
								StringBuilder inheritReponseDescription = this.visitReturn(returnTreeOptional.get(), context.withDocMode(WebServerOpenApiGenerationContext.DocGenerationMode.RESPONSE).withDocCommentTrees(context.docCommentTrees.subList(i, context.docCommentTrees.size())));
								return inheritReponseDescription.deleteCharAt(0).deleteCharAt(inheritReponseDescription.length() - 1);
							}
						}
					}
				}
			}
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitLink(LinkTree node, WebServerOpenApiGenerationContext context) {
			// {@link reference label}
			StringBuilder result = new StringBuilder();
			if(node.getLabel().isEmpty()) {
				result.append(node.getReference().toString());
			}
			else {
				result.append(node.getLabel().stream().map(docTree -> docTree.accept(this, context)).collect(context.joining()));
			}
			return result;
		}

		@Override
		public StringBuilder visitLiteral(LiteralTree node, WebServerOpenApiGenerationContext context) {
			// {@literal a<b>c}
			return node.getBody().accept(this, context);
		}

		@Override
		public StringBuilder visitParam(ParamTree node, WebServerOpenApiGenerationContext context) {
			if(context.docMode == WebServerOpenApiGenerationContext.DocGenerationMode.PARAMETER) {
				return new StringBuilder("'").append(node.getDescription().stream().map(docTree -> docTree.accept(this, context.withDocParameterName(node.getName().toString()).withDocMode(WebServerOpenApiGenerationContext.DocGenerationMode.RICH_TEXT))).collect(context.joining())).append("'");
			}
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitProvides(ProvidesTree node, WebServerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitReference(ReferenceTree node, WebServerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitReturn(ReturnTree node, WebServerOpenApiGenerationContext context) {
			if(context.docMode == WebServerOpenApiGenerationContext.DocGenerationMode.RESPONSE) {
				// @return {@inverno.web.status 201} dslgdfjgdf {@inverno.web.status 200} dslgdfjgdf
				StringBuilder result = new StringBuilder();
				result.append("'");
				if(!node.getDescription().isEmpty()) {
					result.append(node.getDescription().stream().map(docTree -> docTree.accept(this, context.withDocMode(WebServerOpenApiGenerationContext.DocGenerationMode.RICH_TEXT))).collect(context.joining()));
				}
				result.append("'");
				return result;
			}
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitSee(SeeTree node, WebServerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitSerial(SerialTree node, WebServerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitSerialData(SerialDataTree node, WebServerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitSerialField(SerialFieldTree node, WebServerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitSince(SinceTree node, WebServerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitStartElement(StartElementTree node, WebServerOpenApiGenerationContext context) {
			if(context.docMode == WebServerOpenApiGenerationContext.DocGenerationMode.RICH_TEXT) {
				StringBuilder result = new StringBuilder();
				result.append("<").append(node.getName().toString());
				if(!node.getAttributes().isEmpty()) {
					result.append(" ");
					result.append(node.getAttributes().stream().map(docTree -> docTree.accept(this, context)).collect(context.joining(" ")));
				}

				if(node.isSelfClosing()) {
					result.append("/>");
				}
				else {
					result.append(">");
				}
				return result;
			}
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitText(TextTree node, WebServerOpenApiGenerationContext context) {
			return new StringBuilder().append(node.getBody().replace("'", "''"));
		}

		@Override
		public StringBuilder visitThrows(ThrowsTree node, WebServerOpenApiGenerationContext context) {
			if(context.docMode == WebServerOpenApiGenerationContext.DocGenerationMode.RESPONSE) {
				// @throws Exception {@inverno.web.status 500} dsfdg
				StringBuilder result = new StringBuilder();
				result.append("'");
				if(!node.getDescription().isEmpty()) {
					result.append(node.getDescription().stream().map(docTree -> docTree.accept(this, context.withDocExceptionName(node.getExceptionName().toString()).withDocMode(WebServerOpenApiGenerationContext.DocGenerationMode.RICH_TEXT))).collect(context.joining()));
				}
				result.append("'");
				return result;
			}
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitUnknownBlockTag(UnknownBlockTagTree node, WebServerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitUnknownInlineTag(UnknownInlineTagTree node, WebServerOpenApiGenerationContext context) {
			if(context.docMode == WebServerOpenApiGenerationContext.DocGenerationMode.RESPONSE) {
				if(node.getTagName().equalsIgnoreCase("inverno.web.status")) {
					// TODO support reference?: {@inverno.web.status Status#UNSUPPORTED_MEDIA_TYPE}
					return new StringBuilder().append(node.getContent().stream().map(docTree -> docTree.accept(this, context.withDocMode(WebServerOpenApiGenerationContext.DocGenerationMode.PLAIN_TEXT))).collect(context.joining()));
				}
			}
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitUses(UsesTree node, WebServerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitValue(ValueTree node, WebServerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitVersion(VersionTree node, WebServerOpenApiGenerationContext context) {
			if(context.docMode == WebServerOpenApiGenerationContext.DocGenerationMode.VERSION) {
				return new StringBuilder().append("'").append(node.getBody().stream().map(docTree -> docTree.accept(this, context.withDocMode(WebServerOpenApiGenerationContext.DocGenerationMode.PLAIN_TEXT))).collect(Collectors.joining())).append("'");
			}
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitOther(DocTree node, WebServerOpenApiGenerationContext context) {
			return new StringBuilder();
		}
	}

	private static class OpenApiSchemaGenerator implements TypeVisitor<StringBuilder, WebServerOpenApiGenerationContext> {

		@Override
		public StringBuilder visit(TypeMirror type, WebServerOpenApiGenerationContext context) {
			Objects.requireNonNull(type, "type");
			if(type instanceof PrimitiveType) {
				return this.visitPrimitive((PrimitiveType)type, context);
			}
			else if(type instanceof ArrayType) {
				return this.visitArray((ArrayType)type, context);
			}
			else if(type instanceof DeclaredType) {
				return this.visitDeclared((DeclaredType)type, context);
			}
			else if(type instanceof WildcardType) {
				return this.visitWildcard((WildcardType)type, context);
			}
			else if(type instanceof IntersectionType) {
				return this.visitIntersection((IntersectionType)type, context);
			}
			else if(type instanceof NoType) {
				return this.visitNoType((NoType)type, context);
			}
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitPrimitive(PrimitiveType type, WebServerOpenApiGenerationContext context) {
			Objects.requireNonNull(type, "type");
			StringBuilder result = new StringBuilder();
			if(context.schemaOptions.inList) {
				result.append(context.indentList(0));
			}
			else {
				result.append(context.indent(0));
			}
			result.append("type: ");

			if(type.getKind() == TypeKind.CHAR) {
				result.append("string");
			}
			else if(type.getKind() == TypeKind.BOOLEAN) {
				result.append("boolean");
			}
			else if(type.getKind() == TypeKind.BYTE || type.getKind() == TypeKind.SHORT || type.getKind() == TypeKind.INT) {
				result.append("integer").append(System.lineSeparator());
				result.append(context.indent(0)).append("format: ").append("int32");
			}
			else if(type.getKind() == TypeKind.LONG) {
				result.append("integer").append(System.lineSeparator());
				result.append(context.indent(0)).append("format: ").append("int64");
			}
			else if(type.getKind() == TypeKind.FLOAT) {
				result.append("number").append(System.lineSeparator());
				result.append(context.indent(0)).append("format: ").append("float");
			}
			else if(type.getKind() == TypeKind.DOUBLE) {
				result.append("number").append(System.lineSeparator());
				result.append(context.indent(0)).append("format: ").append("double");
			}
			return result;
		}

		@Override
		public StringBuilder visitNull(NullType type, WebServerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitArray(ArrayType type, WebServerOpenApiGenerationContext context) {
			Objects.requireNonNull(type, "type");
			StringBuilder result = new StringBuilder();
			if(context.schemaOptions.inList) {
				result.append(context.indentList(0));
			}
			else {
				result.append(context.indent(0));
			}
			result.append("type: ");

			result.append("array").append(System.lineSeparator());
			result.append(context.indent(0)).append("items: ").append(System.lineSeparator());

			StringBuilder componentSchema = this.visit(type.getComponentType(), context.withIndentDepthAdd(1).withSchemaOptions(new WebServerOpenApiGenerationContext.SchemaGenerationOptions(false, true)));
			if(!componentSchema.isEmpty()) {
				result.append(componentSchema);
			}
			else {
				result.append(context.indent(1)).append("type: object");
			}
			return result;
		}

		@Override
		public StringBuilder visitDeclared(DeclaredType type, WebServerOpenApiGenerationContext context) {
			Objects.requireNonNull(type, "type");

			try {
				return this.visit(context.typeUtils.unboxedType(type), context);
			}
			catch (Exception e) {
				// type is not a primitive wrapper
			}

			StringBuilder result = new StringBuilder();
			if(context.schemaOptions.inList) {
				result.append(context.indentList(0));
			}
			else {
				result.append(context.indent(0));
			}
			result.append("type: ");

			if(context.typeUtils.isAssignable(context.typeUtils.erasure(type), context.getClassType())) {
				result.append("string");
			}
			else if(context.typeUtils.isAssignable(type, context.getCharSequenceType())) {
				result.append("string");
			}
			else if(context.typeUtils.isSameType(type, context.getByteBufType())) {
				result.append("string");
			}
			else if(context.typeUtils.isSameType(type, context.getLocalDateType())) {
				result.append("string").append(System.lineSeparator());
				result.append(context.indent(0)).append("format: ").append("date");
			}
			else if(context.typeUtils.isSameType(type, context.getLocalDateTimeType())) {
				result.append("string").append(System.lineSeparator());
				result.append(context.indent(0)).append("format: ").append("date-time");
			}
			else if(context.typeUtils.isSameType(type, context.getZonedDateTimeType())) {
				result.append("string").append(System.lineSeparator());
				result.append(context.indent(0)).append("format: ").append("date-time");
			}
			else if(context.typeUtils.isAssignable(context.typeUtils.erasure(type), context.getEnumType())) {
				if(context.schemaOptions.useReference) {
					StringBuilder reference = new StringBuilder();
					if(context.schemaOptions.inList) {
						reference.append(context.indentList(0));
					}
					else {
						reference.append(context.indent(0));
					}
					reference.append("$ref: '#/components/schemas/").append(type).append("'");
					context.componentSchemaTypes.put(type.toString(), type);

					return reference;
				}
				else {
					result.append("string").append(System.lineSeparator());
					result.append(context.indent(0)).append("enum: ").append(System.lineSeparator());
					result.append(context.typeUtils.asElement(type).getEnclosedElements().stream()
						.filter(element -> element.getKind() == ElementKind.ENUM_CONSTANT)
						.map(enumConstant -> new StringBuilder(context.indentList(1)).append(enumConstant))
						.collect(context.joining(System.lineSeparator()))
					);
				}
			}
			else if(context.typeUtils.isAssignable(context.typeUtils.erasure(type), context.getCollectionType())) {
				result.append("array").append(System.lineSeparator());
				result.append(context.indent(0)).append("items: ").append(System.lineSeparator());
				StringBuilder componentSchema = this.visit(type.getTypeArguments().getFirst(), context.withIndentDepthAdd(1).withSchemaOptions(new WebServerOpenApiGenerationContext.SchemaGenerationOptions(false, true)));
				if(!componentSchema.isEmpty()) {
					result.append(componentSchema);
				}
				else {
					result.append(context.indent(1)).append("type: object");
				}
			}
			else if(context.typeUtils.isAssignable(context.typeUtils.erasure(type), context.getMapType())) {
				result.append("object").append(System.lineSeparator());
				result.append(context.indent(0)).append("additionalProperties: ").append(System.lineSeparator());
				StringBuilder componentSchema = this.visit(type.getTypeArguments().get(1), context.withIndentDepthAdd(1).withSchemaOptions(new WebServerOpenApiGenerationContext.SchemaGenerationOptions(false, true)));
				if(!componentSchema.isEmpty()) {
					result.append(componentSchema);
				}
				else {
					result.append(context.indent(1)).append("type: object");
				}
			}
			else {
				if(context.schemaOptions.useReference) {
					StringBuilder reference = new StringBuilder();
					if(context.schemaOptions.inList) {
						reference.append(context.indentList(0));
					}
					else {
						reference.append(context.indent(0));
					}
					reference.append("$ref: '#/components/schemas/").append(type).append("'");
					context.componentSchemaTypes.put(type.toString(), type);

					return reference;
				}
				else {
					result.append("object");
					// Look for accessor methods and public field and iterate...

					List<? extends Element> typeMemberElements = context.elementUtils.getAllMembers((TypeElement)context.typeUtils.asElement(type)).stream()
						.filter(element -> !context.typeUtils.isSameType(element.getEnclosingElement().asType(), context.getObjectType()))
						.collect(Collectors.toList());

					StringBuilder publicFieldsProperties = ElementFilter.fieldsIn(typeMemberElements).stream()
						.filter(element -> element.getModifiers().contains(Modifier.PUBLIC) && !element.getModifiers().contains(Modifier.STATIC))
						.map(element -> {
							StringBuilder property = new StringBuilder();

							TypeMirror propertyType = ((ExecutableType)context.typeUtils.asMemberOf(type, element)).getReturnType();

							property.append(context.indent(1)).append(element.getSimpleName().toString()).append(": ").append(System.lineSeparator());
							StringBuilder propertySchema = this.visit(propertyType, context.withIndentDepthAdd(2).withSchemaOptions(new WebServerOpenApiGenerationContext.SchemaGenerationOptions(false, true)));
							if(!propertySchema.isEmpty()) {
								property.append(propertySchema);
							}
							else {
								property.append(context.indent(1)).append("type: object");
							}
							return property;
						})
						.collect(context.joining(System.lineSeparator()));

					Map<String, List<ExecutableElement>> accessorsByPropertyName = ElementFilter.methodsIn(typeMemberElements).stream()
						.filter(element -> element.getParameters().size() <= 1)
						.filter(element -> element.getModifiers().contains(Modifier.PUBLIC) && !element.getModifiers().contains(Modifier.ABSTRACT) && !element.getModifiers().contains(Modifier.STATIC))
						.filter(element -> {
							String elementName = element.getSimpleName().toString();
							if(elementName.startsWith("get")) {
								return element.getParameters().isEmpty() && element.getReturnType().getKind() != TypeKind.VOID;
							}
							else if(elementName.startsWith("set")) {
								return element.getParameters().size() == 1 && element.getReturnType().getKind() == TypeKind.VOID;
							}
							return false;
						})
						.collect(Collectors.groupingBy(element -> element.getSimpleName().toString().substring(3)));

					StringBuilder accessorsProperties = accessorsByPropertyName.entrySet().stream()
						.map(e -> {
							String propertyName = e.getKey();
							propertyName = Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);

							boolean hasGetter = false;
							boolean hasSetter = false;
							Map<String, TypeMirror> propertyTypeByName = new HashMap<>();
							for(ExecutableElement element : e.getValue()) {
								TypeMirror propertyType;
								String accessorName = element.getSimpleName().toString();
								if(accessorName.startsWith("get")) {
									hasGetter = true;
									propertyType = ((ExecutableType)context.typeUtils.asMemberOf(type, element)).getReturnType();
								}
								else if(accessorName.startsWith("set")) {
									hasSetter = true;
									propertyType = ((ExecutableType)context.typeUtils.asMemberOf(type, element)).getParameterTypes().getFirst();
								}
								else {
									throw new IllegalStateException("Element should be an accessor");
								}
								propertyTypeByName.put(propertyType.toString(), propertyType);
							}

							int propertyTypesCount = propertyTypeByName.size();
							if(propertyTypesCount > 0) {
								StringBuilder property = new StringBuilder();
								property.append(context.indent(1)).append(propertyName).append(": ").append(System.lineSeparator());
								if(hasGetter && !hasSetter) {
									property.append(context.indent(2)).append("readOnly: true").append(System.lineSeparator());
								}
								if(!hasGetter) {
									property.append(context.indent(2)).append("writeOnly: true").append(System.lineSeparator());
								}
								if(propertyTypesCount > 1) {
									StringBuilder propertySchema = propertyTypeByName.values().stream()
										.map(propertyType -> this.visit(propertyType, context.withIndentDepthAdd(3).withSchemaOptions(new WebServerOpenApiGenerationContext.SchemaGenerationOptions(true, true))))
										.filter(sb -> !sb.isEmpty())
										.collect(context.joining(System.lineSeparator()));

									if(!propertySchema.isEmpty()) {
										property.append(context.indent(2)).append("oneOf: ").append(System.lineSeparator());
										property.append(propertySchema);
									}
									else {
										property.append(context.indent(1)).append("type: object");
									}

								}
								else {
									StringBuilder propertySchema = this.visit(propertyTypeByName.values().iterator().next(), context.withIndentDepthAdd(2).withSchemaOptions(new WebServerOpenApiGenerationContext.SchemaGenerationOptions(false, true)));
									if(!propertySchema.isEmpty()) {
										property.append(propertySchema);
									}
									else {
										property.append(context.indent(1)).append("type: object");
									}
								}
								return property;
							}
							else {
								return null;
							}
						})
						.filter(Objects::nonNull)
						.collect(context.joining(System.lineSeparator()));

					StringBuilder properties = Stream.of(publicFieldsProperties, accessorsProperties).filter(sb -> !sb.isEmpty()).collect(context.joining(System.lineSeparator()));
					if(!properties.isEmpty()) {
						result.append(System.lineSeparator()).append(context.indent(0)).append("properties: ").append(System.lineSeparator());
						result.append(properties);
					}
				}
			}
			return result;
		}

		@Override
		public StringBuilder visitError(ErrorType type, WebServerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitTypeVariable(TypeVariable type, WebServerOpenApiGenerationContext context) {
			Objects.requireNonNull(type, "type");
			if(type.getLowerBound() != null && type.getLowerBound().getKind() != TypeKind.NULL) {
				return this.visit(type.getLowerBound(), context);
			}
			else {
				StringBuilder result = new StringBuilder();
				if(context.schemaOptions.inList) {
					result.append(context.indentList(0));
				}
				else {
					result.append(context.indent(0));
				}
				result.append("type: ").append("object");
				return result;
			}
		}

		@Override
		public StringBuilder visitWildcard(WildcardType type, WebServerOpenApiGenerationContext context) {
			Objects.requireNonNull(type, "type");
			if(type.getExtendsBound() != null) {
				return this.visit(type.getExtendsBound(), context);
			}
			else {
				StringBuilder result = new StringBuilder();
				if(context.schemaOptions.inList) {
					result.append(context.indentList(0));
				}
				else {
					result.append(context.indent(0));
				}
				result.append("type: ").append("object");
				return result;
			}
		}

		@Override
		public StringBuilder visitExecutable(ExecutableType t, WebServerOpenApiGenerationContext p) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitNoType(NoType t, WebServerOpenApiGenerationContext p) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitUnknown(TypeMirror t, WebServerOpenApiGenerationContext p) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitUnion(UnionType type, WebServerOpenApiGenerationContext context) {
			Objects.requireNonNull(type, "type");
			StringBuilder result = new StringBuilder();
			if(context.schemaOptions.inList) {
				result.append(context.indentList(0));
			}
			else {
				result.append(context.indent(0));
			}
			result.append("type: ").append("object");
			return result;
		}

		@Override
		public StringBuilder visitIntersection(IntersectionType type, WebServerOpenApiGenerationContext context) {
			StringBuilder result = new StringBuilder();
			if(context.schemaOptions.inList) {
				result.append(context.indentList(0));
			}
			else {
				result.append(context.indent(0));
			}
			result.append("type: ").append("object");
			return result;
		}
	}

	public static class ResponseSpec {

		private final String status;

		private final StringBuilder description;

		private final TypeMirror type;

		private ResponseSpec (String status, StringBuilder description, TypeMirror type) {
			this.status = status;
			this.type = type;
			this.description = description;
		}

		public String getStatus() {
			return status;
		}

		public StringBuilder getDescription() {
			return description;
		}

		public TypeMirror getType() {
			return type;
		}
	}
}

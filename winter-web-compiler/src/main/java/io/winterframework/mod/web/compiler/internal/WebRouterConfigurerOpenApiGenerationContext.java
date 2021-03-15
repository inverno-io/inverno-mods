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
package io.winterframework.mod.web.compiler.internal;

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

import com.sun.source.doctree.AttributeTree;
import com.sun.source.doctree.AuthorTree;
import com.sun.source.doctree.CommentTree;
import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocRootTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTree.Kind;
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

import io.netty.buffer.ByteBuf;
import io.winterframework.core.compiler.spi.ModuleQualifiedName;
import io.winterframework.core.compiler.spi.support.AbstractSourceGenerationContext;
import io.winterframework.mod.http.base.BadRequestException;
import io.winterframework.mod.http.base.ForbiddenException;
import io.winterframework.mod.http.base.InternalServerErrorException;
import io.winterframework.mod.http.base.MethodNotAllowedException;
import io.winterframework.mod.http.base.NotAcceptableException;
import io.winterframework.mod.http.base.NotFoundException;
import io.winterframework.mod.http.base.ServiceUnavailableException;
import io.winterframework.mod.http.base.UnauthorizedException;
import io.winterframework.mod.http.base.UnsupportedMediaTypeException;
import io.winterframework.mod.http.base.WebException;
import io.winterframework.mod.web.compiler.spi.WebRouteInfo;

/**
 * <p>
 * Represents a generation context used by the
 * {@link WebRouterConfigurerOpenApiGenerator} during the generation of an Open
 * API specification.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class WebRouterConfigurerOpenApiGenerationContext extends AbstractSourceGenerationContext<WebRouterConfigurerOpenApiGenerationContext, WebRouterConfigurerOpenApiGenerationContext.GenerationMode> {

	protected static final String DEFAULT_INDENT = "    ";
	
	public static enum GenerationMode {
		ROUTER_SPEC,
		CONTROLLER_TAG,
		ROUTE_PATH,
		ROUTE_PARAMETER,
		ROUTE_BODY
	}
	
	private static enum DocGenerationMode {
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
	private final JavadocToOpenApi javadocToOpenApi;
	private final OpenApiSchemaGenerator openApiSchemaGenerator;
	private final Map<String, DeclaredType> componentSchemaTypes;
	private final TypeHierarchyExtractor typeHierarchyExtractor;
	
	private TypeMirror webExceptionType;
	private TypeMirror badRequestExceptionType;
	private TypeMirror forbiddenExceptionType;
	private TypeMirror internalServerErrorExceptionType;
	private TypeMirror methodNotAllowedExceptionType;
	private TypeMirror notAcceptableExceptionType;
	private TypeMirror notFoundExceptionType;
	private TypeMirror serviceUnavailableExceptionType;
	private TypeMirror unauthorizedExceptionType;
	private TypeMirror unsupportedMediaTypeExceptionType;
	private TypeMirror classType;
	private TypeMirror objectType;
	private TypeMirror charSequenceType;
	private TypeMirror localDateType;
	private TypeMirror localDateTimeType;
	private TypeMirror zonedDateTimeType;
	private TypeMirror enumType;
	private TypeMirror collectionType;
	private TypeMirror mapType;
	private TypeMirror byteBufType;
	
	private DocGenerationMode docMode;
	private DocGenerationMode docInheritMode;
	private List<DocCommentTree> docCommentTrees;
	private String docParameterName;
	private String docExceptionName;
	private SchemaGenerationOptions schemaOptions;
	
	private WebRouteInfo webRoute;

	private String indentList;
	
	public WebRouterConfigurerOpenApiGenerationContext(Types typeUtils, Elements elementUtils, DocTrees docUtils, GenerationMode mode) {
		super(typeUtils, elementUtils, mode, DEFAULT_INDENT);
		this.docUtils = docUtils;
		this.javadocToOpenApi = new JavadocToOpenApi();
		this.openApiSchemaGenerator = new OpenApiSchemaGenerator();
		this.componentSchemaTypes = new HashMap<>();
		this.typeHierarchyExtractor = new TypeHierarchyExtractor(this.typeUtils);
		
		this.webExceptionType = this.elementUtils.getTypeElement(WebException.class.getCanonicalName()).asType();
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
	
	private WebRouterConfigurerOpenApiGenerationContext(WebRouterConfigurerOpenApiGenerationContext parentGeneration) {
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
		
		String repeatIndent = "";
		for(int i=0;i<this.indentDepth + depth - 1;i++) {
			repeatIndent += this.indent;
		}
		repeatIndent += this.indentList;
		return repeatIndent;
	}
	
	public DocTrees getDocUtils() {
		return docUtils;
	}
	
	@Override
	public WebRouterConfigurerOpenApiGenerationContext withMode(GenerationMode mode) {
		WebRouterConfigurerOpenApiGenerationContext context = new WebRouterConfigurerOpenApiGenerationContext(this);
		context.mode = mode;
		return context;
	}

	@Override
	public WebRouterConfigurerOpenApiGenerationContext withIndentDepth(int indentDepth) {
		WebRouterConfigurerOpenApiGenerationContext context = new WebRouterConfigurerOpenApiGenerationContext(this);
		context.indentDepth = indentDepth;
		return context;
	}

	@Override
	public WebRouterConfigurerOpenApiGenerationContext withModule(ModuleQualifiedName moduleQualifiedName) {
		WebRouterConfigurerOpenApiGenerationContext context = new WebRouterConfigurerOpenApiGenerationContext(this);
		context.moduleQualifiedName = moduleQualifiedName;
		return context;
	}
	
	public WebRouterConfigurerOpenApiGenerationContext withWebRoute(WebRouteInfo webRoute) {
		WebRouterConfigurerOpenApiGenerationContext context = new WebRouterConfigurerOpenApiGenerationContext(this);
		context.webRoute = webRoute;
		return context;
	}
	
	public WebRouterConfigurerOpenApiGenerationContext withDocElement(Element element) {
		WebRouterConfigurerOpenApiGenerationContext context = new WebRouterConfigurerOpenApiGenerationContext(this);
		if(element == null) {
			context.docCommentTrees = List.of();
		}
		
		// We need to get all DCT in the inheritance tree for methods
		if(element.getKind() == ElementKind.METHOD) {
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
			context.docCommentTrees = List.of(this.docUtils.getDocCommentTree(element));
		}
		return context;
	}
	
	private WebRouterConfigurerOpenApiGenerationContext withDocMode(DocGenerationMode docMode) {
		WebRouterConfigurerOpenApiGenerationContext context = new WebRouterConfigurerOpenApiGenerationContext(this);
		if(docMode == DocGenerationMode.DESCRIPTION) {
			context.docInheritMode = DocGenerationMode.DESCRIPTION;
		}
		else if(docMode == DocGenerationMode.PARAMETER) {
			context.docInheritMode = DocGenerationMode.PARAMETER;
		}
		else if(docMode == DocGenerationMode.RESPONSE) {
			context.docInheritMode = DocGenerationMode.RESPONSE;
		}
		context.docMode = docMode;
		return context;
	}
	
	private WebRouterConfigurerOpenApiGenerationContext withDocCommentTrees(List<DocCommentTree> docCommentTrees) {
		WebRouterConfigurerOpenApiGenerationContext context = new WebRouterConfigurerOpenApiGenerationContext(this);
		context.docCommentTrees = docCommentTrees;
		return context;
	}
	
	private WebRouterConfigurerOpenApiGenerationContext withDocParameterName(String parameterName) {
		WebRouterConfigurerOpenApiGenerationContext context = new WebRouterConfigurerOpenApiGenerationContext(this);
		context.docParameterName = parameterName;
		return context;
	}
	
	private WebRouterConfigurerOpenApiGenerationContext withDocExceptionName(String exceptionName) {
		WebRouterConfigurerOpenApiGenerationContext context = new WebRouterConfigurerOpenApiGenerationContext(this);
		context.docExceptionName = exceptionName;
		return context;
	}
	
	private WebRouterConfigurerOpenApiGenerationContext withSchemaOptions(SchemaGenerationOptions schemaOptions) {
		WebRouterConfigurerOpenApiGenerationContext context = new WebRouterConfigurerOpenApiGenerationContext(this);
		context.schemaOptions = schemaOptions;
		return context;
	}
	
	public WebRouteInfo getWebRoute() {
		return webRoute;
	}
	
	public List<DocCommentTree> getDocCommentTrees() {
		return this.docCommentTrees;
	}
	
	private TypeMirror getWebExceptionType() {
		return webExceptionType != null ? webExceptionType : this.parentGeneration.getWebExceptionType();
	}

	private TypeMirror getBadRequestExceptionType() {
		return badRequestExceptionType != null ? badRequestExceptionType : this.parentGeneration.getBadRequestExceptionType();
	}

	private TypeMirror getForbiddenExceptionType() {
		return forbiddenExceptionType != null ? forbiddenExceptionType : this.parentGeneration.getForbiddenExceptionType();
	}

	private TypeMirror getInternalServerErrorExceptionType() {
		return internalServerErrorExceptionType != null ? internalServerErrorExceptionType : this.parentGeneration.getInternalServerErrorExceptionType();
	}

	private TypeMirror getMethodNotAllowedExceptionType() {
		return methodNotAllowedExceptionType != null ? methodNotAllowedExceptionType : this.parentGeneration.getMethodNotAllowedExceptionType();
	}

	private TypeMirror getNotAcceptableExceptionType() {
		return notAcceptableExceptionType != null ? notAcceptableExceptionType : this.parentGeneration.getNotAcceptableExceptionType();
	}

	private TypeMirror getNotFoundExceptionType() {
		return notFoundExceptionType != null ? notFoundExceptionType : this.parentGeneration.getNotFoundExceptionType();
	}

	private TypeMirror getServiceUnavailableExceptionType() {
		return serviceUnavailableExceptionType != null ? serviceUnavailableExceptionType : this.parentGeneration.getServiceUnavailableExceptionType();
	}

	private TypeMirror getUnauthorizedExceptionType() {
		return unauthorizedExceptionType != null ? unauthorizedExceptionType : this.parentGeneration.getUnauthorizedExceptionType();
	}

	private TypeMirror getUnsupportedMediaTypeExceptionType() {
		return unsupportedMediaTypeExceptionType != null ? unsupportedMediaTypeExceptionType : this.parentGeneration.getUnsupportedMediaTypeExceptionType();
	}

	private TypeMirror getClassType() {
		return classType != null ? classType : this.parentGeneration.getClassType();
	}
	
	private TypeMirror getObjectType() {
		return objectType != null ? objectType : this.parentGeneration.getObjectType();
	}
	
	private TypeMirror getCharSequenceType() {
		return charSequenceType != null ? charSequenceType : this.parentGeneration.getCharSequenceType();
	}
	
	private TypeMirror getLocalDateType() {
		return localDateType != null ? charSequenceType : this.parentGeneration.getCharSequenceType();
	}
	
	private TypeMirror getLocalDateTimeType() {
		return localDateTimeType != null ? localDateTimeType : this.parentGeneration.getLocalDateTimeType();
	}
	
	private TypeMirror getZonedDateTimeType() {
		return zonedDateTimeType != null ? zonedDateTimeType : this.parentGeneration.getZonedDateTimeType();
	}
	
	private TypeMirror getEnumType() {
		return enumType != null ? enumType : this.parentGeneration.getEnumType();
	}
	
	private TypeMirror getCollectionType() {
		return collectionType != null ? collectionType : this.parentGeneration.getCollectionType();
	}
	
	private TypeMirror getMapType() {
		return mapType != null ? mapType : this.parentGeneration.getMapType();
	}
	
	private TypeMirror getByteBufType() {
		return byteBufType != null ? byteBufType : this.parentGeneration.getByteBufType();
	}
	
	public Optional<StringBuilder> getSummary() {
		if(this.docCommentTrees == null || this.docCommentTrees.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(this.docCommentTrees.get(0).accept(this.javadocToOpenApi, this.withDocMode(DocGenerationMode.SUMMARY)));
	}
	
	public Optional<StringBuilder> getDescription() {
		if(this.docCommentTrees == null || this.docCommentTrees.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(this.docCommentTrees.get(0).accept(this.javadocToOpenApi, this.withDocMode(DocGenerationMode.DESCRIPTION)));
	}
	
	public Optional<StringBuilder> getVersion() {
		if(this.docCommentTrees == null || this.docCommentTrees.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(this.docCommentTrees.get(0).accept(this.javadocToOpenApi, this.withDocMode(DocGenerationMode.VERSION)));
	}
	
	public Optional<StringBuilder> getContact() {
		if(this.docCommentTrees == null || this.docCommentTrees.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(this.docCommentTrees.get(0).accept(this.javadocToOpenApi, this.withDocMode(DocGenerationMode.CONTACT)));
	}
	
	public Optional<StringBuilder> getParameterDescription(String parameterName) {
		if(parameterName == null || this.docCommentTrees == null || this.docCommentTrees.isEmpty()) {
			return Optional.empty();
		}
		
		for(DocCommentTree dct : this.docCommentTrees) {
			Optional<ParamTree> paramTreeOptional = dct.getBlockTags().stream()
				.filter(docTree -> docTree.getKind() == Kind.PARAM && ((ParamTree)docTree).getName().toString().equals(parameterName))
				.findFirst()
				.map(docTree -> (ParamTree)docTree);
			
			if(paramTreeOptional.isPresent()) {
				return paramTreeOptional.map(paramTree -> paramTree.accept(this.javadocToOpenApi, this.withDocMode(DocGenerationMode.PARAMETER)));
			}
		}
		return Optional.empty();
	}
	
	public List<ResponseSpec> getResponses(ExecutableElement routeElement, TypeMirror responseBodyType) {
		if(routeElement == null || responseBodyType == null) {
			return List.of();
		}
		List<ResponseSpec> returnResponses = new ArrayList<>();
		Map<String, ResponseSpec> thrownResponsesByType = new HashMap<>();
		
		if(this.docCommentTrees != null && !this.docCommentTrees.isEmpty()) {
			// @return
			for(DocCommentTree dct : this.docCommentTrees) {
				List<ReturnTree> returnTags = dct.getBlockTags().stream()
					.filter(docTree -> docTree.getKind() == Kind.RETURN)
					.map(docTree -> (ReturnTree)docTree)
					.collect(Collectors.toList());
				
				if(!returnTags.isEmpty()) {
					for(ReturnTree returnTag: returnTags) {
						String status = returnTag.getDescription().stream()
							.filter(docTree -> docTree.getKind() == Kind.UNKNOWN_INLINE_TAG && ((UnknownInlineTagTree)docTree).getTagName().equalsIgnoreCase("winter.web.status"))
							.findFirst()
							.map(docTree -> docTree.accept(this.javadocToOpenApi, this.withDocMode(DocGenerationMode.RESPONSE)))
							.map(StringBuilder::toString)
							.orElse("200");
						
						StringBuilder description = returnTag.accept(this.javadocToOpenApi, this.withDocMode(DocGenerationMode.RESPONSE)); 
						returnResponses.add(new ResponseSpec(status, description, responseBodyType));
					}
					break;
				}
			}
			
			// @throws
			for(DocCommentTree dct : this.docCommentTrees) {
				List<ThrowsTree> throwsTags = dct.getBlockTags().stream()
					.filter(docTree -> docTree.getKind() == Kind.THROWS)
					.map(docTree -> (ThrowsTree)docTree)
					.collect(Collectors.toList());
				
				if(!throwsTags.isEmpty()) {
					for(ThrowsTree throwsTag: throwsTags) {
						DocTreePath thrownPath = DocTreePath.getPath(this.docUtils.getPath(routeElement), dct, throwsTag.getExceptionName());
						TypeElement thrownElement = (TypeElement)this.docUtils.getElement(thrownPath);
						if(thrownElement != null) {
							TypeMirror thrownType = thrownElement.asType();
							String status = throwsTag.getDescription().stream()
									.filter(docTree -> docTree.getKind() == Kind.UNKNOWN_INLINE_TAG && ((UnknownInlineTagTree)docTree).getTagName().equalsIgnoreCase("winter.web.status"))
									.findFirst()
									.map(docTree -> docTree.accept(this.javadocToOpenApi, this.withDocMode(DocGenerationMode.RESPONSE)))
									.map(StringBuilder::toString)
									.orElse(this.getResponseStatus(thrownType));
						
							StringBuilder description = throwsTag.accept(this.javadocToOpenApi, this.withDocMode(DocGenerationMode.RESPONSE));
							thrownResponsesByType.put(thrownType.toString(), new ResponseSpec(status, description, thrownType));
						}
					}
					break;
				}
			}
		}
		
		if(returnResponses.isEmpty()) {
			returnResponses.add(new ResponseSpec("200", new StringBuilder("''"), responseBodyType));
		}
		
		// throws
		for(TypeMirror thrownType : routeElement.getThrownTypes()) {
			thrownResponsesByType.putIfAbsent(thrownType.toString(), new ResponseSpec(this.getResponseStatus(thrownType), new StringBuilder("''"), thrownType));
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
		return Optional.ofNullable(type.accept(this.openApiSchemaGenerator, this.withSchemaOptions(new SchemaGenerationOptions(inList, true)))).filter(sb -> sb.length() > 0);
	}
	
	public Optional<StringBuilder> getComponentsSchemas() {
		StringBuilder result = new StringBuilder();
		
		Set<String> generatedSchemas = new HashSet<>();
		while(generatedSchemas.size() < this.componentSchemaTypes.size()) {
			for(Map.Entry<String, DeclaredType> e : new HashMap<>(this.componentSchemaTypes).entrySet()) {
				if(generatedSchemas.add(e.getValue().toString())) {
					StringBuilder schema = e.getValue().accept(this.openApiSchemaGenerator, this.withIndentDepthAdd(1).withSchemaOptions(new SchemaGenerationOptions(false, false)));
					if(schema.length() > 0) {
						result.append(this.indent(0)).append(e.getKey()).append(": \n");
						result.append(schema).append("\n");
					}
				}
			}
		}
		return Optional.of(result).filter(sb -> sb.length() > 0);
	}
	
	private static class JavadocToOpenApi implements DocTreeVisitor<StringBuilder, WebRouterConfigurerOpenApiGenerationContext> {

		@Override
		public StringBuilder visitAttribute(AttributeTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			if(context.docMode == DocGenerationMode.RICH_TEXT) {
				return new StringBuilder(node.getName().toString()).append("=\"").append(node.getValue().stream().map(docTree -> docTree.accept(this, context.withDocMode(DocGenerationMode.PLAIN_TEXT))).collect(context.joining())).append("\"");
			}
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitAuthor(AuthorTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			if(context.docMode == DocGenerationMode.CONTACT) {
				StringBuilder result = new StringBuilder();
				StringBuilder nameBuilder = new StringBuilder(context.indent(1)).append("name: '");
				Optional<StringBuilder> linkBuilderOptional = Optional.empty();
				for(DocTree namePart : ((AuthorTree)node).getName()) {
					if(!linkBuilderOptional.isPresent() && namePart.getKind() == Kind.START_ELEMENT && ((StartElementTree)namePart).getName().toString().equalsIgnoreCase("a")) {
						linkBuilderOptional = ((StartElementTree)namePart).getAttributes().stream()
							.filter(attribute -> attribute.getKind() == Kind.ATTRIBUTE && ((AttributeTree)attribute).getName().toString().equalsIgnoreCase("href"))
							.findFirst()
							.map(attribute -> {
								StringBuilder linkBuilder = new StringBuilder();
								String value = ((AttributeTree)attribute).getValue().stream().map(docTree -> docTree.accept(this, context.withDocMode(DocGenerationMode.PLAIN_TEXT))).collect(context.joining()).toString();
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
						nameBuilder.append(namePart.accept(this, context.withDocMode(DocGenerationMode.PLAIN_TEXT)));
					}
				}
				result.append(nameBuilder).append("'");
				linkBuilderOptional.ifPresent(link -> result.append("\n").append(link));
				return result;
			}
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitComment(CommentTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			// HTML comment
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitDeprecated(DeprecatedTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitDocComment(DocCommentTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			if(context.docMode == DocGenerationMode.SUMMARY) {
				return new StringBuilder().append("'").append(node.getFirstSentence().stream().map(docTree -> docTree.accept(this, context.withDocMode(DocGenerationMode.PLAIN_TEXT))).collect(context.joining())).append("'");
			}
			else if(context.docMode == DocGenerationMode.DESCRIPTION) {
				return new StringBuilder().append("'").append(node.getFullBody().stream().map(docTree -> docTree.accept(this, context.withDocMode(DocGenerationMode.RICH_TEXT))).collect(context.joining())).append("'");
			}
			else if(context.docMode == DocGenerationMode.CONTACT) {
				return node.getBlockTags().stream()
					.filter(docTree -> docTree.getKind() == Kind.AUTHOR).findFirst()
					.map(authorTree -> authorTree.accept(this, context)).orElse(null);
			}
			else if(context.docMode == DocGenerationMode.VERSION) {
				return node.getBlockTags().stream()
					.filter(docTree -> docTree.getKind() == Kind.VERSION).findFirst()
					.map(versionTree -> versionTree.accept(this, context)).orElse(null);
			}
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitDocRoot(DocRootTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitEndElement(EndElementTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			if(context.docMode == DocGenerationMode.RICH_TEXT) {
				return new StringBuilder("</").append(node.getName().toString()).append(">");
			}
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitEntity(EntityTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			return new StringBuilder(node.getName().toString());
		}

		@Override
		public StringBuilder visitErroneous(ErroneousTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitHidden(HiddenTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitIdentifier(IdentifierTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			// TODO seems to be the identifier after the @parameter tag for instance
			return new StringBuilder(node.getName().toString());
		}

		@Override
		public StringBuilder visitIndex(IndexTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitInheritDoc(InheritDocTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			// This basically just indicates that the doc is inherited but it doesn't provide anything...
			// We could maybe navigate here in the type hierarchy but this would require to have:
			// - the executable element for a method description
			// - the executable element AND the parameter name for a parameter description
			//   - we have it from the ParameterTree
			// - the executable element for a return description
			// - the executable element AND the exception name for a throws description
			//   - we have it from the ThrowsTree
			
			
			if(context.docCommentTrees.size() > 1) {
				if(context.docInheritMode == DocGenerationMode.DESCRIPTION) {
					StringBuilder inheritDescription = this.visitDocComment(context.docCommentTrees.get(1), context.withDocMode(DocGenerationMode.DESCRIPTION).withDocCommentTrees(context.docCommentTrees.subList(1, context.docCommentTrees.size())));
					return inheritDescription.deleteCharAt(0).deleteCharAt(inheritDescription.length() - 1);
				}
				else if(context.docInheritMode == DocGenerationMode.PARAMETER) {
					if(context.docParameterName != null) {
						for(int i=1;i<context.docCommentTrees.size();i++) {
							DocCommentTree dct = context.docCommentTrees.get(i);
							
							Optional<ParamTree> paramTreeOptional = dct.getBlockTags().stream()
								.filter(docTree -> docTree.getKind() == Kind.PARAM && ((ParamTree)docTree).getName().toString().equals(context.docParameterName))
								.findFirst()
								.map(docTree -> (ParamTree)docTree);
							
							if(paramTreeOptional.isPresent()) {
								StringBuilder inheritParameterDescription = this.visitParam(paramTreeOptional.get(), context.withDocMode(DocGenerationMode.PARAMETER).withDocCommentTrees(context.docCommentTrees.subList(i, context.docCommentTrees.size())));
								return inheritParameterDescription.deleteCharAt(0).deleteCharAt(inheritParameterDescription.length() - 1);
							}
						}
					}
				}
				else if(context.docInheritMode == DocGenerationMode.RESPONSE) {
					if(context.docExceptionName != null) {
						// @throws
						for(int i=1;i<context.docCommentTrees.size();i++) {
							DocCommentTree dct = context.docCommentTrees.get(i);
							
							Optional<ThrowsTree> throwsTreeOptional = dct.getBlockTags().stream()
								.filter(docTree -> docTree.getKind() == Kind.THROWS && ((ThrowsTree)docTree).getExceptionName().toString().equals(context.docExceptionName))
								.findFirst()
								.map(docTree -> (ThrowsTree)docTree);
							
							if(throwsTreeOptional.isPresent()) {
								StringBuilder inheritReponseDescription = this.visitThrows(throwsTreeOptional.get(), context.withDocMode(DocGenerationMode.RESPONSE).withDocCommentTrees(context.docCommentTrees.subList(i, context.docCommentTrees.size())));
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
								.filter(docTree -> docTree.getKind() == Kind.RETURN)
								.findFirst()
								.map(docTree -> (ReturnTree)docTree);
							
							if(returnTreeOptional.isPresent()) {
								StringBuilder inheritReponseDescription = this.visitReturn(returnTreeOptional.get(), context.withDocMode(DocGenerationMode.RESPONSE).withDocCommentTrees(context.docCommentTrees.subList(i, context.docCommentTrees.size())));
								return inheritReponseDescription.deleteCharAt(0).deleteCharAt(inheritReponseDescription.length() - 1);
							}
						}
					}
				}
			}
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitLink(LinkTree node, WebRouterConfigurerOpenApiGenerationContext context) {
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
		public StringBuilder visitLiteral(LiteralTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			// {@literal a<b>c}
			return node.getBody().accept(this, context);
		}

		@Override
		public StringBuilder visitParam(ParamTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			if(context.docMode == DocGenerationMode.PARAMETER) {
				return new StringBuilder("'").append(node.getDescription().stream().map(docTree -> docTree.accept(this, context.withDocParameterName(node.getName().toString()).withDocMode(DocGenerationMode.RICH_TEXT))).collect(context.joining())).append("'");
			}
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitProvides(ProvidesTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitReference(ReferenceTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitReturn(ReturnTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			if(context.docMode == DocGenerationMode.RESPONSE) {
				// @return {@winter.web.status 201} dslgdfjgdf {@winter.web.status 200} dslgdfjgdf
				StringBuilder result = new StringBuilder();
				result.append("'");
				if(!node.getDescription().isEmpty()) {
					result.append(node.getDescription().stream().map(docTree -> docTree.accept(this, context.withDocMode(DocGenerationMode.RICH_TEXT))).collect(context.joining()));
				}
				result.append("'");
				return result;
			}
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitSee(SeeTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitSerial(SerialTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitSerialData(SerialDataTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitSerialField(SerialFieldTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitSince(SinceTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitStartElement(StartElementTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			if(context.docMode == DocGenerationMode.RICH_TEXT) {
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
		public StringBuilder visitText(TextTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			return new StringBuilder().append(node.getBody().replace("\'", "''"));
		}

		@Override
		public StringBuilder visitThrows(ThrowsTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			if(context.docMode == DocGenerationMode.RESPONSE) {
				// @throws Exception {@winter.web.status 500} dsfdg
				StringBuilder result = new StringBuilder();
				result.append("'");
				if(!node.getDescription().isEmpty()) {
					result.append(node.getDescription().stream().map(docTree -> docTree.accept(this, context.withDocExceptionName(node.getExceptionName().toString()).withDocMode(DocGenerationMode.RICH_TEXT))).collect(context.joining()));
				}
				result.append("'");
				return result;
			}
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitUnknownBlockTag(UnknownBlockTagTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitUnknownInlineTag(UnknownInlineTagTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			if(context.docMode == DocGenerationMode.RESPONSE) {
				if(node.getTagName().equalsIgnoreCase("winter.web.status")) {
					// TODO support reference?: {@winter.web.status Status#UNSUPPORTED_MEDIA_TYPE}
					return new StringBuilder().append(node.getContent().stream().map(docTree -> docTree.accept(this, context.withDocMode(DocGenerationMode.PLAIN_TEXT))).collect(context.joining()));
				}
			}
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitUses(UsesTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitValue(ValueTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitVersion(VersionTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			if(context.docMode == DocGenerationMode.VERSION) {
				return new StringBuilder().append("'").append(node.getBody().stream().map(docTree -> docTree.accept(this, context.withDocMode(DocGenerationMode.PLAIN_TEXT))).collect(Collectors.joining())).append("'");
			}
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitOther(DocTree node, WebRouterConfigurerOpenApiGenerationContext context) {
			return new StringBuilder();
		}
	}
	
	private static class OpenApiSchemaGenerator implements TypeVisitor<StringBuilder, WebRouterConfigurerOpenApiGenerationContext> {

		@Override
		public StringBuilder visit(TypeMirror type, WebRouterConfigurerOpenApiGenerationContext context) {
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
		public StringBuilder visitPrimitive(PrimitiveType type, WebRouterConfigurerOpenApiGenerationContext context) {
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
				result.append("integer").append("\n");
				result.append(context.indent(0)).append("format: ").append("int32");
			}
			else if(type.getKind() == TypeKind.LONG) {
				result.append("integer").append("\n");
				result.append(context.indent(0)).append("format: ").append("int64");
			}
			else if(type.getKind() == TypeKind.FLOAT) {
				result.append("number").append("\n");
				result.append(context.indent(0)).append("format: ").append("float");
			}
			else if(type.getKind() == TypeKind.DOUBLE) {
				result.append("number").append("\n");
				result.append(context.indent(0)).append("format: ").append("double");
			}
			return result;
		}

		@Override
		public StringBuilder visitNull(NullType type, WebRouterConfigurerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitArray(ArrayType type, WebRouterConfigurerOpenApiGenerationContext context) {
			Objects.requireNonNull(type, "type");
			StringBuilder result = new StringBuilder();
			if(context.schemaOptions.inList) {
				result.append(context.indentList(0));
			}
			else {
				result.append(context.indent(0));
			}
			result.append("type: ");
			
			result.append("array").append("\n");
			result.append(context.indent(0)).append("items: ").append("\n");
			
			StringBuilder componentSchema = this.visit(type.getComponentType(), context.withIndentDepthAdd(1).withSchemaOptions(new SchemaGenerationOptions(false, true)));
			if(componentSchema.length() > 0) {
				result.append(componentSchema);
			}
			else {
				result.append(context.indent(1)).append("type: object");
			}
			return result;
		}

		@Override
		public StringBuilder visitDeclared(DeclaredType type, WebRouterConfigurerOpenApiGenerationContext context) {
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
				result.append("string").append("\n");
				result.append(context.indent(0)).append("format: ").append("date");
			}
			else if(context.typeUtils.isSameType(type, context.getLocalDateTimeType())) {
				result.append("string").append("\n");
				result.append(context.indent(0)).append("format: ").append("date-time");
			}
			else if(context.typeUtils.isSameType(type, context.getZonedDateTimeType())) {
				result.append("string").append("\n");
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
					reference.append("$ref: '#/components/schemas/").append(type.toString()).append("'");
					context.componentSchemaTypes.put(type.toString(), type);

					return reference;
				}
				else {
					result.append("string").append("\n");
					result.append(context.indent(0)).append("enum: ").append("\n");
					result.append(context.typeUtils.asElement(type).getEnclosedElements().stream()
						.filter(element -> element.getKind() == ElementKind.ENUM_CONSTANT)
						.map(enumConstant -> new StringBuilder(context.indentList(1)).append(enumConstant.toString()))
						.collect(context.joining("\n"))
					);
				}
			}
			else if(context.typeUtils.isAssignable(context.typeUtils.erasure(type), context.getCollectionType())) {
				result.append("array").append("\n");
				result.append(context.indent(0)).append("items: ").append("\n");
				StringBuilder componentSchema = this.visit(((DeclaredType)type).getTypeArguments().get(0), context.withIndentDepthAdd(1).withSchemaOptions(new SchemaGenerationOptions(false, true)));
				if(componentSchema.length() > 0) {
					result.append(componentSchema);
				}
				else {
					result.append(context.indent(1)).append("type: object");
				}
			}
			else if(context.typeUtils.isAssignable(context.typeUtils.erasure(type), context.getMapType())) {
				result.append("object").append("\n");
				result.append(context.indent(0)).append("additionalProperties: ").append("\n");
				StringBuilder componentSchema = this.visit(((DeclaredType)type).getTypeArguments().get(1), context.withIndentDepthAdd(1).withSchemaOptions(new SchemaGenerationOptions(false, true)));
				if(componentSchema.length() > 0) {
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
					reference.append("$ref: '#/components/schemas/").append(type.toString()).append("'");
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
							
							property.append(context.indent(1)).append(element.getSimpleName().toString()).append(": ").append("\n");
							StringBuilder propertySchema = this.visit(propertyType, context.withIndentDepthAdd(2).withSchemaOptions(new SchemaGenerationOptions(false, true)));
							if(propertySchema.length() > 0) {
								property.append(propertySchema);
							}
							else {
								property.append(context.indent(1)).append("type: object");
							}
							return property;
						})
						.collect(context.joining("\n"));
					
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
									propertyType = ((ExecutableType)context.typeUtils.asMemberOf(type, element)).getParameterTypes().get(0);
								}
								else {
									throw new IllegalStateException("Element should be an accessor");									
								}
								propertyTypeByName.put(propertyType.toString(), propertyType);
							}
							
							int propertyTypesCount = propertyTypeByName.size();
							if(propertyTypesCount > 0) {
								StringBuilder property = new StringBuilder();
								property.append(context.indent(1)).append(propertyName).append(": ").append("\n");
								if(hasGetter && !hasSetter) {
									property.append(context.indent(2)).append("readOnly: true").append("\n");
								}
								if(!hasGetter && hasSetter) {
									property.append(context.indent(2)).append("writeOnly: true").append("\n");
								}
								if(propertyTypesCount > 1) {
									StringBuilder propertySchema = propertyTypeByName.values().stream()
										.map(propertyType -> this.visit(propertyType, context.withIndentDepthAdd(3).withSchemaOptions(new SchemaGenerationOptions(true, true))))
										.filter(sb -> sb.length() > 0)
										.collect(context.joining("\n"));
									
									if(propertySchema.length() > 0) {
										property.append(context.indent(2)).append("oneOf: ").append("\n");
										property.append(propertySchema);
									}
									else {
										property.append(context.indent(1)).append("type: object");
									}
									
								}
								else {
									StringBuilder propertySchema = this.visit(propertyTypeByName.values().iterator().next(), context.withIndentDepthAdd(2).withSchemaOptions(new SchemaGenerationOptions(false, true)));
									if(propertySchema.length() > 0) {
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
						.collect(context.joining("\n"));
					
					StringBuilder properties = Stream.of(publicFieldsProperties, accessorsProperties).filter(sb -> sb.length() > 0).collect(context.joining("\n"));
					if(properties.length() > 0) {
						result.append("\n").append(context.indent(0)).append("properties: ").append("\n");
						result.append(properties);
					}
				}
			}
			return result;
		}

		@Override
		public StringBuilder visitError(ErrorType type, WebRouterConfigurerOpenApiGenerationContext context) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitTypeVariable(TypeVariable type, WebRouterConfigurerOpenApiGenerationContext context) {
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
		public StringBuilder visitWildcard(WildcardType type, WebRouterConfigurerOpenApiGenerationContext context) {
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
		public StringBuilder visitExecutable(ExecutableType t, WebRouterConfigurerOpenApiGenerationContext p) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitNoType(NoType t, WebRouterConfigurerOpenApiGenerationContext p) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitUnknown(TypeMirror t, WebRouterConfigurerOpenApiGenerationContext p) {
			return new StringBuilder();
		}

		@Override
		public StringBuilder visitUnion(UnionType type, WebRouterConfigurerOpenApiGenerationContext context) {
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
		public StringBuilder visitIntersection(IntersectionType type, WebRouterConfigurerOpenApiGenerationContext context) {
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

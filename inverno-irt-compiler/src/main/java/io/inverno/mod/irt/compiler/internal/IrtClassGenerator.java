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
package io.inverno.mod.irt.compiler.internal;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CaseUtils;
import org.apache.commons.text.StringEscapeUtils;

import io.inverno.mod.irt.ByteBufTemplateSet;
import io.inverno.mod.irt.Pipe;
import io.inverno.mod.irt.TemplateSet;
import io.inverno.mod.irt.compiler.internal.IrtClassGenerationContext.ExtendedTemplateMode;
import io.inverno.mod.irt.compiler.spi.ApplyInfo;
import io.inverno.mod.irt.compiler.spi.ApplyInfo.ArgumentInfo;
import io.inverno.mod.irt.compiler.spi.ApplyInfo.TargetInfo;
import io.inverno.mod.irt.compiler.spi.ApplyInfo.TargetParameterInfo;
import io.inverno.mod.irt.compiler.spi.CommentInfo;
import io.inverno.mod.irt.compiler.spi.IfInfo;
import io.inverno.mod.irt.compiler.spi.IfInfo.CaseInfo;
import io.inverno.mod.irt.compiler.spi.ImportInfo;
import io.inverno.mod.irt.compiler.spi.IncludeInfo;
import io.inverno.mod.irt.compiler.spi.NameInfo;
import io.inverno.mod.irt.compiler.spi.OptionInfo;
import io.inverno.mod.irt.compiler.spi.PackageInfo;
import io.inverno.mod.irt.compiler.spi.ParameterInfo;
import io.inverno.mod.irt.compiler.spi.PipeInfo;
import io.inverno.mod.irt.compiler.spi.StatementInfo;
import io.inverno.mod.irt.compiler.spi.StaticContentInfo;
import io.inverno.mod.irt.compiler.spi.TemplateInfo;
import io.inverno.mod.irt.compiler.spi.TemplateInfo.SelectInfo;
import io.inverno.mod.irt.compiler.spi.TemplateSetInfo;
import io.inverno.mod.irt.compiler.spi.TemplateSetInfoVisitor;
import io.inverno.mod.irt.compiler.spi.ValueInfo;
import io.netty.buffer.ByteBuf;

/**
 * <p>
 * A {@link TemplateSetInfoVisitor} implementation used to generate a template
 * set class from a {@link TemplateSetInfo} extracted from an IRT source file.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 * 
 */
public class IrtClassGenerator implements TemplateSetInfoVisitor<StringBuilder, IrtClassGenerationContext> {

	@Override
	public StringBuilder visit(TemplateSetInfo info, IrtClassGenerationContext context) {
		if(context.getMode() == IrtClassGenerationContext.GenerationMode.IRT_CLASS) {
			StringBuilder irtClass = new StringBuilder();
			
			// package
			info.getPackage().ifPresent(packageInfo -> irtClass.append(this.visit(packageInfo, context).append(System.lineSeparator()).append(System.lineSeparator())));
	
			// imports
			if(info.getImports().length > 0) {
				irtClass.append(Arrays.stream(info.getImports()).map(importInfo -> this.visit(importInfo, context)).collect(context.joining(System.lineSeparator()))).append(System.lineSeparator()).append(System.lineSeparator());
			}
			
			// includes
			if(info.getIncludes().length > 0) {
				irtClass.append(Arrays.stream(info.getIncludes()).map(includeInfo -> this.visit(includeInfo, context)).collect(context.joining(System.lineSeparator()))).append(System.lineSeparator()).append(System.lineSeparator());
			}
			
			irtClass.append("public class ").append(context.getTemplateName()).append(" {").append(System.lineSeparator()).append(System.lineSeparator());

			irtClass.append(context.indent(1)).append("private static final ").append(Charset.class.getCanonicalName()).append(" CHARSET = ").append(Charset.class.getCanonicalName()).append(".forName(\"").append(context.getOptions().getCharset().name()).append("\");").append(System.lineSeparator()).append(System.lineSeparator());
			
			// TemplateSet interface
			StringBuilder templateSetInterfaces = this.visit(info, context.withIndentDepth(1).withMode(IrtClassGenerationContext.GenerationMode.TEMPLATE_SET_INTERFACES));
			
			// Build static contents
			List<StringBuilder> staticContentsList = new LinkedList<>();
			StringBuilder defaultStaticContents = new StringBuilder();
			defaultStaticContents.append(context.indent(1)).append("private static final String[] STATIC_").append(ExtendedTemplateMode.StaticContentType.STRING).append(" = {").append(System.lineSeparator()).append(context.indent(2));
			defaultStaticContents.append(context.getStaticContents().stream().map(content -> new StringBuilder("\"").append(StringEscapeUtils.escapeJava(content)).append("\"")).collect(context.joining(", " + System.lineSeparator() + context.indent(2)))).append(System.lineSeparator());
			defaultStaticContents.append(context.indent(1)).append("};");
			staticContentsList.add(defaultStaticContents);
			Arrays.stream(context.getOptions().getModes()).map(ExtendedTemplateMode::getStaticContentType).distinct().filter(type -> !type.equals(ExtendedTemplateMode.StaticContentType.STRING)).forEach(type -> {
				if(type.equals(ExtendedTemplateMode.StaticContentType.BYTEBUF)) {
					StringBuilder byteBufStaticContents = new StringBuilder();
					byteBufStaticContents.append(context.indent(1)).append("private static final ").append(ByteBuf.class.getCanonicalName()).append("[] STATIC_").append(type).append(" = {").append(System.lineSeparator());

					for(int i=0;i<context.getStaticContents().size();i++) {
						byteBufStaticContents.append(context.indent(2)).append(ByteBufTemplateSet.class.getCanonicalName()).append(".directByteBuf(STATIC_").append(ExtendedTemplateMode.StaticContentType.STRING).append("[").append(i).append("], ").append(context.getTemplateName()).append(".CHARSET)");
						if(i<context.getStaticContents().size() - 1) {
							byteBufStaticContents.append(",");
						}
						byteBufStaticContents.append(System.lineSeparator());
					}
					byteBufStaticContents.append(context.indent(1)).append("};");
					staticContentsList.add(byteBufStaticContents);
				}
				else if(type.equals(ExtendedTemplateMode.StaticContentType.BYTES)) {
					StringBuilder bytesStaticContents = new StringBuilder();
					bytesStaticContents.append(context.indent(1)).append("private static final byte[][] STATIC_").append(type).append(" = {").append(System.lineSeparator());

					for(int i=0;i<context.getStaticContents().size();i++) {
						bytesStaticContents.append(context.indent(2)).append("STATIC_").append(ExtendedTemplateMode.StaticContentType.STRING).append("[").append(i).append("].getBytes(").append(context.getTemplateName()).append(".CHARSET)");
						if(i<context.getStaticContents().size() - 1) {
							bytesStaticContents.append(",");
						}
						bytesStaticContents.append(System.lineSeparator());
					}
					bytesStaticContents.append(context.indent(1)).append("};");
					staticContentsList.add(bytesStaticContents);
				}
			});
			
			// TemplateSet implementations (1 per template mode)
			StringBuilder templateSetClasses = this.visit(info, context.withIndentDepth(1).withMode(IrtClassGenerationContext.GenerationMode.TEMPLATE_SET_CLASSES));
			
			// Renderer interface
			StringBuilder rendererInterface = this.visit(info, context.withIndentDepth(1).withMode(IrtClassGenerationContext.GenerationMode.RENDERER_INTERFACE));
			
			// Renderer methods
			StringBuilder rendererMethods = this.visit(info, context.withMode(IrtClassGenerationContext.GenerationMode.IRT_RENDERER_METHODS));
			
			irtClass.append(staticContentsList.stream().collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()))).append(System.lineSeparator()).append(System.lineSeparator());
			irtClass.append(templateSetInterfaces).append(System.lineSeparator()).append(System.lineSeparator());
			irtClass.append(templateSetClasses).append(System.lineSeparator()).append(System.lineSeparator());
			irtClass.append(rendererInterface).append(System.lineSeparator()).append(System.lineSeparator());
			irtClass.append(rendererMethods).append(System.lineSeparator());
			
			irtClass.append("}").append(System.lineSeparator());
			
			return irtClass;
		}
		else if(context.getMode() == IrtClassGenerationContext.GenerationMode.TEMPLATE_SET_INTERFACES) {
			StringBuilder templateSetInterfaces = Arrays.stream(context.getOptions().getModes())
				.map(ExtendedTemplateMode::getStaticContentType)
				.distinct()
				.map(staticContentType -> {
					final IrtClassGenerationContext contextWithStaticContentType = context.withStaticContentType(staticContentType);
					
					StringBuilder templateSetInterface = new StringBuilder();
					
					templateSetInterface.append(context.indent(0)).append("public static interface ").append(StringUtils.capitalize(staticContentType.toString().toLowerCase())).append("TemplateSet extends ");
					if(staticContentType == ExtendedTemplateMode.StaticContentType.BYTEBUF) {
						templateSetInterface.append(ByteBufTemplateSet.class.getCanonicalName());
					}
					else {
						templateSetInterface.append(TemplateSet.class.getCanonicalName());
					}
					if(info.getIncludes().length > 0) {
						templateSetInterface.append(", ");
						templateSetInterface.append(Arrays.stream(info.getIncludes()).map(includeInfo -> this.visit(includeInfo, contextWithStaticContentType)).collect(context.joining(", ")));
					}
					templateSetInterface.append(" {").append(System.lineSeparator()).append(System.lineSeparator());
					templateSetInterface.append(Arrays.stream(info.getTemplates()).map(templateInfo -> this.visit(templateInfo, contextWithStaticContentType.withIndentDepthAdd(1))).collect(context.joining(System.lineSeparator() + System.lineSeparator()))).append(System.lineSeparator());
					templateSetInterface.append(context.indent(0)).append("}");
					
					return templateSetInterface;
				})
				.collect(context.joining(System.lineSeparator() + System.lineSeparator()));
			
			return templateSetInterfaces;
		}
		else if(context.getMode() == IrtClassGenerationContext.GenerationMode.TEMPLATE_SET_CLASSES) {
			StringBuilder templateSetClasses = Arrays.stream(context.getOptions().getModes())
				.map(mode -> {
					StringBuilder templateSetModeClass = new StringBuilder();
					if(mode == ExtendedTemplateMode.STREAM) {
						templateSetModeClass.append(context.indent(0)).append("private static class ");
						templateSetModeClass.append(CaseUtils.toCamelCase(mode.toString().toLowerCase(), true, '_')).append("TemplateSetImpl").append("<T extends ").append(OutputStream.class.getCanonicalName()).append(">");
						templateSetModeClass.append(" extends ").append(mode.getTemplateSetClass().getCanonicalName()).append("<T>");
						templateSetModeClass.append(" implements ").append(context.getTemplateName()).append(".").append(StringUtils.capitalize(mode.getStaticContentType().toString().toLowerCase())).append("TemplateSet {").append(System.lineSeparator()).append(System.lineSeparator());
						
						templateSetModeClass.append(context.indent(1)).append("private ").append(CaseUtils.toCamelCase(mode.toString().toLowerCase(), true, '_')).append("TemplateSetImpl").append("(").append("T output").append(") {").append(System.lineSeparator());
						templateSetModeClass.append(context.indent(2)).append("super(").append(context.getTemplateName()).append(".CHARSET, output);").append(System.lineSeparator());
						templateSetModeClass.append(context.indent(1)).append("}").append(System.lineSeparator());
						
						templateSetModeClass.append(context.indent(0)).append("}");
					}
					else if(mode == ExtendedTemplateMode.BYTEBUF) {
						templateSetModeClass.append(context.indent(0)).append("private static class ").append(CaseUtils.toCamelCase(mode.toString().toLowerCase(), true, '_')).append("TemplateSetImpl");
						templateSetModeClass.append(" extends ").append(mode.getTemplateSetClass().getCanonicalName());
						templateSetModeClass.append(" implements ").append(context.getTemplateName()).append(".").append(StringUtils.capitalize(mode.getStaticContentType().toString().toLowerCase())).append("TemplateSet {").append(System.lineSeparator()).append(System.lineSeparator());
						
						templateSetModeClass.append(context.indent(1)).append("private ").append(CaseUtils.toCamelCase(mode.toString().toLowerCase(), true, '_')).append("TemplateSetImpl").append("(").append(ByteBuf.class.getCanonicalName()).append(" buffer) {").append(System.lineSeparator());
						templateSetModeClass.append(context.indent(2)).append("super(").append(context.getTemplateName()).append(".CHARSET, buffer);").append(System.lineSeparator());
						templateSetModeClass.append(context.indent(1)).append("}").append(System.lineSeparator());
						
						templateSetModeClass.append(context.indent(0)).append("}");
					}
					else {
						templateSetModeClass.append(context.indent(0)).append("private static class ").append(CaseUtils.toCamelCase(mode.toString().toLowerCase(), true, '_')).append("TemplateSetImpl");
						templateSetModeClass.append(" extends ").append(mode.getTemplateSetClass().getCanonicalName());
						templateSetModeClass.append(" implements ").append(context.getTemplateName()).append(".").append(StringUtils.capitalize(mode.getStaticContentType().toString().toLowerCase())).append("TemplateSet {").append(System.lineSeparator()).append(System.lineSeparator());
						
						templateSetModeClass.append(context.indent(1)).append("private ").append(CaseUtils.toCamelCase(mode.toString().toLowerCase(), true, '_')).append("TemplateSetImpl").append("() {").append(System.lineSeparator());
						templateSetModeClass.append(context.indent(2)).append("super(").append(context.getTemplateName()).append(".CHARSET);").append(System.lineSeparator());
						templateSetModeClass.append(context.indent(1)).append("}").append(System.lineSeparator());
						
						templateSetModeClass.append(context.indent(0)).append("}");
					}
					return templateSetModeClass;
				})
				.collect(context.joining(System.lineSeparator() + System.lineSeparator()));
			
			return templateSetClasses;
		}
		else if(context.getMode() == IrtClassGenerationContext.GenerationMode.RENDERER_INTERFACE) {
			StringBuilder templateSetInterface = new StringBuilder();
			
			templateSetInterface.append(context.indent(0)).append("public static interface Renderer<T> {").append(System.lineSeparator()).append(System.lineSeparator());
			templateSetInterface.append(Arrays.stream(info.getTemplates()).map(templateInfo -> this.visit(templateInfo, context.withIndentDepthAdd(1))).collect(context.joining(System.lineSeparator() + System.lineSeparator()))).append(System.lineSeparator());
			templateSetInterface.append(context.indent(0)).append("}");
			
			return templateSetInterface;
		}
		else if(context.getMode() == IrtClassGenerationContext.GenerationMode.IRT_RENDERER_METHODS) {
			StringBuilder rendererMethods = Arrays.stream(context.getOptions().getModes()).map(mode -> {
				final IrtClassGenerationContext contextWithTemplateMode = context.withTemplateMode(mode);
				
				StringBuilder rendererMethod = new StringBuilder();
				
				if(mode == ExtendedTemplateMode.STREAM) {
					rendererMethod.append(context.indent(1)).append("public static <T extends ").append(OutputStream.class.getCanonicalName()).append("> Renderer<").append(mode.getOutputType()).append("> ").append(mode.toMethodName()).append("(").append(Supplier.class.getCanonicalName()).append("<T> outputFactory) {").append(System.lineSeparator());
					
					rendererMethod.append(context.indent(2)).append("return new Renderer<").append(mode.getOutputType()).append(">() {").append(System.lineSeparator()).append(System.lineSeparator());
					rendererMethod.append(context.indent(3)).append("private ").append(Supplier.class.getCanonicalName()).append("<T> outputs = outputFactory;").append(System.lineSeparator()).append(System.lineSeparator());
					rendererMethod.append(Arrays.stream(info.getTemplates()).map(templateInfo -> this.visit(templateInfo, contextWithTemplateMode.withIndentDepthAdd(3).withMode(IrtClassGenerationContext.GenerationMode.RENDER_METHOD))).collect(context.joining(System.lineSeparator() + System.lineSeparator()))).append(System.lineSeparator());
					rendererMethod.append(context.indent(2)).append("};").append(System.lineSeparator());
					
					rendererMethod.append(context.indent(1)).append("}");
				}
				else if(mode == ExtendedTemplateMode.BYTEBUF) {
					
					rendererMethod.append(context.indent(1)).append("public static Renderer<").append(mode.getOutputType()).append("> ").append(mode.toMethodName()).append("(").append(Supplier.class.getCanonicalName()).append("<").append(ByteBuf.class.getCanonicalName()).append("> bufferFactory) {").append(System.lineSeparator());

					rendererMethod.append(context.indent(2)).append("return new Renderer<").append(mode.getOutputType()).append(">() {").append(System.lineSeparator()).append(System.lineSeparator());
					rendererMethod.append(context.indent(3)).append("private ").append(Supplier.class.getCanonicalName()).append("<").append(ByteBuf.class.getCanonicalName()).append("> buffers = bufferFactory;").append(System.lineSeparator()).append(System.lineSeparator());
					rendererMethod.append(Arrays.stream(info.getTemplates()).map(templateInfo -> this.visit(templateInfo, contextWithTemplateMode.withIndentDepthAdd(3).withMode(IrtClassGenerationContext.GenerationMode.RENDER_METHOD))).collect(context.joining(System.lineSeparator() + System.lineSeparator()))).append(System.lineSeparator());
					rendererMethod.append(context.indent(2)).append("};").append(System.lineSeparator());
					
					rendererMethod.append(context.indent(1)).append("}");
				}
				else {
					rendererMethod.append(context.indent(1)).append("public static Renderer<").append(mode.getOutputType()).append("> ").append(mode.toMethodName()).append("() {").append(System.lineSeparator());

					rendererMethod.append(context.indent(2)).append("return new Renderer<").append(mode.getOutputType()).append(">() {").append(System.lineSeparator()).append(System.lineSeparator());
					rendererMethod.append(Arrays.stream(info.getTemplates()).map(templateInfo -> this.visit(templateInfo, contextWithTemplateMode.withIndentDepthAdd(3).withMode(IrtClassGenerationContext.GenerationMode.RENDER_METHOD))).collect(context.joining(System.lineSeparator() + System.lineSeparator()))).append(System.lineSeparator());
					rendererMethod.append(context.indent(2)).append("};").append(System.lineSeparator());
					
					rendererMethod.append(context.indent(1)).append("}");
				}
				
				return rendererMethod;
			}).collect(context.joining(System.lineSeparator() + System.lineSeparator()));
			
			return rendererMethods;
		}
		return new StringBuilder();
	}
	
	@Override
	public StringBuilder visit(PackageInfo info, IrtClassGenerationContext context) {
		if(context.getMode() == IrtClassGenerationContext.GenerationMode.IRT_CLASS) {
			return new StringBuilder().append("package ").append(this.visit(info.getName(), context)).append(";").append(this.visit((LocatableInfo)info, context));
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(ImportInfo info, IrtClassGenerationContext context) {
		if(context.getMode() == IrtClassGenerationContext.GenerationMode.IRT_CLASS) {
			StringBuilder result = new StringBuilder();
			
			result.append("import ");
			if(info.isStatic()) {
				result.append("static ");
			}
			result.append(this.visit(info.getName(), context)).append(";").append(this.visit((LocatableInfo)info, context));
	
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(IncludeInfo info, IrtClassGenerationContext context) {
		if(context.getMode() == IrtClassGenerationContext.GenerationMode.IRT_CLASS) {
			return new StringBuilder().append("import ").append(this.visit(info.getName(), context)).append(";").append(this.visit((LocatableInfo)info, context));
		}
		else if(context.getMode() == IrtClassGenerationContext.GenerationMode.TEMPLATE_SET_INTERFACES) {
			return new StringBuilder(info.getName().getParts()[info.getName().getParts().length - 1]).append(".").append(StringUtils.capitalize(context.getStaticContentType().toString().toLowerCase())).append("TemplateSet");
		}
		return new StringBuilder();
	}
	
	@Override
	public StringBuilder visit(OptionInfo info, IrtClassGenerationContext context) {
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(TemplateInfo info, IrtClassGenerationContext context) {
		if(context.getMode() == IrtClassGenerationContext.GenerationMode.TEMPLATE_SET_INTERFACES) {
			final IrtClassGenerationContext contextWithTemplate = context.withTemplateInfo(info);
			
			StringBuilder templateMethod = new StringBuilder();
			
			templateMethod.append(context.indent(0)).append("default ").append(CompletableFuture.class.getCanonicalName()).append("<Void> template");
			info.getName().ifPresent(templateName -> templateMethod.append("_").append(templateName));
			templateMethod.append("(").append(info.getParameters().values().stream().map(formalParameterInfo -> this.visit(formalParameterInfo, contextWithTemplate)).collect(context.joining(", ")));
			templateMethod.append(") {").append(this.visit((LocatableInfo)info, context)).append(System.lineSeparator());

			if(info.getSelect().isPresent()) {
				templateMethod.append(this.visit(info.getSelect().get(), contextWithTemplate.withIndentDepthAdd(1)));
				templateMethod.append("(").append(info.getParameters().values().stream().map(formalParameterInfo -> formalParameterInfo.getName()).collect(Collectors.joining(", "))).append(");").append(System.lineSeparator());
			}
			else if(info.getStatements().isPresent()) {
				StatementInfo[] statements = info.getStatements().get();
				
				templateMethod.append(context.indent(1)).append("return ");
				
				templateMethod.append(TemplateSet.class.getCanonicalName()).append(".COMPLETED_FUTURE").append(System.lineSeparator());
				templateMethod.append(Arrays.asList(statements).stream().filter(statementInfo -> !(statementInfo instanceof CommentInfo)).map(statementInfo -> new StringBuilder().append(context.indent(2)).append(".thenCompose(_").append(info.hashCode()).append(" -> ").append(this.visit(statementInfo, contextWithTemplate.withIndentDepthAdd(2))).append(")").append(this.visit((LocatableInfo)statementInfo, context))).collect(context.joining(System.lineSeparator())));
				templateMethod.append(System.lineSeparator()).append(context.indent(2)).append(";").append(System.lineSeparator());
			}
			else {
				templateMethod.append(context.indent(1)).append("return ").append(TemplateSet.class.getCanonicalName()).append(".COMPLETED_FUTURE;").append(System.lineSeparator());
			}
			templateMethod.append(context.indent(0)).append("}");
			
			return templateMethod;
		}
		else if(context.getMode() == IrtClassGenerationContext.GenerationMode.RENDERER_INTERFACE) {
			final IrtClassGenerationContext contextWithTemplate = context.withTemplateInfo(info);
			
			StringBuilder renderMethodDeclaration = new StringBuilder();
			
			renderMethodDeclaration.append(context.indent(0)).append("T render");
			info.getName().ifPresent(templateName -> renderMethodDeclaration.append("_").append(templateName));
			renderMethodDeclaration.append("(").append(info.getParameters().values().stream().map(formalParameterInfo -> this.visit(formalParameterInfo, contextWithTemplate)).collect(context.joining(", ")));
			renderMethodDeclaration.append(");").append(this.visit((LocatableInfo)info, context));
			
			return renderMethodDeclaration;
		}
		else if(context.getMode() == IrtClassGenerationContext.GenerationMode.RENDER_METHOD) {
			final IrtClassGenerationContext contextWithTemplate = context.withTemplateInfo(info);
			
			StringBuilder renderMethod = new StringBuilder();
			
			if(context.getTemplateMode() == ExtendedTemplateMode.STREAM) {
				renderMethod.append(context.indent(0)).append("@Override").append(System.lineSeparator());
				renderMethod.append(context.indent(0)).append("public ").append(context.getTemplateMode().getOutputType()).append(" render");
				info.getName().ifPresent(templateName -> renderMethod.append("_").append(templateName));
				renderMethod.append("(").append(info.getParameters().values().stream().map(formalParameterInfo -> this.visit(formalParameterInfo, contextWithTemplate)).collect(context.joining(", "))).append(") {").append(this.visit((LocatableInfo)info, context)).append(System.lineSeparator());
				
				renderMethod.append(context.indent(1)).append(CaseUtils.toCamelCase(context.getTemplateMode().toString().toLowerCase(), true, '_')).append("TemplateSetImpl<T> tpl = new ").append(CaseUtils.toCamelCase(context.getTemplateMode().toString().toLowerCase(), true, '_')).append("TemplateSetImpl<>(this.outputs.get());").append(System.lineSeparator());
				
				renderMethod.append(context.indent(1)).append("return tpl.template");
				info.getName().ifPresent(templateName -> renderMethod.append("_").append(templateName));
				renderMethod.append("(").append(info.getParameters().keySet().stream().collect(Collectors.joining(", "))).append(").thenApply(_").append(info.hashCode()).append(" -> tpl.getOutput());").append(System.lineSeparator());
				renderMethod.append(context.indent(0)).append("}");
			}
			else if(context.getTemplateMode() == ExtendedTemplateMode.BYTEBUF) {
				renderMethod.append(context.indent(0)).append("@Override").append(System.lineSeparator());
				renderMethod.append(context.indent(0)).append("public ").append(context.getTemplateMode().getOutputType()).append(" render");
				info.getName().ifPresent(templateName -> renderMethod.append("_").append(templateName));
				renderMethod.append("(").append(info.getParameters().values().stream().map(formalParameterInfo -> this.visit(formalParameterInfo, contextWithTemplate)).collect(context.joining(", "))).append(") {").append(this.visit((LocatableInfo)info, context)).append(System.lineSeparator());
				
				renderMethod.append(context.indent(1)).append(CaseUtils.toCamelCase(context.getTemplateMode().toString().toLowerCase(), true, '_')).append("TemplateSetImpl tpl = new ").append(CaseUtils.toCamelCase(context.getTemplateMode().toString().toLowerCase(), true, '_')).append("TemplateSetImpl(this.buffers.get());").append(System.lineSeparator());
				
				renderMethod.append(context.indent(1)).append("return tpl.template");
				info.getName().ifPresent(templateName -> renderMethod.append("_").append(templateName));
				renderMethod.append("(").append(info.getParameters().keySet().stream().collect(Collectors.joining(", "))).append(").thenApply(_").append(info.hashCode()).append(" -> tpl.getOutput());").append(System.lineSeparator());
				renderMethod.append(context.indent(0)).append("}");
			}
			else {
				renderMethod.append(context.indent(0)).append("@Override").append(System.lineSeparator());
				renderMethod.append(context.indent(0)).append("public ").append(context.getTemplateMode().getOutputType()).append(" render");
				info.getName().ifPresent(templateName -> renderMethod.append("_").append(templateName));
				renderMethod.append("(").append(info.getParameters().values().stream().map(formalParameterInfo -> this.visit(formalParameterInfo, contextWithTemplate)).collect(context.joining(", "))).append(") {").append(this.visit((LocatableInfo)info, context)).append(System.lineSeparator());
				
				renderMethod.append(context.indent(1)).append(CaseUtils.toCamelCase(context.getTemplateMode().toString().toLowerCase(), true, '_')).append("TemplateSetImpl tpl = new ").append(CaseUtils.toCamelCase(context.getTemplateMode().toString().toLowerCase(), true, '_')).append("TemplateSetImpl();").append(System.lineSeparator());
				if(context.getTemplateMode() == ExtendedTemplateMode.PUBLISHER_STRING || context.getTemplateMode() == ExtendedTemplateMode.PUBLISHER_BYTEBUF) {
					renderMethod.append(context.indent(1)).append("return tpl.getSink().asFlux().doOnSubscribe(_").append(info.hashCode()).append(" -> tpl.template");
					info.getName().ifPresent(templateName -> renderMethod.append("_").append(templateName));
					renderMethod.append("(").append(info.getParameters().keySet().stream().collect(Collectors.joining(", "))).append(").thenRun(tpl.getSink()::tryEmitComplete));").append(System.lineSeparator());
				}
				else {
					renderMethod.append(context.indent(1)).append("return tpl.template");
					info.getName().ifPresent(templateName -> renderMethod.append("_").append(templateName));
					renderMethod.append("(").append(info.getParameters().keySet().stream().collect(Collectors.joining(", "))).append(").thenApply(_").append(info.hashCode()).append(" -> tpl.getOutput());").append(System.lineSeparator());
				}
				renderMethod.append(context.indent(0)).append("}");
			}
			
			return renderMethod;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(SelectInfo info, IrtClassGenerationContext context) {
		if(context.getMode() == IrtClassGenerationContext.GenerationMode.TEMPLATE_SET_INTERFACES) {
			StringBuilder result = new StringBuilder();
			result.append(context.indent(0)).append("return ").append(info.getTemplateSetName().map(nameInfo -> this.visit(nameInfo, context).append(".").append(StringUtils.capitalize(context.getStaticContentType().toString().toLowerCase())).append("TemplateSet").append(".super")).orElse(new StringBuilder("this"))).append(".template");
			info.getTemplateName().ifPresent(templateName -> result.append("_").append(templateName));
			result.append("(").append(context.getTemplateInfo().getParameters().keySet().stream().collect(Collectors.joining(", "))).append(");").append(this.visit((LocatableInfo)info, context));
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(StatementInfo info, IrtClassGenerationContext context) {
		if(info instanceof CommentInfo) {
			return this.visit((CommentInfo)info, context);
		}
		else if(info instanceof StaticContentInfo) {
			return this.visit((StaticContentInfo)info, context);
		}
		else if(info instanceof IfInfo) {
			return this.visit((IfInfo)info, context);
		}
		else if(info instanceof ApplyInfo) {
			return this.visit((ApplyInfo)info, context);
		}
		else if(info instanceof ValueInfo) {
			return this.visit((ValueInfo)info, context);
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(CommentInfo info, IrtClassGenerationContext context) {
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(StaticContentInfo info, IrtClassGenerationContext context) {
		if(context.getMode() == IrtClassGenerationContext.GenerationMode.TEMPLATE_SET_INTERFACES) {
			StringBuilder result = new StringBuilder();
			result.append("this.render(STATIC_").append(context.getStaticContentType()).append("[").append(context.getIndexOfStaticContent(info.getContent())).append("])");
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(ValueInfo info, IrtClassGenerationContext context) {
		if(context.getMode() == IrtClassGenerationContext.GenerationMode.TEMPLATE_SET_INTERFACES) {
			StringBuilder result = new StringBuilder();
			result.append("this.render(");
			result.append(this.visit(info, context.withMode(IrtClassGenerationContext.GenerationMode.VALUE)));
			result.append(")");
			
			return result;
		}
		else if(context.getMode() == IrtClassGenerationContext.GenerationMode.VALUE) {
			StringBuilder result = new StringBuilder();
			
			StringBuilder valueExpression;
			if(info.getExpression().isPresent()) {
				valueExpression = new StringBuilder(info.getExpression().get());
			}
			else {
				valueExpression = this.visit(info.getName().get(), context.withMode(IrtClassGenerationContext.GenerationMode.NAME_EXPRESSION));
			}
			
			PipeInfo[] pipes = info.getPipes();
			if(pipes.length > 0) {
				result.append(Pipe.class.getCanonicalName()).append(".apply(").append(valueExpression).append(", ");
				result.append(this.visit(pipes[0], context));
				if(pipes.length > 1) {
					result.append(".and(");
					result.append(Arrays.asList(pipes).subList(1, pipes.length).stream().map(pipeInfo -> this.visit(pipeInfo, context)).collect(context.joining(").and("))).append(")");
				}
				result.append(")");
			}
			else {
				result.append(valueExpression);
			}
			
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(IfInfo info, IrtClassGenerationContext context) {
		if(context.getMode() == IrtClassGenerationContext.GenerationMode.TEMPLATE_SET_INTERFACES) {
			StringBuilder result = new StringBuilder();
			
			result.append("{").append(System.lineSeparator());
			result.append(context.indent(1)).append(Arrays.stream(info.getCases()).map(caseInfo -> this.visit(caseInfo, context.withIndentDepthAdd(1))).collect(context.joining(System.lineSeparator() + context.indent(1) + "else "))).append(System.lineSeparator());
			result.append(context.indent(0)).append("}");
			
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(CaseInfo info, IrtClassGenerationContext context) {
		if(context.getMode() == IrtClassGenerationContext.GenerationMode.TEMPLATE_SET_INTERFACES) {
			StringBuilder result = new StringBuilder();

			if(info.getGuardExpression().isPresent()) {
				result.append("if(").append(info.getGuardExpression().get()).append(") ");
			}
			result.append("{").append(System.lineSeparator());
			
			if(info.getStatements().length > 0) {
				StatementInfo[] statements = info.getStatements();
				
				result.append(context.indent(1)).append("return ");
				
				result.append(TemplateSet.class.getCanonicalName()).append(".COMPLETED_FUTURE").append(System.lineSeparator());
				result.append(Arrays.asList(statements).stream().filter(statementInfo -> !(statementInfo instanceof CommentInfo)).map(statementInfo -> new StringBuilder().append(context.indent(2)).append(".thenCompose(_").append(info.hashCode()).append(" -> ").append(this.visit(statementInfo, context.withIndentDepthAdd(2))).append(")").append(this.visit((LocatableInfo)statementInfo, context))).collect(context.joining(System.lineSeparator())));
				result.append(System.lineSeparator()).append(context.indent(2)).append(";").append(System.lineSeparator());
			}
			else {
				result.append(context.indent(1)).append("return ").append(CompletableFuture.class.getCanonicalName()).append(".completedFuture(null);").append(System.lineSeparator());
			}
			result.append(context.indent(0)).append("}");
			
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(ApplyInfo info, IrtClassGenerationContext context) {
		if(context.getMode() == IrtClassGenerationContext.GenerationMode.TEMPLATE_SET_INTERFACES) {
			final IrtClassGenerationContext contextWithApply = context.withApplyInfo(info);
			
			StringBuilder result = new StringBuilder();
			
			if(info.getValue().isPresent()) {
				result.append("this.applyTemplate(");
				result.append(this.visit(info.getValue().get(), context.withMode(IrtClassGenerationContext.GenerationMode.VALUE)));
				result.append(").render(");
				if(info.getTargets().length == 0) {
					result.append("this::template");
				}
				else {
					if(info.getTargetParameters().length > 0) {
						result.append("(").append(Arrays.stream(info.getTargetParameters()).map(targetParameterInfo -> this.visit(targetParameterInfo, contextWithApply)).collect(context.joining(", "))).append(") -> ");
					}
					else {
						result.append("_").append(info.hashCode()).append(" -> ");
					}
					
					result.append("{").append(System.lineSeparator());
					result.append(context.indent(1)).append(Arrays.stream(info.getTargets()).map(targetInfo -> this.visit(targetInfo, contextWithApply)).collect(context.joining(System.lineSeparator() + context.indent(1) + "else "))).append(System.lineSeparator());
					if(Arrays.stream(info.getTargets()).allMatch(targetInfo -> targetInfo.getGuardExpression().isPresent())) {
						result.append(context.indent(1)).append("return ").append(TemplateSet.class.getCanonicalName()).append(".COMPLETED_FUTURE;").append(System.lineSeparator());
					}
					result.append(context.indent(0)).append("}");
				}
				result.append(")");
			}
			else {
				if(info.getTargets().length == 0) {
					result.append("this.template()");
				}
				else {
					// There must be no target parameters (this is also checked in the parser)
					if(info.getTargetParameters().length > 0) {
						throw new IrtCompilationException("Parameters can't be declared without a value", ((LocatableInfo)info.getTargetParameters()[0]).getRange());
					}
					result.append("{").append(System.lineSeparator());
					result.append(context.indent(1)).append(Arrays.stream(info.getTargets()).map(targetInfo -> this.visit(targetInfo, contextWithApply)).collect(context.joining(System.lineSeparator() + context.indent(1) + "else "))).append(System.lineSeparator());
					if(Arrays.stream(info.getTargets()).allMatch(targetInfo -> targetInfo.getGuardExpression().isPresent())) {
						result.append(context.indent(1)).append("return ").append(TemplateSet.class.getCanonicalName()).append(".COMPLETED_FUTURE;").append(System.lineSeparator());
					}
					result.append(context.indent(0)).append("}");
				}
			}
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(TargetParameterInfo info, IrtClassGenerationContext context) {
		if(context.getMode() == IrtClassGenerationContext.GenerationMode.TEMPLATE_SET_INTERFACES) {
			StringBuilder result = new StringBuilder();
			
			if(info.getName().isPresent()) {
				result.append(info.getName().get());
			}
			else {
				result.append(info.getParameter().get().getValue());
			}
			return result;
		}
		return new StringBuilder();
	}
	
	@Override
	public StringBuilder visit(TargetInfo info, IrtClassGenerationContext context) {
		if(context.getMode() == IrtClassGenerationContext.GenerationMode.TEMPLATE_SET_INTERFACES) {
			StringBuilder result = new StringBuilder();
			
			boolean hasGuards = context.getApplyInfo().getTargets().length > 1 || info.getGuardExpression().isPresent();
			
			if(hasGuards) {
				info.getGuardExpression().ifPresent(guardExpression -> result.append("if(").append(guardExpression).append(") "));
				result.append("{ ");
			}
			
			result.append("return this.template");
			info.getName().ifPresent(name -> result.append("_").append(name));
			result.append("(");
			info.getArguments().ifPresentOrElse(
				targetArgs -> result.append(Arrays.stream(targetArgs).map(targetArgInfo -> this.visit(targetArgInfo, context)).collect(context.joining(", "))),
				() -> {
					if(context.getApplyInfo().getValue().isPresent()) {
						result.append("_").append(context.getApplyInfo().hashCode());
					}
				}
			);
			result.append(");");
			
			if(hasGuards) {
				result.append(" }");
			}
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(ArgumentInfo info, IrtClassGenerationContext context) {
		if(context.getMode() == IrtClassGenerationContext.GenerationMode.TEMPLATE_SET_INTERFACES || context.getMode() == IrtClassGenerationContext.GenerationMode.VALUE) {
			StringBuilder result = new StringBuilder();
			result.append(info.getValue());
			return result;
		}
		return new StringBuilder();
	}
	
	@Override
	public StringBuilder visit(NameInfo info, IrtClassGenerationContext context) {
		if(context.getMode() == IrtClassGenerationContext.GenerationMode.NAME_EXPRESSION) {
			StringBuilder result = new StringBuilder();
			
			Map<String, ParameterInfo> formalParameters = context.getTemplateInfo().getParameters();
			
			String[] parts = info.getParts();

			String currentName = parts[0];
			if(!formalParameters.containsKey(parts[0])) {
				throw new IrtCompilationException(parts[0] + " cannot be resolved", ((LocatableInfo)info).getRange());
			}
			TypeMirror currentType = formalParameters.get(parts[0]).getType();
			result.append(currentName);
			for(int i=1;i<parts.length;i++) {
				result.append(".");
				
				if(currentType.getKind() == TypeKind.ARRAY) {
					if(parts[i].equals("length")) {
						result.append(parts[i]);
					}
					else {
						throw new IrtCompilationException(parts[i] + " cannot be resolved", ((LocatableInfo)info).getRange());
					}
				}
				else if(currentType.getKind() == TypeKind.DECLARED) {
					Element partAccessor = context.getAccessor(((DeclaredType)currentType), parts[i]);
					if(partAccessor != null) {
						result.append(partAccessor.getSimpleName().toString());
						if(partAccessor.getKind() == ElementKind.METHOD) {
							result.append("()");
							currentType = ((ExecutableType)context.getTypeUtils().asMemberOf((DeclaredType)currentType, partAccessor)).getReturnType();
						}
						else if(partAccessor.getKind() == ElementKind.FIELD) {
							currentType = ((ExecutableType)context.getTypeUtils().asMemberOf((DeclaredType)currentType, partAccessor));
						}
						else {
							throw new IllegalStateException("Unexpected element kind: " + partAccessor.getKind());
						}
					}
					else {
						throw new IrtCompilationException(parts[i] + " cannot be resolved", ((LocatableInfo)info).getRange());
					}
				}
				else {
					throw new IrtCompilationException(parts[i] + " cannot be resolved", ((LocatableInfo)info).getRange());
				}
			}
			return result;
		}
		else {
			StringBuilder result = new StringBuilder();
			result.append(String.join(".", info.getParts()));
			return result;
		}
	}

	@Override
	public StringBuilder visit(ParameterInfo info, IrtClassGenerationContext context) {
		if(context.getMode() == IrtClassGenerationContext.GenerationMode.TEMPLATE_SET_INTERFACES) {
			return new StringBuilder(info.getValue());
		}
		else if(context.getMode() == IrtClassGenerationContext.GenerationMode.RENDERER_INTERFACE) {
			return new StringBuilder(info.getValue());
		}
		else if(context.getMode() == IrtClassGenerationContext.GenerationMode.RENDER_METHOD) {
			return new StringBuilder(info.getValue());
		}
		return new StringBuilder();
	}
	
	@Override
	public StringBuilder visit(PipeInfo info, IrtClassGenerationContext context) {
		if(context.getMode() == IrtClassGenerationContext.GenerationMode.VALUE) {
			StringBuilder result = new StringBuilder();
			if(info.getName().isPresent()) {
				// In this case the name is necessarily of the form Type.field.field.method: this is regular java unlike name in the value context
				result.append(Arrays.stream(info.getName().get().getParts()).collect(Collectors.joining(".")));
				result.append("(");
				info.getArguments().ifPresent(pipeArgs -> result.append(Arrays.stream(pipeArgs).map(pipeArgInfo -> this.visit(pipeArgInfo, context)).collect(context.joining(", "))));
				result.append(")");
			}
			else {
				result.append(info.getExpression().get());
			}
			return result;
		}
		return new StringBuilder();
	}

	/**
	 * <p>
	 * Visits locatable info.
	 * </p>
	 * 
	 * @param info the info to visit
	 * @param p    a visitor parameter
	 * 
	 * @return a visitor result
	 */
	public StringBuilder visit(LocatableInfo info, IrtClassGenerationContext p) {
		return new StringBuilder().append(" // ").append(info.toString());
	}
}

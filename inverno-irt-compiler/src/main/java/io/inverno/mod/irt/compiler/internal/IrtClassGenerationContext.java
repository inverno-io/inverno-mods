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

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;

import io.inverno.mod.base.Charsets;
import io.inverno.mod.irt.AbstractByteBufPublisherTemplateSet;
import io.inverno.mod.irt.AbstractByteBufTemplateSet;
import io.inverno.mod.irt.AbstractCompositeByteBufTemplateSet;
import io.inverno.mod.irt.AbstractStreamTemplateSet;
import io.inverno.mod.irt.AbstractStringPublisherTemplateSet;
import io.inverno.mod.irt.AbstractStringTemplateSet;
import io.inverno.mod.irt.compiler.spi.ApplyInfo;
import io.inverno.mod.irt.compiler.spi.OptionInfo;
import io.inverno.mod.irt.compiler.spi.TemplateInfo;
import io.inverno.mod.irt.compiler.spi.TemplateMode;
import io.netty.buffer.ByteBuf;

/**
 * <p>
 * Represents a generation context used by the {@link IrtClassGenerator} during the generation of an Inverno Reactive Template class.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public class IrtClassGenerationContext {

	/**
	 * <p>
	 * Represents the generation mode.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 */
	public enum GenerationMode {
		IRT_CLASS,
		TEMPLATE_SET_INTERFACES,
		TEMPLATE_SET_CLASSES,
		RENDERER_INTERFACE,
		IRT_RENDERER_CLASSES,
		IRT_RENDERER_METHODS,
		RENDER_METHOD,
		NAME_EXPRESSION,
		VALUE
	}
	
	private final Types typeUtils;
	private final Elements elementUtils;
	private final String templateName;
	private final Options options;
	private final List<String> staticContents;
	
	private TemplateInfo templateInfo;
	private ApplyInfo applyInfo;
	private ExtendedTemplateMode templateMode;
	private ExtendedTemplateMode.StaticContentType staticContentType;
	
	/**
	 * The generation mode.
	 */
	private GenerationMode mode;
	
	/**
	 * The indent depth.
	 */
	protected int indentDepth = 0;
	
	/**
	 * The default indent.
	 */
	protected static final String DEFAULT_INDENT = "\t";
	
	/**
	 * The indent used during generation.
	 */
	protected String indent;
	
	/**
	 * <p>
	 * Creates an IRT generation context.
	 * </p>
	 * 
	 * @param typeUtils    the types utility
	 * @param elementUtils the elements utility
	 * @param templateName the name of the template set being generated
	 * @param options      the options specified in the template set
	 * @param mode         the generation mode
	 */
	public IrtClassGenerationContext(Types typeUtils, Elements elementUtils, String templateName, OptionInfo[] options, IrtClassGenerationContext.GenerationMode mode) {
		this(typeUtils, elementUtils, templateName, options, mode, DEFAULT_INDENT);
	}
	
	/**
	 * <p>
	 * Creates an IRT generation context with the specified indent.
	 * </p>
	 * 
	 * @param typeUtils    the types utility
	 * @param elementUtils the elements utility
	 * @param templateName the name of the template set being generated
	 * @param options      the options specified in the template set
	 * @param mode         the generation mode
	 * @param indent       the indent
	 */
	public IrtClassGenerationContext(Types typeUtils, Elements elementUtils, String templateName, OptionInfo[] options, IrtClassGenerationContext.GenerationMode mode, String indent) {
		this.typeUtils = typeUtils;
		this.elementUtils = elementUtils;
		this.templateName = templateName;
		this.options = new Options(options);
		this.mode = mode;
		this.setIndent(indent);
		this.staticContents = new ArrayList<>();
	}
	
	/**
	 * <p>
	 * Creates an IRT generation context from a parent generation context.
	 * </p>
	 * 
	 * @param parentGeneration the parent generation context
	 */
	private IrtClassGenerationContext(IrtClassGenerationContext parentGeneration) {
		this.typeUtils = parentGeneration.typeUtils;
		this.elementUtils = parentGeneration.elementUtils;
		this.templateName = parentGeneration.templateName;
		this.options = parentGeneration.options;
		this.staticContents = parentGeneration.staticContents;
		this.templateInfo = parentGeneration.templateInfo;
		this.applyInfo = parentGeneration.applyInfo;
		this.templateMode = parentGeneration.templateMode;
		this.staticContentType = parentGeneration.staticContentType;
		this.mode = parentGeneration.getMode();
		this.setIndent(parentGeneration.indent);
		this.indentDepth = parentGeneration.indentDepth;
	}
	
	/**
	 * <p>
	 * Sets the generation indent.
	 * </p>
	 * 
	 * @param indent an indent
	 */
	public void setIndent(String indent) {
		this.indent = indent;
	}
	
	/**
	 * <p>
	 * Returns an indent of the specified depth from the current generation indent depth.
	 * </p>
	 * 
	 * @param depth the relative indent depth
	 * 
	 * @return an indent
	 */
	public String indent(int depth) {
		return String.valueOf(this.indent).repeat(Math.max(0, this.indentDepth + depth));
	}
	
	/**
	 * <p>
	 * Returns a new generation context created from this context with the specified mode.
	 * </p>
	 * 
	 * <p>
	 * This generation context remains untouched.
	 * </p>
	 * 
	 * @param mode a generation mode
	 * 
	 * @return a new generation context
	 */
	public IrtClassGenerationContext withMode(IrtClassGenerationContext.GenerationMode mode) {
		IrtClassGenerationContext context = new IrtClassGenerationContext(this);
		context.mode = mode;
		return context;
	}
	
	/**
	 * <p>
	 * Returns a new generation context created from this context with an indent depth increased by the specified delta.
	 * </p>
	 * 
	 * <p>
	 * This generation context remains untouched.
	 * </p>
	 * 
	 * @param delta an indent depth delta
	 * 
	 * @return a new generation context
	 */
	public IrtClassGenerationContext withIndentDepthAdd(int delta) {
		return this.withIndentDepth(this.indentDepth + delta);
	}
	
	/**
	 * <p>
	 * Returns a new generation context created from this context with the specified indent depth.
	 * </p>
	 * 
	 * <p>
	 * This generation context remains untouched.
	 * </p>
	 * 
	 * @param indentDepth an indent depth
	 * 
	 * @return a new generation context
	 */
	public IrtClassGenerationContext withIndentDepth(int indentDepth) {
		IrtClassGenerationContext context = new IrtClassGenerationContext(this);
		context.indentDepth = indentDepth;
		return context;
	}
	
	/**
	 * <p>
	 * Returns a new generation context created from this context with the specified template info.
	 * </p>
	 * 
	 * <p>
	 * This generation context remains untouched.
	 * </p>
	 * 
	 * @param templateInfo a template info
	 * 
	 * @return a new generation context
	 */
	public IrtClassGenerationContext withTemplateInfo(TemplateInfo templateInfo) {
		IrtClassGenerationContext context = new IrtClassGenerationContext(this);
		context.templateInfo = templateInfo;
		return context;
	}
	
	/**
	 * <p>
	 * Returns a new generation context created from this context with the specified apply info.
	 * </p>
	 * 
	 * <p>
	 * This generation context remains untouched.
	 * </p>
	 * 
	 * @param applyInfo an apply info
	 * 
	 * @return a new generation context
	 */
	public IrtClassGenerationContext withApplyInfo(ApplyInfo applyInfo) {
		IrtClassGenerationContext context = new IrtClassGenerationContext(this);
		context.applyInfo = applyInfo;
		return context;
	}
	
	/**
	 * <p>
	 * Returns a new generation context created from this context with the specified template mode.
	 * </p>
	 * 
	 * <p>
	 * This generation context remains untouched.
	 * </p>
	 * 
	 * @param templateMode a template mode
	 * 
	 * @return a new generation context
	 */
	public IrtClassGenerationContext withTemplateMode(ExtendedTemplateMode templateMode) {
		IrtClassGenerationContext context = new IrtClassGenerationContext(this);
		context.templateMode = templateMode;
		context.staticContentType = templateMode.getStaticContentType();
		return context;
	}
	
	/**
	 * <p>
	 * Returns a new generation context created from this context with the specified static content type.
	 * </p>
	 * 
	 * <p>
	 * This generation context remains untouched.
	 * </p>
	 * 
	 * @param staticContentType a static content type
	 * 
	 * @return a new generation context
	 */
	public IrtClassGenerationContext withStaticContentType(ExtendedTemplateMode.StaticContentType staticContentType) {
		if(this.templateMode != null && this.templateMode.getStaticContentType() != staticContentType) {
			throw new IllegalArgumentException("Can't specify static content type " + staticContentType + " which is conflicting with template mode " + this.templateMode);
		}
		IrtClassGenerationContext context = new IrtClassGenerationContext(this);
		context.staticContentType = staticContentType;
		return context;
	}
	
	/**
	 * <p>
	 * Returns the types utility.
	 * </p>
	 * 
	 * @return the types utility
	 */
	public Types getTypeUtils() {
		return typeUtils;
	}
	
	/**
	 * <p>
	 * Returns the elements utility.
	 * </p>
	 * 
	 * @return the elements utility
	 */
	public Elements getElementUtils() {
		return elementUtils;
	}
	
	/**
	 * <p>
	 * Returns the template info.
	 * </p>
	 * 
	 * @return the template info
	 */
	public TemplateInfo getTemplateInfo() {
		return this.templateInfo;
	}
	
	/**
	 * <p>
	 * Returns the apply info.
	 * </p>
	 * 
	 * @return the apply info
	 */
	public ApplyInfo getApplyInfo() {
		return this.applyInfo;
	}
	
	/**
	 * <p>
	 * Returns the template mode.
	 * </p>
	 * 
	 * @return the template mode
	 */
	public ExtendedTemplateMode getTemplateMode() {
		return templateMode;
	}
	
	/**
	 * <p>
	 * Returns the static content type.
	 * </p>
	 * 
	 * @return the static content type
	 */
	public ExtendedTemplateMode.StaticContentType getStaticContentType() {
		return staticContentType;
	}
	
	/**
	 * <p>
	 * Returns the template name.
	 * </p>
	 * 
	 * @return the template name
	 */
	public String getTemplateName() {
		return templateName;
	}
	
	/**
	 * <p>
	 * Returns the template options.
	 * </p>
	 * 
	 * @return the template options
	 */
	public Options getOptions() {
		return options;
	}
	
	/**
	 * <p>
	 * Returns the generation mode.
	 * </p>
	 * 
	 * @return the generation mode
	 */
	public IrtClassGenerationContext.GenerationMode getMode() {
		return this.mode;
	}
	
	/**
	 * <p>
	 * Returns the indent depth.
	 * </p>
	 * 
	 * @return the indent depth
	 */
	public int getIndentDepth() {
		return indentDepth;
	}
	
	/**
	 * <p>
	 * Returns the static contents registered so far.
	 * </p>
	 * 
	 * @return the current of static contents
	 */
	public List<String> getStaticContents() {
		return this.staticContents;
	}
	
	/**
	 * <p>
	 * Returns the index of the specified static content and registers it if needed.
	 * </p>
	 * 
	 * @param content a static content
	 * 
	 * @return the index of the registered static content
	 */
	public int getIndexOfStaticContent(String content) {
		int index = this.staticContents.indexOf(content);
		if(index < 0) {
			index = this.staticContents.size();
			this.staticContents.add(content);
		}
		return index;
	}
	
	/**
	 * <p>
	 * Returns the accessor element (executable or variable) in the specified declared type that should be used to access the value targeted by the specified name.
	 * </p>
	 * 
	 * <p>An accessor is resolved using the following rules:</p>
	 * <ol>
	 * <li>consider the public getter method: {@code get[name]()}</li>
	 * <li>consider the public named method: {@code [name]()}</li>
	 * <li>consider the public field: {@code [name]}</li>
	 * </ol>
	 * 
	 * @param type a declared type
	 * @param name a name designating a value in the type
	 * 
	 * @return an accessor element (executable or variable) or null if no accessor exists
	 */
	public Element getAccessor(DeclaredType type, String name) {
		TypeElement element = (TypeElement)this.typeUtils.asElement(type);
		
		List<? extends Element> members = this.elementUtils.getAllMembers(element);
		
		// 1. e.getAbc()
		Optional<ExecutableElement> getterMethod = members.stream()
			.filter(e -> 
				!e.getModifiers().contains(Modifier.PRIVATE) && 
				e.getKind() == ElementKind.METHOD && 
				((ExecutableElement)e).getParameters().isEmpty() && 
				e.getSimpleName().toString().equals("get" + StringUtils.capitalize(name))
			)
			.map(e -> (ExecutableElement)e)
			.findFirst();
		
		if(getterMethod.isPresent()) {
			return getterMethod.get();
		}
		
		// 2. e.abc()
		Optional<ExecutableElement> namedMethod = members.stream()
			.filter(e -> 
				!e.getModifiers().contains(Modifier.PRIVATE) && 
				e.getKind() == ElementKind.METHOD && 
				((ExecutableElement)e).getParameters().isEmpty() && 
				e.getSimpleName().toString().equals(name)
			)
			.map(e -> (ExecutableElement)e)
			.findFirst();
		
		if(namedMethod.isPresent()) {
			return namedMethod.get();
		}
		
		// 3. e.abc
		Optional<VariableElement> field = members.stream()
			.filter(e -> 
				!e.getModifiers().contains(Modifier.PRIVATE) && 
				e.getKind() == ElementKind.FIELD && 
				e.getSimpleName().toString().equals(name)
			)
			.map(e -> (VariableElement)e)
			.findFirst();

		return field.orElse(null);
	}
	
	/**
	 * <p>
	 * Returns a Collector that concatenates the input elements into a StringBuilder, in encounter order.
	 * </p>
	 * 
	 * @return a Collector that concatenates the input elements into a StringBuilder, in encounter order
	 */
	public Collector<CharSequence, ?, StringBuilder> joining() {
		return Collector.of(
				StringBuilder::new,
				StringBuilder::append,
				StringBuilder::append, 
				stringBuilder -> stringBuilder
			);
	}
	
	/**
	 * <p>
	 * Returns a Collector that concatenates the input elements, separated by the specified delimiter, in encounter order.
	 * </p>
	 * 
	 * @param delimiter the delimiter to be used between each element
	 * 
	 * @return A Collector which concatenates CharSequence elements, separated by the specified delimiter, in encounter order
	 */
	public Collector<CharSequence, ?, StringBuilder> joining(CharSequence delimiter) {
		return Collector.of(
			StringBuilder::new, 
			(stringBuilder, seq) -> stringBuilder.append(seq).append(delimiter),
			StringBuilder::append, 
			stringBuilder -> !stringBuilder.isEmpty() ? stringBuilder.delete(stringBuilder.length() - delimiter.length(), stringBuilder.length()) : stringBuilder
		);
	}
	
	/**
	 * <p>
	 * This class is used to validate and expose the template set options specified in the IRT source file.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 *
	 */
	public static class Options {
		
		/**
		 * The charset option.
		 */
		public static final String OPTION_CHARSET = "charset";
		/**
		 * The modes option.
		 */
		public static final String OPTION_MODES = "modes";

		private Charset charset = Charsets.DEFAULT;
		
		private ExtendedTemplateMode[] modes = { ExtendedTemplateMode.STRING };

		/**
		 * <p>
		 * Creates options after validating the option info extracted from the IRT source file.
		 * </p>
		 * 
		 * @param options the list of option info extracted from the IRT source file
		 * 
		 * @throws IrtCompilationException if options are invalid
		 */
		@SuppressWarnings("unchecked")
		public Options(OptionInfo[] options) throws IrtCompilationException {
			for(OptionInfo option : options) {
				if(option.getName().equals(OPTION_CHARSET)) {
					if(option.getValue() instanceof String) {
						this.charset = Charset.forName((String)option.getValue());
					}
					else {
						reportTypeError(option, String.class);
					}
				}
				else if(option.getName().equals(OPTION_MODES)) {
					if(option.getValue() instanceof String) {
						this.modes = ExtendedTemplateMode.fromTemplateMode(TemplateMode.valueOf((String)option.getValue()));
					}
					else if(option.getValue() instanceof List) {
						try {
							this.modes = ((List<String>)option.getValue()).stream().map(TemplateMode::valueOf).map(ExtendedTemplateMode::fromTemplateMode).flatMap(Arrays::stream).toArray(ExtendedTemplateMode[]::new);
						} 
						catch (ClassCastException e) {
							reportTypeError(option, String.class, io.inverno.mod.base.reflect.Types.type(List.class).type(String.class).and().build());
						}
					}
					else {
						reportTypeError(option, String.class, io.inverno.mod.base.reflect.Types.type(List.class).type(String.class).and().build());
					}
				}
			}
		}
		
		/**
		 * <p>
		 * Reports an invalid option type error.
		 * </p>
		 * 
		 * @param option   the invalid option
		 * @param expected the expected types
		 */
		private static void reportTypeError(OptionInfo option, Type... expected) {
			throw new IrtCompilationException("Invalid value for option " + option.getName() + ", allowed type(s): " + Arrays.stream(expected).map(Type::toString).collect(Collectors.joining(", ")), ((LocatableInfo)option).getRange());
		}
		
		/**
		 * <p>
		 * Returns the value of the charset option.
		 * </p>
		 * 
		 * @return the charset option value
		 */
		public Charset getCharset() {
			return charset;
		}
		
		/**
		 * <p>
		 * Returns the list of template modes to generate.
		 * </p>
		 * 
		 * @return an array of template modes
		 */
		public ExtendedTemplateMode[] getModes() {
			return modes;
		}
	}
	
	/**
	 * <p>
	 * Extended template mode option which includes information for template set
	 * class generation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 */
	public enum ExtendedTemplateMode {
		
		/**
		 * Publisher ByteBuf mode
		 */
		PUBLISHER_BYTEBUF(TemplateMode.PUBLISHER_BYTEBUF, AbstractByteBufPublisherTemplateSet.class, StaticContentType.BYTEBUF, io.inverno.mod.base.reflect.Types.type(Publisher.class).type(ByteBuf.class).and().build().getTypeName()),
		/**
		 * Composite ByteBuf mode
		 */
		COMPOSITE_BYTEBUF(TemplateMode.BYTEBUF, AbstractCompositeByteBufTemplateSet.class, StaticContentType.BYTEBUF, io.inverno.mod.base.reflect.Types.type(CompletableFuture.class).type(ByteBuf.class).and().build().getTypeName()),
		/**
		 * ByteBuf mode
		 */
		BYTEBUF(TemplateMode.BYTEBUF, AbstractByteBufTemplateSet.class, StaticContentType.BYTES, io.inverno.mod.base.reflect.Types.type(CompletableFuture.class).type(ByteBuf.class).and().build().getTypeName()),
		/**
		 * Publisher String mode
		 */
		PUBLISHER_STRING(TemplateMode.PUBLISHER_STRING, AbstractStringPublisherTemplateSet.class, StaticContentType.STRING, io.inverno.mod.base.reflect.Types.type(Publisher.class).type(String.class).and().build().getTypeName()),
		/**
		 * String mode
		 */
		STRING(TemplateMode.STRING, AbstractStringTemplateSet.class, StaticContentType.STRING, io.inverno.mod.base.reflect.Types.type(CompletableFuture.class).type(String.class).and().build().getTypeName()),
		/**
		 * Stream mode
		 */
		STREAM(TemplateMode.STREAM, AbstractStreamTemplateSet.class, StaticContentType.BYTES, io.inverno.mod.base.reflect.Types.type(CompletableFuture.class).variableType("T").and().build().getTypeName());
		
		private final TemplateMode mode;
		
		private final Class<?> templateSetClass;
		
		private final StaticContentType staticContentType;
		
		private final String outputType;
		
		ExtendedTemplateMode(TemplateMode mode, Class<?> templateSetClass, StaticContentType staticContentType, String outputType) {
			this.mode = mode;
			this.templateSetClass = templateSetClass;
			this.staticContentType = staticContentType;
			this.outputType = outputType;
		}
		
		/**
		 * <p>
		 * Returns the original template mode.
		 * </p>
		 * 
		 * @return the original template mode
		 */
		public TemplateMode getMode() {
			return mode;
		}
		
		/**
		 * <p>
		 * Returns the template set base class to use to generate the mode.
		 * </p>
		 * 
		 * @return a template set base class
		 */
		public Class<?> getTemplateSetClass() {
			return templateSetClass;
		}
		
		/**
		 * <p>
		 * Returns the type of static content to use for this mode.
		 * </p>
		 * 
		 * @return a static content type
		 */
		public StaticContentType getStaticContentType() {
			return staticContentType;
		}
		
		/**
		 * <p>
		 * Returns the template set output type as string.
		 * </p>
		 * 
		 * @return the template set output type
		 */
		public String getOutputType() {
			return outputType;
		}
		
		/**
		 * <p>
		 * Returns the name of the method to generate for this mode.
		 * </p>
		 * 
		 * @return a method name
		 */
		public String toMethodName() {
			switch(this) {
				case STRING: return "string";
				case BYTEBUF:
				case COMPOSITE_BYTEBUF: return "bytebuf";
				case STREAM: return "stream";
				case PUBLISHER_STRING: return "publisherString";
				case PUBLISHER_BYTEBUF: return "publisherByteBuf";
				default: throw new IllegalArgumentException("Unsupported mode: " + mode);
			}
		}
		
		/**
		 * <p>
		 * Returns the extended template mode corresponding to the specified template mode.
		 * </p>
		 * 
		 * @param mode a template mode
		 * 
		 * @return an array of extended template modes
		 * 
		 * @throws IllegalArgumentException if the specified template mode is not supported
		 */
		public static ExtendedTemplateMode[] fromTemplateMode(TemplateMode mode) throws IllegalArgumentException {
			switch(mode) {
				case STRING: return new ExtendedTemplateMode[] { ExtendedTemplateMode.STRING };
				case BYTEBUF: return new ExtendedTemplateMode[] { ExtendedTemplateMode.COMPOSITE_BYTEBUF, ExtendedTemplateMode.BYTEBUF };
				case STREAM: return new ExtendedTemplateMode[] { ExtendedTemplateMode.STREAM };
				case PUBLISHER_STRING: return new ExtendedTemplateMode[] { ExtendedTemplateMode.PUBLISHER_STRING };
				case PUBLISHER_BYTEBUF: return new ExtendedTemplateMode[] { ExtendedTemplateMode.PUBLISHER_BYTEBUF };
				default: throw new IllegalArgumentException("Unsupported mode: " + mode);
			}
		}
		
		/**
		 * <p>
		 * The static content type specifies how static content should be stored to be optimized for a particular template mode.
		 * </p>
		 * 
		 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
		 * @since 1.2
		 */
		public enum StaticContentType {
			/**
			 * Indicates that static contents should be stored as String.
			 */
			STRING, 
			/**
			 * Indicates that static contents should be stored as ByteBuf.
			 */
			BYTEBUF, 
			/**
			 * Indicates that static contents should be stored as byte arrays.
			 */
			BYTES
		}
	}
}

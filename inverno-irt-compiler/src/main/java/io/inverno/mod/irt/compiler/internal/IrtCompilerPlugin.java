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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.DirectiveKind;

import io.inverno.core.compiler.spi.plugin.CompilerPlugin;
import io.inverno.core.compiler.spi.plugin.PluginContext;
import io.inverno.core.compiler.spi.plugin.PluginExecution;
import io.inverno.core.compiler.spi.plugin.PluginExecutionException;
import io.inverno.mod.irt.compiler.internal.parser.IrtParser;
import io.inverno.mod.irt.compiler.internal.parser.IrtTypeResolver;
import io.inverno.mod.irt.compiler.internal.parser.ParseException;
import io.inverno.mod.irt.compiler.internal.parser.StreamProvider;
import io.inverno.mod.irt.compiler.spi.TemplateSetInfo;

/**
 * <p>
 * The Inverno Reactive Template compiler plugin generates a template set class
 * for each <code>*.irt</code> source file present in the module source folder.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 * 
 */
public class IrtCompilerPlugin implements CompilerPlugin {

	public static final String DEFAULT_IRT_SOURCE_EXTENSION = ".irt";
	
	private final String irtFileExtension;
	
	private final IrtClassGenerator generator;
	
	private PluginContext pluginContext;
	
	private ModuleElement irtModuleElement;
	
	public IrtCompilerPlugin() {
		this.irtFileExtension = DEFAULT_IRT_SOURCE_EXTENSION;
		this.generator = new IrtClassGenerator();
	}

	@Override
	public void init(PluginContext pluginContext) {
		this.pluginContext = pluginContext;
		this.irtModuleElement = pluginContext.getElementUtils().getModuleElement("io.inverno.mod.irt");
	}

	@Override
	public boolean canExecute(ModuleElement moduleElement) {
		return this.irtModuleElement != null && moduleElement.getDirectives().stream().filter(d -> d.getKind() == DirectiveKind.REQUIRES).anyMatch(d -> ((ModuleElement.RequiresDirective)d).getDependency().equals(this.irtModuleElement));
	}

	@Override
	public void execute(PluginExecution execution) throws PluginExecutionException {
		try {
			Path moduleSourcePath = execution.getModuleSourceDir();
			List<CompilationResult> results = Files.walk(moduleSourcePath).filter(p -> p.getFileName().toString().endsWith(this.irtFileExtension))
				.map(sourceFilePath -> {
					String templateName = sourceFilePath.getFileName().toString();
					templateName = templateName.substring(0, templateName.length() - this.irtFileExtension.length());
					return this.compileSingle(execution, sourceFilePath.normalize(), StreamSupport.stream(moduleSourcePath.relativize(sourceFilePath).getParent().spliterator(), false).map(p -> p.toString()).collect(Collectors.joining(".")), templateName);
				})
				.collect(Collectors.toList());
			if(this.pluginContext.getOptions().isVerbose()) {
				System.out.println();
				System.out.print("     ");
			}
			if(results.stream().anyMatch(CompilationResult::hasError)) {
				StringBuilder errorMessage = new StringBuilder();
				errorMessage.append("IRT compilation failure").append(System.lineSeparator());
				errorMessage.append(results.stream()
					.filter(result -> !result.getErrors().isEmpty())
					.map(result -> result.errors.stream().map(e -> result.getSourceFilePath().toString() + ": " + e.getMessage()).collect(Collectors.joining(System.lineSeparator())))
					.collect(Collectors.joining(System.lineSeparator()))
				);
				throw new PluginExecutionException(errorMessage.toString());
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private CompilationResult compileSingle(PluginExecution execution, Path sourceFilePath, String expectedPackage, String templateName) {
		if(this.pluginContext.getOptions().isVerbose()) {
			System.out.println();
			System.out.print("     - Compiling " + sourceFilePath + "...");
		}
		
		CompilationResult result = new CompilationResult(sourceFilePath);
		
		if(!SourceVersion.isIdentifier(templateName)) {
			result.addError(new IrtCompilationException("Template name is not a valid Java identifier: " + templateName));
		}

		try (InputStream input = Files.newInputStream(sourceFilePath, StandardOpenOption.READ)) {
			IrtParser irtParser = new IrtParser(new StreamProvider(input));
			irtParser.setTypeResolver(new IrtTypeResolver(this.pluginContext.getTypeUtils(), this.pluginContext.getElementUtils()));
			
			TemplateSetInfo info = irtParser.TemplateSet();
			
			String declaredPackage = info.getPackage().map(packageInfo -> Arrays.stream(packageInfo.getName().getParts()).collect(Collectors.joining("."))).orElse("");
			if(!expectedPackage.equals(declaredPackage)) {
				result.addError(new IrtCompilationException("The declared package \"" + declaredPackage + "\" does not match the expected package \"" + expectedPackage + "\""));
			}
			
			execution.createSourceFile(declaredPackage.isEmpty() ? templateName : (declaredPackage + "." + templateName), new Element[0], () -> {
				return info.accept(this.generator, new IrtClassGenerationContext(this.pluginContext.getTypeUtils(), this.pluginContext.getElementUtils(), templateName, info.getOptions(), IrtClassGenerationContext.GenerationMode.IRT_CLASS)).toString();
			});
		}
		catch (IrtCompilationException e) {
			result.addError(e);
		}
		catch (ParseException e) {
			result.addError(new IrtCompilationException(e));
		}
		catch (Exception e) {
			result.addError(new IrtCompilationException(e));
		}
		
		if(this.pluginContext.getOptions().isVerbose()) {
			if(result.hasError()) {
				System.out.print(" [  KO  ]");
				for(int i=0;i<result.getErrors().size();i++) {
					if(i >= 0) {
						System.err.println();
					}
					System.err.print("         - [ERROR] " + result.getErrors().get(i).getMessage());
				}
			}
			else {
				System.out.print(" [  OK  ]");
			}
		}
		return result;
	}
	
	/**
	 * <p>
	 * The result of the compilation of an IRT source file.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 *
	 */
	private static class CompilationResult {
		
		private Path sourceFilePath;
		
		private List<IrtCompilationException> errors;
		
		public CompilationResult(Path sourceFilePath) {
			this.sourceFilePath = sourceFilePath;
			this.errors = new ArrayList<>();
		}

		public Path getSourceFilePath() {
			return sourceFilePath;
		}
		
		public void addError(IrtCompilationException error) {
			this.errors.add(error);
		}
		
		public boolean hasError() {
			return !this.errors.isEmpty();
		}
		
		public List<IrtCompilationException> getErrors() {
			return errors;
		}
	}
}

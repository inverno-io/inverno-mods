/*
 * Copyright 2020 Jeremy KUHN
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
package io.inverno.mod.configuration.compiler.internal;

import io.inverno.core.compiler.spi.BeanQualifiedName;
import io.inverno.core.compiler.spi.QualifiedNameFormatException;
import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.core.compiler.spi.plugin.CompilerPlugin;
import io.inverno.core.compiler.spi.plugin.PluginContext;
import io.inverno.core.compiler.spi.plugin.PluginExecution;
import io.inverno.core.compiler.spi.plugin.PluginExecutionException;
import io.inverno.mod.configuration.Configuration;
import io.inverno.mod.configuration.ConfigurationLoader;
import io.inverno.mod.configuration.compiler.internal.ConfigurationLoaderClassGenerationContext.GenerationMode;
import io.inverno.mod.configuration.compiler.spi.ConfigurationInfo;
import io.inverno.mod.configuration.compiler.spi.ConfigurationPropertyInfo;
import io.inverno.mod.configuration.compiler.spi.PropertyQualifiedName;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * <p>
 * The configuration Inverno compiler plugin generates a {@link ConfigurationLoader} implementation for each {@link Configuration @Configuration} annotated types in a module.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class ConfigurationCompilerPlugin implements CompilerPlugin {

	private final ConfigurationLoaderClassGenerator configurationLoaderClassGenerator;
	
	private PluginContext pluginContext;
	
	private TypeMirror configurationAnnotationType;
	
	private boolean enabled = true;
	
	public ConfigurationCompilerPlugin() {
		this.configurationLoaderClassGenerator = new ConfigurationLoaderClassGenerator();
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Set.of(Configuration.class.getCanonicalName());
	}

	@Override
	public void init(PluginContext pluginContext) {
		this.pluginContext = pluginContext;
		
		TypeElement configurationAnnotationElement = this.pluginContext.getElementUtils().getTypeElement(Configuration.class.getCanonicalName());
		if(configurationAnnotationElement == null) {
			this.enabled = false;
			if(pluginContext.getOptions().isDebug()) {
				System.err.println("Plugin " + ConfigurationCompilerPlugin.class.getCanonicalName() + " disabled due to missing dependencies");
			}
			return;
		}
		this.configurationAnnotationType = configurationAnnotationElement.asType();
	}
	
	@Override
	public boolean canExecute(ModuleElement moduleElement) {
		// We check that Configuration is accessible from the module being compiled
		return this.enabled && this.pluginContext.getElementUtils().getTypeElement(moduleElement, Configuration.class.getCanonicalName()) != null;
	}

	@Override
	public void execute(PluginExecution execution) throws PluginExecutionException {
		for(TypeElement element : execution.<TypeElement>getElementsAnnotatedWith(Configuration.class)) {
			DeclaredType configurationType = (DeclaredType)element.asType();
			
			AnnotationMirror configurationAnnotation = null;
			for(AnnotationMirror annotation : element.getAnnotationMirrors()) {
				if(this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.configurationAnnotationType)) {
					configurationAnnotation = annotation;
					break;
				}
			}
			if(configurationAnnotation == null) {
				throw new PluginExecutionException("The specified element is not annotated with " + Configuration.class.getSimpleName());
			}
			
			if(!this.pluginContext.getElementUtils().getModuleOf(element).getQualifiedName().toString().equals(execution.getModuleQualifiedName().toString())) {
				throw new PluginExecutionException("The specified element doesn't belong to module " + execution.getModuleQualifiedName());
			}
			
			ReporterInfo beanReporter = execution.getReporter(element, configurationAnnotation);
			
			if(!element.getKind().equals(ElementKind.INTERFACE)) {
				// This should never happen, we shouldn't get there if it wasn't an interface
				beanReporter.error("A configuration must be an interface");
				continue;
			}
			
			String name = null;
			boolean generateBean = false;
			boolean overridable = false;
			for(Entry<? extends ExecutableElement, ? extends AnnotationValue> value : this.pluginContext.getElementUtils().getElementValuesWithDefaults(configurationAnnotation).entrySet()) {
				switch(value.getKey().getSimpleName().toString()) {
					case "name" : name = (String)value.getValue().getValue();
						break;
					case "generateBean" : generateBean = Boolean.parseBoolean(value.getValue().getValue().toString());
						break;
					case "overridable" : overridable = Boolean.parseBoolean(value.getValue().getValue().toString());
						break;
				}
			}
			
			// Bean qualified name
			if(name == null || name.isEmpty()) {
				name = element.getSimpleName().toString();
				name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
			}
			
			BeanQualifiedName configurationQName;
			try {
				configurationQName = new BeanQualifiedName(execution.getModuleQualifiedName(), name);
			} 
			catch (QualifiedNameFormatException e) {
				beanReporter.error("Invalid bean qualified name: " + e.getMessage());
				continue;
			}

			List<? extends ConfigurationPropertyInfo> configurationProperties = this.pluginContext.getElementUtils().getAllMembers(element).stream()
				.filter(el -> el.getKind().equals(ElementKind.METHOD) && !this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getElementUtils().getTypeElement(Object.class.getCanonicalName()).asType(), el.getEnclosingElement().asType()))
				.map(el -> {
					ExecutableElement propertyMethod = (ExecutableElement)el;
					
					PropertyQualifiedName propertyQName = new PropertyQualifiedName(configurationQName, propertyMethod.getSimpleName().toString());
					ReporterInfo propertyReporter = execution.getReporter(propertyMethod);
					boolean invalid = false;
					if(!propertyMethod.getParameters().isEmpty()) {
						execution.getReporter(propertyMethod.getParameters().getFirst()).error("Configuration property must be declared as a no-argument method");
						invalid = true;
					}
					if(propertyMethod.getReturnType().getKind().equals(TypeKind.VOID)) {
						propertyReporter.error("Configuration property must be declared as a non-void method");
						invalid = true;
					}
					if(invalid) {
						return null;
					}
					
					if(this.isNestedConfiguration(propertyMethod)) {
						return new GenericNestedConfigurationProperty(propertyQName, propertyReporter, propertyMethod, this.extractNestedConfigurationInfo(propertyReporter, propertyQName, (TypeElement)this.pluginContext.getTypeUtils().asElement(propertyMethod.getReturnType())));
					}
					else {
						return new GenericConfigurationPropertyInfo(propertyQName, propertyReporter, propertyMethod);
					}
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
			
			ConfigurationInfo configurationInfo = new GenericConfigurationInfo(configurationQName, beanReporter, configurationType, configurationProperties, generateBean, overridable);
			
			try {
				execution.createSourceFile(configurationInfo.getType().toString() + "Loader", new Element[] {element}, () -> configurationInfo.accept(this.configurationLoaderClassGenerator, new ConfigurationLoaderClassGenerationContext(this.pluginContext.getTypeUtils(), this.pluginContext.getElementUtils(), GenerationMode.CONFIGURATION_LOADER_CLASS)).toString());
			}
			catch (IOException e) {
				throw new PluginExecutionException("Unable to generate configuration loader class " + configurationInfo.getType().toString() + "Loader", e);
			}
		}
	}
	
	private ConfigurationInfo extractNestedConfigurationInfo(ReporterInfo rootPropertyReporter, PropertyQualifiedName nestedPropertyName, TypeElement nestedTypeElement) {
		List<? extends ConfigurationPropertyInfo> configurationProperties = this.pluginContext.getElementUtils().getAllMembers(nestedTypeElement).stream()
			.filter(el -> el.getKind().equals(ElementKind.METHOD) && !this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getElementUtils().getTypeElement(Object.class.getCanonicalName()).asType(), el.getEnclosingElement().asType()))
			.map(el -> {
				ExecutableElement propertyMethod = (ExecutableElement)el;
				PropertyQualifiedName propertyQName = new PropertyQualifiedName(nestedPropertyName, propertyMethod.getSimpleName().toString());
				
				boolean invalid = false;
				if(!propertyMethod.getParameters().isEmpty()) {
					rootPropertyReporter.warning("Ignoring invalid nested property " + propertyQName.getBeanName() + " which should be defined as a no-argument method");
					invalid = true;
				}
				if(propertyMethod.getReturnType().getKind().equals(TypeKind.VOID)) {
					rootPropertyReporter.warning("Ignoring invalid nested property " + propertyQName.getBeanName() + " which should be defined as a non-void method");
					invalid = true;
				}
				if(invalid) {
					return null;
				}
				
				if(this.isNestedConfiguration(propertyMethod)) {
					return new GenericNestedConfigurationProperty(propertyQName, rootPropertyReporter, propertyMethod, this.extractNestedConfigurationInfo(rootPropertyReporter, propertyQName, (TypeElement)this.pluginContext.getTypeUtils().asElement(propertyMethod.getReturnType())));
				}
				else {
					return new GenericConfigurationPropertyInfo(propertyQName, rootPropertyReporter, propertyMethod);
				}
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		
		return new GenericConfigurationInfo(nestedPropertyName, rootPropertyReporter, (DeclaredType)nestedTypeElement.asType(), configurationProperties);
	}
	
	private boolean isNestedConfiguration(ExecutableElement propertyMethod) {
		TypeMirror type = propertyMethod.getReturnType();
		if(type.getKind().equals(TypeKind.DECLARED)) {
			TypeElement typeElement = (TypeElement)this.pluginContext.getTypeUtils().asElement(type);
			return typeElement.getAnnotationMirrors().stream().anyMatch(annotation -> this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.configurationAnnotationType));
		}
		else {
			// primitive, array...
			return false;
		}
	}
}

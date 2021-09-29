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

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.NestedBean;
import io.inverno.core.annotation.Overridable;
import io.inverno.core.annotation.Wrapper;
import io.inverno.mod.configuration.ConfigurationKey.Parameter;
import io.inverno.mod.configuration.ConfigurationLoader;
import io.inverno.mod.configuration.ConfigurationLoaderSupport;
import io.inverno.mod.configuration.ConfigurationSource;
import io.inverno.mod.configuration.compiler.internal.ConfigurationLoaderClassGenerationContext.GenerationMode;
import io.inverno.mod.configuration.compiler.spi.ConfigurationInfo;
import io.inverno.mod.configuration.compiler.spi.ConfigurationInfoVisitor;
import io.inverno.mod.configuration.compiler.spi.ConfigurationPropertyInfo;
import io.inverno.mod.configuration.compiler.spi.NestedConfigurationPropertyInfo;

/**
 * <p>
 * A {@link ConfigurationInfoVisitor} implementation used to generate a
 * {@link ConfigurationLoader} class from a {@link ConfigurationInfo}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ConfigurationInfo
 * @see ConfigurationInfoVisitor
 * @see ConfigurationLoader
 */
class ConfigurationLoaderClassGenerator implements ConfigurationInfoVisitor<StringBuilder, ConfigurationLoaderClassGenerationContext> {
	
	@Override
	public StringBuilder visit(ConfigurationInfo configurationInfo, ConfigurationLoaderClassGenerationContext context) {
		String configurationClassName = configurationInfo.getType().toString();
		String packageName = configurationClassName.lastIndexOf(".") != -1 ? configurationClassName.substring(0, configurationClassName.lastIndexOf(".")) : "";
		configurationClassName = configurationClassName.substring(packageName.length() + 1);
		StringBuilder configurationLoaderClassName = new StringBuilder(configurationClassName).append(ConfigurationLoaderClassGenerationContext.CONFIGURATION_LOADER_CLASS_SUFFIX);
		if(context.getMode() == GenerationMode.CONFIGURATION_LOADER_CLASS) {
			TypeMirror configurationLoaderSupportType = context.getTypeUtils().erasure(context.getElementUtils().getTypeElement(ConfigurationLoaderSupport.class.getCanonicalName()).asType());
			TypeMirror consumerType = context.getTypeUtils().erasure(context.getElementUtils().getTypeElement(Consumer.class.getCanonicalName()).asType());
			
			context.addImport(ConfigurationLoaderClassGenerationContext.CONFIGURATION_INNER_CLASS, configurationClassName + ConfigurationLoaderClassGenerationContext.CONFIGURATION_LOADER_CLASS_SUFFIX + "." + ConfigurationLoaderClassGenerationContext.CONFIGURATION_INNER_CLASS);
			context.addImport(ConfigurationLoaderClassGenerationContext.CONFIGURATOR_INNER_CLASS, configurationClassName + ConfigurationLoaderClassGenerationContext.CONFIGURATION_LOADER_CLASS_SUFFIX + "." + ConfigurationLoaderClassGenerationContext.CONFIGURATOR_INNER_CLASS);
			context.addImport(ConfigurationLoaderClassGenerationContext.CONFIGURATION_BEAN_INNER_CLASS, configurationClassName + ConfigurationLoaderClassGenerationContext.CONFIGURATION_LOADER_CLASS_SUFFIX + "." + ConfigurationLoaderClassGenerationContext.CONFIGURATION_BEAN_INNER_CLASS);
			
			StringBuilder configurationLoader_constructor = new StringBuilder(context.indent(1)).append("public ").append(configurationLoaderClassName).append("() {\n");
			configurationLoader_constructor.append(context.indent(2)).append("super(\n");
			// properties
			configurationLoader_constructor.append(context.indent(3)).append("new String[] {\n");
			configurationLoader_constructor.append(this.visit(configurationInfo, context.withIndentDepth(4).withMode(GenerationMode.CONFIGURATION_PROPERTIES))).append("\n");
			configurationLoader_constructor.append(context.indent(3)).append("},\n");
			// resultsToConfigurer
			context.getResultIndex().set(0);
			configurationLoader_constructor.append(context.indent(3)).append("results -> ");
			configurationLoader_constructor.append(this.visit(configurationInfo, context.withIndentDepth(3).withMode(GenerationMode.CONFIGURATION_CONFIGURER)));
			configurationLoader_constructor.append(",\n");
			// configurationCreator
			configurationLoader_constructor.append(context.indent(3)).append(configurationLoaderClassName).append("::load").append(",\n");
			configurationLoader_constructor.append(context.indent(3)).append(configurationLoaderClassName).append(".").append(ConfigurationLoaderClassGenerationContext.CONFIGURATION_INNER_CLASS).append("::new\n");
			configurationLoader_constructor.append(context.indent(2)).append(");\n");
			configurationLoader_constructor.append(context.indent(1)).append("}");
			
			StringBuilder configurationLoader_load_method = new StringBuilder(context.indent(1)).append("public static ").append(context.getTypeName(configurationInfo.getType())).append(" load(").append(context.getTypeName(consumerType)).append("<").append(configurationLoaderClassName).append(".").append(ConfigurationLoaderClassGenerationContext.CONFIGURATOR_INNER_CLASS).append("> configurer) {\n");
			configurationLoader_load_method.append(context.indent(2)).append(configurationLoaderClassName).append(".").append(ConfigurationLoaderClassGenerationContext.CONFIGURATOR_INNER_CLASS).append(" configurator = new ").append(configurationLoaderClassName).append(".").append(ConfigurationLoaderClassGenerationContext.CONFIGURATOR_INNER_CLASS).append("();\n");
			configurationLoader_load_method.append(context.indent(2)).append("configurer.accept(configurator);\n");
			configurationLoader_load_method.append(context.indent(2)).append("return new ").append(configurationLoaderClassName).append(".").append(ConfigurationLoaderClassGenerationContext.CONFIGURATION_INNER_CLASS).append("(");
			configurationLoader_load_method.append(Arrays.stream(configurationInfo.getProperties())
				.map(propertyInfo -> new StringBuilder().append("configurator.").append(propertyInfo.getQualifiedName().getPropertyName()).toString())
				.collect(context.joining(", ")));
			configurationLoader_load_method.append(");\n");
			configurationLoader_load_method.append(context.indent(1)).append("}");
					
			StringBuilder configurationLoader_configuration_class = this.visit(configurationInfo, context.withIndentDepth(1).withMode(GenerationMode.CONFIGURATION_IMPL_CLASS));
			StringBuilder configurationLoader_configurator_class = this.visit(configurationInfo, context.withIndentDepth(1).withMode(GenerationMode.CONFIGURATION_CONFIGURATOR_CLASS));
			
			StringBuilder configurationLoader_class = new StringBuilder().append(context.indent(0)).append("public final class ").append(configurationLoaderClassName).append(" extends ").append(context.getTypeName(configurationLoaderSupportType)).append("<").append(context.getTypeName(configurationInfo.getType())).append(", ").append(configurationLoaderClassName).append(".").append(ConfigurationLoaderClassGenerationContext.CONFIGURATOR_INNER_CLASS).append(", ").append(configurationLoaderClassName).append("> {\n\n");
			
			configurationLoader_class.append(configurationLoader_constructor).append("\n\n");
			configurationLoader_class.append(configurationLoader_load_method).append("\n\n");
			configurationLoader_class.append(configurationLoader_configuration_class).append("\n\n");
			configurationLoader_class.append(configurationLoader_configurator_class).append("\n");
			
			if(configurationInfo.isGenerateBean()) {
				configurationLoader_class.append("\n");
				configurationLoader_class.append(this.visit(configurationInfo, context.withIndentDepth(1).withMode(GenerationMode.CONFIGURATION_BEAN_CLASS))).append("\n");
			}
			configurationLoader_class.append(context.indent(0)).append("}");
			
			context.removeImport(ConfigurationLoaderClassGenerationContext.CONFIGURATION_INNER_CLASS);
			context.removeImport(ConfigurationLoaderClassGenerationContext.CONFIGURATOR_INNER_CLASS);
			context.removeImport(ConfigurationLoaderClassGenerationContext.CONFIGURATION_BEAN_INNER_CLASS);
			
			configurationLoader_class.insert(0, "\n\n").insert(0, context.getImports().stream().sorted().filter(i -> i.lastIndexOf(".") > 0 && !i.substring(0, i.lastIndexOf(".")).equals(packageName)).map(i -> new StringBuilder().append("import ").append(i).append(";")).collect(context.joining("\n")));
			if(!packageName.equals("")) {
				configurationLoader_class.insert(0, ";\n\n").insert(0, packageName).insert(0, "package ");
			}
			
			return configurationLoader_class;
		}
		else if(context.getMode() == GenerationMode.CONFIGURATION_PROPERTIES) {
			return Arrays.stream(configurationInfo.getProperties()).map(propertyInfo -> this.visit(propertyInfo , context.withConfiguration(configurationInfo).withMode(GenerationMode.CONFIGURATION_PROPERTY_NAME))).collect(context.joining(",\n"));
		}
		else if(context.getMode() == GenerationMode.CONFIGURATION_CONFIGURER) {
			StringBuilder configurer = new StringBuilder().append(configurationInfo.getQualifiedName().normalize()).append("_configurator -> {\n"); 
			configurer.append(Arrays.stream(configurationInfo.getProperties()).map(propertyInfo -> this.visit(propertyInfo, context.withIndentDepthAdd(1).withConfiguration(configurationInfo).withMode(GenerationMode.CONFIGURATION_PROPERTY_CONFIGURER))).collect(context.joining("\n"))).append("\n");
			configurer.append(context.indent(0)).append("}");
			return configurer;
		}
		else if(context.getMode() == GenerationMode.CONFIGURATION_IMPL_CLASS) {
			StringBuilder configurationImpl_field_properties = Arrays.stream(configurationInfo.getProperties())
				.map(propertyInfo -> this.visit(propertyInfo, context.withConfiguration(configurationInfo).withMode(GenerationMode.CONFIGURATION_IMPL_PROPERTY_FIELD)))
				.collect(context.joining("\n"));
			
			StringBuilder configurationImpl_default_constructor = new StringBuilder().append(context.indent(1)).append("private ").append(ConfigurationLoaderClassGenerationContext.CONFIGURATION_INNER_CLASS).append("() {}");
			
			StringBuilder configurationImpl_constructor = new StringBuilder();
			if(configurationInfo.getProperties().length > 0) {
				configurationImpl_constructor.append(context.indent(1)).append("public ").append(ConfigurationLoaderClassGenerationContext.CONFIGURATION_INNER_CLASS).append("(");
				configurationImpl_constructor.append(Arrays.stream(configurationInfo.getProperties()).map(propertyInfo -> this.visit(propertyInfo, context.withMode(GenerationMode.CONFIGURATION_IMPL_PROPERTY_PARAMETER))).collect(context.joining(", ")));
				configurationImpl_constructor.append(") {\n");
				configurationImpl_constructor.append(Arrays.stream(configurationInfo.getProperties()).map(propertyInfo -> this.visit(propertyInfo, context.withMode(GenerationMode.CONFIGURATION_IMPL_PROPERTY_ASSIGNMENT))).collect(context.joining("\n")));
				configurationImpl_constructor.append("\n");
				configurationImpl_constructor.append(context.indent(1)).append("}");
			}
			
			StringBuilder configurationImpl_property_accessors = Arrays.stream(configurationInfo.getProperties())
				.map(propertyInfo -> this.visit(propertyInfo, context.withMode(GenerationMode.CONFIGURATION_IMPL_PROPERTY_ACCESSOR)))
				.collect(context.joining("\n"));
			
			StringBuilder configurationImplClass = new StringBuilder().append(context.indent(0)).append("private static final class ").append(ConfigurationLoaderClassGenerationContext.CONFIGURATION_INNER_CLASS).append(" implements ").append(context.getTypeName(configurationInfo.getType())).append(" {\n\n");
			
			configurationImplClass.append(configurationImpl_field_properties).append("\n\n");
			configurationImplClass.append(configurationImpl_default_constructor).append("\n\n");
			if(configurationImpl_constructor.length() > 0) {
				configurationImplClass.append(configurationImpl_constructor).append("\n\n");
			}
			configurationImplClass.append(configurationImpl_property_accessors);
			
			configurationImplClass.append(context.indent(0)).append("}");
			
			return configurationImplClass;
		}
		else if(context.getMode() == GenerationMode.CONFIGURATION_CONFIGURATOR_CLASS) {
			StringBuilder configurationConfigurator_field_properties = Arrays.stream(configurationInfo.getProperties())
				.map(propertyInfo -> this.visit(propertyInfo, context.withMode(GenerationMode.CONFIGURATION_CONFIGURATOR_PROPERTY_FIELD)))
				.collect(context.joining("\n"));
			
			StringBuilder configurationConfigurator_default_constructor = new StringBuilder().append(context.indent(1)).append("private ").append(ConfigurationLoaderClassGenerationContext.CONFIGURATOR_INNER_CLASS).append("() {}");
			
			StringBuilder configurationConfigurator_property_injectors = Arrays.stream(configurationInfo.getProperties())
				.map(propertyInfo -> this.visit(propertyInfo, context.withMode(GenerationMode.CONFIGURATION_CONFIGURATOR_PROPERTY_INJECTOR)))
				.collect(context.joining("\n"));
			
			StringBuilder configurationConfiguratorClass = new StringBuilder().append(context.indent(0)).append("public static final class ").append(ConfigurationLoaderClassGenerationContext.CONFIGURATOR_INNER_CLASS).append(" {\n\n");
			
			configurationConfiguratorClass.append(configurationConfigurator_field_properties).append("\n\n");
			configurationConfiguratorClass.append(configurationConfigurator_default_constructor).append("\n\n");
			configurationConfiguratorClass.append(configurationConfigurator_property_injectors);
			
			configurationConfiguratorClass.append(context.indent(0)).append("}");
			
			return configurationConfiguratorClass;
		}
		else if(context.getMode() == GenerationMode.CONFIGURATION_BEAN_CLASS) {
			TypeMirror beanAnnotationType = context.getElementUtils().getTypeElement(Bean.class.getCanonicalName()).asType();
			TypeMirror wrapperAnnotationType = context.getElementUtils().getTypeElement(Wrapper.class.getCanonicalName()).asType();
			TypeMirror configurationLoaderSupportType = context.getTypeUtils().erasure(context.getElementUtils().getTypeElement(ConfigurationLoaderSupport.class.getCanonicalName()).asType());
			TypeMirror configurationSupplierType = context.getTypeUtils().getDeclaredType(context.getElementUtils().getTypeElement(Supplier.class.getCanonicalName()), configurationInfo.getType());
			TypeMirror consumerType = context.getTypeUtils().erasure(context.getElementUtils().getTypeElement(Consumer.class.getCanonicalName()).asType());
			WildcardType unknownWildcardType = context.getTypeUtils().getWildcardType(null, null);
			TypeMirror configurationSourceType = context.getTypeUtils().getDeclaredType(context.getElementUtils().getTypeElement(ConfigurationSource.class.getCanonicalName()), unknownWildcardType, unknownWildcardType, unknownWildcardType);
			TypeMirror parameterType = context.getElementUtils().getTypeElement(Parameter.class.getCanonicalName()).asType();
			
			StringBuilder configurationBeanClass = new StringBuilder().append(context.indent(0)).append("@").append(context.getTypeName(beanAnnotationType)).append("(name = \"").append(configurationInfo.getQualifiedName().getBeanName()).append("\")\n");
			configurationBeanClass.append(context.indent(0)).append("@").append(context.getTypeName(wrapperAnnotationType)).append("\n");
			if(configurationInfo.isOverridable()) {
				TypeMirror overridableAnnotationType = context.getElementUtils().getTypeElement(Overridable.class.getCanonicalName()).asType();
				configurationBeanClass.append(context.indent(0)).append("@").append(context.getTypeName(overridableAnnotationType)).append("\n");
			}
			configurationBeanClass.append(context.indent(0)).append("public static final class ").append(ConfigurationLoaderClassGenerationContext.CONFIGURATION_BEAN_INNER_CLASS).append(" extends ").append(context.getTypeName(configurationLoaderSupportType)).append(".ConfigurationBeanSupport<").append(context.getTypeName(configurationInfo.getType())).append(", ").append(configurationLoaderClassName).append(".").append(ConfigurationLoaderClassGenerationContext.CONFIGURATOR_INNER_CLASS).append(", ").append(configurationLoaderClassName).append("> implements ").append(context.getTypeName(configurationSupplierType)).append(" {\n\n");
			configurationBeanClass.append(context.indent(1)).append("public ").append(ConfigurationLoaderClassGenerationContext.CONFIGURATION_BEAN_INNER_CLASS).append("() {\n");
			configurationBeanClass.append(context.indent(2)).append("super(new ").append(configurationLoaderClassName).append("());\n");
			configurationBeanClass.append(context.indent(1)).append("}\n\n");
			configurationBeanClass.append(context.indent(1)).append("public void setConfigurationSource(").append(context.getTypeName(configurationSourceType)).append(" source) {\n");
			configurationBeanClass.append(context.indent(2)).append("this.loader.withSource(source);\n");
			configurationBeanClass.append(context.indent(1)).append("}\n\n");
			configurationBeanClass.append(context.indent(1)).append("public void setParameters(").append(context.getTypeName(parameterType)).append("[] parameters) {\n");
			configurationBeanClass.append(context.indent(2)).append("this.loader.withParameters(parameters);\n");
			configurationBeanClass.append(context.indent(1)).append("}\n\n");
			configurationBeanClass.append(context.indent(1)).append("public void setConfigurer(").append(context.getTypeName(consumerType)).append("<").append(configurationLoaderClassName).append(".").append(ConfigurationLoaderClassGenerationContext.CONFIGURATOR_INNER_CLASS).append("> configurer) {\n");
			configurationBeanClass.append(context.indent(2)).append("this.loader.withConfigurer(configurer);\n");
			configurationBeanClass.append(context.indent(1)).append("}\n");
			configurationBeanClass.append(context.indent(0)).append("}");
			
			return configurationBeanClass;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(ConfigurationPropertyInfo configurationPropertyInfo, ConfigurationLoaderClassGenerationContext context) {
		if(context.getMode() == GenerationMode.CONFIGURATION_IMPL_PROPERTY_FIELD) {
			StringBuilder result = new StringBuilder().append(context.indent(1)).append("private ").append(context.getTypeName(configurationPropertyInfo.getType())).append(" ").append(configurationPropertyInfo.getQualifiedName().getPropertyName());
			if(configurationPropertyInfo.isDefault()) {
				result.append(" = ").append(context.getTypeName(context.getConfiguration().getType())).append(".super.").append(configurationPropertyInfo.getQualifiedName().getPropertyName()).append("()");
			}
			else if(configurationPropertyInfo instanceof NestedConfigurationPropertyInfo) {
				result.append(" = ").append(context.getTypeName(((NestedConfigurationPropertyInfo)configurationPropertyInfo).getConfiguration().getType())).append(ConfigurationLoaderClassGenerationContext.CONFIGURATION_LOADER_CLASS_SUFFIX + ".load(configurator -> {})");
			}
			result.append(";");
			return result;
		}
		else if(context.getMode() == GenerationMode.CONFIGURATION_IMPL_PROPERTY_PARAMETER) {
			TypeMirror boxedType = configurationPropertyInfo.getType() instanceof PrimitiveType ? context.getTypeUtils().boxedClass((PrimitiveType)configurationPropertyInfo.getType()).asType() : configurationPropertyInfo.getType();
			return new StringBuilder().append(context.getTypeName(context.getTypeUtils().getDeclaredType(context.getElementUtils().getTypeElement(Supplier.class.getCanonicalName()), boxedType))).append(" ").append(configurationPropertyInfo.getQualifiedName().getPropertyName());
		}
		else if(context.getMode() == GenerationMode.CONFIGURATION_IMPL_PROPERTY_ASSIGNMENT) {
			TypeMirror optionalType = context.getTypeUtils().erasure(context.getElementUtils().getTypeElement(Optional.class.getCanonicalName()).asType());
			return new StringBuilder().append(context.indent(2)).append(context.getTypeName(optionalType)).append(".ofNullable(").append(configurationPropertyInfo.getQualifiedName().getPropertyName()).append(").ifPresent(s -> this.").append(configurationPropertyInfo.getQualifiedName().getPropertyName()).append(" = s.get());");
		}
		else if(context.getMode() == GenerationMode.CONFIGURATION_IMPL_PROPERTY_ACCESSOR) {
			StringBuilder result = new StringBuilder();
			if(configurationPropertyInfo instanceof NestedConfigurationPropertyInfo) {
				TypeMirror nestedAnnotationType = context.getElementUtils().getTypeElement(NestedBean.class.getCanonicalName()).asType();
				result.append(context.indent(1)).append("@").append(context.getTypeName(nestedAnnotationType)).append("\n");
			}
			result.append(context.indent(1)).append("public ").append(context.getTypeName(configurationPropertyInfo.getType())).append(" ").append(configurationPropertyInfo.getQualifiedName().getPropertyName()).append("() {\n");
			result.append(context.indent(2)).append(" return this.").append(configurationPropertyInfo.getQualifiedName().getPropertyName()).append(";\n");
			result.append(context.indent(1)).append("}\n");
			return result;
		}
		else if(context.getMode() == GenerationMode.CONFIGURATION_CONFIGURATOR_PROPERTY_FIELD) {
			TypeMirror boxedType = configurationPropertyInfo.getType() instanceof PrimitiveType ? context.getTypeUtils().boxedClass((PrimitiveType)configurationPropertyInfo.getType()).asType() : configurationPropertyInfo.getType();
			return new StringBuilder().append(context.indent(1)).append("private ").append(context.getTypeName(context.getTypeUtils().getDeclaredType(context.getElementUtils().getTypeElement(Supplier.class.getCanonicalName()), boxedType))).append(" ").append(configurationPropertyInfo.getQualifiedName().getPropertyName()).append(";");
		}
		else if(context.getMode() == GenerationMode.CONFIGURATION_CONFIGURATOR_PROPERTY_INJECTOR) {
			StringBuilder result = new StringBuilder().append(context.indent(1)).append("public ").append(ConfigurationLoaderClassGenerationContext.CONFIGURATOR_INNER_CLASS).append(" ").append(configurationPropertyInfo.getQualifiedName().getPropertyName()).append("(").append(context.getTypeName(configurationPropertyInfo.getType())).append(" ").append(configurationPropertyInfo.getQualifiedName().getPropertyName()).append(") {\n");
			result.append(context.indent(2)).append("this.").append(configurationPropertyInfo.getQualifiedName().getPropertyName()).append(" = () -> ").append(configurationPropertyInfo.getQualifiedName().getPropertyName()).append(";\n");
			result.append(context.indent(2)).append("return this;\n");
			result.append(context.indent(1)).append("}\n");
			if(configurationPropertyInfo instanceof NestedConfigurationPropertyInfo) {
				result.append("\n").append(this.visit((NestedConfigurationPropertyInfo)configurationPropertyInfo, context));
			}
			return result;
		}
		else if(configurationPropertyInfo instanceof NestedConfigurationPropertyInfo) {
			return this.visit((NestedConfigurationPropertyInfo)configurationPropertyInfo, context);
		}
		else if(context.getMode() == GenerationMode.CONFIGURATION_PROPERTY_NAME) {
			return new StringBuilder().append(context.indent(0)).append("\"").append(context.getConfiguration().getQualifiedName().getModuleQName()).append(".").append(configurationPropertyInfo.getQualifiedName().getBeanName()).append("\"");
		}
		else if(context.getMode() == GenerationMode.CONFIGURATION_PROPERTY_CONFIGURER) {
			StringBuilder result = new StringBuilder().append(context.indent(0)).append("results.get(").append(context.getResultIndex().getAndIncrement()).append(").getResult().ifPresent(property -> ").append(context.getConfiguration().getQualifiedName().normalize()).append("_configurator.").append(configurationPropertyInfo.getQualifiedName().getPropertyName()).append("(property.");
			
			if(configurationPropertyInfo.getType().getKind() == TypeKind.ARRAY) {
				result.append("asArrayOf(").append(context.getTypeName(((ArrayType)configurationPropertyInfo.getType()).getComponentType())).append(".class).orElse(null)");
			}
			else if(context.getTypeUtils().isSameType(context.getCollectionType(), context.getTypeUtils().erasure(configurationPropertyInfo.getType()))) {
				result.append("asListOf(").append(context.getTypeName(((DeclaredType)configurationPropertyInfo.getType()).getTypeArguments().get(0))).append(".class).orElse(null)");
			}
			else if(context.getTypeUtils().isSameType(context.getListType(), context.getTypeUtils().erasure(configurationPropertyInfo.getType()))) {
				result.append("asListOf(").append(context.getTypeName(((DeclaredType)configurationPropertyInfo.getType()).getTypeArguments().get(0))).append(".class).orElse(null)");
			}
			else if(context.getTypeUtils().isSameType(context.getSetType(), context.getTypeUtils().erasure(configurationPropertyInfo.getType()))) {
				result.append("asSetOf(").append(context.getTypeName(((DeclaredType)configurationPropertyInfo.getType()).getTypeArguments().get(0))).append(".class).orElse(null)");
			}
			else if(configurationPropertyInfo.getType() instanceof PrimitiveType) {
				// boolean, byte, short, int, long, char, float, and double.
				switch(configurationPropertyInfo.getType().getKind()) {
					case BOOLEAN: result.append("asBoolean().orElseGet(() -> (new boolean[1])[0])");
						break;
					case BYTE: result.append("asByte().orElseGet(() -> (new byte[1])[0])");
						break;
					case SHORT: result.append("asShort().orElseGet(() -> (new short[1])[0])");
						break;
					case INT: result.append("asInteger().orElseGet(() -> (new int[1])[0])");
						break;
					case LONG: result.append("asLong().orElseGet(() -> (new long[1])[0])");
						break;
					case CHAR: result.append("asCharacter().orElseGet(() -> (new char[1])[0])");
						break;
					case FLOAT: result.append("asFloat().orElseGet(() -> (new float[1])[0])");
						break;
					case DOUBLE: result.append("asDouble().orElseGet(() -> (new double[1])[0])");
						break;
					default:
						throw new IllegalStateException("Unsupported primitive type: " + configurationPropertyInfo.getType());
				}
			}
			else {
				result.append("as(").append(context.getTypeName(configurationPropertyInfo.getType())).append(".class).orElse(null)");
			}
			result.append("));");
			return result;
		}
		return new StringBuilder();
	}

	@Override
	public StringBuilder visit(NestedConfigurationPropertyInfo nestedConfigurationPropertyInfo, ConfigurationLoaderClassGenerationContext context) {
		if(context.getMode() == GenerationMode.CONFIGURATION_PROPERTY_NAME) {
			return this.visit(nestedConfigurationPropertyInfo.getConfiguration(), context.withMode(GenerationMode.CONFIGURATION_PROPERTIES));
		}
		else if(context.getMode() == GenerationMode.CONFIGURATION_PROPERTY_CONFIGURER) {
			StringBuilder result = new StringBuilder().append(context.indent(0)).append(context.getConfiguration().getQualifiedName().normalize()).append("_configurator.").append(nestedConfigurationPropertyInfo.getQualifiedName().getPropertyName()).append("(");
			result.append(this.visit(nestedConfigurationPropertyInfo.getConfiguration(), context.withMode(GenerationMode.CONFIGURATION_CONFIGURER)));
			result.append(");");
			return result;
		}
		else if(context.getMode() == GenerationMode.CONFIGURATION_CONFIGURATOR_PROPERTY_INJECTOR) {
			TypeMirror consumerType = context.getTypeUtils().erasure(context.getElementUtils().getTypeElement(Consumer.class.getCanonicalName()).asType());
			
			// Loader class might not be there yet if the nested configuration is defined the current module so we must build the configuration type name without resolving a type element.
//			TypeMirror nestedConfigurationLoaderType = context.getElementUtils().getTypeElement(nestedConfigurationPropertyInfo.getConfiguration().getType().toString() + ConfigurationLoaderClassGenerationContext.CONFIGURATION_LOADER_CLASS_SUFFIX).asType();
			
			String nestedConfigurationLoaderTypeName = context.getConfigurationLoaderTypeName(nestedConfigurationPropertyInfo.getConfiguration());
			
			StringBuilder result = new StringBuilder().append(context.indent(1)).append("public ").append(ConfigurationLoaderClassGenerationContext.CONFIGURATOR_INNER_CLASS).append(" ").append(nestedConfigurationPropertyInfo.getQualifiedName().getPropertyName()).append("(").append(context.getTypeName(consumerType)).append("<").append(nestedConfigurationLoaderTypeName).append(".").append(ConfigurationLoaderClassGenerationContext.CONFIGURATOR_INNER_CLASS).append("> ").append(nestedConfigurationPropertyInfo.getQualifiedName().getPropertyName()).append("_configurer) {\n");
			result.append(context.indent(2)).append("this.").append(nestedConfigurationPropertyInfo.getQualifiedName().getPropertyName()).append(" = () -> ").append(nestedConfigurationLoaderTypeName).append(".load(").append(nestedConfigurationPropertyInfo.getQualifiedName().getPropertyName()).append("_configurer);\n");
			result.append(context.indent(2)).append("return this;\n");
			result.append(context.indent(1)).append("}\n");
			
			return result;
		}
		return new StringBuilder();
	}
}

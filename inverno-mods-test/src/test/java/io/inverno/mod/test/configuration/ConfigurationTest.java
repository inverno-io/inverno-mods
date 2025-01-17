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
package io.inverno.mod.test.configuration;

import io.inverno.mod.test.AbstractInvernoModTest;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.tools.Diagnostic.Kind;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.inverno.test.InvernoCompilationException;
import io.inverno.test.InvernoModuleException;
import io.inverno.test.InvernoModuleLoader;
import io.inverno.test.InvernoModuleProxy;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class ConfigurationTest extends AbstractInvernoModTest {
	
	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
	}
	
	private static final String CLASS_ConfigurationSource = "io.inverno.mod.configuration.ConfigurationSource";
	private static final String CLASS_ConfigurationKey_Parameter = "io.inverno.mod.configuration.ConfigurationKey$Parameter";
	private static final String CLASS_CommandLineConfigurationSource = "io.inverno.mod.configuration.source.CommandLineConfigurationSource";
	private static final String CLASS_MapConfigurationSource = "io.inverno.mod.configuration.source.MapConfigurationSource";
	
	private static final String MODULEA = "io.inverno.mod.test.config.moduleA";
	private static final String MODULEB = "io.inverno.mod.test.config.moduleB";
	private static final String MODULEC = "io.inverno.mod.test.config.moduleC";
	private static final String MODULED = "io.inverno.mod.test.config.moduleD";
	private static final String MODULEE = "io.inverno.mod.test.config.moduleE";
	private static final String MODULEF = "io.inverno.mod.test.config.moduleF";
	private static final String MODULEG = "io.inverno.mod.test.config.moduleG";
	private static final String MODULEH = "io.inverno.mod.test.config.moduleH";
	
	private Object getProperty(Object config, String propertyName) {
		try {
			Method propertyMethod = config.getClass().getMethod(propertyName);
			propertyMethod.setAccessible(true);
			return propertyMethod.invoke(config);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void setProperty(Object builder, String propertyName, Class<?> propertyType, Object value) {
		try {
			Method propertyMethod = builder.getClass().getMethod(propertyName, propertyType);
			propertyMethod.setAccessible(true);
			propertyMethod.invoke(builder, value);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Test
	public void testConfigurationBeanNameConflict() throws IOException, InvernoCompilationException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, ClassNotFoundException {
		this.getInvernoCompiler().cleanModuleTarget();
		try {
			this.getInvernoCompiler().compile(MODULEE);
			Assertions.fail("Should throw a InvernoCompilationException");
		}
		catch(InvernoCompilationException e) {
			List<String> messages = e.getDiagnostics().stream().filter(d -> d.getKind().equals(Kind.ERROR)).map(d -> d.getMessage(Locale.getDefault())).collect(Collectors.toList());
			
			Assertions.assertEquals(2, messages.size());
			
			String configurationTypeNameConflict = "Multiple beans with name configE exist in module io.inverno.mod.test.config.moduleE";

			Assertions.assertEquals(configurationTypeNameConflict, messages.get(0));
			Assertions.assertEquals(configurationTypeNameConflict, messages.get(1));
		}
	}
	
	@Test
	public void testConfiguration_none() throws IOException, InvernoCompilationException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, InvocationTargetException, NoSuchMethodException {
		this.getInvernoCompiler().cleanModuleTarget();
		InvernoModuleLoader moduleLoader = this.getInvernoCompiler().compile(MODULEA);
		InvernoModuleProxy moduleA = moduleLoader.load(MODULEA).build();
		moduleA.start();
		try {
			Object beanA = moduleA.getBean("beanA");
			Assertions.assertNotNull(beanA);
			
			Object beanA_configA = beanA.getClass().getField("configA").get(beanA);
			Assertions.assertNotNull(beanA_configA);
			
			Object beanA_configA_param1 = this.getProperty(beanA_configA, "param1");
			Assertions.assertNull(beanA_configA_param1);
			
			Object beanA_configA_param2 = this.getProperty(beanA_configA, "param2");
			Assertions.assertNotNull(beanA_configA_param2);
			Assertions.assertEquals(Integer.valueOf(53), beanA_configA_param2);
		}
		finally {
			moduleA.stop();
		}
	}
	
	@Test
	public void testConfiguration_override() throws IOException, InvernoCompilationException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
		this.getInvernoCompiler().cleanModuleTarget();
		InvernoModuleLoader moduleLoader = this.getInvernoCompiler().compile(MODULEA);
		
		Class<?> configAClass = moduleLoader.loadClass(MODULEA, "io.inverno.mod.test.config.moduleA.ConfigA");
		ConfigurationInvocationHandler configAHandler = new ConfigurationInvocationHandler(configAClass, Map.of("param1", "abcdef", "param2", 5));
		Object configA = Proxy.newProxyInstance(configAClass.getClassLoader(),
                new Class<?>[] { configAClass },
                configAHandler);
		
		InvernoModuleProxy moduleA = moduleLoader.load(MODULEA).optionalDependency("configA", configAClass, configA).build();
		moduleA.start();
		try {
			Object beanA = moduleA.getBean("beanA");
			Assertions.assertNotNull(beanA);
			
			Object beanA_configA = beanA.getClass().getField("configA").get(beanA);
			Assertions.assertNotNull(beanA_configA);
			
			Object beanA_configA_param1 = this.getProperty(beanA_configA, "param1");
			Assertions.assertNotNull(beanA_configA_param1);
			Assertions.assertEquals("abcdef", beanA_configA_param1);
			
			Object beanA_configA_param2 = this.getProperty(beanA_configA, "param2");
			Assertions.assertNotNull(beanA_configA_param2);
			Assertions.assertEquals(Integer.valueOf(5), beanA_configA_param2);
		}
		finally {
			moduleA.stop();
		}
	}

	@Test
	public void testConfigurationConflict() throws IOException, InvernoCompilationException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
		this.getInvernoCompiler().cleanModuleTarget();
		try {
			this.getInvernoCompiler().compile(MODULEB);
			Assertions.fail("Should throw a InvernoCompilationException");
		}
		catch(InvernoCompilationException e) {
			List<String> messages = e.getDiagnostics().stream().filter(d -> d.getKind().equals(Kind.ERROR)).map(d -> d.getMessage(Locale.getDefault())).collect(Collectors.toList());
			Assertions.assertEquals(1, messages.size());
			
			String configurationBeanConflict = "Multiple beans matching socket io.inverno.mod.test.config.moduleB:beanB:configB were found\n" + 
					"  - io.inverno.mod.test.config.moduleB:configB of type io.inverno.mod.test.config.moduleB.ConfigB\n" +
					"  - io.inverno.mod.test.config.moduleB:configB_Bean of type io.inverno.mod.test.config.moduleB.ConfigB_Bean\n" + 
					"  \n" + 
					"  Consider specifying an explicit wiring in module io.inverno.mod.test.config.moduleB (eg. @io.inverno.core.annotation.Wire(beans=\"io.inverno.mod.test.config.moduleB:configB\", into=\"io.inverno.mod.test.config.moduleB:beanB:configB\") )\n" + 
					"   ";
					
			Assertions.assertEquals(configurationBeanConflict, messages.get(0));
		}
	}
	
	@Test
	public void testNestedConfiguration_none_none() throws IOException, InvernoCompilationException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		this.getInvernoCompiler().cleanModuleTarget();
		InvernoModuleLoader moduleLoader = this.getInvernoCompiler().compile(MODULEA, MODULEC);
		InvernoModuleProxy moduleC = moduleLoader.load(MODULEC).build();
		moduleC.start();
		try {
			Object beanC = moduleC.getBean("beanC");
			Assertions.assertNotNull(beanC);
			
			Object beanC_configC = beanC.getClass().getField("configC").get(beanC);
			Assertions.assertNotNull(beanC_configC);
			
			Object beanC_configA = beanC.getClass().getField("configA").get(beanC);
			Assertions.assertNotNull(beanC_configA);
			
			Object beanC_configC_param1 = this.getProperty(beanC_configC, "param1");
			Assertions.assertNotNull(beanC_configC_param1);
			Assertions.assertEquals("abc", beanC_configC_param1);
			
			Object beanC_configC_configA = this.getProperty(beanC_configC, "configA");
			Assertions.assertNotNull(beanC_configC_configA);
			Assertions.assertEquals(beanC_configA, beanC_configC_configA);
			
			Object beanC_configA_param1 = this.getProperty(beanC_configA, "param1");
			Assertions.assertNull(beanC_configA_param1);
			
			Object beanC_configA_param2 = this.getProperty(beanC_configA, "param2");
			Assertions.assertNotNull(beanC_configA_param2);
			Assertions.assertEquals(Integer.valueOf(53), beanC_configA_param2);
		}
		finally {
			moduleC.stop();
		}
	}
	
	@Test
	public void testNestedConfiguration_impl_null() throws IOException, InvernoCompilationException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, ClassNotFoundException {
		this.getInvernoCompiler().cleanModuleTarget();
		InvernoModuleLoader moduleLoader = this.getInvernoCompiler().compile(MODULEA, MODULEC);
		
		Class<?> configCClass = moduleLoader.loadClass(MODULEC, "io.inverno.mod.test.config.moduleC.ConfigC");
		ConfigurationInvocationHandler configCHandler = new ConfigurationInvocationHandler(configCClass, Map.of("param1", "def"));
		Object configC = Proxy.newProxyInstance(configCClass.getClassLoader(),
                new Class<?>[] { configCClass },
                configCHandler);
		
		try {
			moduleLoader.load(MODULEC).optionalDependency("configC", configCClass, configC).build().start();
			Assertions.fail("Should throw a InvernoModuleException with a NullpointerException");
		} 
		catch (InvernoModuleException e) {
			Assertions.assertTrue(e.getCause() instanceof NullPointerException);
			Assertions.assertEquals("configC.configA", e.getCause().getMessage());
		}
	}
	
	@Test
	public void testNestedConfiguration_impl_impl() throws IOException, InvernoCompilationException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, ClassNotFoundException {
		this.getInvernoCompiler().cleanModuleTarget();
		InvernoModuleLoader moduleLoader = this.getInvernoCompiler().compile(MODULEA, MODULEC);
		
		Class<?> configAClass = moduleLoader.loadClass(MODULEA, "io.inverno.mod.test.config.moduleA.ConfigA");
		ConfigurationInvocationHandler configAHandler = new ConfigurationInvocationHandler(configAClass, Map.of("param1", "abcdef", "param2", 5));
		Object configA = Proxy.newProxyInstance(configAClass.getClassLoader(),
                new Class<?>[] { configAClass },
                configAHandler);
		
		Class<?> configCClass = moduleLoader.loadClass(MODULEC, "io.inverno.mod.test.config.moduleC.ConfigC");
		ConfigurationInvocationHandler configCHandler = new ConfigurationInvocationHandler(configCClass, Map.of("param1", "def", "configA", configA));
		Object configC = Proxy.newProxyInstance(configCClass.getClassLoader(),
                new Class<?>[] { configCClass },
                configCHandler);
		
		InvernoModuleProxy moduleC = moduleLoader.load(MODULEC).optionalDependency("configC", configCClass, configC).build();
		moduleC.start();
		try {
			Object beanC = moduleC.getBean("beanC");
			Assertions.assertNotNull(beanC);
			
			Object beanC_configC = beanC.getClass().getField("configC").get(beanC);
			Assertions.assertNotNull(beanC_configC);
			
			Object beanC_configA = beanC.getClass().getField("configA").get(beanC);
			Assertions.assertNotNull(beanC_configA);
			
			Object beanC_configC_param1 = this.getProperty(beanC_configC, "param1");
			Assertions.assertNotNull(beanC_configC_param1);
			Assertions.assertEquals("def", beanC_configC_param1);
			
			Object beanC_configA_param1 = this.getProperty(beanC_configA, "param1");
			Assertions.assertNotNull(beanC_configA_param1);
			Assertions.assertEquals("abcdef", beanC_configA_param1);
			
			Object beanC_configA_param2 = this.getProperty(beanC_configA, "param2");
			Assertions.assertNotNull(beanC_configA_param2);
			Assertions.assertEquals(Integer.valueOf(5), beanC_configA_param2);
			
			Object beanC_configC_configA = this.getProperty(beanC_configC, "configA");
			Assertions.assertNotNull(beanC_configC_configA);
			Assertions.assertEquals(beanC_configA, beanC_configC_configA);
		}
		finally {
			moduleC.stop();
		}
	}
	
	@Test
	public void testNestedConfiguration_none_default() throws IOException, InvernoCompilationException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		this.getInvernoCompiler().cleanModuleTarget();
		InvernoModuleLoader moduleLoader = this.getInvernoCompiler().compile(MODULEA, MODULED);
		InvernoModuleProxy moduleD = moduleLoader.load(MODULED).build();
		moduleD.start();
		try {
			Object beanD = moduleD.getBean("beanD");
			Assertions.assertNotNull(beanD);
			
			Object beanD_configD = beanD.getClass().getField("configD").get(beanD);
			Assertions.assertNotNull(beanD_configD);
			
			Object beanD_configA = beanD.getClass().getField("configA").get(beanD);
			Assertions.assertNotNull(beanD_configA);
			
			Object beanD_configD_param1 = this.getProperty(beanD_configD, "param1");
			Assertions.assertNotNull(beanD_configD_param1);
			Assertions.assertEquals("abc", beanD_configD_param1);
			
			Object beanD_configD_configA = this.getProperty(beanD_configD, "configA");
			Assertions.assertNotNull(beanD_configD_configA);
			Assertions.assertEquals(beanD_configA, beanD_configD_configA);
			
			Object beanD_configA_param1 = this.getProperty(beanD_configA, "param1");
			Assertions.assertNotNull(beanD_configA_param1);
			Assertions.assertEquals("default param1", beanD_configA_param1);
			
			Object beanD_configA_param2 = this.getProperty(beanD_configA, "param2");
			Assertions.assertNotNull(beanD_configA_param2);
			Assertions.assertEquals(Integer.valueOf(1234), beanD_configA_param2);
		}
		finally {
			moduleD.stop();
		}
	}
	
	@Test
	public void testNestedConfiguration_impl_default() throws IOException, InvernoCompilationException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, ClassNotFoundException {
		this.getInvernoCompiler().cleanModuleTarget();
		InvernoModuleLoader moduleLoader = this.getInvernoCompiler().compile(MODULEA, MODULED);
		
		Class<?> configDClass = moduleLoader.loadClass(MODULED, "io.inverno.mod.test.config.moduleD.ConfigD");
		ConfigurationInvocationHandler configDHandler = new ConfigurationInvocationHandler(configDClass, Map.of("param1", "def"));
		Object configD = Proxy.newProxyInstance(configDClass.getClassLoader(),
                new Class<?>[] { configDClass },
                configDHandler);
		
		InvernoModuleProxy moduleC = moduleLoader.load(MODULED).optionalDependency("configD", configDClass, configD).build();
		moduleC.start();
		try {
			Object beanD = moduleC.getBean("beanD");
			Assertions.assertNotNull(beanD);
			
			Object beanD_configD = beanD.getClass().getField("configD").get(beanD);
			Assertions.assertNotNull(beanD_configD);
			
			Object beanD_configA = beanD.getClass().getField("configA").get(beanD);
			Assertions.assertNotNull(beanD_configA);
			
			Object beanD_configD_param1 = this.getProperty(beanD_configD, "param1");
			Assertions.assertNotNull(beanD_configD_param1);
			Assertions.assertEquals("def", beanD_configD_param1);
			
			Object beanD_configD_configA = this.getProperty(beanD_configD, "configA");
			Assertions.assertNotNull(beanD_configD_configA);
			Assertions.assertEquals(beanD_configA, beanD_configD_configA);
			
			Object beanD_configA_param1 = this.getProperty(beanD_configA, "param1");
			Assertions.assertNotNull(beanD_configA_param1);
			Assertions.assertEquals("default param1", beanD_configA_param1);
			
			Object beanD_configA_param2 = this.getProperty(beanD_configA, "param2");
			Assertions.assertNotNull(beanD_configA_param2);
			Assertions.assertEquals(Integer.valueOf(1234), beanD_configA_param2);
		}
		finally {
			moduleC.stop();
		}
	}
	
	@Test
	public void testLoaderNested_static_load_configurer_configurer() throws IOException, InvernoCompilationException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		this.getInvernoCompiler().cleanModuleTarget();
		InvernoModuleLoader moduleLoader = this.getInvernoCompiler().compile(MODULEA, MODULEC);
		
		Class<?> configCLoaderClass = moduleLoader.loadClass(MODULEC, "io.inverno.mod.test.config.moduleC.ConfigCLoader");
		
		Consumer<Object> configAConfigurer = configurator -> {
			this.setProperty(configurator, "param1", String.class, "ghi");
			this.setProperty(configurator, "param2", int.class, 421);
		};
		
		Consumer<Object> configCConfigurer = configurator -> {
			this.setProperty(configurator, "param1", String.class, "def");
			this.setProperty(configurator, "configA", Consumer.class, configAConfigurer);
		};
		
		Object configC = configCLoaderClass.getMethod("load", Consumer.class).invoke(null, configCConfigurer);
		Assertions.assertNotNull(configC);
		
		Object configC_param1 = this.getProperty(configC, "param1");
		Assertions.assertNotNull(configC_param1);
		Assertions.assertEquals("def", configC_param1);
		
		Object configC_configA = this.getProperty(configC, "configA");
		Assertions.assertNotNull(configC_configA);
		
		Object configC_configA_param1 = this.getProperty(configC_configA, "param1");
		Assertions.assertNotNull(configC_configA_param1);
		Assertions.assertEquals("ghi", configC_configA_param1);
		
		Object configC_configA_param2 = this.getProperty(configC_configA, "param2");
		Assertions.assertNotNull(configC_configA_param2);
		Assertions.assertEquals(Integer.valueOf(421), configC_configA_param2);
	}
	
	@Test
	public void testLoaderNested_static_load_configurer_none() throws IOException, InvernoCompilationException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		this.getInvernoCompiler().cleanModuleTarget();
		InvernoModuleLoader moduleLoader = this.getInvernoCompiler().compile(MODULEA, MODULEC);
		
		Class<?> configCLoaderClass = moduleLoader.loadClass(MODULEC, "io.inverno.mod.test.config.moduleC.ConfigCLoader");
		
		Consumer<Object> configCConfigurer = configurator -> {
			this.setProperty(configurator, "param1", String.class, "def");
		};
		
		Object configC = configCLoaderClass.getMethod("load", Consumer.class).invoke(null, configCConfigurer);
		Assertions.assertNotNull(configC);
		
		Object configC_param1 = this.getProperty(configC, "param1");
		Assertions.assertNotNull(configC_param1);
		Assertions.assertEquals("def", configC_param1);
		
		Object configC_configA = this.getProperty(configC, "configA");
		Assertions.assertNotNull(configC_configA);
		
		Object configC_configA_param1 = this.getProperty(configC_configA, "param1");
		Assertions.assertNull(configC_configA_param1);
		
		Object configC_configA_param2 = this.getProperty(configC_configA, "param2");
		Assertions.assertNotNull(configC_configA_param2);
		Assertions.assertEquals(Integer.valueOf(53), configC_configA_param2);
	}
	
	@Test
	public void testLoaderNested_load() throws IOException, InvernoCompilationException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		this.getInvernoCompiler().cleanModuleTarget();
		InvernoModuleLoader moduleLoader = this.getInvernoCompiler().compile(MODULEA, MODULEC);
		
		Class<?> configCLoaderClass = moduleLoader.loadClass(MODULEC, "io.inverno.mod.test.config.moduleC.ConfigCLoader");
		
		Object configCLoader = configCLoaderClass.getDeclaredConstructor().newInstance();
		
		Method monoBlock = moduleLoader.loadClass("reactor.core", Mono.class.getCanonicalName()).getMethod("block");
		Object configC = monoBlock.invoke(configCLoaderClass.getMethod("load").invoke(configCLoader));
		Assertions.assertNotNull(configC);
		
		Object configC_param1 = this.getProperty(configC, "param1");
		Assertions.assertNotNull(configC_param1);
		Assertions.assertEquals("abc", configC_param1);
		
		Object configC_configA = this.getProperty(configC, "configA");
		Assertions.assertNotNull(configC_configA);
		
		Object configC_configA_param1 = this.getProperty(configC_configA, "param1");
		Assertions.assertNull(configC_configA_param1);
		
		Object configC_configA_param2 = this.getProperty(configC_configA, "param2");
		Assertions.assertNotNull(configC_configA_param2);
		Assertions.assertEquals(Integer.valueOf(53), configC_configA_param2);
	}
	
	public void testLoaderNested_withConfigurer_load() throws IOException, InvernoCompilationException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		this.getInvernoCompiler().cleanModuleTarget();
		InvernoModuleLoader moduleLoader = this.getInvernoCompiler().compile(MODULEA, MODULEC);
		
		Class<?> configCLoaderClass = moduleLoader.loadClass(MODULEC, "io.inverno.mod.test.config.moduleC.ConfigCLoader");
		
		Object configCLoader = configCLoaderClass.getDeclaredConstructor().newInstance();
		
		Consumer<Object> configAConfigurer = configurator -> {
			this.setProperty(configurator, "param1", String.class, "ghi");
			this.setProperty(configurator, "param2", int.class, 421);
		};
		
		Consumer<Object> configCConfigurer = configurator -> {
			this.setProperty(configurator, "param1", String.class, "def");
			this.setProperty(configurator, "configA", Consumer.class, configAConfigurer);
		};
		
		configCLoaderClass.getMethod("withConfigurer", Consumer.class).invoke(configCLoader, configCConfigurer);
		
		Method monoBlock = moduleLoader.loadClass("reactor.core", Mono.class.getCanonicalName()).getMethod("block");
		Object configC = monoBlock.invoke(configCLoaderClass.getMethod("load").invoke(configCLoader));
		Assertions.assertNotNull(configC);
		
		Object configC_param1 = this.getProperty(configC, "param1");
		Assertions.assertNotNull(configC_param1);
		Assertions.assertEquals("def", configC_param1);
		
		Object configC_configA = this.getProperty(configC, "configA");
		Assertions.assertNotNull(configC_configA);
		
		Object configC_configA_param1 = this.getProperty(configC_configA, "param1");
		Assertions.assertNotNull(configC_configA_param1);
		Assertions.assertEquals("ghi", configC_configA_param1);
		
		Object configC_configA_param2 = this.getProperty(configC_configA, "param2");
		Assertions.assertNotNull(configC_configA_param2);
		Assertions.assertEquals(Integer.valueOf(421), configC_configA_param2);
	}
	
	@Test
	public void testLoaderNested_withSource_load() throws IOException, InvernoCompilationException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		this.getInvernoCompiler().cleanModuleTarget();
		InvernoModuleLoader moduleLoader = this.getInvernoCompiler().compile(MODULEA, MODULEC);
		
		Class<?> configCLoaderClass = moduleLoader.loadClass(MODULEC, "io.inverno.mod.test.config.moduleC.ConfigCLoader");
		
		Object configCLoader = configCLoaderClass.getDeclaredConstructor().newInstance();
		
		Map<String, Object> properties = Map.ofEntries(
			Map.entry("io.inverno.mod.test.config.moduleC.configC.param1", "aaaa"),
			Map.entry("io.inverno.mod.test.config.moduleC.configC.configA.param1", "bbbb")
		);
		
		Object source = moduleLoader.loadClass("io.inverno.mod.configuration", CLASS_MapConfigurationSource).getDeclaredConstructor(Map.class).newInstance(properties);
		configCLoaderClass.getMethod("withSource", moduleLoader.loadClass("io.inverno.mod.configuration", CLASS_ConfigurationSource)).invoke(configCLoader, source);
		
		Method monoBlock = moduleLoader.loadClass("reactor.core", "reactor.core.publisher.Mono").getMethod("block");
		Object configC = monoBlock.invoke(configCLoaderClass.getMethod("load").invoke(configCLoader));
		Assertions.assertNotNull(configC);
		
		Object configC_param1 = this.getProperty(configC, "param1");
		Assertions.assertNotNull(configC_param1);
		Assertions.assertEquals("aaaa", configC_param1);
		
		Object configC_configA = this.getProperty(configC, "configA");
		Assertions.assertNotNull(configC_configA);
		
		Object configC_configA_param1 = this.getProperty(configC_configA, "param1");
		Assertions.assertNotNull(configC_configA_param1);
		Assertions.assertEquals("bbbb", configC_configA_param1);
		
		Object configC_configA_param2 = this.getProperty(configC_configA, "param2");
		Assertions.assertNotNull(configC_configA_param2);
		Assertions.assertEquals(Integer.valueOf(53), configC_configA_param2);
	}
	
	@Test
	public void testLoaderNested_withSource_withConfigurer_load() throws IOException, InvernoCompilationException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		this.getInvernoCompiler().cleanModuleTarget();
		InvernoModuleLoader moduleLoader = this.getInvernoCompiler().compile(MODULEA, MODULEC);
		
		Class<?> configCLoaderClass = moduleLoader.loadClass(MODULEC, "io.inverno.mod.test.config.moduleC.ConfigCLoader");
		
		Object configCLoader = configCLoaderClass.getDeclaredConstructor().newInstance();
		
		Map<String, Object> properties = Map.ofEntries(
			Map.entry("io.inverno.mod.test.config.moduleC.configC.param1", "aaaa"),
			Map.entry("io.inverno.mod.test.config.moduleC.configC.configA.param1", "bbbb")
		);
		
		Object source = moduleLoader.loadClass("io.inverno.mod.configuration", CLASS_MapConfigurationSource).getDeclaredConstructor(Map.class).newInstance(properties);
		configCLoaderClass.getMethod("withSource", moduleLoader.loadClass("io.inverno.mod.configuration", CLASS_ConfigurationSource)).invoke(configCLoader, source);
		
		Consumer<Object> configCConfigurer = configurator -> {
			this.setProperty(configurator, "param1", String.class, "def");
		};
		configCLoaderClass.getMethod("withConfigurer", Consumer.class).invoke(configCLoader, configCConfigurer);
		
		Method monoBlock = moduleLoader.loadClass("reactor.core", Mono.class.getCanonicalName()).getMethod("block");
		Object configC = monoBlock.invoke(configCLoaderClass.getMethod("load").invoke(configCLoader));
		Assertions.assertNotNull(configC);
		
		Object configC_param1 = this.getProperty(configC, "param1");
		Assertions.assertNotNull(configC_param1);
		Assertions.assertEquals("def", configC_param1);
		
		Object configC_configA = this.getProperty(configC, "configA");
		Assertions.assertNotNull(configC_configA);
		
		Object configC_configA_param1 = this.getProperty(configC_configA, "param1");
		Assertions.assertNotNull(configC_configA_param1);
		Assertions.assertEquals("bbbb", configC_configA_param1);
		
		Object configC_configA_param2 = this.getProperty(configC_configA, "param2");
		Assertions.assertNotNull(configC_configA_param2);
		Assertions.assertEquals(Integer.valueOf(53), configC_configA_param2);
	}
	
	@Test
	public void testLoaderNested_withSource_withParameters_load() throws IOException, InvernoCompilationException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		this.getInvernoCompiler().cleanModuleTarget();
		InvernoModuleLoader moduleLoader = this.getInvernoCompiler().compile(MODULEA, MODULEC);
		
		Class<?> configCLoaderClass = moduleLoader.loadClass(MODULEC, "io.inverno.mod.test.config.moduleC.ConfigCLoader");
		
		Object configCLoader = configCLoaderClass.getDeclaredConstructor().newInstance();
		
		String[] args = {
			"--io.inverno.mod.test.config.moduleC.configC.param1[env=\"prod\"]=\"aaaa\"",
			"--io.inverno.mod.test.config.moduleC.configC.configA.param1[env=\"dev\"]=\"bbbb\""
		};
			
		Object source = moduleLoader.loadClass("io.inverno.mod.configuration", CLASS_CommandLineConfigurationSource).getDeclaredConstructor(String[].class).newInstance((Object)args);
		configCLoaderClass.getMethod("withSource", moduleLoader.loadClass("io.inverno.mod.configuration", CLASS_ConfigurationSource)).invoke(configCLoader, source);
		
		Class<?> ConfigurationQueryParameterClass = moduleLoader.loadClass("io.inverno.mod.configuration", CLASS_ConfigurationKey_Parameter);
		Object parameter = ConfigurationQueryParameterClass.getMethod("of", String.class, Object.class).invoke(null, "env", "prod");
		
		Object parameters = Array.newInstance(parameter.getClass(), 1);
		Array.set(parameters, 0, parameter);
		
		configCLoaderClass.getMethod("withParameters", parameters.getClass()).invoke(configCLoader, parameters);
		
		Method monoBlock = moduleLoader.loadClass("reactor.core", Mono.class.getCanonicalName()).getMethod("block");
		Object configC = monoBlock.invoke(configCLoaderClass.getMethod("load").invoke(configCLoader));
		Assertions.assertNotNull(configC);
		
		Object configC_param1 = this.getProperty(configC, "param1");
		Assertions.assertNotNull(configC_param1);
		Assertions.assertEquals("aaaa", configC_param1);
		
		Object configC_configA = this.getProperty(configC, "configA");
		Assertions.assertNotNull(configC_configA);
		
		Object configC_configA_param1 = this.getProperty(configC_configA, "param1");
		Assertions.assertNull(configC_configA_param1);
//		Assertions.assertEquals("bbbb", configC_configA_param1);
		
		Object configC_configA_param2 = this.getProperty(configC_configA, "param2");
		Assertions.assertNotNull(configC_configA_param2);
		Assertions.assertEquals(Integer.valueOf(53), configC_configA_param2);
	}
	
	@Test
	public void testConfigurationName() throws IOException, InvernoCompilationException, ClassNotFoundException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		this.getInvernoCompiler().cleanModuleTarget();
		InvernoModuleLoader moduleLoader = this.getInvernoCompiler().compile(MODULEF);
		
		Class<?> configFClass = moduleLoader.loadClass(MODULEF, "io.inverno.mod.test.config.moduleF.ConfigF");
		ConfigurationInvocationHandler configFHandler = new ConfigurationInvocationHandler(configFClass, Map.of("param1", "abcdef", "param2", 5));
		Object configF = Proxy.newProxyInstance(configFClass.getClassLoader(),
                new Class<?>[] { configFClass },
                configFHandler);
		
		InvernoModuleProxy moduleF = moduleLoader.load(MODULEF).optionalDependency("customConfigName", configFClass, configF).build();
		moduleF.start();
		try {
			Object beanF = moduleF.getBean("beanF");
			Assertions.assertNotNull(beanF);
			
			Object beanF_configF = beanF.getClass().getField("configF").get(beanF);
			Assertions.assertNotNull(beanF_configF);
			
			Object beanF_configF_param1 = this.getProperty(beanF_configF, "param1");
			Assertions.assertNotNull(beanF_configF_param1);
			Assertions.assertEquals("abcdef", beanF_configF_param1);
			
			Object beanF_configF_param2 = this.getProperty(beanF_configF, "param2");
			Assertions.assertNotNull(beanF_configF_param2);
			Assertions.assertEquals(Integer.valueOf(5), beanF_configF_param2);
		}
		finally {
			moduleF.stop();
		}
	}
	
	@Test
	public void testConfigurationNoBeanGeneration() throws IOException, InvernoCompilationException, ClassNotFoundException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		this.getInvernoCompiler().cleanModuleTarget();
		InvernoModuleLoader moduleLoader = this.getInvernoCompiler().compile(MODULEG);
		
		InvernoModuleProxy moduleG = moduleLoader.load(MODULEG).build();
		moduleG.start();
		try {
			Object beanG = moduleG.getBean("beanG");
			Assertions.assertNotNull(beanG);
			
			Object beanG_configG = beanG.getClass().getField("configG").get(beanG);
			Assertions.assertNull(beanG_configG);
		}
		finally {
			moduleG.stop();
		}
	}
	
	@Test
	public void testConfigurationNonOverridableBeanGeneration() throws IOException, InvernoCompilationException, ClassNotFoundException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		this.getInvernoCompiler().cleanModuleTarget();
		InvernoModuleLoader moduleLoader = this.getInvernoCompiler().compile(MODULEH);
		
		InvernoModuleProxy moduleH = moduleLoader.load(MODULEH).build();
		moduleH.start();
		try {
			Object beanH = moduleH.getBean("beanH");
			Assertions.assertNotNull(beanH);
			
			Object beanH_configH = beanH.getClass().getField("configH").get(beanH);
			Assertions.assertNotNull(beanH_configH);
			
			Object beanH_configH_param1 = this.getProperty(beanH_configH, "param1");
			Assertions.assertNull(beanH_configH_param1);
			
			Object beanH_configH_param2 = this.getProperty(beanH_configH, "param2");
			Assertions.assertNotNull(beanH_configH_param2);
			Assertions.assertEquals(Integer.valueOf(53), beanH_configH_param2);
		}
		finally {
			moduleH.stop();
		}
		
		try {
			moduleLoader.load(MODULEH).optionalDependency("configH", null);
		}
		catch (IllegalArgumentException e) {
			Assertions.assertEquals("No dependency configH exists on module io.inverno.mod.test.config.moduleH", e.getMessage());
		}
	}
}

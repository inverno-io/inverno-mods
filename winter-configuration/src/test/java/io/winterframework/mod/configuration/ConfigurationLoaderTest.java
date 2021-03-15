package io.winterframework.mod.configuration;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.winterframework.mod.configuration.source.CompositeConfigurationSource;
import io.winterframework.mod.configuration.source.CPropsFileConfigurationSource;

public class ConfigurationLoaderTest {

	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
	}
	
	@Test
	public void testLoad() throws URISyntaxException, MalformedURLException {
		CPropsFileConfigurationSource src = new CPropsFileConfigurationSource(Paths.get(ClassLoader.getSystemResource("test-loader.cprops").toURI()));
		CompositeConfigurationSource comp_src = new CompositeConfigurationSource(List.of(src));
		
		DummyConfiguration conf = ConfigurationLoader.withConfiguration(DummyConfiguration.class).withSource(comp_src).load().block();
		Assertions.assertEquals("default", conf.string_with_default());
		Assertions.assertEquals(new URL("https://localhost:8443/default"), conf.some_url());
		Assertions.assertEquals(1, conf.some_int());
		Assertions.assertNull(conf.undefined_string());
		Assertions.assertArrayEquals(new String[] {"a","b","c","d"}, conf.some_array());
		Assertions.assertNull(conf.some_collection());
		Assertions.assertNull(conf.some_list());
		Assertions.assertNull(conf.some_set());
		NestedConfiguration nestedConf = conf.nested();
		Assertions.assertNull(nestedConf.nested_string());
		NestedNestedConfiguration nestedNestedConf = nestedConf.nested_nested();
		Assertions.assertNull(nestedNestedConf.nested_nested_string());
		Nested2Configuration nested2Conf = conf.nested2();
		Assertions.assertEquals("default_nested2_string", nested2Conf.nested2_string());
		Assertions.assertEquals(0, nested2Conf.nested2_float());
		
		conf = ConfigurationLoader.withConfiguration(DummyConfiguration.class).withSource(comp_src).withParameters("environment", "test").load().block();
		Assertions.assertEquals("default", conf.string_with_default());
		Assertions.assertEquals(new URL("https://localhost:8443/test"), conf.some_url());
		Assertions.assertEquals(2, conf.some_int());
		Assertions.assertNull(conf.undefined_string());
		Assertions.assertArrayEquals(new String[] {"a","b","c"}, conf.some_array());
		Assertions.assertNull(conf.some_collection());
		Assertions.assertEquals(List.of("d","e","f"), conf.some_list());
		Assertions.assertNull(conf.some_set());
		nestedConf = conf.nested();
		Assertions.assertNull(nestedConf.nested_string());
		nestedNestedConf = nestedConf.nested_nested();
		Assertions.assertEquals("nested_nested_test", nestedNestedConf.nested_nested_string());
		nested2Conf = conf.nested2();
		Assertions.assertEquals("nested2_test", nested2Conf.nested2_string());
		Assertions.assertEquals(0, nested2Conf.nested2_float());
		
		conf = ConfigurationLoader.withConfiguration(DummyConfiguration.class).withSource(comp_src).withParameters("node", "node1", "environment", "test").load().block();
		Assertions.assertEquals("node1_test", conf.string_with_default());
		Assertions.assertEquals(new URL("https://localhost:8443/test/node1"), conf.some_url());
		Assertions.assertEquals(3, conf.some_int());
		Assertions.assertNull(conf.undefined_string());
		Assertions.assertArrayEquals(new String[] {"a","b","c"}, conf.some_array());
		Assertions.assertTrue(conf.some_collection().size() == 3 && conf.some_collection().containsAll(List.of("g","h","i")));
		Assertions.assertEquals(List.of("m","n","o"), conf.some_list());
		Assertions.assertEquals(Set.of("j","k","l"), conf.some_set());
		nestedConf = conf.nested();
		Assertions.assertEquals("nested_node1_test", nestedConf.nested_string());
		nestedNestedConf = nestedConf.nested_nested();
		Assertions.assertEquals("nested_nested_node1_test", nestedNestedConf.nested_nested_string());
		nested2Conf = conf.nested2();
		Assertions.assertEquals("nested2_test", nested2Conf.nested2_string());
		Assertions.assertEquals(1.25, nested2Conf.nested2_float());
	}
	
	@Test
	public void testLoadConfigurator() throws URISyntaxException, MalformedURLException {
		CPropsFileConfigurationSource src = new CPropsFileConfigurationSource(Paths.get(ClassLoader.getSystemResource("test-loader.cprops").toURI()));
		CompositeConfigurationSource comp_src = new CompositeConfigurationSource(List.of(src));
		
		DummyConfiguration conf = ConfigurationLoader.withConfigurator(DummyConfigurationBuilder.class, DummyConfigurationBuilder::build).withSource(comp_src).load().block();
		Assertions.assertEquals("default", conf.string_with_default());
		Assertions.assertEquals(new URL("https://localhost:8443/default"), conf.some_url());
		Assertions.assertEquals(1, conf.some_int());
		Assertions.assertNull(conf.undefined_string());
		Assertions.assertArrayEquals(new String[] {"a","b","c","d"}, conf.some_array());
		Assertions.assertNull(conf.some_collection());
		Assertions.assertNull(conf.some_list());
		Assertions.assertNull(conf.some_set());
		NestedConfiguration nestedConf = conf.nested();
		Assertions.assertNull(nestedConf.nested_string());
		NestedNestedConfiguration nestedNestedConf = nestedConf.nested_nested();
		Assertions.assertNull(nestedNestedConf.nested_nested_string());
		Nested2Configuration nested2Conf = conf.nested2();
		Assertions.assertEquals("default_nested2_string", nested2Conf.nested2_string());
		Assertions.assertEquals(0, nested2Conf.nested2_float());
		
		conf = ConfigurationLoader.withConfigurator(DummyConfigurationBuilder.class, DummyConfigurationBuilder::build).withSource(comp_src).withParameters("environment", "test").load().block();
		Assertions.assertEquals("default", conf.string_with_default());
		Assertions.assertEquals(new URL("https://localhost:8443/test"), conf.some_url());
		Assertions.assertEquals(2, conf.some_int());
		Assertions.assertNull(conf.undefined_string());
		Assertions.assertArrayEquals(new String[] {"a","b","c"}, conf.some_array());
		Assertions.assertNull(conf.some_collection());
		Assertions.assertEquals(List.of("d","e","f"), conf.some_list());
		Assertions.assertNull(conf.some_set());
		nestedConf = conf.nested();
		Assertions.assertNull(nestedConf.nested_string());
		nestedNestedConf = nestedConf.nested_nested();
		Assertions.assertEquals("nested_nested_test", nestedNestedConf.nested_nested_string());
		nested2Conf = conf.nested2();
		Assertions.assertEquals("nested2_test", nested2Conf.nested2_string());
		Assertions.assertEquals(0, nested2Conf.nested2_float());
		
		conf = ConfigurationLoader.withConfigurator(DummyConfigurationBuilder.class, DummyConfigurationBuilder::build).withSource(comp_src).withParameters("node", "node1", "environment", "test").load().block();
		Assertions.assertEquals("node1_test", conf.string_with_default());
		Assertions.assertEquals(new URL("https://localhost:8443/test/node1"), conf.some_url());
		Assertions.assertEquals(3, conf.some_int());
		Assertions.assertNull(conf.undefined_string());
		Assertions.assertArrayEquals(new String[] {"a","b","c"}, conf.some_array());
		Assertions.assertTrue(conf.some_collection().size() == 3 && conf.some_collection().containsAll(List.of("g","h","i")));
		Assertions.assertEquals(List.of("m","n","o"), conf.some_list());
		Assertions.assertEquals(Set.of("j","k","l"), conf.some_set());
		nestedConf = conf.nested();
		Assertions.assertEquals("nested_node1_test", nestedConf.nested_string());
		nestedNestedConf = nestedConf.nested_nested();
		Assertions.assertEquals("nested_nested_node1_test", nestedNestedConf.nested_nested_string());
		nested2Conf = conf.nested2();
		Assertions.assertEquals("nested2_test", nested2Conf.nested2_string());
		Assertions.assertEquals(1.25, nested2Conf.nested2_float());
	}
}

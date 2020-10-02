module io.winterframework.mod.configuration {
	requires io.winterframework.core.compiler;
	
	requires jdk.unsupported;
	requires transitive reactor.core;
	requires transitive org.reactivestreams;
	requires static lettuce.core;
	requires org.apache.logging.log4j;
	requires org.apache.commons.text;
	
	exports io.winterframework.mod.configuration;
	exports io.winterframework.mod.configuration.codec;
	exports io.winterframework.mod.configuration.source;
	
	provides io.winterframework.core.compiler.spi.plugin.CompilerPlugin with io.winterframework.mod.configuration.internal.compiler.ConfigurationCompilerPlugin;
}
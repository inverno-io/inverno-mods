module io.winterframework.mod.configuration {
	requires io.winterframework.core.compiler;
	requires transitive reactor.core;
	requires transitive org.reactivestreams;
	
	requires org.apache.logging.log4j;
	
	exports io.winterframework.mod.configuration;
	exports io.winterframework.mod.configuration.converter;
	exports io.winterframework.mod.configuration.source;
	
	provides io.winterframework.core.compiler.spi.plugin.CompilerPlugin with io.winterframework.mod.configuration.internal.compiler.ConfigurationCompilerPlugin;
}
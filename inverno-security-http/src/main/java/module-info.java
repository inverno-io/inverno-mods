@io.inverno.core.annotation.Module(excludes = {"io.inverno.mod.http.server"})
module io.inverno.mod.security.http {
	requires io.inverno.core;
	requires static io.inverno.core.annotation; // for javadoc...
	
	requires io.inverno.mod.base;
    requires transitive io.inverno.mod.security;
    requires transitive io.inverno.mod.http.server;
	requires io.inverno.mod.irt;

    requires transitive reactor.core;
    requires transitive org.reactivestreams;
	requires org.apache.commons.codec;
    requires org.apache.commons.lang3;
    requires org.apache.commons.text;

    exports io.inverno.mod.security.http;
	exports io.inverno.mod.security.http.basic;
	exports io.inverno.mod.security.http.context;
	exports io.inverno.mod.security.http.digest;
	exports io.inverno.mod.security.http.form;
	exports io.inverno.mod.security.http.token;
}

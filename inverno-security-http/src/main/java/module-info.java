module io.inverno.mod.security.http {
    requires transitive io.inverno.mod.security;
    requires transitive io.inverno.mod.http.server;

    requires transitive reactor.core;
    requires transitive org.reactivestreams;
    requires org.apache.commons.lang3;
    requires org.apache.commons.text;

    exports io.inverno.mod.security.http;
}

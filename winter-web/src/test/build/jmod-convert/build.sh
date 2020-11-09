rm -rf target/classes
rm -rf target/out
mkdir -p target/classes
mkdir -p target/out

unzip target/in/netty-common-4.1.51.Final.jar -d target/classes/io.netty.common
unzip target/in/netty-buffer-4.1.51.Final.jar -d target/classes/io.netty.buffer
unzip target/in/netty-resolver-4.1.51.Final.jar -d target/classes/io.netty.resolver
unzip target/in/netty-transport-4.1.51.Final.jar -d target/classes/io.netty.transport
unzip target/in/netty-codec-4.1.51.Final.jar -d target/classes/io.netty.codec
unzip target/in/netty-tcnative-boringssl-static-2.0.31.Final.jar -d target/classes/io.netty.tcnative.boringssl
unzip target/in/netty-handler-4.1.51.Final.jar -d target/classes/io.netty.handler
unzip target/in/netty-codec-http-4.1.51.Final.jar -d target/classes/io.netty.codec.http
unzip target/in/netty-codec-http2-4.1.51.Final.jar -d target/classes/io.netty.codec.http2
unzip target/in/netty-transport-native-unix-common-4.1.51.Final.jar -d target/classes/io.netty.transport.unix.common
unzip target/in/netty-transport-native-epoll-4.1.51.Final-linux-x86_64.jar -d target/classes/io.netty.transport.epoll

$JAVA_HOME/bin/javac -p target/classes -d target/classes/io.netty.common target/src/io.netty.common/module-info.java
$JAVA_HOME/bin/javac -p target/classes -d target/classes/io.netty.buffer target/src/io.netty.buffer/module-info.java
$JAVA_HOME/bin/javac -p target/classes -d target/classes/io.netty.resolver target/src/io.netty.resolver/module-info.java
$JAVA_HOME/bin/javac -p target/classes -d target/classes/io.netty.transport target/src/io.netty.transport/module-info.java
$JAVA_HOME/bin/javac -p target/classes -d target/classes/io.netty.codec target/src/io.netty.codec/module-info.java
$JAVA_HOME/bin/javac -p target/classes -d target/classes/io.netty.tcnative.boringssl target/src/io.netty.tcnative.boringssl/module-info.java
$JAVA_HOME/bin/javac -p target/classes -d target/classes/io.netty.handler target/src/io.netty.handler/module-info.java
$JAVA_HOME/bin/javac -p target/classes -d target/classes/io.netty.codec.http target/src/io.netty.codec.http/module-info.java
$JAVA_HOME/bin/javac -p target/classes -d target/classes/io.netty.codec.http2 target/src/io.netty.codec.http2/module-info.java
$JAVA_HOME/bin/javac -p target/classes -d target/classes/io.netty.transport.unix.common target/src/io.netty.transport.unix.common/module-info.java
$JAVA_HOME/bin/javac -p target/classes -d target/classes/io.netty.transport.epoll target/src/io.netty.transport.epoll/module-info.java

cd target/classes/io.netty.common
zip -r ../../out/netty-common-4.1.51.Final.jar ./*
cd -
cd target/classes/io.netty.buffer
zip -r ../../out/netty-buffer-4.1.51.Final.jar ./*
cd -
cd target/classes/io.netty.resolver 
zip -r ../../out/netty-resolver-4.1.51.Final.jar ./*
cd - 
cd  target/classes/io.netty.transport
zip -r ../../out/netty-transport-4.1.51.Final.jar ./*
cd -
cd target/classes/io.netty.codec
zip -r ../../out/netty-codec-4.1.51.Final.jar ./*
cd -
cd  target/classes/io.netty.handler
zip -r ../../out/netty-handler-4.1.51.Final.jar ./*
cd -
cd target/classes/io.netty.codec.http
zip -r ../../out/netty-codec-http-4.1.51.Final.jar ./*
cd -
cd target/classes/io.netty.codec.http2
zip -r ../../out/netty-codec-http2-4.1.51.Final.jar ./*
cd -
cd target/classes/io.netty.transport.unix.common
zip -r ../../out/netty-transport-native-unix-common-4.1.51.Final.jar ./*
cd -
cd target/classes/io.netty.tcnative.boringssl
zip -r ../../out/netty-tcnative-boringssl-static-2.0.31.Final.jar ./*
cd -
cd target/classes/io.netty.transport.epoll
zip -r ../../out/netty-transport-native-epoll-4.1.51.Final-linux-x86_64.jar ./*
cd -

cp target/in/winter* target/out


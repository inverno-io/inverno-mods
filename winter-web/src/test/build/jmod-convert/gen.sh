rm -rf target/in
rm -rf target/src
mkdir -p target/in
mkdir -p target/src

cd ..
mvn dependency:copy-dependencies -DoutputDirectory=jmod-convert/target/in
cd -

$JAVA_HOME/bin/jdeps --ignore-missing-deps --module-path target/in/ --generate-module-info target/src target/in/netty-common-4.1.51.Final.jar
$JAVA_HOME/bin/jdeps --ignore-missing-deps --module-path target/in/ --generate-module-info target/src target/in/netty-buffer-4.1.51.Final.jar
$JAVA_HOME/bin/jdeps --ignore-missing-deps --module-path target/in/ --generate-module-info target/src target/in/netty-resolver-4.1.51.Final.jar
$JAVA_HOME/bin/jdeps --ignore-missing-deps --module-path target/in/ --generate-module-info target/src target/in/netty-transport-4.1.51.Final.jar
$JAVA_HOME/bin/jdeps --ignore-missing-deps --module-path target/in/ --generate-module-info target/src target/in/netty-codec-4.1.51.Final.jar
$JAVA_HOME/bin/jdeps --ignore-missing-deps --module-path target/in/ --generate-module-info target/src target/in/netty-tcnative-boringssl-static-2.0.31.Final.jar
$JAVA_HOME/bin/jdeps --ignore-missing-deps --module-path target/in/ --generate-module-info target/src target/in/netty-handler-4.1.51.Final.jar
$JAVA_HOME/bin/jdeps --ignore-missing-deps --module-path target/in/ --generate-module-info target/src target/in/netty-codec-http-4.1.51.Final.jar
$JAVA_HOME/bin/jdeps --ignore-missing-deps --module-path target/in/ --generate-module-info target/src target/in/netty-codec-http2-4.1.51.Final.jar
$JAVA_HOME/bin/jdeps --ignore-missing-deps --module-path target/in/ --generate-module-info target/src target/in/netty-transport-native-unix-common-4.1.51.Final.jar
$JAVA_HOME/bin/jdeps --ignore-missing-deps --module-path target/in/ --generate-module-info target/src target/in/netty-transport-native-epoll-4.1.51.Final-linux-x86_64.jar


$JAVA_HOME/bin/jlink -v --compress=2 -p /home/jkuhn/Devel/mvn/winter-http2/target/classes:/home/jkuhn/Devel/mvn/winter-http2/jmod-convert/target/out --add-modules io.winterframework.http2 --launcher winter-http2=io.winterframework.http2/io.winterframework.http2.App --output target/jlink/winter-http2-1.0.0


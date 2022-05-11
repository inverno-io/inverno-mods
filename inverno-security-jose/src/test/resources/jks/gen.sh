${JAVA_HOME}/bin/keytool -importkeystore -destkeystore keystore.jks -srckeystore ../ec/keystore.p12 -srcstoretype pkcs12 -alias ec -storepass password -srcstorepass password
${JAVA_HOME}/bin/keytool -importkeystore -destkeystore keystore.jks -srckeystore ../oct/keystore.jks -srcstoretype jks -alias oct -storepass password -srcstorepass password
${JAVA_HOME}/bin/keytool -importkeystore -destkeystore keystore.jks -srckeystore ../okp/edec/keystore.p12 -srcstoretype pkcs12 -alias edec -storepass password -srcstorepass password
${JAVA_HOME}/bin/keytool -importkeystore -destkeystore keystore.jks -srckeystore ../okp/xec/keystore.p12 -srcstoretype pkcs12 -alias xec -storepass password -srcstorepass password
${JAVA_HOME}/bin/keytool -importkeystore -destkeystore keystore.jks -srckeystore ../rsa/keystore.p12 -srcstoretype pkcs12 -alias rsa -storepass password -srcstorepass password
${JAVA_HOME}/bin/keytool -importkeystore -destkeystore keystore.jks -srckeystore ../rsa/pss/keystore.p12 -srcstoretype pkcs12 -alias rsa_pss -storepass password -srcstorepass password

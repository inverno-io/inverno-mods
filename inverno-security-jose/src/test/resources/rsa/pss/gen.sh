openssl genpkey -algorithm rsa-pss -pkeyopt rsa_keygen_bits:2048 -pkeyopt rsa_keygen_pubexp:65537 -out private_key.pem
openssl pkcs8 -topk8 -outform DER -in private_key.pem -out private_key.der -nocrypt

openssl req -new -x509 -key private_key.pem -subj "/CN=TEST_RSA_PSS" -out cert.pem -days 3650
openssl x509 -in cert.pem -outform DER -out cert.der

openssl pkcs12 -export -name rsa_pss -in cert.pem -inkey private_key.pem -out keystore.p12 -passin pass:password -passout pass:password

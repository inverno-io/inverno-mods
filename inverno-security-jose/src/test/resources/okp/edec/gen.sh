openssl genpkey -algorithm ED25519 > private_key.pem
openssl pkcs8 -topk8 -outform DER -in private_key.pem -out private_key.der -nocrypt

openssl req -new -x509 -key private_key.pem -subj "/CN=TEST_EdEC" -out cert.pem -days 3650
openssl x509 -in cert.pem -outform DER -out cert.der

openssl pkcs12 -export -name edec -in cert.pem -inkey private_key.pem -out keystore.p12 -passin pass:password -passout pass:password

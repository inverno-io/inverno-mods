openssl genrsa -out ca_private_key.pem 2048
openssl req -new -x509 -key ca_private_key.pem -subj "/CN=CA_TEST_XEC" -out ca_cert.pem -days 3650

openssl genpkey -algorithm X448 > private_key.pem
openssl pkey -in private_key.pem -pubout -out public_key.pem

openssl genrsa -out rsakey.pem 2048
openssl req -new -key rsakey.pem -subj "/CN=TEST_XEC" -out rsa.csr

openssl x509 -req -in rsa.csr -CAkey ca_private_key.pem -CA ca_cert.pem -force_pubkey public_key.pem -out cert.pem -CAcreateserial

openssl pkcs8 -topk8 -outform DER -in private_key.pem -out private_key.der -nocrypt
openssl x509 -in cert.pem -outform DER -out cert.der

openssl pkcs12 -export -chain -CAfile ca_cert.pem -name xec -in cert.pem -inkey private_key.pem -out keystore.p12 -passin pass:password -passout pass:password

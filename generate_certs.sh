#!/bin/bash
mkdir -p certs
cd certs

keytool -genkeypair -alias hap-service -keyalg RSA -keysize 2048 -storetype PKCS12 \
  -keystore hap-keystore.p12 -validity 3650 \
  -storepass secretpassword -keypass secretpassword \
  -dname "CN=hap-service, OU=HAP, O=LETI, L=Porto, C=PT"

keytool -export -alias hap-service -keystore hap-keystore.p12 \
  -storepass secretpassword -file hap-public.cer

keytool -import -alias hap-service -keystore hap-truststore.p12 \
  -storepass secretpassword -noprompt -file hap-public.cer

echo "✅ Certificados gerados em /certs!"
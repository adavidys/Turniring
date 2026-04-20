#!/usr/bin/env sh
set -eu

CERT_DIR="${CERT_DIR:-deploy/certs}"
DOMAIN="${1:-localhost}"
DAYS="${DAYS:-825}"

mkdir -p "$CERT_DIR"

for file in tls.crt tls.key; do
  if [ -d "$CERT_DIR/$file" ]; then
    rm -rf "$CERT_DIR/$file"
  fi
done

openssl req \
  -x509 \
  -nodes \
  -newkey rsa:4096 \
  -sha256 \
  -days "$DAYS" \
  -keyout "$CERT_DIR/tls.key" \
  -out "$CERT_DIR/tls.crt" \
  -subj "/CN=${DOMAIN}" \
  -addext "subjectAltName=DNS:${DOMAIN},DNS:localhost,IP:127.0.0.1"

chmod 600 "$CERT_DIR/tls.key"
chmod 644 "$CERT_DIR/tls.crt"

echo "TLS certificate generated in ${CERT_DIR}"

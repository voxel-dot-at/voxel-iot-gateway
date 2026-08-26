#!/bin/bash
#
# Script to set PSK (Pre-Shared Key) for an endpoint in the SQLite security store
#
# Usage: ./set_psk.sh <endpoint> <psk_identity> <hex_key>
# Example: ./set_psk.sh "device1" "device1" "0123456789abcdef0123456789abcdef"
#

DB_FILE="data/security.db"

if [ $# -lt 3 ]; then
    echo "Usage: $0 <endpoint> <psk_identity> <hex_key>"
    echo "Example: $0 device1 device1 0123456789abcdef0123456789abcdef"
    exit 1
fi

ENDPOINT=$1
PSK_IDENTITY=$2
HEX_KEY=$3

# Convert hex string to binary and insert/update in SQLite
sqlite3 "$DB_FILE" <<EOF
UPDATE security_info 
SET psk_identity = '${PSK_IDENTITY}', 
    pre_shared_key = X'${HEX_KEY}',
    use_x509_cert = 0,
    oscore_sender_id = NULL,
    oscore_recipient_id = NULL,
    oscore_master_secret = NULL,
    oscore_aead_algorithm = NULL,
    oscore_hkdf_algorithm = NULL,
    oscore_master_salt = NULL
WHERE endpoint = '${ENDPOINT}';

INSERT OR IGNORE INTO security_info (endpoint, psk_identity, pre_shared_key, use_x509_cert)
VALUES ('${ENDPOINT}', '${PSK_IDENTITY}', X'${HEX_KEY}', 0);
EOF

echo "PSK set for endpoint: ${ENDPOINT}"
echo "PSK Identity: ${PSK_IDENTITY}"
echo "Hex Key: ${HEX_KEY}"

# Verify the update
echo ""
echo "Verification:"
sqlite3 "$DB_FILE" "SELECT endpoint, psk_identity, length(pre_shared_key) as key_length FROM security_info WHERE endpoint = '${ENDPOINT}';"

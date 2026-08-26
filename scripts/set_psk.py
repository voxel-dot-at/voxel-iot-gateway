#!/usr/bin/env python3
"""
Script to set PSK (Pre-Shared Key) for an endpoint in the SQLite security store.

Usage: python3 set_psk.py <endpoint> <psk_identity> <hex_key>
Example: python3 set_psk.py "device1" "device1" "0123456789abcdef0123456789abcdef"
"""

import sqlite3
import sys
import os

DB_FILE = "data/security.db"

def set_psk(endpoint, psk_identity, hex_key):
    """Set PSK for an endpoint."""
    # Convert hex string to bytes
    binary_key = bytes.fromhex(hex_key)
    
    conn = sqlite3.connect(DB_FILE)
    cursor = conn.cursor()
    
    # Update existing record or insert new one
    cursor.execute("""
        INSERT OR REPLACE INTO security_info 
        (endpoint, psk_identity, pre_shared_key, use_x509_cert)
        VALUES (?, ?, ?, 0)
    """, (endpoint, psk_identity, binary_key))
    
    conn.commit()
    conn.close()
    
    print(f"PSK set for endpoint: {endpoint}")
    print(f"PSK Identity: {psk_identity}")
    print(f"Hex Key: {hex_key}")
    print(f"Binary Key length: {len(binary_key)} bytes")

def verify_psk(endpoint):
    """Verify the PSK was set correctly."""
    conn = sqlite3.connect(DB_FILE)
    cursor = conn.cursor()
    
    cursor.execute("""
        SELECT endpoint, psk_identity, length(pre_shared_key) as key_length 
        FROM security_info 
        WHERE endpoint = ?
    """, (endpoint,))
    
    row = cursor.fetchone()
    conn.close()
    
    if row:
        print(f"\nVerification:")
        print(f"Endpoint: {row[0]}")
        print(f"PSK Identity: {row[1]}")
        print(f"Key length: {row[2]} bytes")
    else:
        print(f"\nNo record found for endpoint: {endpoint}")

if __name__ == "__main__":
    if len(sys.argv) < 4:
        print("Usage: python3 set_psk.py <endpoint> <psk_identity> <hex_key>")
        print("Example: python3 set_psk.py device1 device1 0123456789abcdef0123456789abcdef")
        sys.exit(1)
    
    endpoint = sys.argv[1]
    psk_identity = sys.argv[2]
    hex_key = sys.argv[3]
    
    # Remove quotes if present
    hex_key = hex_key.strip('"').strip("'")
    
    set_psk(endpoint, psk_identity, hex_key)
    verify_psk(endpoint)

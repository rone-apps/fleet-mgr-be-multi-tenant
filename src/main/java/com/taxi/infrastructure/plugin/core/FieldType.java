package com.taxi.infrastructure.plugin.core;

/**
 * Types of configuration fields for plugin config.
 */
public enum FieldType {
    STRING,
    INTEGER,
    BOOLEAN,
    URL,
    SECRET,    // Encrypted field (API keys, passwords, tokens)
    JSON
}

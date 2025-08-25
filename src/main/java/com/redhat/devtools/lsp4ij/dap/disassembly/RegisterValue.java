package com.redhat.devtools.lsp4ij.dap.disassembly;

/**
 * Represents a CPU register value.
 */
public record RegisterValue(String name, String value) {}

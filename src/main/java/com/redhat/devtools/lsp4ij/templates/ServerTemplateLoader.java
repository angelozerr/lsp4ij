package com.redhat.devtools.lsp4ij.templates;

public interface ServerTemplateLoader<T extends ServerTemplate> {

    public T load(String content);
}

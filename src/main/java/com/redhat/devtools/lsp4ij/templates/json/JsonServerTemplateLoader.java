package com.redhat.devtools.lsp4ij.templates.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redhat.devtools.lsp4ij.dap.descriptors.templates.DAPTemplate;
import com.redhat.devtools.lsp4ij.dap.descriptors.templates.DAPTemplateDeserializer;
import com.redhat.devtools.lsp4ij.templates.ServerTemplate;
import com.redhat.devtools.lsp4ij.templates.ServerTemplateLoader;
import org.jetbrains.annotations.NotNull;

public abstract class JsonServerTemplateLoader<T extends ServerTemplate> implements ServerTemplateLoader<T> {

    private final Class<T> clazz;
    private final boolean json5;

    public JsonServerTemplateLoader(Class<T> clazz, boolean json5) {
        this.clazz = clazz;
        this.json5 = json5;
    }

    @Override
    public T load(String content) {
        GsonBuilder builder = new GsonBuilder();
        configure(builder);
        return builder
                .create()
                .fromJson(json5 ? Json5Sanitizer.stripCommentsAndTrailingCommas(content) : content, clazz);
    }

    protected abstract void configure(@NotNull GsonBuilder builder);
}

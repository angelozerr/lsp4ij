package com.redhat.devtools.lsp4ij.settings.jsonSchema;

import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider;
import com.jetbrains.jsonSchema.extension.SchemaType;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LSPJsonSchemaFileProvider implements JsonSchemaFileProvider {
    @Override
    public boolean isAvailable(@NotNull VirtualFile file) {
        return false;
    }

    @Override
    public @NotNull @Nls String getName() {
        return "TODO-SCHEMA";
    }

    @Override
    public @Nullable VirtualFile getSchemaFile() {
        return null;
    }

    @Override
    public @NotNull SchemaType getSchemaType() {
        return SchemaType.schema;
    }

    @Override
    public boolean isUserVisible() {
        return false;
    }
}

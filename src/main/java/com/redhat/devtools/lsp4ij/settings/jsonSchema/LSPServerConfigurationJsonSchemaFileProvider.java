package com.redhat.devtools.lsp4ij.settings.jsonSchema;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class LSPServerConfigurationJsonSchemaFileProvider extends AbstractLSPJsonSchemaFileProvider {

    private final int index;
    private LightVirtualFile file;
    private boolean unused;

    public LSPServerConfigurationJsonSchemaFileProvider(int index) {
        super(generateJsonFileName(index));
        this.index = index;
        this.file = new LightVirtualFile("lsp.server." + index +  ".schema.json", "");
        this.unused = true;
    }

    private static String generateJsonFileName(int index) {
        return "lsp.server." + index +  ".schema.json";
    }

    public int getIndex() {
        return index;
    }

    @Override
    public @Nullable VirtualFile getSchemaFile() {
        return file;
    }

    public void reset() {
        unused = true;
        try {
            VfsUtil.saveText(file, "");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setSchemaContent(@NotNull String jsonSchemaContent) {
        try {
            VfsUtil.saveText(file, jsonSchemaContent);
            unused = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isUnused() {
        return unused;
    }

}

package com.redhat.devtools.lsp4ij.settings.jsonSchema;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LSPServerConfigurationJsonSchemaManager {

    public static LSPServerConfigurationJsonSchemaManager getInstance(@NotNull Project project) {
        return project.getService(LSPServerConfigurationJsonSchemaManager.class);
    }

    private final List<LSPServerConfigurationJsonSchemaFileProvider> providers;

    public LSPServerConfigurationJsonSchemaManager() {
        providers = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            providers.add(new LSPServerConfigurationJsonSchemaFileProvider(i));
        }
    }

    public List<LSPServerConfigurationJsonSchemaFileProvider> getProviders() {
        return providers;
    }

    @Nullable
    public Integer getUnusedIndex() {
        var result = getProviders()
                .stream()
                .filter(LSPServerConfigurationJsonSchemaFileProvider::isUnused)
                .map(LSPServerConfigurationJsonSchemaFileProvider::getIndex)
                .findFirst();
        return result.isPresent() ? result.get() :  null;
    }

    public String setJsonSchemaContent(@NotNull Integer index,
                                       @Nullable String jsonSchemaContent) {
        LSPServerConfigurationJsonSchemaFileProvider provider = getProviders().get(index);
        provider.setSchemaContent(jsonSchemaContent);
        return provider.getName();
    }

    public void reset(@NotNull Integer index) {
        LSPServerConfigurationJsonSchemaFileProvider provider = getProviders().get(index);
        provider.reset();
    }
}

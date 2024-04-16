package com.redhat.devtools.lsp4ij.features.navigation;

import com.intellij.model.Pointer;
import com.intellij.navigation.NavigatableSymbol;
import com.intellij.openapi.project.Project;
import com.intellij.platform.backend.navigation.NavigationTarget;
import org.eclipse.lsp4j.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public class LSPNavigatableSymbol implements NavigatableSymbol  {
    private final Location location;

    public LSPNavigatableSymbol(Location location) {
        this.location = location;
    }

    @Override
    public @NotNull Collection<? extends NavigationTarget> getNavigationTargets(@NotNull Project project) {
        return Collections.singletonList(new LSPNavigationTarget(location, project));
    }

    @Override
    public @NotNull Pointer<LSPNavigatableSymbol> createPointer() {
        return Pointer.hardPointer(this);
    }

}

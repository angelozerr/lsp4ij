package com.redhat.devtools.lsp4ij.features.navigation;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.model.Pointer;
import com.intellij.model.Symbol;
import com.intellij.navigation.NavigatableSymbol;
import com.intellij.openapi.project.Project;
import com.intellij.platform.backend.documentation.DocumentationTarget;
import com.intellij.platform.backend.navigation.NavigationRequest;
import com.intellij.platform.backend.navigation.NavigationTarget;
import com.intellij.platform.backend.presentation.TargetPresentation;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class LSPUrlSymbol implements NavigatableSymbol, DocumentationTarget {

    private final @NotNull String url;

    public LSPUrlSymbol(@NotNull String url) {
        this.url = url;
    }

    @Override
    public @NotNull Collection<? extends NavigationTarget> getNavigationTargets(@NotNull Project project) {
        return Collections.singletonList(new NavigationTarget() {
            public Pointer<NavigationTarget> createPointer() {
                return Pointer.hardPointer(this);
            }

            public TargetPresentation computePresentation() {
                return LSPUrlSymbol.this.computePresentation();
            }

            public NavigationRequest navigationRequest() {
                return (new Navigatable() {
                    public boolean canNavigate() {
                        return true;
                    }

                    public void navigate(boolean requestFocus) {
                        BrowserUtil.browse(LSPUrlSymbol.this.url);
                    }
                }).navigationRequest();
            }
        });
    }

    @Override
    public @NotNull Pointer<LSPUrlSymbol> createPointer() {
        return Pointer.hardPointer(this);
    }

    @Override
    public @NotNull TargetPresentation computePresentation() {
        return TargetPresentation.Companion.builder(this.url)
                .icon(AllIcons.General.Web)
                .presentation();
    }
}

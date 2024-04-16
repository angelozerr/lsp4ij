package com.redhat.devtools.lsp4ij.features.navigation;

import com.intellij.model.Pointer;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.backend.navigation.NavigationRequest;
import com.intellij.platform.backend.navigation.NavigationTarget;
import com.intellij.platform.backend.presentation.TargetPresentation;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import org.eclipse.lsp4j.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LSPNavigationTarget implements NavigationTarget {

    private final Location location;
    private final VirtualFile file;
    private final Project project;

    public LSPNavigationTarget(Location location, Project project) {
        this.location = location;
        this.project = project;
        file = LSPIJUtils.findResourceFor(location.getUri());
    }

    @Override
    public @NotNull Pointer<? extends NavigationTarget> createPointer() {
        return Pointer.hardPointer(this);
    }

    @Override
    public @NotNull TargetPresentation computePresentation() {
        return TargetPresentation
                .builder(file.getName())
                .presentation();
    }

    @Override
    public @Nullable NavigationRequest navigationRequest() {
        Document document = LSPIJUtils.getDocument(file);
        int offset = LSPIJUtils.toOffset(location.getRange().getStart(), document);
        return NavigationRequest
                .Companion
                .sourceNavigationRequest(this.project, this.file, offset);
    }
}

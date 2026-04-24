package com.redhat.devtools.lsp4ij.features.navigation;

import com.intellij.model.Pointer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.backend.navigation.NavigationRequest;
import com.intellij.platform.backend.navigation.NavigationTarget;
import com.intellij.platform.backend.presentation.TargetPresentation;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.redhat.devtools.lsp4ij.client.features.FileUriSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LSPFileNavigationTarget implements NavigationTarget {

    private final @Nullable VirtualFile targetFile;
    private final @NotNull Project project;

    public LSPFileNavigationTarget(String target, String tooltip, FileUriSupport fileUriSupport, @NotNull Project project) {
        targetFile = FileUriSupport.findFileByUri(target, fileUriSupport);
        this.project = project;
    }

    @Override
    public @NotNull Pointer<? extends NavigationTarget> createPointer() {
        return Pointer.hardPointer(this);
    }

    @Override
    public @NotNull TargetPresentation computePresentation() {
        return null;
    }

    @Override
    public @Nullable NavigationRequest navigationRequest() {
        if (targetFile == null) {
            return null;
        }
        if (targetFile.isDirectory()) {
            PsiDirectory psiDirectory = PsiManager.getInstance(this.project).findDirectory(targetFile);
            return NavigationRequest.Companion.directoryNavigationRequest(psiDirectory);
        }
        return NavigationRequest.Companion.sourceNavigationRequest(project, targetFile, 0);
    }
}

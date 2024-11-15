package com.redhat.devtools.lsp4ij.client;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.lsp4ij.LanguageServersRegistry;
import org.eclipse.lsp4j.jsonrpc.CompletableFutures;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

public class ProjectIndexingManager implements Disposable {

    final @NotNull Project project;
    boolean dumbIndexing;
    boolean scanning;
    final Set<VirtualFile> filesToRefresh;

    ProjectIndexingManager(@NotNull Project project) {
        this.project = project;
        this.filesToRefresh = new CopyOnWriteArraySet<>();
    }

    public static ProjectIndexingManager getInstance(@NotNull Project project) {
        return project.getService(ProjectIndexingManager.class);
    }

    @Override
    public void dispose() {
    }

    public boolean isIndexing() {
        return scanning || dumbIndexing || DumbService.isDumb(project);
    }

    public CompletableFuture<Void> waitForIndexing() {
        return CompletableFutures.computeAsync(cancelChecker -> {
            while (isIndexing()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                cancelChecker.checkCanceled();
            }
            return null;
        });
    }

    public static boolean isIndexing(@NotNull Project project) {
        return getInstance(project).isIndexing();
    }

    public static CompletableFuture<Void> waitForIndexing(@NotNull Project project) {
        return getInstance(project).waitForIndexing();
    }

    public static AcceptsFile acceptsFile(@Nullable PsiFile file) {
        if (file == null) {
            return AcceptsFile.NOT;
        }
        return acceptsFile(file.getVirtualFile(), file.getProject());
    }

    public static AcceptsFile acceptsFile(@Nullable VirtualFile file,
                                       @NotNull Project project) {
        if(!LanguageServersRegistry.getInstance().isFileSupported(file, project)) {
            return AcceptsFile.NOT;
        }
        ProjectIndexingManager manager = getInstance(project);
        if (manager.isIndexing()) {
            manager.filesToRefresh.add(file);
            return AcceptsFile.OK_AFTER_INDEXING;
        }
        return AcceptsFile.OK_AND_NOW;
    }
}

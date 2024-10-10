package com.redhat.devtools.lsp4ij.client;

import com.intellij.util.indexing.diagnostic.ProjectDumbIndexingHistory;
import com.intellij.util.indexing.diagnostic.ProjectIndexingActivityHistoryListener;
import com.intellij.util.indexing.diagnostic.ProjectScanningHistory;
import com.redhat.devtools.lsp4ij.internal.editor.EditorFeatureManager;
import com.redhat.devtools.lsp4ij.internal.editor.EditorFeatureType;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

public class ProjectIndexingListener implements ProjectIndexingActivityHistoryListener {

    @Override
    public void onStartedDumbIndexing(@NotNull ProjectDumbIndexingHistory history) {
        ProjectIndexingManager.getInstance(history.getProject()).dumbIndexing = true;
    }

    @Override
    public void onFinishedDumbIndexing(@NotNull ProjectDumbIndexingHistory history) {
        ProjectIndexingManager manager = ProjectIndexingManager.getInstance(history.getProject());
        manager.dumbIndexing = false;
        fireIfNeeded(manager);
    }

    @Override
    public void onStartedScanning(@NotNull ProjectScanningHistory history) {
        ProjectIndexingManager.getInstance(history.getProject()).scanning = true;
    }

    @Override
    public void onFinishedScanning(@NotNull ProjectScanningHistory history) {
        ProjectIndexingManager manager = ProjectIndexingManager.getInstance(history.getProject());
        manager.scanning = false;
        fireIfNeeded(manager);
    }

    private void fireIfNeeded(ProjectIndexingManager manager) {
        if (!manager.isIndexing()) {
            while(!manager.filesToRefresh.isEmpty()) {
                var files = new HashSet<>(manager.filesToRefresh);
                for(var file : files) {
                    EditorFeatureManager.getInstance(manager.project)
                            .refreshEditorFeature(file, EditorFeatureType.ALL, true);
                }
                manager.filesToRefresh.removeAll(files);
            }
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.lsp4ij.dap.configurations;

import com.intellij.execution.RunManager;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 *  Debug Adapter Protocol (DAP) file mappings registry.
 */
public class DAPFileMappingRegistry {

    public static DAPFileMappingRegistry getInstance(@NotNull Project project) {
        return project.getService(DAPFileMappingRegistry.class);
    }

    private final @NotNull Project project;

    public DAPFileMappingRegistry(@NotNull Project project) {
        this.project = project;
    }

    public boolean isSupported(@NotNull VirtualFile file) {
        List<RunConfiguration> all = RunManager.getInstance(project).getAllConfigurationsList();
        for (var runConfiguration : all) {
            if (runConfiguration instanceof DAPRunConfiguration dapConfig) {
                if (dapConfig.isFileSupported(file)) {
                    return true;
                }
            }
        }
        return false;
    }

}

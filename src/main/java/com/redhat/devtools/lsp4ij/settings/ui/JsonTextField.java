/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.lsp4ij.settings.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Wrapper for EditorTextField configured for JSON.
 */
public class JsonTextField extends AbstractLanguageTextField {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonTextField.class);

    private static final String JSON_LANGUAGE_NAME = "JSON";
    private static final String DEFAULT_VALUE = "{}";

    public JsonTextField(@NotNull Project project) {
        super(JSON_LANGUAGE_NAME, DEFAULT_VALUE, project);
    }

    /**
     * Sets the editor's filename so that the correct JSON schema will be used for code completion and validation.
     *
     * @param jsonFilename the JSON file name
     */
    public void setJsonFilename(@NotNull String jsonFilename) {
        try {
            VirtualFile file = LSPIJUtils.getFile(editorTextField.getDocument());
            if (file != null) {
                file.rename(this, jsonFilename);
            } else {
                LOGGER.warn("Failed to rename the JSON text field file to '{}'.", jsonFilename);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to configure JSON text field for JSON schema '{}'.", jsonFilename, e);
        }
    }
}

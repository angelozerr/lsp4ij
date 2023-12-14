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
package com.redhat.devtools.lsp4ij.launching.templates;

import com.redhat.devtools.lsp4ij.launching.ServerMappingSettings;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A language server template.
 */
public class LanguageServerTemplate {

    public static final LanguageServerTemplate NONE = new LanguageServerTemplate() {

        @Override
        public String getName() {
            return "None";
        }
    };

    private String name;
    private String runtime;
    private String programArgs;

    private List<ServerMappingSettings> fileTypeMappings;

    private List<ServerMappingSettings> languageMappings;

    private List<String> instructions;

    public String getName() {
        return name;
    }

    public String getRuntime() {
        return runtime;
    }

    public String getProgramArgs() {
        return programArgs;
    }

    public List<ServerMappingSettings> getLanguageMappings() {
        return languageMappings != null ? languageMappings : Collections.emptyList();
    }

    public List<ServerMappingSettings> getFileTypeMappings() {
        return fileTypeMappings != null ? fileTypeMappings : Collections.emptyList();
    }

    public String getDescription() {
        if (instructions == null || instructions.isEmpty()) {
            return null;
        }
        return instructions
                .stream()
                .collect( Collectors.joining("") );
    }
}

/*******************************************************************************
 * Copyright (c) 2024 Red Hat Inc. and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Mitja Leino <mitja.leino@hotmail.com> - Initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.lsp4ij.launching.templates;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.*;
import com.intellij.openapi.util.Pair;
import com.redhat.devtools.lsp4ij.server.definition.launching.UserDefinedLanguageServerDefinition;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LanguageServerDefinitionSerializerTest {
    @Test
    public void testBasicUserDefinedLsSerialization() {
        UserDefinedLanguageServerDefinition lsDef = new UserDefinedLanguageServerDefinition(
                "id",
                "lsName",
                "description",
                "./start.sh",
                Map.of(),
                false,
                "",
                ""
        );

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(UserDefinedLanguageServerDefinition.class, new LanguageServerDefinitionSerializer())
                .create();
        JsonObject json = gson.toJsonTree(lsDef).getAsJsonObject();
        assertEquals("lsName", json.get("name").getAsString());

        String defaultArg = json.get("programArgs").getAsJsonObject().get("default").getAsString();
        assertEquals("./start.sh", defaultArg);
    }

    @Test
    public void testLanguageMapping() {
        UserDefinedLanguageServerDefinition lsDef = new UserDefinedLanguageServerDefinition(
                "id",
                "lsName",
                "description",
                "./start.sh",
                Map.of(),
                false,
                "",
                ""
        );
        lsDef.getLanguageMappings().put(Language.ANY, "testing");

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(UserDefinedLanguageServerDefinition.class, new LanguageServerDefinitionSerializer())
                .create();
        JsonObject json = gson.toJsonTree(lsDef).getAsJsonObject();
        assertEquals("lsName", json.get("name").getAsString());

        JsonObject mapping = json.get("languageMappings").getAsJsonArray().get(0).getAsJsonObject();
        String language = mapping.get("language").getAsString();
        String languageId = mapping.get("languageId").getAsString();
        assertEquals("", language);
        assertEquals("testing", languageId);
    }

    @Test
    public void testFilePatternsMapping() {
        UserDefinedLanguageServerDefinition lsDef = new UserDefinedLanguageServerDefinition(
                "id",
                "lsName",
                "description",
                "./start.sh",
                Map.of(),
                false,
                "",
                ""
        );
        FileNameMatcher fileNameMatcher1 = new ExtensionFileNameMatcher("rs");
        FileNameMatcher fileNameMatcher2 = new WildcardFileNameMatcher("*kt");
        List<FileNameMatcher> fileNameMatcherList = List.of(fileNameMatcher1, fileNameMatcher2);
        lsDef.getFilenameMatcherMappings().add(Pair.create(fileNameMatcherList, "lang"));

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(UserDefinedLanguageServerDefinition.class, new LanguageServerDefinitionSerializer())
                .create();
        JsonObject json = gson.toJsonTree(lsDef).getAsJsonObject();
        assertEquals("lsName", json.get("name").getAsString());

        JsonObject matcher = json.get("fileTypeMappings").getAsJsonArray().get(0).getAsJsonObject();
        JsonArray patterns = matcher.get("fileType").getAsJsonObject().get("patterns").getAsJsonArray();
        String pattern1 = patterns.get(0).getAsString();
        String pattern2 = patterns.get(1).getAsString();
        String langId = matcher.get("languageId").getAsString();

        assertEquals("*.rs", pattern1);
        assertEquals("*kt", pattern2);
        assertEquals("lang", langId);
    }

    @Test
    public void testFileTypeMappings() {
        UserDefinedLanguageServerDefinition lsDef = new UserDefinedLanguageServerDefinition(
                "id",
                "lsName",
                "description",
                "./start.sh",
                Map.of(),
                false,
                "",
                ""
        );
        FileTypeManager fileTypeManager = FileTypeManager.getInstance();
        lsDef.getFileTypeMappings().put(fileTypeManager.getFileTypeByExtension("any"), "Mock");

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(UserDefinedLanguageServerDefinition.class, new LanguageServerDefinitionSerializer())
                .create();
        JsonObject json = gson.toJsonTree(lsDef).getAsJsonObject();
        assertEquals("lsName", json.get("name").getAsString());

        JsonObject matcher = json.get("fileTypeMappings").getAsJsonArray().get(0).getAsJsonObject();
        String fileTypeName = matcher.get("fileType").getAsJsonObject().get("name").getAsString();
        String langId = matcher.get("languageId").getAsString();

        assertEquals("Mock", fileTypeName);
        assertEquals("Mock", langId);
    }
}
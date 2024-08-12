/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.lsp4ij.features.completion;

import com.redhat.devtools.lsp4ij.fixtures.LSPCompletionFixtureTestCase;

/**
 * Completion tests by emulating LSP 'textDocument/completion' responses
 * from the EmmyLua language server
 * which returns completion items without text edit.
 */
public class EmmyLuaPropertiesCompletionTest extends LSPCompletionFixtureTestCase {

    public EmmyLuaPropertiesCompletionTest() {
        super("test.lua");
    }

    // ------------ Completion on property key

    public void testCompletionOnPropertyKeyAtEnd() {
        // 1. Test completion items result
        assertCompletion("test.lua",
                "---@diagnostic disable-next-line : u<caret>", """                
                        [
                          {
                             "label": "unused",
                             "kind": 20,
                             "detail": "diagnostic code",
                             "deprecated": false
                           },
                           {
                             "label": "undefined-global",
                             "kind": 20,
                             "detail": "diagnostic code",
                             "deprecated": false
                           },
                           {
                             "label": "need-import",
                             "kind": 20,
                             "detail": "diagnostic code",
                             "deprecated": false
                           },
                           {
                             "label": "deprecated",
                             "kind": 20,
                             "detail": "diagnostic code",
                             "deprecated": false
                           },
                           {
                             "label": "access-private-member",
                             "kind": 20,
                             "detail": "diagnostic code",
                             "deprecated": false
                           },
                           {
                             "label": "access-protected-member",
                             "kind": 20,
                             "detail": "diagnostic code",
                             "deprecated": false
                           },
                           {
                             "label": "access-package-member",
                             "kind": 20,
                             "detail": "diagnostic code",
                             "deprecated": false
                           },
                           {
                             "label": "missing-parameter",
                             "kind": 20,
                             "detail": "diagnostic code",
                             "deprecated": false
                           },
                           {
                             "label": "no-discard",
                             "kind": 20,
                             "detail": "diagnostic code",
                             "deprecated": false
                           },
                           {
                             "label": "disable-global-define",
                             "kind": 20,
                             "detail": "diagnostic code",
                             "deprecated": false
                           },
                           {
                             "label": "local-const-reassign",
                             "kind": 20,
                             "detail": "diagnostic code",
                             "deprecated": false
                           },
                           {
                             "label": "type-not-found",
                             "kind": 20,
                             "detail": "diagnostic code",
                             "deprecated": false
                           },
                           {
                             "label": "undefined-field",
                             "kind": 20,
                             "detail": "diagnostic code",
                             "deprecated": false
                           },
                           {
                             "label": "inject-field-fail",
                             "kind": 20,
                             "detail": "diagnostic code",
                             "deprecated": false
                           }
                          ]"""
                ,
                "unused",
                "undefined-global");
        // 2. Test new editor content after applying the first completion item
        assertApplyCompletionItem(0, "com.ibm.ws.logging.max.file.size<caret>");
    }

}

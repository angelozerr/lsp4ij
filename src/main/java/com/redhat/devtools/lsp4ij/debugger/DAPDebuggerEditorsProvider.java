package com.redhat.devtools.lsp4ij.debugger;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.EvaluationMode;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.textmate.TextMateFileType;

public class DAPDebuggerEditorsProvider extends XDebuggerEditorsProvider {
    @Override
    public @NotNull FileType getFileType() {
        return TextMateFileType.INSTANCE;
    }

    @Override
    public @NotNull Document createDocument(@NotNull Project project, @NotNull XExpression expression, @Nullable XSourcePosition sourcePosition, @NotNull EvaluationMode mode) {
        String text = expression.getExpression();
        return new DocumentImpl(text);
    }
}

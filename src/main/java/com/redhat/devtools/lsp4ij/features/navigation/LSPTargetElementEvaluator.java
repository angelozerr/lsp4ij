package com.redhat.devtools.lsp4ij.features.navigation;

import com.intellij.codeInsight.TargetElementEvaluatorEx2;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.features.LSPPsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LSPTargetElementEvaluator extends TargetElementEvaluatorEx2 {

    @Override
    public @Nullable PsiElement adjustTargetElement(Editor editor, int offset, int flags, @NotNull PsiElement targetElement) {
        var project = editor.getProject();
        var psiFile = LSPIJUtils.getPsiFile(editor.getVirtualFile(), project);
        return new LSPPsiElement(psiFile, new TextRange(offset, offset));
    }
}

package com.redhat.devtools.lsp4ij.features.codeactions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.lsp4ij.LSPFileSupport;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.LanguageServersRegistry;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeActionTriggerKind;
import org.eclipse.lsp4j.Range;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.redhat.devtools.lsp4ij.internal.CompletableFutures.isDoneNormally;
import static com.redhat.devtools.lsp4ij.internal.CompletableFutures.waitUntilDone;

public class LSPRefactoringMenuGroup extends DefaultActionGroup {

    private static final Logger LOGGER = LoggerFactory.getLogger(LSPRefactoringMenuGroup.class);

    LSPRefactoringMenuGroup() {
        super();
    }
    @Override
    public void update(@NotNull AnActionEvent e) {
        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        //final
        boolean shouldShow = LanguageServersRegistry.getInstance().isFileSupported(file);
        e.getPresentation().setEnabledAndVisible(shouldShow);
        //shouldShow = true;
        if (shouldShow) {
            super.removeAll();
            LSPRefactoringCodeActionSupport codeActionSupport = LSPFileSupport.getSupport(file).getRefactoringCodeActionSupport();
            codeActionSupport.cancel();
            Range range = LSPIJUtils.toRange(file.getTextRange(), LSPIJUtils.getDocument(file.getVirtualFile()));
            CodeActionContext context = new CodeActionContext();
            context.setTriggerKind(CodeActionTriggerKind.Invoked);
            context.setDiagnostics(Collections.emptyList());

            CodeActionParams params = new CodeActionParams(LSPIJUtils.toTextDocumentIdentifier(file.getVirtualFile()), range, context);
            CompletableFuture<List<CodeActionData>> codeActionsFuture = codeActionSupport.getCodeActions(params);
            try {
                waitUntilDone(codeActionsFuture, file);
            } catch (ProcessCanceledException | CancellationException ex) {
                // cancel the LSP requests textDocument/codeAction
                codeActionSupport.cancel();
                return;
            } catch (ExecutionException ex) {
                LOGGER.error("Error while consuming LSP 'textDocument/codeAction' request", e);
                return;
            }

            if (isDoneNormally(codeActionsFuture)) {
                List<CodeActionData> codeActions = codeActionsFuture.getNow(null);
                if (codeActions != null) {
                    for (CodeActionData codeActionData : codeActions) {
                        super.add(new AnAction(codeActionData.documentLink().getRight().getTitle()) {
                            @Override
                            public void actionPerformed(@NotNull AnActionEvent e) {

                            }
                        });
                    }
                }
            }
        }
    }


    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}

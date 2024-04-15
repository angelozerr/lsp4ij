package com.redhat.devtools.lsp4ij.features.linkedEditingRange;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.PsiDocumentManagerBase;
import com.redhat.devtools.lsp4ij.LSPFileSupport;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.LanguageServersRegistry;
import org.eclipse.lsp4j.LinkedEditingRanges;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LSPLinkedEditingRangeSynchronizer implements EditorFactoryListener {

    private static final Key<LSPLinkedEditingRangeSynchronizer.LinkedEditingRangeSynchronizer> SYNCHRONIZER_KEY = Key.create("lsp.linked.editing.range.synchronizer");

    public static class MyEditorFactoryListener implements EditorFactoryListener {
        @Override
        public void editorCreated(@NotNull EditorFactoryEvent event) {
            createSynchronizerFor(event.getEditor());
        }
    }

    private static void createSynchronizerFor(Editor editor) {
        Project project = editor.getProject();
        if (project == null || !(editor instanceof EditorImpl)) {
            return;
        }
        Document document = editor.getDocument();
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null) {
            return;
        }
        PsiFile psiFile = LSPIJUtils.getPsiFile(file, project);
        if (psiFile == null || !LanguageServersRegistry.getInstance().isFileSupported(psiFile)) {
            return;
        }
        LSPLinkedEditingRangeSupport linkedEditingRangeSupport = LSPFileSupport.getSupport(psiFile).getLinkedEditingRangeSupport();
        final TextDocumentIdentifier textDocument = LSPIJUtils.toTextDocumentIdentifier(file);
        new LinkedEditingRangeSynchronizer((EditorImpl)editor, psiFile,textDocument, document, linkedEditingRangeSupport, project)
                .listenForCaretChanges();
    }

    private static final class LinkedEditingRangeSynchronizer implements CaretListener, Disposable {

        private final PsiDocumentManagerBase myDocumentManager;

        private final EditorImpl myEditor;
        private final Project myProject;
        private final TextDocumentIdentifier textDocument;
        private final Document document;

        private final LSPLinkedEditingRangeSupport linkedEditingRangeSupport;
        private final PsiFile psiFile;

        private LinkedEditingRangeSynchronizer(EditorImpl editor, PsiFile psiFile, TextDocumentIdentifier textDocument, Document document, LSPLinkedEditingRangeSupport linkedEditingRangeSupport, Project project) {
            myEditor = editor;
            this.psiFile = psiFile;
            myDocumentManager = (PsiDocumentManagerBase) PsiDocumentManager.getInstance(project);
            myProject = project;
            this.textDocument = textDocument;
            this.document = document;
            this.linkedEditingRangeSupport = linkedEditingRangeSupport;
        }

        private void listenForCaretChanges() {
            Disposer.register(myEditor.getDisposable(), this);
        //    myEditor.getDocument().addDocumentListener(this, this);
            myEditor.getCaretModel().addCaretListener(this, this);
            myEditor.putUserData(SYNCHRONIZER_KEY, this);
          /*  for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                Couple<RangeMarker> markers = getMarkers(caret);
                if (markers != null) {
                    allMarkers.add(markers.first);
                    allMarkers.add(markers.second);
                }
            }*/
        }

        @Override
        public void caretPositionChanged(@NotNull CaretEvent event) {
            linkedEditingRangeSupport.cancel();

            int offset = event.getCaret().getOffset();
            final Position position = LSPIJUtils.toPosition(offset, document);
            LSPLinkedEditingRangeParams params = new LSPLinkedEditingRangeParams(textDocument,position, offset, document);
            linkedEditingRangeSupport.getLinkedEditingRanges(params)
                    .thenApply(ranges -> {
                        if (ranges.isEmpty()) {
                            return null;
                        }
                        LinkedEditingRanges result = ranges.get(0);
                        List<TextRange> textRanges = result.getRanges()
                                .stream()
                                .map(r -> LSPIJUtils.toTextRange(r, document))
                                .toList();
                        LSPInplaceRenamer.rename(myEditor, psiFile, textRanges);
                        return null;
                    });
        }

        @Override
        public void caretAdded(@NotNull CaretEvent event) {
            CaretListener.super.caretAdded(event);
        }

        @Override
        public void caretRemoved(@NotNull CaretEvent event) {
            CaretListener.super.caretRemoved(event);
        }


        @Override
        public void dispose() {
            myEditor.putUserData(SYNCHRONIZER_KEY, null);
        }

    }
}

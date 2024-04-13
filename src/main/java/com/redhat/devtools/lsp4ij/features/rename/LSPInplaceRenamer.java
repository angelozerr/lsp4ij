// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.redhat.devtools.lsp4ij.features.rename;

import com.intellij.codeInsight.highlighting.HighlightManager;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateBuilderImpl;
import com.intellij.codeInsight.template.TemplateEditingAdapter;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.ConstantNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.util.containers.Stack;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.features.LSPPsiElement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class LSPInplaceRenamer {
  private static final @NonNls String PRIMARY_VARIABLE_NAME = "PrimaryVariable";
  private static final @NonNls String OTHER_VARIABLE_NAME = "OtherVariable";

  private final Editor myEditor;

  private static final Stack<LSPInplaceRenamer> ourRenamersStack = new Stack<>();
  private ArrayList<RangeHighlighter> myHighlighters;

  private LSPInplaceRenamer(final @NotNull Editor editor) {
    myEditor = editor;
  }

  public static void rename(final @NotNull Editor editor, final @NotNull PsiFile file, @NotNull List<TextRange> textRanges) {
    if (!ourRenamersStack.isEmpty()) {
      ourRenamersStack.peek().finish();
    }

    final LSPInplaceRenamer renamer = new LSPInplaceRenamer(editor);
    ourRenamersStack.push(renamer);
    renamer.rename(file, textRanges);
  }

  private void rename(@NotNull PsiFile file, final @NotNull List<TextRange> highlightRanges) {
    if (highlightRanges.size() <= 1) {
      return;
    }

    final Project project = myEditor.getProject();
    if (project != null) {


      /*if (!CommonRefactoringUtil.checkReadOnlyStatus(project, tag)) {
        return;
      }*/

      myHighlighters = new ArrayList<>();

      CommandProcessor.getInstance().executeCommand(project, () -> ApplicationManager.getApplication().runWriteAction(() -> {
        final int offset = myEditor.getCaretModel().getOffset();
        int textOffset = highlightRanges.get(0).getStartOffset();
        myEditor.getCaretModel().moveToOffset(textOffset);

        final Template t = buildTemplate(file, highlightRanges);
        TemplateManager.getInstance(project).startTemplate(myEditor, t, new TemplateEditingAdapter() {
          @Override
          public void templateFinished(final @NotNull Template template, boolean brokenOff) {
            finish();
          }

          @Override
          public void templateCancelled(final Template template) {
            finish();
          }
        }, (variableName, value) -> value.length() == 0 || value.charAt(value.length() - 1) != ' ');

        // restore old offset
        myEditor.getCaretModel().moveToOffset(offset);

        addHighlights(highlightRanges, myEditor, myHighlighters);
      }), RefactoringBundle.message("rename.title"), null);
    }
  }

  private void finish() {
    ourRenamersStack.pop();

    if (myHighlighters != null) {
      Project project = myEditor.getProject();
      if (project != null && !project.isDisposed()) {
        final HighlightManager highlightManager = HighlightManager.getInstance(project);
        for (final RangeHighlighter highlighter : myHighlighters) {
          highlightManager.removeSegmentHighlighter(myEditor, highlighter);
        }
      }
    }
  }

  private static Template buildTemplate(final @NotNull PsiFile file, final @NotNull List<TextRange> highlightRanges) {
    var start = highlightRanges.get(0);
    var end = highlightRanges.get(highlightRanges.size()-1);
    LSPPsiElement coveredElement = new LSPPsiElement(file, new TextRange(start.getStartOffset(), end.getEndOffset()));
    final TemplateBuilderImpl builder = new TemplateBuilderImpl(coveredElement);
    var document = LSPIJUtils.getDocument(file.getVirtualFile());
    for (int i = 0; i < highlightRanges.size(); i++) {
      var textRange = highlightRanges.get(i);
      if (i == 0) {
        LSPPsiElement element = new LSPPsiElement(file, textRange);
        String text = document.getText(textRange);
        builder.replaceElement(element, PRIMARY_VARIABLE_NAME, new ConstantNode(text), true);
      } else {
        LSPPsiElement element = new LSPPsiElement(file, textRange);
        builder.replaceElement(element, OTHER_VARIABLE_NAME, PRIMARY_VARIABLE_NAME, false);
      }
    }
    /*final ASTNode selected = pair.first;
    final ASTNode other = pair.second;

    builder.replaceElement(selected.getPsi(), PRIMARY_VARIABLE_NAME, new ConstantNode(selected.getText()), true);

    if (other != null) {
      builder.replaceElement(other.getPsi(), OTHER_VARIABLE_NAME, PRIMARY_VARIABLE_NAME, false);
    }*/

    return builder.buildInlineTemplate();
  }

  private static void addHighlights(List<? extends TextRange> ranges, Editor editor, ArrayList<RangeHighlighter> highlighters) {
    EditorColorsManager colorsManager = EditorColorsManager.getInstance();
    final TextAttributes attributes = colorsManager.getGlobalScheme().getAttributes(EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES);

    final HighlightManager highlightManager = HighlightManager.getInstance(editor.getProject());
    for (final TextRange range : ranges) {
      highlightManager.addOccurrenceHighlight(editor, range.getStartOffset(), range.getEndOffset(), attributes, 0, highlighters, null);
    }

    for (RangeHighlighter highlighter : highlighters) {
      highlighter.setGreedyToLeft(true);
      highlighter.setGreedyToRight(true);
    }
  }

}
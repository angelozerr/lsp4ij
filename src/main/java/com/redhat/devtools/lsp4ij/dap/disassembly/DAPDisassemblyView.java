package com.redhat.devtools.lsp4ij.dap.disassembly;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.redhat.devtools.lsp4ij.dap.client.DAPClient;
import org.eclipse.lsp4j.debug.DisassembleArguments;
import org.eclipse.lsp4j.debug.DisassembledInstruction;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Editor-based disassembly view with CLion-like visuals:
 * - PC highlight
 * - Folding
 * - Breakpoints (gutter icons)
 * - Inline / optimized annotations
 * - Split view with code source
 * - Registers panel
 * - Tooltips
 */
public class DAPDisassemblyView {
    private final Editor disassemblyEditor;
    private final Project project;
    private final JPanel registersPanel;
    private int currentPCLine = -1;
    private final List<DisassembledInstruction> instructions = new ArrayList<>();
    private Runnable loadMoreCallback;
    private Editor sourceEditor;

    private static final Key<Boolean> PC_KEY = Key.create("PC_HIGHLIGHT");

    public DAPDisassemblyView(Project project) {
        this.project = project;
        this.registersPanel = new JPanel(new BorderLayout());

        Document doc = EditorFactory.getInstance().createDocument("");
        this.disassemblyEditor = EditorFactory.getInstance().createViewer(doc, project);

        disassemblyEditor.getSettings().setLineNumbersShown(true);
        disassemblyEditor.getSettings().setFoldingOutlineShown(true);
        disassemblyEditor.getSettings().setCaretRowShown(false);
        disassemblyEditor.getSettings().setRightMarginShown(false);

        setupScrollListener();
        setupTooltipListener();
    }

    private void setupScrollListener() {
        disassemblyEditor.getScrollingModel().addVisibleAreaListener(e -> {
            int lastLine = (disassemblyEditor.getScrollingModel().getVisibleArea().y
                    + disassemblyEditor.getScrollingModel().getVisibleArea().height)
                    / disassemblyEditor.getLineHeight();
            if (lastLine > instructions.size() - 5 && loadMoreCallback != null) {
                loadMoreCallback.run();
            }
        });
    }

    private void setupTooltipListener() {
        disassemblyEditor.addEditorMouseMotionListener(new EditorMouseMotionListener() {
            @Override
            public void mouseMoved(EditorMouseEvent e) {
                LogicalPosition pos = disassemblyEditor.xyToLogicalPosition(e.getMouseEvent().getPoint());
                int line = pos.line;
                if (line >= 0 && line < instructions.size()) {
                    DisassembledInstruction instr = instructions.get(line);
                    String tooltipText = String.format("<html>%s<br/>%s</html>",
                            instr.getInstruction(), instr.getAddress());
                    HintManager.getInstance().showInformationHint(disassemblyEditor, tooltipText);
                }
            }
        });
    }

    public void setLoadMoreCallback(Runnable callback) {
        this.loadMoreCallback = callback;
    }

    public JComponent getComponent() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, disassemblyEditor.getComponent(), registersPanel);
        split.setDividerLocation(700);
        return split;
    }

    public void setInstructions(List<DisassembledInstruction> newInstructions) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            instructions.clear();
            instructions.addAll(newInstructions);
            refreshDocument();
        });
    }


    public void refreshInstructions(String memoryRef, DAPClient client) {
        var server = client.getDebugProtocolServer();
        if (server == null) {
            return;
        }
        DisassembleArguments args = new DisassembleArguments();
        args.setMemoryReference(memoryRef);
        server.disassemble(args)
                .thenApply(response -> {
                    if (response != null) {
                        setInstructions(Arrays.asList(response.getInstructions()));
                    }
                    return null;
                });
    }

    public void appendInstructions(List<DisassembledInstruction> moreInstructions) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            instructions.addAll(moreInstructions);
            refreshDocument();
        });
    }

    private void refreshDocument() {
        Document doc = disassemblyEditor.getDocument();
        doc.setText("");
        MarkupModel markup = disassemblyEditor.getMarkupModel();
        markup.removeAllHighlighters();

        for (int i = 0; i < instructions.size(); i++) {
            DisassembledInstruction instr = instructions.get(i);
            int startOffset = doc.getTextLength();
            String lineText = String.format("%s: %s %s%n",
                    instr.getAddress(),
                    instr.getInstructionBytes(),
                    instr.getInstruction());
            doc.insertString(doc.getTextLength(), lineText);

            TextAttributes attrs = new TextAttributes();
            //if (instr.isOptimized()) attrs.setForegroundColor(Color.GRAY);
            //if (instr.isInline()) attrs.setFontType(Font.ITALIC);
            markup.addRangeHighlighter(startOffset, doc.getTextLength(),
                    HighlighterLayer.ADDITIONAL_SYNTAX, attrs, HighlighterTargetArea.EXACT_RANGE);

            if (i % 10 == 0 && i + 10 < instructions.size()) {
                // markup.addFoldRegion(doc.getLineStartOffset(i), doc.getLineEndOffset(i + 9), "...");
            }
        }
    }

    public void highlightPC(int line) {
        MarkupModel markup = disassemblyEditor.getMarkupModel();
        if (currentPCLine >= 0) {
            for (RangeHighlighter hl : markup.getAllHighlighters()) {
                if (hl.getUserData(PC_KEY) != null) markup.removeHighlighter(hl);
            }
        }

        TextAttributes attrs = new TextAttributes(Color.YELLOW, null, null, null, Font.BOLD);
        RangeHighlighter hl = markup.addLineHighlighter(line, HighlighterLayer.SELECTION - 1, attrs);
        hl.putUserData(PC_KEY, Boolean.TRUE);

        disassemblyEditor.getScrollingModel().scrollTo(new LogicalPosition(line, 0), ScrollType.CENTER);

        if (sourceEditor != null && line >= 0 && line < instructions.size()) {
            DisassembledInstruction instr = instructions.get(line);
            // TODO: source
            /*if (instr.sourcePath() != null) {
                var vfile = LocalFileSystem.getInstance().findFileByPath(instr.sourcePath());
                if (vfile != null) {
                    FileEditorManager.getInstance(project).openFile(vfile, true);
                    sourceEditor.getScrollingModel().scrollTo(new LogicalPosition(instr.getLine(), 0),
                            ScrollType.CENTER);
                }
            }*/
        }

        currentPCLine = line;
    }

    public void addBreakpointIcon(int line, Icon icon) {
        MarkupModel markup = disassemblyEditor.getMarkupModel();
        RangeHighlighter hl = markup.addLineHighlighter(line, HighlighterLayer.ERROR + 1, null);
        GutterIconRenderer renderer = new GutterIconRenderer() {
            @Override
            public @NotNull Icon getIcon() {
                return icon;
            }

            @Override
            public boolean equals(Object obj) {
                return this == obj;
            }

            @Override
            public int hashCode() {
                return System.identityHashCode(this);
            }
        };
        hl.setGutterIconRenderer(renderer);
    }

    public void updateRegisters(List<RegisterValue> registers) {
        registersPanel.removeAll();
        JPanel panel = new JPanel(new GridLayout(registers.size(), 2));
        for (RegisterValue reg : registers) {
            panel.add(new JLabel(reg.name()));
            panel.add(new JLabel(reg.value()));
        }
        registersPanel.add(panel, BorderLayout.CENTER);
        registersPanel.revalidate();
        registersPanel.repaint();
    }

    public void setSourceEditor(Editor editor) {
        this.sourceEditor = editor;
    }

    public void stepOverInstruction() {
        highlightStepLine(Math.min(currentPCLine + 1, instructions.size() - 1));
    }

    public void stepIntoInstruction() {
        highlightStepLine(Math.min(currentPCLine + 1, instructions.size() - 1));
    }

    private void highlightStepLine(int line) {
        MarkupModel markup = disassemblyEditor.getMarkupModel();
        TextAttributes flashAttrs = new TextAttributes(Color.ORANGE, null, null, null, Font.BOLD);
        RangeHighlighter hl = markup.addLineHighlighter(line, HighlighterLayer.SELECTION + 1, flashAttrs);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
            ApplicationManager.getApplication().invokeLater(() -> markup.removeHighlighter(hl));
        });
        highlightPC(line);
    }

}
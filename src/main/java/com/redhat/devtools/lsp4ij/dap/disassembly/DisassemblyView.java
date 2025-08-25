package com.redhat.devtools.lsp4ij.dap.disassembly;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowManager;
import com.redhat.devtools.lsp4ij.ServerMessageHandler;
import com.redhat.devtools.lsp4ij.dap.client.DAPClient;
import org.eclipse.lsp4j.debug.DisassembleArguments;
import org.eclipse.lsp4j.debug.DisassembledInstruction;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DisassemblyView is responsible for displaying the actual disassembly content.
 * It is embedded inside the DisassemblyFileEditor, so that it appears as a tab
 * in the editor area.
 */
public class DisassemblyView {

    private static final int NUM_INSTRUCTIONS_TO_LOAD = 50;

    private final Editor disassemblyEditor;
    private final Project project;
    private final JPanel registersPanel;
    private int currentPCLine = -1;
    private final List<DisassembledInstruction> instructions = new ArrayList<>();
    private Runnable loadMoreCallback;
    private Editor sourceEditor;

    private static final Key<Boolean> PC_KEY = Key.create("PC_HIGHLIGHT");
    public static final Key<DisassemblyView> VIEW_KEY = Key.create("disassembly.view");

    public static DisassemblyView getDisassemblyView(@NotNull Project project) {
        var file = DisassemblyFile.getInstance(project);
        Document doc = FileDocumentManager.getInstance().getDocument(file);
        if (doc == null) return null;

        Editor[] editors = EditorFactory.getInstance().getEditors(doc, project);
        for (Editor editor : editors) {
            FileEditor fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(file);
            if (fileEditor instanceof DisassemblyFileEditor disassemblyEditor) {
                return disassemblyEditor.getView();
            }
        }
        return null;
    }

    public DisassemblyView(DisassemblyFile file, Project project) {
        this.project = project;
        this.registersPanel = new JPanel(new BorderLayout());

        var doc = FileDocumentManager.getInstance().getDocument(file);
        this.disassemblyEditor = EditorFactory.getInstance().createEditor(doc, project, file, true, EditorKind.MAIN_EDITOR);

        disassemblyEditor.getSettings().setLineNumbersShown(true);
        disassemblyEditor.getSettings().setFoldingOutlineShown(true);
        disassemblyEditor.getSettings().setCaretRowShown(false);
        disassemblyEditor.getSettings().setRightMarginShown(false);
        disassemblyEditor.getSettings().setGutterIconsShown(true);

        registersPanel.add(disassemblyEditor.getComponent());

        setupScrollListener();
        //setupTooltipListener();
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

    /*private void setupTooltipListener() {
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
    }*/

    public void setLoadMoreCallback(Runnable callback) {
        this.loadMoreCallback = callback;
    }

    public JComponent getComponent() {
        return registersPanel;
        /*JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, disassemblyEditor.getComponent(), registersPanel);
        split.setDividerLocation(700);
        return split;*/
    }

    public void setInstructions(List<DisassembledInstruction> newInstructions) {
        ApplicationManager.getApplication().invokeLater(() -> {
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
        args.setInstructionCount(NUM_INSTRUCTIONS_TO_LOAD);
        server.disassemble(args)
                .handle((response, error) -> {
                    if (error != null) {
                        String errorMessage = error.getMessage();
                        if (error instanceof ResponseErrorException dapError && dapError.getResponseError() != null) {
                            errorMessage = dapError.getResponseError().getMessage();
                        }
                        showError(errorMessage, project);
                    }
                    if (response != null) {
                        setInstructions(Arrays.asList(response.getInstructions()));
                    }
                    return null;
                });
    }

    private static void showError(@NotNull String errorMessage, @NotNull Project project) {
        Notification notification = new Notification(ServerMessageHandler.LSP_WINDOW_SHOW_MESSAGE_GROUP_ID,
                "DAP Disassembly error",
                errorMessage,
                NotificationType.ERROR);
        //notification.setSubtitle(subtitle);
        Notifications.Bus.notify(notification, project);

    }

/*    public void appendInstructions(List<DisassembledInstruction> moreInstructions) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            instructions.addAll(moreInstructions);
            refreshDocument();
        });
    }
*/

    /**
     * Refreshes the entire disassembly document with the current instructions.
     * Performs batch updates for better performance.
     */
    private void refreshDocument() {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            Document doc = disassemblyEditor.getDocument();
            MarkupModel markup = disassemblyEditor.getMarkupModel();
            FoldingModel foldingModel = disassemblyEditor.getFoldingModel();

            // Save caret and scroll to restore later
            int caretOffset = disassemblyEditor.getCaretModel().getOffset();
            int scrollOffset = disassemblyEditor.getScrollingModel().getVerticalScrollOffset();

            // Clear document and highlighters
            doc.setText("");
            markup.removeAllHighlighters();

            // Clear existing fold regions
            foldingModel.runBatchFoldingOperation(() -> {
                for (FoldRegion region : foldingModel.getAllFoldRegions()) {
                    foldingModel.removeFoldRegion(region);
                }
            });

            // Build text in batch
            StringBuilder sb = new StringBuilder();
            for (DisassembledInstruction instr : instructions) {
                sb.append(String.format("%s: %s %s\n",
                        instr.getAddress(),
                        instr.getInstructionBytes(),
                        instr.getInstruction()));
            }
            String fullText = sb.toString();
            doc.insertString(0, fullText);

            // Add highlighters for each line
            int offset = 0;
            for (DisassembledInstruction instr : instructions) {
                int lineLength = instr.getAddress().length() + instr.getInstructionBytes().length()
                        + instr.getInstruction().length() + 3; // ": " + spaces + newline
                TextAttributes attrs = new TextAttributes();
                markup.addRangeHighlighter(offset, offset + lineLength,
                        HighlighterLayer.ADDITIONAL_SYNTAX, attrs, HighlighterTargetArea.EXACT_RANGE);
                offset += lineLength + 1; // newline
            }

            // Optional folding: fold every 10 lines
            foldingModel.runBatchFoldingOperation(() -> {
                for (int i = 0; i < instructions.size(); i += 10) {
                    if (i + 9 < instructions.size()) {
                        int startOffset = doc.getLineStartOffset(i);
                        int endOffset = doc.getLineEndOffset(i + 9);
                        foldingModel.addFoldRegion(startOffset, endOffset, "...");
                    }
                }
            });


            // Restore caret and scroll
            disassemblyEditor.getCaretModel().moveToOffset(caretOffset);
            disassemblyEditor.getScrollingModel().scrollVertically(scrollOffset);
        });
    }

    /**
     * Appends new instructions to the disassembly document in a lazy and performant way.
     * Does not force repaint; IntelliJ automatically refreshes the Viewer.
     */
    public void appendInstructions(List<DisassembledInstruction> moreInstructions) {
        if (moreInstructions.isEmpty()) return;

        // Build text for all new instructions at once
        StringBuilder sb = new StringBuilder();
        for (DisassembledInstruction instr : moreInstructions) {
            sb.append(String.format("%s: %s %s\n",
                    instr.getAddress(),
                    instr.getInstructionBytes(),
                    instr.getInstruction()));
        }
        String newText = sb.toString();

        Document doc = disassemblyEditor.getDocument();
        int startOffset = doc.getTextLength();

        // Update document and add highlighters in a single batch
        WriteCommandAction.runWriteCommandAction(project, () -> {
            // Append new text
            doc.insertString(startOffset, newText);

            // Add highlighters for each new line
            MarkupModel markup = disassemblyEditor.getMarkupModel();
            int offset = startOffset;
            for (DisassembledInstruction instr : moreInstructions) {
                int lineLength = instr.getAddress().length() + instr.getInstructionBytes().length()
                        + instr.getInstruction().length() + 3; // ": " + spaces + newline
                TextAttributes attrs = new TextAttributes();
                markup.addRangeHighlighter(offset, offset + lineLength,
                        HighlighterLayer.ADDITIONAL_SYNTAX, attrs, HighlighterTargetArea.EXACT_RANGE);
                offset += lineLength + 1; // newline
            }

            // Update internal list
            instructions.addAll(moreInstructions);
        });
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

    public @Nullable JComponent getPreferredFocusedComponent() {
        return disassemblyEditor.getComponent();
    }
    
    public DisassemblyFile getFile() {
        return (DisassemblyFile) disassemblyEditor.getVirtualFile();
    }
}
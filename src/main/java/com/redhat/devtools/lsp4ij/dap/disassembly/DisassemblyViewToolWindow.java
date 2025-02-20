package com.redhat.devtools.lsp4ij.dap.disassembly;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XDebuggerTreeNodeHyperlink;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XValueChildrenList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class DisassemblyViewToolWindow implements ToolWindowFactory {
    private static DisassemblyViewToolWindow instance;
    private JBList<String> addressList;
    private JBTextArea disassemblyTextArea;
    private DefaultListModel<String> addressModel;

    public DisassemblyViewToolWindow() {
        instance = this;
    }

    public static DisassemblyViewToolWindow getInstance() {
        return instance;
    }

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        SimpleToolWindowPanel panel = new SimpleToolWindowPanel(true, true);
        panel.setContent(createMainPanel(project));

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "Disassembly View", false);
        toolWindow.getContentManager().addContent(content);
    }

    private JPanel createMainPanel(Project project) {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // List of addresses
        addressModel = new DefaultListModel<>();
        addressList = new JBList<>(addressModel);
        addressList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JBScrollPane addressScrollPane = new JBScrollPane(addressList);
        addressScrollPane.setPreferredSize(new Dimension(150, 0));

        // Disassembly code display
        disassemblyTextArea = new JBTextArea();
        disassemblyTextArea.setEditable(false);
        JBScrollPane disassemblyScrollPane = new JBScrollPane(disassemblyTextArea);

        // Address input and load button
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField addressInput = new JTextField(10);
        JButton loadButton = new JButton("Load");
        controlPanel.add(new JLabel("Address:"));
        controlPanel.add(addressInput);
        controlPanel.add(loadButton);

        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(addressScrollPane, BorderLayout.WEST);
        mainPanel.add(disassemblyScrollPane, BorderLayout.CENTER);

        // Connect to debugger
        connectDebugger(project);

        return mainPanel;
    }

    private void connectDebugger(Project project) {
        XDebuggerManager debuggerManager = XDebuggerManager.getInstance(project);
        XDebugSession session = debuggerManager.getCurrentSession();
        if (session != null) {
            XStackFrame frame = session.getCurrentStackFrame();
            if (frame != null) {
                frame.computeChildren(new XCompositeNode() {
                    @Override
                    public void addChildren(@NotNull XValueChildrenList xValueChildrenList, boolean b) {
                        addressModel.clear();
                        //for (XStackFrame stackFrame : list) {
                            String frameInfo = frame.toString();  // Personnaliser selon vos besoins
                            addressModel.addElement(frameInfo);
                        //}
                    }

                    @Override
                    public void tooManyChildren(int i) {

                    }

                    @Override
                    public void setAlreadySorted(boolean b) {

                    }

                    @Override
                    public void setErrorMessage(@NotNull String errorMessage) {
// Affichage du message d'erreur dans la zone de texte
                        disassemblyTextArea.setText("Error: " + errorMessage);
                    }

                    @Override
                    public void setErrorMessage(@NotNull String s, @Nullable XDebuggerTreeNodeHyperlink xDebuggerTreeNodeHyperlink) {

                    }

                    @Override
                    public void setMessage(@NotNull String s, @Nullable Icon icon, @NotNull SimpleTextAttributes simpleTextAttributes, @Nullable XDebuggerTreeNodeHyperlink xDebuggerTreeNodeHyperlink) {

                    }
                });

            }
        }
    }

    public void updateDisassemblyView(String address, String disassemblyCode) {
        addressModel.addElement(address);
        disassemblyTextArea.setText(disassemblyCode);
    }
}
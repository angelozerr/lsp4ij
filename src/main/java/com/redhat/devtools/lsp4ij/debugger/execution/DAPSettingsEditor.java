package com.redhat.devtools.lsp4ij.debugger.execution;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class DAPSettingsEditor extends SettingsEditor<DAPRunConfiguration> {

  private final JPanel myPanel;
  private final TextFieldWithBrowseButton scriptPathField;

  public DAPSettingsEditor() {
    scriptPathField = new TextFieldWithBrowseButton();
    scriptPathField.addBrowseFolderListener("Select Script File", null, null,
        FileChooserDescriptorFactory.createSingleFileDescriptor());
    myPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent("Script file", scriptPathField)
        .getPanel();
  }

  @Override
  protected void resetEditorFrom(DAPRunConfiguration DAPRunConfiguration) {
    scriptPathField.setText(DAPRunConfiguration.getScriptName());
  }

  @Override
  protected void applyEditorTo(@NotNull DAPRunConfiguration DAPRunConfiguration) {
    DAPRunConfiguration.setScriptName(scriptPathField.getText());
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    return myPanel;
  }

}
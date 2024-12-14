package com.redhat.devtools.lsp4ij.debugger.execution;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.components.BaseState;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DAPConfigurationFactory extends ConfigurationFactory {

  protected DAPConfigurationFactory(ConfigurationType type) {
    super(type);
  }

  @Override
  public @NotNull String getId() {
    return DAPRunConfigurationType.ID;
  }

  @NotNull
  @Override
  public RunConfiguration createTemplateConfiguration(
      @NotNull Project project) {
    return new DAPRunConfiguration(project, this, "Demo");
  }

  @Nullable
  @Override
  public Class<? extends BaseState> getOptionsClass() {
    return DAPRunConfigurationOptions.class;
  }

}
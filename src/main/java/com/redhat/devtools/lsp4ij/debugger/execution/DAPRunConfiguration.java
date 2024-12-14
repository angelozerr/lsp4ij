package com.redhat.devtools.lsp4ij.debugger.execution;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.redhat.devtools.lsp4ij.debugger.DAPCommandLineState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DAPRunConfiguration extends RunConfigurationBase<DAPRunConfigurationOptions> {

  protected DAPRunConfiguration(Project project,
                                 ConfigurationFactory factory,
                                 String name) {
    super(project, factory, name);
  }

  @NotNull
  @Override
  protected DAPRunConfigurationOptions getOptions() {
    return (DAPRunConfigurationOptions) super.getOptions();
  }

  public String getScriptName() {
    return getOptions().getScriptName();
  }

  public void setScriptName(String scriptName) {
    getOptions().setScriptName(scriptName);
  }

  @NotNull
  @Override
  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new DAPSettingsEditor();
  }

  @Nullable
  @Override
  public RunProfileState getState(@NotNull Executor executor,
                                  @NotNull ExecutionEnvironment environment) {
    DAPCommandLineState state = new DAPCommandLineState(environment);
    return state;
    /*return new CommandLineState(environment) {
      @NotNull
      @Override
      protected ProcessHandler startProcess() throws ExecutionException {
        GeneralCommandLine commandLine =
            new GeneralCommandLine(getOptions().getScriptName());
        OSProcessHandler processHandler = ProcessHandlerFactory.getInstance()
            .createColoredProcessHandler(commandLine);
        ProcessTerminatedListener.attach(processHandler);
        return processHandler;
      }
    };*/
  }

}
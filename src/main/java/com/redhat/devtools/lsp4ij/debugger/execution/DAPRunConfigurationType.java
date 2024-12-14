package com.redhat.devtools.lsp4ij.debugger.execution;

import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.NotNullLazyValue;

final class DAPRunConfigurationType extends ConfigurationTypeBase {

  static final String ID = "DAPRunConfiguration";

  DAPRunConfigurationType() {
    super(ID, "DAP", "Debug Adapter Protocol configuration type",
        NotNullLazyValue.createValue(() -> AllIcons.Nodes.Console));
    addFactory(new DAPConfigurationFactory(this));
  }

}
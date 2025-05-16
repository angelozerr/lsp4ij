package com.redhat.devtools.lsp4ij.installation.definition.steps;

import com.google.gson.JsonObject;
import com.redhat.devtools.lsp4ij.JSONUtils;
import com.redhat.devtools.lsp4ij.installation.definition.InstallerStepActionFactoryBase;
import com.redhat.devtools.lsp4ij.installation.definition.ServerInstallerRunner;
import com.redhat.devtools.lsp4ij.installation.definition.StepAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <pre>
 * {
 *       "type": "showMessage",
 *         "message": "typescript-language-server is not installed.",
 *         "actions": [
 *           {
 *             "label": "Install typescript-language-server globally",
 *             "type": "exec",
 *             "command": {
 *               "default": ["npm", "install", "-g", "typescript-language-server"]
 *             }
 *           },
 *           {
 *             "label": "Visit typescript-language-server GitHub",
 *             "type": "openUrl",
 *             "url": "https://github.com/typescript-language-server/typescript-language-server"
 *           }
 *         ]
 *       }
 * }
 * </pre>
 */
public class ShowMessageStepActionFactory extends InstallerStepActionFactoryBase {

    @Override
    protected @NotNull StepAction create(@Nullable String id,
                                         @Nullable String name,
                                         @Nullable StepAction onFail,
                                         @NotNull JsonObject json,
                                         @NotNull ServerInstallerRunner runner) {
        @NotNull List<StepAction> actions = loadActions(json, runner);
        return new ShowMessageStepAction(id, name, onFail, actions, runner);
    }

    private static @NotNull List<StepAction> loadActions(@NotNull JsonObject json,
                                                         @NotNull ServerInstallerRunner runner) {
        var actions = JSONUtils.getJsonArray(json, "actions");
        if (actions == null) {
            return Collections.emptyList();
        }
        List<StepAction> stepsActions = new ArrayList<>();
        for (int i = 0; i < actions.size(); i++) {
            var current = actions.get(i);
            if (current.isJsonObject()) {
                var jsonStep = current.getAsJsonObject();
                var action = runner.getStepActionRegistry().loadStep(jsonStep, runner);
                if (action != null) {
                    stepsActions.add(action);
                }
            }
        }
        return stepsActions;
    }
}

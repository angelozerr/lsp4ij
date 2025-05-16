package com.redhat.devtools.lsp4ij.installation.definition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.redhat.devtools.lsp4ij.JSONUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class StepActionRegistry {

    private final @NotNull Map<String, InstallerStepActionFactory> factories;

    public StepActionRegistry() {
        factories = new HashMap<>();
    }

    public void registerFactory(String type, InstallerStepActionFactory factory) {
        factories.put(type, factory);
    }

    public @NotNull ServerInstallerRunner loadRunner(@NotNull JsonObject json) {
        String name = JSONUtils.getString(json, "name");
        ServerInstallerRunner runner = new ServerInstallerRunner(name != null ? name : "Untitled", this);
        JsonArray steps = JSONUtils.getJsonArray(json, "steps");
        if (steps != null) {
            for (int i = 0; i < steps.size(); i++) {
                var step = steps.get(i);
                if (step.isJsonObject()) {
                    var stepObject = step.getAsJsonObject();
                    String ref = JSONUtils.getString(stepObject, "ref");
                    if (ref != null) {

                    } else {
                        var stepAction = loadStep(stepObject, runner);
                        if (stepAction != null) {
                            runner.getSteps().add(stepAction);
                        }
                    }
                }
            }
        }
        return runner;
    }

    public StepAction loadStep(@Nullable JsonObject stepObject,
                               @NotNull ServerInstallerRunner runner) {
        if (stepObject == null) {
            return null;
        }
        String type = getType(stepObject);
        if (type != null) {
            var factory = factories.get(type);
            if (factory != null) {
                return factory.create(stepObject.get(type).getAsJsonObject(), runner);
            }
        }
        return null;
    }

    private @Nullable String getType(@NotNull JsonObject json) {
        var keys = json.keySet();
        if (keys.isEmpty()) {
            return null;
        }
        return keys.iterator().next();
    }
}

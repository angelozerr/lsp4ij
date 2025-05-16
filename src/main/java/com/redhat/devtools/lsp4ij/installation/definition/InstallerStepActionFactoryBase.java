package com.redhat.devtools.lsp4ij.installation.definition;

import com.google.gson.JsonObject;
import com.redhat.devtools.lsp4ij.JSONUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class InstallerStepActionFactoryBase implements InstallerStepActionFactory{

    public static final String ID_JSON_PROPERTY = "id";
    public static final String NAME_JSON_PROPERTY = "name";
    public static final String ON_FAIL_JSON_PROPERTY = "onFail";

    @Override
    public final @NotNull StepAction create(@NotNull JsonObject json,
                                            @NotNull ServerInstallerRunner runner) {
        @Nullable String id = JSONUtils.getString(json, ID_JSON_PROPERTY);
        @Nullable  String name = JSONUtils.getString(json, NAME_JSON_PROPERTY);
        @Nullable StepAction onFail = loadOnFail(json, runner);
        return create(id, name, onFail, json, runner);
    }

    private @Nullable StepAction loadOnFail(@NotNull JsonObject json,
                                            @NotNull ServerInstallerRunner runner) {
        JsonObject onFail = JSONUtils.getJsonObject(json, ON_FAIL_JSON_PROPERTY);
        if (onFail == null) {
            return null;
        }
        return runner.getStepActionRegistry().loadStep(onFail, runner);
    }

    protected abstract @NotNull StepAction create(@Nullable String id,
                                                  @Nullable String name,
                                                  @Nullable StepAction onFail,
                                                  @NotNull JsonObject json,
                                                  @NotNull ServerInstallerRunner runner);

}

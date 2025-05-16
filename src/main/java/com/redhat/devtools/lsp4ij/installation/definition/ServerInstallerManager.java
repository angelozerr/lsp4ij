package com.redhat.devtools.lsp4ij.installation.definition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.redhat.devtools.lsp4ij.JSONUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class ServerInstallerManager extends StepActionRegistry {

    private final @NotNull Project project;

    /**
     * Returns the server installer manager instance for the given project.
     *
     * @param project the project.
     * @return the server installer manager instance for the given project.
     */
    public static ServerInstallerManager getInstance(@NotNull Project project) {
        return project.getService(ServerInstallerManager.class);
    }

    private ServerInstallerManager(@NotNull Project project) {
        super();
        this.project = project;
        var beans = InstallerStepActionFactoryPointBean.EP_NAME.getExtensions();
        for (int i = 0; i < beans.length; i++) {
            try {
                var bean = beans[i];
                super.registerFactory(bean.type, bean.getInstance());
            } catch (Exception e) {

            }
        }
    }


    public void install(@NotNull String installerConfigurationContent,
                        @NotNull String title,
                        @NotNull InstallerContext context) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, title, true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                context.setProgressIndicator(indicator);
                JsonElement installerConfiguration = JsonParser.parseReader(new StringReader(installerConfigurationContent));
                if (installerConfiguration.isJsonObject()) {
                    var json = installerConfiguration.getAsJsonObject();
                    install(json, context);
                }

            }
        });
    }

    private void install(@NotNull JsonObject json,
                         @NotNull InstallerContext context) {
        var runner = loadRunner(json);
        install(runner, context);
    }

    private void install(@NotNull ServerInstallerRunner runner,
                         @NotNull InstallerContext context) {
        context.clear();
        context.print(runner.getName());
        for (int i = 0; i < runner.getSteps().size(); i++) {
            var step = runner.getSteps().get(i);
            // Execute step.
            step.execute(context);
        }
    }
}

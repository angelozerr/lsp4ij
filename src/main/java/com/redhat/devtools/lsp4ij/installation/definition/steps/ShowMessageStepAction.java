package com.redhat.devtools.lsp4ij.installation.definition.steps;


import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.util.Ref;
import com.redhat.devtools.lsp4ij.LSP4IJWebsiteUrlConstants;
import com.redhat.devtools.lsp4ij.ServerMessageHandler;
import com.redhat.devtools.lsp4ij.installation.definition.InstallerContext;
import com.redhat.devtools.lsp4ij.installation.definition.ServerInstallerRunner;
import com.redhat.devtools.lsp4ij.installation.definition.StepAction;
import com.redhat.devtools.lsp4ij.settings.actions.DisableLanguageServerErrorAction;
import com.redhat.devtools.lsp4ij.settings.actions.OpenUrlAction;
import com.redhat.devtools.lsp4ij.settings.actions.ReportErrorInLogAction;
import com.redhat.devtools.lsp4ij.settings.actions.ShowErrorLogAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
public class ShowMessageStepAction extends StepAction {

    private final @NotNull List<StepAction> actions;

    public ShowMessageStepAction(@Nullable String id,
                                 @Nullable String name,
                                 @Nullable StepAction onFail,
                                 @NotNull List<StepAction> actions,
                                 @NotNull ServerInstallerRunner runner) {
        super(id, name, onFail, runner);
        this.actions = actions;
    }

    @Override
    public boolean run(@NotNull InstallerContext context) {
        Ref<StepAction> selectedAction = new Ref<>();
        Notification notification = new Notification(ServerMessageHandler.LSP_WINDOW_SHOW_MESSAGE_GROUP_ID,
                "Install error",
                getName() != null  ? getName() : "Untitled",
                NotificationType.ERROR);
        for (var action : actions) {
            notification.addAction(new AnAction(action.getName()) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                    selectedAction.set(action);
                }
            });
        }
        Notifications.Bus.notify(notification, context.getProject());
        while(selectedAction.isNull()) {
            // Wait...
            try {
                synchronized (selectedAction) {
                    selectedAction.wait(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (!selectedAction.isNull()) {
            var stepAction = selectedAction.get();
            return stepAction.execute(context);
        }
        return false;
    }
}

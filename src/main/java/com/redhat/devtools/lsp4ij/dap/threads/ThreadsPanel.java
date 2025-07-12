package com.redhat.devtools.lsp4ij.dap.threads;

import com.intellij.CommonBundle;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.components.BorderLayoutPanel;
import org.eclipse.lsp4j.debug.ExceptionBreakpointsFilter;
import org.eclipse.lsp4j.debug.Thread;
import org.eclipse.lsp4j.debug.ThreadEventArguments;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;

public class ThreadsPanel extends BorderLayoutPanel implements Disposable {

    public static final @NotNull @NonNls String ID = "dap-threads-panel";

    static final NameColumnInfo NAME_COLUMN = new NameColumnInfo();

    private final @NotNull ListTableModel<Thread> myModel;
    private final @NotNull TableView<Thread> myTable;

    public ThreadsPanel() {
        this.myModel = new ListTableModel<>(new ColumnInfo[]{NAME_COLUMN});
        this.myModel.setSortable(true);
        this.myTable = new TableView<>(this.myModel);
        this.addToCenter(ScrollPaneFactory.createScrollPane(this.myTable));
    }

    public JComponent getDefaultFocusedComponent() {
        return this.myTable;
    }

    @Override
    public void dispose() {

    }

    public void refreshThread(ThreadEventArguments args) {

    }

    private static class NameColumnInfo extends ColumnInfo<ExceptionBreakpointsFilter, ExceptionBreakpointsFilter> {

        NameColumnInfo() {
            super(CommonBundle.message("title.name", new Object[0]));
        }

        public @Nullable ExceptionBreakpointsFilter valueOf(ExceptionBreakpointsFilter aspects) {
            return aspects;
        }

        public Class<?> getColumnClass() {
            return ExceptionBreakpointsFilter.class;
        }

        public @Nullable TableCellRenderer getRenderer(ExceptionBreakpointsFilter producer) {
            return new ColoredTableCellRenderer() {
                protected void customizeCellRenderer(@NotNull JTable table, @Nullable Object value, boolean selected, boolean hasFocus, int row, int column) {
                    if (value instanceof Thread thread) {
                        String label = thread.getName();
                        this.append(label);
                        super.setIcon(AllIcons.Debugger.Db_exception_breakpoint);
                        this.setTransparentIconBackground(true);
                    }
                }
            };
        }
    }
}

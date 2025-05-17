package com.redhat.devtools.lsp4ij.server;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.impl.ExecutionManagerImpl;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.util.io.BaseOutputReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * StreamConnectionProvider that launches a process which exposes a TCP socket,
 * then connects via socket to the language server.
 */
public class SocketStreamConnectionProvider implements StreamConnectionProvider, ProcessDataProvider {

    private final String host;
    private final int port;
    private final GeneralCommandLine commandLine;

    private OSProcessHandler processHandler;
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    private final List<LanguageServerLogErrorHandler> handlers = new ArrayList<>();
    private final List<Runnable> unexpectedServerStopHandlers = new ArrayList<>();
    private volatile boolean stopped;

    // Timeout to wait for socket availability after process start (ms)
    private static final int SOCKET_CONNECT_TIMEOUT_MS = 5000;
    private static final int SOCKET_READ_TIMEOUT_MS = 2000;

    public SocketStreamConnectionProvider(@NotNull GeneralCommandLine commandLine, @NotNull String host, int port) {
        this.commandLine = commandLine;
        this.host = host;
        this.port = port;
    }

    @Override
    public void start() throws CannotStartProcessException {
        if (commandLine == null) {
            throw new CannotStartProcessException("Command line not set");
        }
        try {
            processHandler = new OSProcessHandler(commandLine) {
                @Override
                protected BaseOutputReader.@NotNull Options readerOptions() {
                    return BaseOutputReader.Options.forMostlySilentProcess();
                }
            };
            processHandler.addProcessListener(new LSPProcessListener(this));
            processHandler.startNotify();

            // Wait until the TCP socket is available for connection or timeout
            waitForSocketReady();

            this.socket = new Socket(host, port);
            this.socket.setSoTimeout(SOCKET_READ_TIMEOUT_MS);
            this.inputStream = socket.getInputStream();
            this.outputStream = socket.getOutputStream();

        } catch (IOException e) {
            throw new CannotStartProcessException(e); //"Failed to connect to language server socket", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CannotStartProcessException(e); //"Interrupted while waiting for socket", e);
        } catch (Exception e) {
            throw new CannotStartProcessException(e);
        }
    }

    private void waitForSocketReady() throws InterruptedException, IOException, CannotStartProcessException {
        Thread.sleep(5000);
        if (true) {
            return;
        }
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < SOCKET_CONNECT_TIMEOUT_MS) {
            try (Socket testSocket = new Socket(host, port)) {
                // Success, socket is ready
                return;
            } catch (IOException ignored) {
                Thread.sleep(5000);
                if (processHandler.isProcessTerminated()) {
                    throw new CannotStartProcessException("Language server process terminated before socket became available");
                }
            }
        }
        throw new CannotStartProcessException("Timeout waiting for socket at " + host + ":" + port);
    }

    @Override
    public void stop() {
        if (stopped) return;
        stopped = true;

        if (processHandler != null && !processHandler.isProcessTerminated()) {
            ExecutionManagerImpl.stopProcess(processHandler);
        }
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public OutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public boolean isAlive() {
        return processHandler != null && processHandler.isStartNotified() && !processHandler.isProcessTerminated()
                && socket != null && socket.isConnected() && !socket.isClosed();
    }

    @Override
    public void ensureIsAlive() throws CannotStartProcessException {
        if (!isAlive()) {
            throw new CannotStartProcessException("Language server is not alive");
        }
    }

    @Override
    public void addLogErrorHandler(LanguageServerLogErrorHandler handler) {
        handlers.add(handler);
    }

    @Override
    public void addUnexpectedServerStopHandler(Runnable handler) {
        unexpectedServerStopHandlers.add(handler);
    }

    @Override
    public List<String> getCommands() {
        if (commandLine == null) {
            return Collections.emptyList();
        }
        List<String> commands = new ArrayList<>();
        commands.add(commandLine.getExePath());
        commands.addAll(commandLine.getParametersList().getParameters());
        return commands;
    }

    @Override
    public @Nullable Long getPid() {
        Process p = processHandler != null ? processHandler.getProcess() : null;
        return p == null ? null : p.pid();
    }

    @Override
    public String toString() {
        return "SocketStreamConnectionProvider [commandLine=" + commandLine +
                ", host=" + host + ", port=" + port + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(commandLine, host, port);
    }
}

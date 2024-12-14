package com.redhat.devtools.lsp4ij.debugger;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.redhat.devtools.lsp4ij.internal.CompletableFutures;
import org.eclipse.lsp4j.debug.Thread;
import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.validation.ReflectiveMessageValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;

public class DAPDebugProtocolClient implements IDebugProtocolClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DAPDebugProtocolClient.class);

    private static final boolean TRACE_MESSAGES = true;
    private static final boolean TRACE_IO = true;
    private final DAPDebugProcess debugProcess;

    private boolean fTerminated = false;

    /**
     * Any events we receive from the adapter that require further contact with the
     * adapter needs to be farmed off to another thread as the events arrive on the
     * same thread. (Note for requests, use the *Async versions on
     * completeablefuture to achieve the same effect.)
     */
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    private TransportStreams transportStreams;

    private Future<Void> debugProtocolFuture;
    private IDebugProtocolServer debugProtocolServer;

    private final Map<String, Object> dspParameters;

    /**
     * Debug adapters are not supposed to send initialized event until after
     * replying to initializeRequest with Capabilities. However some debug adapters
     * don't respect this (or didn't in the past). Even if they do respect this,
     * sometimes due to the multithreaded event handling, the initialized event
     * arrives before the capabilities are stored. Therefore use a future to guard
     * reading the capabilities before they are ready.
     */
    private final CompletableFuture<@Nullable Capabilities> capabilitiesFuture = new CompletableFuture<>();
    /**
     * Once we have received initialized event, this member will be "done" as a flag
     */
    private final CompletableFuture<@Nullable Void> initialized = new CompletableFuture<>();

    /**
     * The cached set of current threads. This should generally not be directly
     * accessed and instead accessed via {@link #getThreads()} which will ensure
     * they are up to date (against the {@link #refreshThreads} flag).
     */
    private final Map<Integer, DSPThread> threads = Collections.synchronizedMap(new TreeMap<>());

    /**
     * Set to true to update the threads list from the debug adapter.
     */
    private final AtomicBoolean refreshThreads = new AtomicBoolean(true);

    private boolean fSentTerminateRequest;
    private boolean isConnected;

    public DAPDebugProtocolClient(@NotNull DAPDebugProcess debugProcess,
                                  @NotNull Map<String, Object> dspParameters) {
        this.transportStreams = debugProcess.streamsSupplier.get();
        this.debugProcess = debugProcess;
        this.dspParameters = dspParameters;
        this.isConnected = true;
    }


    private CompletableFuture<?> initialize(@NotNull Map<String, Object> dspParameters,
                                            @NotNull ProgressIndicator monitor) {
        final var arguments = DABRequestFactory.createInitializeRequestArguments(dspParameters);
        //targetName = Objects.toString(dspParameters.get("program"), "Debug Adapter Target");

        monitor.setText2("Initializing connection to debug adapter");
        boolean isLaunchRequest = "launch".equals(dspParameters.getOrDefault("request", "launch"));
        CompletableFuture<?> launchAttachFuture = getDebugProtocolServer()
                .initialize(arguments)
                .thenAccept((Capabilities capabilities) -> {
                    monitor.setFraction(10 / 100);
                    if (capabilities == null) {
                        LOGGER.error(
                                "Debug adapter unexpectedly returned 'null' Capabilities from initializeRequest. "
                                        + "A default Capabilities will be used instead.");
                        capabilities = new Capabilities();
                    }
                    capabilitiesFuture.complete(capabilities);
                }).thenCompose(unused -> {
                    monitor.setFraction(20 / 100);
                    if (isLaunchRequest) {
                        monitor.setText2("Launching program");
                        return getDebugProtocolServer().launch(dspParameters);
                    } else {
                        monitor.setText2("Attaching to running program");
                        return getDebugProtocolServer().attach(dspParameters);
                    }
                }).handle((q, t) -> {
                    if (t != null) {
                        initialized.completeExceptionally(t);
                    }
                    return q;
                });
        CompletableFuture<@Nullable Void> configurationDoneFuture = CompletableFuture
                .allOf(initialized, capabilitiesFuture).thenRun(() -> monitor.setFraction(30 / 100));
        //if (ILaunchManager.DEBUG_MODE.equals(launch.getLaunchMode())) {
        boolean debugMode = true;
        if (debugMode) {
            configurationDoneFuture = configurationDoneFuture
                    .thenCompose(v -> {
                        monitor.setFraction(60 / 100);
                        monitor.setText2("Sending breakpoints");
                        return debugProcess.breakpointHandler.initialize(getDebugProtocolServer(),
                                getCapabilities());
                    });
        }
        configurationDoneFuture = configurationDoneFuture
                .thenCompose(v -> {
                    monitor.setFraction(70 / 100);
                    monitor.setText2("Sending configuration done");
                    if (Boolean.TRUE.equals(getCapabilities().getSupportsConfigurationDoneRequest())) {
                        return getDebugProtocolServer().configurationDone(new ConfigurationDoneArguments());
                    }
                    return CompletableFuture.completedFuture(null);
                });
        return CompletableFuture.allOf(launchAttachFuture, configurationDoneFuture);
    }

    IDebugProtocolServer getDebugProtocolServer() {
        return debugProtocolServer;
    }


    /**
     * Return the Capabilities of the currently attached debug adapter.
     * <p>
     * Clients should not call this method until after the debug adapter has been
     * initialized.
     *
     * @return the current capabilities if they have been retrieved, or else
     * return @{code null}
     */
    @Nullable Capabilities getCapabilities() {
        return capabilitiesFuture.getNow(null);
    }

    CompletableFuture<Void> init() {
        if (TRACE_IO) {
            transportStreams = transportStreams.withTrace();
        }

        UnaryOperator<MessageConsumer> wrapper = consumer -> {
            MessageConsumer result = consumer;
            if (TRACE_MESSAGES) {
                result = message -> {
                    getProcessHandler().notifyTextAvailable(message.toString(), ProcessOutputType.STDOUT);
                    consumer.consume(message);
                };
            }
            if (true) {
                result = new ReflectiveMessageValidator(result);
            }
            return result;
        };

        Launcher<? extends IDebugProtocolServer> debugProtocolLauncher = createLauncher(wrapper,
                transportStreams.in,
                transportStreams.out,
                threadPool);

        debugProtocolFuture = debugProtocolLauncher.startListening();
        debugProtocolServer = debugProtocolLauncher.getRemoteProxy();

        CompletableFuture<Void> result = new CompletableFuture<>();
        ProgressManager.getInstance().run(new Task.Backgroundable(getSession().getProject(), "DAP starting", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                CompletableFuture<?> future = initialize(dspParameters, indicator);
                try {
                    CompletableFutures.waitUntilDone(future);
                    result.complete(null);
                } catch (ExecutionException e) {
                    result.completeExceptionally(e);
                }
            }
        });
        return result;
    }

    private ProcessHandler getProcessHandler() {
        return debugProcess.getProcessHandler();
    }


    @Override
    public void initialized() {
        initialized.complete(null);
    }

    @Override
    public CompletableFuture<Void> startDebugging(StartDebuggingRequestArguments args) {
        final var parameters = new HashMap<String, Object>(/* dspParameters */);
        parameters.putAll(args.getConfiguration());

        DAPDebugProtocolClient client = new DAPDebugProtocolClient(debugProcess,parameters);
        debugProcess.clients.add(client);
        return client.init();
    }

    @Override
    public void thread(ThreadEventArguments args) {
        refreshThreads.set(true);
        CompletableFuture.runAsync(() -> {
            getThreads();
            DSPThread thread = getThread(args.getThreadId());
            if (thread != null) {
                if (!thread.getStackFrames().isEmpty()) {
                }
            }
        });
    }


    @Override
    public void terminated(TerminatedEventArguments body) {
        terminated();
    }

    @Override
    public void continued(ContinuedEventArguments body) {
        /*threadPool.execute(() -> {
            DSPThread source = getThread(body.getThreadId());
            if (source == null || body.getAllThreadsContinued() == null || body.getAllThreadsContinued()) {
                Arrays.asList(getThreads()).forEach(DSPThread::continued);
            }
            if (source != null) {
                getSession().resume();
                //source.fireResumeEvent(DebugEvent.CLIENT_REQUEST);
            }
        });*/
    }

    @Override
    public void stopped(StoppedEventArguments body) {
        handleDebugHit(body);
        /*threadPool.execute(() -> {
            DSPThread source = null;
            if (body.getThreadId() != null) {
                source = getThread(body.getThreadId());
            }
            if (source == null || body.getAllThreadsStopped() == null || body.getAllThreadsStopped()) {
                Arrays.asList(getThreads()).forEach(t -> {
                    t.stopped();
           //         t.fireChangeEvent(DebugEvent.CHANGE);
                });
            }

            if (source != null) {
                source.stopped();
                //debugProcess.getSession().positionReached(new DAPSuspendContext(this, source));
            }
        });*/
    }

    //@Override
    public DSPThread[] getThreads() {
        if (!refreshThreads.getAndSet(false)) {
            synchronized (threads) {
                Collection<DSPThread> values = threads.values();
                return values.toArray(new DSPThread[values.size()]);
            }
        }
        try {
            var server = getDebugProtocolServer();
            if (server == null) {
                return new DSPThread[0];
            }
            CompletableFuture<ThreadsResponse> threads2 = server.threads();
            CompletableFuture<DSPThread[]> future = threads2.thenApplyAsync(threadsResponse -> {
                synchronized (threads) {
                    final var lastThreads = new TreeMap<Integer, DSPThread>(threads);
                    threads.clear();
                    Thread[] body = threadsResponse.getThreads();
                    for (Thread thread : body) {
                        DSPThread dspThread = lastThreads.get(thread.getId());
                        if (dspThread == null) {
                            dspThread = new DSPThread(this, thread.getId());
                        }
                        dspThread.update(thread);
                        threads.put(thread.getId(), dspThread);
                        // fireChangeEvent(DebugEvent.CONTENT);
                    }
                    Collection<DSPThread> values = threads.values();
                    return values.toArray(new DSPThread[values.size()]);
                }
            });
            return future.get();
        } catch (RuntimeException | ExecutionException e) {
            if (isTerminated()) {
                return new DSPThread[0];
            }
    //        DSPPlugin.logError(e);
        } catch (InterruptedException e) {
            java.lang.Thread.currentThread().interrupt();
        }
        return new DSPThread[0];
    }

    private boolean isTerminated() {
        return fTerminated;
    }

    /**
     * Get a thread object without connecting to the debug adapter. This is for when
     * we get a thread before it is fully populated, so return a new thread in that
     * case and let it be populated later
     *
     * @param threadId
     * @return
     */
    private DSPThread getThread(Integer threadId) {
        return threads.computeIfAbsent(threadId, id -> new DSPThread(this, threadId));
    }

    /*@Override
    public boolean hasThreads() throws DebugException {
        return getThreads().length > 0;
    }*/

    @Override
    public void output(OutputEventArguments args) {
        String output = args.getOutput();

        if (args.getCategory() == null //
                || OutputEventArgumentsCategory.CONSOLE.equals(args.getCategory()) //
                || OutputEventArgumentsCategory.STDOUT.equals(args.getCategory())) {
            getProcessHandler().notifyTextAvailable(output, ProcessOutputType.STDOUT);

            // TODO put this data in a different region with a different colour
            //getOrCreateStreamsProxy().getOutputStreamMonitor().append(output);
        } else if (OutputEventArgumentsCategory.STDERR.equals(args.getCategory())) {
            //getOrCreateStreamsProxy().getErrorStreamMonitor().append(output);
            //executionResult.getExecutionConsole().
            getProcessHandler().notifyTextAvailable(output, ProcessOutputType.STDERR);

        }
        //else if (DSPPlugin.DEBUG) {
        //    System.out.println("output: " + args);
        // }
    }

    void terminated() {
        fTerminated = true;
        Arrays.stream(getThreads()).forEach(t -> {
            t.terminate();
        });
        getSession().stop();
        getProcessHandler().destroyProcess();
        /*if (process != null) {
            // Disable the terminate button of the console associated with the DSPProcess.
            DebugPlugin.getDefault()
                    .fireDebugEventSet(new DebugEvent[] { new DebugEvent(process, DebugEvent.TERMINATE) });
        }
        if (breakpointManager != null) {
            breakpointManager.shutdown();
        }*/
        if (debugProtocolFuture != null) {
            debugProtocolFuture.cancel(true);
            /*
             * If the debugProtocolFuture is running the message loop on the same thread we
             * are in currently, the cancel call will set the interrupt flag. That isn't
             * necessary as the message loop isn't waiting on anything (since this code is
             * running). The interrupt flag may affect future processing, like terminating
             * processes. Therefore clear the flag. See lsp4e#264
             */
            java.lang.Thread.interrupted();
        }
        if (transportStreams != null) {
            transportStreams.close();
        }
    }

    private Launcher<? extends IDebugProtocolServer> createLauncher(UnaryOperator<MessageConsumer> wrapper,
                                                                    InputStream in, OutputStream out,
                                                                    ExecutorService threadPool) {
        return DSPLauncher.createClientLauncher(this, in, out, threadPool, wrapper);
    }

    XDebugSession getSession() {
        return debugProcess.getSession();
    }

    public void sessionStopped() {
        IDebugProtocolServer server = getDebugProtocolServer();
        if (server == null) {
            return;
        }
        boolean shouldSendTerminateRequest = !fSentTerminateRequest
                        &&
                isSupportsTerminateRequest()
                        && "launch".equals(dspParameters.getOrDefault("request", "launch"));
        if (shouldSendTerminateRequest) {
            fSentTerminateRequest = true;
            server
                    .terminate(new TerminateArguments())
                    .thenRunAsync(this::terminated);
        } else {
            final var arguments = new DisconnectArguments();
            arguments.setTerminateDebuggee(true);
            server
                    .disconnect(arguments)
                    .thenRunAsync(this::terminated);
        }
    }


    private boolean isSupportsTerminateRequest() {
        return DAPCapabilitiesUtils.isSupportsTerminateRequest(getCapabilities());
    }


    public void handleDebugHit(StoppedEventArguments args) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!isConnected) {
                return;
            }
            StackTraceArguments stackTraceArgs = new StackTraceArguments();
            stackTraceArgs.setThreadId(args.getThreadId());
            try {
                getThreads();
                DSPThread thread = getThread(args.getThreadId());
                if (thread != null) {
                    thread.stopped();
                    List<DSPStackFrame> stackFrames = thread.getStackFrames();
                    if (!stackFrames.isEmpty()) {
                        XBreakpoint/*<DAPBreakpointProperties>*/ breakpoint = debugProcess.breakpointHandler.findBreakPoint(stackFrames.get(0));
                        // Get the current suspend context from the session. If the context is null, we need to create a new
                        // context. If the context is not null, we need to add a new execution stack to the current suspend
                        // context.
                        XSuspendContext context = getSession().getSuspendContext();
                        if (context == null) {
                            context = new DAPSuspendContext(DAPDebugProtocolClient.this, thread);
                        }
                        //((DAPSuspendContext) context).addToExecutionStack(args.getThreadId(), stackFrames);
                        ((DAPSuspendContext) context).addToExecutionStack(thread);
                        XDebugSession session = getSession();
                        if (breakpoint == null) {
                            session.positionReached(context);
                        } else {
                            session.breakpointReached(breakpoint, null, context);
                        }
                    }
                }
            } catch(Exception e){
                LOGGER.warn("Error occurred when fetching stack frames", e);
            }
        });

    }

}


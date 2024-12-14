package com.redhat.devtools.lsp4ij.debugger;

import com.intellij.execution.ExecutionResult;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.redhat.devtools.lsp4ij.debugger.breakpoints.DAPBreakpointHandler;
import org.apache.commons.collections.map.HashedMap;
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
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class DAPDebugProcess extends XDebugProcess implements IDebugProtocolClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DAPDebugProcess.class);

    private static final boolean TRACE_MESSAGES = true;
    private static final boolean TRACE_IO = true;
    /**
     * Any events we receive from the adapter that require further contact with the
     * adapter needs to be farmed off to another thread as the events arrive on the
     * same thread. (Note for requests, use the *Async versions on
     * completeablefuture to achieve the same effect.)
     */
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    private final XDebuggerEditorsProvider editorsProvider;
    private final Supplier<TransportStreams> streamsSupplier;

    private TransportStreams transportStreams;

    private Future<Void> debugProtocolFuture;
    private IDebugProtocolServer debugProtocolServer;

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
    private final ExecutionResult executionResult;
    private final DAPBreakpointHandler breakpointHandler;

    /**
     * The cached set of current threads. This should generally not be directly
     * accessed and instead accessed via {@link #getThreads()} which will ensure
     * they are up to date (against the {@link #refreshThreads} flag).
     */
    private final Map<Integer, DSPThread> threads = Collections.synchronizedMap(new TreeMap<>());

    /**
     * @param session         pass {@code session} parameter of {@link com.intellij.xdebugger.XDebugProcessStarter#start} method to this constructor
     * @param executionResult
     */
    protected DAPDebugProcess(@NotNull XDebugSession session, ExecutionResult executionResult, Supplier<TransportStreams> streamsSupplier) {
        super(session);
        this.streamsSupplier = streamsSupplier;
        this.editorsProvider = new DAPDebuggerEditorsProvider();
        this.executionResult = executionResult;
        this.breakpointHandler = new DAPBreakpointHandler();
        session.addSessionListener(new DAPDebugSessionListener(this));
        executionResult.getProcessHandler().addProcessListener(new ProcessListener() {
            @Override
            public void startNotified(@NotNull ProcessEvent event) {
                DAPDebugProcess.this.transportStreams = streamsSupplier.get();
                init();
            }
        });
    }

    void init() {
        PrintWriter traceMessages;
        if (TRACE_MESSAGES) {
            traceMessages = new PrintWriter(System.out);
        } else {
            traceMessages = null;
        }
        if (TRACE_IO) {
            transportStreams = transportStreams.withTrace();
        }

        UnaryOperator<MessageConsumer> wrapper = consumer -> {
            MessageConsumer result = consumer;
            if (traceMessages != null) {
                result = message -> {
                    traceMessages.println(message);
                    traceMessages.flush();
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

        Map<String, Object> dspParameters = new HashedMap();
        dspParameters.put("type", "pwa-node");
        dspParameters.put("request", "launch");
        dspParameters.put("outputCapture", "std");
        dspParameters.put("program", "C:\\Users\\azerr\\IdeaProjects\\untitled13\\test.js");

        ProgressManager.getInstance().run(new Task.Backgroundable(getSession().getProject(), "DAP starting", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                CompletableFuture<?> future = initialize(dspParameters, indicator);
                future.thenRun(DAPDebugProcess.this::triggerUpdateThreads); // VSCode always queries "threads" after configurationDone
            }
        });
    }

    @Override
    public @NotNull XDebuggerEditorsProvider getEditorsProvider() {
        return editorsProvider;
    }


    private Launcher<? extends IDebugProtocolServer> createLauncher(UnaryOperator<MessageConsumer> wrapper,
                                                                    InputStream in, OutputStream out,
                                                                    ExecutorService threadPool) {
        return DSPLauncher.createClientLauncher(this, in, out, threadPool, wrapper);
    }

    private CompletableFuture<?> initialize(@NotNull Map<String, Object> dspParameters,
                                            @NotNull ProgressIndicator monitor) {
        final var arguments = DABRequestFactory.createInitializeRequestArguments();
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
                return breakpointHandler.initialize(getDebugProtocolServer(),
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

    void terminated() {
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
    }

    @Nullable
    @Override
    protected ProcessHandler doGetProcessHandler() {
       return executionResult.getProcessHandler();
    }

    @Override
    public XBreakpointHandler<?> @NotNull [] getBreakpointHandlers() {
        return new XBreakpointHandler[] {breakpointHandler};
    }

    @Override
    public void stop() {
        //super.stop();
    }

    // DAP client implementation

    @Override
    public void initialized() {
        initialized.complete(null);
    }

    @Override
    public CompletableFuture<Void> startDebugging(StartDebuggingRequestArguments args) {
        final var parameters = new HashMap<String, Object>(/* dspParameters */);
        parameters.putAll(args.getConfiguration());
    /*try {
        final var newTarget = new DSPDebugTarget(launch, streamsSupplier, parameters);
        launch.addDebugTarget(newTarget);
        newTarget.initialize(new NullProgressMonitor());
        debuggees.add(newTarget);
    } catch (CoreException e) {
        DSPPlugin.logError(e);
    }*/
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void thread(ThreadEventArguments args) {
        triggerUpdateThreads();
    }

    private CompletableFuture<?> triggerUpdateThreads() {
        return getDebugProtocolServer().threads().thenAcceptAsync(threadsResponse -> {
            var threadIds = Arrays.stream(threadsResponse.getThreads())
                    .map(Thread::getId)
                    .collect(Collectors.toSet());
            boolean contentChanged = false;
            synchronized (threads) {
                contentChanged = threads.keySet().removeIf(Predicate.not(threadIds::contains));
                for (Thread thread : threadsResponse.getThreads()) {
                    DSPThread dspThread = threads.get(thread.getId());
                    if (dspThread == null) {
                        dspThread = new DSPThread(this, thread.getId());
                        threads.put(dspThread.getId(), dspThread);
                        contentChanged = true;
                    }
                    dspThread.update(thread);
                }
            }
            if (contentChanged) {
                //fireChangeEvent(DebugEvent.CONTENT);
            }
        });
    }

    @Override
    public void stopped(StoppedEventArguments body) {
        triggerUpdateThreads().thenRunAsync(() -> {
            DSPThread source = null;
            if (body.getThreadId() != null) {
                source = getThread(body.getThreadId());
            }
            if (source == null || body.getAllThreadsStopped() == null || body.getAllThreadsStopped()) {
                for (final DSPThread t : getThreads()) {
                    t.stopped();
                    //t.fireChangeEvent(DebugEvent.CHANGE);
                }
            }

            if (source != null) {
                source.stopped();
                //source.fireSuspendEvent(calcDetail(body.getReason()));
            }
        }, threadPool);
    }

    //@Override
    public DSPThread[] getThreads() {
        return threads.values().toArray(DSPThread[]::new);
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
}

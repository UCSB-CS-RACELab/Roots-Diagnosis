package edu.ucsb.cs.roots;

import com.google.common.base.Strings;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import edu.ucsb.cs.roots.anomaly.AnomalyDetectorService;
import edu.ucsb.cs.roots.bi.BottleneckFinderService;
import edu.ucsb.cs.roots.bm.BenchmarkingService;
import edu.ucsb.cs.roots.data.DataStoreService;
import edu.ucsb.cs.roots.rlang.RService;
import edu.ucsb.cs.roots.utils.RootsThreadFactory;
import edu.ucsb.cs.roots.workload.WorkloadAnalyzerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public final class RootsEnvironment {

    private static final Logger log = LoggerFactory.getLogger(RootsEnvironment.class);

    public static final String EVENT_BUS_TYPE = "eventBus.type";

    private final String id;
    private final ConfigLoader configLoader;

    private final DataStoreService dataStoreService;
    private final RService rService;
    private final WorkloadAnalyzerService workloadAnalyzerService;
    private final BottleneckFinderService bottleneckFinderService;
    private final BenchmarkingService benchmarkingService;
    private final AnomalyDetectorService anomalyDetectorService;

    private final Properties properties;
    private final Stack<ManagedService> activeServices;
    private final ExecutorService exec;
    private final EventBus eventBus;

    private State state;

    public RootsEnvironment(String id, ConfigLoader configLoader) throws Exception {
        checkArgument(!Strings.isNullOrEmpty(id), "Environment ID is required");
        checkNotNull(configLoader);
        this.id = id;
        this.configLoader = configLoader;
        this.properties = configLoader.loadGlobalProperties();

        this.dataStoreService = new DataStoreService(this);
        this.rService = new RService(this);
        this.workloadAnalyzerService = new WorkloadAnalyzerService(this);
        this.bottleneckFinderService = new BottleneckFinderService(this);
        this.benchmarkingService = new BenchmarkingService(this);
        this.anomalyDetectorService = new AnomalyDetectorService(this);

        this.activeServices = new Stack<>();

        String eventBusType = this.properties.getProperty(EVENT_BUS_TYPE, "async");
        checkArgument("async".equals(eventBusType) || "sync".equals(eventBusType),
                "Invalid event bus type: %s", eventBusType);
        if ("async".equals(eventBusType)) {
            this.exec = Executors.newCachedThreadPool(new RootsThreadFactory(id + "-event-bus"));
            this.eventBus = new AsyncEventBus(id, this.exec);
        } else {
            this.exec = null;
            this.eventBus = new EventBus(id);
        }
        this.state = State.STANDBY;
    }

    public ConfigLoader getConfigLoader() {
        return configLoader;
    }

    public synchronized void init() throws Exception {
        checkState(state == State.STANDBY);
        log.info("Initializing Roots environment.");
        try {
            initService(dataStoreService);
            initService(rService);
            initService(workloadAnalyzerService);
            initService(bottleneckFinderService);
            initService(benchmarkingService);
            initService(anomalyDetectorService);
            state = State.INITIALIZED;
        } catch (Exception e) {
            cleanupForExit();
            throw e;
        }
    }

    private void initService(ManagedService service) throws Exception {
        service.init();
        activeServices.push(service);
    }

    public synchronized void destroy() {
        checkState(state == State.INITIALIZED);
        log.info("Terminating Roots environment.");
        cleanupForExit();
        state = State.DESTROYED;
        this.notifyAll();
    }

    private void cleanupForExit() {
        while (!activeServices.isEmpty()) {
            activeServices.pop().destroy();
        }
        if (exec != null) {
            exec.shutdownNow();
        }
        properties.clear();
    }

    public String getId() {
        return id;
    }

    public DataStoreService getDataStoreService() {
        checkState(dataStoreService.getState() == State.INITIALIZED);
        return dataStoreService;
    }

    public RService getRService() {
        checkState(rService.getState() == State.INITIALIZED);
        return rService;
    }

    public String getProperty(String key, String def) {
        return properties.getProperty(key, def);
    }

    public synchronized void waitFor() {
        while (state == State.INITIALIZED) {
            try {
                this.wait(10000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public void publishEvent(Object event) {
        eventBus.post(event);
    }

    public void subscribe(Object subscriber) {
        eventBus.register(subscriber);
    }

    public static void main(String[] args) throws Exception {
        RootsEnvironment environment = new RootsEnvironment("Roots", new FileConfigLoader("conf"));
        environment.init();
        Runtime.getRuntime().addShutdownHook(new Thread("RootsShutdownHook") {
            @Override
            public void run() {
                environment.destroy();
            }
        });
        environment.waitFor();
    }
}

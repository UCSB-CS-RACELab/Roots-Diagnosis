# Roots-Diagnosis
Roots is an experimental application performance monitoring, and root cause 
analysis system for platform-as-a-service (PaaS) clouds. The code available 
in this repository analyzes the data collected from a cloud platform to
detect anomalies, and conduct root cause analysis. Roots was originally developed
to monitor an AppScale cloud. However, the data analysis code is not specific
to AppScale, and thus can be used with other cloud platforms as well.

## Concepts
There are three types of components implemented in this code.

* Anomaly detectors
* Anomaly handlers
* Benchmarkers

Anomaly detectors periodically analyze the data collected from the cloud
platform to detect anomalies. They extend the `AnomalyDetector` class.
The `SLOBasedAnomayDetector` is such an implementation that detects
anomalies by looking for performance SLO violations. New anomaly detectors
can be implemented by extending the same interface.

Anomaly hanlders get notified when an anomaly detector uncovers an anomaly.
They do not implement a specific interface. Rather, they register with the
`RootsEnvironment` class by invoking its `subscribe` method. Once subscribed,
anomaly events fired by anomaly detectors will flow into the handlers. Some
example anomaly handlers include `AnomalyLogger` and `BottleneckFinderService`.

Benchmarkers are responsible for probing an application to measure its
response time periodically. This functionality is implemented in the 
`BenchmarkingService` and `Benchmark` classes.

In addition to the above three types of components, Roots also provides
an abstraction to integrate with different data stores. Anomaly detectors,
handlers and benchmarkers
read/write data from/to a data store. Each data store must implement the
`DataStore` interface. The `ElasticSearchDataStore` class allows using
an ElasticSearch server as the data store.

All the above components are grouped and executed as a single Java process.
We refer to this process as a Roots pod. A pod uses separate threads to
execute anomaly detectors, handlers and benchmarkers. Data store abstraction
allows a pod to communicate with a local or remote database.

## Building
Use Maven 3 or higher with JDK 1.8 to build the code. From the root of the repository,
execute `mvn clean install`. This will compile the code and create a zipped
distribution package under `dist/target` directory. This zip file can 
be extracted to the target deployment environment to deploy a Roots pod.

## Configuration
Extracting the zipped distribution package will create a directory with
several subdirectories. One of these subdirectories is `conf`, which houses
all the configuration files related to the Roots pod.

To create a new benchmarker, create a properties file under `conf/benchmarks`.
An example benchmarker configuration is shown below.

```
# javabook-benchmark.properties
# =============================
application=javabook
period=15
dataStore=elk
call.0.method=GET
call.0.url=http://10.0.0.1:8080/
```
This will benchmark the javabook application every 15 seconds by sending
a GET request to the http://10.0.0.1:8080/ URL. Current implementation only 
supports GET and POST methods (see `BenchmarkCall` class). Benchmark results
are stored in a data store named elk (see below).

To configure a data store, create a properties file under `conf/dataStores`.
Here's an example configuration for an ElasticSearch data store.

```
# elk.properties
# =============================
name=elk
type=ElasticSearchDataStore
host=10.0.0.2
port=9200
accessLog.index=appscale-logs
apiCall.index=appscale-apicalls
benchmark.index=appscale-benchmarks
```

Name is used to refer to this data store from other components (e.g. from a benchmarker
configuration as shown before). Three separate indices are used to store application 
access logs, SDK call data and benchmark results.

Finally, to configure an anomaly detector, create a properties file under `conf/detectors`.
An example is given below.

```
# javabook-detector.properties
# =============================
application=javabook
dataStore=elk
period=60
history=3600
detector=SLOBasedDetector
responseTimeUpperBound=32
sloPercentage=95
windowFillPercentage=75
```

This will start an anomaly detector on the javabook application. It reads the necessary
data from the elk data store. The detector runs every 60 seconds, with a sliding window
that spans 1 hour (3600 seconds). It looks for any violations of the SLO `32ms at 95%`.
The detector will not run until the sliding window is at least 75% full. That is, if we
are benchmarking the javabook application every 15 seconds, a full window should consist
of 240 data points. The detector will not activate unless there are at least 180 data
points.

## Setting Up R
Roots uses R to execute some of the more complex data anlysis tasks (e.g. relative importance).
Therefore an R language runtime must be installed alongside each Roots pod. We recommend
R 3.2.3 or higher. 

The Java-based Roots pod integrates with R through a technology called Rserve 
(https://rforge.net/Rserve/). Follow the installation instructions at https://rforge.net/Rserve/doc.html
to install Rserve. Launch it as a background process by running `R CMD Rserve`. 
This will start a local TCP/IP server. The Roots pod will communicate with the
R language runtime via this server.

## Setting up ElasticSearch
Use the `setup_elasticsearch.sh` script in the `bin` directory of the Roots pod
to configure your ElasticSearch server. For example, if you have an ElasticSearch
server running at 10.0.0.2, run the command `./setup_elasticsearch.sh 10.0.0.2`.
This will create 3 new indices in ElasticSearch, and configure their schemas
to have the proper timestamp fields. The default index names are appscale-logs, 
appscale-apicalls and appscale-benchmarks. Be cautious if you choose to change
these names. It may require changes in multiple files.

Roots uses [ElasticSearch types](https://www.elastic.co/guide/en/elasticsearch/guide/current/mapping.html) 
to store the data from different applications.
For example suppose we have applications foo, bar and baz. The benchmarking
and other monitoring data collected from these applications will be stored
as different types. For instance: `appscale-benchmarks/foo`, `appscale-benchmarks/bar`
and `appscale-benchmarks/baz`.

## Launching the Roots Pod
Once all the configurations are in place (along with ElasticSearch and R), you 
can start the pod by executing the `roots.sh` script in the `bin` subdirectory
of the pod. This will start the configured benchmarkers and detectors immediately.

```
2016-08-19 12:19:49,117 [main]  INFO RootsEnvironment Initializing Roots environment.
2016-08-19 12:19:49,135 [main]  INFO FileConfigLoader Loading data store from: /home/ubuntu/roots-1.0.0-SNAPSHOT/conf/dataStores/elk.properties
2016-08-19 12:19:49,156 [main]  INFO FileConfigLoader Loading data store from: /home/ubuntu/roots-1.0.0-SNAPSHOT/conf/dataStores/default.properties
2016-08-19 12:19:49,280 [main]  INFO FileConfigLoader Loading benchmark from: /home/ubuntu/roots-1.0.0-SNAPSHOT/conf/benchmarks/javabook.properties
2016-08-19 12:19:49,295 [main]  INFO BenchmarkingService Scheduled job for: javabook [Benchmark]
2016-08-19 12:19:49,296 [main]  INFO AnomalyLogger Initializing AnomalyLogger
2016-08-19 12:19:49,303 [main]  INFO FileConfigLoader Loading detector from: /home/ubuntu/roots-1.0.0-SNAPSHOT/conf/detectors/javabook.properties
2016-08-19 12:19:49,311 [main]  INFO AnomalyDetectorService Scheduled job for: javabook [SLOBasedDetector]
2016-08-19 12:19:49,320 [Roots-anomaly-detector-scheduler_Worker-1] DEBUG SLOBasedDetector Updating history for javabook (Fri Aug 19 11:18:49 PDT 2016 - Fri Aug 19 12:18:49 PDT 2016)
```

An example benchmarking event in the longs:

```
2016-08-19 12:19:49,420 [Roots-benchmark-scheduler_Worker-1] DEBUG Benchmark Benchmark result for javabook [GET /]: 20 ms
```

An example anomaly detection event in the logs:

```
2016-08-19 12:19:49,471 [Roots-anomaly-detector-scheduler_Worker-1] DEBUG SLOBasedDetector Calculating SLO with 229 data points.
2016-08-19 12:19:49,476 [Roots-anomaly-detector-scheduler_Worker-1]  INFO SLOBasedDetector SLO metrics. Supported: 92.5764192139738, Expected: 95.0
2016-08-19 12:19:49,481 [Roots-event-bus-2] DEBUG WorkloadAnalyzerService Loading workload history from: Fri Aug 19 10:18:49 PDT 2016, to: Fri Aug 19 12:18:49 PDT 2016
2016-08-19 12:19:49,481 [Roots-event-bus-0]  WARN AnomalyLogger Anomaly (0c9d5086-0572-44a5-a290-c50292330ae9, javabook, GET /): Detected at Fri Aug 19 12:18:49 PDT 2016: SLA satisfaction: 92.5764
```

Use the `log4j.properties` file in the `lib` directory to control how the logs are 
generated. To get a very detailed log output, add the following entry somewhere in the file:

```
log4j.logger.edu.ucsb.cs.roots=DEBUG
```
## Implementation Details
The `SLOBasedDetector` is the primary anomaly detector supported in Roots. This class 
reads benchmark results from the data store, and checks to see if a performance SLO
has been violated. This computation is very simple, and implemented entirely in Java
within the `SLOBasedDetector` class (see the `computeSLO` method). When an SLO violation
is detected, the detector invokes the `reportAnomaly` method of the parent `AnomalyDetector`
class, which triggers the anomaly handlers.

One of the most basic anomaly handlers available in Roots is the `AnomalyLogger`. This
implementation simply logs the detected anomalies:

```
2016-08-19 12:19:49,481 [Roots-event-bus-0]  WARN AnomalyLogger Anomaly (0c9d5086-0572-44a5-a290-c50292330ae9, javabook, GET /): Detected at Fri Aug 19 12:18:49 PDT 2016: SLA satisfaction: 92.5764
```
More sophisticated event handling behavior can be implemented by modifying the `AnomalyLogger`
class, or implementing a whole new anomaly handler. Each new anomaly handler must have a method
with the `@Subscribe` annotation (from [Guava](https://github.com/google/guava/wiki/EventBusExplained)), 
and each such handler must be registerd explicitly
within the `AnomalyDetectorService` class. For example:

```java
// in AnomalyLogger
@Subscribe
public void log(Anomaly anomaly) {
    anomalyLog.warn(anomaly, "Detected at {}: {}", new Date(anomaly.getEnd()),
            anomaly.getDescription());
}
```

```java
// in AnomalyDetectorService
@Override
protected void doInit() throws Exception {
    environment.subscribe(new AnomalyLogger());
    super.doInit();
}
```
Another anomaly handler that gets triggered when an anomaly is detected is the
`WorkloadAnalyzerService`. This class performs change point detection on
workload data. Workload data is inferred from the access logs gathered from
the applications. The default ElasticSearch data store configuration loads
access logs from an index named appscale-logs. The results from the change
point detection are logged as follows.

```
2016-08-19 12:19:51,167 [Roots-event-bus-2]  INFO WorkloadAnalyzerService Anomaly (0c9d5086-0572-44a5-a290-c50292330ae9, javabook, GET /): Workload level shift at Fri Aug 19 12:15:49 PDT 2016: 4.0 --> 0.3333333333333333
```

Workload unit is number of requests per unit time, where unit time is the period of the
anomaly detector. For example, if the anomaly detector runs every minute, the workload
unit will be number of requests per minute. The above sample log entry shows a drop
in workload (from 4 to 0.33). This shift is logged with the exact time of the workload
change.

The actual change point detection algorithms are implemented in the `PELTChangePointDetector`,
`BinSegChangePointDetector` and `CLChangePointDetector` classes. By default
Roots uses the PELT implementation. This can be changed by editing the `workload.analyzer`
property in the `conf/roots.properties` file of the Roots pod. All change point
detector classes must extend the `ChangePointDetector` abstract class.

Finally, Roots provides `BottleneckFinderService` -- the anomaly handler responsible
for identifying the bottleneck components in the cloud platform. By default this 
class simply calls out to the `RelativeImportanceBasedFinder` class, which 
performs the relative impotance calculations using the data gathered from the
cloud platform. In addition to computing the relative importance rankings at the 
SLO violation (`computeRankings` method), this class also computes the historical trend
of relative importance for each regressor (`analyzeHistory` method). PELT algorithm
is used to detect change points in these relative importance trends. Finally,
`RelativeImportanceBasedFinder` invokes the `PercentileBasedVerifier` to compute
some quantile metrics. The verifier computes a high quantile (0.99 by default)
on each regressor, and also the data points that exceed this high quantile.
These components log their output as follows.

`RelativeImportanceBasedFinder initial rankings`
```
2016-09-09 14:06:41,706 [Roots-event-bus-5]  INFO RelativeImportanceBasedFinder Anomaly (3eb5ea9b-dd4f-4757-9793-6fa07de2b552, j4, GET /): Relative importance metrics for path: user:CreateLoginURL, datastore_v3:RunQuery
[ 1] user:CreateLoginURL 0.499862
[ 2] datastore_v3:RunQuery 0.499449
[ 3] LOCAL 0.000690

Total variance explained: 0.9993102676179328
```

`RelativeImportanceBasedFinder relative importance trends`
```
2016-09-09 14:06:41,706 [Roots-event-bus-5] DEBUG RelativeImportanceBasedFinder Analyzing historical trend for API call user:CreateLoginURL with ranking 1
2016-09-09 14:06:41,707 [Roots-event-bus-5] DEBUG RelativeImportanceBasedFinder Performing change point analysis using 25 data points
2016-09-09 14:06:41,710 [Roots-event-bus-5]  INFO RelativeImportanceBasedFinder Anomaly (3eb5ea9b-dd4f-4757-9793-6fa07de2b552, j4, GET /): Relative importance level shift at Fri Sep 09 13:59:35 PDT 2016 for user:CreateLoginURL: 0.03308690111838919 --> 0.49877762639541867
2016-09-09 14:06:41,710 [Roots-event-bus-5]  INFO RelativeImportanceBasedFinder Anomaly (3eb5ea9b-dd4f-4757-9793-6fa07de2b552, j4, GET /): Net change in relative importance for user:CreateLoginURL: 0.03308690111838919 --> 0.49877762639541867 [1407.4776105828955%]
```

`PercentileBasedVerifier 0.99 quantiles`
```
2016-09-09 14:06:41,711 [Roots-event-bus-5]  INFO RelativeImportanceBasedFinder Anomaly (3eb5ea9b-dd4f-4757-9793-6fa07de2b552, j4, GET /): 99.0p for user:CreateLoginURL: 101.0
2016-09-09 14:06:41,711 [Roots-event-bus-5]  INFO RelativeImportanceBasedFinder Anomaly (3eb5ea9b-dd4f-4757-9793-6fa07de2b552, j4, GET /): 99.0p for datastore_v3:RunQuery: 118.97
2016-09-09 14:06:41,711 [Roots-event-bus-5]  INFO RelativeImportanceBasedFinder Anomaly (3eb5ea9b-dd4f-4757-9793-6fa07de2b552, j4, GET /): 99.0p for LOCAL: 21.879999999999995
```

`PercentileBasedVerifier point anomalies`
```
2016-09-09 14:06:41,712 [Roots-event-bus-5]  INFO RelativeImportanceBasedFinder Anomaly (3eb5ea9b-dd4f-4757-9793-6fa07de2b552, j4, GET /): Top 1 anomalous value; index: 1 timestamp: Fri Sep 09 14:02:05 PDT 2016 values: 118.97 --> 148.0
2016-09-09 14:06:41,712 [Roots-event-bus-5]  INFO RelativeImportanceBasedFinder Anomaly (3eb5ea9b-dd4f-4757-9793-6fa07de2b552, j4, GET /): Top 2 anomalous value; index: 2 timestamp: Fri Sep 09 13:45:05 PDT 2016 values: 21.879999999999995 --> 26.0
2016-09-09 14:06:41,712 [Roots-event-bus-5]  INFO RelativeImportanceBasedFinder Anomaly (3eb5ea9b-dd4f-4757-9793-6fa07de2b552, j4, GET /): Top 3 anomalous value; index: 2 timestamp: Fri Sep 09 13:40:35 PDT 2016 values: 21.879999999999995 --> 22.0
2016-09-09 14:06:41,712 [Roots-event-bus-5]  INFO RelativeImportanceBasedFinder Anomaly (3eb5ea9b-dd4f-4757-9793-6fa07de2b552, j4, GET /): Top 4 anomalous value; index: 1 timestamp: Fri Sep 09 14:02:50 PDT 2016 values: 118.97 --> 119.0
2016-09-09 14:06:41,712 [Roots-event-bus-5]  INFO RelativeImportanceBasedFinder Anomaly (3eb5ea9b-dd4f-4757-9793-6fa07de2b552, j4, GET /): Secondary verification result; percentiles: 1 percentiles2: 1 ri: 0 match: false ri_onset: true ri_top: 0 data_points: 104
```

The last log entry (Secondary verification result) summarizes the outcome of 
the bottleneck identification process.
* `percentiles`: Component with the highest 0.99 quantile
* `percentiles2`: Component with the highest outliers that exceed 0.99 quantile
* `ri`: Component chosen by performing change point detection on relative importance trends
* `ri_top`: Component with the highest relative importance when the SLO was violated

In the above example the quantile metrics have picked the SDK call no. 1 (`datastore_v3:RunQuery`)
as the bottleneck candidate. But the relative importance metrics have picked the SDK call
no. 0 (`user:CreateLoginURL`) as the bottleneck candidate.  A voting algorithm can be
implemented to determine the actual bottleneck based on these results.

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
an abstraction to integrate with different data sources. The above components
read/write data from data sources. Each data source must implement the
`DataStore` interface. The `ElasticSearchDataStore` class allows using
an ElasticSearch server as the data store.


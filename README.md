# GELF Metrics Reporter

[![Build Status](https://travis-ci.org/Graylog2/metrics-gelf.svg?branch=master)](https://travis-ci.org/Graylog2/metrics-gelf)

This is a reporter for the [Dropwizard Metrics library](http://metrics.dropwizard.io/), similar to the [Graphite](http://metrics.dropwizard.io/3.1.0/manual/graphite/) or [Ganglia](http://metrics.dropwizard.io/3.1.0/manual/ganglia/) reporters, except that it reports to a GELF-capable server, such as [Graylog](https://www.graylog.org/).

As this metrics reporter is using GELF for sending data into Graylog, the only library needed is [gelfclient](https://github.com/Graylog2/gelfclient).


## Installation

You can simply add a dependency in your `pom.xml` (or whatever dependency resolution system you might have)

```
<dependency>
  <groupId>org.graylog.metrics</groupId>
  <artifactId>metrics-gelf</artifactId>
  <version>1.0.0</version>
</dependency>
```

## Configuration

```
final MetricRegistry registry = new MetricRegistry();
GelfReporter reporter = GelfReporter.forRegistry(registry)
    .host(new InetSocketAddress("127.0.0.1", 12201))
    .build();
reporter.start(60, TimeUnit.SECONDS);
```

Define your metrics and registries as usual

```
private final Meter incomingRequestsMeter = registry.meter("incoming-http-requests");

// in your app code
incomingRequestsMeter.mark(1);
```


## Format of metrics

This is how the serialized metrics looks like in Graylog:

### Counter

```
{
  "message": "name=usa-gov-heartbearts type=COUNTER",
  "name": "usa-gov-heartbearts",
  "type": "COUNTER",
  "timestamp": "2016-07-20T09:29:58.000",
  "count": 18
}
```

### Timer

```
{
  "message" : "name=bulk-request-timer type=TIMER",
  "name" : "bulk-request-timer",
  "type" : "TIMER",
  "timestamp" : "2016-07-20T09:43:58.000",
  "count" : 114,
  "max" : 109.681,
  "mean" : 5.439666666666667,
  "median" : 5.439666666666667,
  "min" : 2.457,
  "p50" : 4.3389999999999995,
  "p75" : 5.0169999999999995,
  "p95" : 8.37175,
  "p98" : 9.6832,
  "p99" : 94.68429999999942,
  "p999" : 109.681,
  "stddev" : 9.956913151098842,
  "m15_rate" : 0.10779994503690074,
  "m1_rate" : 0.07283351433589833,
  "m5_rate" : 0.10101298115113727,
  "mean_rate" : 0.08251056571678642,
  "duration_unit" : "milliseconds",
  "rate_unit" : "second"
}
```

### Meter

```
{
  "message" : "name=usagov-incoming-requests type=METER",
  "name" : "usagov-incoming-requests",
  "type" : "METER",
  "timestamp" : "2016-07-20T09:29:58.000",
  "count" : 224,
  "m1_rate" : 0.3236309568191993,
  "m5_rate" : 0.45207208204948995,
  "m15_rate" : 0.5014348927301423,
  "mean_rate" : 0.4135529888278531,
  "rate_unit" : "second"
}
```

### Histogram

```
{
  "message" : "name=my-histgram type=HISTOGRAM",
  "name" : "my-histgram",
  "type" : "HISTOGRAM",
  "timestamp" : "2016-07-20T09:29:58.000",
  "count" : 114,
  "min" : 2.457,
  "max" : 109.681,
  "mean" : 5.439666666666667,
  "stddev" : 9.956913151098842,
  "median" : 5.439666666666667,
  "p50" : 4.3389999999999995,
  "p75" : 5.0169999999999995,
  "p95" : 8.37175,
  "p98" : 9.6832,
  "p99" : 94.68429999999942,
  "p999" : 109.681
}
```

### Gauge

```
{
  "message" : "name=usagov-incoming-requests type=GAUGE",
  "name" : "usagov-incoming-requests",
  "type" : "GAUGE",
  "timestamp" : "2016-07-20T09:29:58.000",
  "value" : 123
}
```

## License

Copyright (c) 2016 Graylog, Inc.

This library is licensed under the Apache License, Version 2.0.

See http://www.apache.org/licenses/LICENSE-2.0.html or the [LICENSE](LICENSE) file in this repository for the full license text.

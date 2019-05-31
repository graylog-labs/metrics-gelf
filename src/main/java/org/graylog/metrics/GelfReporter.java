/**
 * Copyright © 2016 Graylog, Inc. (hello@graylog.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.graylog.metrics;

import com.codahale.metrics.*;
import org.graylog2.gelfclient.GelfConfiguration;
import org.graylog2.gelfclient.GelfMessageBuilder;
import org.graylog2.gelfclient.GelfMessageLevel;
import org.graylog2.gelfclient.GelfTransports;
import org.graylog2.gelfclient.transport.GelfTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class GelfReporter extends ScheduledReporter {
    /**
     * Returns a new {@link Builder} for {@link GelfReporter}.
     *
     * @param registry the registry to report
     * @return a {@link Builder} instance for a {@link GelfReporter}
     */
    public static Builder forRegistry(MetricRegistry registry) {
        return new Builder(registry);
    }

    /**
     * A builder for {@link GelfReporter} instances. Defaults to using {@code 127.0.0.1:12201}
     * via UDP as default target, using the {@code INFO} message level,
     * converting rates to events/second, converting durations to milliseconds, and not filtering metrics.
     */
    public static class Builder {
        private final MetricRegistry registry;
        private Clock clock;
        private String prefix;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private MetricFilter filter;
        private InetSocketAddress hostAddress;
        private int queueSize;
        private boolean tlsEnabled;
        private File tlsTrustCertChainFile;
        private boolean tlsCertVerificationEnabled;
        private int reconnectDelay;
        private int connectTimeout;
        private boolean tcpNoDelay;
        private boolean tcpKeepAlive;
        private int sendBufferSize;
        private int maxInFlightSends;
        private GelfMessageLevel level;
        private String source;

        private Map<String, Object> additionalFields;
        private GelfTransports transport;
        private boolean omitNanValues;

        private Builder(MetricRegistry registry) {
            this.registry = registry;
            this.clock = Clock.defaultClock();
            this.prefix = null;
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.filter = MetricFilter.ALL;
            this.hostAddress = new InetSocketAddress("127.0.0.1", 12201);
            this.transport = GelfTransports.UDP;
            this.queueSize = 512;
            this.tlsEnabled = false;
            this.tlsTrustCertChainFile = null;
            this.tlsCertVerificationEnabled = true;
            this.reconnectDelay = 500;
            this.connectTimeout = 1000;
            this.tcpNoDelay = false;
            this.tcpKeepAlive = false;
            this.sendBufferSize = -1;
            this.maxInFlightSends = 512;
            this.level = GelfMessageLevel.INFO;
            this.source = "metrics";
            this.omitNanValues = false;
        }

        /**
         * Use the given {@link Clock} instance for the time.
         *
         * @param clock a {@link Clock} instance
         * @return {@code this}
         */
        public Builder withClock(Clock clock) {
            this.clock = requireNonNull(clock);
            return this;
        }

        /**
         * Configure a prefix for each metric name. Optional, but useful to identify single hosts.
         *
         * @param prefix The prefix for each metric name
         * @return {@code this}
         */
        public Builder prefixedWith(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Convert rates to the given time unit.
         *
         * @param rateUnit a unit of time
         * @return {@code this}
         */
        public Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = requireNonNull(rateUnit);
            return this;
        }

        /**
         * Convert durations to the given time unit.
         *
         * @param durationUnit a unit of time
         * @return {@code this}
         */
        public Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = requireNonNull(durationUnit);
            return this;
        }

        /**
         * Only report metrics which match the given filter.
         *
         * @param filter a {@link MetricFilter}
         * @return {@code this}
         */
        public Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Address of the GELF server to connect to, defaults to {@code 127.0.0.1:12201}
         *
         * @param hostAddress the address of the GELF server to connect to
         * @return {@code this}
         */
        public Builder host(InetSocketAddress hostAddress) {
            this.hostAddress = requireNonNull(hostAddress);
            return this;
        }

        /**
         * Additional fields to be included with each GELF message.
         *
         * @param additionalFields A map of additional fields to include with each GELF message
         * @return {@code this}
         */
        public Builder additionalFields(Map<String, Object> additionalFields) {
            this.additionalFields = requireNonNull(additionalFields);
            return this;
        }

        /**
         * The transport protocol used with the GELF server.
         *
         * @param transport the transport protocol used with the GELF server
         * @return {@code this}
         * @see GelfTransports
         */
        public Builder transport(final GelfTransports transport) {
            this.transport = requireNonNull(transport);
            return this;
        }

        /**
         * The size of the internally used {@link java.util.concurrent.BlockingQueue}.
         *
         * @param size the size of the internally used queue
         * @return {@code this}
         */
        public Builder queueSize(final int size) {
            queueSize = size;
            return this;
        }

        /**
         * Enable TLS for transport.
         *
         * @param tlsEnabled Whether to enable TLS
         * @return {@code this}
         */
        public Builder tlsEnabled(boolean tlsEnabled) {
            this.tlsEnabled = tlsEnabled;
            return this;
        }

        /**
         * The trust certificate chain file for the TLS connection.
         *
         * @param tlsTrustCertChainFile the trust certificate chain file
         * @return {@code this}
         */
        public Builder tlsTrustCertChainFile(final File tlsTrustCertChainFile) {
            this.tlsTrustCertChainFile = tlsTrustCertChainFile;
            return this;
        }

        /**
         * Enable TLS certificate verification for transport.
         *
         * @param tlsCertVerificationEnabled Whether to enable TLS certificate verification
         * @return {@code this}
         */
        public Builder tlsCertVerificationEnabled(boolean tlsCertVerificationEnabled) {
            this.tlsCertVerificationEnabled = tlsCertVerificationEnabled;
            return this;
        }

        /**
         * The time to wait between reconnects in milliseconds.
         *
         * @param reconnectDelay the time to wait between reconnects in milliseconds
         * @return {@code this}
         */
        public Builder reconnectDelay(final int reconnectDelay) {
            this.reconnectDelay = reconnectDelay;
            return this;
        }

        /**
         * The connection timeout for TCP connections in milliseconds.
         *
         * @param connectTimeout the connection timeout for TCP connections in milliseconds
         * @return {@code this}
         */
        public Builder connectTimeout(final int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        /**
         * Whether to use <a href="https://en.wikipedia.org/wiki/Nagle's_algorithm">Nagle's algorithm</a> for TCP connections.
         *
         * @param tcpNoDelay {@code true} if Nagle's algorithm should used for TCP connections, {@code false} otherwise
         * @return {@code this} instance
         */
        public Builder tcpNoDelay(final boolean tcpNoDelay) {
            this.tcpNoDelay = tcpNoDelay;
            return this;
        }

        /**
         * Whether to use <a href="https://en.wikipedia.org/wiki/Keepalive#TCP_keepalive">TCP keepalive</a> for TCP connections.
         *
         * @param tcpKeepAlive Whether to use TCP keepalive for TCP connections
         * @return {@code this} instance
         */
        public Builder tcpKeepAlive(final boolean tcpKeepAlive) {
            this.tcpKeepAlive = tcpKeepAlive;
            return this;
        }

        /**
         * The size of the socket send buffer in bytes.
         *
         * @param sendBufferSize the size of the socket send buffer in bytes.
         *                       A value of {@code -1} deactivates the socket send buffer.
         * @return {@code this}
         */
        public Builder sendBufferSize(final int sendBufferSize) {
            this.sendBufferSize = sendBufferSize;
            return this;
        }

        /**
         * The number of max queued network operations.
         *
         * @param maxInFlightSends max number of queued network operations
         * @return {@code this}
         */
        public Builder maxInFlightSends(int maxInFlightSends) {
            this.maxInFlightSends = maxInFlightSends;
            return this;
        }

        /**
         * The GELF message level used for reporting metrics.
         *
         * @param level GELF message level
         * @return {@code this}
         * @see GelfMessageLevel
         */
        public Builder level(GelfMessageLevel level) {
            this.level = requireNonNull(level);
            return this;
        }

        /**
         * The name of the source of the GELF messages.
         *
         * @param source the name of the source host in the GELF messages
         * @return {@code this}
         */
        public Builder source(String source) {
            this.source = requireNonNull(source);
            return this;
        }

        /**
         * Whether to omit NaN values
         *
         * @param omitNanValues {@code true} if NaN values should be omitted {@code false} otherwise
         * @return {@code this}
         */
        public Builder omitNanValues(boolean omitNanValues) {
            this.source = requireNonNull(source);
            return this;
        }

        /**
         * Builds a {@link GelfReporter} with the given properties.
         *
         * @return a {@link GelfReporter}
         */
        public GelfReporter build() {
            final GelfConfiguration configuration = new GelfConfiguration(hostAddress)
                    .transport(transport)
                    .queueSize(queueSize)
                    .maxInflightSends(maxInFlightSends)
                    .connectTimeout(connectTimeout)
                    .sendBufferSize(sendBufferSize)
                    .reconnectDelay(reconnectDelay)
                    .tcpKeepAlive(tcpKeepAlive)
                    .tcpNoDelay(tcpNoDelay);

            if (tlsEnabled) {
                configuration.enableTls();
            }

            if (tlsCertVerificationEnabled) {
                configuration.enableTlsCertVerification()
                        .tlsTrustCertChainFile(tlsTrustCertChainFile);
            }

            final GelfTransport gelfTransport = GelfTransports.create(configuration);

            return new GelfReporter(registry,
                    gelfTransport,
                    clock,
                    prefix,
                    rateUnit,
                    durationUnit,
                    filter,
                    level,
                    source,
                    additionalFields,
                    omitNanValues);
        }
    }

    private static class FieldsBuilder {
        private final Map<String, Object> map = new HashMap<>();
        private final boolean omitNanValues;

        FieldsBuilder(boolean omitNanValues) {
            this.omitNanValues = omitNanValues;
        }

        FieldsBuilder additionalFields(final Map<String, Object> additionalFields) {
            map.putAll(additionalFields);
            return this;
        }


        FieldsBuilder additionalField(String name, Object value) {
            if (omitNanValues) {
                if (value == null) {
                    return this;
                }
                if (value instanceof Double && (((Double) value).isNaN() || ((Double) value).isInfinite())) {
                    return this;
                }
            }
            map.put(name, value);
            return this;
        }

        Map<String, Object> build() {
            return map;
        }

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(GelfReporter.class);

    private final GelfTransport gelfTransport;
    private final Clock clock;
    private final String prefix;
    private final GelfMessageLevel level;
    private final String source;
    private final Map<String, Object> additionalFields;
    private final boolean omitNanValues;

    /**
     * {@inheritDoc}
     */
    GelfReporter(MetricRegistry registry, GelfTransport gelfTransport,
                 Clock clock, String prefix, TimeUnit rateUnit, TimeUnit durationUnit,
                 MetricFilter filter, GelfMessageLevel level, String source, Map<String, Object> additionalFields,
                 boolean omitNanValues) {
        super(registry, "gelf-reporter", filter, rateUnit, durationUnit);
        this.gelfTransport = requireNonNull(gelfTransport);
        this.clock = clock;
        this.prefix = prefix;
        this.level = level;
        this.source = source;
        this.additionalFields = additionalFields == null ? Collections.<String, Object>emptyMap() : new HashMap<>(additionalFields);
        this.omitNanValues = omitNanValues;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void report(SortedMap<String, Gauge> gauges,
                       SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms,
                       SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {
        if (gauges.isEmpty() && counters.isEmpty() && histograms.isEmpty() && meters.isEmpty() && timers.isEmpty()) {
            LOGGER.debug("All metrics are empty, nothing to report.");
            return;
        }

        final long timestamp = clock.getTime();

        for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
            if (entry.getValue().getValue() != null) {
                final String name = prefix(entry.getKey());
                sendGauge(timestamp, name, entry.getValue());
            }
        }

        for (Map.Entry<String, Counter> entry : counters.entrySet()) {
            final String name = prefix(entry.getKey());
            sendCounter(timestamp, name, entry.getValue());
        }

        for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
            final String name = prefix(entry.getKey());
            sendHistogram(timestamp, name, entry.getValue());
        }

        for (Map.Entry<String, Meter> entry : meters.entrySet()) {
            final String name = prefix(entry.getKey());
            sendMeter(timestamp, name, entry.getValue());
        }

        for (Map.Entry<String, Timer> entry : timers.entrySet()) {
            final String name = prefix(entry.getKey());
            sendTimer(timestamp, name, entry.getValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        super.stop();
        if (gelfTransport != null) {
            gelfTransport.stop();
        }
    }

    private void sendTimer(long timestamp, String name, Timer timer) {
        final Snapshot snapshot = timer.getSnapshot();
        final Map<String, Object> fieldsMap = new FieldsBuilder(omitNanValues)
                .additionalFields(additionalFields)
                .additionalField("name", name)
                .additionalField("type", "TIMER")
                .additionalField("count", timer.getCount())
                .additionalField("min", convertDuration(snapshot.getMin()))
                .additionalField("max", convertDuration(snapshot.getMax()))
                .additionalField("mean", convertDuration(snapshot.getMean()))
                .additionalField("stddev", convertDuration(snapshot.getStdDev()))
                .additionalField("median", convertDuration(snapshot.getMedian()))
                .additionalField("p75", convertDuration(snapshot.get75thPercentile()))
                .additionalField("p95", convertDuration(snapshot.get95thPercentile()))
                .additionalField("p98", convertDuration(snapshot.get98thPercentile()))
                .additionalField("p99", convertDuration(snapshot.get99thPercentile()))
                .additionalField("p999", convertDuration(snapshot.get999thPercentile()))
                .additionalField("duration_unit", getDurationUnit())
                .additionalField("mean_rate", convertRate(timer.getMeanRate()))
                .additionalField("m1", convertRate(timer.getOneMinuteRate()))
                .additionalField("m5", convertRate(timer.getFiveMinuteRate()))
                .additionalField("m15", convertRate(timer.getFifteenMinuteRate()))
                .additionalField("rate_unit", getRateUnit())
                .build();

        final GelfMessageBuilder message = new GelfMessageBuilder("name=" + name + " type=TIMER", source)
                .timestamp(timestamp)
                .level(level)
                .additionalFields(fieldsMap);

        gelfTransport.trySend(message.build());
    }

    private void sendMeter(long timestamp, String name, Meter meter) {
        final Map<String, Object> fieldsMap = new FieldsBuilder(omitNanValues)
                .additionalFields(additionalFields)
                .additionalField("name", name)
                .additionalField("type", "METER")
                .additionalField("count", meter.getCount())
                .additionalField("mean_rate", convertRate(meter.getMeanRate()))
                .additionalField("m1", convertRate(meter.getOneMinuteRate()))
                .additionalField("m5", convertRate(meter.getFiveMinuteRate()))
                .additionalField("m15", convertRate(meter.getFifteenMinuteRate()))
                .additionalField("rate_unit", getRateUnit())
                .build();

        final GelfMessageBuilder message = new GelfMessageBuilder("name=" + name + " type=METER", source)
                .timestamp(timestamp)
                .level(level)
                .additionalFields(fieldsMap);

        gelfTransport.trySend(message.build());
    }

    private void sendHistogram(long timestamp, String name, Histogram histogram) {
        final Snapshot snapshot = histogram.getSnapshot();
        final Map<String, Object> fieldsMap = new FieldsBuilder(omitNanValues)
                .additionalFields(additionalFields)
                .additionalField("name", name)
                .additionalField("type", "HISTOGRAM")
                .additionalField("count", histogram.getCount())
                .additionalField("min", snapshot.getMin())
                .additionalField("max", snapshot.getMax())
                .additionalField("mean", snapshot.getMean())
                .additionalField("stddev", snapshot.getStdDev())
                .additionalField("median", snapshot.getMedian())
                .additionalField("p75", snapshot.get75thPercentile())
                .additionalField("p95", snapshot.get95thPercentile())
                .additionalField("p98", snapshot.get98thPercentile())
                .additionalField("p99", snapshot.get99thPercentile())
                .additionalField("p999", snapshot.get999thPercentile())
                .build();

        final GelfMessageBuilder message = new GelfMessageBuilder("name=" + name + " type=HISTOGRAM", source)
                .timestamp(timestamp)
                .level(level)
                .additionalFields(fieldsMap);

        gelfTransport.trySend(message.build());
    }

    private void sendCounter(long timestamp, String name, Counter counter) {
        final Map<String, Object> fieldsMap = new FieldsBuilder(omitNanValues)
                .additionalFields(additionalFields)
                .additionalField("name", name)
                .additionalField("type", "COUNTER")
                .additionalField("count", counter.getCount())
                .build();
        final GelfMessageBuilder message = new GelfMessageBuilder("name=" + name + " type=COUNTER", source)
                .timestamp(timestamp)
                .level(level)
                .additionalFields(fieldsMap);

        gelfTransport.trySend(message.build());
    }

    private void sendGauge(long timestamp, String name, Gauge gauge) {
        final Map<String, Object> fieldsMap = new FieldsBuilder(omitNanValues)
                .additionalFields(additionalFields)
                .additionalField("name", name)
                .additionalField("type", "GAUGE")
                .additionalField("value", gauge.getValue())
                .build();
        final GelfMessageBuilder message = new GelfMessageBuilder("name=" + name + " type=GAUGE", source)
                .timestamp(timestamp)
                .level(level)
                .additionalFields(fieldsMap);

        gelfTransport.trySend(message.build());
    }

    private String prefix(String... components) {
        return MetricRegistry.name(prefix, components);
    }
}

/**
 * Copyright © 2016 Graylog, Inc. (hello@graylog.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.graylog.metrics;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.graylog2.gelfclient.GelfMessage;
import org.graylog2.gelfclient.GelfMessageLevel;
import org.graylog2.gelfclient.transport.GelfTransport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class GelfReporterTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Captor
    private ArgumentCaptor<GelfMessage> gelfMessageCaptor;

    @Mock
    private GelfTransport transport;

    private MetricRegistry registry;
    private GelfReporter gelfReporter;

    @Before
    public void setup() throws IOException {
        this.registry = new MetricRegistry();
        this.gelfReporter = new GelfReporter(
                registry,
                transport,
                Clock.defaultClock(),
                "prefix",
                TimeUnit.SECONDS,
                TimeUnit.MILLISECONDS,
                MetricFilter.ALL,
                GelfMessageLevel.DEBUG,
                "source",
                Collections.<String, Object>singletonMap("test", "foobar")
        );
    }

    @Test
    public void testCounter() throws Exception {
        final Counter counter = registry.counter(name("foo", "bar"));
        counter.inc(42);
        gelfReporter.report();

        verify(transport, times(1)).trySend(gelfMessageCaptor.capture());

        final GelfMessage gelfMessage = gelfMessageCaptor.getValue();
        assertThat(gelfMessage.getMessage()).isEqualTo("name=prefix.foo.bar type=COUNTER");
        assertThat(gelfMessage.getLevel()).isEqualTo(GelfMessageLevel.DEBUG);
        assertThat(gelfMessage.getHost()).isEqualTo("source");
        assertThat(gelfMessage.getAdditionalFields())
                .hasSize(4)
                .containsEntry("name", "prefix.foo.bar")
                .containsEntry("type", "COUNTER")
                .containsEntry("count", 42L)
                .containsEntry("test", "foobar");
    }

    @Test
    public void testHistogram() {
        final Histogram histogram = registry.histogram(name("foo", "bar"));
        histogram.update(20);
        histogram.update(40);
        gelfReporter.report();

        verify(transport, times(1)).trySend(gelfMessageCaptor.capture());

        final GelfMessage gelfMessage = gelfMessageCaptor.getValue();
        assertThat(gelfMessage.getMessage()).isEqualTo("name=prefix.foo.bar type=HISTOGRAM");
        assertThat(gelfMessage.getLevel()).isEqualTo(GelfMessageLevel.DEBUG);
        assertThat(gelfMessage.getHost()).isEqualTo("source");
        assertThat(gelfMessage.getAdditionalFields())
                .hasSize(14)
                .containsEntry("name", "prefix.foo.bar")
                .containsEntry("type", "HISTOGRAM")
                .containsEntry("count", 2L)
                .containsEntry("max", 40L)
                .containsEntry("min", 20L)
                .containsEntry("mean", 30.0)
                .containsEntry("test", "foobar");
    }

    @Test
    public void testMeter() {
        final Meter meter = registry.meter(name("foo", "bar"));
        meter.mark(10);
        meter.mark(20);
        gelfReporter.report();

        verify(transport, times(1)).trySend(gelfMessageCaptor.capture());

        final GelfMessage gelfMessage = gelfMessageCaptor.getValue();
        assertThat(gelfMessage.getMessage()).isEqualTo("name=prefix.foo.bar type=METER");
        assertThat(gelfMessage.getLevel()).isEqualTo(GelfMessageLevel.DEBUG);
        assertThat(gelfMessage.getHost()).isEqualTo("source");
        assertThat(gelfMessage.getAdditionalFields())
                .hasSize(9)
                .containsEntry("name", "prefix.foo.bar")
                .containsEntry("type", "METER")
                .containsEntry("count", 30L)
                .containsEntry("test", "foobar");
    }

    @Test
    public void testTimer() throws Exception {
        final Timer timer = registry.timer(name("foo", "bar"));
        timer.update(5, TimeUnit.MILLISECONDS);
        timer.update(10, TimeUnit.MILLISECONDS);
        gelfReporter.report();

        verify(transport, times(1)).trySend(gelfMessageCaptor.capture());

        final GelfMessage gelfMessage = gelfMessageCaptor.getValue();
        assertThat(gelfMessage.getMessage()).isEqualTo("name=prefix.foo.bar type=TIMER");
        assertThat(gelfMessage.getLevel()).isEqualTo(GelfMessageLevel.DEBUG);
        assertThat(gelfMessage.getHost()).isEqualTo("source");
        assertThat(gelfMessage.getAdditionalFields())
                .hasSize(20)
                .containsEntry("name", "prefix.foo.bar")
                .containsEntry("type", "TIMER")
                .containsEntry("count", 2L)
                .containsEntry("min", 5.0)
                .containsEntry("max", 10.0)
                .containsEntry("mean", 7.5)
                .containsEntry("test", "foobar");
    }

    @Test
    public void testGauge() throws Exception {
        registry.register(name("foo", "bar"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return 1234;
            }
        });
        gelfReporter.report();

        verify(transport, times(1)).trySend(gelfMessageCaptor.capture());

        final GelfMessage gelfMessage = gelfMessageCaptor.getValue();
        assertThat(gelfMessage.getMessage()).isEqualTo("name=prefix.foo.bar type=GAUGE");
        assertThat(gelfMessage.getLevel()).isEqualTo(GelfMessageLevel.DEBUG);
        assertThat(gelfMessage.getHost()).isEqualTo("source");
        assertThat(gelfMessage.getAdditionalFields())
                .hasSize(4)
                .containsEntry("name", "prefix.foo.bar")
                .containsEntry("type", "GAUGE")
                .containsEntry("value", 1234)
                .containsEntry("test", "foobar");
    }

    @Test
    public void testGracefulFailureIfNoHostIsReachable() throws IOException {
        // if no exception is thrown during the test, we consider it all graceful, as we connected to a dead host
        gelfReporter = GelfReporter.forRegistry(registry)
                .host(InetSocketAddress.createUnresolved("127.42.23.1", 12201))
                .build();
        registry.counter(name("test", "counter")).inc();
        gelfReporter.report();
    }


    @Test
    public void testStop() throws Exception {
        gelfReporter.stop();

        verify(transport, times(1)).stop();
    }
}

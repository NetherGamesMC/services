package org.nethergames.observer.server.util;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.nethergames.observer.server.Observer;

import java.time.Duration;

public class MetricsAttacher {
    private static Counter reports;
    public static PrometheusMeterRegistry attach(Observer observer) {
        PrometheusConfig config = new PrometheusConfig() {
            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public Duration step() {
                return Duration.ofSeconds(15);
            }
        };
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(config);

        registry.config().commonTags("service", "observer", "environment", Observer.getEnvironment());

        new ClassLoaderMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
        new UptimeMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);

        Metrics.addRegistry(registry);

        return registry;
    }
}

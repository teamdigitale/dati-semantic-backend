package it.gov.innovazione.ndc.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;

public class PercentileStats {
    private final List<Long> values;

    public PercentileStats(Collection<Long> values) {
        this.values = new ArrayList<>(values);
        Collections.sort(this.values);
    }

    public static <T> Collector<T, ?, PercentileStats> summarizingPercentiles(ToLongFunction<? super T> mapper) {
        return Collector.of(
                ArrayList::new,
                (list, element) -> list.add(mapper.applyAsLong(element)),
                (left, right) -> {
                    left.addAll(right);
                    return left;
                },
                PercentileStats::new
        );
    }

    public long getCount() {
        return values.size();
    }

    public long getMin() {
        return values.stream().min(Long::compareTo).orElse(0L);
    }

    public long getMax() {
        return values.stream().max(Long::compareTo).orElse(0L);
    }

    public double getAverage() {
        return values.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    public double getPercentile(double percentile) {
        if (values.isEmpty()) {
            return Double.NaN;
        }
        double index = percentile / 100.0 * (values.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) {
            return values.get(lower);
        }
        double fraction = index - lower;
        return values.get(lower) + fraction * (values.get(upper) - values.get(lower));
    }

    public double getP25() {
        return getPercentile(25);
    }

    public double getMedian() {
        return getPercentile(50);
    }

    public double getP75() {
        return getPercentile(75);
    }

    @Override
    public String toString() {
        return String.format(
                "Count: %d, Min: %d, Max: %d, Avg: %.2f, P25: %.2f, Median: %.2f, P75: %.2f",
                getCount(), getMin(), getMax(), getAverage(), getP25(), getMedian(), getP75()
        );
    }

}

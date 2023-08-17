package edu.iu.terracotta.service.app.dashboard.results.util;

import java.util.DoubleSummaryStatistics;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.google.common.util.concurrent.AtomicDouble;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class StatisticsUtils {

    /**
     * Calculate the statistics for the given list
     *
     * @param values
     * @return
     */
    public static DoubleSummaryStatistics calculateStatistics(List<Double> values) {
        DoubleSummaryStatistics statistics = new DoubleSummaryStatistics();
        Iterator<Double> iterator = values.listIterator();

        while (iterator.hasNext()) {
            statistics.accept(iterator.next());
        }

        return statistics;
    }

    /**
     * Calculate the Standard Deviation for the given list
     *
     * @param values
     * @return
     */
    public static double calculateStandardDeviation(List<Double> values) {
        return calculateStandardDeviation(values, calculateStatistics(values).getAverage());
    }

    /**
     * Calculate the Standard Deviation for the given list and mean
     *
     * @param values
     * @param mean
     * @return
     */
    public static double calculateStandardDeviation(List<Double> values, double mean) {
        if (CollectionUtils.isEmpty(values)) {
            return 0d;
        }

        AtomicDouble sum = new AtomicDouble(0.0d);

        values.stream()
            .map(value -> Math.pow(value - mean, 2))
            .forEach(value -> sum.addAndGet(value));

        return Math.sqrt(sum.get() / values.size());
    }

}

/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.util.MiscUtil.or0;

/**
 * Prints statistics in selected format.
 */
public abstract class AbstractStatisticsPrinter<T> {

    public enum Format {
        TEXT, CSV
    }

    public enum SortBy {
        NAME, COUNT, TIME
    }

    /**
     * Information that is to be formatted.
     */
    @NotNull final T information;

    @NotNull final Options options;

    /**
     * Number of iterations, e.g. objects processed.
     * Null means that this option is not applicable, or we do not want to show per-iteration information.
     */
    final Integer iterations;

    /**
     * Number of seconds to which the data are related.
     * Null means that this option is not applicable, or we do not want to show time-related information.
     */
    final Integer seconds;

    /**
     * Data to be formatted.
     */
    Data data;

    /**
     * Formatting to be used.
     */
    private Formatting formatting;

    public AbstractStatisticsPrinter(@NotNull T information, Options options, Integer iterations, Integer seconds) {
        this.information = information;
        this.options = options != null ? options : new Options();
        this.iterations = iterations;
        this.seconds = seconds;
    }

    protected void initData() {
        if (data == null) {
            data = new Data();
        } else {
            throw new IllegalStateException("data already created");
        }
    }

    protected void initFormatting() {
        if (formatting == null) {
            if (isCsv()) {
                formatting = new CsvFormatting();
            } else {
                formatting = new AsciiTableFormatting();
            }
        } else {
            throw new IllegalStateException("formatting already created");
        }
    }

    protected boolean isCsv() {
        return options.format == Format.CSV;
    }

    protected String applyFormatting() {
        return formatting.apply(data);
    }

    void addColumn(String label, Formatting.Alignment alignment, String format) {
        formatting.addColumn(label, alignment, format);
    }

    protected Number avg(Number total, Integer countObject) {
        int count = or0(countObject);
        return total != null && count > 0 ? total.floatValue() / count : null;
    }

    protected Number div(Number total, Integer countObject) {
        int count = or0(countObject);
        return total != null && count > 0 ? total.floatValue() / count : null;
    }

    protected Number percent(Long value, Long baseObject) {
        long base = or0(baseObject);
        if (value == null) {
            return null;
        } else if (base != 0) {
            return 100.0f * value / base;
        } else if (value == 0) {
            return 0.0f;
        } else {
            return Float.NaN;
        }
    }

    int zeroIfNull(Integer n) {
        return n != null ? n : 0;
    }

    long zeroIfNull(Long n) {
        return n != null ? n : 0;
    }

    protected <X> X nullIfFalse(boolean condition, X value) {
        return condition ? value : null;
    }

    public static class Options {
        final Format format;
        final SortBy sortBy;

        public Options() {
            this(null, null);
        }

        public Options(Format format, SortBy sortBy) {
            this.format = format;
            this.sortBy = sortBy;
        }
    }

    String formatString() {
        return "%s";
    }

    String formatInt() {
        return isCsv() ? "%d" : "%,d";
    }

    String formatFloat1() {
        return isCsv() ? "%f" : "%,.1f";
    }

    String formatPercent1() {
        return isCsv() ? "%f%%" : "%.1f%%";
    }

    String formatPercent2() {
        return isCsv() ? "%f%%" : "%.2f%%";
    }
}

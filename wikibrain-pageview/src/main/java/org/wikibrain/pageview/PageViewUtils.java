package org.wikibrain.pageview;

import org.joda.time.DateTime;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Shilad Sen
 */
public class PageViewUtils {
    public static SortedSet<DateTime> timestampsInInterval(DateTime start, DateTime end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException();
        }
        DateTime current = new DateTime(
                start.year().get(),
                start.monthOfYear().get(),
                start.dayOfMonth().get(),
                start.hourOfDay().get(),
                0);
        if (current.isBefore(start)) {
            current = current.plusHours(1);
        }
        SortedSet<DateTime> result = new TreeSet<DateTime>();
        while (!current.isAfter(end)) {
            result.add(current);
            current = current.plusHours(1);
        }
        return result;
    }
}

package com.example.flipandfind;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.TimeZone;

/** Pure local-time grouping for the game-history screen. */
public final class GameHistoryGrouping {
    public enum Period {
        TODAY("Today"),
        YESTERDAY("Yesterday"),
        THIS_WEEK("This week"),
        THIS_MONTH("This month"),
        OLDER("Older");

        private final String displayName;

        Period(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /** A retained entry plus its stable index in the archive snapshot. */
    public static final class GroupedEntry {
        private final GameHistoryEntry entry;
        private final int retainedIndex;

        private GroupedEntry(GameHistoryEntry entry, int retainedIndex) {
            this.entry = entry;
            this.retainedIndex = retainedIndex;
        }

        public GameHistoryEntry getEntry() {
            return entry;
        }

        public int getRetainedIndex() {
            return retainedIndex;
        }
    }

    public static final class Section {
        private final Period period;
        private final List<GroupedEntry> entries;

        private Section(Period period, List<GroupedEntry> entries) {
            this.period = period;
            this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
        }

        public Period getPeriod() {
            return period;
        }

        public List<GroupedEntry> getEntries() {
            return entries;
        }
    }

    private GameHistoryGrouping() {
    }

    /**
     * Groups retained games using the supplied local timezone. Weeks start on Monday and all
     * sections are non-overlapping; yesterday remains Yesterday across week/month boundaries.
     */
    public static List<Section> group(
        List<GameHistoryEntry> retainedEntries,
        long nowEpochMillis,
        TimeZone timeZone
    ) {
        if (retainedEntries == null) {
            throw new IllegalArgumentException("History entries are required");
        }
        if (timeZone == null) {
            throw new IllegalArgumentException("A local timezone is required");
        }

        Calendar startTodayCalendar = Calendar.getInstance(timeZone);
        startTodayCalendar.setTimeInMillis(nowEpochMillis);
        startTodayCalendar.set(Calendar.HOUR_OF_DAY, 0);
        startTodayCalendar.set(Calendar.MINUTE, 0);
        startTodayCalendar.set(Calendar.SECOND, 0);
        startTodayCalendar.set(Calendar.MILLISECOND, 0);

        Calendar startYesterdayCalendar = (Calendar) startTodayCalendar.clone();
        startYesterdayCalendar.add(Calendar.DAY_OF_MONTH, -1);

        Calendar startWeekCalendar = (Calendar) startTodayCalendar.clone();
        int daysSinceMonday = (startWeekCalendar.get(Calendar.DAY_OF_WEEK)
            - Calendar.MONDAY + 7) % 7;
        startWeekCalendar.add(Calendar.DAY_OF_MONTH, -daysSinceMonday);

        Calendar startMonthCalendar = (Calendar) startTodayCalendar.clone();
        startMonthCalendar.set(Calendar.DAY_OF_MONTH, 1);

        long startToday = startTodayCalendar.getTimeInMillis();
        long startYesterday = startYesterdayCalendar.getTimeInMillis();
        long startWeek = startWeekCalendar.getTimeInMillis();
        long startMonth = startMonthCalendar.getTimeInMillis();

        ArrayList<GroupedEntry> sorted = new ArrayList<>(retainedEntries.size());
        for (int index = 0; index < retainedEntries.size(); index++) {
            GameHistoryEntry entry = retainedEntries.get(index);
            if (entry == null) {
                throw new IllegalArgumentException("History entries cannot be null");
            }
            sorted.add(new GroupedEntry(entry, index));
        }
        Collections.sort(sorted, new Comparator<GroupedEntry>() {
            @Override
            public int compare(GroupedEntry left, GroupedEntry right) {
                return Long.compare(
                    right.entry.getCompletedAtEpochMillis(),
                    left.entry.getCompletedAtEpochMillis()
                );
            }
        });

        EnumMap<Period, ArrayList<GroupedEntry>> buckets = new EnumMap<>(Period.class);
        for (Period period : Period.values()) {
            buckets.put(period, new ArrayList<>());
        }
        for (GroupedEntry groupedEntry : sorted) {
            long completedAt = groupedEntry.entry.getCompletedAtEpochMillis();
            Period period;
            if (completedAt >= startToday) {
                period = Period.TODAY;
            } else if (completedAt >= startYesterday) {
                period = Period.YESTERDAY;
            } else if (completedAt >= startWeek) {
                period = Period.THIS_WEEK;
            } else if (completedAt >= startMonth) {
                period = Period.THIS_MONTH;
            } else {
                period = Period.OLDER;
            }
            buckets.get(period).add(groupedEntry);
        }

        ArrayList<Section> result = new ArrayList<>();
        for (Period period : Period.values()) {
            List<GroupedEntry> entries = buckets.get(period);
            if (!entries.isEmpty()) {
                result.add(new Section(period, entries));
            }
        }
        return Collections.unmodifiableList(result);
    }
}

package com.example.flipandfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import org.junit.Test;

public final class GameHistoryGroupingTest {
    private static final TimeZone KOLKATA = TimeZone.getTimeZone("Asia/Kolkata");

    @Test
    public void groupsIntoNonOverlappingLocalPeriodsAndSortsNewestFirst() {
        long now = localMillis(KOLKATA, 2026, Calendar.JULY, 8, 12, 0);
        GameHistoryEntry older = entry(localMillis(
            KOLKATA, 2026, Calendar.JUNE, 30, 23, 59
        ));
        GameHistoryEntry today = entry(localMillis(
            KOLKATA, 2026, Calendar.JULY, 8, 9, 0
        ));
        GameHistoryEntry month = entry(localMillis(
            KOLKATA, 2026, Calendar.JULY, 1, 8, 0
        ));
        GameHistoryEntry yesterday = entry(localMillis(
            KOLKATA, 2026, Calendar.JULY, 7, 16, 0
        ));
        GameHistoryEntry week = entry(localMillis(
            KOLKATA, 2026, Calendar.JULY, 6, 7, 0
        ));

        List<GameHistoryGrouping.Section> sections = GameHistoryGrouping.group(
            Arrays.asList(older, today, month, yesterday, week),
            now,
            KOLKATA
        );

        assertEquals(5, sections.size());
        assertSection(sections.get(0), GameHistoryGrouping.Period.TODAY, today, 1);
        assertSection(sections.get(1), GameHistoryGrouping.Period.YESTERDAY, yesterday, 3);
        assertSection(sections.get(2), GameHistoryGrouping.Period.THIS_WEEK, week, 4);
        assertSection(sections.get(3), GameHistoryGrouping.Period.THIS_MONTH, month, 2);
        assertSection(sections.get(4), GameHistoryGrouping.Period.OLDER, older, 0);
    }

    @Test
    public void exactLocalMidnightBoundariesBelongToTheNewerSection() {
        long now = localMillis(KOLKATA, 2026, Calendar.JULY, 8, 12, 0);
        GameHistoryEntry today = entry(localMillis(
            KOLKATA, 2026, Calendar.JULY, 8, 0, 0
        ));
        GameHistoryEntry yesterday = entry(localMillis(
            KOLKATA, 2026, Calendar.JULY, 7, 0, 0
        ));
        GameHistoryEntry week = entry(localMillis(
            KOLKATA, 2026, Calendar.JULY, 6, 0, 0
        ));
        GameHistoryEntry month = entry(localMillis(
            KOLKATA, 2026, Calendar.JULY, 1, 0, 0
        ));
        GameHistoryEntry older = entry(localMillis(
            KOLKATA, 2026, Calendar.JUNE, 30, 23, 59
        ));

        List<GameHistoryGrouping.Section> sections = GameHistoryGrouping.group(
            Arrays.asList(today, yesterday, week, month, older),
            now,
            KOLKATA
        );

        assertEquals(GameHistoryGrouping.Period.TODAY, sections.get(0).getPeriod());
        assertEquals(GameHistoryGrouping.Period.YESTERDAY, sections.get(1).getPeriod());
        assertEquals(GameHistoryGrouping.Period.THIS_WEEK, sections.get(2).getPeriod());
        assertEquals(GameHistoryGrouping.Period.THIS_MONTH, sections.get(3).getPeriod());
        assertEquals(GameHistoryGrouping.Period.OLDER, sections.get(4).getPeriod());
    }

    @Test
    public void yesterdayUsesDeviceTimezoneRatherThanUtcDate() {
        long now = localMillis(KOLKATA, 2026, Calendar.JULY, 8, 0, 15);
        GameHistoryEntry localYesterday = entry(localMillis(
            KOLKATA, 2026, Calendar.JULY, 7, 23, 59
        ));

        List<GameHistoryGrouping.Section> sections = GameHistoryGrouping.group(
            Arrays.asList(localYesterday),
            now,
            KOLKATA
        );

        assertEquals(1, sections.size());
        assertEquals(GameHistoryGrouping.Period.YESTERDAY, sections.get(0).getPeriod());
    }

    @Test
    public void multipleGamesWithinSectionRemainNewestFirst() {
        long now = localMillis(KOLKATA, 2026, Calendar.JULY, 8, 20, 0);
        GameHistoryEntry morning = entry(localMillis(
            KOLKATA, 2026, Calendar.JULY, 8, 8, 0
        ));
        GameHistoryEntry evening = entry(localMillis(
            KOLKATA, 2026, Calendar.JULY, 8, 18, 0
        ));

        List<GameHistoryGrouping.GroupedEntry> grouped = GameHistoryGrouping.group(
            Arrays.asList(morning, evening),
            now,
            KOLKATA
        ).get(0).getEntries();

        assertSame(evening, grouped.get(0).getEntry());
        assertEquals(1, grouped.get(0).getRetainedIndex());
        assertSame(morning, grouped.get(1).getEntry());
        assertEquals(0, grouped.get(1).getRetainedIndex());
    }

    private static void assertSection(
        GameHistoryGrouping.Section section,
        GameHistoryGrouping.Period expectedPeriod,
        GameHistoryEntry expectedEntry,
        int expectedRetainedIndex
    ) {
        assertEquals(expectedPeriod, section.getPeriod());
        assertEquals(1, section.getEntries().size());
        assertSame(expectedEntry, section.getEntries().get(0).getEntry());
        assertEquals(expectedRetainedIndex, section.getEntries().get(0).getRetainedIndex());
    }

    private static long localMillis(
        TimeZone timeZone,
        int year,
        int month,
        int day,
        int hour,
        int minute
    ) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.clear();
        calendar.set(year, month, day, hour, minute, 0);
        return calendar.getTimeInMillis();
    }

    private static GameHistoryEntry entry(long completedAt) {
        return new GameHistoryEntry(
            completedAt,
            1_000L,
            1,
            new String[] {"Player A"},
            new int[] {1},
            new int[] {100},
            new int[] {1}
        );
    }
}

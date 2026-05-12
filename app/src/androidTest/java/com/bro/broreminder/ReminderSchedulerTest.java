package com.bro.broreminder;

import static org.junit.Assert.*;

import com.bro.broreminder.reminder.ReminderScheduler;

import java.util.Calendar;

import org.junit.Test;

public class ReminderSchedulerTest {
    private long expectedOccurrence(int dayOfWeek, String time) {
        String[] parts = time.split(":");
        int h = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]);

        Calendar now = Calendar.getInstance();
        int currentDay = now.get(Calendar.DAY_OF_WEEK);
        int targetDay = (Calendar.MONDAY - 1 + dayOfWeek) % 7 + 1;
        int diff = targetDay - currentDay;
        if (diff < 0) diff += 7;
        now.add(Calendar.DAY_OF_MONTH, diff);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        now.set(Calendar.HOUR_OF_DAY, h);
        now.set(Calendar.MINUTE, m);
        if (now.getTimeInMillis() <= System.currentTimeMillis()) {
            now.add(Calendar.DAY_OF_MONTH, 7);
        }
        return now.getTimeInMillis();
    }

    private int currentDayIndex() {
        int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        return (currentDay + 5) % 7;
    }

    @Test
    public void testSameDayFutureTime() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MINUTE, 5);
        String time = String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
        int day = currentDayIndex();
        long expected = expectedOccurrence(day, time);
        long actual = ReminderScheduler.computeNextOccurrence(day, time);
        assertEquals(expected, actual);
    }

    @Test
    public void testSameDayPastTimeSchedulesNextWeek() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MINUTE, -5);
        String time = String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
        int day = currentDayIndex();
        long expected = expectedOccurrence(day, time);
        long actual = ReminderScheduler.computeNextOccurrence(day, time);
        assertEquals(expected, actual);
        assertTrue(expected - System.currentTimeMillis() >= 6L * 24 * 60 * 60 * 1000);
    }

    @Test
    public void testFutureDay() {
        String time = "08:30";
        int day = (currentDayIndex() + 2) % 7;
        long expected = expectedOccurrence(day, time);
        long actual = ReminderScheduler.computeNextOccurrence(day, time);
        assertEquals(expected, actual);
    }

    @Test
    public void testInvalidTimeReturnsSoon() {
        long before = System.currentTimeMillis();
        long actual = ReminderScheduler.computeNextOccurrence(currentDayIndex(), "bad");
        long diff = actual - before;
        assertTrue(diff >= 60000 && diff < 120000);
    }
    @Test
    public void testAllDaysAllTimes() {
        for (int day = 0; day < 7; day++) {
            for (int hour = 0; hour < 24; hour++) {
                for (int minute : new int[] {0, 30}) {
                    String time = String.format("%02d:%02d", hour, minute);
                    long expected = expectedOccurrence(day, time);
                    long actual = ReminderScheduler.computeNextOccurrence(day, time);
                    assertEquals("Day " + day + " time " + time, expected, actual);
                }
            }
        }
    }
}

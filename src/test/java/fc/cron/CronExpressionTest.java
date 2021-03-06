/*
 * Copyright (C) 2012 Frode Carlsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fc.cron;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.Hours;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

public class CronExpressionTest {
    DateTimeZone original;

    @Before
    public void setUp() {
        original = DateTimeZone.getDefault();
        DateTimeZone.setDefault(DateTimeZone.forID("Europe/Oslo"));
    }

    @After
    public void tearDown() {
        DateTimeZone.setDefault(original);
    }

    @Test
    public void shall_parse_number() throws Exception {
        SimpleField field = new SimpleField(CronFieldType.MINUTE, "5");
        assertPossibleValues(field, 5);
    }

    private void assertPossibleValues(BasicField field, Integer... values) {
        Set<Integer> valid = values == null ? Collections.<Integer>emptySet() : new HashSet<Integer>(Arrays.asList(values));
        for (int i = field.fieldType.getFrom(); i <= field.fieldType.getTo(); i++) {
            String errorText = String.format("%d in %s", i, valid);
            if (valid.contains(i)) {
                assertThat(field.matches(i)).as(errorText).isTrue();
            } else {
                assertThat(field.matches(i)).as(errorText).isFalse();
            }
        }
    }

    @Test
    public void shall_parse_number_with_increment() throws Exception {
        SimpleField field = new SimpleField(CronFieldType.MINUTE, "0/15");
        assertPossibleValues(field, 0, 15, 30, 45);
    }

    @Test
    public void shall_parse_range() throws Exception {
        SimpleField field = new SimpleField(CronFieldType.MINUTE, "5-10");
        assertPossibleValues(field, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void shall_parse_range_with_increment() throws Exception {
        SimpleField field = new SimpleField(CronFieldType.MINUTE, "20-30/2");
        assertPossibleValues(field, 20, 22, 24, 26, 28, 30);
    }

    @Test
    public void shall_parse_range_with_rolling_period() throws Exception {
        assertPossibleValues(new DayOfWeekField("SAT-MON", false), 1, 6, 7);
        assertPossibleValues(new DayOfWeekField("FRI-MON", false), 1, 5, 6, 7);
        assertPossibleValues(new DayOfWeekField("SUN-SAT", true), 1, 2, 3, 4, 5, 6, 7);
    }

    @Test
    public void shall_parse_range_with_increment_for_day_of_week() throws Exception {
        assertPossibleValues(new DayOfWeekField("SUN-SAT/5", true), DateTimeConstants.FRIDAY, DateTimeConstants.SUNDAY);
        assertPossibleValues(new DayOfWeekField("SUN-SAT/5", false), DateTimeConstants.FRIDAY, DateTimeConstants.SUNDAY);

        assertPossibleValues(new DayOfWeekField("1-6/5", true), DateTimeConstants.FRIDAY, DateTimeConstants.SUNDAY);
        assertPossibleValues(new DayOfWeekField("1-6/5", false), DateTimeConstants.MONDAY, DateTimeConstants.SATURDAY);

        assertPossibleValues(new DayOfWeekField("1/5", true), DateTimeConstants.FRIDAY, DateTimeConstants.SUNDAY);
        assertPossibleValues(new DayOfWeekField("1/5", false), DateTimeConstants.MONDAY, DateTimeConstants.SATURDAY);

        assertPossibleValues(new DayOfWeekField("*/5", true), DateTimeConstants.FRIDAY, DateTimeConstants.SUNDAY);
        assertPossibleValues(new DayOfWeekField("*/5", false), DateTimeConstants.MONDAY, DateTimeConstants.SATURDAY);
    }

    @Test
    public void shall_parse_range_with_increment2() throws Exception {
        assertPossibleValues(new SimpleField(CronFieldType.MINUTE, "*/3"),
                0, 3, 6, 9, 12, 15, 18, 21, 24, 27, 30, 33, 36, 39, 42, 45, 48, 51, 54, 57);

        assertPossibleValues(new DayOfMonthField("*/3"), 1, 4, 7, 10, 13, 16, 19, 22, 25, 28, 31);

        assertPossibleValues(new DayOfMonthField("*/5"), 1, 6, 11, 16, 21, 26, 31);

        assertPossibleValues(new SimpleField(CronFieldType.MONTH, "7/6"), 7);
    }

    @Test
    public void shall_parse_asterix() throws Exception {
        assertPossibleValues(new DayOfWeekField("*"), 1, 2, 3, 4, 5, 6, 7);
    }

    @Test
    public void shall_parse_asterix_with_increment() throws Exception {
        assertPossibleValues(new DayOfWeekField("*/2"), 1, 3, 5, 7);
    }

    @Test
    public void shall_ignore_field_in_day_of_week() throws Exception {
        DayOfWeekField field = new DayOfWeekField("?");
        assertThat(field.matches(new LocalDate())).isTrue();
    }

    @Test
    public void shall_ignore_field_in_day_of_month() throws Exception {
        DayOfMonthField field = new DayOfMonthField("?");
        assertThat(field.matches(new LocalDate())).isTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shall_give_error_if_invalid_count_field() throws Exception {
        new CronExpression("* 3 *");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shall_give_error_if_minute_field_ignored() throws Exception {
        SimpleField field = new SimpleField(CronFieldType.MINUTE, "?");
        field.matches(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shall_give_error_if_hour_field_ignored() throws Exception {
        SimpleField field = new SimpleField(CronFieldType.HOUR, "?");
        field.matches(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shall_give_error_if_month_field_ignored() throws Exception {
        SimpleField field = new SimpleField(CronFieldType.MONTH, "?");
        field.matches(1);
    }

    @Test
    public void shall_give_last_day_of_month_in_leapyear() throws Exception {
        DayOfMonthField field = new DayOfMonthField("L");
        assertThat(field.matches(new LocalDate(2012, 02, 29))).isTrue();
    }

    @Test
    public void shall_give_last_day_of_month() throws Exception {
        DayOfMonthField field = new DayOfMonthField("L");
        assertThat(field.matches(new LocalDate().withDayOfMonth(new LocalDate().dayOfMonth().getMaximumValue()))).isTrue();
    }

    @Test
    public void check_all() throws Exception {
        assertThat(new CronExpression("* * * * * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 00, 01))).isEqualTo(new DateTime(2012, 4, 10, 13, 00, 02));
        assertThat(new CronExpression("* * * * * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 02))).isEqualTo(new DateTime(2012, 4, 10, 13, 02, 01));
        assertThat(new CronExpression("* * * * * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 59, 59))).isEqualTo(new DateTime(2012, 4, 10, 14, 00));
    }

    @Test(expected = IllegalArgumentException.class)
    public void check_invalid_input() throws Exception {
        new CronExpression(null);
    }

    @Test
    public void check_second_number() throws Exception {
        assertThat(new CronExpression("3 * * * * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 01))).isEqualTo(new DateTime(2012, 4, 10, 13, 01, 03));
        assertThat(new CronExpression("3 * * * * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 01, 03))).isEqualTo(new DateTime(2012, 4, 10, 13, 02, 03));
        assertThat(new CronExpression("3 * * * * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 59, 03))).isEqualTo(new DateTime(2012, 4, 10, 14, 00, 03));
        assertThat(new CronExpression("3 * * * * *").nextTimeAfter(new DateTime(2012, 4, 10, 23, 59, 03))).isEqualTo(new DateTime(2012, 4, 11, 00, 00, 03));
        assertThat(new CronExpression("3 * * * * *").nextTimeAfter(new DateTime(2012, 4, 30, 23, 59, 03))).isEqualTo(new DateTime(2012, 5, 01, 00, 00, 03));
    }

    @Test
    public void check_second_increment() throws Exception {
        CronExpression cron = new CronExpression("5/15 * * * * *");
        assertThat(cron.nextTimeAfter(new DateTime(2012, 4, 10, 13, 00))).isEqualTo(new DateTime(2012, 4, 10, 13, 00, 05));
        assertThat(cron.nextTimeAfter(new DateTime(2012, 4, 10, 13, 00, 05))).isEqualTo(new DateTime(2012, 4, 10, 13, 00, 20));
        assertThat(cron.nextTimeAfter(new DateTime(2012, 4, 10, 13, 00, 20))).isEqualTo(new DateTime(2012, 4, 10, 13, 00, 35));
        assertThat(cron.nextTimeAfter(new DateTime(2012, 4, 10, 13, 00, 35))).isEqualTo(new DateTime(2012, 4, 10, 13, 00, 50));
        assertThat(cron.nextTimeAfter(new DateTime(2012, 4, 10, 13, 00, 50))).isEqualTo(new DateTime(2012, 4, 10, 13, 01, 05));

        // if rolling over minute then reset second (cron rules - increment affects only values in own field)
        cron = new CronExpression("10/100 * * * * *");
        assertThat(cron.nextTimeAfter(new DateTime(2012, 4, 10, 13, 00, 50)))
                .isEqualTo(new DateTime(2012, 4, 10, 13, 01, 10));
        assertThat(cron.nextTimeAfter(new DateTime(2012, 4, 10, 13, 01, 10)))
                .isEqualTo(new DateTime(2012, 4, 10, 13, 02, 10));
    }

    @Test
    public void check_second_list() throws Exception {
        assertThat(new CronExpression("7,19 * * * * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 00))).isEqualTo(new DateTime(2012, 4, 10, 13, 00, 07));
        assertThat(new CronExpression("7,19 * * * * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 00, 07))).isEqualTo(new DateTime(2012, 4, 10, 13, 00, 19));
        assertThat(new CronExpression("7,19 * * * * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 00, 19))).isEqualTo(new DateTime(2012, 4, 10, 13, 01, 07));
    }

    @Test
    public void check_second_range() throws Exception {
        assertThat(new CronExpression("42-45 * * * * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 00))).isEqualTo(new DateTime(2012, 4, 10, 13, 00, 42));
        assertThat(new CronExpression("42-45 * * * * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 00, 42))).isEqualTo(new DateTime(2012, 4, 10, 13, 00, 43));
        assertThat(new CronExpression("42-45 * * * * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 00, 43))).isEqualTo(new DateTime(2012, 4, 10, 13, 00, 44));
        assertThat(new CronExpression("42-45 * * * * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 00, 44))).isEqualTo(new DateTime(2012, 4, 10, 13, 00, 45));
        assertThat(new CronExpression("42-45 * * * * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 00, 45))).isEqualTo(new DateTime(2012, 4, 10, 13, 01, 42));
    }

    @Test(expected = IllegalArgumentException.class)
    public void check_second_invalid_range() throws Exception {
        new CronExpression("42-63 * * * * *");
    }

    @Test(expected = IllegalArgumentException.class)
    public void check_second_invalid_increment_modifier() throws Exception {
        new CronExpression("42#3 * * * * *");
    }

    @Test
    public void check_minute_number() throws Exception {
        assertThat(new CronExpression("0 3 * * * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 01))).isEqualTo(new DateTime(2012, 4, 10, 13, 03));
        assertThat(new CronExpression("0 3 * * * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 03))).isEqualTo(new DateTime(2012, 4, 10, 14, 03));
    }

    @Test
    public void check_minute_increment() throws Exception {
        assertThat(new CronExpression("0 0/15 * * * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 00))).isEqualTo(new DateTime(2012, 4, 10, 13, 15));
        assertThat(new CronExpression("0 0/15 * * * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 15))).isEqualTo(new DateTime(2012, 4, 10, 13, 30));
        assertThat(new CronExpression("0 0/15 * * * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 30))).isEqualTo(new DateTime(2012, 4, 10, 13, 45));
        assertThat(new CronExpression("0 0/15 * * * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 45))).isEqualTo(new DateTime(2012, 4, 10, 14, 00));
    }

    @Test
    public void check_minute_list() throws Exception {
        assertThat(new CronExpression("0 7,19 * * * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 00))).isEqualTo(new DateTime(2012, 4, 10, 13, 07));
        assertThat(new CronExpression("0 7,19 * * * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 07))).isEqualTo(new DateTime(2012, 4, 10, 13, 19));
    }

    @Test
    public void check_hour_number() throws Exception {
        assertThat(new CronExpression("0 * 3 * * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 01))).isEqualTo(new DateTime(2012, 4, 11, 03, 00));
        assertThat(new CronExpression("0 * 3 * * *").nextTimeAfter(new DateTime(2012, 4, 11, 03, 00))).isEqualTo(new DateTime(2012, 4, 11, 03, 01));
        assertThat(new CronExpression("0 * 3 * * *").nextTimeAfter(new DateTime(2012, 4, 11, 03, 59))).isEqualTo(new DateTime(2012, 4, 12, 03, 00));
    }

    @Test
    public void check_hour_increment() throws Exception {
        assertThat(new CronExpression("0 * 0/15 * * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 00))).isEqualTo(new DateTime(2012, 4, 10, 15, 00));
        assertThat(new CronExpression("0 * 0/15 * * *").nextTimeAfter(new DateTime(2012, 4, 10, 15, 00))).isEqualTo(new DateTime(2012, 4, 10, 15, 01));
        assertThat(new CronExpression("0 * 0/15 * * *").nextTimeAfter(new DateTime(2012, 4, 10, 15, 59))).isEqualTo(new DateTime(2012, 4, 11, 00, 00));
        assertThat(new CronExpression("0 * 0/15 * * *").nextTimeAfter(new DateTime(2012, 4, 11, 00, 00))).isEqualTo(new DateTime(2012, 4, 11, 00, 01));
        assertThat(new CronExpression("0 * 0/15 * * *").nextTimeAfter(new DateTime(2012, 4, 11, 15, 00))).isEqualTo(new DateTime(2012, 4, 11, 15, 01));
    }

    @Test
    public void check_hour_list() throws Exception {
        assertThat(new CronExpression("0 * 7,19 * * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 00))).isEqualTo(new DateTime(2012, 4, 10, 19, 00));
        assertThat(new CronExpression("0 * 7,19 * * *").nextTimeAfter(new DateTime(2012, 4, 10, 19, 00))).isEqualTo(new DateTime(2012, 4, 10, 19, 01));
        assertThat(new CronExpression("0 * 7,19 * * *").nextTimeAfter(new DateTime(2012, 4, 10, 19, 59))).isEqualTo(new DateTime(2012, 4, 11, 07, 00));
    }

    @Test
    public void check_hour_shall_run_25_times_in_DST_change_to_wintertime() throws Exception {
        CronExpression cron = new CronExpression("0 1 * * * *");
        DateTime start = new DateTime(2011, 10, 30, 0, 0, 0, 0);
        DateTime slutt = start.toLocalDate().plusDays(1).toDateTimeAtStartOfDay();
        DateTime tid = start;
        assertThat(Hours.hoursBetween(start, slutt).getHours()).isEqualTo(25);
        int count=0;
        DateTime lastTime = tid;
        while(tid.isBefore(slutt)){
            DateTime nextTime = cron.nextTimeAfter(tid);
            assertThat(nextTime.isAfter(lastTime)).isTrue();
            lastTime = nextTime;
            tid = tid.plusHours(1);
            count++;
        }
        assertThat(count).isEqualTo(25);
    }

    @Test
    public void check_hour_shall_run_23_times_in_DST_change_to_summertime() throws Exception {
        CronExpression cron = new CronExpression("0 0 * * * *");
        DateTime start = new DateTime(2011, 03, 27, 0, 0, 0, 0);
        DateTime slutt = start.toLocalDate().plusDays(1).toDateTimeAtStartOfDay();
        DateTime tid = start;
        assertThat(Hours.hoursBetween(start, slutt).getHours()).isEqualTo(23);
        int count=0;
        DateTime lastTime = tid;
        while(tid.isBefore(slutt)){
            DateTime nextTime = cron.nextTimeAfter(tid);
            assertThat(nextTime.isAfter(lastTime)).isTrue();
            lastTime = nextTime;
            tid = tid.plusHours(1);
            count++;
        }
        assertThat(count).isEqualTo(23);
    }

    @Test
    public void check_dayOfMonth_number() throws Exception {
        assertThat(new CronExpression("0 * * 3 * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 00))).isEqualTo(new DateTime(2012, 5, 03, 00, 00));
        assertThat(new CronExpression("0 * * 3 * *").nextTimeAfter(new DateTime(2012, 5, 03, 00, 00))).isEqualTo(new DateTime(2012, 5, 03, 00, 01));
        assertThat(new CronExpression("0 * * 3 * *").nextTimeAfter(new DateTime(2012, 5, 03, 00, 59))).isEqualTo(new DateTime(2012, 5, 03, 01, 00));
        assertThat(new CronExpression("0 * * 3 * *").nextTimeAfter(new DateTime(2012, 5, 03, 23, 59))).isEqualTo(new DateTime(2012, 6, 03, 00, 00));
    }

    @Test
    public void check_dayOfMonthField_number() throws Exception {
        DayOfMonthField field = new DayOfMonthField("3");
        assertThat(field.nextDate(new LocalDate(2012, 4, 10))).isEqualTo(new LocalDate(2012, 5, 3));
        assertThat(field.nextDate(new LocalDate(2012, 5, 03))).isEqualTo(new LocalDate(2012, 6, 3));
    }

    @Test
    public void check_dayOfMonth_increment() throws Exception {
        assertThat(new CronExpression("0 0 0 1/15 * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 00))).isEqualTo(new DateTime(2012, 4, 16, 00, 00));
        assertThat(new CronExpression("0 0 0 1/15 * *").nextTimeAfter(new DateTime(2012, 4, 16, 00, 00))).isEqualTo(new DateTime(2012, 5, 01, 00, 00));
        assertThat(new CronExpression("0 0 0 1/15 * *").nextTimeAfter(new DateTime(2012, 4, 30, 00, 00))).isEqualTo(new DateTime(2012, 5, 01, 00, 00));
        assertThat(new CronExpression("0 0 0 1/15 * *").nextTimeAfter(new DateTime(2012, 5, 01, 00, 00))).isEqualTo(new DateTime(2012, 5, 16, 00, 00));
        assertThat(new CronExpression("0 0 0 1/15 * *").nextTimeAfter(new DateTime(2012, 5, 16, 00, 00))).isEqualTo(new DateTime(2012, 5, 31, 00, 00));
    }

    @Test
    public void check_dayOfMonthField_increment() throws Exception {
        DayOfMonthField field = new DayOfMonthField("1/15");
        assertThat(field.nextDate(new LocalDate(2012, 4, 10))).isEqualTo(new LocalDate(2012, 4, 16));
        assertThat(field.nextDate(new LocalDate(2012, 4, 16))).isEqualTo(new LocalDate(2012, 5, 1));
        assertThat(field.nextDate(new LocalDate(2012, 4, 30))).isEqualTo(new LocalDate(2012, 5, 1));
        assertThat(field.nextDate(new LocalDate(2012, 5, 01))).isEqualTo(new LocalDate(2012, 5, 16));
        assertThat(field.nextDate(new LocalDate(2012, 5, 16))).isEqualTo(new LocalDate(2012, 5, 31));
    }

    @Test
    public void check_dayOfMonth_list() throws Exception {
        assertThat(new CronExpression("0 0 0 7,19 * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 00))).isEqualTo(new DateTime(2012, 4, 19, 00, 00));
        assertThat(new CronExpression("0 0 0 7,19 * *").nextTimeAfter(new DateTime(2012, 4, 19, 00, 00))).isEqualTo(new DateTime(2012, 5, 07, 00, 00));
        assertThat(new CronExpression("0 0 0 7,19 * *").nextTimeAfter(new DateTime(2012, 5, 07, 00, 00))).isEqualTo(new DateTime(2012, 5, 19, 00, 00));
        assertThat(new CronExpression("0 0 0 7,19 * *").nextTimeAfter(new DateTime(2012, 5, 30, 00, 00))).isEqualTo(new DateTime(2012, 6, 07, 00, 00));
    }

    @Test
    public void check_dayOfMonthField_list() throws Exception {
        DayOfMonthField field = new DayOfMonthField("7,19");
        assertThat(field.nextDate(new LocalDate(2012, 4, 10))).isEqualTo(new LocalDate(2012, 4, 19));
        assertThat(field.nextDate(new LocalDate(2012, 4, 19))).isEqualTo(new LocalDate(2012, 5, 07));
        assertThat(field.nextDate(new LocalDate(2012, 5, 07))).isEqualTo(new LocalDate(2012, 5, 19));
        assertThat(field.nextDate(new LocalDate(2012, 5, 30))).isEqualTo(new LocalDate(2012, 6, 07));
    }

    @Test
    public void check_dayOfMonth_last() throws Exception {
        assertThat(new CronExpression("0 0 0 L * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 00))).isEqualTo(new DateTime(2012, 4, 30, 00, 00));
        assertThat(new CronExpression("0 0 0 L * *").nextTimeAfter(new DateTime(2012, 2, 12, 00, 00))).isEqualTo(new DateTime(2012, 2, 29, 00, 00));
        assertThat(new CronExpression("0 0 0 L * *").nextTimeAfter(new DateTime(2012, 2, 29, 00, 00))).isEqualTo(new DateTime(2012, 3, 31, 00, 00));
    }

    @Test
    public void check_dayOfMonthField_last() throws Exception {
        DayOfMonthField field = new DayOfMonthField("L");
        assertThat(field.nextDate(new LocalDate(2012, 4, 10))).isEqualTo(new LocalDate(2012, 4, 30));
        assertThat(field.nextDate(new LocalDate(2012, 2, 12))).isEqualTo(new LocalDate(2012, 2, 29));
        assertThat(field.nextDate(new LocalDate(2012, 2, 29))).isEqualTo(new LocalDate(2012, 3, 31));
        assertThat(field.nextDate(new LocalDate(2013, 2, 12))).isEqualTo(new LocalDate(2013, 2, 28));
    }

    @Test
    public void check_dayOfMonth_number_last_L() throws Exception {
        assertThat(new CronExpression("0 0 0 3L * *").nextTimeAfter(new DateTime(2012, 4, 10, 13, 00))).isEqualTo(new DateTime(2012, 4, 30 - 3, 00, 00));
        assertThat(new CronExpression("0 0 0 3L * *").nextTimeAfter(new DateTime(2012, 2, 12, 00, 00))).isEqualTo(new DateTime(2012, 2, 29 - 3, 00, 00));
    }

    @Test
    public void check_dayOfMonth_last_weekday() throws Exception {
        CronExpression cron = new CronExpression("0 0 0 LW * *");
        assertThat(cron.nextTimeAfter(new DateTime(2012, 4, 10, 00, 00))).isEqualTo(new DateTime(2012, 4, 30, 00, 00));
        assertThat(cron.nextTimeAfter(new DateTime(2012, 6, 10, 00, 00))).isEqualTo(new DateTime(2012, 6, 29, 00, 00));
        assertThat(cron.nextTimeAfter(new DateTime(2012, 9, 10, 00, 00))).isEqualTo(new DateTime(2012, 9, 28, 00, 00));
        assertThat(cron.nextTimeAfter(new DateTime(2004, 2, 10, 00, 00))).isEqualTo(new DateTime(2004, 2, 27, 00, 00));
    }

    @Test
    public void check_dayOfMonthField_number_last_L() throws Exception {
        DayOfMonthField field = new DayOfMonthField("3L");
        assertThat(field.nextDate(new LocalDate(2012, 4, 10))).isEqualTo(new LocalDate(2012, 4, 30 - 3));
        assertThat(field.nextDate(new LocalDate(2012, 2, 12))).isEqualTo(new LocalDate(2012, 2, 29 - 3));

        field = new DayOfMonthField("L-3");
        assertThat(field.nextDate(new LocalDate(2012, 4, 10))).isEqualTo(new LocalDate(2012, 4, 30 - 3));
        assertThat(field.nextDate(new LocalDate(2012, 2, 12))).isEqualTo(new LocalDate(2012, 2, 29 - 3));
    }

    @Test
    public void check_dayOfMonth_closest_weekday_W() throws Exception {
        // 9 - is weekday in may
        assertThat(new CronExpression("0 0 0 9W * *").nextTimeAfter(new DateTime(2012, 5, 2, 00, 00))).isEqualTo(new DateTime(2012, 5, 9, 00, 00));

        // 9 - is weekday in may
        assertThat(new CronExpression("0 0 0 9W * *").nextTimeAfter(new DateTime(2012, 5, 8, 00, 00))).isEqualTo(new DateTime(2012, 5, 9, 00, 00));

        // 9 - saturday, friday closest weekday in june
        assertThat(new CronExpression("0 0 0 9W * *").nextTimeAfter(new DateTime(2012, 5, 9, 00, 00))).isEqualTo(new DateTime(2012, 6, 8, 00, 00));

        // 9 - sunday, monday closest weekday in september
        assertThat(new CronExpression("0 0 0 9W * *").nextTimeAfter(new DateTime(2012, 9, 1, 00, 00))).isEqualTo(new DateTime(2012, 9, 10, 00, 00));
    }

    @Test
    public void check_dayOfMonthField_closest_weekday_W() throws Exception {
        DayOfMonthField field = new DayOfMonthField("9W");
        // 9 - is weekday in may
        assertThat(field.nextDate(new LocalDate(2012, 5, 2))).isEqualTo(new LocalDate(2012, 5, 9));

        // 9 - is weekday in may
        assertThat(field.nextDate(new LocalDate(2012, 5, 8))).isEqualTo(new LocalDate(2012, 5, 9));

        // 9 - saturday, friday closest weekday in june
        assertThat(field.nextDate(new LocalDate(2012, 5, 9))).isEqualTo(new LocalDate(2012, 6, 8));

        // 9 - sunday, monday closest weekday in september
        assertThat(field.nextDate(new LocalDate(2012, 9, 1))).isEqualTo(new LocalDate(2012, 9, 10));
    }

    @Test(expected = IllegalArgumentException.class)
    public void check_dayOfMonth_invalid_modifier() throws Exception {
        new CronExpression("0 0 0 9X * *");
    }

    @Test(expected = IllegalArgumentException.class)
    public void check_dayOfMonth_invalid_increment_modifier() throws Exception {
        new CronExpression("0 0 0 9#2 * *");
    }

    @Test
    public void check_month_number() throws Exception {
        assertThat(new CronExpression("0 0 0 1 5 *").nextTimeAfter(new DateTime(2012, 2, 12, 00, 00))).isEqualTo(new DateTime(2012, 5, 1, 00, 00));
    }

    @Test
    public void check_month_increment() throws Exception {
        assertThat(new CronExpression("0 0 0 1 5/2 *").nextTimeAfter(new DateTime(2012, 2, 12, 00, 00))).isEqualTo(new DateTime(2012, 5, 1, 00, 00));
        assertThat(new CronExpression("0 0 0 1 5/2 *").nextTimeAfter(new DateTime(2012, 5, 1, 00, 00))).isEqualTo(new DateTime(2012, 7, 1, 00, 00));

        // if rolling over year then reset month field (cron rules - increments only affect own field)
        assertThat(new CronExpression("0 0 0 1 5/10 *").nextTimeAfter(new DateTime(2012, 5, 1, 00, 00))).isEqualTo(new DateTime(2013, 5, 1, 00, 00));
    }

    @Test
    public void check_month_list() throws Exception {
        assertThat(new CronExpression("0 0 0 1 3,7,12 *").nextTimeAfter(new DateTime(2012, 2, 12, 00, 00))).isEqualTo(new DateTime(2012, 3, 1, 00, 00));
        assertThat(new CronExpression("0 0 0 1 3,7,12 *").nextTimeAfter(new DateTime(2012, 3, 1, 00, 00))).isEqualTo(new DateTime(2012, 7, 1, 00, 00));
        assertThat(new CronExpression("0 0 0 1 3,7,12 *").nextTimeAfter(new DateTime(2012, 7, 1, 00, 00))).isEqualTo(new DateTime(2012, 12, 1, 00, 00));
    }

    @Test
    public void check_month_list_by_name() throws Exception {
        assertThat(new CronExpression("0 0 0 1 MAR,JUL,DEC *").nextTimeAfter(new DateTime(2012, 2, 12, 00, 00))).isEqualTo(new DateTime(2012, 3, 1, 00, 00));
        assertThat(new CronExpression("0 0 0 1 MAR,JUL,DEC *").nextTimeAfter(new DateTime(2012, 3, 1, 00, 00))).isEqualTo(new DateTime(2012, 7, 1, 00, 00));
        assertThat(new CronExpression("0 0 0 1 MAR,JUL,DEC *").nextTimeAfter(new DateTime(2012, 7, 1, 00, 00))).isEqualTo(new DateTime(2012, 12, 1, 00, 00));
    }

    @Test(expected = IllegalArgumentException.class)
    public void check_month_invalid_modifier() throws Exception {
        new CronExpression("0 0 0 1 ? *");
    }

    @Test
    public void check_dayOfWeek_number() throws Exception {
        assertThat(new CronExpression("0 0 0 * * 3").nextTimeAfter(new DateTime(2012, 4, 1, 00, 00))).isEqualTo(new DateTime(2012, 4, 4, 00, 00));
        assertThat(new CronExpression("0 0 0 * * 3").nextTimeAfter(new DateTime(2012, 4, 4, 00, 00))).isEqualTo(new DateTime(2012, 4, 11, 00, 00));
        assertThat(new CronExpression("0 0 0 * * 3").nextTimeAfter(new DateTime(2012, 4, 12, 00, 00))).isEqualTo(new DateTime(2012, 4, 18, 00, 00));
        assertThat(new CronExpression("0 0 0 * * 3").nextTimeAfter(new DateTime(2012, 4, 18, 00, 00))).isEqualTo(new DateTime(2012, 4, 25, 00, 00));
    }

    @Test
    public void check_dayOfWeek_increment() throws Exception {
        assertThat(new CronExpression("0 0 0 * * 3/2").nextTimeAfter(new DateTime(2012, 4, 1, 00, 00))).isEqualTo(new DateTime(2012, 4, 4, 00, 00));
        assertThat(new CronExpression("0 0 0 * * 3/2").nextTimeAfter(new DateTime(2012, 4, 4, 00, 00))).isEqualTo(new DateTime(2012, 4, 6, 00, 00));
        assertThat(new CronExpression("0 0 0 * * 3/2").nextTimeAfter(new DateTime(2012, 4, 6, 00, 00))).isEqualTo(new DateTime(2012, 4, 8, 00, 00));
        assertThat(new CronExpression("0 0 0 * * 3/2").nextTimeAfter(new DateTime(2012, 4, 8, 00, 00))).isEqualTo(new DateTime(2012, 4, 11, 00, 00));
    }

    @Test
    public void check_dayOfWeekField_increment() throws Exception {
        DayOfWeekField field = new DayOfWeekField("3/2");
        assertThat(field.nextDate(new LocalDate(2012, 4, 1))).isEqualTo(new LocalDate(2012, 4, 4));
        assertThat(field.nextDate(new LocalDate(2012, 4, 4))).isEqualTo(new LocalDate(2012, 4, 6));
        assertThat(field.nextDate(new LocalDate(2012, 4, 6))).isEqualTo(new LocalDate(2012, 4, 8));
        assertThat(field.nextDate(new LocalDate(2012, 4, 8))).isEqualTo(new LocalDate(2012, 4, 11));
    }

    @Test
    public void check_dayOfWeek_list() throws Exception {
        assertThat(new CronExpression("0 0 0 * * 1,5,7").nextTimeAfter(new DateTime(2012, 4, 1, 00, 00))).isEqualTo(new DateTime(2012, 4, 2, 00, 00));
        assertThat(new CronExpression("0 0 0 * * 1,5,7").nextTimeAfter(new DateTime(2012, 4, 2, 00, 00))).isEqualTo(new DateTime(2012, 4, 6, 00, 00));
        assertThat(new CronExpression("0 0 0 * * 1,5,7").nextTimeAfter(new DateTime(2012, 4, 6, 00, 00))).isEqualTo(new DateTime(2012, 4, 8, 00, 00));
    }

    @Test
    public void check_dayOfWeekField_list() throws Exception {
        DayOfWeekField field = new DayOfWeekField("1,5,7");
        assertThat(field.nextDate(new LocalDate(2012, 4, 1))).isEqualTo(new LocalDate(2012, 4, 2));
        assertThat(field.nextDate(new LocalDate(2012, 4, 2))).isEqualTo(new LocalDate(2012, 4, 6));
        assertThat(field.nextDate(new LocalDate(2012, 4, 6))).isEqualTo(new LocalDate(2012, 4, 8));
    }

    @Test
    public void check_dayOfWeekFieldWeekStartsSunday() throws Exception {
        LocalDate start = new LocalDate(2012, 4, 1);
        assertThat(new DayOfWeekField("1",  true).nextDate(start)).isEqualTo(new LocalDate(2012, 4, 8));
        assertThat(new DayOfWeekField("SUN", true).nextDate(start)).isEqualTo(new LocalDate(2012, 4, 8));

        assertThat(new DayOfWeekField("2", true).nextDate(start)).isEqualTo(new LocalDate(2012, 4, 2));
        assertThat(new DayOfWeekField("MON", true).nextDate(start)).isEqualTo(new LocalDate(2012, 4, 2));

        assertThat(new DayOfWeekField("3", true).nextDate(start)).isEqualTo(new LocalDate(2012, 4, 3));
        assertThat(new DayOfWeekField("TUE", true).nextDate(start)).isEqualTo(new LocalDate(2012, 4, 3));

        assertThat(new DayOfWeekField("4", true).nextDate(start)).isEqualTo(new LocalDate(2012, 4, 4));
        assertThat(new DayOfWeekField("WED", true).nextDate(start)).isEqualTo(new LocalDate(2012, 4, 4));

        assertThat(new DayOfWeekField("5", true).nextDate(start)).isEqualTo(new LocalDate(2012, 4, 5));
        assertThat(new DayOfWeekField("THU", true).nextDate(start)).isEqualTo(new LocalDate(2012, 4, 5));

        assertThat(new DayOfWeekField("6", true).nextDate(start)).isEqualTo(new LocalDate(2012, 4, 6));
        assertThat(new DayOfWeekField("FRI", true).nextDate(start)).isEqualTo(new LocalDate(2012, 4, 6));

        assertThat(new DayOfWeekField("7", true).nextDate(start)).isEqualTo(new LocalDate(2012, 4, 7));
        assertThat(new DayOfWeekField("SAT", true).nextDate(start)).isEqualTo(new LocalDate(2012, 4, 7));
    }

    @Test
    public void check_dayOfWeekWeekStartsSunday() throws Exception {
        DateTime start = new LocalDate(2012, 4, 1).toDateTimeAtStartOfDay();
        assertThat(new CronExpression("0 0 0 * * 1", true, true).nextTimeAfter(start)).isEqualTo(new DateTime(2012, 4, 8, 00, 00));
        assertThat(new CronExpression("0 0 0 * * 2", true, true).nextTimeAfter(start)).isEqualTo(new DateTime(2012, 4, 2, 00, 00));
        assertThat(new CronExpression("0 0 0 * * 3", true, true).nextTimeAfter(start)).isEqualTo(new DateTime(2012, 4, 3, 00, 00));
        assertThat(new CronExpression("0 0 0 * * 4", true, true).nextTimeAfter(start)).isEqualTo(new DateTime(2012, 4, 4, 00, 00));
        assertThat(new CronExpression("0 0 0 * * 5", true, true).nextTimeAfter(start)).isEqualTo(new DateTime(2012, 4, 5, 00, 00));
        assertThat(new CronExpression("0 0 0 * * 6", true, true).nextTimeAfter(start)).isEqualTo(new DateTime(2012, 4, 6, 00, 00));
        assertThat(new CronExpression("0 0 0 * * 7", true, true).nextTimeAfter(start)).isEqualTo(new DateTime(2012, 4, 7, 00, 00));
    }

    @Test
    public void check_dayOfWeek_list_by_name() throws Exception {
        assertThat(new CronExpression("0 0 0 * * MON,FRI,SUN").nextTimeAfter(new DateTime(2012, 4, 1, 00, 00))).isEqualTo(new DateTime(2012, 4, 2, 00, 00));
        assertThat(new CronExpression("0 0 0 * * MON,FRI,SUN").nextTimeAfter(new DateTime(2012, 4, 2, 00, 00))).isEqualTo(new DateTime(2012, 4, 6, 00, 00));
        assertThat(new CronExpression("0 0 0 * * MON,FRI,SUN").nextTimeAfter(new DateTime(2012, 4, 6, 00, 00))).isEqualTo(new DateTime(2012, 4, 8, 00, 00));
    }

    @Test
    public void check_dayOfWeekField_list_by_name() throws Exception {
        DayOfWeekField field = new DayOfWeekField("MON,FRI,SUN");
        assertThat(field.nextDate(new LocalDate(2012, 4, 1))).isEqualTo(new LocalDate(2012, 4, 2));
        assertThat(field.nextDate(new LocalDate(2012, 4, 2))).isEqualTo(new LocalDate(2012, 4, 6));
        assertThat(field.nextDate(new LocalDate(2012, 4, 6))).isEqualTo(new LocalDate(2012, 4, 8));

    }

    @Test
    public void check_dayOfWeek_last_friday_in_month() throws Exception {
        assertThat(new CronExpression("0 0 0 * * 5L").nextTimeAfter(new DateTime(2012, 4, 1, 00, 00))).isEqualTo(new DateTime(2012, 4, 27, 00, 00));
        assertThat(new CronExpression("0 0 0 * * 5L").nextTimeAfter(new DateTime(2012, 4, 27, 00, 00))).isEqualTo(new DateTime(2012, 5, 25, 00, 00));
        assertThat(new CronExpression("0 0 0 * * 5L").nextTimeAfter(new DateTime(2012, 2, 6, 00, 00))).isEqualTo(new DateTime(2012, 2, 24, 00, 00));
        assertThat(new CronExpression("0 0 0 * * FRIL").nextTimeAfter(new DateTime(2012, 2, 6, 00, 00))).isEqualTo(new DateTime(2012, 2, 24, 00, 00));
    }

    @Test
    public void check_dayOfWeekField_last_friday_in_month() throws Exception {
        DayOfWeekField field = new DayOfWeekField("5L");
        assertThat(field.nextDate(new LocalDate(2012, 4, 1))).isEqualTo(new LocalDate(2012, 4, 27));
        assertThat(field.nextDate(new LocalDate(2012, 4, 27))).isEqualTo(new LocalDate(2012, 5, 25));
        assertThat(field.nextDate(new LocalDate(2012, 2, 6))).isEqualTo(new LocalDate(2012, 2, 24));
        assertThat(new DayOfWeekField("FRIL").nextDate(new LocalDate(2012, 2, 6))).isEqualTo(new LocalDate(2012, 2, 24));
    }

    @Test(expected = IllegalArgumentException.class)
    public void check_dayOfWeek_invalid_modifier() throws Exception {
        new CronExpression("0 0 0 * * 5W");
    }

    @Test(expected = IllegalArgumentException.class)
    public void check_dayOfWeek_invalid_increment_modifier() throws Exception {
        new CronExpression("0 0 0 * * 5?3");
    }

    @Test
    public void check_dayOfWeek_shall_interpret_0_as_sunday() throws Exception {
        assertThat(new CronExpression("0 0 0 * * 0").nextTimeAfter(new DateTime(2012, 4, 1, 00, 00))).isEqualTo(new DateTime(2012, 4, 8, 00, 00));
        assertThat(new CronExpression("0 0 0 * * 0L").nextTimeAfter(new DateTime(2012, 4, 1, 00, 00))).isEqualTo(new DateTime(2012, 4, 29, 00, 00));
        assertThat(new CronExpression("0 0 0 * * 0#2").nextTimeAfter(new DateTime(2012, 4, 1, 00, 00))).isEqualTo(new DateTime(2012, 4, 8, 00, 00));
    }

    @Test
    public void check_dayOfWeek_shall_interpret_7_as_sunday() throws Exception {
        assertThat(new CronExpression("0 0 0 * * 7").nextTimeAfter(new DateTime(2012, 4, 1, 00, 00))).isEqualTo(new DateTime(2012, 4, 8, 00, 00));
        assertThat(new CronExpression("0 0 0 * * 7L").nextTimeAfter(new DateTime(2012, 4, 1, 00, 00))).isEqualTo(new DateTime(2012, 4, 29, 00, 00));
        assertThat(new CronExpression("0 0 0 * * 7#2").nextTimeAfter(new DateTime(2012, 4, 1, 00, 00))).isEqualTo(new DateTime(2012, 4, 8, 00, 00));
    }

    @Test
    public void check_dayOfWeek_nth_friday_in_month() throws Exception {
        assertThat(new CronExpression("0 0 0 * * 5#3").nextTimeAfter(new DateTime(2012, 4, 1, 00, 00))).isEqualTo(new DateTime(2012, 4, 20, 00, 00));
        assertThat(new CronExpression("0 0 0 * * 5#3").nextTimeAfter(new DateTime(2012, 4, 20, 00, 00))).isEqualTo(new DateTime(2012, 5, 18, 00, 00));
        assertThat(new CronExpression("0 0 0 * * 7#1").nextTimeAfter(new DateTime(2012, 3, 30, 00, 00))).isEqualTo(new DateTime(2012, 4, 1, 00, 00));
        assertThat(new CronExpression("0 0 0 * * 7#1").nextTimeAfter(new DateTime(2012, 4, 1, 00, 00))).isEqualTo(new DateTime(2012, 5, 6, 00, 00));
        assertThat(new CronExpression("0 0 0 * * 3#5").nextTimeAfter(new DateTime(2012, 2, 6, 00, 00))).isEqualTo(new DateTime(2012, 2, 29, 00, 00)); // leapday
        assertThat(new CronExpression("0 0 0 * * WED#5").nextTimeAfter(new DateTime(2012, 2, 6, 00, 00))).isEqualTo(new DateTime(2012, 2, 29, 00, 00)); // leapday
    }

    @Test
    public void check_dayOfWeekField_nth_friday_in_month() throws Exception {
        assertThat(new DayOfWeekField("5#3").nextDate(new LocalDate(2012, 4, 1))).isEqualTo(new LocalDate(2012, 4, 20));
        assertThat(new DayOfWeekField("5#3").nextDate(new LocalDate(2012, 4, 20))).isEqualTo(new LocalDate(2012, 5, 18));
        assertThat(new DayOfWeekField("7#1").nextDate(new LocalDate(2012, 3, 30))).isEqualTo(new LocalDate(2012, 4, 1));
        assertThat(new DayOfWeekField("7#1").nextDate(new LocalDate(2012, 4, 1))).isEqualTo(new LocalDate(2012, 5, 6));
        assertThat(new DayOfWeekField("3#5").nextDate(new LocalDate(2012, 2, 6))).isEqualTo(new LocalDate(2012, 2, 29));
        assertThat(new DayOfWeekField("WED#5").nextDate(new LocalDate(2012, 2, 6))).isEqualTo(new LocalDate(2012, 2, 29));
    }

    @Test
    public void check_dayOfWeekField_supports_multiple_nth_entries() throws Exception {
        DayOfWeekField f = new DayOfWeekField("TUE#2,TUE#3");
        assertThat(f.nextDate(new LocalDate(2016, 4, 7))).isEqualTo(new LocalDate(2016, 4, 12));
        assertThat(f.nextDate(new LocalDate(2016, 4, 12))).isEqualTo(new LocalDate(2016, 4, 19));
        assertThat(f.nextDate(new LocalDate(2016, 4, 19))).isEqualTo(new LocalDate(2016, 5, 10));
        assertThat(f.nextDate(new LocalDate(2016, 5, 10))).isEqualTo(new LocalDate(2016, 5, 17));
    }

    @Test
    public void shall_support_rolling_period() throws Exception {
        assertPossibleValues(new SimpleField(CronFieldType.HOUR, "22-2"), 22, 23, 0, 1, 2);
        assertPossibleValues(new SimpleField(CronFieldType.HOUR, "22-2/2"), 22, 0, 2);
        assertPossibleValues(new SimpleField(CronFieldType.HOUR, "22-2/3"), 22, 1);
        assertPossibleValues(new SimpleField(CronFieldType.HOUR, "22-2/4"), 22, 2);
        assertPossibleValues(new SimpleField(CronFieldType.HOUR, "22-2/5"), 22);


        assertPossibleValues(new DayOfWeekField("SUN-SAT/5", true), 7, 5);
        assertPossibleValues(new DayOfWeekField("SUN-SAT/4", true), 7, 4);
        assertPossibleValues(new DayOfWeekField("SUN-SAT/3", true), 7, 3, 6);
        assertPossibleValues(new DayOfWeekField("SUN-SAT/2", true), 7, 2, 4, 6);
        assertPossibleValues(new DayOfWeekField("SUN-SAT/1", true), 1, 2, 3, 4, 5, 6, 7);
    }

    @Test(expected = IllegalArgumentException.class)
    public void non_existing_date_throws_exception() throws Exception {
        // Will check for the next 4 years - no 30th of February is found so a IAE is thrown.
        new CronExpression("* * * 30 2 *").nextTimeAfter(DateTime.now());
    }

    @Test
    public void test_default_barrier() throws Exception {
        // the default barrier is 4 years - so leap years are considered.
        assertThat(new CronExpression("* * * 29 2 *").nextTimeAfter(new DateTime(2012, 3, 1, 00, 00))).isEqualTo(new DateTime(2016, 2, 29, 00, 00));
    }

    @Test
    public void check_year_number() throws Exception {
        assertThat(new CronExpression("0 0 0 29 FEB ? 2012").nextTimeAfter(new DateTime(2012, 2, 1, 00, 00))).isEqualTo(new DateTime(2012, 2, 29, 00, 00));
    }

    @Test(expected = NoSuchElementException.class)
    public void test_error_when_no_more_dates() throws Exception {
        new CronExpression("0 0 0 29 FEB ? 2012").nextTimeAfter(new DateTime(2015, 2, 1, 00, 00));
    }

    @Test
    public void test_year_in_the_future() throws Exception {
        assertThat(new CronExpression("0 11 11 11 11 ?").nextTimeAfter(new DateTime(2116,11,11,11,11,00))).isEqualTo(new DateTime(2117,11,11,11,11,00));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_one_year_barrier() throws Exception {
        // The next leap year is 2016, so an IllegalArgumentException is expected.
        new CronExpression("* * * 29 2 *").nextTimeAfter(new DateTime(2012, 3, 1, 00, 00), new DateTime(2013, 3, 1, 00, 00));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_two_year_barrier() throws Exception {
        // The next leap year is 2016, so an IllegalArgumentException is expected.
        new CronExpression("* * * 29 2 *").nextTimeAfter(new DateTime(2012, 3, 1, 00, 00), 1000 * 60 * 60 * 24 * 356 * 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_seconds_specified_but_should_be_omitted() throws Exception {
        CronExpression.createWithoutSeconds("* * * 29 2 *");
    }

    @Test
    public void test_without_seconds() throws Exception {
        assertThat(CronExpression.createWithoutSeconds("* * 29 2 *").nextTimeAfter(new DateTime(2012, 3, 1, 00, 00))).isEqualTo(new DateTime(2016, 2, 29, 00, 00));
    }
}

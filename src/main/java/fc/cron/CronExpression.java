/*
 * Copyright (C) 2012 Frode Carlsen.
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
import org.joda.time.LocalDate;
import org.joda.time.MutableDateTime;

import java.util.NoSuchElementException;
import java.util.regex.Pattern;

/**
 * Parser for unix-like cron expressions: Cron expressions allow specifying combinations of criteria
 * for time such as: &quot;Each Monday-Friday at 08:00&quot; or &quot;Every last friday of the month
 * at 01:30&quot; <p> A cron expressions consists of 5 or 6 mandatory fields (seconds may be
 * omitted) separated by space. <br> These are:
 *
 * <table cellspacing="8"> <tr> <th align="left">Field</th> <th align="left">&nbsp;</th> <th
 * align="left">Allowable values</th> <th align="left">&nbsp;</th> <th align="left">Special
 * Characters</th> </tr> <tr> <td align="left"><code>Seconds (may be omitted)</code></td> <td
 * align="left">&nbsp;</th> <td align="left"><code>0-59</code></td> <td align="left">&nbsp;</th> <td
 * align="left"><code>, - * /</code></td> </tr> <tr> <td align="left"><code>Minutes</code></td> <td
 * align="left">&nbsp;</th> <td align="left"><code>0-59</code></td> <td align="left">&nbsp;</th> <td
 * align="left"><code>, - * /</code></td> </tr> <tr> <td align="left"><code>Hours</code></td> <td
 * align="left">&nbsp;</th> <td align="left"><code>0-23</code></td> <td align="left">&nbsp;</th> <td
 * align="left"><code>, - * /</code></td> </tr> <tr> <td align="left"><code>Day of month</code></td>
 * <td align="left">&nbsp;</th> <td align="left"><code>1-31</code></td> <td align="left">&nbsp;</th>
 * <td align="left"><code>, - * ? / L W</code></td> </tr> <tr> <td align="left"><code>Month</code></td>
 * <td align="left">&nbsp;</th> <td align="left"><code>1-12 or JAN-DEC (note: english
 * abbreviations)</code></td> <td align="left">&nbsp;</th> <td align="left"><code>, - *
 * /</code></td> </tr> <tr> <td align="left"><code>Day of week</code></td> <td
 * align="left">&nbsp;</th> <td align="left"><code>1-7 or MON-SUN (note: english
 * abbreviations)</code></td> <td align="left">&nbsp;</th> <td align="left"><code>, - * ? / L
 * #</code></td> </tr> </table>
 *
 * <P> '*' Can be used in all fields and means 'for all values'. E.g. &quot;*&quot; in minutes,
 * means 'for all minutes' <P> '?' Ca be used in Day-of-month and Day-of-week fields. Used to
 * signify 'no special value'. It is used when one want to specify something for one of those two
 * fields, but not the other. <P> '-' Used to specify a time interval. E.g. &quot;10-12&quot; in
 * Hours field means 'for hours 10, 11 and 12' <P> ',' Used to specify multiple values for a field.
 * E.g. &quot;MON,WED,FRI&quot; in Day-of-week field means &quot;for monday, wednesday and
 * friday&quot; <P> '/' Used to specify increments. E.g. &quot;0/15&quot; in Seconds field means
 * &quot;for seconds 0, 15, 30, ad 45&quot;. And &quot;5/15&quot; in seconds field means &quot;for
 * seconds 5, 20, 35, and 50&quot;. If '*' s specified before '/' it is the same as saying it starts
 * at 0. For every field there's a list of values that can be turned on or off. For Seconds and
 * Minutes these range from 0-59. For Hours from 0 to 23, For Day-of-month it's 1 to 31, For Months
 * 1 to 12. &quot;/&quot; character helsp turn some of these values back on. Thus &quot;7/6&quot; in
 * Months field specify just Month 7. It doesn't turn on every 6 month following, since cron fields
 * never roll over <P> 'L' Can be used on Day-of-month and Day-of-week fields. It signifies last day
 * of the set of allowed values. In Day-of-month field it's the last day of the month (e.g.. 31 jan,
 * 28 feb (29 in leap years), 31 march, etc.). In Day-of-week field it's Sunday. If there's a
 * prefix, this will be subtracted (5L in Day-of-month means 5 days before last day of Month: 26
 * jan, 23 feb, etc.) <P> 'W' Can be specified in Day-of-Month field. It specifies closest weekday
 * (monday-friday). Holidays are not accounted for. &quot;15W&quot; in Day-of-Month field means
 * 'closest weekday to 15 i in given month'. If the 15th is a Saturday, it gives Friday. If 15th is
 * a Sunday, the it gives following Monday. <P> '#' Can be used in Day-of-Week field. For example:
 * &quot;5#3&quot; means 'third friday in month' (day 5 = friday, #3 - the third). If the day does
 * not exist (e.g. &quot;5#5&quot; - 5th friday of month) and there aren't 5 fridays in the month,
 * then it won't match until the next month with 5 fridays. <P> <b>Case-sensitivt</b> No fields are
 * case-sensitive <P> <b>Dependencies between fields</b> Fields are always evaluated independently,
 * but the expression doesn't match until the constraints of each field are met.Feltene evalueres
 * Overlap of intervals are not allowed. That is: for Day-of-week field &quot;FRI-MON&quot; is
 * invalid,but &quot;FRI-SUN,MON&quot; is valid
 */
public class CronExpression {

    private final String expr;
    private final SimpleField secondField;
    private final SimpleField minuteField;
    private final SimpleField hourField;
    private final DayOfWeekField dayOfWeekField;
    private final SimpleField monthField;
    private final DayOfMonthField dayOfMonthField;
    private final SimpleField yearField;

    public CronExpression(final String expr) {
        this(expr, true);
    }

    public CronExpression(final String expr, boolean shouldHaveSeconds) {
        this(expr, shouldHaveSeconds, false);
    }

    public CronExpression(final String expr, boolean shouldHaveSeconds, boolean weekStartsSunday) {
        if (expr == null) {
            throw new IllegalArgumentException("expr is null"); //$NON-NLS-1$
        }

        this.expr = expr;
        boolean withYear = false;
        boolean withSeconds = false;

        final String[] parts = expr.split("\\s+"); //$NON-NLS-1$
        if (parts.length < 5) {
            throw new IllegalArgumentException(String.format("Invalid cron expression [%s], expected at least 5 felt, got %s"
                    , expr, parts.length));
        } else if (parts.length == 5) {
            withSeconds = false;
            withYear = false;
        } else if (parts.length == 6) {
            //
            //If last element contains 4 digits, a year element has been supplied and no seconds element
            Pattern yearRegex = Pattern.compile("\\d{4}");
            if (yearRegex.matcher(parts[5]).find()) {
                withSeconds = false;
                withYear = true;
            } /*else if("*".equals(parts[5])) {
                // check if the next to last element is valid as day of week field
                try {
                    new DayOfWeekField(parts[4]);
                    withSeconds = false;
                    withYear = true;
                } catch (IllegalArgumentException e) {
                    withSeconds = true;
                    withYear = false;
                }
            }*/ else {
                withSeconds = true;
                withYear = false;
            }
        } else if (parts.length == 7) {
            withSeconds = true;
            withYear = true;
        } else {
            throw new IllegalArgumentException(String.format("Invalid cron expression [%s], expected at most 7 felt, got %s"
                    , expr, parts.length));
        }

        if (shouldHaveSeconds ^ withSeconds) {
            throw new IllegalArgumentException(String.format("Invalid cron expression [%s], seconds field is not specified, got %s"
                    , expr, parts.length));
        }

        int ix = withSeconds ? 1 : 0;
        this.secondField = new SimpleField(CronFieldType.SECOND, withSeconds ? parts[0] : "0");
        this.minuteField = new SimpleField(CronFieldType.MINUTE, parts[ix++]);
        this.hourField = new SimpleField(CronFieldType.HOUR, parts[ix++]);
        this.dayOfMonthField = new DayOfMonthField(parts[ix++]);
        this.monthField = new SimpleField(CronFieldType.MONTH, parts[ix++]);
        this.dayOfWeekField = new DayOfWeekField(parts[ix++], weekStartsSunday);
        this.yearField = new SimpleField(CronFieldType.YEAR, withYear ? parts[ix] : "*");
    }

    public static CronExpression create(final String expr) {
        return new CronExpression(expr, true);
    }

    public static CronExpression createWithoutSeconds(final String expr) {
        return new CronExpression(expr, false);
    }

    public DateTime nextTimeAfter(DateTime afterTime) {
        // will search for the next time within the next 4 years. If there is no
        // time matching, an InvalidArgumentException will be thrown (it is very
        // likely that the cron expression is invalid, like the February 30th).
        return nextTimeAfter(afterTime, afterTime.plusYears(4));
    }

    public DateTime nextTimeAfter(DateTime afterTime, long durationInMillis) {
        // will search for the next time within the next durationInMillis
        // millisecond. Be aware that the duration is specified in millis,
        // but in fact the limit is checked on a day-to-day basis.
        return nextTimeAfter(afterTime, afterTime.plus(durationInMillis));
    }

    public DateTime nextTimeAfter(DateTime afterTime, DateTime dateTimeBarrier) {
        MutableDateTime nextTime = new MutableDateTime(afterTime);
        nextTime.setMillisOfSecond(0);
        nextTime.secondOfDay().add(1);

        nextYear(dateTimeBarrier, nextTime);

        return nextTime.toDateTime();
    }

    private void nextYear(DateTime dateTimeBarrier, MutableDateTime nextTime) {
        while (true) { // year
            nextDayOfWeek(dateTimeBarrier, nextTime);
            if (yearField.matches(nextTime.getYear())) {
                break;
            }
            int year = yearField.nextValue(nextTime.getYear());
            if (year < 0) {
                throw new NoSuchElementException("No next execution time exists after " + nextTime);
            } else {
                nextTime.setYear(year);
            }
            nextTime.setMonthOfYear(1);
            nextTime.setDayOfMonth(1);
            nextTime.setTime(0, 0, 0, 0);
            checkIfDateTimeBarrierIsReached(nextTime, dateTimeBarrier);
        }
    }

    private void nextDayOfWeek(DateTime dateTimeBarrier, MutableDateTime nextTime) {
        while (true) { // day of week
            nextMonth(dateTimeBarrier, nextTime);
            if (dayOfWeekField.matches(new LocalDate(nextTime))) {
                break;
            }
            LocalDate nextDate = dayOfWeekField.nextDate(new LocalDate(nextTime));
            nextTime.setDate(nextDate.getYear(), nextDate.getMonthOfYear(), nextDate.getDayOfMonth());
            nextTime.setTime(0, 0, 0, 0);
            checkIfDateTimeBarrierIsReached(nextTime, dateTimeBarrier);
        }
    }

    private void nextMonth(DateTime dateTimeBarrier, MutableDateTime nextTime) {
        while (true) { // month
            nextDayOfMonth(dateTimeBarrier, nextTime);
            if (monthField.matches(nextTime.getMonthOfYear())) {
                break;
            }
            int month = monthField.nextValue(nextTime.getMonthOfYear());
            if (month < 0) {
                nextTime.year().add(1);
                nextTime.setMonthOfYear(monthField.nextValue(1));
            } else {
                nextTime.setMonthOfYear(month);
            }
            nextTime.setDayOfMonth(1);
            nextTime.setTime(0, 0, 0, 0);
            checkIfDateTimeBarrierIsReached(nextTime, dateTimeBarrier);
        }
    }

    private void nextDayOfMonth(DateTime dateTimeBarrier, MutableDateTime nextTime) {
        while (true) { // day of month
            nextHour(nextTime);
            if (dayOfMonthField.matches(new LocalDate(nextTime))) {
                break;
            }

            LocalDate nextDate = dayOfMonthField.nextDate(new LocalDate(nextTime));
            nextTime.setDate(nextDate.getYear(), nextDate.getMonthOfYear(), nextDate.getDayOfMonth());
            nextTime.setTime(0, 0, 0, 0);
            checkIfDateTimeBarrierIsReached(nextTime, dateTimeBarrier);
        }
    }

    private void nextHour(MutableDateTime nextTime) {
        while (true) { // hour
            nextMinute(nextTime);
            if (hourField.matches(nextTime.getHourOfDay())) {
                break;
            }
            int hour = hourField.nextValue(nextTime.getHourOfDay());
            if (hour < 0) {
                nextTime.dayOfYear().add(1);
                nextTime.setHourOfDay(hourField.nextValue(0));
            } else {
                nextTime.setHourOfDay(hour);
            }
            nextTime.minuteOfHour().set(0);
            nextTime.secondOfMinute().set(0);
        }
    }

    private void nextMinute(MutableDateTime nextTime) {
        while (true) { // minute
            nextSecond(nextTime);
            if (minuteField.matches(nextTime.getMinuteOfHour())) {
                break;
            }
            int minute = minuteField.nextValue(nextTime.getMinuteOfHour());
            if (minute < 0) {
                nextTime.hourOfDay().add(1);
                nextTime.setMinuteOfHour(minuteField.nextValue(0));
            } else {
                nextTime.setMinuteOfHour(minute);
            }
            nextTime.secondOfMinute().set(0);
        }
    }

    private void nextSecond(MutableDateTime nextTime) {
        while (true) { // second
            if (secondField.matches(nextTime.getSecondOfMinute())) {
                break;
            }

            int second = secondField.nextValue(nextTime.getSecondOfMinute());

            if (second < 0) {
                nextTime.minuteOfDay().add(1);
                nextTime.setSecondOfMinute(secondField.nextValue(0));
            } else {
                nextTime.setSecondOfMinute(second);
            }
        }
    }

    private static void checkIfDateTimeBarrierIsReached(MutableDateTime nextTime, DateTime dateTimeBarrier) {
        if (nextTime.isAfter(dateTimeBarrier)) {
            throw new IllegalArgumentException("No next execution time could be determined that is before the limit of " + dateTimeBarrier);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + expr + ">";
    }
}

package fc.cron;

import org.joda.time.LocalDate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.joda.time.DateTimeConstants.DAYS_PER_WEEK;

class DayOfWeekField extends BasicField {

    private static final Set<String> ALLOWED_MODIFIERS = new HashSet<String>(Arrays.asList("L", "?"));
    private static final Set<String> ALLOWED_INCREMENTS = new HashSet<String>(Arrays.asList("/", "#"));

    DayOfWeekField(String fieldExpr) {
        super(CronFieldType.DAY_OF_WEEK, fieldExpr);
    }

    boolean matches(LocalDate date) {
        for (FieldPart part : parts) {
            if ("L".equals(part.getModifier())) {
                return date.getDayOfWeek() == part.getFrom() && date.getDayOfMonth() > (date.dayOfMonth().getMaximumValue() - DAYS_PER_WEEK);
            } else if ("#".equals(part.getIncrementModifier())) {
                if (date.getDayOfWeek() == part.getFrom()) {
                    int num = date.getDayOfMonth() / DAYS_PER_WEEK;
                    return part.getIncrement() == (date.getDayOfMonth() % DAYS_PER_WEEK == 0 ? num : num + 1);
                }
                return false;
            } else if ("?".equals(part.getModifier())) {
                return true;
            }
        }
        return matches(date.getDayOfWeek());
    }

    LocalDate nextDate(LocalDate date) {
        LocalDate result = null;
        for (FieldPart part : parts) {
            LocalDate partDate = date;
            if ("?".equals(part.getModifier())) {
                partDate = date.plusDays(1);
            } else if ("L".equals(part.getModifier())) {
                partDate = nextLastDayOfWeek(date, part.getFrom());
            } else if ("#".equals(part.getIncrementModifier())) {
                partDate = nextNthDay(date, part.getFrom(), part.getIncrement());
            }

            result = result != null && result.isBefore(partDate) ? result : partDate;
        }

        if (hasValues()) {
            LocalDate partDate = date.plusDays(1);
            int weekday = nextValue(partDate.getDayOfWeek());
            if (weekday > 0) {
                partDate = partDate.withDayOfWeek(weekday);
            } else {
                partDate = partDate.plusWeeks(1).withDayOfWeek(nextValue(fieldType.getFrom()));
            }
            result = result != null && result.isBefore(partDate) ? result : partDate;
        }

        return result;
    }

    private LocalDate nextLastDayOfWeek(LocalDate date, int dayOfWeek) {
        LocalDate lastWeekDay = date.dayOfMonth().withMaximumValue().minusWeeks(1).withDayOfWeek(dayOfWeek);
        if (date.isBefore(lastWeekDay)) {
            return lastWeekDay;
        } else {
            return date.plusMonths(1).dayOfMonth().withMaximumValue().minusWeeks(1).withDayOfWeek(dayOfWeek);
        }
    }

    private LocalDate nextNthDay(LocalDate date, int dayOfWeek, int nth) {
        LocalDate next = date;
        while (true) {
            LocalDate start = next.dayOfMonth().withMinimumValue();
            next = start.withDayOfWeek(dayOfWeek);
            next = next.plusWeeks(nth - (start.getMonthOfYear() != next.getMonthOfYear() ? 0 : 1));

            if (start.getMonthOfYear() == next.getMonthOfYear() && next.isAfter(date)) {
                return next;
            } else {
                next = next.plusMonths(1);
            }
        }
    }

    @Override
    protected Integer mapValue(String value) {
        // Use 1-7 for weekdays, but 0 will also represent sunday (linux practice)
        return "0".equals(value) ? Integer.valueOf(7) : super.mapValue(value);
    }

    @Override
    protected void validatePart(FieldPart part) {
        if (part.getModifier() != null && !ALLOWED_MODIFIERS.contains(part.getModifier())) {
            throw new IllegalArgumentException(String.format("Invalid modifier [%s]", part.getModifier()));
        } else if (part.getIncrementModifier() != null && !ALLOWED_INCREMENTS.contains(part.getIncrementModifier())) {
            throw new IllegalArgumentException(String.format("Invalid increment modifier [%s]", part.getIncrementModifier()));
        } else if ("#".equals(part.getIncrementModifier()) && part.getIncrement() > 5) {
            throw new IllegalArgumentException(String.format("Invalid nth increment modifier [%d]", part.getIncrement()));
        }
    }
}

package fc.cron;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class DayOfMonthField extends BasicField {

    private static final Set<String> ALLOWED_MODIFIERS = new HashSet<String>(Arrays.asList("L", "LW", "W", "?"));

    DayOfMonthField(String fieldExpr) {
        super(CronFieldType.DAY_OF_MONTH, fieldExpr);
    }

    boolean matches(LocalDate date) {
        for (FieldPart part : parts) {
            if (matches(part, date)) {
                return true;
            }
        }

        return matches(date.getDayOfMonth());
    }

    private boolean matches(FieldPart part, LocalDate date) {
        switch (part.getModifier()) {
            case "L":
                return date.getDayOfMonth() == (date.dayOfMonth().getMaximumValue() - (part.getFrom() == null ? 0 : part.getFrom()));
            case "W":
                if (date.getDayOfWeek() <= DateTimeConstants.FRIDAY) {
                    if (date.getDayOfMonth() == part.getFrom()) {
                        return true;
                    } else if (date.getDayOfWeek() == DateTimeConstants.FRIDAY) {
                        return date.plusDays(1).getDayOfMonth() == part.getFrom();
                    } else if (date.getDayOfWeek() == DateTimeConstants.MONDAY) {
                        return date.minusDays(1).getDayOfMonth() == part.getFrom();
                    }
                }
                break;
            case "LW":
                LocalDate last = date.dayOfMonth().withMaximumValue();
                last = last.minusDays(Math.max(0, last.getDayOfWeek() - DateTimeConstants.FRIDAY));
                return last.getDayOfMonth() == date.getDayOfMonth();
            case "?":
                return true;
            default:
                throw new IllegalStateException("Unknown modifier: " + part.getModifier());
        }
        return false;
    }

    public LocalDate nextDate(LocalDate date) {
        LocalDate result = null;
        for (FieldPart part : parts) {
            LocalDate partDate = date;
            switch (part.getModifier()) {
                case "?":
                    partDate = date.plusDays(1);
                    break;
                case "L":
                    partDate = nextLastDayOfMonth(date, part.getFrom());
                    break;
                case "W":
                    partDate = nextWeekday(date, part.getFrom());
                    break;
                case "LW":
                    partDate = nextLastWeekday(date);
                    break;
                default:
                    throw new IllegalStateException("Unknown modifier: " + part.getModifier());
            }

            result = result != null && result.isBefore(partDate) ? result : partDate;
        }

        if (hasValues()) {
            LocalDate partDate = date.plusDays(1);
            int day = nextValue(partDate.getDayOfMonth());
            if (day > 0 && day <= partDate.dayOfMonth().getMaximumValue()) {
                partDate = partDate.withDayOfMonth(day);
            } else {
                partDate = partDate.plusMonths(1).withDayOfMonth(nextValue(fieldType.getFrom()));
            }
            result = result != null && result.isBefore(partDate) ? result : partDate;
        }

        return result;
    }

    private LocalDate nextLastWeekday(LocalDate date) {
        LocalDate last = date.plusDays(1).dayOfMonth().withMaximumValue();
        return last.minusDays(Math.max(0, last.getDayOfWeek() - DateTimeConstants.FRIDAY));
    }

    private LocalDate nextLastDayOfMonth(LocalDate date, Integer offset) {
        return date.plusDays(1).dayOfMonth().withMaximumValue().minusDays(offset == null ? 0 : offset);
    }

    private LocalDate nextWeekday(LocalDate date, Integer dayOfMonth) {
        LocalDate result = date;
        while (true) {
            result = result.withDayOfMonth(dayOfMonth);
            if (result.getDayOfWeek() == DateTimeConstants.SATURDAY) {
                result = result.minusDays(1);
            } else if (result.getDayOfWeek() == DateTimeConstants.SUNDAY) {
                result = result.plusDays(1);
            }

            if (result.isAfter(date)) {
                return result;
            } else {
                result = result.plusMonths(1);
            }
        }
    }

    @Override
    protected void validatePart(FieldPart part) {
        if (part.getModifier() != null && !ALLOWED_MODIFIERS.contains(part.getModifier())) {
            throw new IllegalArgumentException(String.format("Invalid modifier [%s]", part.getModifier()));
        } else if (part.getIncrementModifier() != null && !"/".equals(part.getIncrementModifier())) {
            throw new IllegalArgumentException(String.format("Invalid increment modifier [%s]", part.getIncrementModifier()));
        }
    }
}

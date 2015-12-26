package fc.cron;

import java.util.Arrays;
import java.util.List;

enum CronFieldType {
    SECOND(0, 59, null),
    MINUTE(0, 59, null),
    HOUR(0, 23, null),
    DAY_OF_MONTH(1, 31, null),
    MONTH(1, 12, Arrays.asList("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")),
    DAY_OF_WEEK(1, 7, Arrays.asList("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")),
    YEAR(1970, 2199, null);

    private final int from;
    private final int to;
    private final List<String> names;

    CronFieldType(int from, int to, List<String> names) {
        this.from = from;
        this.to = to;
        this.names = names;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public List<String> getNames() {
        return names;
    }
}

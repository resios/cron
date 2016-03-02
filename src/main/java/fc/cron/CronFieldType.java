package fc.cron;

import java.util.Arrays;
import java.util.List;

enum CronFieldType {
    SECOND(0, 59),
    MINUTE(0, 59),
    HOUR(0, 23),
    DAY_OF_MONTH(1, 31),
    MONTH(1, 12, Arrays.asList("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")),
    DAY_OF_WEEK(1, 7, Arrays.asList("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")),
    DAY_OF_WEEK_US(1, 7, 7, 6, Arrays.asList("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")),
    YEAR(1970, 2199);

    private final int from;
    private final int to;
    private final int start;
    private final int end;
    private final List<String> names;

    CronFieldType(int from, int to) {
        this(from, to, null);
    }

    CronFieldType(int from, int to, List<String> names) {
        this(from, to, from, to, names);
    }

    CronFieldType(int from, int to, int start, int end, List<String> names) {
        this.from = from;
        this.to = to;
        this.start = start;
        this.end = end;
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

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
}

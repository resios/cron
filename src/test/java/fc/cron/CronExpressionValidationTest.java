package fc.cron;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.text.ParseException;
import java.util.Date;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;

public class CronExpressionValidationTest {

    private final String[] expressions = new String[]{
            "0 0 12 * * ?",
            "0 15 10 ? * *",
            "0 15 10 * * ?",
            "0 15 10 * * ? *",
            "0 15 10 * * ? 2005",
            "0 * 14 * * ?",
            "0 0/5 14 * * ?",
            "0 0/5 14,18 * * ?",
            "0 0-5 14 * * ?",
            "0 10,44 14 ? 3 WED",
            "0 15 10 ? * MON-FRI",
            "0 15 10 15 * ?",
            "0 15 10 L * ?",
            "0 15 10 L-2 * ?",
            "0 15 10 ? * FRIL",
            "0 15 10 ? * 6L",
            "0 15 10 ? * FRIL 2002-2005",
            "0 15 10 ? * 6L 2002-2005",
            "0 15 10 ? * FRI#3",
            "0 15 10 ? * 6#3",
            "0 0 12 1/5 * ?",
            "0 11 11 11 11 ?",
            "* * * * * ? *",
            "0/5 14,18,3-39,52 * ? JAN,MAR,SEP MON-FRI 2002-2010"
    };

    @Test
    public void testIsCompatibleWithQuartz() throws Exception {
        for (String expression : expressions) {
            testIsCompatibleWithQuartz(expression);
        }
    }

    private void testIsCompatibleWithQuartz(String cron) throws ParseException {
        CronExpression expression = new CronExpression(cron, true, true);
        org.quartz.CronExpression quartz = new org.quartz.CronExpression(cron);
        DateTimeZone zone = DateTimeZone.UTC;
        quartz.setTimeZone(zone.toTimeZone());

        DateTime last = new DateTime(2000, 1, 1, 0, 0, zone);

        for (int i = 0; i < 1000; i++) {
            try {
                Date date = quartz.getNextValidTimeAfter(last.toDate());
                DateTime next = null;
                try {
                    next = expression.nextTimeAfter(last, last.plusYears(10));
                } catch (NoSuchElementException ignored) {
                }

                assertEquals(String.format("next cron '%s' for date %s is", cron, last), next == null ? null : next.toDate(), date);
                if (next == null || next.minusYears(100).isBeforeNow()) {
                    break;
                }
                last = next;
            } catch (IllegalArgumentException e) {
                System.out.printf("next cron '%s' for date %s at iteration %d", cron, last, i);
                throw e;
            }
        }
    }
}
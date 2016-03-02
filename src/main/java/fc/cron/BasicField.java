package fc.cron;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class BasicField {
    private static final Pattern CRON_FELT_REGEXP = Pattern
            .compile("(?:                                             # start of group 1\n"
                            + "   (?:(?<all>\\*)|(?<any>\\?) # globalt flag ( *, ? )\n"
                            + " | (?:(?<last>L)((?<lastWeek>W)|(?:-(?<lastOffset>[0-9]{1,2}))?)))  # last modifier (L-4)\n"
                            + " | (?<start>[0-9]{1,4}|[a-z]{3,3})              # or start number or symbol\n"
                            + "      (?:                                        # start of group 2\n"
                            + "         (?<mod>L|W)                             # modifier (L,W)\n"
                            + "       | -(?<end>[0-9]{1,4}|[a-z]{3,3})        # or end nummer or symbol (in range)\n"
                            + "      )?                                         # end of group 2\n"
                            + ")                                              # end of group 1\n"
                            + "(?:(?<incMod>/|\\#)(?<inc>[0-9]{1,7}))?        # increment and increment modifier (/ or \\#)\n"
                    , Pattern.CASE_INSENSITIVE | Pattern.COMMENTS);

    protected final CronFieldType fieldType;
    protected final List<FieldPart> parts = new ArrayList<FieldPart>();
    private final BitSet values;

    BasicField(CronFieldType fieldType, String fieldExpr) {
        this.fieldType = fieldType;
        values = new BitSet(Math.min(fieldType.getTo() - fieldType.getFrom(), 64));
        parse(fieldExpr);
    }

    private void parse(String fieldExpr) { // NOSONAR
        String[] rangeParts = fieldExpr.split(",");
        for (String rangePart : rangeParts) {
            Matcher m = CRON_FELT_REGEXP.matcher(rangePart);
            if (!m.matches()) {
                throw new IllegalArgumentException(String.format("Invalid cron field '%s' for field [%s]", rangePart, fieldType));
            }
            String startNumber = m.group("start");
            String modifier = m.group("mod");
            String endNumber = m.group("end");
            String incrementModifier = m.group("incMod");
            String increment = m.group("inc");
            String tmp = null;

            FieldPart part = new FieldPart();
            part.setIncrement(999);
            if (startNumber != null) {
                part.setFrom(mapValue(startNumber));
                part.setModifier(modifier);
                if (endNumber != null) {
                    part.setTo(mapValue(endNumber));
                    part.setIncrement(1);
                } else if (increment != null) {
                    part.setTo(fieldType.getEnd());
                } else {
                    part.setTo(part.getFrom());
                }
            } else if (m.group("all") != null) {
                part.setFrom(fieldType.getStart());
                part.setTo(fieldType.getEnd());
                part.setIncrement(1);
            } else if ((tmp = m.group("any")) != null) {
                part.setModifier(tmp);

            } else if ((tmp = m.group("last")) != null) {
                part.setModifier(tmp);
                startNumber = m.group("lastOffset");
                if(startNumber != null){
                    part.setFrom(mapValue(startNumber));
                    part.setTo(part.getFrom());
                }
                if (m.group("lastWeek") != null) {
                    part.setModifier("LW");
                }
            } else {
                throw new IllegalArgumentException("Invalid cron part: " + rangePart);
            }

            if (increment != null) {
                part.setIncrementModifier(incrementModifier);
                part.setIncrement(Integer.valueOf(increment));
            }

            validateRange(part);
            validatePart(part);

            if (part.getModifier() != null || part.getIncrementModifier() != null && !"/".equals(part.getIncrementModifier())) {
                parts.add(part);
            } else if (part.getFrom() != null && part.getTo() != null) {
                int from = part.getFrom();
                int to = part.getTo();
                int partIncrement = part.getIncrement();
                final int min = fieldType.getFrom();
                if (from <= to) {
                    for (int i = from; i <= to; i += partIncrement) {
                        values.set(i - min);
                    }
                } else {
                    int i = from;
                    final int max = fieldType.getTo();
                    for (; i <= max; i += partIncrement) {
                        values.set(i - min);
                    }

                    for (i -= max - min + 1; i <= to; i += partIncrement) {
                        values.set(i - min);
                    }
                }
            }
        }
    }

    protected void validatePart(FieldPart part) {
        if (part.getModifier() != null) {
            throw new IllegalArgumentException(String.format("Invalid modifier [%s]", part.getModifier()));
        } else if (part.getIncrementModifier() != null && !"/".equals(part.getIncrementModifier())) {
            throw new IllegalArgumentException(String.format("Invalid increment modifier [%s]", part.getIncrementModifier()));
        }
    }

    private void validateRange(FieldPart part) {
        if ((part.getFrom() != null && part.getFrom() < fieldType.getFrom()) || (part.getTo() != null && part.getTo() > fieldType.getTo())) {
            throw new IllegalArgumentException(String.format("Invalid interval [%s-%s], must be %s<=_<=%s", part.getFrom(), part.getTo(), fieldType.getFrom(),
                    fieldType.getTo()));
        }
        //TODO: validate increment
    }

    protected Integer mapValue(String value) {
        Integer idx;
        if (fieldType.getNames() != null && (idx = fieldType.getNames().indexOf(value.toUpperCase(Locale.US))) >= 0) {
            return idx + 1;
        }
        return Integer.valueOf(value);
    }

    int nextValue(int previous) {
        int i = values.nextSetBit(previous - fieldType.getFrom());
        return i >= 0 ? i + fieldType.getFrom() : i;
    }

    boolean hasValues(){
        return !values.isEmpty();
    }

    boolean matches(int val) {
        return values.get(val - fieldType.getFrom());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BasicField{");
        sb.append("fieldType=").append(fieldType);
        sb.append(", parts=").append(parts);
        sb.append(", values=").append(values);
        sb.append('}');
        return sb.toString();
    }
}

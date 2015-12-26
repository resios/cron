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
                            + "   (?:(?<all>\\*)|(?<ignorer>\\?)|(?<last>L))  # globalt flag (L, ?, *)\n"
                            + " | (?<start>[0-9]{1,4}|[a-z]{3,3})              # or start number or symbol\n"
                            + "      (?:                                        # start of group 2\n"
                            + "         (?<mod>L|W)                             # modifier (L,W)\n"
                            + "       | -(?<end>[0-9]{1,4}|[a-z]{3,3})        # or end nummer or symbol (in range)\n"
                            + "      )?                                         # end of group 2\n"
                            + ")                                              # end of group 1\n"
                            + "(?:(?<inkmod>/|\\#)(?<ink>[0-9]{1,7}))?        # increment and increment modifier (/ or \\#)\n"
                    , Pattern.CASE_INSENSITIVE | Pattern.COMMENTS);

    protected final CronFieldType fieldType;
    protected final List<FieldPart> parts = new ArrayList<FieldPart>();
    protected final BitSet values;

    BasicField(CronFieldType fieldType, String fieldExpr) {
        this.fieldType = fieldType;
        values = new BitSet(fieldType.getTo() - fieldType.getFrom());
        parse(fieldExpr);
    }

    private void parse(String fieldExpr) { // NOSONAR
        String[] rangeParts = fieldExpr.split(",");
        for (String rangePart : rangeParts) {
            Matcher m = CRON_FELT_REGEXP.matcher(rangePart);
            if (!m.matches()) {
                throw new IllegalArgumentException("Invalid cron field '" + rangePart + "' for field [" + fieldType + "]");
            }
            String startNummer = m.group("start");
            String modifier = m.group("mod");
            String sluttNummer = m.group("end");
            String inkrementModifier = m.group("inkmod");
            String inkrement = m.group("ink");

            FieldPart part = new FieldPart();
            part.setIncrement(999);
            if (startNummer != null) {
                part.setFrom(mapValue(startNummer));
                part.setModifier(modifier);
                if (sluttNummer != null) {
                    part.setTo(mapValue(sluttNummer));
                    part.setIncrement(1);
                } else if (inkrement != null) {
                    part.setTo(fieldType.getTo());
                } else {
                    part.setTo(part.getFrom());
                }
            } else if (m.group("all") != null) {
                part.setFrom(fieldType.getFrom());
                part.setTo(fieldType.getTo());
                part.setIncrement(1);
            } else if (m.group("ignorer") != null) {
                part.setModifier(m.group("ignorer"));
            } else if (m.group("last") != null) {
                part.setModifier(m.group("last"));
            } else {
                throw new IllegalArgumentException("Invalid cron part: " + rangePart);
            }

            if (inkrement != null) {
                part.setIncrementModifier(inkrementModifier);
                part.setIncrement(Integer.valueOf(inkrement));
            }

            validateRange(part);
            validatePart(part);

            if (part.getModifier() != null || part.getIncrementModifier() != null && !part.getIncrementModifier().equals("/")) {
                parts.add(part);
            } else if (part.getFrom() != null && part.getTo() != null) {
                for (int i = part.getFrom(); i <= part.getTo(); i += part.getIncrement()) {
                    values.set(i - fieldType.getFrom());
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
        } else if (part.getFrom() != null && part.getTo() != null && part.getFrom() > part.getTo()) {
            throw new IllegalArgumentException(
                    String.format(
                            "Invalid interval [%s-%s].  Rolling periods are not supported (ex. 5-1, only 1-5) since this won't give a deterministic result. Must be %s<=_<=%s",
                            part.getFrom(), part.getTo(), fieldType.getFrom(), fieldType.getTo()));
        }
    }

    protected Integer mapValue(String value) {
        Integer idx;
        if (fieldType.getNames() != null && (idx = fieldType.getNames().indexOf(value.toUpperCase(Locale.getDefault()))) >= 0) {
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BasicField{");
        sb.append("fieldType=").append(fieldType);
        sb.append(", parts=").append(parts);
        sb.append(", values=").append(values);
        sb.append('}');
        return sb.toString();
    }

    boolean matches(int val) {
        return values.get(val - fieldType.getFrom());
    }
}

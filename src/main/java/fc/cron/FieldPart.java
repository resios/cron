package fc.cron;

class FieldPart {
    private Integer from;
    private Integer to;
    private Integer increment;
    private String modifier;
    private String incrementModifier;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FieldPart{");
        sb.append("from=").append(getFrom());
        sb.append(", to=").append(getTo());
        sb.append(", increment=").append(getIncrement());
        sb.append(", modifier='").append(getModifier()).append('\'');
        sb.append(", incrementModifier='").append(getIncrementModifier()).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public Integer getFrom() {
        return from;
    }

    public void setFrom(Integer from) {
        this.from = from;
    }

    public Integer getTo() {
        return to;
    }

    public void setTo(Integer to) {
        this.to = to;
    }

    public Integer getIncrement() {
        return increment;
    }

    public void setIncrement(Integer increment) {
        this.increment = increment;
    }

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier;
    }

    public String getIncrementModifier() {
        return incrementModifier;
    }

    public void setIncrementModifier(String incrementModifier) {
        this.incrementModifier = incrementModifier;
    }
}

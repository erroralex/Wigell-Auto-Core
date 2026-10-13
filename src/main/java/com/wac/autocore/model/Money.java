package com.wac.autocore.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Objects;

// Värdeobjekt som representerar ett penningbelopp med två decimaler. Immutable.
public final class Money implements Comparable<Money> {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    private final BigDecimal amount; // alltid med två decimaler, avrundat med HALF_UP

    private Money(BigDecimal amount) {
        this.amount = Objects.requireNonNull(amount, "Money value must not be null")
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal value) { return new Money(value); }
    public static Money of(double value) { return new Money(BigDecimal.valueOf(value)); }
    public static Money of(String text) {
        Objects.requireNonNull(text, "Money text must not be null");
        return new Money(new BigDecimal(text.trim().replace(',', '.')));
    }

    public static Money sum(Collection<Money> values) {
        Money total = ZERO;
        for (Money m : values) total = total.add(m);
        return total;
    }

    public Money add(Money o) { return new Money(amount.add(o.amount)); }
    public Money subtract(Money o) { return new Money(amount.subtract(o.amount)); }
    public Money multiply(BigDecimal factor) { return new Money(amount.multiply(factor)); }
    public Money min(Money o) { return compareTo(o) <= 0 ? this : o; }

    public boolean isNegative() { return amount.signum() < 0; }
    public boolean isZero() { return amount.signum() == 0; }
    public boolean isPositive() { return amount.signum() > 0; }
    public BigDecimal toBigDecimal() { return amount; }
    public double toDouble() { return amount.doubleValue(); } // bara för konvertern

    @Override public int compareTo(Money o) { return amount.compareTo(o.amount); }

    @Override public boolean equals(Object o) {
        return this == o || (o instanceof Money && amount.equals(((Money) o).amount));
    }

    @Override public int hashCode() { return amount.hashCode(); }
    @Override public String toString() { return amount.toPlainString(); }

}
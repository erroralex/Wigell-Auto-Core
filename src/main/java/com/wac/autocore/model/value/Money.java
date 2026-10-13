package com.wac.autocore.model.value;

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

    public static Money of(BigDecimal value) {
        return new Money(value);
    }

    public static Money of(double value) {
        return new Money(BigDecimal.valueOf(value));
    }

    public static Money of(String text) {
        Objects.requireNonNull(text, "Money text must not be null");
        return new Money(new BigDecimal(text.trim().replace(',', '.')));
    }

    public static Money sum(Collection<Money> values) {
        Objects.requireNonNull(values, "Money collection must not be null");

        Money total = ZERO;

        for (Money value : values) {
            total = total.add(
                    Objects.requireNonNull(
                            value, "Money collection must not contain null values"
                    )
            );
        }

        return total;
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "Money to add must not be null");
        return new Money(amount.add(other.amount));
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other, "Money to subtract must not be null");
        return new Money(amount.subtract(other.amount));
    }

    public Money multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "Multiplication factor must not be null");
        return new Money(amount.multiply(factor));
    }

    public Money min(Money other) {
        Objects.requireNonNull(other, "Money to compare must not be null");
        return compareTo(other) <= 0 ? this : other;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public BigDecimal toBigDecimal() {
        return amount;
    }

    public double toDouble() { // Bara för konvertern så att den kan sparas i databasen som en double. Använd inte i affärslogik.
        return amount.doubleValue();
    }

    @Override
    public int compareTo(Money other) {
        Objects.requireNonNull(other, "Money to compare must not be null");
        return amount.compareTo(other.amount);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Money)) {
            return false;
        }

        Money other = (Money) obj;
        return amount.equals(other.amount);
    }

    @Override
    public int hashCode() {
        return amount.hashCode();
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }

}
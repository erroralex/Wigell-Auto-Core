package com.wac.autocore.standalone;

import com.wac.autocore.i18n.Translator;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.model.value.MoneyFormat;

import java.io.PrintStream;
import java.util.Objects;

/**
 * <b>StandaloneInvoicePrinter</b>
 * <p>Responsibility: Prints an invoice's lines, discounts and total in the standalone report,
 * with the same language keys as the GUI's invoice view.</p>
 * <p>Amounts are formatted by {@link MoneyFormat} for the report's language, the same way
 * the GUI and the language bundles format them.</p>
 */
final class StandaloneInvoicePrinter {

    private static final String ROW = "  %-20s %18s %18s %18s%n";

    private final PrintStream out;
    private final Translator translator;

    StandaloneInvoicePrinter(PrintStream out, Translator translator) {
        this.out = Objects.requireNonNull(out, "out");
        this.translator = Objects.requireNonNull(translator, "translator");
    }

    void print(Invoice invoice) {
        Objects.requireNonNull(invoice, "invoice");

        out.println();
        out.println(translator.get("invoice.detailsTitle", invoice.getId()));
        out.printf(ROW,
                translator.get("table.serviceItem"),
                translator.get("invoice.originalPrice"),
                translator.get("table.discount"),
                translator.get("table.total"));

        for (InvoiceLine line : invoice.getLines()) {
            out.printf(ROW,
                    line.getServiceItemName(),
                    money(line.getAmount()),
                    discount(line.getDiscount()),
                    money(line.getTotal()));
        }

        out.println();
        out.println("  " + translator.get("invoice.subtotal", invoice.getAmount()));
        out.println("  " + translator.get("invoice.totalDiscount", invoice.getDiscount()));
        out.println("  " + translator.get("invoice.grandTotal", invoice.getTotalAmount()));
    }

    private String money(Money amount) {
        return MoneyFormat.format(amount, translator.getLocale());
    }

    // A discount is shown as a reduction, so a non-zero discount gets a minus sign.
    private String discount(Money amount) {
        return amount.isZero() ? money(amount) : "-" + money(amount);
    }
}

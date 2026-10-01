package com.wac.autocore.view.dialog;

import com.wac.autocore.model.Booking;
import com.wac.autocore.service.BookingService;
import com.wac.autocore.service.LanguageManager;
import com.wac.autocore.view.util.DialogUtil;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * <b>EditBookingServicesDialog</b>
 * <p>Ansvar: Dialog för att redigera tjänster på en befintlig bokning.</p>
 */
public class EditBookingServicesDialog extends Dialog<EditBookingServicesDialog.Result> {

    private static final LanguageManager lang = LanguageManager.getInstance();

    private final BookingService bookingService;
    private final Booking booking;

    private final Label infoLabel = new Label();
    private final VBox serviceItemsBox = new VBox(8);
    private final Label estimatedTimeLabel = new Label("Uppskattad Tid: ");
    private final Label basePriceLabel = new Label("Pris: ");

    private final ButtonType saveButtonType = new ButtonType(lang.get("btn.save"), ButtonBar.ButtonData.OK_DONE);
    private final ButtonType cancelButtonType = new ButtonType(lang.get("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

    public EditBookingServicesDialog(Booking booking, BookingService bookingService) {
        this.booking = booking;
        this.bookingService = bookingService;

        setTitle(lang.get("booking.edit"));
        setHeaderText(lang.get("booking.editServices"));

        DialogUtil.applyTheme(this);
    }


    public static class Result {
        private final List<Integer> serviceItemIds;

        public Result(List<Integer> serviceItemIds) {
            this.serviceItemIds = serviceItemIds;
        }

        public List<Integer> getServiceItemIds() {
            return serviceItemIds;
        }
    }
}
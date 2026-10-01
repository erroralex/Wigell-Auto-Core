package com.wac.autocore.view.component;

import com.wac.autocore.service.LanguageManager;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.Objects;
import java.util.function.Function;

/**
 * MultiSelectListView är en ListView som visar objekt med en Ta bort-knapp för varje rad.
 * Den tar emot en labelProvider som används för att generera texten som visas för varje objekt.
 * Listan är generisk och vet inget om vad objekten är eller hur de väljs; det sköts av den komponent som äger listan.
 * @link getItems() är de valda objekten. Ta bort-knappen tar bort raden ur den listan.
 * Om setCellFactory skrivs över försvinner Ta bort-knapparna.
 **/
public class MultiSelectListView<T> extends ListView<T> {

    private static final LanguageManager lang = LanguageManager.getInstance();
    private final Function<T, String> labelProvider;

    public MultiSelectListView(Function<T, String> labelProvider) {
        this.labelProvider = Objects.requireNonNull(labelProvider, "labelProvider");

        getStyleClass().add("multi-select-list");
        setCellFactory(listView -> new RemovableCell());
    }

    // Sätter texten som visas när listan är tom.
    public void setPlaceholderText(String text) {
        Label placeholder = new Label(text);
        placeholder.getStyleClass().add("text-secondary");
        placeholder.setWrapText(true);
        setPlaceholder(placeholder);
    }

    // Cellen som visar ett objekt med en Ta bort-knapp.
    private class RemovableCell extends ListCell<T> {

        private final Label label = new Label();
        private final Button removeButton = new Button(lang.get("btn.remove"));
        private final Tooltip removeTooltip = new Tooltip();
        private final HBox row = new HBox(label, removeButton);

        public RemovableCell() {
            label.getStyleClass().add("multi-select-cell");
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            // Cellen följer listans bredd, så att ingen horisontell scrollbar uppstår vid zoom
            setPrefWidth(0);

            label.setMinWidth(0);
            label.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(label, Priority.ALWAYS);
        }

        // Tar bort raden från listan när knappen trycks
        private void removeThisRow() {
            int index = getIndex();
            if (!isEmpty() && index >= 0 && index < getListView().getItems().size()) {
                getListView().getItems().remove(index);
            }
        }

        // Uppdaterar cellens innehåll när listan ändras
        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            // Sätter texten via labelProvider och uppdaterar tooltip
            String text = labelProvider.apply(item);
            label.setText(text);
            removeTooltip.setText(lang.get("multiSelect.removeTooltip", text));
            setGraphic(row);

        }

    }

}

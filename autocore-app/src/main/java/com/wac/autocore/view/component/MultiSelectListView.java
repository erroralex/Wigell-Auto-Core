package com.wac.autocore.view.component;

import com.wac.autocore.view.i18n.LanguageManager;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.Objects;
import java.util.function.Function;

/**
 * MultiSelectListView is a ListView that shows items with a Remove button for each row.
 * It takes a labelProvider that is used to generate the text shown for each item.
 * The list is generic and knows nothing about what the items are or how they are selected; that is handled by the component that owns the list.
 * @link getItems() are the selected items. The Remove button removes the row from that list.
 * If setCellFactory is overridden, the Remove buttons disappear.
 **/
public class MultiSelectListView<T> extends ListView<T> {

    private static final LanguageManager lang = LanguageManager.getInstance();
    private final Function<T, String> labelProvider;

    public MultiSelectListView(Function<T, String> labelProvider) {
        this.labelProvider = Objects.requireNonNull(labelProvider, "labelProvider");

        getStyleClass().add("multi-select-list");
        setCellFactory(listView -> new RemovableCell());
    }

    // Sets the text shown when the list is empty.
    public void setPlaceholderText(String text) {
        Label placeholder = new Label(text);
        placeholder.getStyleClass().add("text-secondary");
        placeholder.setWrapText(true);
        setPlaceholder(placeholder);
    }

    // The cell that shows an item with a Remove button.
    private class RemovableCell extends ListCell<T> {

        private final Label label = new Label();
        private final Button removeButton = new Button(lang.get("btn.remove"));
        private final Tooltip removeTooltip = new Tooltip();
        private final HBox row = new HBox(label, removeButton);

        public RemovableCell() {
            label.getStyleClass().add("multi-select-cell");
            row.getStyleClass().add("multi-select-row");

            removeButton.getStyleClass().add("multi-select-remove");
            removeButton.setTooltip(removeTooltip);
            removeButton.setOnAction(event -> removeThisRow());

            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            // The cell follows the list's width, so no horizontal scrollbar appears when zooming
            setPrefWidth(0);

            label.setMinWidth(0);
            label.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(label, Priority.ALWAYS);
        }

        // Removes the row from the list when the button is pressed
        private void removeThisRow() {
            int index = getIndex();
            if (!isEmpty() && index >= 0 && index < getListView().getItems().size()) {
                getListView().getItems().remove(index);
            }
        }

        // Updates the cell's content when the list changes
        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            // Sets the text via labelProvider and updates the tooltip
            String text = labelProvider.apply(item);
            label.setText(text);
            removeTooltip.setText(lang.get("multiSelect.removeTooltip", text));
            setGraphic(row);

        }

    }

}

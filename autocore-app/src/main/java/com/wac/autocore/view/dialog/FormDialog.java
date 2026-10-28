package com.wac.autocore.view.dialog;

import com.wac.autocore.view.i18n.LanguageManager;
import com.wac.autocore.view.util.DialogUtil;
import com.wac.autocore.view.util.ErrorFacade;
import javafx.beans.value.ObservableBooleanValue;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

/**
 * <b>FormDialog</b>
 * <p>Responsibility: Common base for all form dialogs (Template Method). {@link #initDialog()}
 * builds theme, title, content, buttons, save handler and result in a fixed order. The theme
 * comes first since {@link DialogUtil#applyTheme} wraps new content in a scroll pane.</p>
 *
 * <p>Subclasses implement {@code buildContent()}, {@code submit()} and {@code buildResult()},
 * may override the hooks {@code buildHeaderText()} and {@code saveDisabled()}, and call
 * {@code initDialog()} last in their constructor. If {@code submit()} throws, the dialog stays
 * open. Use {@code addEventHandler(DialogEvent.DIALOG_SHOWN, ...)}, not {@code setOnShown}.</p>
 */
public abstract class FormDialog<T> extends Dialog<T> {

    protected final LanguageManager lang = LanguageManager.getInstance();

    private final ButtonType saveButtonType = new ButtonType(lang.get("btn.save"), ButtonBar.ButtonData.OK_DONE);
    private final ButtonType cancelButtonType = new ButtonType(lang.get("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

    protected abstract String getTitleKey();

    protected String buildHeaderText() {
        return null;
    }

    protected abstract Node buildContent();

    protected abstract void submit();

    protected abstract T buildResult();

    protected ObservableBooleanValue saveDisabled() {
        return null;
    }

    public final void initDialog() {
        applyStyling();
        buildHeader();
        getDialogPane().setContent(buildContent());
        buildButtons();
        installSaveHandler();
        installResultConverter();
    }

    private void applyStyling() {
        DialogUtil.applyTheme(this);
        getDialogPane().getStyleClass().add("form-dialog");
    }

    private void buildHeader() {
        setTitle(lang.get(getTitleKey()));

        String headerText = buildHeaderText();
        if (headerText != null) {
            setHeaderText(headerText);
        }
    }

    private void buildButtons() {
        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);
        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
        Button cancelButton = (Button) getDialogPane().lookupButton(cancelButtonType);
        saveButton.getStyleClass().addAll("btn", "btn-primary");
        cancelButton.getStyleClass().addAll("btn", "btn-secondary");

        ObservableBooleanValue disabled = saveDisabled();
        if (disabled != null) {
            saveButton.disableProperty().bind(disabled);
        }
    }

    private void installSaveHandler() {
        getDialogPane().lookupButton(saveButtonType).addEventFilter(
                ActionEvent.ACTION, event -> {
                    try {
                        submit();
                    } catch (RuntimeException e) {
                        ErrorFacade.handle(e);
                        event.consume();
                    }
                });
    }

    private void installResultConverter() {
        setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                return buildResult();
            } else {
                return null;
            }
        });
    }
}

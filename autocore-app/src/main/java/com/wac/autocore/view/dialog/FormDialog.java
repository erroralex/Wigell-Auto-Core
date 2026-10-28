package com.wac.autocore.view.dialog;

import com.wac.autocore.view.i18n.LanguageManager;
import com.wac.autocore.view.util.DialogUtil;
import com.wac.autocore.view.util.ErrorFacade;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

/**
 * <b>FormDialog</b>
 * <p>Responsibility: Common base for all form dialogs. Builds title, header, content,
 * buttons and theme in a fixed order, and keeps the dialog open when saving fails.</p>
 *
 * <p>Template Method (GoF): {@link #initDialog()} is the template method and fixes the
 * order of the steps. Subclasses supply the varying steps ({@link #buildContent()},
 * {@link #submit()}, {@link #buildResult()}) and may override the hook
 * {@link #getHeaderKey()}. Mirrors {@link com.wac.autocore.view.BaseView} for views.</p>
 *
 * <p>Subclasses must call {@link #initDialog()} last in their constructor. If the base
 * class called it, {@link #buildContent()} would run before the subclass fields are
 * initialized, and they would be {@code null}.</p>
 *
 * @param <T>
 *         the type returned by {@link #showAndWait()} when the user saves
 */
public abstract class FormDialog<T> extends Dialog<T> {

    protected final LanguageManager lang = LanguageManager.getInstance();

    private final ButtonType saveButtonType = new ButtonType(lang.get("btn.save"), ButtonBar.ButtonData.OK_DONE);
    private final ButtonType cancelButtonType = new ButtonType(lang.get("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

    protected abstract String getTitleKey();

    protected String getHeaderKey() {
        return null;
    }

    protected abstract Node buildContent();

    protected abstract void submit();

    protected abstract T buildResult();

    /**
     * Template method: builds the dialog in a fixed order. Final so the order cannot be changed.
     * <ol>
     *   <li>Theme and owner. Must come before the content, since {@link DialogUtil#applyTheme}
     *       listens for new content and wraps it in a scroll pane.</li>
     *   <li>Title and optional header text.</li>
     *   <li>The subclass content from {@link #buildContent()}.</li>
     *   <li>Save and cancel buttons.</li>
     *   <li>Save handler: runs {@link #submit()} and keeps the dialog open if it throws.</li>
     *   <li>Result converter: returns {@link #buildResult()} on save, otherwise {@code null}.</li>
     * </ol>
     * Must be called last in the subclass constructor.
     */
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
        if (getHeaderKey() != null) {
            setHeaderText(lang.get(getHeaderKey()));
        }
    }

    private void buildButtons() {
        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);
        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
        Button cancelButton = (Button) getDialogPane().lookupButton(cancelButtonType);
        saveButton.getStyleClass().addAll("btn", "btn-primary");
        cancelButton.getStyleClass().addAll("btn", "btn-secondary");
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

package cz.cuni.mff.xrg.uv.extractor.sukl;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import eu.unifiedviews.dpu.config.DPUConfigException;
import eu.unifiedviews.helpers.dpu.vaadin.dialog.AbstractDialog;

public class SuklVaadinDialog extends AbstractDialog<SuklConfig_V1> {

    private CheckBox checkCountMissing;

    private CheckBox checkFailOnDownloadError;

    private CheckBox checkNewFilesToOutput;

    private TextField txtDeleteStorage;

    private CheckBox checkDelteOnError;

    public SuklVaadinDialog() {
        super(Sukl.class);
    }

    @Override
    public void setConfiguration(SuklConfig_V1 c) throws DPUConfigException {
        checkCountMissing.setValue(c.isCountNumberOfMissing());
        checkFailOnDownloadError.setValue(c.isFailOnDownloadError());
        checkNewFilesToOutput.setValue(c.isNewFileToOutput());
        txtDeleteStorage.setValue(c.getDeletedFileStorage());
        checkDelteOnError.setValue(c.isDeletePagesOnError());
    }

    @Override
    public SuklConfig_V1 getConfiguration() throws DPUConfigException {
        final SuklConfig_V1 c = new SuklConfig_V1();
        c.setCountNumberOfMissing(checkCountMissing.getValue());
        c.setFailOnDownloadError(checkFailOnDownloadError.getValue());
        c.setNewFileToOutput(checkNewFilesToOutput.getValue());
        c.setDeletedFileStorage(txtDeleteStorage.getValue());
        c.setDeletePagesOnError(checkDelteOnError.getValue());
        return c;
    }

    @Override
    protected void buildDialogLayout() {
        final VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setWidth("100%");
        mainLayout.setHeight("-1px");
        mainLayout.setSpacing(true);
        mainLayout.setMargin(true);

        checkCountMissing = new CheckBox("Count missing");
        mainLayout.addComponent(checkCountMissing);

        checkFailOnDownloadError = new CheckBox("Fail on download error");
        mainLayout.addComponent(checkFailOnDownloadError);

        checkNewFilesToOutput = new CheckBox("Add new files to output (filesOutNewTexts)");
        mainLayout.addComponent(checkNewFilesToOutput);

        txtDeleteStorage = new TextField("Where to store deleted files");
        txtDeleteStorage.setWidth("100%");
        mainLayout.addComponent(txtDeleteStorage);

        checkDelteOnError = new CheckBox("Delete index page on download error");
        mainLayout.addComponent(checkDelteOnError);

        this.setCompositionRoot(mainLayout);
    }

}

package cz.opendata.linked.ares.updates;

import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;

import eu.unifiedviews.dpu.config.DPUConfigException;
import eu.unifiedviews.helpers.dpu.vaadin.dialog.AbstractDialog;

/**
 * DPU's configuration dialog. User can use this dialog to configure DPU configuration.
 *
 */
public class ExtractorDialog extends AbstractDialog<ExtractorConfig> {

    private static final long serialVersionUID = 7003725620084616056L;

    private GridLayout mainLayout;
    private TextField interval;
    private TextField timeout;
    
    public ExtractorDialog() {
        super(Extractor.class);
    }  

    @Override
    protected void buildDialogLayout() {
        // common part: create layout
        mainLayout = new GridLayout(1, 2);
        mainLayout.setImmediate(false);
        mainLayout.setWidth("100%");
        mainLayout.setHeight("-1px");
        mainLayout.setMargin(false);
        mainLayout.setSpacing(true);

        // top-level component properties
        setWidth("100%");
        setHeight("100%");

        interval = new TextField();
        interval.setCaption("Interval between downloads:");
        mainLayout.addComponent(interval);
        
        timeout = new TextField();
        timeout.setCaption("Timeout for download:");
        
        mainLayout.addComponent(timeout);
        
        Panel panel = new Panel();
        panel.setSizeFull();
        panel.setContent(mainLayout);
        setCompositionRoot(panel);

    }    
     
    @Override
    public void setConfiguration(ExtractorConfig conf) throws DPUConfigException {
        interval.setValue(Integer.toString(conf.getInterval()));
        timeout.setValue(Integer.toString(conf.getTimeout()));
    
    }

    @Override
    public ExtractorConfig getConfiguration() throws DPUConfigException {
        ExtractorConfig conf = new ExtractorConfig();
        try {
            conf.setInterval(Integer.parseInt(interval.getValue()));
        } catch (InvalidValueException e) {
        }
        try {
            conf.setTimeout(Integer.parseInt(timeout.getValue()));
        } catch (InvalidValueException e) {
        }

        return conf;
    }
    
}

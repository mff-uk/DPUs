package cz.opendata.linked.cz.gov.agendy;

import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.TextField;

import cz.cuni.mff.xrg.odcs.commons.configuration.ConfigException;
import cz.cuni.mff.xrg.odcs.commons.module.dialog.BaseConfigDialog;

/**
 * DPU's configuration dialog. User can use this dialog to configure DPU configuration.
 *
 */
public class ExtractorDialog extends BaseConfigDialog<ExtractorConfig> {

    private GridLayout mainLayout;
    private CheckBox chkRewriteCache;
    private TextField interval;
    private TextField timeout;
    
	public ExtractorDialog() {
		super(ExtractorConfig.class);
        buildMainLayout();
        setCompositionRoot(mainLayout);        
    }  
	
    private GridLayout buildMainLayout() {
        // common part: create layout
        mainLayout = new GridLayout(1, 2);
        mainLayout.setImmediate(false);
        mainLayout.setWidth("100%");
        mainLayout.setHeight("100%");
        mainLayout.setMargin(false);
        //mainLayout.setSpacing(true);

        // top-level component properties
        setWidth("100%");
        setHeight("100%");
                
        chkRewriteCache = new CheckBox("Ignore (Rewrite) document cache:");
        chkRewriteCache.setDescription("When selected, documents cache will be ignored and rewritten.");
        chkRewriteCache.setWidth("100%");
        
        mainLayout.addComponent(chkRewriteCache);

        interval = new TextField();
        interval.setCaption("Interval between downloads:");
        mainLayout.addComponent(interval);
        
        timeout = new TextField();
        timeout.setCaption("Timeout for download:");
        mainLayout.addComponent(timeout);
        
        return mainLayout;
    }	
     
	/**
	 * 
	 */
	private static final long serialVersionUID = -8158163219102623590L;
	
	@Override
	public void setConfiguration(ExtractorConfig conf) throws ConfigException {
		chkRewriteCache.setValue(conf.rewriteCache);
		interval.setValue(Integer.toString(conf.interval));
		timeout.setValue(Integer.toString(conf.timeout));
	}

	@Override
	public ExtractorConfig getConfiguration() throws ConfigException {
		ExtractorConfig conf = new ExtractorConfig();
		conf.rewriteCache = chkRewriteCache.getValue();
		try { Integer.parseInt(interval.getValue()); } catch (InvalidValueException e) { return conf;}
		conf.interval = Integer.parseInt(interval.getValue());
		try { Integer.parseInt(timeout.getValue()); } catch (InvalidValueException e) { return conf;}
		conf.timeout = Integer.parseInt(timeout.getValue());
		return conf;
	}
	
}

package cz.cuni.mff.xrg.intlib.extractor.legislation.decisions;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.Validator;
import com.vaadin.ui.*;
import cz.cuni.mff.xrg.uv.boost.dpu.addon.AddonInitializer;
import cz.cuni.mff.xrg.uv.boost.dpu.gui.AdvancedVaadinDialogBase;
import eu.unifiedviews.dpu.config.DPUConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DPU's configuration dialog. User can use this dialog to configure DPU
 * configuration.
 *
 */
public class UnzipperDialog extends AdvancedVaadinDialogBase<UnzipperConfig> {

	private static final Logger log = LoggerFactory.getLogger(
			UnzipperDialog.class);

	private VerticalLayout mainLayout;

	private TextField dateTo;

	private TextField dateFrom;

	private TextField maxNumOfExtractedDecisions;

	private CheckBox cbCurrentDay;

	private CheckBox cbSinceLastSuccess;

	public UnzipperDialog() {
		super(UnzipperConfig.class,  AddonInitializer.noAddons());
		buildMainLayout();
		Panel panel = new Panel();
		panel.setSizeFull();
		panel.setContent(mainLayout);
		setCompositionRoot(panel);
	}

	private VerticalLayout buildMainLayout() {
		// common part: create layout
		mainLayout = new VerticalLayout();
		mainLayout.setImmediate(false);
		mainLayout.setWidth("100%");
		mainLayout.setHeight("-1px");
		mainLayout.setMargin(false);
        mainLayout.setSpacing(true);

		// top-level component properties
		setWidth("100%");
		setHeight("100%");

		// textFieldPath
		dateFrom = new TextField();
		dateFrom.setNullRepresentation("");
		dateFrom.setCaption("Date From (DD/MM/YYYY):");
		dateFrom.setImmediate(false);
		dateFrom.setWidth("100%");
		dateFrom.setHeight("-1px");
		dateFrom.setInputPrompt("09/07/2013");
		dateFrom.addValidator(new Validator() {
			@Override
			public void validate(Object value) throws Validator.InvalidValueException {
				if (value.getClass() == String.class && !((String) value)
						.isEmpty()) {
					return;
				}
				throw new Validator.InvalidValueException(
						"Date from must be filled!");
			}
		});
		mainLayout.addComponent(dateFrom);

		dateTo = new TextField();
		dateTo.setNullRepresentation("");
		dateTo.setCaption("Date To (DD/MM/YYYY):");
		dateTo.setImmediate(false);
		dateTo.setWidth("100%");
		dateTo.setHeight("-1px");
		dateTo.setInputPrompt("10/07/2013");
//        dateTo.addValidator(new Validator() {
//            @Override
//            public void validate(Object value) throws Validator.InvalidValueException {
//                if (value.getClass() == String.class && !((String) value).isEmpty()) {
//                    return;
//                }
//                throw new Validator.InvalidValueException("Path must be filled!");
//            }
//        });
		mainLayout.addComponent(dateTo);

		maxNumOfExtractedDecisions = new TextField();
		maxNumOfExtractedDecisions.setNullRepresentation("");
		maxNumOfExtractedDecisions.setCaption(
				"Maximum number of extracted decisions:");
		maxNumOfExtractedDecisions.setImmediate(false);
		maxNumOfExtractedDecisions.setWidth("100%");
		maxNumOfExtractedDecisions.setHeight("-1px");
//        dateTo.addValidator(new Validator() {
//            @Override
//            public void validate(Object value) throws Validator.InvalidValueException {
//                if (value.getClass() == String.class && !((String) value).isEmpty()) {
//                    return;
//                }
//                throw new Validator.InvalidValueException("Path must be filled!");
//            }
//        });
		mainLayout.addComponent(maxNumOfExtractedDecisions);

		cbCurrentDay = new CheckBox("Process yesterday");
		cbCurrentDay.addValueChangeListener(new ValueChangeListener() {
			public void valueChange(ValueChangeEvent event) {
				// Copy the value to the other checkbox.
				boolean value = (Boolean) event.getProperty().getValue();
				dateFrom.setEnabled(!value);
				dateTo.setEnabled(!value);
				cbSinceLastSuccess.setEnabled(!value);
			}
		});
		cbCurrentDay.setValue(false);

		cbSinceLastSuccess = new CheckBox(
				"Process the days from last successful run up to yesterday");
		cbSinceLastSuccess.addValueChangeListener(new ValueChangeListener() {
			public void valueChange(ValueChangeEvent event) {
				// Copy the value to the other checkbox.
				boolean value = (Boolean) event.getProperty().getValue();
				dateFrom.setEnabled(!value);
				dateTo.setEnabled(!value);
				cbCurrentDay.setEnabled(!value);
			}
		});
		cbSinceLastSuccess.setValue(false);

		mainLayout.addComponent(cbCurrentDay);
		mainLayout.addComponent(cbSinceLastSuccess);

		return mainLayout;
	}

        @Override
        protected void setConfiguration(UnzipperConfig conf) throws DPUConfigException {
            
            if (!conf.getDateTO().isEmpty()) {
			dateTo.setValue(conf.getDateTO());
		}
		if (!conf.getDateFrom().isEmpty()) {
			dateFrom.setValue(conf.getDateFrom());
		}
		maxNumOfExtractedDecisions.setValue(String.valueOf(conf
				.getMaxExtractedDecisions()));

		cbCurrentDay.setValue(conf.isCurrentDay());
		cbSinceLastSuccess.setValue(conf.isFromLastSuccess());
        }
        
	
	@Override
	public UnzipperConfig getConfiguration() throws DPUConfigException {
		//get the conf from the dialog
		if (!dateFrom.isValid()) {
			throw new DPUConfigException("Date from is not present");
		}

        //TODO validate from/to dates better
		int maxNumOfDec;
		try {
			maxNumOfDec = Integer.parseInt(maxNumOfExtractedDecisions.getValue()
					.trim());
		} catch (NumberFormatException e) {
			log.error(
					"Cannot store the max num of extracted decisions, default is used");
			log.debug(e.getLocalizedMessage());
			UnzipperConfig conf = new UnzipperConfig(dateFrom.getValue().trim(),
					dateTo.getValue().trim());
			return conf;
		}

		UnzipperConfig conf = new UnzipperConfig(dateFrom.getValue().trim(),
				dateTo.getValue().trim(), maxNumOfDec, cbCurrentDay.getValue(),
				cbSinceLastSuccess.getValue());
		return conf;

	}

    }

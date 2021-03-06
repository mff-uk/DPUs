package cz.cuni.mff.xrg.intlib.extractor.legislation.decisions;

import com.vaadin.data.Validator;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import cz.cuni.mff.xrg.odcs.commons.configuration.ConfigException;
import cz.cuni.mff.xrg.odcs.commons.module.dialog.BaseConfigDialog;


/**
 * DPU's configuration dialog. NO Dialog needed for this DPU
 *
 */
public class JTaggerAnnotatorDialog extends BaseConfigDialog<JTaggerAnnotatorConfig> {

	private VerticalLayout mainLayout;

	private TextField mode; //Path

	public JTaggerAnnotatorDialog() {
		super(JTaggerAnnotatorConfig.class);
		buildMainLayout();
		setCompositionRoot(mainLayout);
	}

	@Override
	public void setConfiguration(JTaggerAnnotatorConfig conf) throws ConfigException {
		mode.setValue(conf.getMode());
	}

	@Override
	public JTaggerAnnotatorConfig getConfiguration() throws ConfigException {

		//get the conf from the dialog
		if (!(mode.getValue().equals("nscr") || mode.getValue().equals("uscr"))) {
			throw new ConfigException("Mode is not correctly specified.");
		}

		JTaggerAnnotatorConfig conf = new JTaggerAnnotatorConfig(mode.getValue()
				.trim());
		return conf;

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
		mode = new TextField();
		mode.setNullRepresentation("");
		mode.setCaption("Mode (nscr/uscr):");
		mode.setImmediate(false);
		mode.setWidth("100%");
		mode.setHeight("-1px");
		mode.setInputPrompt("");
		mode.addValidator(new Validator() {
			@Override
			public void validate(Object value) throws Validator.InvalidValueException {
				if (value.getClass() == String.class && !((String) value)
						.isEmpty()) {
					if ((mode.getValue().equals("nscr") || mode.getValue()
							.equals("uscr"))) {
						return;
					} else {
						throw new Validator.InvalidValueException(
								"Mode is not correctly specified! Only values nscr and uscr allowed");
					}
				}
				throw new Validator.InvalidValueException(
						"Mode must be specified!");
			}
		});

		mainLayout.addComponent(mode);

		return mainLayout;

	}
}

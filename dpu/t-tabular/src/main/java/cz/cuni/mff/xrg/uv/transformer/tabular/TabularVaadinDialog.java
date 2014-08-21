package cz.cuni.mff.xrg.uv.transformer.tabular;

import com.vaadin.data.Property;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.TabSheet.Tab;
import cz.cuni.mff.xrg.uv.boost.dpu.addon.AddonInitializer;
import cz.cuni.mff.xrg.uv.boost.dpu.gui.AdvancedVaadinDialogBase;
import cz.cuni.mff.xrg.uv.transformer.tabular.column.ColumnInfo_V1;
import cz.cuni.mff.xrg.uv.transformer.tabular.gui.PropertyGroup;
import cz.cuni.mff.xrg.uv.transformer.tabular.gui.PropertyGroupAdv;
import cz.cuni.mff.xrg.uv.transformer.tabular.parser.ParserType;
import eu.unifiedviews.dpu.config.DPUConfigException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TabularVaadinDialog extends AdvancedVaadinDialogBase<TabularConfig_V1> {

    private static final Logger LOG = LoggerFactory.getLogger(
            TabularVaadinDialog.class);

    private OptionGroup optionTableType;

    private TextField txtBaseUri;

    private TextField txtKeyColumnName;

    private TextField txtEncoding;

    private TextField txtRowsLimit;

    private TextField txtRowsClass;

    private CheckBox checkHasHeader;

    private CheckBox checkGenerateNew;

    private CheckBox checkIgnoreBlankCell;

    private CheckBox checkStaticRowCounter;

    private CheckBox checkAdvancedKeyColumn;

    private CheckBox checkGenerateLabels;

    private CheckBox checkGenerateRowTriple;

    private TextField txtCsvQuoteChar;

    private TextField txtCsvDelimeterChar;

    private TextField txtCsvLinesToIgnore;

    /**
     * Layout for basic column mapping.
     */
    private GridLayout basicLayout;

    /**
     * Layout for advanced column mapping.
     */
    private GridLayout advancedLayout;

    private final List<PropertyGroup> basisMapping = new ArrayList<>();

    private final List<PropertyGroupAdv> advancedMapping = new ArrayList<>();

    private Panel mainPanel;

    private VerticalLayout mainLayout;

    /**
     * If true then the composite root has already been set.
     */
    private boolean layoutSet = false;

	public TabularVaadinDialog() {
		super(TabularConfig_V1.class, AddonInitializer.noAddons());
        try {
            buildMappingImportExportTab();
            buildMainLayout();
        } catch (Exception ex) {
            LOG.error("TabularVaadinDialog", ex);
            throw ex;
        }
	}
	
	private void buildMainLayout() {

        // ------------------------ General ------------------------

        final VerticalLayout generalLayout = new VerticalLayout();
		generalLayout.setImmediate(true);
		generalLayout.setWidth("100%");
		generalLayout.setHeight("-1px");

		this.optionTableType = new OptionGroup("Choose the input type:");
        this.optionTableType.setImmediate(true);
		this.optionTableType.addItem(ParserType.CSV);
		this.optionTableType.addItem(ParserType.DBF);
        this.optionTableType.setNullSelectionAllowed(false);
        this.optionTableType.setValue(ParserType.CSV);
		generalLayout.addComponent(this.optionTableType);

        this.txtBaseUri = new TextField("Resource URI base");
        this.txtBaseUri.setWidth("100%");
        this.txtBaseUri.setRequired(true);
        this.txtBaseUri.setRequiredError("Resource URI base must be supplied.");
        this.txtBaseUri.setDescription("This value is used as base URI for automatic column property generation "
                + "and also to create absolute URI if relative uri is provided in 'Property URI' column.");
        generalLayout.addComponent(this.txtBaseUri);

        this.txtKeyColumnName = new TextField("Key column");
        this.txtKeyColumnName.setNullRepresentation("");
        this.txtKeyColumnName.setNullSettingAllowed(true);
        this.txtKeyColumnName.setWidth("100%");
        generalLayout.addComponent(this.txtKeyColumnName);

        this.txtEncoding = new TextField("Encoding");
//        this.txtEncoding.setInputPrompt("UTF-8, Cp1250, ...");
        this.txtEncoding.setRequired(true);
        generalLayout.addComponent(this.txtEncoding);

        this.txtRowsLimit = new TextField("Rows limit");
        this.txtRowsLimit.setNullRepresentation("");
        this.txtRowsLimit.setNullSettingAllowed(true);
        generalLayout.addComponent(this.txtRowsLimit);

        this.txtRowsClass = new TextField("Class for a row object");
        this.txtRowsClass.setWidth("100%");
        this.txtRowsClass.setNullRepresentation("");
        this.txtRowsClass.setNullSettingAllowed(true);
        generalLayout.addComponent(this.txtRowsClass);

        // area with check boxes

        GridLayout checkLayout = new GridLayout(3,1);
        checkLayout.setWidth("100%");
        checkLayout.setHeight("-1px");
        checkLayout.setSpacing(true);
        generalLayout.addComponent(checkLayout);

        this.checkHasHeader = new CheckBox("Has header");
        this.checkHasHeader.setDescription("Uncheck if there is no header in given file. "
                        + "The columns are then accessible under names col0, col1, ..");
        checkLayout.addComponent(this.checkHasHeader);

        this.checkGenerateNew = new CheckBox("Full column mapping");
        this.checkGenerateNew.setDescription("If true then default mapping is generated for every column.");
        checkLayout.addComponent(this.checkGenerateNew);

        this.checkIgnoreBlankCell = new CheckBox("Ignore blank cells");
        this.checkIgnoreBlankCell.setDescription("If unchecked and and cell is blank then URI for blank cell is inserted else cell is ignored.");
        checkLayout.addComponent(this.checkIgnoreBlankCell);

        this.checkStaticRowCounter = new CheckBox("Use static row counter");
        this.checkStaticRowCounter.setDescription("If checked and multiple files are precessed, then those files share the same row counter."
                + "The process can be viewsed as if files are appended before parsing.");
        checkLayout.addComponent(checkStaticRowCounter);

        this.checkAdvancedKeyColumn = new CheckBox("Advanced key column");
        this.checkAdvancedKeyColumn.setDescription("If checked then 'Key column' is interpreted as tempalate. Experimental functionality! If checked the output value of tempalte is used a subject without any additional changes.");
        checkLayout.addComponent(this.checkAdvancedKeyColumn);

        this.checkGenerateLabels = new CheckBox("Generate labels");
        this.checkGenerateLabels.setDescription("If checked then rdfs:labels are generated to column URIs, as the value original column name is used. If file does not contain header then data from first row are used. Does not generate labels for advanced mapping.");
        checkLayout.addComponent(this.checkGenerateLabels);

        this.checkGenerateRowTriple = new CheckBox("Generate row column");
        this.checkGenerateRowTriple.setDescription("If checked then column with row number is generated for each row.");
        checkLayout.addComponent(this.checkGenerateRowTriple);

        // -------------------------- CSV ----------------------------

        final FormLayout csvLayout = new FormLayout();
		csvLayout.setImmediate(true);
        csvLayout.setSpacing(true);
		csvLayout.setWidth("100%");
		csvLayout.setHeight("-1px");
        csvLayout.addComponent(new Label("CSV specific settings"));

        this.txtCsvQuoteChar = new TextField("Quote char");
//        this.txtCsvQuoteChar.setInputPrompt("\"");
        this.txtCsvQuoteChar.setNullRepresentation("");
        this.txtCsvQuoteChar.setRequired(true);
        csvLayout.addComponent(this.txtCsvQuoteChar);

        this.txtCsvDelimeterChar = new TextField("Delimiter char");
//        this.txtCsvDelimeterChar.setInputPrompt(",");
        this.txtCsvDelimeterChar.setNullRepresentation("");
        this.txtCsvDelimeterChar.setRequired(true);
        csvLayout.addComponent(this.txtCsvDelimeterChar);

        this.txtCsvLinesToIgnore = new TextField("Skip n first lines");
        this.txtCsvLinesToIgnore.setNullRepresentation("");
        this.txtCsvQuoteChar.setRequired(true);
        csvLayout.addComponent(this.txtCsvLinesToIgnore);

        // add change listener
        this.optionTableType.addValueChangeListener(new Property.ValueChangeListener() {

            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                final ParserType value = (ParserType)event.getProperty().getValue();
                boolean csvEnabled = ParserType.CSV == value;
                txtCsvQuoteChar.setEnabled(csvEnabled);
                txtCsvDelimeterChar.setEnabled(csvEnabled);
                txtCsvLinesToIgnore.setEnabled(csvEnabled);
            }
        });

        // --------------------- Mapping - simple ---------------------

        this.basicLayout = new GridLayout(5,1);
		this.basicLayout.setWidth("100%");
		this.basicLayout.setHeight("-1px");
        this.basicLayout.setImmediate(true);
        this.basicLayout.setSpacing(true);
        this.basicLayout.setMargin(true);

        //  add headers
        this.basicLayout.addComponent(new Label("Column name"));
        this.basicLayout.setColumnExpandRatio(0, 0.3f);

        this.basicLayout.addComponent(new Label("Output type"));
        this.basicLayout.setColumnExpandRatio(1, 0.0f);

        this.basicLayout.addComponent(new Label("Language"));
        this.basicLayout.setColumnExpandRatio(2, 0.0f);

        this.basicLayout.addComponent(new Label("Use Dbf types"));
        this.basicLayout.setColumnExpandRatio(3, 0.0f);

        this.basicLayout.addComponent(new Label("Property URI"));
        this.basicLayout.setColumnExpandRatio(4, 0.7f);

        addSimplePropertyMapping(null, null);

        // --------------------- Mapping - template based --------------

        this.advancedLayout = new GridLayout(2, 1);
		this.advancedLayout.setWidth("100%");
		this.advancedLayout.setHeight("-1px");
        this.advancedLayout.setImmediate(true);
        this.advancedLayout.setSpacing(true);
        this.advancedLayout.setMargin(true);

        this.advancedLayout.addComponent(new Label("Property URI"));
        this.advancedLayout.setColumnExpandRatio(0, 0.3f);

        this.advancedLayout.addComponent(new Label("Template"));
        this.advancedLayout.setColumnExpandRatio(1, 0.7f);

        // -------------------------------------------------------------

        final TabSheet propertiesTab = new TabSheet();
        propertiesTab.setSizeFull();

        propertiesTab.addTab(this.basicLayout, "Simple");
        Tab tab = propertiesTab.addTab(this.advancedLayout, "Advanced - experimental functionality!");
        tab.setDescription("Templates based on http://w3c.github.io/csvw/csv2rdf/#. If { or } is part of column name"
                + "then before use they must be escaped ie. \\{ or \\} should be used."
                + "Use \"...\" to denote literal and <...>  to denote uri. '...' then represent the content of literal/uri.");

        // -------------------------------------------------------------

        // top layout with configuration
        final HorizontalLayout configLayout = new HorizontalLayout();
		configLayout.setWidth("100%");
		configLayout.setHeight("-1px");
        configLayout.setSpacing(true);

        configLayout.addComponent(generalLayout);
        configLayout.addComponent(csvLayout);


        // main layout for whole dialog
        mainLayout = new VerticalLayout();
		mainLayout.setImmediate(false);
        mainLayout.setSpacing(true);
		mainLayout.setWidth("100%");
		mainLayout.setHeight("-1px");
        mainLayout.setMargin(true);
        mainLayout.addComponent(configLayout);
        mainLayout.setExpandRatio(configLayout, 0.0f);

        mainLayout.addComponent(new Label("Mapping"));

        mainLayout.addComponent(propertiesTab);
        mainLayout.setExpandRatio(propertiesTab, 1.0f);

        final Button btnAddMapping = new Button("Add mapping");
        btnAddMapping.addClickListener(new Button.ClickListener() {

            @Override
            public void buttonClick(Button.ClickEvent event) {
                // add empty line with mapping
                if (propertiesTab.getSelectedTab() == basicLayout) {
                    addSimplePropertyMapping(null, null);
                } else if (propertiesTab.getSelectedTab() == advancedLayout) {
                    addAdvancedPropertyMapping(null, null);
                } else {
                    LOG.error("No tabs selected!");
                }
            }
        });
        mainLayout.addComponent(btnAddMapping);
        mainLayout.setExpandRatio(configLayout, 0.0f);

		mainPanel = new Panel();
        mainPanel.setContent(mainLayout);
        mainPanel.setSizeFull();

        setCompositionRoot(mainPanel);
        // composite root can be updated in
        // setConfiguration method in reaction to dialog type (instance, template)

        // then we
	}

    private void buildMappingImportExportTab() {
        final VerticalLayout generalLayout = new VerticalLayout();
        generalLayout.setMargin(true);
		generalLayout.setSizeFull();

        final Label label = new Label("Hower over buttons to get additional info",
                ContentMode.HTML);
        generalLayout.addComponent(label);
        generalLayout.setExpandRatio(label, 0.0f);

        final Button btnImportColNames = new Button("Import column names");
        btnImportColNames.setDescription("You can insert \t separated "
                + "names of columns. After pressing import given text is "
                + "paresed and used as column names in Mapping. "
                + "Old data ARE LOST!");
        generalLayout.addComponent(btnImportColNames);
        generalLayout.setExpandRatio(btnImportColNames, 0.0f);

        final TextField txtSeparator = new TextField("Separator");
        txtSeparator.setValue("\\t");
        generalLayout.addComponent(txtSeparator);
        generalLayout.setExpandRatio(txtSeparator, 0.0f);

        final TextArea txtColNames = new TextArea("");
        txtColNames.setSizeFull();
        generalLayout.addComponent(txtColNames);
        generalLayout.setExpandRatio(txtColNames, 1.0f);

        btnImportColNames.addClickListener(new Button.ClickListener() {

            @Override
            public void buttonClick(Button.ClickEvent event) {
                parseStringForColumnNames(txtColNames.getValue(),
                        txtSeparator.getValue().trim());

                Notification.show("Import", "Column name import done.",
                        Notification.Type.HUMANIZED_MESSAGE);
            }
        });

        this.addTab(generalLayout, "Mapping Import/Export");
    }

    /**
     * Add new line (component) into tab "Simple" mapping.
     *
     * @param name
     * @param setting
     */
    private void addSimplePropertyMapping(String name, ColumnInfo_V1 setting) {
        final PropertyGroup newGroup = new PropertyGroup(basicLayout);
        basisMapping.add(newGroup);

        if (name != null && setting != null) {
            newGroup.set(name, setting);
        }
    }

    /**
     * Add new line (component) into tab "Advanced" mapping.
     *
     * @param uri
     * @param template
     */
    private void addAdvancedPropertyMapping(String uri, String template) {
        final PropertyGroupAdv newGroup = new PropertyGroupAdv(advancedLayout);
        advancedMapping.add(newGroup);

        if (uri != null && template != null) {
            newGroup.set(uri, template);
        }
    }

    /**
     * Parse given string and use it to set column (properties) names. Original
     * data are lost.
     *
     * @param str
     */
    private void parseStringForColumnNames(String str, String separator) {
        if (separator == null || separator.isEmpty()) {
            separator = "\\t";
        }
        final String[] columnNames = str.split(separator);
        int index = 0;
        for (; index < columnNames.length; ++index) {
            if (index >= basisMapping.size()) {
                addSimplePropertyMapping(columnNames[index], 
                        new ColumnInfo_V1());
            } else {
                // use existing
                basisMapping.get(index).set(columnNames[index],
                        new ColumnInfo_V1());
            }
        }
        // clear old
        for (;index < basisMapping.size(); ++index) {
            basisMapping.get(index).clear();
        }
    }

	@Override
	protected void setConfiguration(TabularConfig_V1 c) throws DPUConfigException {
        //
        // update dialog, as the isTempalte is decided at the begining
        // this should occure only once per dialog creation
        //
        if (!layoutSet) {
            if (getContext().isTemplate()) {
                setCompositionRoot(mainLayout);
            } else {
                setCompositionRoot(mainPanel);
            }
        }
        //
        txtKeyColumnName.setValue(c.getKeyColumnName());
        //
        // save uri
        //
        String uriStr = c.getBaseURI();
        if (!uriStr.endsWith("/")) {
            uriStr = uriStr + "/";
        }
        try {
            new java.net.URI(uriStr);
        } catch (URISyntaxException ex) {
            throw new DPUConfigException("Base URI has invalid format", ex);

        }
        txtBaseUri.setValue(uriStr);
        //
        // column info basic
        //
        int index = 0;
        for (String key : c.getColumnsInfo().keySet()) {
            final ColumnInfo_V1 info = c.getColumnsInfo().get(key);
            if (index >= basisMapping.size()) {
                addSimplePropertyMapping(key, info);
            } else {
                // use existing
                basisMapping.get(index).set(key, info);
            }
            index++;
        }
        // clear old
        for (;index < basisMapping.size(); ++index) {
            basisMapping.get(index).clear();
        }
        //
        // column info advanced
        //
        index = 0;
        if (c.getColumnsInfoAdv() != null) {
            for (String key : c.getColumnsInfoAdv().keySet()) {
                final String template = c.getColumnsInfoAdv().get(key);
                if (index >= advancedMapping.size()) {
                    addAdvancedPropertyMapping(key, template);
                } else {
                    // use existing
                    advancedMapping.get(index).set(key, template);
                }
                index++;
            }
            // clear old
            for (;index < advancedMapping.size(); ++index) {
                advancedMapping.get(index).clear();
            }
        } else {
            LOG.debug("c.getColumnsInfoAdv() is null!");
        }
        //
        // csv data
        //
        if (c.getTableType() == ParserType.CSV) {
            txtCsvQuoteChar.setValue(c.getQuoteChar());
            txtCsvDelimeterChar.setValue(c.getDelimiterChar());
            txtCsvLinesToIgnore.setValue(c.getLinesToIgnore().toString());
        } else {
            txtCsvQuoteChar.setValue("\"");
            txtCsvDelimeterChar.setValue(",");
            txtCsvLinesToIgnore.setValue("0");
        }
        //
        // other data
        //
        txtEncoding.setValue(c.getEncoding());
        if (c.getRowsLimit() == null) {
            txtRowsLimit.setValue(null);
        } else {
            txtRowsLimit.setValue(c.getRowsLimit().toString());
        }
        optionTableType.setValue(c.getTableType());
        checkHasHeader.setValue(c.isHasHeader());
        checkGenerateNew.setValue(c.isGenerateNew());
        txtRowsClass.setValue(c.getRowsClass());
        checkIgnoreBlankCell.setValue(c.isIgnoreBlankCells());
        checkStaticRowCounter.setValue(c.isStaticRowCounter());
        checkAdvancedKeyColumn.setValue(c.isAdvancedKeyColumn());
        checkGenerateLabels.setValue(c.isGenerateLabels());
        checkGenerateRowTriple.setValue(c.isGenerateRowTriple());
    }

	@Override
	protected TabularConfig_V1 getConfiguration() throws DPUConfigException {
		TabularConfig_V1 cnf = new TabularConfig_V1();

        // check global validity
        if (!txtBaseUri.isValid() || !txtEncoding.isValid()) {
            throw new DPUConfigException("Configuration contains invalid inputs.");
        }
        
        cnf.setKeyColumnName(txtKeyColumnName.getValue());
        cnf.setBaseURI(txtBaseUri.getValue());
        //
        // column info basic
        //
        for (PropertyGroup item : basisMapping) {
            final String name = item.getColumnName();
            if (name != null) {
                cnf.getColumnsInfo().put(name, item.get());
            }
        }
        //
        // column info advanced
        //
        for (PropertyGroupAdv item : advancedMapping) {
            final String uri = item.getUri();
            final String template = item.getTemplate();
            if (uri != null && template != null) {
                cnf.getColumnsInfoAdv().put(uri, template);
            }
        }
        //
        // csv data
        //
        final ParserType value = (ParserType)optionTableType.getValue();
        if (value == ParserType.CSV) {

            if (!txtCsvQuoteChar.isValid() || !txtCsvDelimeterChar.isValid() ||
                    !txtCsvLinesToIgnore.isValid()) {
                throw new DPUConfigException(
                        "CSV configuration contains invalid inputs.");
            }

            cnf.setQuoteChar(txtCsvQuoteChar.getValue());
            cnf.setDelimiterChar(txtCsvDelimeterChar.getValue());
            try {
                final String linesToSkipStr = txtCsvLinesToIgnore.getValue();
                if (linesToSkipStr == null) {
                    cnf.setLinesToIgnore(0);
                } else {
                    cnf.setLinesToIgnore(
                        Integer.parseInt(linesToSkipStr));
                }
            } catch (NumberFormatException ex) {
                throw new DPUConfigException("Wrong format of lines to skip.",
                        ex);
            }
        }
        //
        // other data
        //
        cnf.setEncoding(txtEncoding.getValue());

        final String rowsLimitStr = txtRowsLimit.getValue();
        if (rowsLimitStr == null || rowsLimitStr.isEmpty()) {
            cnf.setRowsLimit(null);
        } else {
            try {
                cnf.setRowsLimit(Integer.parseInt(rowsLimitStr));
            } catch (NumberFormatException ex) {
                throw new DPUConfigException("Wrong format of row limit.", ex);
            }
        }

        cnf.setTableType((ParserType)optionTableType.getValue());
        cnf.setHasHeader(checkHasHeader.getValue());
        cnf.setGenerateNew(checkGenerateNew.getValue());
        cnf.setIgnoreBlankCells(checkIgnoreBlankCell.getValue());
        cnf.setStaticRowCounter(checkStaticRowCounter.getValue());
        cnf.setAdvancedKeyColumn(checkAdvancedKeyColumn.getValue());
        cnf.setGenerateLabels(checkGenerateLabels.getValue());
        cnf.setGenerateRowTriple(checkGenerateRowTriple.getValue());

        final String rowsClass = txtRowsClass.getValue();
        if (rowsClass == null || rowsClass.isEmpty()) {
            cnf.setRowsClass(null);
        } else {
            // try parse URI
            try {
                new java.net.URI(rowsClass);
            } catch(URISyntaxException ex) {
                throw new DPUConfigException("Wrong uri for row class.", ex);
            }
            cnf.setRowsClass(txtRowsClass.getValue());
        }
        //
        // additional checks
        //
        if (!cnf.isGenerateNew() && cnf.getColumnsInfo().isEmpty()) {
            throw new DPUConfigException("Specify at least one column mapping or check 'Full column mapping' option.");
        }

		return cnf;
	}

	@Override
	public String getDescription() {
		StringBuilder desc = new StringBuilder();
	
		return desc.toString();
	}


}

package cz.cuni.mff.xrg.uv.addressmapper;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.ui.*;
import cz.cuni.mff.xrg.uv.addressmapper.mapping.*;
import cz.cuni.mff.xrg.uv.boost.dpu.addon.AddonInitializer;
import cz.cuni.mff.xrg.uv.boost.dpu.gui.AdvancedVaadinDialogBase;
import eu.unifiedviews.dpu.config.DPUConfigException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddressMapperVaadinDialog extends AdvancedVaadinDialogBase<AddressMapperConfig_V1> {

    private static final String COLUMN_NAME_MAPPER = "Mapper";

    private static final String COLUMN_NAME_URI = "Uri";

    private TextArea txtQuery;

    private TextField txtRuianUri;

    private TextField txtRuianFailRetry;

    private TextField txtRuianFailDelay;

    private TextField txtBaseUri;

    private Table tableMappers;

    public AddressMapperVaadinDialog() {
        super(AddressMapperConfig_V1.class, AddonInitializer.noAddons());
        buildLayout();
    }

    private void buildLayout() {
        setWidth("100%");
        setHeight("100%");

        final VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setImmediate(false);
        mainLayout.setSpacing(true);
        mainLayout.setWidth("100%");
        mainLayout.setHeight("-1px");

        txtQuery = new TextArea();
        txtQuery.setWidth("100%");
        txtQuery.setHeight("100%");
        txtQuery.setCaption("Query for PostalAddress (must select ?s):");
        txtQuery.setRequired(true);
        txtQuery.setDescription("Query for subjects \"?s\" of type http://schema.org/PostalAddress.");
        mainLayout.addComponent(txtQuery);
        mainLayout.setExpandRatio(txtQuery, 0.2f);

        txtRuianUri = new TextField();
        txtRuianUri.setWidth("100%");
        txtRuianUri.setHeight("-1px");
        txtRuianUri.setCaption("Ruian URI:");
        txtRuianUri.setRequired(true);
        mainLayout.addComponent(txtRuianUri);
        mainLayout.setExpandRatio(txtRuianUri, 0);

        txtRuianFailRetry = new TextField();
        txtRuianFailRetry.setWidth("100%");
        txtRuianFailRetry.setHeight("-1px");
        txtRuianFailRetry.setCaption("Ruian fail retry (-1 for infinity):");
        txtRuianFailRetry.setRequired(true);
        mainLayout.addComponent(txtRuianFailRetry);
        mainLayout.setExpandRatio(txtRuianFailRetry, 0);

        txtRuianFailDelay = new TextField();
        txtRuianFailDelay.setWidth("100%");
        txtRuianFailDelay.setHeight("-1px");
        txtRuianFailDelay.setCaption("Ruian fail delay:");
        txtRuianFailDelay.setRequired(true);
        mainLayout.addComponent(txtRuianFailDelay);
        mainLayout.setExpandRatio(txtRuianFailDelay, 0);

        txtBaseUri = new TextField();
        txtBaseUri.setWidth("100%");
        txtBaseUri.setHeight("-1px");
        txtBaseUri.setCaption("Mapping base URI (shlould end with '/'):");
        txtBaseUri.setRequired(true);
        mainLayout.addComponent(txtBaseUri);
        mainLayout.setExpandRatio(txtBaseUri, 0);

        Label lblTable = new Label("Column 'Uri' can contains multiple URIs separated by white space.");
        mainLayout.addComponent(lblTable);
        mainLayout.setExpandRatio(lblTable, 0);

        tableMappers = new Table();
        tableMappers.setImmediate(true);
        tableMappers.setWidth("100%");
        tableMappers.setHeight("150px");
        tableMappers.setCaption("Mappers configuration:");
        // Add columns.
        tableMappers.addGeneratedColumn(COLUMN_NAME_MAPPER,
                new Table.ColumnGenerator() {
                    @Override
                    public Object generateCell(Table source, Object itemId, Object columnId) {
                        return itemId;
                    }
                });
        tableMappers.addContainerProperty(COLUMN_NAME_URI, String.class, null);
        tableMappers.setTableFieldFactory(new TableFieldFactory() {
            @Override
            public Field createField(Container container, Object itemId, Object propertyId,
                    Component uiContext) {
                if (propertyId == COLUMN_NAME_URI) {
                    // We use full comuln space.
                    final TextField txtEdit = new TextField();
                    txtEdit.setWidth("100%");
                    return txtEdit;
                } else {
                    return null;
                }
            }
        });

        tableMappers.setColumnWidth(COLUMN_NAME_MAPPER, -1);
        tableMappers.setColumnExpandRatio(COLUMN_NAME_MAPPER, 0.1f);

        tableMappers.setColumnWidth(COLUMN_NAME_MAPPER, -1);
        tableMappers.setColumnExpandRatio(COLUMN_NAME_URI, 1.0f);
        tableMappers.setColumnAlignment(COLUMN_NAME_URI, Table.Align.LEFT);

        mainLayout.addComponent(tableMappers);
        mainLayout.setExpandRatio(tableMappers, 0.3f);

        Panel mainPanel = new Panel();
        mainPanel.setWidth("100%");
        mainPanel.setHeight("100%");
        mainPanel.setContent(mainLayout);

        setCompositionRoot(mainPanel);
    }

    @Override
    protected void setConfiguration(AddressMapperConfig_V1 c) throws DPUConfigException {
        txtQuery.setValue(c.getAddressQuery());
        txtRuianUri.setValue(c.getRuainEndpoint());
        txtRuianFailRetry.setValue(c.getRuianFailRetry().toString());
        txtRuianFailDelay.setValue(c.getRuianFailDelay().toString());
        txtBaseUri.setValue(c.getBaseUri());
        // Configure table.
        tableMappers.setEditable(false);
        tableMappers.removeAllItems();

        Map<String, List<String>> mapper = c.getMapperConfig();
        if (mapper == null) {
            // Use default.
            mapper = new HashMap<>();
            mapper.put(AddressRegionMapper.NAME, Arrays.asList("http://schema.org/addressRegion"));
            mapper.put(PostalCodeMapper.NAME, Arrays.asList("http://schema.org/postalCode"));
            mapper.put(StreetAddressMapper.NAME, Arrays.asList("http://schema.org/streetAddress"));
            mapper.put(AddressLocalityMapper.NAME, Arrays.asList("http://schema.org/addressLocality"));
        }

        for (String name : MapperFactory.getNames()) {
            final List<String> uris = mapper.get(name);
            final StringBuilder str = new StringBuilder();
            if (uris != null) {
                for (String uri : uris) {
                    str.append(uri);
                    str.append(" ");
                }
            }
            tableMappers.addItem(new Object[]{str.toString()}, name);
        }
        tableMappers.setEditable(true);
    }

    @Override
    protected AddressMapperConfig_V1 getConfiguration() throws DPUConfigException {
        final AddressMapperConfig_V1 cnf = new AddressMapperConfig_V1();
        cnf.setAddressQuery(txtQuery.getValue());
        cnf.setRuainEndpoint(txtRuianUri.getValue());

        try {
            cnf.setRuianFailRetry(Integer.parseInt(txtRuianFailRetry.getValue()));
            cnf.setRuianFailDelay(Integer.parseInt(txtRuianFailDelay.getValue()));
        } catch (NumberFormatException e) {
            throw new DPUConfigException("Ruian fail retry/delay msut be numbers.", e);
        }

        final List<String> mapperNames = MapperFactory.getNames();
        final Map<String, List<String>> mappers = new HashMap<>();
        cnf.setMapperConfig(mappers);
        for (Object id : tableMappers.getItemIds()) {
            final String name = (String) id;
            if (mapperNames.contains(name)) {
                // Get column
                final Item item = tableMappers.getItem(id);
                String value = item.getItemProperty(COLUMN_NAME_URI).getValue().toString();
                if (value.isEmpty()) {
                    // No value here, so skip.
                } else {
                    value = value.replaceAll("\\s+", " ");
                    mappers.put(name, Arrays.asList(value.split(" ")));
                }
            }
        }

        String value = txtBaseUri.getValue();
        if (!value.endsWith("/")) {
            value = value + "/";
        }
        cnf.setBaseUri(value);

        return cnf;
    }

    @Override
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("RUIAN endpoint: ");
        desc.append(txtRuianUri.getValue());
        return desc.toString();
    }

}

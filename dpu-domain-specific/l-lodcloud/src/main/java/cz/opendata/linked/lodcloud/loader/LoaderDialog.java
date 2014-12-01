package cz.opendata.linked.lodcloud.loader;

import java.util.Collection;
import java.util.LinkedList;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import cz.cuni.mff.xrg.uv.boost.dpu.addon.AddonInitializer;
import cz.cuni.mff.xrg.uv.boost.dpu.addon.impl.SimpleRdfConfigurator;
import cz.cuni.mff.xrg.uv.boost.dpu.gui.AdvancedVaadinDialogBase;
import cz.cuni.mff.xrg.uv.utils.dialog.container.ComponentTable;
import cz.opendata.linked.lodcloud.loader.LoaderConfig.LicenseMetadataTags;
import cz.opendata.linked.lodcloud.loader.LoaderConfig.Licenses;
import cz.opendata.linked.lodcloud.loader.LoaderConfig.ProvenanceMetadataTags;
import cz.opendata.linked.lodcloud.loader.LoaderConfig.PublishedTags;
import cz.opendata.linked.lodcloud.loader.LoaderConfig.Topics;
import cz.opendata.linked.lodcloud.loader.LoaderConfig.VocabMappingsTags;
import cz.opendata.linked.lodcloud.loader.LoaderConfig.VocabTags;
import eu.unifiedviews.dpu.config.DPUConfigException;

/**
 * DPU's configuration dialog. User can use this dialog to configure DPU configuration.
 *
 */
public class LoaderDialog extends AdvancedVaadinDialogBase<LoaderConfig> {

	private static final long serialVersionUID = -1989608763609859477L;
	
	private ComponentTable<LoaderConfig.LinkCount> genTable;
	private VerticalLayout mainLayout;
    private Label lblRestApiUrl;
    private TextField tfDatasetID;
    private TextField tfApiKey;
    private TextField tfMaintainerName;
    private TextField tfMaintainerEmail;
    private TextField tfAuthorName;
    private TextField tfAuthorEmail;
    private TextField tfVersion;
    private TextField tfSPARQLName;
    private TextField tfSPARQLDescription;
    private TextField tfNamespace;
    private TextField tfShortName;
    private TextField tfCustomLicenseLink;
    private CheckBox chkGenerateVersion;
    private ComboBox cbTopic;
    private ComboBox cbLicense;
    private ListSelect lsLicenseMetadataTag;
    private ListSelect lsProvenanceMetadataTag;
    private ListSelect lsPublishedTag;
    private ListSelect lsVocabMappingsTag;
    private ListSelect lsVocabTag;
    private ListSelect lsVocabularies;
    
    public LoaderDialog() {
        super(LoaderConfig.class,AddonInitializer.create(new SimpleRdfConfigurator<>(Loader.class)));
        buildMainLayout();
        Panel panel = new Panel();
        panel.setSizeFull();
        panel.setContent(mainLayout);
        setCompositionRoot(panel);
    }  
    
    @SuppressWarnings("serial")
	private VerticalLayout buildMainLayout() {
        // common part: create layout
        mainLayout = new VerticalLayout();
        mainLayout.setImmediate(false);
        mainLayout.setSizeUndefined();
        mainLayout.setWidth("100%");
        //mainLayout.setHeight("-1px");
        mainLayout.setMargin(false);
        //mainLayout.setSpacing(true);

        // top-level component properties
        setWidth("100%");
        setHeight("100%");
        
        tfApiKey = new TextField();
        tfApiKey.setWidth("100%");
        tfApiKey.setCaption("Datahub.io CKAN API Key");
        tfApiKey.setDescription("Datahub.io CKAN API Key");
        tfApiKey.setInputPrompt("00000000-0000-0000-0000-000000000000");
        mainLayout.addComponent(tfApiKey);
        
        tfDatasetID = new TextField();
        tfDatasetID.setImmediate(true);
        tfDatasetID.setWidth("100%");
        tfDatasetID.setTextChangeEventMode(TextChangeEventMode.EAGER);
        tfDatasetID.setCaption("Dataset ID");
        tfDatasetID.setDescription("CKAN Dataset Name used in CKAN Dataset URL");
        tfDatasetID.setInputPrompt("cz-test");
        tfDatasetID.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				String url = "http://datahub.io/api/rest/dataset/" + tfDatasetID.getValue();
				lblRestApiUrl.setValue("<a href=\"" + url + "\" target=\"_blank\">" + url + "</a>");
		}});
        mainLayout.addComponent(tfDatasetID);
        
        lblRestApiUrl = new Label();
        lblRestApiUrl.setContentMode(ContentMode.HTML);
        mainLayout.addComponent(lblRestApiUrl);
        
        tfShortName = new TextField();
        tfShortName.setWidth("100%");
        tfShortName.setCaption("Dataset short name");
        tfShortName.setInputPrompt("CZ IC");
        mainLayout.addComponent(tfShortName);

        cbTopic = new ComboBox();
        cbTopic.setWidth("100%");
        cbTopic.setCaption("Topic");
        cbTopic.setDescription("Topic is used for coloring of the LOD cloud");
        for (LoaderConfig.Topics topic : LoaderConfig.Topics.values())
        {
        	cbTopic.addItem(topic);
        }
        cbTopic.setInvalidAllowed(false);
        cbTopic.setNullSelectionAllowed(false);
        cbTopic.setTextInputAllowed(false);
        mainLayout.addComponent(cbTopic);

        tfMaintainerName = new TextField();
        tfMaintainerName.setWidth("100%");
        tfMaintainerName.setCaption("Maintainer name");
        tfMaintainerName.setInputPrompt("Jakub Klímek");
        mainLayout.addComponent(tfMaintainerName);
        
        tfMaintainerEmail = new TextField();
        tfMaintainerEmail.setWidth("100%");
        tfMaintainerEmail.setCaption("Maintainer email");
        tfMaintainerEmail.setInputPrompt("klimek@opendata.cz");
        mainLayout.addComponent(tfMaintainerEmail);
        
        tfAuthorName = new TextField();
        tfAuthorName.setWidth("100%");
        tfAuthorName.setCaption("Author name");
        tfAuthorName.setInputPrompt("Jakub Klímek");
        mainLayout.addComponent(tfAuthorName);
        
        tfAuthorEmail = new TextField();
        tfAuthorEmail.setWidth("100%");
        tfAuthorEmail.setCaption("Author email");
        tfAuthorEmail.setInputPrompt("klimek@opendata.cz");
        mainLayout.addComponent(tfAuthorEmail);
        
        tfVersion = new TextField();
        tfVersion.setWidth("100%");
        tfVersion.setCaption("Version");
        tfVersion.setInputPrompt("2014-03-01");
        mainLayout.addComponent(tfVersion);
        
        chkGenerateVersion = new CheckBox();
        chkGenerateVersion.setCaption("Generate Version as current date");
        chkGenerateVersion.setImmediate(true);
        chkGenerateVersion.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				tfVersion.setEnabled(!chkGenerateVersion.getValue());
		}});
        mainLayout.addComponent(chkGenerateVersion);

        cbLicense = new ComboBox();
        cbLicense.setWidth("100%");
        cbLicense.setCaption("License");
        cbLicense.setDescription("License displayed in CKAN");
        for (LoaderConfig.Licenses license : LoaderConfig.Licenses.values())
        {
        	cbLicense.addItem(license);
        }
        cbLicense.setImmediate(true);
        cbLicense.setInvalidAllowed(false);
        cbLicense.setTextInputAllowed(false);
        cbLicense.setNullSelectionAllowed(false);
        cbLicense.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				LoaderConfig.Licenses l = (Licenses) cbLicense.getValue();
				boolean enabled = false;
				enabled = enabled || l == LoaderConfig.Licenses.otherat;
				enabled = enabled || l == LoaderConfig.Licenses.otherclosed;
				enabled = enabled || l == LoaderConfig.Licenses.othernc;
				enabled = enabled || l == LoaderConfig.Licenses.otheropen;
				enabled = enabled || l == LoaderConfig.Licenses.otherpd;
				tfCustomLicenseLink.setEnabled(enabled);
		}});
        
        mainLayout.addComponent(cbLicense);
        
        tfCustomLicenseLink = new TextField();
        tfCustomLicenseLink.setWidth("100%");
        tfCustomLicenseLink.setCaption("Custom license link");
        tfCustomLicenseLink.setDescription("Only valid when no standard license applies");
        tfCustomLicenseLink.setInputPrompt("http://link.to.license");
        mainLayout.addComponent(tfCustomLicenseLink);

        tfSPARQLName = new TextField();
        tfSPARQLName.setWidth("100%");
        tfSPARQLName.setCaption("SPARQL Endpoint name");
        tfSPARQLName.setInputPrompt("Opendata.cz SPARQL Endpoint");
        mainLayout.addComponent(tfSPARQLName);

        tfSPARQLDescription = new TextField();
        tfSPARQLDescription.setWidth("100%");
        tfSPARQLDescription.setCaption("SPARQL Endpoint description");
        tfSPARQLDescription.setInputPrompt("Running Virtuoso 7");
        mainLayout.addComponent(tfSPARQLDescription);

        tfNamespace = new TextField();
        tfNamespace.setWidth("100%");
        tfNamespace.setCaption("RDF namespace");
        tfNamespace.setInputPrompt("http://linked.opendata.cz/resource/");
        mainLayout.addComponent(tfNamespace);

        lsLicenseMetadataTag = new ListSelect();
        lsLicenseMetadataTag.setWidth("100%");
        lsLicenseMetadataTag.setCaption("License metadata");
        lsLicenseMetadataTag.setDescription("Switches between license-metadata and no-license-metadata tags");
        for (LoaderConfig.LicenseMetadataTags lmdTag : LoaderConfig.LicenseMetadataTags.values())
        {
        	lsLicenseMetadataTag.addItem(lmdTag);
        }
        lsLicenseMetadataTag.setNewItemsAllowed(false);
        lsLicenseMetadataTag.setMultiSelect(false);
        lsLicenseMetadataTag.setNullSelectionAllowed(false);
        lsLicenseMetadataTag.setRows(LoaderConfig.LicenseMetadataTags.values().length);
        mainLayout.addComponent(lsLicenseMetadataTag);

        lsProvenanceMetadataTag = new ListSelect();
        lsProvenanceMetadataTag.setWidth("100%");
        lsProvenanceMetadataTag.setCaption("Provenance metadata");
        lsProvenanceMetadataTag.setDescription("Switches between provenance-metadata and no-provenance-metadata tags");
        for (LoaderConfig.ProvenanceMetadataTags pmdTag : LoaderConfig.ProvenanceMetadataTags.values())
        {
        	lsProvenanceMetadataTag.addItem(pmdTag);
        }
        lsProvenanceMetadataTag.setNewItemsAllowed(false);
        lsProvenanceMetadataTag.setMultiSelect(false);
        lsProvenanceMetadataTag.setNullSelectionAllowed(false);
        lsProvenanceMetadataTag.setRows(LoaderConfig.ProvenanceMetadataTags.values().length);
        mainLayout.addComponent(lsProvenanceMetadataTag);

        lsPublishedTag = new ListSelect();
        lsPublishedTag.setWidth("100%");
        lsPublishedTag.setCaption("Publised by");
        lsPublishedTag.setDescription("Switches between published-by-producer and published-by-third-party tags");
        for (LoaderConfig.PublishedTags pTag : LoaderConfig.PublishedTags.values())
        {
        	lsPublishedTag.addItem(pTag);
        }
        lsPublishedTag.setNewItemsAllowed(false);
        lsPublishedTag.setMultiSelect(false);
        lsPublishedTag.setNullSelectionAllowed(false);
        lsPublishedTag.setRows(LoaderConfig.PublishedTags.values().length);
        mainLayout.addComponent(lsPublishedTag);

        lsVocabTag = new ListSelect();
        lsVocabTag.setWidth("100%");
        lsVocabTag.setCaption("Proprietary vocabulary");
        lsVocabTag.setDescription("Switches among no-proprietary-vocab deref-vocab and no-deref-vocab tags");
        for (LoaderConfig.VocabTags vTag : LoaderConfig.VocabTags.values())
        {
        	lsVocabTag.addItem(vTag);
        }
        lsVocabTag.setNewItemsAllowed(false);
        lsVocabTag.setMultiSelect(false);
        lsVocabTag.setNullSelectionAllowed(false);
        lsVocabTag.setRows(LoaderConfig.VocabTags.values().length);
        mainLayout.addComponent(lsVocabTag);

        lsVocabMappingsTag = new ListSelect();
        lsVocabMappingsTag.setWidth("100%");
        lsVocabMappingsTag.setCaption("Vocabulary mapping");
        lsVocabMappingsTag.setDescription("Only valid when using proprietary vocabulary. Switches between vocab-mappings and no-vocab-mappings tags");
        for (LoaderConfig.VocabMappingsTags vmTag : LoaderConfig.VocabMappingsTags.values())
        {
        	lsVocabMappingsTag.addItem(vmTag);
        }
        lsVocabMappingsTag.setNewItemsAllowed(false);
        lsVocabMappingsTag.setMultiSelect(false);
        lsVocabMappingsTag.setNullSelectionAllowed(false);
        lsVocabMappingsTag.setRows(LoaderConfig.VocabMappingsTags.values().length);
        mainLayout.addComponent(lsVocabMappingsTag);

        lsVocabularies = new ListSelect();
        lsVocabularies.setRows(4);
        lsVocabularies.setWidth("100%");
        lsVocabularies.setCaption("Standard prefixes of vocabularies used");
        lsVocabularies.setDescription("Tags the dataset with used vocabulary prefixes. Lookup: http://prefix.cc");
        lsVocabularies.setNewItemsAllowed(true);
        lsVocabularies.setNullSelectionAllowed(false);
        lsVocabularies.setMultiSelect(true);
        mainLayout.addComponent(lsVocabularies);

        genTable = new ComponentTable<LoaderConfig.LinkCount>(LoaderConfig.LinkCount.class,
                new ComponentTable.ColumnInfo("targetDataset", "Target CKAN dataset name", null, 0.4f),
                new ComponentTable.ColumnInfo("linkCount", "Link count", null, 0.1f));
        
        genTable.setPolicy(new ComponentTable.Policy<LoaderConfig.LinkCount>() {

            @Override
            public boolean isSet(LoaderConfig.LinkCount value) {
                return !value.getTargetDataset().isEmpty();
            }

        });
        mainLayout.addComponent(genTable);
        
        return mainLayout;
    }    
     
    @Override
    public void setConfiguration(LoaderConfig conf) throws DPUConfigException {
    	tfApiKey.setValue(conf.getApiKey());
    	tfDatasetID.setValue(conf.getDatasetID());
    	tfMaintainerName.setValue(conf.getMaintainerName());
    	tfMaintainerEmail.setValue(conf.getMaintainerEmail());
    	tfAuthorName.setValue(conf.getAuthorName());
    	tfAuthorEmail.setValue(conf.getAuthorEmail());
    	tfVersion.setValue(conf.getVersion());
    	tfSPARQLName.setValue(conf.getSparqlEndpointName());
    	tfSPARQLDescription.setValue(conf.getSparqlEndpointDescription());
    	tfNamespace.setValue(conf.getNamespace());
    	tfShortName.setValue(conf.getShortname());
    	tfCustomLicenseLink.setValue(conf.getCustomLicenseLink());
    	chkGenerateVersion.setValue(conf.isVersionGenerated());
    	genTable.setValue(conf.getLinks());
    	cbTopic.setValue(conf.getTopic());
    	cbLicense.setValue(conf.getLicense_id());
    	lsLicenseMetadataTag.setValue(conf.getLicenseMetadataTag());
    	lsProvenanceMetadataTag.setValue(conf.getProvenanceMetadataTag());
    	lsPublishedTag.setValue(conf.getPublishedTag());
    	lsVocabMappingsTag.setValue(conf.getVocabMappingTag());
    	lsVocabTag.setValue(conf.getVocabTag());
    	
    	for (String s: conf.getVocabularies()) lsVocabularies.addItem(s);
    	lsVocabularies.setValue(conf.getVocabularies());
    }

    @Override
    public LoaderConfig getConfiguration() throws DPUConfigException {
    	LoaderConfig conf = new LoaderConfig();
        conf.setApiKey(tfApiKey.getValue());
        conf.setDatasetID(tfDatasetID.getValue());
        conf.setMaintainerName(tfMaintainerName.getValue());
        conf.setMaintainerEmail(tfMaintainerEmail.getValue());
        conf.setAuthorName(tfAuthorName.getValue());
        conf.setAuthorEmail(tfAuthorEmail.getValue());
        conf.setVersion(tfVersion.getValue());
        conf.setSparqlEndpointName(tfSPARQLName.getValue());
        conf.setSparqlEndpointDescription(tfSPARQLDescription.getValue());
        conf.setNamespace(tfNamespace.getValue());
        conf.setShortname(tfShortName.getValue());
        conf.setCustomLicenseLink(tfCustomLicenseLink.getValue());
        conf.setVersionGenerated(chkGenerateVersion.getValue());
        conf.setLinks(genTable.getValue());
        conf.setTopic((Topics) cbTopic.getValue());
        conf.setLicense_id((Licenses) cbLicense.getValue());
        conf.setLicenseMetadataTag((LicenseMetadataTags) lsLicenseMetadataTag.getValue());
        conf.setProvenanceMetadataTag((ProvenanceMetadataTags) lsProvenanceMetadataTag.getValue());
        conf.setPublishedTag((PublishedTags) lsPublishedTag.getValue());
        conf.setVocabMappingTag((VocabMappingsTags) lsVocabMappingsTag.getValue());
        conf.setVocabTag((VocabTags) lsVocabTag.getValue());
        @SuppressWarnings("unchecked")
        Collection<String> value = ((Collection<String>) lsVocabularies.getValue());
		conf.setVocabularies(new LinkedList<String>(value));
        return conf;
    }

}

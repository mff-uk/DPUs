package cz.cuni.mff.xrg.uv.transformer.sparql.construct;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import cz.cuni.mff.xrg.uv.boost.dpu.addon.AddonInitializer;
import cz.cuni.mff.xrg.uv.boost.dpu.addon.impl.FaultToleranceWrap;
import cz.cuni.mff.xrg.uv.boost.dpu.gui.AdvancedVaadinDialogBase;
import eu.unifiedviews.dpu.config.DPUConfigException;

/**
 * DPU's configuration dialog.
 *
 * @author Škoda Petr
 */
public class SparqlConstructVaadinDialog extends AdvancedVaadinDialogBase<SparqlConstructConfig_V1> {

    private TextArea txtQuery;

    private CheckBox checkPerGraph;

    private CheckBox checkUseDataset;

    public SparqlConstructVaadinDialog() {
        super(SparqlConstructConfig_V1.class, AddonInitializer.create(new FaultToleranceWrap()));
        buildLayout();
    }

    @Override
    public void setConfiguration(SparqlConstructConfig_V1 c) throws DPUConfigException {
        txtQuery.setValue(c.getQuery());
        checkPerGraph.setValue(c.isPerGraph());
        checkUseDataset.setValue(c.isUseDataset());
    }

    @Override
    public SparqlConstructConfig_V1 getConfiguration() throws DPUConfigException {
        final SparqlConstructConfig_V1 c = new SparqlConstructConfig_V1();
        if (txtQuery.getValue().isEmpty()) {
            throw new DPUConfigException("Query must not be empty.");
        }
        c.setQuery(txtQuery.getValue());
        c.setPerGraph(checkPerGraph.getValue());
        c.setUseDataset(checkUseDataset.getValue());
        return c;
    }

    private void buildLayout() {
        final VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setSizeFull();
        mainLayout.setSpacing(true);
        mainLayout.setMargin(true);

        checkUseDataset = new CheckBox("Use dataset class (check for Sesame, uncheck for Virtuoso)");
        checkUseDataset.setWidth("100%");
        mainLayout.addComponent(checkUseDataset);
        mainLayout.setExpandRatio(checkUseDataset, 0.0f);

        checkPerGraph = new CheckBox("Per-graph execution");
        checkPerGraph.setWidth("100%");
        mainLayout.addComponent(checkPerGraph);
        mainLayout.setExpandRatio(checkPerGraph, 0.0f);

		txtQuery = new TextArea("SPARQL construct query");
        txtQuery.setSizeFull();
        txtQuery.setRequired(true);
        mainLayout.addComponent(txtQuery);
        mainLayout.setExpandRatio(txtQuery, 1.0f);

        setCompositionRoot(mainLayout);
    }
    
}

package cz.cuni.mff.xrg.uv.transformer.check.rdfnotemtpy;

import cz.cuni.mff.xrg.uv.boost.dpu.addon.AddonInitializer;
import cz.cuni.mff.xrg.uv.boost.dpu.addon.impl.CloseCloseable;
import cz.cuni.mff.xrg.uv.boost.dpu.advanced.DpuAdvancedBase;
import cz.cuni.mff.xrg.uv.boost.dpu.config.MasterConfigObject;
import cz.cuni.mff.xrg.uv.boost.dpu.utils.SendMessage;
import eu.unifiedviews.dataunit.DataUnit;
import eu.unifiedviews.dataunit.DataUnitException;
import eu.unifiedviews.dataunit.rdf.RDFDataUnit;
import eu.unifiedviews.dataunit.rdf.WritableRDFDataUnit;
import eu.unifiedviews.dpu.DPU;
import eu.unifiedviews.dpu.DPUException;
import eu.unifiedviews.helpers.dpu.config.AbstractConfigDialog;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.cuni.mff.xrg.uv.utils.dataunit.DataUnitUtils;

@DPU.AsQuality
public class RdfNotEmpty extends DpuAdvancedBase<RdfNotEmptyConfig_V1> {

	private static final Logger LOG = LoggerFactory.getLogger(RdfNotEmpty.class);

    @DataUnit.AsInput(name = "rdf")
    public RDFDataUnit rdfInData;

    @DataUnit.AsOutput(name = "rdf")
    public WritableRDFDataUnit rdfOutData;

	public RdfNotEmpty() {
		super(RdfNotEmptyConfig_V1.class, 
                AddonInitializer.create(new CloseCloseable()));
	}
		
    @Override
    protected void innerExecute() throws DPUException {
        boolean isEmpty = true;

        RDFDataUnit.Iteration iter;
        try {
            iter = rdfInData.getIteration();
        } catch (DataUnitException ex) {
            SendMessage.sendMessage(context, ex);
            return;
        }
        getAddon(CloseCloseable.class).add(iter);

        RepositoryConnection connection = null;
        try {
            connection = rdfInData.getConnection();

            while (iter.hasNext()) {
                RDFDataUnit.Entry entry = iter.next();
                // chgeck size
                long size = connection.size(entry.getDataGraphURI());
                if (size > 0) {
                    isEmpty = false;
                }
                // log in debug mode
                LOG.debug("size( {} ) = {}", entry.getDataGraphURI(), size);
            }
            if (!isEmpty) {
                // Copy data.
                DataUnitUtils.copyGraphs(rdfInData, rdfOutData, connection);
            }
        } catch (DataUnitException ex) {
            SendMessage.sendMessage(context, ex);
            return;
        } catch (RepositoryException ex) {
            SendMessage.sendMessage(context, ex);
            return;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (RepositoryException ex) {
                    LOG.warn("Can't close conneciton.", ex);
                }
            }
        }

        if (isEmpty) {
           String msg = config.getMessage();
           if (msg == null || msg.isEmpty()) {
               msg = RdfNotEmptyConfig_V1.AUTO_MESSAGE;
           }
           context.sendMessage(config.getMessageType(),msg);
        }
    }

    @Override
    public AbstractConfigDialog<MasterConfigObject> getConfigurationDialog() {
        return new RdfNotEmptyVaadinDialog();
    }
	
}

package cz.opendata.linked.cz.cenia.irz;

import java.io.IOException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.cuni.mff.xrg.uv.boost.dpu.advanced.DpuAdvancedBase;
import cz.cuni.mff.xrg.uv.boost.dpu.addon.AddonInitializer;
import cz.cuni.mff.xrg.uv.boost.dpu.addon.impl.SimpleRdfConfigurator;
import eu.unifiedviews.dataunit.DataUnit;
import eu.unifiedviews.dataunit.DataUnitException;
import cz.cuni.mff.xrg.uv.boost.dpu.config.MasterConfigObject;
import eu.unifiedviews.dpu.DPU;
import eu.unifiedviews.dpu.DPUContext;
import eu.unifiedviews.dpu.DPUException;
import eu.unifiedviews.helpers.dpu.config.AbstractConfigDialog;
import eu.unifiedviews.dataunit.rdf.WritableRDFDataUnit;
import cz.cuni.mff.xrg.uv.rdf.utils.dataunit.rdf.simple.AddPolicy;
import cz.cuni.mff.xrg.uv.rdf.utils.dataunit.rdf.simple.SimpleRdfWrite;
import cz.cuni.mff.xrg.scraper.css_parser.utils.Cache;
import cz.cuni.mff.xrg.uv.rdf.utils.dataunit.rdf.simple.SimpleRdfFactory;

@DPU.AsExtractor
public class Extractor 
extends DpuAdvancedBase<ExtractorConfig> 
{

	@SimpleRdfConfigurator.Configure(dataUnitFieldName="output")
	public SimpleRdfWrite outputWrap;
	
	@DataUnit.AsOutput(name = "output")
    public WritableRDFDataUnit output;

    private Logger LOG = LoggerFactory.getLogger(DPU.class);

    public Extractor(){
        super(ExtractorConfig.class,AddonInitializer.create(new SimpleRdfConfigurator(Extractor.class)));
    }

    @Override
    public AbstractConfigDialog<MasterConfigObject> getConfigurationDialog() {        
        return new ExtractorDialog();
    }

    @Override
    protected void innerExecute() throws DPUException, DataUnitException
    {
        Cache.setInterval(config.getInterval());
        Cache.setTimeout(config.getTimeout());
        Cache.setBaseDir(context.getUserDirectory() + "/cache/");
        Cache.rewriteCache = config.isRewriteCache();
        Cache.logger = LOG;

        try {
            Cache.setTrustAllCerts();
        } catch (Exception e) {
            LOG.error("Unexpected error when setting trust to all certificates.",e );
        }
        
        Parser s = new Parser();
        s.logger = LOG;
        s.context = context;
        s.outputDataUnit = outputWrap;
        s.valueFactory = outputWrap.getValueFactory();

        LOG.info("Starting extraction.");
        
        try {
            try {
                java.util.Date date = new java.util.Date();
                long start = date.getTime();
                
                for (int i = config.getStartYear(); i <= config.getEndYear(); i++)
                {
                    if (context.canceled()) break;
                    s.parse(new URL("http://portal.cenia.cz/irz/unikyPrenosy.jsp?rok=" + i + "&unikyPrenosyVyhledatVse=1"), "list");
                }
                
                context.sendMessage(DPUContext.MessageType.INFO, "Bad PSCs: " + s.badPostals);
                context.sendMessage(DPUContext.MessageType.INFO, "Switched GPS coordinats: " + s.switchedGPS);
                java.util.Date date2 = new java.util.Date();
                long end = date2.getTime();
                
                context.sendMessage(DPUContext.MessageType.INFO, "Processed in " + (end-start) + "ms");
            }
            catch (IOException e) {
                LOG.error("IOException", e);
            }
            
            outputWrap.flushBuffer();
            
            LOG.info("Parsing done.");
        } catch (InterruptedException intex) {
            LOG.error("Interrupted");
        }

    }

}

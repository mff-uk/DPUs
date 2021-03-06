package cz.opendata.linked.cz.gov.agendy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.cuni.mff.xrg.scraper.css_parser.utils.Cache;
import eu.unifiedviews.dataunit.DataUnit;
import eu.unifiedviews.dataunit.files.WritableFilesDataUnit;
import eu.unifiedviews.dpu.DPU;
import eu.unifiedviews.dpu.DPUException;
import eu.unifiedviews.helpers.dpu.config.ConfigHistory;
import eu.unifiedviews.helpers.dpu.context.ContextUtils;
import eu.unifiedviews.helpers.dpu.exec.AbstractDpu;
import eu.unifiedviews.helpers.dpu.extension.ExtensionInitializer;
import eu.unifiedviews.helpers.dpu.extension.faulttolerance.FaultTolerance;
import eu.unifiedviews.helpers.dpu.extension.files.simple.WritableSimpleFiles;

@DPU.AsExtractor
public class Extractor 
extends AbstractDpu<ExtractorConfig> 
{

    private static final Logger LOG = LoggerFactory.getLogger(Extractor.class);

    @DataUnit.AsOutput(name = "output")
    public WritableFilesDataUnit outputDataUnit;

    @ExtensionInitializer.Init
    public FaultTolerance faultTolerance;
    
    @ExtensionInitializer.Init(param = "outputDataUnit")
    public WritableSimpleFiles outputDataUnitSimple;

    public Extractor(){
        super(ExtractorDialog.class,ConfigHistory.noHistory(ExtractorConfig.class));
    }

    @Override
    protected void innerExecute() throws DPUException
    {
        // vytvorime si parser
        Cache.setInterval(config.getInterval());
        Cache.setTimeout(config.getTimeout());
        Cache.setBaseDir(ctx.getExecMasterContext().getDpuContext().getUserDirectory() + "/cache/");
        Cache.rewriteCache = config.isRewriteCache();
        Cache.logger = LOG;

        try {
            Cache.setTrustAllCerts();
        } catch (Exception e) {
            LOG.error("Unexpected error when setting trust to all certificates.",e );
        }
        
        String tempfilename = ctx.getExecMasterContext().getDpuContext().getWorkingDir() + "/" + config.getOutputFileName();
        Parser s = new Parser();
        s.logger = LOG;
        s.context = ctx.getExecMasterContext().getDpuContext();
        try {
            s.ps = new PrintStream(tempfilename, "UTF-8");
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            LOG.error("Failed to create PrintStream.", e);
        }

        s.ps.println(
                "@prefix dcterms:  <http://purl.org/dc/terms/> .\n" +
                        //"@prefix rdf:      <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                        //"@prefix rdfs:     <http://www.w3.org/2000/01/rdf-schema#> .\n" +
                        "@prefix xsd:      <http://www.w3.org/2001/XMLSchema#> .\n" +
                        "@prefix skos:       <http://www.w3.org/2004/02/skos/core#> .\n" +
                        "@prefix gr:       <http://purl.org/goodrelations/v1#> .\n" +
                        "@prefix adms:       <http://www.w3.org/ns/adms#> .\n" +
                        "@prefix dcterms:       <http://purl.org/dc/terms/> .\n" +
                        
                        "\n" +
                        "@prefix ovm-a:      <http://linked.opendata.cz/ontology/domain/seznam.gov.cz/agendy/> .\n" +
                        "@prefix ovm-r:      <http://linked.opendata.cz/resource/domain/seznam.gov.cz/agendy/> .\n" +
                        "@prefix ovm-t:      <http://linked.opendata.cz/resource/domain/seznam.gov.cz/typyOVM/> .\n" +
                        "@prefix ovm-c:      <http://linked.opendata.cz/resource/domain/seznam.gov.cz/cinnosti/> .\n" +
                        "@prefix ovm-co:     <http://linked.opendata.cz/ontology/domain/seznam.gov.cz/cinnosti/> .\n" +

                        ""
                );
        

        // a spustim na vychozi stranku
        LOG.info("Starting extraction. Output: " + tempfilename);
        
        try {
            try {
                java.util.Date date = new java.util.Date();
                long start = date.getTime();
                
                s.parse(new URL("https://rpp-ais.egon.gov.cz/gen/agendy-detail/"), "list");
                
                java.util.Date date2 = new java.util.Date();
                long end = date2.getTime();
                
                ContextUtils.sendShortInfo(ctx, "Processed in {0} ms", (end-start));
            }
            catch (IOException e) {
                LOG.error("IOException", e);
            }
            
            LOG.info("Parsing done. Passing RDF to ODCS");
            
            outputDataUnitSimple.add(new File(tempfilename), tempfilename);

        } catch (InterruptedException e) {
            LOG.error("Interrupted");
        }

        s.ps.close();
    }

}

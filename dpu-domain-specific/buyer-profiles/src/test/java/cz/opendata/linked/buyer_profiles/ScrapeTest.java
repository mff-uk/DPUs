package cz.opendata.linked.buyer_profiles;

import org.junit.Test;

import cz.cuni.mff.xrg.odcs.dpu.test.TestEnvironment;
import eu.unifiedviews.dataunit.rdf.RDFDataUnit;

public class ScrapeTest {
//    @Test
//    public void constructAllTest() throws Exception {
//        // prepare dpu instance and configure it
//        Extractor extractor = new Extractor();
//        ExtractorConfig config = new ExtractorConfig();
//        
//        config.setRewriteCache(false);
//        config.setInterval(0);
//        config.setTimeout(2000);
//        config.setMaxAttempts(1);
//        config.setValidateXSD(true);
//        config.setCurrentYearOnly(true);
//        config.setAccessProfiles(true);        
//        
//        extractor.configureDirectly(config);
//            
//        // prepare test environment, we use system tmp directory
//        TestEnvironment env = new TestEnvironment();
//        // prepare input and output data units
//        
//        // here we can simply pre-fill input data unit with content from 
//        // resource file
//        
//        RDFDataUnit contracts = env.createRdfOutput("contracts", false);
//        RDFDataUnit profiles = env.createRdfOutput("profiles", false);
//        RDFDataUnit profile_stats = env.createRdfOutput("profile_statistics", false);
//        
//        try {
//            // run the execution
//            env.run(extractor);
////TODO: add loadToFile method - possibly into test environment?
////            contracts.loadToFile("C:\\temp\\contracts.ttl", RDFFormatType.TTL);
////            profiles.loadToFile("C:\\temp\\profiles.ttl", RDFFormatType.TTL);
////            profile_stats.loadToFile("C:\\temp\\profile_stats.ttl", RDFFormatType.TTL);            
//        }
//        catch(Exception e) {
//            e.printStackTrace();
//        } finally {
//            // release resources
//            env.release();
//        }
//    }
}
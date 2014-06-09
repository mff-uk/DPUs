package cz.opendata.linked.cz.gov.organy;

import org.junit.Test;

import cz.cuni.mff.xrg.odcs.dpu.test.TestEnvironment;
import cz.cuni.mff.xrg.odcs.rdf.RDFDataUnit;

public class ScrapeTest {

	@Test
	public void constructAllTest() throws Exception {
		// prepare dpu instance and configure it
		Extractor extractor = new Extractor();
		ExtractorConfig config = new ExtractorConfig();

		config.setInterval(0);
		config.setTimeout(10000);
		config.setRewriteCache(false);

		extractor.configureDirectly(config);

		// prepare test environment, we use system tmp directory
		TestEnvironment env = new TestEnvironment();
		// prepare input and output data units

		RDFDataUnit list = env.createRdfOutput("XMLList", false);
		RDFDataUnit details = env.createRdfOutput("XMLDetails", false);

		// here we can simply pre-fill input data unit with content from 
		// resource file
		try {
			// run the execution
			env.run(extractor);

//			list.loadToFile("C:\\temp\\list.ttl", RDFFormatType.TTL);
//			details.loadToFile("C:\\temp\\details.ttl", RDFFormatType.TTL);

			// verify result
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// release resources
			env.release();
		}
	}
}

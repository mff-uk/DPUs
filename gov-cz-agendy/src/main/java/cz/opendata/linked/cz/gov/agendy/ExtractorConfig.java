package cz.opendata.linked.cz.gov.agendy;

import cz.cuni.mff.xrg.odcs.commons.module.config.DPUConfigObjectBase;

/**
 *
 * Put your DPU's configuration here.
 *
 */
public class ExtractorConfig extends DPUConfigObjectBase {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5577275030298541080L;

	public String outputFileName = "agendy.ttl";
	
	public boolean rewriteCache = false;
		
	public int timeout = 10000;

	public int interval = 0;

	@Override
    public boolean isValid() {
        return true;
    }

}

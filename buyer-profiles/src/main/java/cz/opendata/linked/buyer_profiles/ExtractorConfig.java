package cz.opendata.linked.buyer_profiles;

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
	private static final long serialVersionUID = 3509477277481754571L;

	public boolean rewriteCache = false;
	
	public boolean accessProfiles = true;
	
	public int timeout = 10000;
	
	public int interval = 0;
	
	public boolean currentYearOnly = false;
	
	@Override
    public boolean isValid() {
        return true;
    }

}

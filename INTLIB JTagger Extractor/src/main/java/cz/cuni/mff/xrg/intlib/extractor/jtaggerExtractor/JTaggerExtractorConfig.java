package cz.cuni.mff.xrg.intlib.extractor.jtaggerExtractor;

import cz.cuni.mff.xrg.odcs.commons.module.config.DPUConfigObjectBase;

/**
 *
 * Put your DPU's configuration here.
 *
 */
public class JTaggerExtractorConfig extends DPUConfigObjectBase {
    
    
    private String dateTo;
     private String dateFrom;
    
     public static final int maxExtractedDecisions = 1000;
     
  
    /**
     *
     * @param dateTo
     * @param dateFrom
     */
    public JTaggerExtractorConfig(String dateTo, String dateFrom) {
        this.dateTo = dateTo;
        this.dateFrom = dateFrom;
    }

    JTaggerExtractorConfig() {

    }

    public String getDateFrom() {
        return dateFrom;
    }

    public String getDateTO() {
        return dateTo;
    }

    @Override
    public boolean isValid() {
        return true;
    }


}

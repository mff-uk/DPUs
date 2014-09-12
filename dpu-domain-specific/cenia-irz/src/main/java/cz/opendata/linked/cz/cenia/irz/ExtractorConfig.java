package cz.opendata.linked.cz.cenia.irz;


/**
 *
 * Put your DPU's configuration here.
 *
 */
public class ExtractorConfig  {
    
    private boolean rewriteCache = false;
        
    private int timeout = 10000;

    private int interval = 0;
    
    private int startYear = 2004;
    
    private int endYear = 2012;

    public boolean isRewriteCache() {
        return rewriteCache;
    }

    public void setRewriteCache(boolean rewriteCache) {
        this.rewriteCache = rewriteCache;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public int getStartYear() {
        return startYear;
    }

    public void setStartYear(int startYear) {
        this.startYear = startYear;
    }

    public int getEndYear() {
        return endYear;
    }

    public void setEndYear(int endYear) {
        this.endYear = endYear;
    }

}

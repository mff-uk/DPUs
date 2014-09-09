package cz.opendata.linked.cz.gov.organy;


/**
 *
 * Put your DPU's configuration here.
 *
 */
public class ExtractorConfig  {
    
    private static final long serialVersionUID = 8719241993054209502L;

    private boolean rewriteCache = true;
    
    private int timeout = 40000;
    
    private int interval = 0;

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
    
}

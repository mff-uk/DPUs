package cz.cuni.mff.xrg.scraper.lib.template;

import eu.unifiedviews.dpu.DPUContext;
import cz.cuni.mff.xrg.scraper.css_parser.utils.Cache;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import org.slf4j.Logger;

import org.jsoup.nodes.Document;
import org.openrdf.model.ValueFactory;
import org.slf4j.LoggerFactory;

import eu.unifiedviews.dpu.DPUException;
import eu.unifiedviews.helpers.dpu.extension.rdf.simple.WritableSimpleRdf;

/**
 * Abstract class of common scraper.
 * 
 * The scraping process is done in following steps:
 *  - parse with doc type has to be called on initial URL
 *  - getLinks is called on initial URL with given docType
 *  - found links are added to queue for future parsing
 *  - parse method is called on initial URL with given docType (and do whathever implemented)
 * 
 * @author Jakub Starka
 */
public abstract class ScrapingTemplate {
    
    public Logger logger;
    
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ScrapingTemplate.class);
    
    public DPUContext context;
    
    public WritableSimpleRdf outputDataUnit;
    
    public ValueFactory valueFactory;
    
    /** 
     * This method looks for links in actual document and create entries with URL and document type.
     * 
     * @param doc Input JSoup document.
     * @param docType Textual name of input document (i.e. initial page, list page, detail page, ...
     * @return List of entries for additional scraping.
     */
    protected abstract LinkedList<ParseEntry> getLinks(Document doc, String docType, URL url);
    
    /**
     * This method parses given document with document type and do something
     * 
     * @param doc Input JSoup document.
     * @param docType Textual name of input document (i.e. initial page, list page, detail page, ...
     */
    protected abstract void parse(Document doc, String docType, URL url) throws DPUException;
    
    /**
     * Run scraping on given URL and given document type.
     * 
     * @param initUrl Initial URL.
     * @param type Initial document type.
     * @throws InterruptedException 
     */
    public void parse(URL initUrl, String type) throws InterruptedException, DPUException{
        LinkedList<ParseEntry> toParse = new LinkedList<>();
        HashSet<ParseEntry> parsed = new HashSet<>();
        toParse.add(new ParseEntry(initUrl, type));
        
        while (!toParse.isEmpty() && !context.canceled()) {
            try {
                ParseEntry p = toParse.pop();
                // skip if parsed
                if (parsed.contains(p)) {
                    continue;
                }
                Document doc = Cache.getDocument(p.url, 10);
                if (doc != null) {
	                toParse.addAll(this.getLinks(doc, p.type, p.url));
	                this.parse(doc, p.type, p.url);
                }
                else {
                	logger.warn("Document null: " + p.url);
                }
                parsed.add(p);
            } catch (IOException ex) {
                LOG.error("IOException in parse", ex);
            } 
        }
        
    }
    
}

package cz.opendata.linked.cz.ruian;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;

import org.apache.commons.pool.impl.GenericKeyedObjectPool.Config;

import cz.cuni.mff.xrg.odcs.rdf.interfaces.RDFDataUnit;
import cz.cuni.mff.xrg.scraper.lib.template.ParseEntry;
import cz.cuni.mff.xrg.scraper.lib.template.ScrapingTemplate;
import cz.cuni.mff.xrg.odcs.commons.ontology.OdcsTerms;

/**
 * Scraper pro RUIAN
 * 
 * @author Jakub Klímek
 */

public class Scraper_parser extends ScrapingTemplate{
    
	public RDFDataUnit obce, zsj;
	private int numDetails = 0;
	private int current;
	public boolean outputFiles;
	
	@Override
    protected LinkedList<ParseEntry> getLinks(String doc, String docType) {
        final LinkedList<ParseEntry> out = new LinkedList<>();
        
        if (docType.equals("init") || docType.equals("initStat"))
        {
        	String[] lines = doc.split("\\r\\n");
        	numDetails += lines.length;
        	logger.info("I see " + numDetails + " files");
        	for (String line : lines)
        	{
        		try {
					if (docType.equals("init")) out.add(new ParseEntry(new URL(line),"obec","gz"));
					else if (docType.equals("initStat")) out.add(new ParseEntry(new URL(line),"zsj","gz"));
				} catch (MalformedURLException e) {
					logger.warn(e.getLocalizedMessage());
				}
        	}
        }
        return out;
    }
    
    @Override
    protected void parse(String doc, String docType, URL url) {
    	if (docType.equals("obec"))
    	{
    		logger.debug("Processing detail " + ++current + "/" + numDetails + ": " + url.toString()); 
    		if (outputFiles) obce.addTriple(obce.createURI("http://linked.opendata.cz/ontology/odcs/DataUnit"), obce.createURI(OdcsTerms.DATA_UNIT_XML_VALUE_PREDICATE),obce.createLiteral(doc));
    	}
    	else if (docType.equals("zsj"))
    	{
    		logger.debug("Processing detail " + ++current + "/" + numDetails + ": " + url.toString());
    		if (outputFiles) zsj.addTriple(zsj.createURI("http://linked.opendata.cz/ontology/odcs/DataUnit"), zsj.createURI(OdcsTerms.DATA_UNIT_XML_VALUE_PREDICATE),zsj.createLiteral(doc));
    	}
    }
}
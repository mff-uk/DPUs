package cz.cuni.mff.xrg.uv.postaladdress.to.ruain.mapping;

import cz.cuni.mff.xrg.uv.postaladdress.to.ruain.query.Requirement;
import cz.cuni.mff.xrg.uv.rdf.simple.OperationFailedException;
import cz.cuni.mff.xrg.uv.rdf.simple.SimpleRdfWrite;
import java.util.LinkedList;
import java.util.List;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;

/**
 * Gather informations about problems that arise during mapping.
 *
 * @author Škoda Petr
 */
public class ErrorLogger {

    private String uri = "";
    
    private final List<String> unused = new LinkedList<>();
    
    private final List<String> noOutput = new LinkedList<>();
    
    private final List<String> parseFaield = new LinkedList<>();
    
    private int failCounter = 0;
    
    private final ValueFactory valueFactory;
    
    private final URI predType;
    
    private final URI predParseFailed;
    
    private final URI objectType;
    
    public ErrorLogger(ValueFactory valueFactory) {
        this.valueFactory = valueFactory;
        // TODO Move into ontology too?
        predType = valueFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        predParseFailed = valueFactory.createURI("http://ruian.uv.xrg.mff.cuni.cz/ontology/parseFailed");
        objectType = valueFactory.createURI("http://schema.org/PostalAddress");
    }
    
    public void start(String uri) { 
        this.uri = uri;
        this.unused.clear();
        this.noOutput.clear();
        this.parseFaield.clear();
        this.failCounter = 0;
    }

    public void failedToMap(String predicate, String object, String message) {
        failCounter++;
        parseFaield.add(predicate + " " + object + "\n\t\t" + message);
    }

    public void failedToMap(String predicate, String object, String message,
            Exception ex) {
        failCounter++;
        parseFaield.add(predicate + " " + object + "\n\t\t" + message + "\n\t\t" + 
                ex.getMessage());
    }    
    
    public void mapped(String predicate, String object,
            List<Requirement> generated) {
        if (generated.isEmpty()) {
            noOutput.add(predicate + " " + object);
        }
    }

    public void unused(String predicate, String object) {
        unused.add(predicate + " " + object);
    }
    
    public void end() {

    }

    public String getReportAsString() {
        final StringBuilder info = new StringBuilder();
    
        info.append("\nReport for: ");
        info.append(uri);
        
        info.append("\nNo output:");
        for (String item : noOutput) {
            info.append("\n\t");
            info.append(item);
        }        
        
        info.append("\nUnunsed:");
        for (String item : unused) {
            info.append("\n\t");
            info.append(item);
        }

        info.append("\nParseFailed:");
        for (String item : parseFaield) {
            info.append("\n\t");
            info.append(item);
        }       
        
        return info.toString();
    }
    
    public void report(SimpleRdfWrite logRdf) throws OperationFailedException {
        URI address = valueFactory.createURI(uri);
        
        logRdf.add(address, predType, objectType);        
        for (String item : parseFaield) {
            Value value = valueFactory.createLiteral(item);
            logRdf.add(address, predParseFailed, value);
        }
        if (!parseFaield.isEmpty()) {
            throw new RuntimeException();
        }
    }
    
    public boolean hasFailed() {
        return failCounter > 0;
    }

    public String getUri() {
        return uri;
    }

    public List<String> getUnused() {
        return unused;
    }

    public List<String> getNoOutput() {
        return noOutput;
    }

    public List<String> getParseFaield() {
        return parseFaield;
    }
    
}

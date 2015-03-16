package cz.cuni.mff.xrg.uv.transformer.sparql.linker;

/**
 *
 * @author Škoda Petr
 */
public class SparqlLinkerConfig_V1 {

    /**
     * SPARQL construct query.
     */
    private String query = "CONSTRUCT {?s ?p ?o} WHERE {?s ?p ?o}";

    private boolean perGraph = true;

    public SparqlLinkerConfig_V1() {

    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public boolean isPerGraph() {
        return perGraph;
    }

    public void setPerGraph(boolean perGraph) {
        this.perGraph = perGraph;
    }

}
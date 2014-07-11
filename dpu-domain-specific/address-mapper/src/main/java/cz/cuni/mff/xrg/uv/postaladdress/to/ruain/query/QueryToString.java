package cz.cuni.mff.xrg.uv.postaladdress.to.ruain.query;

import cz.cuni.mff.xrg.uv.postaladdress.to.ruain.ontology.Subject;

/**
 * Convert {@link Query} to {@link String}.
 *
 * @author Škoda Petr
 */
public class QueryToString {

    private final static String SELECT_PREAMBLE = "PREFIX r: <http://ruian.linked.opendata.cz/ontology/>\n"
            + "PREFIX s: <http://schema.org/>\n"
            + "SELECT ";

    private final static String SELECT_WHERE = " WHERE {\n";

    private final static String SELECT_END = "} LIMIT ";

    private QueryToString() {
    }

    public static String convert(Query query, int resultLimit) {
        if (query.getContent().isEmpty()) {
            return null;
        }
        
        StringBuilder result = new StringBuilder(SELECT_PREAMBLE);
        // put name of the result in the query
        result.append(query.getMainSubject().getText());
        result.append(SELECT_WHERE);

        for (Subject key : query.getContent().keySet()) {
            result.append(key.getText());
            // add type information
            result.append(" rdf:type ");
            result.append(key.getType());

            for (PredicatObject predObj : query.getContent().get(key)) {
                result.append(";\n");
                result.append("    ");
                result.append(predObj.predicate);
                result.append(" ");
                result.append(predObj.object);
            }
            result.append(".\n");
        }
        // return string
        result.append(SELECT_END);
        result.append(resultLimit);
        return result.toString();
    }

}

package cz.cuni.mff.xrg.uv.transformer.tabular.mapper;

import cz.cuni.mff.xrg.uv.rdf.utils.dataunit.rdf.simple.OperationFailedException;
import cz.cuni.mff.xrg.uv.rdf.utils.dataunit.rdf.simple.SimpleRdfWrite;
import cz.cuni.mff.xrg.uv.transformer.tabular.TabularOntology;
import cz.cuni.mff.xrg.uv.transformer.tabular.column.ValueGenerator;
import java.util.List;
import java.util.Map;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parse table data into rdf. Before usage this class must be configured by
 * {@link TableToRdfConfigurator}.
 *
 * @author Škoda Petr
 */
public class TableToRdf {

    private static final Logger LOG = LoggerFactory.getLogger(TableToRdf.class);

    /**
     * Data output.
     */
    final SimpleRdfWrite outRdf;

    final ValueFactory valueFactory;

    final TableToRdfConfig config;

    ValueGenerator[] infoMap = null;

    ValueGenerator keyColumn = null;

    String baseUri = null;

    Map<String, Integer> nameToIndex = null;

    URI rowClass = null;

    private final URI typeUri;

    public TableToRdf(TableToRdfConfig config, SimpleRdfWrite outRdf,
            ValueFactory valueFactory) {
        this.config = config;
        this.outRdf = outRdf;
        this.valueFactory = valueFactory;
        this.typeUri = valueFactory.createURI(
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
    }

    public void paserRow(List<Object> row, int rowNumber) throws OperationFailedException {
        if (row.size() < nameToIndex.size()) {
            LOG.warn("Row is smaller ({} instead of {}) - ignore.",
                    row.size(), nameToIndex.size());
            return;
        }

        //
        // get subject - key
        //
        final URI subj = prepareUri(row, rowNumber);
        if (subj == null) {
            LOG.error("Row ({}) has null key, row skipped.", rowNumber);
        }
        //
        // parse the line, based on configuration
        //
        for (ValueGenerator item : infoMap) {
            final URI predicate = item.getUri();
            final Value value = item.generateValue(row, valueFactory);
            if (value == null) {
                if (config.ignoreBlankCells) {
                    // ignore
                } else {
                    // insert blank cell URI
                    outRdf.add(subj, predicate, TabularOntology.URI_BLANK_CELL);
                }
            } else {
                // insert value
                outRdf.add(subj, predicate, value);
            }
        }
        // add row data - number, class
        outRdf.add(subj, TabularOntology.URI_ROW_NUMBER,
                valueFactory.createLiteral(rowNumber));
        if (rowClass != null) {
            outRdf.add(subj, typeUri, rowClass);
        }
    }

    /**
     * Return key for given row.
     *
     * @param row
     * @param rowNumber
     * @return
     */
    protected URI prepareUri(List<Object> row, int rowNumber) {
        if (keyColumn == null) {
            return valueFactory.createURI(baseUri + Integer.toString(rowNumber));
        } else {
            return (URI)keyColumn.generateValue(row, valueFactory);
        }
    }

}

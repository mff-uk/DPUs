package cz.cuni.mff.xrg.uv.transformer.sparql.linker;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.openrdf.model.URI;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.Update;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import eu.unifiedviews.dpu.DPU;
import eu.unifiedviews.dpu.DPUException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.unifiedviews.dataunit.DataUnit;
import eu.unifiedviews.dataunit.DataUnitException;
import eu.unifiedviews.dataunit.rdf.RDFDataUnit;
import eu.unifiedviews.dataunit.rdf.WritableRDFDataUnit;
import eu.unifiedviews.dpu.DPUContext.MessageType;
import eu.unifiedviews.helpers.dataunit.DataUnitUtils;
import eu.unifiedviews.helpers.dataunit.metadata.MetadataUtilsInstance;
import eu.unifiedviews.helpers.dpu.config.ConfigHistory;
import eu.unifiedviews.helpers.dpu.config.migration.ConfigurationUpdate;
import eu.unifiedviews.helpers.dpu.context.ContextUtils;
import eu.unifiedviews.helpers.dpu.exec.AbstractDpu;
import eu.unifiedviews.helpers.dpu.extension.ExtensionInitializer;
import eu.unifiedviews.helpers.dpu.extension.faulttolerance.FaultTolerance;

/*
 *
 * @author Škoda Petr
 */
@DPU.AsTransformer
public class SparqlLinker extends AbstractDpu<SparqlLinkerConfig_V1> {

    private static final Logger LOG = LoggerFactory.getLogger(SparqlLinker.class);

    private static final int MAX_GRAPH_COUNT = 1000;

    @DataUnit.AsInput(name = "perGraph-Input")
    public RDFDataUnit rdfInput;

    @DataUnit.AsInput(name = "reference-Input")
    public RDFDataUnit rdfReference;

    @DataUnit.AsOutput(name = "output")
    public WritableRDFDataUnit rdfOutput;

    @ExtensionInitializer.Init
    public FaultTolerance faultTolerance;
    
    @ExtensionInitializer.Init(param = "eu.unifiedviews.plugins.transformer.sparql.SPARQLConfig__V1")
    public ConfigurationUpdate _ConfigurationUpdate;

    public SparqlLinker() {
        super(SparqlLinkerVaadinDialog.class,
                ConfigHistory.noHistory(SparqlLinkerConfig_V1.class));
    }

    @Override
    protected void innerExecute() throws DPUException {
        if (useDataset()) {
            ContextUtils.sendShortInfo(ctx, "OpenRdf mode.");
        } else {
            ContextUtils.sendShortInfo(ctx, "Virtuoso mode.");
        }
        // Update query ie. substitute constract with insert.
        String query = config.getQuery();
        if (query == null || query.isEmpty()) {
            throw new DPUException("Query string is null or empty");
        }
        // Modify query - we always do inserts.
        query = query.replaceFirst("(?i)CONSTRUCT", "INSERT");
        // Get graphs.
        final List<RDFDataUnit.Entry> sourceEntries = getInputEntries(rdfInput);
        final List<RDFDataUnit.Entry> referenceEntries = getInputEntries(rdfReference);
        // Execute.
        executeUpdateQuery(query, sourceEntries, referenceEntries);
    }

    /**
     * Get connection and use it to execute given query. Based on user option the query is executed over one
     * or over multiple graphs.
     *
     * @param query
     * @param sourceEntries
     * @param referenceEntries
     * @throws DPUException
     */
    protected void executeUpdateQuery(final String query, final List<RDFDataUnit.Entry> sourceEntries,
            final List<RDFDataUnit.Entry> referenceEntries)
            throws DPUException {
        // Execute based on configuration.
        if (config.isPerGraph()) {
            // Execute one graph at time.
            ContextUtils.sendMessage(ctx, MessageType.INFO, "Per-graph query execution",
                    "Number of graphs: %d", sourceEntries.size());
            // Execute one query per graph, the target graph is always the same.
            int counter = 1;
            // We add current graph to source entries as the last entry.
            final ArrayList<RDFDataUnit.Entry> sources = new ArrayList<>(referenceEntries.size() + 1);
            sources.addAll(referenceEntries);
            for (final RDFDataUnit.Entry sourceEntry : sourceEntries) {
                LOG.info("Executing {}/{}", counter++, sourceEntries.size());
                // For each input graph prepare output graph.
                final URI targetGraph = faultTolerance.execute(new FaultTolerance.ActionReturn<URI>() {

                    @Override
                    public URI action() throws Exception {
                        final URI outputUri = createOutputGraph(sourceEntry);
                        LOG.info("   {} -> {}", sourceEntry.getDataGraphURI(), outputUri);
                        return outputUri;
                    }

                });
                // Set current input as a source entry.
                sources.set(sources.size() - 1, sourceEntry);
                // Execute query 1 -> 1.
                faultTolerance.execute(rdfInput, new FaultTolerance.ConnectionAction() {

                    @Override
                    public void action(RepositoryConnection connection) throws Exception {
                        executeUpdateQuery(query, sources, targetGraph, connection);
                    }

                });
            }
        } else {
            // All graph at once, just check size.
            if (sourceEntries.size() > MAX_GRAPH_COUNT) {
                throw new DPUException("Too many graphs .. (limit: " + MAX_GRAPH_COUNT + ", given: "
                        + sourceEntries.size() + ")");
            }
            ContextUtils.sendMessage(ctx, MessageType.INFO, "Executing over all graphs",
                    "Executing query over all graphs (%d)", sourceEntries.size());
            // Prepare single output graph.
            final URI targetGraph = faultTolerance.execute(new FaultTolerance.ActionReturn<URI>() {

                @Override
                public URI action() throws Exception {
                    return createOutputGraph();
                }

            });
            // Add entries from reference graph.
            sourceEntries.addAll(referenceEntries);
            // Execute query m -> 1.
            faultTolerance.execute(rdfInput, new FaultTolerance.ConnectionAction() {

                @Override
                public void action(RepositoryConnection connection) throws Exception {
                    executeUpdateQuery(query, sourceEntries, targetGraph, connection);
                }

            });
        }
    }

    /**
     * Execute given query.
     *
     * @param query
     * @param sourceEntries USING graphs.
     * @param targetGraph   WITH graphs.
     * @param connection
     * @throws eu.unifiedviews.dpu.DPUException
     * @throws eu.unifiedviews.dataunit.DataUnitException
     */
    protected void executeUpdateQuery(String query, final List<RDFDataUnit.Entry> sourceEntries,
            URI targetGraph,
            RepositoryConnection connection) throws DPUException, DataUnitException {
        // Prepare query.
        if (!useDataset()) {
            if (Pattern.compile(Pattern.quote("DELETE"), Pattern.CASE_INSENSITIVE).matcher(query).find()) {
                query = query.replaceFirst("(?i)DELETE", prepareWithClause(targetGraph) + " DELETE");
            } else {
                query = query.replaceFirst("(?i)INSERT", prepareWithClause(targetGraph) + " INSERT");
            }
            query = query.replaceFirst("(?i)WHERE", prepareUsingClause(sourceEntries) + "WHERE");
        }
        LOG.debug("Query to execute: {}", query);
        try {
            // Execute query.
            final Update update = connection.prepareUpdate(QueryLanguage.SPARQL, query);
            if (useDataset()) {
                final DatasetImpl dataset = new DatasetImpl();
                for (RDFDataUnit.Entry entry : sourceEntries) {
                    dataset.addDefaultGraph(entry.getDataGraphURI());
                }
                dataset.addDefaultRemoveGraph(targetGraph);
                dataset.setDefaultInsertGraph(targetGraph);
                update.setDataset(dataset);
            }
            update.execute();
        } catch (MalformedQueryException | UpdateExecutionException ex) {
            throw new DPUException("Problem with query", ex);
        } catch (RepositoryException ex) {
            throw new DPUException("Problem with repository.", ex);
        }
    }

    /**
     *
     * @return New output graph.
     * @throws DPUException
     */
    protected URI createOutputGraph() throws DPUException {
        // Register new output graph
        final String symbolicName = "http://unifiedviews.eu/resource/sparql-construct/"
                + Long.toString((new Date()).getTime());
        try {
            return rdfOutput.addNewDataGraph(symbolicName);
        } catch (DataUnitException ex) {
            throw new DPUException("DPU failed to add a new graph.", ex);
        }
    }

    /**
     *
     * @param symbolicName
     * @return New output graph.
     * @throws DPUException
     */
    protected URI createOutputGraph(RDFDataUnit.Entry entry) throws DPUException {
        final String suffix = "/" + ctx.getExecMasterContext().getDpuContext().getDpuInstanceId().toString();
        try {
            return rdfOutput.addNewDataGraph(entry.getSymbolicName() + suffix);
        } catch (DataUnitException ex) {
            throw new DPUException("DPU failed to add a new graph.", ex);
        }
    }

    /**
     * Register new output graph and return WITH clause for SPARQL insert.
     *
     * @param graph
     * @return
     */
    protected String prepareWithClause(URI graph) {
        final StringBuilder withClause = new StringBuilder();
        withClause.append("WITH <");
        withClause.append(graph.stringValue());
        withClause.append("> \n");
        return withClause.toString();
    }

    /**
     *
     * @param entries List of entries to use.
     * @return Using clause for SPARQL insert, based on input graphs.
     * @throws DPUException
     */
    protected String prepareUsingClause(final List<RDFDataUnit.Entry> entries) throws DPUException {
        return faultTolerance.execute(new FaultTolerance.ActionReturn<String>() {

            @Override
            public String action() throws Exception {
                final StringBuilder usingClause = new StringBuilder();
                for (RDFDataUnit.Entry entry : entries) {
                    usingClause.append("USING <");
                    try {
                        usingClause.append(entry.getDataGraphURI().stringValue());
                    } catch (DataUnitException ex) {
                        throw new DPUException("Problem with DataUnit.", ex);
                    }
                    usingClause.append("> \n");
                }
                return usingClause.toString();
            }

        });
    }

    /**
     *
     * @param dataUnit
     * @return Data graphs in given DataUnit.
     * @throws DPUException
     */
    protected List<RDFDataUnit.Entry> getInputEntries(final RDFDataUnit dataUnit) throws DPUException {
        return faultTolerance.execute(new FaultTolerance.ActionReturn<List<RDFDataUnit.Entry>>() {

            @Override
            public List<RDFDataUnit.Entry> action() throws Exception {
                return DataUnitUtils.getEntries(dataUnit, RDFDataUnit.Entry.class);
            }
        });
    }

    protected final boolean useDataset() {
        // Should be removed once bug in Sesame or Virtuoso is fixex.
        return System.getProperty(MetadataUtilsInstance.ENV_PROP_VIRTUOSO) == null;
    }

}

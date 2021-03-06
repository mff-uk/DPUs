package cz.cuni.mff.xrg.uv.transformer;

import eu.unifiedviews.dataunit.DataUnit;
import eu.unifiedviews.dataunit.files.FilesDataUnit;
import eu.unifiedviews.dataunit.rdf.WritableRDFDataUnit;
import eu.unifiedviews.dpu.DPU;
import eu.unifiedviews.dpu.DPUException;
import java.io.*;
import java.util.List;
import java.util.Stack;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.unifiedviews.dataunit.rdf.RDFDataUnit;
import eu.unifiedviews.helpers.dataunit.rdf.RdfDataUnitUtils;
import eu.unifiedviews.helpers.dpu.config.ConfigHistory;
import eu.unifiedviews.helpers.dpu.context.ContextUtils;
import eu.unifiedviews.helpers.dpu.exec.AbstractDpu;
import eu.unifiedviews.helpers.dpu.extension.ExtensionInitializer;
import eu.unifiedviews.helpers.dpu.extension.faulttolerance.FaultTolerance;
import eu.unifiedviews.helpers.dpu.extension.faulttolerance.FaultToleranceUtils;
import eu.unifiedviews.helpers.dpu.extension.rdf.simple.WritableSimpleRdf;

/**
 * Explore the tree of {@link NamedData} using DFS. Does not detect cycles!
 *
 * @author Škoda Petr
 */
@DPU.AsTransformer
public class HtmlCss extends AbstractDpu<HtmlCssConfig_V1> {

    /**
     * Store context in processing.
     */
    private static class NamedData {

        private final String name;

        /**
         * Value in elements if presented.
         */
        private final Elements elements;

        /**
         * Current subject.
         */
        private final URI subject;

        /**
         * Subject class, used only if not null.
         */
        private final URI subjectClass;

        /**
         * String value if presented.
         */
        private final String value;

        /**
         * Parent subject, we can connect to it.
         */
        private final URI parentSubject;

        /**
         * Connection predicate between subject and parentSubject.
         */
        private final URI hasPredicate;

        /**
         * Used to create a first object.
         *
         * @param name
         * @param elements
         * @param subject
         */
        public NamedData(String name, Elements elements, URI subject, URI parentSubject, URI hasPredicate) {
            this.name = name;
            this.elements = elements;
            this.subject = subject;
            this.subjectClass = null;
            this.value = null;
            this.parentSubject = parentSubject;
            this.hasPredicate = hasPredicate;
        }

        /**
         * Create a copy of given subject, just set given elements.
         *
         * @param source
         * @param action
         * @param elements
         */
        public NamedData(NamedData source, HtmlCssConfig_V1.Action action, Elements elements) {
            this.name = action.getOutputName();
            this.elements = elements;
            this.subject = source.subject;
            this.subjectClass = source.subjectClass;
            this.value = null;
            this.parentSubject = source.parentSubject;
            this.hasPredicate = source.hasPredicate;
        }

        /**
         *
         * @param source
         * @param action
         * @param subject      If null value form source is used!
         * @param subjectClass If null then parent value is used.
         * @param hasPredicate If null, then subject stay on same level.
         */
        public NamedData(NamedData source, HtmlCssConfig_V1.Action action, URI subject, URI subjectClass,
                URI hasPredicate) {
            this.name = action.getOutputName();
            this.elements = source.elements;
            this.subject = subject == null ? source.subject : subject;
            this.subjectClass = subjectClass == null ? source.subjectClass : subjectClass;
            this.value = source.value;
            if (hasPredicate == null) {
                // Same level.
                this.parentSubject = source.parentSubject;
                this.hasPredicate = source.hasPredicate;
            } else {
                // New level.
                this.parentSubject = source.subject;
                this.hasPredicate = hasPredicate;
            }
        }

        /**
         * Create a copy of given subject, just set given text.
         *
         * @param source
         * @param action
         * @param elements
         */
        public NamedData(NamedData source, HtmlCssConfig_V1.Action action, String value) {
            this.name = action.getOutputName();
            this.elements = null;
            this.subject = source.subject;
            this.subjectClass = source.subjectClass;
            this.value = value;
            this.parentSubject = source.parentSubject;
            this.hasPredicate = source.hasPredicate;
        }

    }

    public static final String WEB_PAGE_NAME = "webPage";

    public static final String SUBJECT_URI_TEMPLATE = "http://localhost/temp/";

    public static final String RDF_TYPE_PREDICATE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    private static final Logger LOG = LoggerFactory.getLogger(HtmlCss.class);

    @DataUnit.AsInput(name = "html")
    public FilesDataUnit inFilesHtml;

    @DataUnit.AsOutput(name = "rdf")
    public WritableRDFDataUnit outRdfData;

    @ExtensionInitializer.Init(param = "outRdfData")
    public WritableSimpleRdf outData;

    @ExtensionInitializer.Init
    public FaultTolerance faultTolerance;

    /**
     * Used to generate original subjects.
     */
    private int subjectIndex = 0;

    public HtmlCss() {
        super(HtmlCssVaadinDialog.class, ConfigHistory.noHistory(HtmlCssConfig_V1.class));
    }

    @Override
    protected void innerExecute() throws DPUException {
        final List<FilesDataUnit.Entry> files = FaultToleranceUtils.getEntries(faultTolerance, inFilesHtml,
                FilesDataUnit.Entry.class);
        final ValueFactory valueFactory = outData.getValueFactory();
        // Parse files.
        final URI predicateSource = valueFactory.createURI(HtmlCssOntology.PREDICATE_SOURCE);
        for (final FilesDataUnit.Entry entry : files) {
            if (ctx.canceled()) {
                throw ContextUtils.dpuExceptionCancelled(ctx);
            }
            LOG.info("Parsing file: {}", entry);
            // Set output.
            RDFDataUnit.Entry outputGraph = faultTolerance.execute(
                    new FaultTolerance.ActionReturn<RDFDataUnit.Entry>() {

                        @Override
                        public RDFDataUnit.Entry action() throws Exception {
                            return RdfDataUnitUtils.addGraph(outRdfData, entry.getSymbolicName());
                        }
                    });
            outData.setOutput(outputGraph);

            // TODO Better generation for subjects.
            final File entryFile = FaultToleranceUtils.asFile(faultTolerance, entry);
            final URI rootSubject = valueFactory.createURI(entryFile.toURI().toString());

            try {
                final Document doc = Jsoup.parse(FaultToleranceUtils.asFile(faultTolerance, entry), null);
                parse(valueFactory, doc, rootSubject);
            } catch (IOException ex) {
                throw ContextUtils.dpuException(ctx, "Can't parse object.", ex);
            }
            // Add "metadata"
            if (config.getClassAsStr() != null && !config.getClassAsStr().isEmpty()) {
                // Class for root object.
                final URI rootClass = valueFactory.createURI(config.getClassAsStr());
                outData.add(rootSubject,
                        valueFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                        rootClass);
            }
            if (config.isSourceInformation()) {
                // Symbolic name of a source file.
                faultTolerance.execute(new FaultTolerance.Action() {

                    @Override
                    public void action() throws Exception {
                        outData.add(rootSubject, predicateSource, valueFactory.createLiteral(entry
                                .getSymbolicName()));
                    }
                });
            }
        }
    }

    /**
     * Parse given document.
     *
     * @param fileUriStr
     * @param doc
     * @param rootSubject Root subject for this document.
     */
    private void parse(ValueFactory valueFactory, Document doc, URI rootSubject) throws IOException, WrongActionArgs, DPUException {
        final URI defaultHasPredicate = createUri(valueFactory, config.getHasPredicateAsStr());
        // Start and parse.
        final Stack<NamedData> states = new Stack();
        states.add(new NamedData(WEB_PAGE_NAME, doc.getAllElements(), rootSubject, null, null));

        final URI rdfType = valueFactory.createURI(RDF_TYPE_PREDICATE);
        while (!states.isEmpty() && !ctx.canceled()) {
            // Get group and apply actions.
            final NamedData state = states.pop();
            for (HtmlCssConfig_V1.Action action : config.getActions()) {
                if (action.getName().compareTo(state.name) == 0) {
                    if (action.getType() == null) {
                        throw ContextUtils.dpuException(ctx, "Missing action type!");
                    }
                    // Execute action.
                    switch (action.getType()) {
                        case ATTRIBUTE:
                            checkElementNotNull(state);
                            // Check for attribute existance. It it exists then extract its value.
                            if (state.elements.size() == 1
                                    && state.elements.get(0).hasAttr(action.getActionData())) {
                                states.add(new NamedData(state, action,
                                        state.elements.get(0).attr(action.getActionData())));
                            } else {
                                throw new WrongActionArgs("Element does not have required attribute: "
                                        + action.getActionData() + " action: " + action.getName() + " html: "
                                        + state.elements.html());
                            }
                            break;
                        case HTML:
                            checkElementNotNull(state);
                            // Get value as html.
                            states.add(new NamedData(state, action, state.elements.html()));
                            break;
                        case OUTPUT:
                            // Output string value as RDF statement.
                            if (state.value == null) {
                                // Nothing to output.
                                if (state.elements != null) {
                                    throw new WrongActionArgs("No string value but jsoup elements set for: "
                                            + action.getActionData());
                                }
                            }
                            // Create triple.
                            outData.add(state.subject,
                                    valueFactory.createURI(action.getActionData()),
                                    valueFactory.createLiteral(state.value));
                            // Create triple with type.
                            if (state.subjectClass != null) {
                                outData.add(state.subject, rdfType, state.subjectClass);
                            }
                            // Connect to parent subject.
                            if (state.parentSubject != null && state.hasPredicate != null) {
                                outData.add(state.parentSubject, state.hasPredicate, state.subject);
                            }
                            break;
                        case QUERY:
                            checkElementNotNull(state);
                            // Execute query and store result.
                            states.add(new NamedData(state, action,
                                    state.elements.select(action.getActionData())));
                            break;
                        case SUBJECT:
                            // Test given data.
                            final URI hasPredicate = createUri(valueFactory, action.getActionData());
                            final URI newSubject = valueFactory.createURI(SUBJECT_URI_TEMPLATE
                                    + Integer.toString(subjectIndex++));
                            // Create a new subject with given type and put it into the tree.
                            states.add(new NamedData(state, action, newSubject, null,
                                    hasPredicate == null ? defaultHasPredicate : hasPredicate));
                            break;
                        case TEXT:
                            checkElementNotNull(state);
                            // Get value as a string.
                            states.add(new NamedData(state, action, state.elements.text()));
                            break;
                        case UNLIST:
                            checkElementNotNull(state);
                            for (Element subElement : state.elements) {
                                states.add(new NamedData(state, action, new Elements(subElement)));
                            }
                            break;
                        case SUBJECT_CLASS:
                            final URI classUri = createUri(valueFactory, action.getActionData());
                            states.add(new NamedData(state, action, null, classUri, null));
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    /**
     *
     * @param valueFactory
     * @param subjectURI   Can be null or empty.
     * @return
     */
    private URI createUri(ValueFactory valueFactory, String uriString) {
        try {
            if (uriString != null && !uriString.isEmpty()) {
                return valueFactory.createURI(uriString);
            }
        } catch (RuntimeException ex) {
            LOG.error("Can't generate URI for value: {}", uriString, ex);
        }
        return null;
    }

    /**
     *
     * @param state If elements of given object is null then this method throws an exception.
     * @throws WrongActionArgs
     */
    private void checkElementNotNull(NamedData state) throws WrongActionArgs {
        if (state.elements == null) {
            throw new WrongActionArgs("Elements are null for action: " + state.name);
        }
    }

}

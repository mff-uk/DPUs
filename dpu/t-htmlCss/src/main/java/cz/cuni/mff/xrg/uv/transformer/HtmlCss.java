package cz.cuni.mff.xrg.uv.transformer;

import cz.cuni.mff.xrg.uv.boost.dpu.addon.AddonInitializer;
import cz.cuni.mff.xrg.uv.boost.dpu.addon.impl.CloseCloseable;
import cz.cuni.mff.xrg.uv.boost.dpu.addon.impl.SimpleRdfConfigurator;
import cz.cuni.mff.xrg.uv.boost.dpu.advanced.DpuAdvancedBase;
import cz.cuni.mff.xrg.uv.boost.dpu.config.MasterConfigObject;
import cz.cuni.mff.xrg.uv.boost.dpu.utils.SendMessage;
import cz.cuni.mff.xrg.uv.rdf.utils.dataunit.rdf.simple.OperationFailedException;
import cz.cuni.mff.xrg.uv.rdf.utils.dataunit.rdf.simple.SimpleRdfWrite;
import eu.unifiedviews.dataunit.DataUnit;
import eu.unifiedviews.dataunit.DataUnitException;
import eu.unifiedviews.dataunit.files.FilesDataUnit;
import eu.unifiedviews.dataunit.rdf.WritableRDFDataUnit;
import eu.unifiedviews.dpu.DPU;
import eu.unifiedviews.dpu.DPUException;
import eu.unifiedviews.helpers.dpu.config.AbstractConfigDialog;
import java.io.*;
import java.util.Stack;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Explore the tree of {@link NamedData} using DFS. Does not detect cycles!
 *
 * @author Škoda Petr
 */
@DPU.AsTransformer
public class HtmlCss extends DpuAdvancedBase<HtmlCssConfig_V1> {

    /**
     * Store context in processing.
     */
    private static class NamedData {

        private final String name;

        /**
         * Value in elements if presented.
         */
        private final Elements elements;

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
         * Used to create a first object.
         *
         * @param name
         * @param elements
         * @param subject
         */
        public NamedData(String name, Elements elements, URI subject) {
            this.name = name;
            this.elements = elements;
            this.subject = subject;
            this.subjectClass = null;
            this.value = null;
        }

        public NamedData(NamedData source, HtmlCssConfig_V1.Action action, Elements elements) {
            this.name = action.getOutputName();
            this.elements = elements;
            this.subject = source.subject;
            this.subjectClass = source.subjectClass;
            this.value = null;
        }

        public NamedData(NamedData source, HtmlCssConfig_V1.Action action, URI subject, URI subjectClass) {
            this.name = action.getOutputName();
            this.elements = source.elements;
            this.subject = subject;
            this.subjectClass = subjectClass;
            this.value = source.value;
        }

        public NamedData(NamedData source, HtmlCssConfig_V1.Action action, String value) {
            this.name = action.getOutputName();
            this.elements = null;
            this.subject = source.subject;
            this.subjectClass = source.subjectClass;
            this.value = value;
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

    @SimpleRdfConfigurator.Configure(dataUnitFieldName = "outRdfData")
    public SimpleRdfWrite outData;

    /**
     * Used to generate original subjects.
     */
    private int subjectIndex = 0;

    public HtmlCss() {
        super(HtmlCssConfig_V1.class,
                AddonInitializer.create(new SimpleRdfConfigurator(HtmlCss.class), new CloseCloseable()));
    }

    @Override
    protected void innerExecute() throws DPUException {

        final FilesDataUnit.Iteration iter;
        try {
            iter = inFilesHtml.getIteration();
        } catch (DataUnitException ex) {
            SendMessage.sendMessage(context, ex);
            return;
        }
        getAddon(CloseCloseable.class).add(iter);

        final ValueFactory valueFactory;
        try {
            valueFactory = outData.getValueFactory();
        } catch (OperationFailedException ex) {
            SendMessage.sendMessage(context, ex);
            return;
        }
        // Parse files.
        try {
            while (iter.hasNext() && !context.canceled()) {
                final FilesDataUnit.Entry entry = iter.next();
                LOG.info("Parsing file: {}", entry);
                Document doc = Jsoup.parse(new File(java.net.URI.create(entry.getFileURIString())), null);
                outData.setOutputGraph(entry.getFileURIString());
                // TODO Better generation for subjects.
                parse(valueFactory, doc, entry.getFileURIString());
            }
        } catch (OperationFailedException ex) {
            throw new DPUException(ex);
        } catch (DataUnitException ex) {
            throw new DPUException(ex);
        } catch (IOException ex) {
            throw new DPUException("Can't parse given document.", ex);
        }
    }

    @Override
    public AbstractConfigDialog<MasterConfigObject> getConfigurationDialog() {
        return new HtmlCssVaadinDialog();
    }

    /**
     * Parse given document.
     *
     * @param fileUriStr
     * @param doc
     * @param docUri
     */
    private void parse(ValueFactory valueFactory, Document doc, String docUri)
            throws OperationFailedException, DataUnitException, IOException, WrongActionArgs {
        final URI rootHasPredicate;
        if (config.getHasPredicateAsStr() != null && !config.getHasPredicateAsStr().isEmpty()) {
            rootHasPredicate = valueFactory.createURI(config.getHasPredicateAsStr());
        } else {
            rootHasPredicate = null;
        }
        // Create root subject.
        final URI rootSubject = valueFactory.createURI(docUri);
        // Insert initial data
        if (config.getClassAsStr() != null && !config.getClassAsStr().isEmpty()) {
            final URI rootClass = valueFactory.createURI(config.getClassAsStr());
            outData.add(rootSubject,
                    valueFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                    rootClass);
        }
        // Start and parse.
        final Stack<NamedData> states = new Stack();
        states.add(new NamedData(WEB_PAGE_NAME, doc.getAllElements(),
                valueFactory.createURI(SUBJECT_URI_TEMPLATE + Integer.toString(subjectIndex++))));
        final URI rdfType = valueFactory.createURI(RDF_TYPE_PREDICATE);
        while (!states.isEmpty() && !context.canceled()) {
            // Get group and apply actions.
            final NamedData state = states.pop();
            for (HtmlCssConfig_V1.Action action : config.getActions()) {
                if (action.getName().compareTo(state.name) == 0) {
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
                            // Crreate triple.
                            outData.add(state.subject, valueFactory.createURI(action.getActionData()),
                                    valueFactory.createLiteral(state.value));
                            // Create triple with type.
                            if (state.subjectClass != null) {
                                outData.add(state.subject, rdfType, state.subjectClass);
                            }
                            // Connect to root subject.
                            if (rootHasPredicate != null) {
                                outData.add(rootSubject, rootHasPredicate, state.subject);
                            }
                            break;
                        case QUERY:
                            checkElementNotNull(state);
                            // Execute query and store result.
                            states.add(new NamedData(state, action,
                                    state.elements.select(action.getActionData())));
                            break;
                        case SUBJECT:
                            // Create a new subject with given type and put it into the tree.
                            states.add(new NamedData(state, action,
                                    valueFactory.createURI(SUBJECT_URI_TEMPLATE
                                            + Integer.toString(subjectIndex++)),
                                    createSubject(valueFactory, action.getActionData())));
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
    private URI createSubject(ValueFactory valueFactory, String subjectUri) {
        if (subjectUri == null || subjectUri.isEmpty()) {
            return null;
        }
        return valueFactory.createURI(subjectUri);
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

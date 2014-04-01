package cz.cuni.mff.xrg.intlib.extractor.simplexslt;

import static cz.cuni.mff.xrg.intlib.extractor.simplexslt.SimpleXSLTConfig.OutputType.Literal;
import static cz.cuni.mff.xrg.intlib.extractor.simplexslt.SimpleXSLTConfig.OutputType.RDFXML;
import static cz.cuni.mff.xrg.intlib.extractor.simplexslt.SimpleXSLTConfig.OutputType.TTL;
import cz.cuni.mff.xrg.intlib.extractor.simplexslt.rdfUtils.DataRDFXML;
import cz.cuni.mff.xrg.intlib.extractor.simplexslt.rdfUtils.DataTTL;
import cz.cuni.mff.xrg.intlib.extractor.simplexslt.rdfUtils.RDFLoaderWrapper;

import cz.cuni.mff.xrg.odcs.commons.data.DataUnitException;
import cz.cuni.mff.xrg.odcs.commons.dpu.DPUContext;
import cz.cuni.mff.xrg.odcs.commons.dpu.DPUException;
import cz.cuni.mff.xrg.odcs.commons.dpu.annotation.AsTransformer;
import cz.cuni.mff.xrg.odcs.commons.dpu.annotation.InputDataUnit;
import cz.cuni.mff.xrg.odcs.commons.dpu.annotation.OutputDataUnit;
import cz.cuni.mff.xrg.odcs.commons.message.MessageType;
import cz.cuni.mff.xrg.odcs.commons.module.dpu.ConfigurableBase;
import cz.cuni.mff.xrg.odcs.commons.module.utils.AddTripleWorkaround;
import cz.cuni.mff.xrg.odcs.commons.module.utils.DataUnitUtils;
import cz.cuni.mff.xrg.odcs.commons.ontology.OdcsTerms;
import cz.cuni.mff.xrg.odcs.commons.web.AbstractConfigDialog;
import cz.cuni.mff.xrg.odcs.commons.web.ConfigDialogProvider;
import cz.cuni.mff.xrg.odcs.dataunit.file.FileDataUnit;
import cz.cuni.mff.xrg.odcs.dataunit.file.handlers.DirectoryHandler;
import cz.cuni.mff.xrg.odcs.dataunit.file.handlers.FileHandler;
import cz.cuni.mff.xrg.odcs.dataunit.file.handlers.Handler;
import cz.cuni.mff.xrg.odcs.rdf.exceptions.InvalidQueryException;
import cz.cuni.mff.xrg.odcs.rdf.exceptions.RDFException;
import cz.cuni.mff.xrg.odcs.rdf.help.MyTupleQueryResultIf;
import cz.cuni.mff.xrg.odcs.rdf.help.OrderTupleQueryResult;
import cz.cuni.mff.xrg.odcs.rdf.interfaces.RDFDataUnit;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.slf4j.LoggerFactory;

/**
 * Simple XSLT Extractor
 *
 * DPU which applies XSLT uploaded via configuration dialog to all y within
 * input data (x,<http://linked.opendata.cz/ontology/odcs/xmlValue>,y).
 *
 * Output may be formed either by triples representing the data itself, or by
 * triples of the form (x,a,b), where "b" is the output of the XSLT
 * transformation , optionally encoded (can be defined in the configuration
 * dialog), and "a" is the predicate defined in the configuration dialog. The
 * type of output may be configured in the configuration dialog. Resulting data
 * are encoded as defined in the configuration dialog.
 *
 * @author tomasknap
 */
@AsTransformer
public class SimpleXSLT extends ConfigurableBase<SimpleXSLTConfig> implements ConfigDialogProvider<SimpleXSLTConfig> {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(
            SimpleXSLT.class);

    public SimpleXSLT() {
        super(SimpleXSLTConfig.class);
    }
    @InputDataUnit(name = "rdfInput", optional = true)
    public RDFDataUnit rdfInput;
    @InputDataUnit(name = "fileInput", optional = true)
    public FileDataUnit fileInput;
    @OutputDataUnit(name = "rdfOutput")
    public RDFDataUnit rdfOutput;
    private Map<String, Map<String, String>> metadataForFilePath; // = new HashMap()<String,String)>;

    //TODO support file output
//    @OutputDataUnit(name = "fileOutput", optional = true)
//    public RDFDataUnit fileOutput;
    @Override
    public AbstractConfigDialog<SimpleXSLTConfig> getConfigurationDialog() {
        return new SimpleXSLTDialog();
    }

    private String getXSLTParamSingleValue(Map<String, List<String>> paramMeta, String predicate) {

        //get param name
        if (paramMeta.containsKey(predicate)) {

            List<String> paramValueList = paramMeta.get(predicate);
            if (paramValueList.isEmpty()) {
                log.debug("No param value for predicate {}", predicate);
                return "";
            }
            if (paramValueList.size() > 1) {
                log.debug("More than one param value for predicate {}. Must be only one. Returning NO value.", predicate);
                return "";
            }

            return paramValueList.get(0);
        } else {
            log.debug("Metadata does not contain any value for predicate {}", predicate);
        }
        return "";

    }

    /**
     * Get XSLT Params associated with a file
     *
     * @param fh Handler to file in file data unit
     * @return Pairs param name - param value
     * @throws InvalidQueryException
     */
    private Map<String, String> getXsltParams(Map<String, List<String>> metadata) {

        Map<String, String> result = new HashMap();
        
        List<String> paramURIs;
        if (metadata.containsKey(OdcsTerms.XSLT_PARAM_PREDICATE)) {
            paramURIs = metadata.get(OdcsTerms.XSLT_PARAM_PREDICATE);

            for (String paramURI : paramURIs) {
                //get param name and value

                List<String> predicates = new ArrayList<>();
                predicates.add(OdcsTerms.XSLT_PARAM_NAME_PREDICATE);
                predicates.add(OdcsTerms.XSLT_PARAM_VALUE_PREDICATE);
                Map<String, List<String>> paramMeta = rdfInput.getRDFMetadataForSubjectURI(paramURI, predicates);

                String paramName = getXSLTParamSingleValue(paramMeta, OdcsTerms.XSLT_PARAM_NAME_PREDICATE);
                String paramValue = getXSLTParamSingleValue(paramMeta, OdcsTerms.XSLT_PARAM_VALUE_PREDICATE);

                if (!paramName.isEmpty() && !paramValue.isEmpty()) {
                    log.info("Registering param {} = {}", paramName, paramValue);
                    result.put(paramName, paramValue);
                }

            }

        } else {
            //create subject based on the fixed prefix and filePath
            log.debug("No definition of XSLT params available");
        }

        return result;

    }

    @Override
    public void execute(DPUContext context) throws DPUException, DataUnitException {


        //get working directory 
        File workingDir = context.getWorkingDir();
        String pathToWorkingDir = null;
        try {
            pathToWorkingDir = workingDir.getCanonicalPath();
        } catch (IOException ex) {
            throw new DPUException("Cannot get path to working dir", ex.getCause());
        }

        //check that XSLT is available 
        if (config.getXslTemplate().isEmpty()) {
            throw new DPUException("No XSLT available, execution interrupted");
        }

        //prepare XSLT 
        String pathToXslTemplate = pathToWorkingDir + File.separator + "template.xslt";
        File xslTemplate = new File(pathToXslTemplate);
        log.debug("Path to xslTemplate: {}", pathToXslTemplate);
        //create new file with the xslTemplate content
        DataUnitUtils.storeStringToTempFile(config.getXslTemplate(), pathToXslTemplate);

        //try to compile XSLT
        TransformerFactory tfactory = new net.sf.saxon.TransformerFactoryImpl(); //TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null);
        Templates templates;
        try {
            templates = tfactory.newTemplates(new StreamSource(xslTemplate));
        } catch (TransformerConfigurationException ex) {
            throw new DPUException("Cannot compile XSLT: " + ex.getLocalizedMessage());
        }
        log.info("Stylesheet was compiled successully");

        if (rdfInput != null) {
            String query = "SELECT (count(distinct(?s)) as ?count ) where {?s <" + config.getInputPredicate() + "> ?o}";
            log.debug("Query for counting number of input files: {}", query);
            //get the number of files in the rdf data unit
            MyTupleQueryResultIf executeSelectQueryAsTuplesCount = rdfInput.executeSelectQueryAsTuples(query);
            int resSize = 0;
            try {
                if (executeSelectQueryAsTuplesCount.hasNext()) {
                    BindingSet solution = executeSelectQueryAsTuplesCount.next();
                    Binding b = solution.getBinding("count");
                    String resSizeString = b.getValue().stringValue();
                    resSize = Integer.parseInt(resSizeString);
                }



            } catch (QueryEvaluationException ex) {
                throw new DPUException("Cannot evaluate query for counting number of triples" + ex.getLocalizedMessage());
            }



            if (resSize > 0) {

                context.sendMessage(MessageType.INFO, "Processing RDF INPUT, files: " + resSize );
                //context.sendMessage(MessageType.INFO, "Input files received via RDF data unit: " + resSize);
                //there are some files to be processed received in the input RDF data unit.        
                query = "SELECT ?s ?o where {?s <" + config.getInputPredicate() + "> ?o } ORDER BY ?s ?o";
                log.debug("Query for getting input files: {}", query);
                OrderTupleQueryResult executeSelectQueryAsTuples = rdfInput.executeOrderSelectQueryAsTuples(query);

                //process all the rdf triples
                int fileNumber = 0;
                try {

                    while (executeSelectQueryAsTuples.hasNext()) {

                        fileNumber++;

                        log.info("Processing file: {}/{} ", fileNumber, resSize);

                        //process the inputs
                        BindingSet solution = executeSelectQueryAsTuples.next();
                        Binding b = solution.getBinding("o");
                        String fileContent = b.getValue().stringValue();
                        String subject = solution.getBinding("s").getValue().stringValue();

                        log.info("The subject is {}", subject);
                        //log.debug("The object is {}", fileContent);

                        //store the input content to file, inputs are xml files!
                        String inputFilePath = pathToWorkingDir + File.separator + String.valueOf(fileNumber) + ".xml";
                        File inputFile = DataUnitUtils.storeStringToTempFile(removeTrailingQuotes(fileContent), inputFilePath);
                        if (inputFile == null) {
                            log.warn("Problem processing object for subject {}", subject);
                            continue;
                        }
                        
                        //generate path to file with output
                        //prepare the name of a file holding the output of XSLT
                        String outputPathFileWithoutExtension = prepareOutputFileNameStub(pathToWorkingDir, fileNumber);
                        File outputFile = new File(outputPathFileWithoutExtension);
                        //String outputFilePath = pathToWorkingDir + File.separator + String.valueOf(fileNumber) + ".xml";


                        //get metadata - XSLT params only
                        List<String> predicates = new ArrayList<String>();
                        predicates.add(OdcsTerms.XSLT_PARAM_PREDICATE);
                        Map<String, List<String>> metadata = new HashMap<>();
                        if (rdfInput != null) {
                            metadata = rdfInput.getRDFMetadataForSubjectURI(subject, predicates);
                        }
                         Map<String, String> xsltParams = getXsltParams(metadata);
                       


                        //CALL XSLT Template
                        //TODO Collect XSLT params
                        //Map<String, String> collectedXsltParams = collectXsltParams(xslTemplate,subject);
                        //String outputString = executeXSLT(xslTemplate, file, xsltParams);
                        if (!executeXSLT(templates, inputFile, outputFile, xsltParams)) {
                            log.warn("Problem generating output of xslt transformation for subject {}. No output created. ", subject);
                            continue;
                        }

                        //Prepares object being responsible for storing output of the XSLT to the output rdf data unit 
                        if(!loadDataToTargetRDFDataUnit(outputFile, subject, outputPathFileWithoutExtension)) {
                             log.warn("Problem adding output of xslt transformation for subject {} to rdf data unit. No output created. ", subject);
                             continue;
                        };

                        log.info("XSLT executed successfully, output created successfully");
                        
                        //stores data to target rdf unit
                        //loadDataToTargetRDFDataUnit(resultRDFDataLoader, String.valueOf(fileNumber), context);

                        //if the DPU was cancelled, execution ends. 
                        if (context.canceled()) {
                            log.info("DPU cancelled");
                            return;
                        }

                    }
                } catch (QueryEvaluationException ex) {
                    context.sendMessage(MessageType.ERROR, "Problem evaluating the query to obtain files to be processed. Processing ends.", ex.getLocalizedMessage());
                    log.error("Problem evaluating the query to obtain values of the {} literals. Processing ends.", config.getInputPredicate());
                    log.debug(ex.getLocalizedMessage());
                }

                context.sendMessage(MessageType.INFO, "Processed " + fileNumber + " files");
            } else {

                context.sendMessage(MessageType.INFO, "NO files received via RDF INPUT");

            }
        }
        else {
             context.sendMessage(MessageType.INFO, "NO files received via RDF INPUT");

        }

        //------------------
        // Process data received via FILE DATA UNIT
        //------------------
        if (fileInput != null) {

            context.sendMessage(MessageType.INFO, "Processing FILE INPUT");
            DirectoryHandler root = fileInput.getRootDir();
            int processFiles = processDirectory(root, context, templates);
            context.sendMessage(MessageType.INFO, "Processed " + processFiles + " files");
        } else {
            context.sendMessage(MessageType.INFO, "NO files received via FILE INPUT");
        }

    }

    private int processDirectory(DirectoryHandler directory, DPUContext context, Templates templates) throws DPUException {

        log.debug("Processing directory: {}, path {}", directory.getName(), directory.getRootedPath());
        int filesInDir = 0;
        for (Handler handler : directory) {

            //if the DPU was cancelled, execution ends. 
            if (context.canceled()) {
                throw new DPUException("DPU Cancelled");
            }

            if (handler instanceof FileHandler) {
                // it's a file
                FileHandler file = (FileHandler) handler;
                if (processFile(file, context, templates)) {
                    filesInDir++;
                } else {
                    context.sendMessage(MessageType.WARNING, "Problem processing file " + file.getName());
                }

            } else if (handler instanceof DirectoryHandler) {
                // it's a directory
                DirectoryHandler file = (DirectoryHandler) handler;
                filesInDir += processDirectory(file, context, templates);
            }

        }
        return filesInDir;


    }

    /**
     * Processes a single file - applies XSLT to the file and adds it to the
     * output.
     *
     * @param fh
     * @param context
     * @param xslTemplate
     * @return True if the processing was ok, otherwise false in case of
     * problems
     * @throws DPUException
     */
    private boolean processFile(FileHandler fh, DPUContext context, Templates templates) throws DPUException {

        log.debug("Processing file with file path {}", fh.getRootedPath());

        //inputs
        final File inputFile = fh.asFile();
        String fileName = fh.getName();

        //prepare the stub of the name of a file holding the output of XSLT
        String pathToWorkingDir;
        try {
            pathToWorkingDir = context.getWorkingDir().getCanonicalPath();
        } catch (IOException ex) {
            throw new DPUException("Cannot get path to working dir: " + ex.getLocalizedMessage());
        }
        //prepare the output file holding the output of XSLT
        String outputPathFolder = pathToWorkingDir + File.separator + "out" + File.separator;
        DataUnitUtils.checkExistanceOfDir(outputPathFolder);
        String outputPathFileWithoutExtension = outputPathFolder + fileName;
        File outputFile = new File(outputPathFileWithoutExtension);
        //String outputFilePath = pathToWorkingDir + File.separator + String.valueOf(fileNumber) + ".xml";
        
        //get metadata
        List<String> predicates = new ArrayList<String>();
        predicates.add(OdcsTerms.DATA_UNIT_FILE_URI_PREDICATE);
        predicates.add(OdcsTerms.XSLT_PARAM_PREDICATE);
        Map<String, List<String>> metadata = new HashMap<>();
        if (rdfInput != null) {
            log.info("Trying to fetch metadata from RDF data unit for path {}", fh.getRootedPath());
            metadata = rdfInput.getRDFMetadataForFile(fh.getRootedPath(), predicates);
        }
        else {
            log.info("No metadata available becauese there is no input RDF data unit");
        }
        //metadata
        log.info("Available metadata: {}", printAvailableMetadata(metadata));
        
        //collected XSLT params 
        Map<String, String> collectedXsltParams = getXsltParams(metadata);
        
        //String outputString = executeXSLT(xslTemplate, file, xsltParams);
        if (!executeXSLT(templates, inputFile, outputFile, collectedXsltParams)) {
            log.warn("Problem generating output of xslt transformation for subject {}. No output created. ", fileName);
            return false;
        }
        
        //subject is interesting only when "Literal" output is used
        String subject = "";
        if (config.getOutputType().equals(SimpleXSLTConfig.OutputType.Literal)) {
                
            //if there is proper metadata in the rdf data unit, adjust "subject" (which is equal to filePath by default)
            subject = getXSLTParamSingleValue(metadata, OdcsTerms.DATA_UNIT_FILE_URI_PREDICATE);
            if (!subject.isEmpty()) {
                log.debug("Mapping found, using subject URI {} for file name {}", subject, fileName);
            } else {
                //create subject based on the fixed prefix and filePath
                subject = "http://file" + fh.getRootedPath();
                log.debug("Mapping not found, subject is set to: {}", subject);

            }
        }

        //Prepares object being responsible for storing output of the XSLT to the output rdf data unit 
        if(!loadDataToTargetRDFDataUnit(outputFile, subject, outputPathFileWithoutExtension)) {
             log.warn("Problem adding output of xslt transformation for subject {} to rdf data unit. No output created. ", subject);
             return false;
        }

        log.info("XSLT executed successfully, output created successfully");
        
        
        //if the DPU was cancelled, execution ends. 
        if (context.canceled()) {
            throw new DPUException("DPU Cancelled");
        }

        return true;


    }

    private static String encode(String literalValue, String escapedMappings) {

        String val = literalValue;
        if (escapedMappings.length() > 0) {
            String[] split = escapedMappings.split("\\s+");
            for (String s : split) {
                String[] keyAndVal = s.split(":");
                if (keyAndVal.length == 2) {
                    val = val.replaceAll(keyAndVal[0], keyAndVal[1]);
                    log.debug("Encoding mapping {} to {} was applied.", keyAndVal[0], keyAndVal[1]);

                } else {
                    log.warn("Wrong format of escaped character mappings {}, skipping the mapping", escapedMappings);

                }
            }
        }
        return val;

    }

    /**
     *
     * @param xslTemplate
     * @param inputFile
     * @param xsltParams
     * @param exp Compiled stylesheet
     * @return
     */
    private boolean executeXSLT(Templates templates, File inputFile, File outputFile, Map<String, String> xsltParams) {

        if (inputFile == null) {
            log.error("Invalid inputs to executeXSLT method");
            return false;
        }
         if (outputFile == null) {
            log.error("No output file to executeXSLT method");
            return false;
        }

//        String result = null;
        log.debug("XSLT is being prepared to be executed");
        try {

            Transformer transformer = templates.newTransformer();
            transformer.setParameter(OutputKeys.INDENT, "yes");
            if (!config.getOutputXSLTMethod().isEmpty()) {
                log.debug("Overwriting output method in XSLT from the DPU configuration to {}", config.getOutputXSLTMethod());
                transformer.setParameter(OutputKeys.METHOD, config.getOutputXSLTMethod());
            }

            //set params for the template!
            for (String s : xsltParams.keySet()) {
                //QName langParam = new QName(s);
                //transformer.setParameter(s, new XdmAtomicValue(xsltParams.get(s)));
                transformer.setParameter(s, xsltParams.get(s));
                log.debug("Set param {} with value {}", s, xsltParams.get(s));
            }

            
//            StringWriter resultWriter = new StringWriter();
            Date start = new Date();
            log.debug("XSLT is about to be executed");
            transformer.transform(new StreamSource(inputFile),
                    new StreamResult(outputFile));

            log.debug("XSLT executed in {} ms", (System.currentTimeMillis() - start.getTime()));

//            resultWriter.flush();
//            result = resultWriter.toString();

// TODO Do we need these refinements of the output? 
//            if (result.trim().equals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")) {
//                log.warn("Template applied to the input generated output containing only: <?xml version=\"1.0\" encoding=\"UTF-8\"?> ");
//                return null;
//            }
//
//            if (result.trim().matches("<?xml[^>]*?>")) {
//                log.warn("Template applied to the input generated output containing only: <?xml ... ?> .");
//                return null;
//            }

            return true;

        } catch (TransformerConfigurationException tce) {
            log.error("Exception: " + tce);
        } catch (TransformerException te) {
            log.error("Exception: " + te);
        }
        return false;


        /*
         log.debug("XSLT is being prepared to be executed");
         //xslt
         //        Processor proc = new Processor(false);
         //        XsltCompiler compiler = proc.newXsltCompiler();
         //XsltExecutable exp;
         try {
         //exp = compiler.compile(new StreamSource(xslTemplate));

         XdmNode source = proc.newDocumentBuilder().build(new StreamSource(file));

         Serializer out = new Serializer();
         out.setOutputProperty(Serializer.Property.METHOD, config.getOutputXSLTMethod());
         out.setOutputProperty(Serializer.Property.INDENT, "yes");
         StringWriter sw = new StringWriter();
         out.setOutputWriter(sw);
         //out.setOutputFile(outputFile);

         XsltTransformer trans = exp.load();
         trans.setInitialContextNode(source);
         trans.setDestination(out);

         //set params for the template!
         for (String s : xsltParams.keySet()) {
         QName langParam = new QName(s);
         trans.setParameter(langParam, new XdmAtomicValue(xsltParams.get(s)));
         log.debug("Set param {} with value {}", s, xsltParams.get(s));
         log.debug("Set param {} with value {}", langParam, new XdmAtomicValue(xsltParams.get(s)));
         }

         Date start = new Date();
         log.debug("XSLT is about to be executed");
         trans.transform();
         log.debug("XSLT executed in {} ms", (System.currentTimeMillis() - start.getTime()) );
         //check that we have some output
         String result = sw.toString();
         if (result.trim().isEmpty()) {
         log.warn("Template applied to the input generated empty output.");
         return null;
         }

         if (result.trim().equals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")) {
         log.warn("Template applied to the input generated output containing only: <?xml version=\"1.0\" encoding=\"UTF-8\"?> ");
         return null;
         }

         if (result.trim().matches("<?xml[^>]*?>")) {
         log.warn("Template applied to the input generated output containing only: <?xml ... ?> .");
         return null;
         }


         return result;

         } catch (SaxonApiException ex) {
         log.error(ex.getLocalizedMessage());
         }
      

         return null;
         */

    }

    private String removeTrailingQuotes(String fileContent) {

        if (fileContent.startsWith("\"")) {
            fileContent = fileContent.substring(1);
        }
        if (fileContent.endsWith("\"")) {
            fileContent = fileContent.substring(0, fileContent.length() - 1);
        }
        return fileContent;
    }

    /**
     * Prepares object being responsible for storing output of the XSLT to the
     * output rdf data unit
     *
     * @param outputString Data being result of the XSLT
     * @param subject URI associated with the given processed data and used to
     * hold the processed data in the triple (subject, x, outputString) (used
     * only in case of Literal output to create subject of the new triples)
     * @param outputFileWithoutExtension Prepared stub of the output file which
     * should be created. Extension is added based on the type of the output
     * @return
     */
    private boolean loadDataToTargetRDFDataUnit(File outputFile, String subject, String outputFileWithoutExtension) {

        
        RDFLoaderWrapper loaderWrapper = null;
        switch (config.getOutputType()) {
            case RDFXML:
                loaderWrapper = new DataRDFXML(rdfOutput, outputFile);
                break;
            case TTL:
                loaderWrapper = new DataTTL(rdfOutput, outputFile);
                break;
            case Literal:
                //OUTPUT
                //check URI
                java.net.URI compiledURI = java.net.URI.create(subject);

                //get the content of outputFile
                //TODO adjust the file directly. Currently, this approach is not suitable for big files 
                //because the whole file is loading to String, adjusted, and then loaded back !
                String pathToOutputFile = outputFile.getPath().toString();
                String outputString = DataUnitUtils.readFile(pathToOutputFile);
                
                Resource subj = rdfOutput.createURI(subject);
                URI pred = rdfOutput.createURI(config.getOutputPredicate());
                //encode object as needed
                Value obj = rdfOutput.createLiteral(encode(outputString, config.getEscapedString()));

                String preparedTriple = AddTripleWorkaround.prepareTriple(subj, pred, obj);


                String outputFileNameAdjusted = pathToOutputFile + "TripleWrap";
                File outputFileAdjusted = DataUnitUtils.storeStringToTempFile(preparedTriple, outputFileNameAdjusted);

                loaderWrapper = new DataTTL(rdfOutput, outputFileAdjusted);

                break;

            default:
                log.error("Unsupported type of output");
                return false;

        }
        
         try {
              //load RDF data to data unit
              loaderWrapper.addData();
          } catch (RDFException ex) {
              log.error(ex.getLocalizedMessage());
          }
          log.info("Output created successfully");

        return true;



    }

//    /**
//     * Physically loads the data prepared in the {@link RDFLoaderWrapper}
//     *
//     * @param resultRDFDataLoader Class holding information about the way how
//     * the data should be loaded to output data unit
//     * @param fileNumber Number of file being processed. Used when logging
//     * success/failure
//     * @param context Context of the XSLT DPU used when DPU is canceled
//     */
//    private void loadDataToTargetRDFDataUnit(RDFLoaderWrapper resultRDFDataLoader, String fileName, DPUContext context) {
//        
//        try {
//            //load RDF data to data unit
//            resultRDFDataLoader.addData();
//        } catch (RDFException ex) {
//            log.error(ex.getLocalizedMessage());
//        }
//        log.info("Output created successfully");

//        boolean retry = false;
//        int numberOfTries = 0;
//        do {
//            try {
//
//                resultRDFDataLoader.addData(new File(resultRDFDataLoader.getOutputPath()));
//                log.info("Output created successfully");
//
//
//            } catch (RDFException e) {
//
//                log.warn("Error when adding file {} to the RDF data unit", fileName);
//                log.debug("Error: {}", e.getLocalizedMessage());
//
//                if (e.getCause() != null) {
//                    log.debug("Cause: {}", e.getCause().getLocalizedMessage());
//                }
//
//
//                if ((config.getNumberOfTriesToConnect() != -1) && (numberOfTries >= config.getNumberOfTriesToConnect())) {
//                    log.warn("Error still occurs after {} tries, skipping input {}", config.getNumberOfTriesToConnect(), fileName);
//                    retry = false;
//                } else {
//                    retry = true;
//                    numberOfTries++;
//
//                    if (context.canceled()) {
//                        log.info("DPU cancelled, no further attempts");
//                        return;
//                    }
//
//                    log.info("Trying again in 10s");
//                    try {
//                        Thread.sleep(10000);
//                    } catch (InterruptedException ex) {
//                        log.info("Sleep interrupted, continues");
//                    }
//
//                }
//
//
//
//            }
//        } while (retry);

//    }

    private String prepareOutputFileNameStub(String pathToWorkingDir, int fileNumber) {

        String outputPathFolder = pathToWorkingDir + File.separator + "out" + File.separator;
        DataUnitUtils.checkExistanceOfDir(outputPathFolder);
        return outputPathFolder + String.valueOf(fileNumber);

    }

    private String printAvailableMetadata(Map<String, List<String>> metadata) {
        
        StringBuilder res = new StringBuilder();
        for (String key : metadata.keySet()) {
            res.append("\nKey: ");
            res.append(key);
            List<String> values = metadata.get(key);
            res.append("Values: ");
            for (String value: values) {
                
                 res.append(value);
                 res.append(", ");
            }
        }
        return res.toString();
        
        
    }
}

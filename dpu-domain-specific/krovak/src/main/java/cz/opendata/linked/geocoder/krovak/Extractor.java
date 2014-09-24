package cz.opendata.linked.geocoder.krovak;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.QueryEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.cuni.mff.xrg.uv.boost.dpu.advanced.DpuAdvancedBase;
import cz.cuni.mff.xrg.uv.boost.dpu.addon.AddonInitializer;
import cz.cuni.mff.xrg.uv.boost.dpu.addon.impl.SimpleRdfConfigurator;
import eu.unifiedviews.dataunit.DataUnit;
import cz.cuni.mff.xrg.uv.boost.dpu.config.MasterConfigObject;
import eu.unifiedviews.dpu.DPU;
import eu.unifiedviews.dpu.DPUContext;
import eu.unifiedviews.dpu.DPUException;
import eu.unifiedviews.helpers.dpu.config.AbstractConfigDialog;
import eu.unifiedviews.dataunit.rdf.RDFDataUnit;
import eu.unifiedviews.dataunit.rdf.WritableRDFDataUnit;
import cz.cuni.mff.xrg.uv.rdf.utils.dataunit.rdf.simple.*;

import org.openrdf.model.*;
import org.openrdf.query.TupleQueryResult;

@DPU.AsExtractor
public class Extractor 
extends DpuAdvancedBase<ExtractorConfig> 
{

    private static final Logger LOG = LoggerFactory.getLogger(Extractor.class);
    
    @DataUnit.AsInput(name = "points")
    public RDFDataUnit gmlPoints;

    @DataUnit.AsOutput(name = "Geocoordinates")
    public WritableRDFDataUnit outGeo;    
    
	@SimpleRdfConfigurator.Configure(dataUnitFieldName="outGeo")
	public SimpleRdfWrite geoValueFacWrap;
    
    public Extractor() {
        super(ExtractorConfig.class,AddonInitializer.create(new SimpleRdfConfigurator(Extractor.class)));
    }

    @Override
    public AbstractConfigDialog<MasterConfigObject> getConfigurationDialog() {        
        return new ExtractorDialog();
    }

    @Override
    protected void innerExecute() throws DPUException, OperationFailedException
    {
        java.util.Date date = new java.util.Date();
        long start = date.getTime();

        final SimpleRdfRead gmlPointsWrap = SimpleRdfFactory.create(gmlPoints, context);    
        
        final ValueFactory geoValueFactory = geoValueFacWrap.getValueFactory();
                
        String countQuery = 
                  "PREFIX s: <http://schema.org/> "
                + "PREFIX gml: <http://www.opengis.net/ont/gml#> "
                + "SELECT (COUNT (*) as ?count) "
                + "WHERE "
                + "{"
                    + "?point a gml:Point . "
                +  "}"; 
        String notGCcountQuery = 
                  "PREFIX s: <http://schema.org/> "
                + "PREFIX gml: <http://www.opengis.net/ont/gml#> "
                + "SELECT (COUNT (*) as ?count) "
                + "WHERE "
                + "{"
                + "?point a gml:Point . "
                    + "FILTER NOT EXISTS {?point s:geo ?geo}"
                +  "}"; 
        String sOrgConstructQuery = "PREFIX s: <http://schema.org/> "
                + "PREFIX gml: <http://www.opengis.net/ont/gml#> "
                + "CONSTRUCT {?point ?p ?o}"
                + "WHERE "
                + "{"
                + "?point a gml:Point ; "
                    + "            ?p ?o . "
                    + "FILTER NOT EXISTS {?point s:geo ?geo}"
                +  "}"; 

        LOG.debug("Init");
        
        int total = 0;
        int ngc = 0;
        
        try (ConnectionPair<TupleQueryResult> countres = gmlPointsWrap.executeSelectQuery(countQuery);
                ConnectionPair<TupleQueryResult> countnotGC = gmlPointsWrap.executeSelectQuery(notGCcountQuery)) {
            total = Integer.parseInt(countres.getObject().next().getValue("count").stringValue());
            ngc = Integer.parseInt(countnotGC.getObject().next().getValue("count").stringValue());
            context.sendMessage(DPUContext.MessageType.INFO, "Found " + total + " points, " + ngc + " not transformed yet.");
        } catch (NumberFormatException | QueryEvaluationException e) {
            LOG.error("Failed to execute query and convert result.", e);
        }

        //Schema.org addresses
        LOG.debug("Getting point data via query: " + sOrgConstructQuery);
        //MyTupleQueryResult res = sAddresses.executeSelectQueryAsTuples(sOrgQuery);
        try (ConnectionPair<Graph> resGraph = gmlPointsWrap.executeConstructQuery(sOrgConstructQuery)) {
            transform(geoValueFactory, ngc, resGraph.getObject(), context, geoValueFacWrap, start);
        }
    }

    private void transform(final ValueFactory geoValueFactory, int ngc,
            Graph resGraph, DPUContext context, final SimpleRdfWrite geoValueFacWrap,
            long start) throws OperationFailedException {
        int count = 0;
        int failed = 0;
        
        URI gmlPoint = geoValueFactory.createURI("http://www.opengis.net/ont/gml#Point");
        URI gmlId = geoValueFactory.createURI("http://www.opengis.net/ont/gml#id");
        URI gmlPos = geoValueFactory.createURI("http://www.opengis.net/ont/gml#pos");
        URI gmlSRS = geoValueFactory.createURI("http://www.opengis.net/ont/gml#srsName");
        URI krovak = geoValueFactory.createURI("urn:ogc:def:crs:EPSG::5514");

        URI geoURI = geoValueFactory.createURI("http://schema.org/geo");
        URI geocoordsURI = geoValueFactory.createURI("http://schema.org/GeoCoordinates");
        //URI xsdDouble = geoValueFacory.createURI("http://www.w3.org/2001/XMLSchema#double");
        //URI xsdDecimal = geoValueFacory.createURI("http://www.w3.org/2001/XMLSchema#decimal");
        URI longURI = geoValueFactory.createURI("http://schema.org/longitude");
        URI latURI = geoValueFactory.createURI("http://schema.org/latitude");        
        
        int expectedNumOfBlocks = ngc/config.getNumofrecords() + 1;

        LOG.debug("Starting transformation, estimating " + expectedNumOfBlocks + " blocks. ");

        Iterator<Statement> it = resGraph.match(null, RDF.TYPE, gmlPoint);

        String url = "http://geoportal.cuzk.cz/(S(" + config.getSessionId() + "))/WCTSHandlerhld.ashx";
        HttpClient httpclient = HttpClientBuilder.create().build();

        int blocksDone = 0;
        while (it.hasNext() && !context.canceled())
        {
            int currentBlock = 0;
            StringBuilder lines = new StringBuilder();
            HashMap<String, String> uriMap = new HashMap<String, String>();

            while (currentBlock < config.getNumofrecords() && it.hasNext() && !context.canceled())
            {
                count++;
                Resource currentPointURI = it.next().getSubject();
                //logger.trace("Point " + count + "/" + ngc + ": " + currentPointURI.toString());
                if (resGraph.match(currentPointURI, gmlSRS, krovak).hasNext())
                {
                    currentBlock++;
                    
                    Iterator<Statement> it1 = resGraph.match(currentPointURI, gmlId, null);
                    
                    Value id = it1.next().getObject();
                    
                    it1 = resGraph.match(currentPointURI, gmlPos, null);
                    
                    Value pos = it1.next().getObject();

                    String posString = pos.stringValue();
                    String Y = posString.substring(0, posString.indexOf(" "));
                    String X = posString.substring(posString.indexOf(" ") + 1);
                    String name = id.stringValue().replace(".", "_");

                    uriMap.put(name, currentPointURI.toString());

                    lines.append(name + "\t" + Y + "\t" + X + "\t0\t\r\n");

                }
                else
                {
                    LOG.info("Point " + currentPointURI.toString() + " not Krovak");
                }
            }

            if (context.canceled()) break;

            blocksDone++;

            LOG.info("Block " + blocksDone + "/" + expectedNumOfBlocks + " of " + currentBlock + " records prepared to send");

            String file = lines.toString();

            HttpResponse response = null;
            boolean goodresponse = false;
            int tries = 0;
            while ((response == null || !goodresponse) && !context.canceled())
            {
                tries++;
                LOG.debug("Try " + tries);
                goodresponse = false;
                try {
                    
                    MultipartEntityBuilder multipartEntity = MultipartEntityBuilder.create();        
                    multipartEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                    multipartEntity.setBoundary("----WebKitFormBoundaryCYQR5wAfAoAP7BrE");

                    multipartEntity.addTextBody("source", "File");
                    multipartEntity.addTextBody("sourceSRS", "urn:ogc:def:crs,crs:EPSG::5514,crs:EPSG::5705");
                    multipartEntity.addTextBody("targetSRS", "urn:ogc:def:crs:EPSG::4937");
                    multipartEntity.addTextBody("sourceXYorder", "xy");
                    multipartEntity.addTextBody("targetXYorder", "xy");
                    multipartEntity.addTextBody("sourceSixtiethView", "false");
                    multipartEntity.addTextBody("targetSixtiethView", "true");
                    multipartEntity.addTextBody("wcts_fileType", "text");
                    multipartEntity.addBinaryBody("wcts_file1", new ByteArrayInputStream(file.getBytes()), ContentType.TEXT_PLAIN, "geo.txt");

                    HttpEntity mpe = multipartEntity.build();
                    HttpPost httppost = new HttpPost(url);
                    httppost.setEntity(mpe);
                    response = httpclient.execute(httppost);
                    httppost.releaseConnection();

                } catch (ClientProtocolException e) {
                    LOG.error(e.getLocalizedMessage());
                    try {
                        Thread.sleep(config.getFailInterval());
                    } catch (InterruptedException e1) {

                    }
                    continue;
                } catch (IOException e) {
                    LOG.error(e.getLocalizedMessage());
                    try {
                        Thread.sleep(config.getFailInterval());
                    } catch (InterruptedException e1) {

                    }
                    continue;
                }
                if (response == null) {
                    LOG.warn("Response null, sleeping and trying again");
                    try {
                        Thread.sleep(config.getFailInterval());
                        continue;
                    } catch (InterruptedException e) { 
                        continue;
                    }
                }
                
                HttpEntity resEntity = response.getEntity();                
                LOG.debug("Got response");

                String result;
                if (resEntity != null) {
                    InputStream inputStream;
                    try {
                        inputStream = resEntity.getContent();
                    } catch (IllegalStateException | IOException e) {
                        LOG.error("Operation failed.", e);
                        try {
                            Thread.sleep(config.getFailInterval());
                        } catch (InterruptedException e1) {

                        }
                        continue;
                    }

                    StringWriter writer = new StringWriter();
                    try {
                        IOUtils.copy(inputStream, writer, "UTF-8");
                    } catch (IOException e) {
                        LOG.error(e.getLocalizedMessage());
                        try {
                            Thread.sleep(config.getFailInterval());
                        } catch (InterruptedException e1) {

                        }
                        continue;
                    }
                    result = writer.toString();
                }
                else continue;

                String[] resultLines = result.split("\\r\\n");

                boolean linesok = true;
                for (String currentLine : resultLines)
                {
                    String[] columns = currentLine.split("\\s");
                    
                    String currentPointUri = uriMap.get(columns[0]); 
                    BigDecimal latitude, longitude;
                    try {
                        latitude = new BigDecimal(Double.parseDouble(columns[1]) + (Double.parseDouble(columns[2])/60) + (Double.parseDouble(columns[3]) / 3600));
                        longitude = new BigDecimal(Double.parseDouble(columns[4]) + (Double.parseDouble(columns[5])/60) + (Double.parseDouble(columns[6]) / 3600));
                    }
                    catch (Exception e)
                    {
                        LOG.warn(e.getLocalizedMessage(), e);
                        try {
                            Thread.sleep(config.getFailInterval());
                        } catch (InterruptedException e1) {

                        }
                        linesok = false;
                        break;
                    }
                    goodresponse = true;

                    URI origPoinURI = geoValueFactory.createURI(currentPointUri);
                    URI coordURI = geoValueFactory.createURI(currentPointUri+"/wgs84");

                    geoValueFacWrap.add(origPoinURI, geoURI , coordURI);
                    geoValueFacWrap.add(coordURI, RDF.TYPE, geocoordsURI);
                    geoValueFacWrap.add(coordURI, longURI, geoValueFactory.createLiteral(longitude.toString()/*, xsdDecimal*/));
                    geoValueFacWrap.add(coordURI, latURI, geoValueFactory.createLiteral(latitude.toString()/*, xsdDecimal*/));
                }
                
                if (linesok) 
                {
                    goodresponse = true;
                    LOG.info("Successfully got response for block: " + blocksDone);
                }

            }
        }
        if (context.canceled()) LOG.info("Cancelled");
        
        LOG.info("Transformation done.");
        
        java.util.Date date2 = new java.util.Date();
        long end = date2.getTime();

        context.sendMessage(DPUContext.MessageType.INFO, "Transformed: " + count + " in " + (end-start) + "ms, failed attempts: " + failed);
    }


}

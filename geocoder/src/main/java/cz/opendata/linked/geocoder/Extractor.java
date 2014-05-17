package cz.opendata.linked.geocoder;

import cz.cuni.mff.xrg.odcs.commons.data.DataUnitException;
import java.io.File;
import java.util.Iterator;

import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.QueryEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.cuni.mff.xrg.odcs.commons.dpu.DPU;
import cz.cuni.mff.xrg.odcs.commons.dpu.DPUContext;
import cz.cuni.mff.xrg.odcs.commons.dpu.DPUException;
import cz.cuni.mff.xrg.odcs.commons.dpu.annotation.AsExtractor;
import cz.cuni.mff.xrg.odcs.commons.dpu.annotation.InputDataUnit;
import cz.cuni.mff.xrg.odcs.commons.dpu.annotation.OutputDataUnit;
import cz.cuni.mff.xrg.odcs.commons.message.MessageType;
import cz.cuni.mff.xrg.odcs.commons.module.dpu.ConfigurableBase;
import cz.cuni.mff.xrg.odcs.commons.web.AbstractConfigDialog;
import cz.cuni.mff.xrg.odcs.commons.web.ConfigDialogProvider;
import cz.cuni.mff.xrg.odcs.rdf.RDFDataUnit;
import cz.cuni.mff.xrg.odcs.rdf.WritableRDFDataUnit;
import cz.cuni.mff.xrg.odcs.rdf.simple.ConnectionPair;
import cz.cuni.mff.xrg.odcs.rdf.simple.OperationFailedException;
import cz.cuni.mff.xrg.odcs.rdf.simple.SimpleRdfRead;
import cz.cuni.mff.xrg.odcs.rdf.simple.SimpleRdfWrite;
import cz.opendata.linked.geocoder.lib.Geocoder;
import cz.opendata.linked.geocoder.lib.Geocoder.GeoProvider;
import cz.opendata.linked.geocoder.lib.Geocoder.GeoProviderFactory;
import cz.opendata.linked.geocoder.lib.Position;
import org.openrdf.model.*;
import org.openrdf.query.TupleQueryResult;

@AsExtractor
public class Extractor 
extends ConfigurableBase<ExtractorConfig> 
implements DPU, ConfigDialogProvider<ExtractorConfig> {

	private static final Logger LOG = LoggerFactory.getLogger(Extractor.class);
	
	@InputDataUnit(name = "Schema.org addresses")
	public RDFDataUnit sAddresses;

	@OutputDataUnit(name = "Geocoordinates")
	public WritableRDFDataUnit outGeo;	
	
	public Extractor() {
		super(ExtractorConfig.class);
	}

	@Override
	public AbstractConfigDialog<ExtractorConfig> getConfigurationDialog() {		
		return new ExtractorDialog();
	}

	@Override
	public void execute(DPUContext ctx) throws DPUException, DataUnitException
	{
		java.util.Date date = new java.util.Date();
		long start = date.getTime();

		SimpleRdfWrite geoValueFacWrap = new SimpleRdfWrite(outGeo, ctx);		
		final ValueFactory geoValueFac = geoValueFacWrap.getValueFactory();
		
		SimpleRdfRead sAddressesWrap = new SimpleRdfRead(sAddresses, ctx);
		final ValueFactory addrValueFac = sAddressesWrap.getValueFactory();
		
		String geoCache = new File(ctx.getGlobalDirectory(), "cache/geocoder.cache").getAbsolutePath();
		String countQuery = "PREFIX s: <http://schema.org/> "
				+ "SELECT (COUNT (*) as ?count) "
				+ "WHERE "
				+ "{"
					+ "?address a s:PostalAddress . "
				+  "}"; 
		String sOrgConstructQuery = "PREFIX s: <http://schema.org/> "
				+ "CONSTRUCT {?address ?p ?o}"
				+ "WHERE "
				+ "{"
					+ "?address a s:PostalAddress ;"
					+ "			?p ?o . "
				+  "}"; 
		String sOrgQuery = "PREFIX s: <http://schema.org/> "
				+ "SELECT DISTINCT * "
				+ "WHERE "
				+ "{"
					+ "{?address a s:PostalAddress . } "
					+ "UNION { ?address s:streetAddress ?street . } "
					+ "UNION { ?address s:addressRegion ?region . } "
					+ "UNION { ?address s:addressLocality ?locality . } "
					+ "UNION { ?address s:postalCode ?postal . } "
					+ "UNION { ?address s:addressCountry ?country . } "
				+ " }";

		LOG.debug("Geocoder init");
		Geocoder.loadCacheIfEmpty(geoCache);
		GeoProvider gisgraphy = GeoProviderFactory.createXMLGeoProvider(
                config.getGeocoderURI() + "/fulltext/fulltextsearch?allwordsrequired=false&from=1&to=1&q=",
                "/response/result/doc[1]/double[@name=\"lat\"]",
                "/response/result/doc[1]/double[@name=\"lng\"]",
                0);
		LOG.debug("Geocoder initialized");

		int total = 0;
		try (ConnectionPair<TupleQueryResult> countres = sAddressesWrap.executeSelectQuery(countQuery)) {
			total = Integer.parseInt(countres.getObject().next().getValue("count").stringValue());
			LOG.info("Found " + total + " addresses.");
		} catch (NumberFormatException | QueryEvaluationException e) {
			LOG.error("Failed to query and parse value.", e);
		}

		//Schema.org addresses
		LOG.debug("Executing Schema.org query: " + sOrgQuery);
		//MyTupleQueryResult res = sAddresses.executeSelectQueryAsTuples(sOrgQuery);

		LOG.debug("Starting geocoding.");
		try (ConnectionPair<Graph> resGraph = sAddressesWrap.executeConstructQuery(sOrgConstructQuery)) {
			geocode(resGraph.getObject(), ctx, total, gisgraphy, geoValueFac, addrValueFac, geoValueFacWrap, geoCache, start);
		}
	}

	private void geocode(
			Graph resGraph, DPUContext ctx,
			int total, GeoProvider gisgraphy,
			final ValueFactory geoValueFac, final ValueFactory addrValueFac,
			SimpleRdfWrite geoValueFacWrap,
			String geoCache, long start) throws OperationFailedException {

		int count = 0;
		int failed = 0;
		
		URI geoURI = geoValueFac.createURI("http://schema.org/geo");
		URI geocoordsURI = geoValueFac.createURI("http://schema.org/GeoCoordinates");
		URI postalAddressURI = addrValueFac.createURI("http://schema.org/PostalAddress");
		URI streetAddressURI = addrValueFac.createURI("http://schema.org/streetAddress");
		URI addressRegionURI = addrValueFac.createURI("http://schema.org/addressRegion");
		URI addressLocalityURI = addrValueFac.createURI("http://schema.org/addressLocality");
		URI addressCountryURI = addrValueFac.createURI("http://schema.org/addressCountry");
		URI postalCodeURI = addrValueFac.createURI("http://schema.org/postalCode");
		URI xsdDouble = geoValueFac.createURI("http://www.w3.org/2001/XMLSchema#double");
		URI xsdDecimal = geoValueFac.createURI("http://www.w3.org/2001/XMLSchema#decimal");
		URI longURI = geoValueFac.createURI("http://schema.org/longitude");
		URI latURI = geoValueFac.createURI("http://schema.org/latitude");		
		
		URI[] propURIs = new URI [] {streetAddressURI, addressLocalityURI, addressRegionURI, postalCodeURI, addressCountryURI};
		Iterator<Statement> it = resGraph.match(null, RDF.TYPE, postalAddressURI);
		while (it.hasNext() && !ctx.canceled())
		{
			count++;
			Resource currentAddressURI = it.next().getSubject();
			StringBuilder addressToGeoCode = new StringBuilder();

			for (URI currentPropURI : propURIs)
			{
				Iterator<Statement> it1 = resGraph.match(currentAddressURI, currentPropURI, null);
				
				if (it1.hasNext())
				{
					Value currentValue = it1.next().getObject();
					if (currentValue != null)
					{
						//logger.trace("Currently " + currentBinding);
						String currentValueString = currentValue.stringValue();
						//logger.trace("Value " + currentValueString);
						addressToGeoCode.append(currentValueString);
						addressToGeoCode.append(" ");
					}
				}
			}
			/*			}
			
			String[] props = new String [] {"street", "locality", "region", "postal", "country"};
			while (res.hasNext() && !ctx.canceled())
			{
			count++;
			BindingSet s = res.next();
			StringBuilder addressToGeoCode = new StringBuilder();

			for (String currentBinding : props)
			{
			Value currentValue = s.getValue(currentBinding);
			if (s.hasBinding(currentBinding) && currentValue != null)
			{
			//logger.trace("Currently " + currentBinding);
			String currentValueString = currentValue.stringValue();
			//logger.trace("Value " + currentValueString);
			addressToGeoCode.append(currentValueString);
			addressToGeoCode.append(" ");
			}
			}*/

			String address = addressToGeoCode.toString();
			LOG.debug("Address to geocode (" + count + "/" + total + "): " + address);

			Position pos = Geocoder.locate(address, gisgraphy);

			Double latitude;
			Double longitude;

			if (pos != null)
			{
				latitude = pos.getLatitude();
				longitude = pos.getLongitude();
				LOG.debug("Located " + address + " Latitude: " + latitude + " Longitude: " + longitude);

				String uri = currentAddressURI.stringValue();
//					String uri = s.getValue("address").stringValue();
				URI addressURI = geoValueFac.createURI(uri);
				URI coordURI = geoValueFac.createURI(uri+"/geocoordinates");

				geoValueFacWrap.add(addressURI, geoURI , coordURI);
				geoValueFacWrap.add(coordURI, RDF.TYPE, geocoordsURI);
				geoValueFacWrap.add(coordURI, longURI, geoValueFac.createLiteral(longitude.toString()/*, xsdDecimal*/));
				geoValueFacWrap.add(coordURI, latURI, geoValueFac.createLiteral(latitude.toString()/*, xsdDecimal*/));
			}
			else {
				failed++;
				LOG.warn("Failed to locate: " + address);
			}
		}
		if (ctx.canceled()) LOG.info("Cancelled");		
		
		LOG.info("Geocoding done.");
		
		if (config.isRewriteCache()) {
			LOG.debug("Saving geo cache");
			Geocoder.saveCache(geoCache);
			LOG.debug("Geo cache saved.");
		}
		
		java.util.Date date2 = new java.util.Date();
		long end = date2.getTime();

		ctx.sendMessage(MessageType.INFO, "Processed " + count + " in " + (end-start) + "ms, failed attempts: " + failed);
	}

}
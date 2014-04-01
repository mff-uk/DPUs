/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.xrg.intlib.extractor.simplexslt.rdfUtils;

import cz.cuni.mff.xrg.odcs.rdf.exceptions.RDFException;
import cz.cuni.mff.xrg.odcs.rdf.interfaces.RDFDataUnit;
import java.io.File;

/**
 *
 * @author tomasknap
 */
public class DataRDFXML extends RDFLoaderWrapper {

    public DataRDFXML(RDFDataUnit _du, File outputFile) {
        super(_du, outputFile);
    }

    
    @Override
    public void addData() throws RDFException {
         du.addFromRDFXMLFile(outputFile);
         
    }
    
}

package cz.cuni.mff.xrg.uv.postaladdress.to.ruain.mapping;

import cz.cuni.mff.xrg.uv.postaladdress.to.ruain.knowledge.KnowledgeBase;
import cz.cuni.mff.xrg.uv.postaladdress.to.ruain.streetAddress.StreetAddress;
import cz.cuni.mff.xrg.uv.postaladdress.to.ruain.query.Requirement;
import cz.cuni.mff.xrg.uv.postaladdress.to.ruain.query.Subject;
import cz.cuni.mff.xrg.uv.postaladdress.to.ruain.streetAddress.StreetAddressParser;
import cz.cuni.mff.xrg.uv.postaladdress.to.ruain.streetAddress.WrongAddressFormatException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Škoda Petr
 */
public class StreetAddressMapper extends StatementMapper {

    private final StreetAddressParser parser = new StreetAddressParser();
    
    private final KnowledgeBase knowledgeBase;
    
    public StreetAddressMapper(ErrorLogger errorLogger) {
        super(errorLogger);
        this.knowledgeBase = null;
    }

    public StreetAddressMapper(ErrorLogger errorLogger, KnowledgeBase knowledgeBase) {
        super(errorLogger);
        this.knowledgeBase = knowledgeBase;
    }
    
    @Override
    public boolean canMap(String predicate) {
        return predicate.compareTo("http://schema.org/streetAddress") == 0;
    }

    @Override
    public List<Requirement> map(String predicate, String object) {
        final String value = object.trim();
        final List<Requirement> results = new LinkedList<>();
        // map the value
        StreetAddress address; 
        try {
            address = parser.parse(value);
        }catch (WrongAddressFormatException ex) {
            errorLogger.failedToMap(predicate, object, "Failed to parse address", ex);
            return Collections.EMPTY_LIST;
        }
        
        // do we have knowledge base?
        if (knowledgeBase == null) {
            mapWithoutKnowledge(address.getTownName(), address.getStreetName(),
                    results);
        } else {
            mapWithKnowledge(address.getTownName(), address.getStreetName(),
                    results);
        }
                
        if (address.getLandRegistryNumber() != null) {
            // check that it's number
            final Integer number;
            try {
                number = Integer.parseInt(address.getLandRegistryNumber()
                        .replaceAll("\\s", ""));
                results.add(new Requirement(Subject.ADRESNI_MISTO, 
                        "r:cisloDomovni", number.toString()));
            } catch (NumberFormatException ex) {
                errorLogger.failedToMap(predicate, object, "Failed to parse 'číslo popisené'");
            }
        }
        
        if (address.getHouseNumber() != null) {
            // check that it's number
            final Integer number;
            try {
                number = Integer.parseInt(address.getHouseNumber()
                        .replaceAll("\\s", ""));
                results.add(new Requirement(Subject.ADRESNI_MISTO, 
                        "r:cisloOrientacni", number.toString()));
            } catch (NumberFormatException ex) {
                errorLogger.failedToMap(predicate, object, "Failed to parse 'číslo orientační'");
            }
        }
        
        return results;
    }

    private void mapWithoutKnowledge(String townName, String streetName, 
            List<Requirement> results) {
        if (townName != null) {
            results.add(createRequirementTownName(townName));
        }
        if (streetName != null) {
            results.add(createRequirementStreetName(streetName));
        }        
    }

    /**
     * Use {@link #knowledgeBase} to check the validity of given names.
     * 
     * @param townName
     * @param streetName
     * @param results 
     */
    private void mapWithKnowledge(String townName, String streetName, 
            List<Requirement> results) {
        
        String checkedTownName = knowledgeBase.checkTownName(townName);
        String checkedStreetName = knowledgeBase.checkStreetName(streetName);
        
        if (checkedStreetName != null) {
            // it's street name
            results.add(createRequirementStreetName(checkedStreetName));
        }
        
        if (checkedTownName != null) {
            // it's town name
            results.add(createRequirementTownName(checkedTownName));
        }
        
        // if checked town or street name is null, we check for
        // switch
        
        if (checkedStreetName == null) {
            final String streetAsTown = knowledgeBase.checkTownName(streetName);
            if (streetAsTown != null) {
                if (checkedTownName == null) {
                    // street name is in fact town name, and original town name
                    // is missing
                    results.add(createRequirementTownName(streetAsTown));
                } else {
                    // TODO street name is twice here
                }
            }
        }
        
        if (checkedTownName == null) {
            final String townAsStreet = knowledgeBase.checkStreetName(townName);
            if (townAsStreet != null) {
                if (checkedTownName == null) {
                    // town name is street name
                    results.add(createRequirementStreetName(townAsStreet));
                } else {
                    // TODO town name is twice here
                }
            }
        }        
    }    
    
    private Requirement createRequirementTownName(String townName) {
        return new Requirement(Subject.OBEC, "s:name", 
                    String.format("\"%s\"", townName));
    }
    
    private Requirement createRequirementStreetName(String streetName) {
        return new Requirement(Subject.ULICE, "s:name", 
                    String.format("\"%s\"", streetName));
    }
    
    
}

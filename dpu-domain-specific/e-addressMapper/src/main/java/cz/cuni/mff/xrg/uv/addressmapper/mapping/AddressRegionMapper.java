package cz.cuni.mff.xrg.uv.addressmapper.mapping;

import cz.cuni.mff.xrg.uv.addressmapper.knowledge.KnowledgeBase;
import cz.cuni.mff.xrg.uv.addressmapper.ontology.Ruian;
import cz.cuni.mff.xrg.uv.addressmapper.query.Requirement;
import cz.cuni.mff.xrg.uv.addressmapper.ontology.Subject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Škoda Petr
 */
public class AddressRegionMapper extends StatementMapper {

    public static final String NAME = "kraj";
    
    private final Map<String, String> vuscMap = new HashMap<>();

    AddressRegionMapper() { }
    
    @Override
    public void bind(ErrorLogger errorLogger, List<String> uri,
            KnowledgeBase knowledgeBase) {
        super.bind(errorLogger, uri, knowledgeBase);
        for (String item : knowledgeBase.getRegions()) {
            vuscMap.put(item.toLowerCase(), item);
        }
    }
    
    @Override
    public String getName() {
        return NAME;
    }
       
    @Override
    public List<Requirement> map(String predicate, String object) {
        final List<Requirement> results = new LinkedList<>();

        if (vuscMap.containsKey(object.toLowerCase())) {
            results.add(createRequirement(vuscMap.get(object.toLowerCase())));
            return results;
        } else {
            // iterate over list and search for match
            // helps in cases where the "Kraj" "kraj" is omited
            // specially in case of coi.cz Kraj Vysočina is denoted as Vysočina
            String objectLowerCase = object.toLowerCase();
            for (String key : vuscMap.keySet()) {
                if (key.contains(objectLowerCase)) {
                    results.add(createRequirement(vuscMap.get(key)));
                    return results;
                }
            }
        }
        // we do not know what to map ..        
        return results;
    }
    
    private Requirement createRequirement(String value) {
        return new Requirement(Subject.VUSC, "<" + Ruian.P_NAME + ">", 
                "\"" + value + "\"");
    }

}
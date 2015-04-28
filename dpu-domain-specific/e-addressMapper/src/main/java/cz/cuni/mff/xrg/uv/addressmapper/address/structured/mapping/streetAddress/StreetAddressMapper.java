package cz.cuni.mff.xrg.uv.addressmapper.address.structured.mapping.streetAddress;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cz.cuni.mff.xrg.uv.addressmapper.AddressMapperOntology;
import cz.cuni.mff.xrg.uv.addressmapper.address.structured.mapping.AbstractMapper;
import cz.cuni.mff.xrg.uv.addressmapper.address.structured.PostalAddress;
import cz.cuni.mff.xrg.uv.addressmapper.knowledgebase.KnowledgeBase;
import cz.cuni.mff.xrg.uv.addressmapper.knowledgebase.KnowledgeBaseException;
import cz.cuni.mff.xrg.uv.addressmapper.objects.Report;
import cz.cuni.mff.xrg.uv.addressmapper.ruian.RuianEntity;
import cz.cuni.mff.xrg.uv.addressmapper.streetAddress.StreetAddress;
import cz.cuni.mff.xrg.uv.addressmapper.streetAddress.StreetAddressParser;
import cz.cuni.mff.xrg.uv.addressmapper.streetAddress.WrongAddressFormatException;

/**
 * Set: 
 *  cisloDomovni
 *  cisloOrientancni
 *  ulice
 *  obec
 *
 * @author Škoda Petr
 */
public class StreetAddressMapper extends AbstractMapper {

    private final StreetAddressParser parser = new StreetAddressParser();

    private final KnowledgeBase knowledgeBase;

    private static final Logger LOG = LoggerFactory.getLogger(StreetAddressMapper.class);

    public StreetAddressMapper(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }
    
    @Override
    public List<RuianEntity> map(PostalAddress address, RuianEntity entity) throws KnowledgeBaseException {
        if (address.getStreetAddress() == null) {
            return Arrays.asList(new RuianEntity(entity));
        }
        // Try to parse given value.
        final StreetAddress streetAddress;
        try {
            streetAddress = parser.parse(address.getStreetAddress());
        } catch (WrongAddressFormatException ex) {
            final RuianEntity outputEntity = new RuianEntity(entity);
            // Add report about our failure.
            Report report = new Report(
                    AddressMapperOntology.MAPPER_STREET_ADDRESS,
                    String.format("Nepodařilo se naparsovat StreetAddress '%s' chyba: %s",
                            address.getStreetAddress(),
                            ex.getMessage()));
            outputEntity.getReports().add(report);
            // And return only input.
            return Arrays.asList(outputEntity);
        }

        final List<RuianEntity> output = new LinkedList<>();
        output.add(entity);
        // - - - - -
        if (streetAddress.getLandRegistryNumber() != null) {
            try {
                final Integer cisloDomovni = Integer.parseInt(streetAddress.getLandRegistryNumber().trim());
                for (RuianEntity item : output) {
                    item.setCisloDomovni(cisloDomovni);
                }
            } catch (NumberFormatException ex) {
                final Report report = new Report(
                        AddressMapperOntology.MAPPER_STREET_ADDRESS,
                        "Cislo domovni neni cislo.");
                addReport(output, report);
            }
        }
        // - - - - -
        if (streetAddress.getHouseNumber() != null) {
            // House number can contains multiple records separated with comma in such case we fail.
            String cisloOrientacni = streetAddress.getHouseNumber().replaceAll("\\s", "");
            if (cisloOrientacni.contains(",")) {
                final Report report = new Report(
                        AddressMapperOntology.MAPPER_STREET_ADDRESS,
                        "Cislo orientancni obsahuje vice hodnot.");
                addReport(output, report);
            } else {
                // 'cislo orientacni' can contains a letter
                String houseNumberLetter = null;
                final char lastChar = cisloOrientacni.charAt(cisloOrientacni.length() - 1);
                if (Character.isLetter(lastChar)) {
                    // Last character is letter, so read and remove it.
                    houseNumberLetter = "" + lastChar;
                    cisloOrientacni = cisloOrientacni.substring(0, cisloOrientacni.length() - 1);
                }
                // Parsse houseNumber as a integer.
                try {
                    final Integer cisloDomovni = Integer.parseInt(cisloOrientacni.trim());
                    for (RuianEntity item : output) {
                        item.setCisloOrientancni(cisloDomovni);
                        item.setCisloOrientancniPismeno(houseNumberLetter);
                    }
                } catch (NumberFormatException ex) {
                    final Report report = new Report(
                            AddressMapperOntology.MAPPER_STREET_ADDRESS,
                            "Cislo orientancni neni cislo.");
                    addReport(output, report);
                }
            }
        }
        // - - - - -

        // TODO Add alternatives?

        if (streetAddress.getStreetName() != null) {
            for (RuianEntity item : output) {
                item.setUlice(streetAddress.getStreetName());
            }
        }
        // - - - - -

        // TODO Add alternatives?

        if (streetAddress.getTownName() != null) {
            final String townName = streetAddress.getTownName();
            // Here a collision can occure as town is set also by other maper.
            final List<RuianEntity> newOutput = new LinkedList<>();
            for (RuianEntity item : output) {
                if (item.getObec() != null && item.getObec().compareTo(townName) != 0) {
                    // Town is alredy set and it's not same as we have -> add alternative.
                    final RuianEntity newEntity = new RuianEntity(item);
                    newEntity.setObec(townName);
                    // Add warning.
                    final Report report = new Report(
                            AddressMapperOntology.MAPPER_STREET_ADDRESS,
                            String.format("Puvodni hodnota obce '%s' nahrazena '%s'",
                                    item.getObec(),
                                    newEntity.getObec()));
                    newEntity.getReports().add(report);
                    // Add to results.
                    newOutput.add(newEntity);
                } else {
                    item.setObec(townName);
                }
            }
            // Merge results.
            output.addAll(newOutput);
        }
        return output;
    }

    /**
     * Add report to all values.
     *
     * @param entities
     * @param repot
     */
    protected void addReport(List<RuianEntity> entities, Report repot) {
        for (RuianEntity item : entities) {
            item.getReports().add(repot);
        }
    }

}
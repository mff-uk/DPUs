package cz.cuni.mff.xrg.uv.addressmapper.streetAddress;

/**
 * Insert space after dot.
 * 
 * @author Škoda Petr
 */
public class SpaceInsertFormatter implements Formatter {

    @Override
    public String format(String streetAddress) {
        return streetAddress.replaceAll("\\.([^\\s])", "\\. $1");
    }
    
}

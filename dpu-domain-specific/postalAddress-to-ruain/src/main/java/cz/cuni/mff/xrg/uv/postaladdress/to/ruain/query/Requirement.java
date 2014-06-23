package cz.cuni.mff.xrg.uv.postaladdress.to.ruain.query;

/**
 *
 * @author Škoda Petr
 */
public class Requirement {

    private final Subject subject;
    
    private final String predicate;
    
    private final String object;

    public Requirement(Subject subject, String predicate, String object) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }

    public Subject getSubject() {
        return subject;
    }

    public String getPredicate() {
        return predicate;
    }

    public String getObject() {
        return object;
    }
    
}

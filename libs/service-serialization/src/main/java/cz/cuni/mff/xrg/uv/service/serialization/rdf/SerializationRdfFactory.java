package cz.cuni.mff.xrg.uv.service.serialization.rdf;

/**
 * Factory to create instances of {@link SerializationRdf}.
 *
 * @author Škoda Petr
 */
public class SerializationRdfFactory {

    private SerializationRdfFactory() {
    }

    /**
     *
     * @param <T>
     * @param clazz
     * @return Class for very simple rdf serialisation.
     */
    public static <T> SerializationRdf<T> rdfSimple(Class<T> clazz) {
        return new SerializationRdfSimple<>();
    }

}

package cz.cuni.mff.xrg.uv.service.serialization.xml;

/**
 * Interface for xml serialisation interface.
 * 
 * @author Škoda Petr
 * @param <T>
 */
public interface SerializationXml<T> {

    /**
     * Create instance generic ConfigSerializer object. In case of error return
     * null.
     * 
     * @return Object instance or null.
     * @throws cz.cuni.mff.xrg.uv.service.serialization.xml.SerializationXmlFailure
     */
    public T createInstance() throws SerializationXmlFailure;

    public T convert(String string) throws SerializationXmlFailure;
    
    public String convert(T object) throws SerializationXmlFailure;
    
}

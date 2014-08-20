package cz.cuni.mff.xrg.uv.transformer.tabular.column;

import cz.cuni.mff.xrg.uv.transformer.tabular.Utils;
import cz.cuni.mff.xrg.uv.transformer.tabular.parser.ParseFailed;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Škoda Petr
 */
public abstract class ValueGeneratorReplace implements ValueGenerator {

    private abstract class Token {

        public abstract String process(List<Object> row);
              
    }

    /**
     * Return fixed string.
     */
    private class TokenString extends Token {

        private final String string;

        public TokenString(String string) {
            this.string = string;
        }

        @Override
        public String process(List<Object> row) {
            return string;
        }

    }

    /**
     * Return value from given index in data row.
     */
    private class TokenReplace extends Token {

        int index;

        public TokenReplace(int index) {
            this.index = index;
        }

        @Override
        public String process(List<Object> row) {
            final Object object = row.get(index);
            if (object == null) {
                return null;
            } else {
                return object.toString();
            }
        }

    }

    /**
     * As {@link TokenReplace} but also encode the value so it can be used in
     * URI.
     */
    private class TokenReplaceUri extends TokenReplace {

        public TokenReplaceUri(int index) {
            super(index);
        }

        @Override
        public String process(List<Object> row) {
            final String replaced = super.process(row);
            if (replaced == null) {
                return null;
            } else {
                return Utils.convertStringToURIPart(replaced);
            }
        }

    }

    private static final Logger LOG = LoggerFactory.getLogger(
            ValueGeneratorReplace.class);

    /**
     * Used URI for column.
     */
    private final URI uri;

    /**
     * Filed reference according to
     * http://w3c.github.io/csvw/csv2rdf/#dfn-field-reference.
     */
    private final String template;

    /**
     * Contains information how to construct
     */
    private final List<Token> tokens = new LinkedList<>();
    
    /**
     *
     * @param uri
     * @param template Replace template without type, language or \\ < > chars
     */
    protected ValueGeneratorReplace(URI uri, String template) {
        this.uri = uri;
        this.template = template;
    }

    @Override
    public void compile(Map<String, Integer> nameToIndex,
            ValueFactory valueFactory) throws ParseFailed {
        tokens.clear();

        // parse inner pattern
        String toParse = template;
        while (!toParse.isEmpty()) {
            int left = toParse.indexOf("{");
            int right = toParse.indexOf("}");

            if (left == -1 && right == -1) {
                tokens.add(new TokenString(toParse));
                break;
            }
            // there is { or }

            if (right == -1 || (left != -1 && left < right)) {
                // { -> string
                final String value = toParse.substring(0, left);
                toParse = toParse.substring(left + 1);
                //
                if (!value.isEmpty()) {
                    tokens.add(new TokenString(value));
                } else {
                    // it can be empty if for example
                    // string starts with { or there is }{ as substring
                }
            } else if (left == -1 || (right != -1 && right < left)) {
                // } --> name
                String name = toParse.substring(0, right);
                toParse = toParse.substring(right + 1);
                //
                boolean isUri = false;
                if (name.startsWith("+")) {
                    name = name.substring(1);
                    isUri = true;
                }
                // translate name to index
                final Integer nameIndex = nameToIndex.get(name);
                if (nameIndex == null) {
                    throw new ParseFailed("Unknown column name: " + name);
                }
                // create token reprezentaion
                if (isUri) {
                    tokens.add(new TokenReplaceUri(nameIndex));
                } else {
                    tokens.add(new TokenReplace(nameIndex));
                }
            } else {
                throw new ParseFailed("Failed to parse: " + template);
            }
        }
    }


    @Override
    public URI getUri() {
        return uri;
    }

    /**
     * Assemble value based on given {@link #template} and data.
     *
     * @param row
     * @return
     */
    protected String process(List<Object> row) {
        final StringBuilder result = new StringBuilder(20);
        for(Token token : tokens) {
            String newString = token.process(row);
            if (newString == null) {
                // if anyone return null, then we do not publish
                // TODO update according to http://w3c.github.io/csvw/csv2rdf/#
                return null;
            } else {
                result.append(newString);
            }
        }
        return result.toString();
    }

    /**
     * Create replace based {@link ValueGenerator}.
     * 
     * @param uri
     * @param template
     * @return
     * @throws ParseFailed
     */
    public static ValueGeneratorReplace create(URI uri, String template)
            throws ParseFailed {
        if (template.startsWith("\"")) {
            // string
            if (template.contains("\"@")) {
                // language tag
                return new ValueGeneratorString(uri,
                    template.substring(1, template.lastIndexOf("\"@")),
                        template.substring(template.lastIndexOf("\"@") + 2));
            }
            if (template.contains("\"^^")) {
                // type
                return new ValueGeneratorTyped(uri,
                    template.substring(1, template.lastIndexOf("\"^^")),
                        template.substring(template.lastIndexOf("\"^^") + 3));
            }
            // string without nothing
            return new ValueGeneratorString(uri,
                    template.substring(1, template.length() - 1), null);
        }
        if (template.startsWith("<")) {
            // uri
            return new ValueGeneratorUri(uri, 
                    template.substring(1, template.length() - 1));
        }
        throw new ParseFailed("Can't parse tempalte: " + template);
    }

}

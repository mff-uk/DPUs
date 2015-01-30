package cz.cuni.mff.xrg.uv.transformer;

import java.util.LinkedList;
import java.util.List;

/**
 * DPU's configuration class.
 *
 * @author Škoda Petr
 */
public class HtmlCssConfig_V1 {

    public enum ActionType {
        /**
         * Execute JSOUP query.
         *
         * Action data: JSOUP query
         */
        QUERY,
        /**
         * Extract content as string.
         *
         * Action data: none
         */
        TEXT,
        /**
         * Extract content as html string.
         *
         * Action data: none
         */
        HTML,
        /**
         * Select attribute of given name.
         *
         * Action data: Name of the attribute.
         */
        ATTRIBUTE,
        /**
         * Create statement with input value as an object, as a subject use given predicate
         * and as a subject used last defined subject.
         *
         * Action data: Used predicate.
         */
        OUTPUT, // LITERAL
        /**
         * Create new subject used by {@link #OUTPUT}. If has predicate is given, then create a new level.
         * Ie. create new subject, create statement: current subject, has predicate, new subject.
         * And set new subject as a current subject. If no predicate is given then just create a new subject
         * and preserve the parent. Subject class is not changed by this option.
         *
         * Action data: Has predicate.
         */
        SUBJECT,
        /**
         * Given list of elements put each element into separated group.
         *
         * Action data: none
         */
        UNLIST,
        /**
         * Set given URI as a subject class.
         *
         * Action data: subject class
         */
        SUBJECT_CLASS
    }

    public static class Action {

        /**
         * Name of action. This value is used to match named output on which this query is executed.
         */
        private String name = HtmlCss.SUBJECT_URI_TEMPLATE;

        /**
         * Determine type of an action.
         */
        private ActionType type = ActionType.TEXT;

        /**
         * Data for action, based on {@link #type}.
         */
        private String actionData = "";

        /**
         * Name out output, if any.
         */
        private String outputName = "";

        public Action() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ActionType getType() {
            return type;
        }

        public void setType(ActionType type) {
            this.type = type;
        }

        public String getActionData() {
            return actionData;
        }

        public void setActionData(String actionData) {
            this.actionData = actionData;
        }

        public String getOutputName() {
            return outputName;
        }

        public void setOutputName(String outputName) {
            this.outputName = outputName;
        }
        
    }

    private List<Action> actions = new LinkedList<>();

    /**
     * Can be null, in such case no value should be generated.
     */
    private String classAsStr = "http://unifiedviews.eu/ontology/e-htmlCss/Page";

    /**
     * Can be null, in such case no value is generated.
     */
    private String hasPredicateAsStr = "http://unifiedviews.eu/ontology/e-htmlCss/hasObject";

    /**
     * If true then generate triple with information about source file.
     */
    private boolean sourceInformation = false;

    public HtmlCssConfig_V1() {

    }

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    public String getClassAsStr() {
        return classAsStr;
    }

    public void setClassAsStr(String classAsStr) {
        this.classAsStr = classAsStr;
    }
    
    public String getHasPredicateAsStr() {
        return hasPredicateAsStr;
    }

    public void setHasPredicateAsStr(String hasPredicateAsStr) {
        this.hasPredicateAsStr = hasPredicateAsStr;
    }

    public boolean isSourceInformation() {
        return sourceInformation;
    }

    public void setSourceInformation(boolean sourceInformation) {
        this.sourceInformation = sourceInformation;
    }
    
}

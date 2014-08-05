package cz.cuni.mff.xrg.uv.transformer.tabular.mapper;

import cz.cuni.mff.xrg.uv.transformer.tabular.column.ColumnInfo_V1;
import java.util.Map;

/**
 *
 * @author Škoda Petr
 */
public class TableToRdfConfig {

    /**
     * Name of column with key, or null.
     */
    final String keyColumnName;

    /**
     * Base URI used to prefix generated URIs.
     */
    final String baseURI;

    /**
     * User configuration about parsing process.
     */
    final Map<String, ColumnInfo_V1> columnsInfo;

    /**
     * If true then new column, not specified in {@link #columnsInfo},
     * can be added.
     */
    final boolean generateNew;

    /**
     * Metadata for column - type.
     */
    final String rowsClass;

    final boolean ignoreBlankCells;

    public TableToRdfConfig(String keyColumnName, String baseURI,
            Map<String, ColumnInfo_V1> columnsInfo, boolean generateNew,
            String rowsClass, boolean ignoreBlankCells) {
        this.keyColumnName = keyColumnName;
        this.baseURI = baseURI;
        this.columnsInfo = columnsInfo;
        this.generateNew = generateNew;
        this.rowsClass = rowsClass;
        this.ignoreBlankCells = ignoreBlankCells;
    }

}

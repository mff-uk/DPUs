package cz.cuni.mff.xrg.uv.utils.dataunit.files;

import cz.cuni.mff.xrg.uv.utils.dataunit.metadata.Manipulator;
import eu.unifiedviews.dataunit.DataUnitException;
import eu.unifiedviews.dataunit.files.WritableFilesDataUnit;
import eu.unifiedviews.helpers.dataunit.virtualpathhelper.VirtualPathHelper;
import java.io.File;

/**
 *
 * @author Škoda Petr
 */
public class CreateFile {

    private CreateFile() {

    }

    /**
     * Create file of under given path and return {@link File} to it. Also add
     * {@link VirtualPathHelper#PREDICATE_VIRTUAL_PATH} metadata to the new
     * file.
     *
     * As this function create new connection is should not be used for 
     * greater number of files.
     *
     * @param dataUnit
     * @param virtualPath
     * @return
     * @throws DataUnitException
     */
    public static File createFile(WritableFilesDataUnit dataUnit,
            String virtualPath) throws DataUnitException {
        final File file = new File(
                java.net.URI.create(dataUnit.addNewFile(virtualPath)));
        Manipulator.add(dataUnit, virtualPath,
                VirtualPathHelper.PREDICATE_VIRTUAL_PATH,
                virtualPath);
        return file;
    }

}

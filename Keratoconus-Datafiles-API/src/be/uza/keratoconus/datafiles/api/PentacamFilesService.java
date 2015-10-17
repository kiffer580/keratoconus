package be.uza.keratoconus.datafiles.api;

import java.io.FileNotFoundException;
import java.util.List;

import aQute.bnd.annotation.ProviderType;

/**
 * Service which provides access to the totality of Pentacam files and the master file.
 * This service is only registered when all data files have been read in and the master file is consistent with the data files.
 * @author Chris Gray
 *
 */
@ProviderType
public interface PentacamFilesService {

	/**
	 * Get all PentacamFiles which are used by the application.
	 * @return
	 * @throws FileNotFoundException
	 */
	List<PentacamFile> getAllFiles() throws FileNotFoundException;

	/**
	 * Get a file by its base name, i.e. the name without the directory path and the .CSV extension.
	 * @param baseName
	 * @return
	 * @throws FileNotFoundException
	 */
	PentacamFile getFileByBaseName(String baseName)
			throws FileNotFoundException;
}

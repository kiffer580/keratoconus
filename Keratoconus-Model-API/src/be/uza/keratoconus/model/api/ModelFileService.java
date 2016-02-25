package be.uza.keratoconus.model.api;

import java.util.List;

import aQute.bnd.annotation.ProviderType;

/**
 * Service which knows about the data files used by the current classification model.
 * 
 * @author Chris Gray
 *
 */
@ProviderType
public interface ModelFileService {

	/**
	 * Base names of all the files to be used. They will be used in the order
	 * given, and the fields in the master file will be presented in the same
	 * order (after the common fields).
	 */
	String[] getFileBaseNames();

	/**
	 * Get the field separator used in CSV files with a given base name.
	 * 
	 * @param fbn
	 *            The file base name as it occurs in the output of
	 *            {@link #getFileBaseNames()}.
	 * @return the separator, usually ";" or ",".
	 */
	String getSeparatorForFile(String fbn);

	/**
	 * Get the fields of the CSV files with a given base name.
	 * 
	 * @param fbn
	 *            The file base name as it occurs in the output of
	 *            {@link #getFileBaseNames()}.
	 * @return the field names.
	 */
	List<String> getFieldsOfFile(String fbn);

}

package be.uza.keratoconus.model.api;

import java.io.IOException;
import java.util.List;

import aQute.bnd.annotation.ProviderType;

/**
 * Service which is used to interact with the WEKA-based model.
 * 
 * @author Chris Gray
 *
 */
@ProviderType
public interface ModelService {

	public static final String MODEL_NAME = "model.name";

	/**
	 * Names of the common fields which occur in several files (although not
	 * always in the same position). These fields will be presented in the
	 * master file in the order given (before the data fields).
	 */
	String[] getCommonFields();

	/**
	 * Names of the fields which are used internally to create a unique key.
	 * (Must be a subset of the common fields).
	 */
	String[] getKeyFields();

	/**
	 * Names of the fields which are used by the classification model, in the
	 * order in which it expects them (i.e. the order of fields in the training
	 * data).
	 */
	String[] getUsedFields();

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
	 * @return the field names, as a comma-separated list.
	 */
	String getFieldsOfFile(String fbn);

	/**
	 * Get the WEKA classifier which has been trained to this model.
	 * 
	 * @return
	 * @throws Exception
	 */
	weka.classifiers.functions.SMO getClassifier() throws Exception;
}

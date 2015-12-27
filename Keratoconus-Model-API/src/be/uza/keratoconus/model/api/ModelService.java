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

	/**
	 * Get the names of all available models in this installation.
	 */
	List<String> getAvailableModelNames();

	/**
	 * Select the model to be used. This should be called before any of the
	 * methods {@link #getCommonFields()}, {@link #getKeyFields()},
	 * {@link #getFileBaseNames()}, {@link #getFieldsOfFile(String)},
	 * {@link #getSeparatorForFile(String)} are called.
	 * 
	 * @param name
	 *            The name of the model (must be one of those returned by
	 *            {@link #getAvailableModelNames()}).
	 * @throws Exception 
	 */
	void selectModel(String name) throws Exception;

	/**
	 * Get the name of the currently selected model.
	 * @return
	 */
	String getSelectedModelName();

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

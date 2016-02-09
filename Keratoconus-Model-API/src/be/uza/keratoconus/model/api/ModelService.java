package be.uza.keratoconus.model.api;

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

	/**
	 * Get the name of the attribute which determines the classification of a
	 * patient examination. <br/>
	 * <b>N.B.</b> This information is obtained by using reflection to examine
	 * private fields of the serialized model. If for any reason this process
	 * fails then <code>null</code> will be returned, and the caller must be
	 * prepared to deal with this situation.
	 * 
	 * @return The name of the classification attribute, or <code>null</code> if
	 *         the introspection fails.
	 */
	String getClassAttributeName();

	/**
	 * Get the possible values of the attribute which determines the
	 * classification of a patient examination. <br/>
	 * <b>N.B.</b> This information is obtained by using reflection to examine
	 * private fields of the serialized model. If for any reason this process
	 * fails then <code>null</code> will be returned, and the caller must be
	 * prepared to deal with this situation.
	 * 
	 * @return A list of the possible values of the classification attribute, or
	 *         <code>null</code> if the introspection fails.
	 */
	List<String> getClassAttributeValues();

	/**
	 * Get the index of the attribute which determines the classification of a
	 * patient examination within the list returned by
	 * {@link #getAttributeNames()}. <br/>
	 * <b>N.B.</b> This information is obtained by using reflection to examine
	 * private fields of the serialized model. If for any reason this process
	 * fails then <code>null</code> will be returned, and the caller must be
	 * prepared to deal with this situation.
	 * 
	 * @return The index of the classification attribute, or <code>null</code>
	 *         if the introspection fails.
	 */
	int getClassAttributeIndex();

	/**
	 * Get the names of all the attributes which are used in the classification
	 * process, including the classification attribute itself (needed for
	 * verification purposes). <br/>
	 * <b>N.B.</b> This information is obtained by using reflection to examine
	 * private fields of the serialized model. If for any reason this process
	 * fails then <code>null</code> will be returned, and the caller must be
	 * prepared to deal with this situation.
	 * 
	 * @return A list containing the names of all the attributes, or
	 *         <code>null</code> if the introspection fails.
	 */
	List<String> getAttributeNames();
}

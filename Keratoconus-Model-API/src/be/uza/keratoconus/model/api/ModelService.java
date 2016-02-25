package be.uza.keratoconus.model.api;

import java.util.List;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.functions.SMO;
import weka.classifiers.SingleClassifierEnhancer;
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
	 * Get the (short) name of the model.
	 * @return
	 */
	String getModelName();

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
	 *            {@link ModelFileService#getFileBaseNames()}.
	 * @return the separator, usually ";" or ",".
	 */
	String getSeparatorForFile(String fbn);

	/**
	 * Get the fields of the CSV files with a given base name.
	 * 
	 * @param fbn
	 *            The file base name as it occurs in the output of
	 *            {@link ModelFileService#getFileBaseNames()}.
	 * @return the field names, as a comma-separated list.
	 */
	String getFieldsOfFile(String fbn);

	/**
	 * Get the WEKA classifier which has been trained to this model.
	 * 
	 * @return
	 * @throws Exception
	 */
	AbstractClassifier getClassifier() throws Exception;

	/**
	 * Get the SMO classifier. If {@link #getClassifier()} returns an object of
	 * type {@link SMO} then this method will return the same result, if
	 * {@link #getClassifier()} returns an object of type
	 * {@link SingleClassifierEnhancer} which wraps an SMO then this method will
	 * return the SMO. In all other cases an {@link IllegalAccessException} will
	 * be thrown.
	 * 
	 * @return the SMO.
	 * @throws Exception
	 *             if an error occurs during introspection of the classifier
	 *             object, or the classifier object is neither an {@link SMO}
	 *             nor a {@link SingleClassifierEnhancer} which wraps an
	 *             {@link SMO}.
	 */
	SMO getSMO() throws Exception;

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
	 * verification purposes). Note that the names are returned exactly as they
	 * are stored in the model; you may need to apply {@link stripJunk()} to
	 * obtain a normalized version.<br/>
	 * <b>N.B.</b> This information is obtained by using reflection to examine
	 * private fields of the serialized model. If for any reason this process
	 * fails then <code>null</code> will be returned, and the caller must be
	 * prepared to deal with this situation.
	 * 
	 * @return A list containing the names of all the attributes, or
	 *         <code>null</code> if the introspection fails.
	 */
	List<String> getAttributeNames();

	/**
	 * Transform a Pentacam field name into something human-readable by removing
	 * junk such as a trailing colon or embedded &ldquo;.-&rdquo;.
	 * 
	 * @param s
	 * @return
	 */
	String normalizeAttributeName(String s);
}

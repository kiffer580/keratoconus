/*
    This file is part of Keratoconus Assistant.

    Keratoconus Assistant is free software: you can redistribute it 
    and/or modify it under the terms of the GNU General Public License 
    as published by the Free Software Foundation, either version 3 of 
    the License, or (at your option) any later version.

    Keratoconus Assistant is distributed in the hope that it will be 
    useful, but WITHOUT ANY WARRANTY; without even the implied warranty 
    of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Keratoconus Assistant.  If not, see 
    <http://www.gnu.org/licenses/>.
 */

package be.uza.keratoconus.model.api;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.SingleClassifierEnhancer;
import weka.classifiers.functions.SMO;
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
	 * Transform a Pentacam field name into something human-readable by removing
	 * junk such as a trailing colon or embedded &ldquo;.-&rdquo;.
	 * 
	 * @param s
	 * @return
	 */
	String normalizeAttributeName(String s);
}

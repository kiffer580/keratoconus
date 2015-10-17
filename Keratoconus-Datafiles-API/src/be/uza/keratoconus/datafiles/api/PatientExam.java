package be.uza.keratoconus.datafiles.api;

import java.util.List;
import java.util.Map;

import aQute.bnd.annotation.ProviderType;

/**
 * A PatientExam object represents the data gathered during a single patient
 * examination (including the information identifying the patient).
 * 
 * @author Chris Gray
 *
 */
@ProviderType
public interface PatientExam {

	/**
	 * Add some fields to this exam (normal case).
	 * 
	 * @param baseName
	 * @param allFields
	 *            the complete list of fields in <code>record</code>.
	 * @param commonFields
	 *            the common fields relating to the exam results.
	 * @param usedFields
	 *            the new fields which must be added to the exam results.
	 * @param record
	 *            the values of all fields, in the same order as in
	 *            <code>allFields</code>.
	 */
	void addData(String baseName, List<PentacamField> allFields,
			List<PentacamField> commonFields, List<PentacamField> usedFields,
			String[] record);

	/**
	 * Add some fields to this exam (bifacial case).
	 * 
	 * @param baseName
	 * @param allFields
	 *            the complete list of fields in <code>record</code>.
	 * @param commonFields
	 *            the common fields relating to the exam results.
	 * @param usedFields
	 *            the fields which must be added to the exam results.
	 * @param frontRecord
	 *            the values of all fields for the FRONT (anterior) surface, in
	 *            the same order as in <code>allFields</code>.
	 * @param record
	 *            the values of all fields for the BACK (posterior) surface, in
	 *            the same order as in <code>allFields</code>.
	 */
	void addData(String baseName, List<PentacamField> allFields,
			List<PentacamField> commonFields, List<PentacamField> usedFields,
			String[] frontRecord, String[] backRecord);

	/**
	 * Get the data for this exam in the form of a map fieldname &rarr; value.
	 * 
	 * @return
	 */
	Map<String, String> getExamData();

}
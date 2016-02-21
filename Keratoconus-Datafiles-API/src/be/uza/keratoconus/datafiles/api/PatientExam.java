package be.uza.keratoconus.datafiles.api;

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
	 *            base name of the file from which these fields are taken (used
	 *            only for error reporting).
	 * @param commonFieldsMap
	 *            the fields which are common to several files, mapped to their
	 *            offset in the record.
	 * @param usedFieldsMap
	 *            the new fields which must be added to the exam results, mapped
	 *            to their offset in the record.
	 * @param record
	 *            the values of all fields, in the same order as in
	 *            <code>allFields</code>.
	 */
	void addData(String baseName, Map<PentacamField, Integer> commonFieldsMap,
			Map<PentacamField, Integer> usedFieldsMap, String[] record);

	/**
	 * Add some fields to this exam (bifacial case).
	 * 
	 * @param baseName
	 *            base name of the file from which these fields are taken (used
	 *            only for error reporting).
	 * @param commonFieldsMap
	 *            the new fields which are common to several files, mapped to
	 *            their offset in the record.
	 * @param usedFieldsMap
	 *            the new fields which must be added to the exam results, mapped
	 *            to their offset in the record.
	 * @param frontRecord
	 *            the values of all fields for the FRONT (anterior) surface, in
	 *            the same order as in <code>allFields</code>.
	 * @param record
	 *            the values of all fields for the BACK (posterior) surface, in
	 *            the same order as in <code>allFields</code>.
	 */
	void addData(String baseName, Map<PentacamField, Integer> commonFieldsMap,
			Map<PentacamField, Integer> usedFieldsMap, String[] frontRecord,
			String[] backRecord);

	/**
	 * Get the data for this exam in the form of a map fieldname &rarr; value.
	 * 
	 * @return
	 */
	Map<String, String> getExamData();

}
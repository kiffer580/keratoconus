package be.uza.keratoconus.datafiles.api;

import java.util.Map;

import aQute.bnd.annotation.ProviderType;

/**
 * The PatientExamService provides access to PatienExam records.
 * @author Chris Gray
 *
 */
@ProviderType
public interface PatientExamService {

	/**
	 * Create an empty PatientExam record with the given key.
	 * @param key
	 * @return
	 */
	PatientExam createPatientExamRecord(String key);

	/**
	 * Get all known PatientExam records.
	 * @return a map key &rarr; exam.
	 */
	Map<String, PatientExam> getAllPatientExamRecords();

}

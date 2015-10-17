package be.uza.keratoconus.analysis.api;

import java.util.Map;

import be.uza.keratoconus.datafiles.api.PatientExam;

/**
 * The Analyser takes a PatientExam and returns a probability distribution as a result.
 * @author Chris Gray
 *
 */
public interface Analyser {

	/**
	 * Feed a patient exam to the analyser.
	 * @param exam
	 */
	void processPatientExam(PatientExam exam);
	
	/**
	 * Obtain the results of the analysis.
	 * @return A map which has an entry [class &rarr; probability] for every class in the model.
	 * (The keys to this map are the same as are used in be.uza.keratoconus.configuration.api.Classification).
	 * @throws Exception
	 */
	Map<String, Double> getDistribution() throws Exception;

}

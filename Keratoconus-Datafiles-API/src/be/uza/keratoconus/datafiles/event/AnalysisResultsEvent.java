package be.uza.keratoconus.datafiles.event;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.service.event.Event;

import be.uza.keratoconus.configuration.api.Classification;
import be.uza.keratoconus.datafiles.api.PatientExam;

public class AnalysisResultsEvent extends Event {

	private Map<String, Double> distribution;

	public AnalysisResultsEvent(String headlineKey,
			Classification.Category headlineCategory,
			Map<String, Double> distribution, PatientExam examRecord) {
		super(AnalysisResultsEventConstants.ANALYSIS_RESULTS_TOPIC,
				toProperties(headlineKey, headlineCategory, distribution,
						examRecord, false));
		this.distribution = distribution;
	}

	/**
	 * Clone an event, optionally also setting the flag &ldquo;isRecall&rdquo;.
	 * 
	 * @param event
	 * @param isRecall
	 */
	public AnalysisResultsEvent(AnalysisResultsEvent event, boolean isRecall) {
		super(AnalysisResultsEventConstants.ANALYSIS_RESULTS_TOPIC,
				toProperties(event.getHeadlineKey(),
						event.getHeadlineCategory(), event.getDistribution(),
						event.getExam(), isRecall));
		distribution = event.distribution == null ? null : new HashMap<>(event.distribution);
	}

	/**
	 * Get the headline key, which can be used to look up the headline
	 * description text.
	 * 
	 * @return
	 */
	public String getHeadlineKey() {
		return (String) getProperty(AnalysisResultsEventConstants.HEADLINE_KEY);
	}

	/**
	 * Get the headline category (MAIN/SIDE/NORMAL/AMBIGUOUS/BAD).
	 * 
	 * @return
	 */
	public Classification.Category getHeadlineCategory() {
		return (Classification.Category) getProperty(AnalysisResultsEventConstants.HEADLINE_CATEGORY);
	}

	/**
	 * Get the probability distribution, as a mapping from indication key to
	 * probability.
	 * 
	 * @return the probability distribution, or <code>null</code> if no
	 *         information available (this will be the case iff
	 *         <code>getHeadlineCategory() == BAD</code>).
	 */
	public Map<String, Double> getDistribution() {
		return distribution;
	}

	/**
	 * Get the patient exam data on which the analysis was performed.
	 * 
	 * @return
	 */
	public PatientExam getExam() {
		return (PatientExam) getProperty(AnalysisResultsEventConstants.EXAM);
	}

	/**
	 * Detect whether this is the initial transmission of a result or an
	 * indication that the current result should be re-displayed.
	 * 
	 * @return <code>false</code> if this is the initial transmission of a
	 *         result, <code>true</code> if an indication that the current
	 *         result should be re-displayed.
	 */
	public boolean isRecall() {
		Object property = getProperty(AnalysisResultsEventConstants.IS_RECALL);
		return (property instanceof Boolean) && (Boolean) property;
	}

	public String toString() {
		StringBuilder properties = new StringBuilder();
		for (String name : getPropertyNames()) {
			properties.append(" ").append(name).append("=").append(getProperty(name));
		}
		return super.toString() + properties;
	}
	
	private static Dictionary<String, Object> toProperties(String headlineKey,
			Classification.Category headlineCategory,
			Map<String, Double> distribution, PatientExam examRecord,
			boolean isRecall) {
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(AnalysisResultsEventConstants.HEADLINE_KEY, headlineKey);
		properties.put(AnalysisResultsEventConstants.HEADLINE_CATEGORY,
				headlineCategory);
		if (distribution != null) {
			properties.put(AnalysisResultsEventConstants.DISTRIBUTION,
					distribution);
		}
		properties.put(AnalysisResultsEventConstants.EXAM, examRecord);
		properties.put(AnalysisResultsEventConstants.IS_RECALL, isRecall);
		return properties;
	}

}
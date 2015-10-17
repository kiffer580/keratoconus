package be.uza.keratoconus.analysis.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import be.uza.keratoconus.configuration.api.Classification.Category;
import be.uza.keratoconus.configuration.api.ClassificationService;
import be.uza.keratoconus.configuration.api.PentacamConfigurationService;
import be.uza.keratoconus.analysis.api.Analyser;
import be.uza.keratoconus.datafiles.event.AnalysisResultsEvent;
import be.uza.keratoconus.datafiles.api.PatientExam;
import be.uza.keratoconus.datafiles.api.PatientExamService;
import be.uza.keratoconus.datafiles.api.PentacamFile;
import be.uza.keratoconus.datafiles.api.PentacamFilesService;
import be.uza.keratoconus.datafiles.event.FileEvent;
import be.uza.keratoconus.datafiles.event.FileEventConstants;

@Component(immediate = true, properties = EventConstants.EVENT_TOPIC + "="
		+ FileEventConstants.DATAFILE_CREATED_TOPIC + ","
		+ EventConstants.EVENT_TOPIC + "="
		+ FileEventConstants.DATAFILE_CHANGED_TOPIC)
public class Collator implements EventHandler {

	private static final String FILENAME_SUFFIX_LOAD = "-LOAD";
	private PentacamConfigurationService pentacamConfigurationService;
	private PentacamFilesService pentacamFilesService;
	private EventAdmin eventAdmin;
	private PatientExamService patientExamService;
	private ClassificationService classificationService;
	private Analyser analyser;
	private List<PentacamFile> allFiles;
	private List<String> allFileKeys;

	// For each patient key, newDatafileRecordMap holds a mapping from file key
	// to the new (unprocessed) records for the patient in the file:
	// patient -> (file key -> array of (array of field values)).
	// In practice there will only be one unprocessed record per patient, or two
	// if the file is bifacial.
	private Map<String, Map<String, String[][]>> newDatafileRecordMap = new HashMap<String, Map<String, String[][]>>();

	private Map<PentacamFile, Long> savedLengths;

	private double headlineThreshold;
	private double thickCorneaThreshold;
	private LogService logService;

	@Reference
	protected void setPentacamConfigurationService(
			PentacamConfigurationService pcs) {
		pentacamConfigurationService = pcs;
	}

	@Reference
	protected void setPentacamFilesService(PentacamFilesService pfs) {
		pentacamFilesService = pfs;
	}

	@Reference
	protected void setClassificationService(ClassificationService cs) {
		classificationService = cs;
	}

	@Reference
	protected void setEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	@Reference
	protected void setPatientExamService(PatientExamService pes) {
		patientExamService = pes;
	}

	@Reference
	protected void setAnalyser(Analyser a) {
		analyser = a;
	}

	@Reference
	protected void setLogService(LogService ls) {
		logService = ls;
	}

	@Activate
	protected void activate() throws IOException {
		allFiles = pentacamFilesService.getAllFiles();
		allFileKeys = new ArrayList<>();
		for (PentacamFile pf : allFiles) {
			String fileKey = getFileKey(pf);
			if (!allFileKeys.contains(fileKey)) {
				allFileKeys.add(fileKey);
			}
		}
		savedLengths = new HashMap<>();
		for (PentacamFile pf : allFiles) {
			savedLengths.put(pf, pf.getCurrentLength());
		}
		headlineThreshold = pentacamConfigurationService.getHeadlineThreshold();
		thickCorneaThreshold = pentacamConfigurationService
				.getThickCorneaThreshold();
	}

	@Override
	public void handleEvent(Event event) {
		FileEvent dfce = (FileEvent) event;
		String fileName = dfce.getFileName();
		if (fileName.toUpperCase().endsWith(".CSV")) {
			String baseName = fileName.substring(0, fileName.length() - 4);
			handlePentacamFileEvent(baseName);
		}
	}

	private void handlePentacamFileEvent(String baseName) {
		try {
			final PentacamFile pf = pentacamFilesService
					.getFileByBaseName(baseName);
			if (pf == null) {
				return;
			}

			long oldLength = savedLengths.get(pf);
			final List<String[]> newRecords = pf.getNewRecords(oldLength);
			final int newRecordCount = newRecords.size();

			if (newRecordCount == 0) {
				return;
			}

			Map<String, String[][]> datafileRecords;
			String fileKey = getFileKey(pf);
			String patientKey;
			synchronized (newDatafileRecordMap) {
				if (pf.isBifacial()) {
					String[] firstRecord = newRecords.get(newRecordCount - 2);
					String[] secondRecord = newRecords.get(newRecordCount - 1);
					patientKey = pf.extractKey(firstRecord);
					datafileRecords = getNewDatafileRecordsFor(patientKey);
					String[][] recordPair = { firstRecord, secondRecord };
					datafileRecords.put(fileKey, recordPair);
				} else {
					int startPoint = newRecordCount - 1;
					String[] record = newRecords.get(startPoint);
					patientKey = pf.extractKey(record);
					datafileRecords = getNewDatafileRecordsFor(patientKey);
					String[][] singleRecord = { record };
					datafileRecords.put(fileKey, singleRecord);
				}
				if (isComplete(datafileRecords)) {
					collateRecords(patientKey, datafileRecords);
					newDatafileRecordMap.remove(patientKey);
				}
			}
		} catch (FileNotFoundException e) {
			logService.log(LogService.LOG_WARNING,
					"Received file event for non-existent file", e);
		}
	}

	private String getFileKey(final PentacamFile pf) {
		String fileKey = pf.getBaseName();
		int baseNameLength = fileKey.length();
		if (baseNameLength > 5
				&& fileKey.substring(baseNameLength - 5).equalsIgnoreCase(
						FILENAME_SUFFIX_LOAD)) {
			fileKey = fileKey.substring(0, baseNameLength - 5);
		}
		return fileKey;
	}

	private Map<String, String[][]> getNewDatafileRecordsFor(String patientKey) {
		Map<String, String[][]> datafileRecords = newDatafileRecordMap
				.get(patientKey);
		if (datafileRecords == null) {
			datafileRecords = new HashMap<String, String[][]>();
			newDatafileRecordMap.put(patientKey, datafileRecords);
		}
		return datafileRecords;
	}

	private boolean isComplete(Map<String, String[][]> datafileRecords) {
		return datafileRecords.keySet().containsAll(allFileKeys);
	}

	private void collateRecords(String patientKey,
			Map<String, String[][]> datafileRecords)
			throws FileNotFoundException {
		PatientExam examRecord = patientExamService
				.createPatientExamRecord(patientKey);
		for (String fileKey : allFileKeys) {
			final String[][] records = datafileRecords.get(fileKey);
			PentacamFile pf;
			try {
				pf = pentacamFilesService.getFileByBaseName(fileKey
						+ FILENAME_SUFFIX_LOAD);
			} catch (FileNotFoundException e) {
				pf = pentacamFilesService.getFileByBaseName(fileKey
						+ FILENAME_SUFFIX_LOAD);
			}
			if (pf.isBifacial()) {
				examRecord.addData(pf.getBaseName(), pf.getAllFields(),
						pf.getCommonFields(), pf.getUsedFields(), records[0],
						records[1]);
			} else {
				examRecord.addData(pf.getBaseName(), pf.getAllFields(),
						pf.getCommonFields(), pf.getUsedFields(), records[0]);
			}
		}

		analyser.processPatientExam(examRecord);
		Map<String, Double> distribution;
		try {
			distribution = analyser.getDistribution();
			String headlineKey = determineHeadlineKey(distribution, examRecord);
			Category headlineCategory = classificationService.getByKey(
					headlineKey).getCategory();
			eventAdmin.postEvent(new AnalysisResultsEvent(headlineKey,
					headlineCategory, headlineCategory == Category.BAD ? null
							: distribution, examRecord));
		} catch (Exception e) {
			logService
					.log(LogService.LOG_WARNING,
							"An exception was thrown while analysing the patient exam record",
							e);
		}
	}

	private String determineHeadlineKey(Map<String, Double> distribution,
			PatientExam examRecord) {
		final Map<String, String> examData = examRecord.getExamData();
		final String errorString = examData.get("Error");
		String result = checkAgainstThreshold(errorString, 1,
				ClassificationService.UNRELIABLE);
		if (result != null) {
			return result;
		}

		String pachyString = examData.get("Pachy Apex");
		result = checkAgainstThreshold(pachyString, thickCorneaThreshold,
				ClassificationService.THICK);
		if (result != null) {
			return result;
		}

		String mostLikelyKey = null;
		double mostLikelyValue = 0.0D;
		for (Map.Entry<String, Double> entry : distribution.entrySet()) {
			if (entry.getValue() > mostLikelyValue) {
				mostLikelyValue = entry.getValue();
				mostLikelyKey = entry.getKey();
			}
		}
		result = checkAgainstThreshold(mostLikelyValue, headlineThreshold,
				mostLikelyKey);
		if (result != null) {
			return result;
		}
		
		return ClassificationService.AMBIGUOUS;
	}

	private String checkAgainstThreshold(double level, double threshold,
			String over) {
				if (level > threshold) {
					return over;
				}
		return null;
	}

	private String checkAgainstThreshold(String string, double threshold,
			String over) {
		if (string != null) {
			try {
				double level = Double.parseDouble(string);
				return checkAgainstThreshold(level, threshold, over);
			} catch (Exception e) {
			}
		}
		return null;
	}
}

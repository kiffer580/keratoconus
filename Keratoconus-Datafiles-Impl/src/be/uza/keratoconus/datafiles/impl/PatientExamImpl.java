package be.uza.keratoconus.datafiles.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import be.uza.keratoconus.datafiles.api.PatientExam;
import be.uza.keratoconus.datafiles.api.PentacamField;

/**
 * Container for the results of one examination of one eye of one patient on one
 * occasion.
 * 
 * @author Chris Gray
 *
 */
public class PatientExamImpl implements PatientExam {

	private final Map<String, String> examData = new LinkedHashMap<>();
	private PatientExamServiceImpl patientExamServiceImpl;

	public PatientExamImpl(PatientExamServiceImpl patientExamServiceImpl) {
		this.patientExamServiceImpl = patientExamServiceImpl;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * be.kiffer.uza.keratoconus.datafiles.PatientExam#addData(java.lang.String,
	 * java.util.List, java.util.List, java.util.List, java.lang.String[])
	 */
	@Override
	public void addData(String baseName, List<PentacamField> allFields,
			List<PentacamField> commonFields, List<PentacamField> usedFields,
			String[] record) {
		handleCommonFields(allFields, commonFields, record);
		for (final PentacamField field : usedFields) {
			// TODO make the mapping field -> index in record once, at startup
			String name = field.getName();
			int i = allFields.indexOf(field);
			if (i < 0) {
				fieldNotFoundError(baseName, name);
				return;
			}
			String value = cleanUp(record[i]);
			String existing = examData.put(name, value);
			if (existing != null) {
				duplicateFieldWarning(baseName, name);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * be.kiffer.uza.keratoconus.datafiles.PatientExam#addData(java.lang.String,
	 * java.util.List, java.util.List, java.util.List, java.lang.String[],
	 * java.lang.String[])
	 */
	@Override
	public void addData(String baseName, List<PentacamField> allFields,
			List<PentacamField> commonFields, List<PentacamField> usedFields,
			String[] frontRecord, String[] backRecord) {
		handleCommonFields(allFields, commonFields, frontRecord);
		for (final PentacamField field : usedFields) {
			// TODO make the mapping field -> index in record once, at startup
			String name = field.getName();
			int i = allFields.indexOf(field);
			if (i < 0) {
				fieldNotFoundError(baseName, name);
				return;
			}
			String frontValue = cleanUp(frontRecord[i]);
			String backValue = cleanUp(backRecord[i]);
			String existing;
			if (field.isBifacial()) {
				existing = examData.put(name + " FRONT", frontValue);
				if (existing != null) {
					duplicateFieldWarning(baseName, name);
				}
				existing = examData.put(name + " BACK", backValue);
				if (existing != null) {
					duplicateFieldWarning(baseName, name);
				}
			} else {
				existing = examData.put(name, frontValue);
				if (existing != null) {
					duplicateFieldWarning(baseName, name);
				}
			}
		}
	}

	private void duplicateFieldWarning(String baseName, String name) {
		patientExamServiceImpl.warn("Field {1} of {0} has the same name as an existing field", baseName, name);
	}

	private void fieldNotFoundError(String baseName, String name) {
		patientExamServiceImpl.error("Field {1} of {0} was not found in the file headers", baseName, name);
	}

	private String cleanUp(final String raw) {
		final String trimmed = raw.trim();
		if (trimmed.isEmpty() || "NA".equals(trimmed) || "NaN".equals(trimmed)
				|| "-".equals(trimmed)) {
			// Missing value
			return "?";
		}
		char firstChar = trimmed.charAt(0);
		if (Character.isDigit(firstChar)
				|| (firstChar == '+' || firstChar == '-')
				&& Character.isDigit(trimmed.charAt(1))) {
			// Numeric value
			return trimmed.replace(',', '.');
		}
		// Else pass through
		return trimmed;
	}

	private void handleCommonFields(List<PentacamField> allFields,
			List<PentacamField> commonFields, String[] record) {
		for (final PentacamField field : commonFields) {
			String name = field.getName();
			int i = allFields.indexOf(field);
			String value = record[i];
			String existing = examData.get(name);
			if (existing == null) {
				examData.put(name, value);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see be.kiffer.uza.keratoconus.datafiles.PatientExam#getExamData()
	 */
	@Override
	public Map<String, String> getExamData() {
		return examData;
	}

	public String toString() {
		return "PatientExam " + examData;
	}
}

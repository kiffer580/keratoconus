package be.uza.keratoconus.analysis.impl;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import be.uza.keratoconus.analysis.api.Analyser;
import be.uza.keratoconus.datafiles.api.PatientExam;
import be.uza.keratoconus.model.api.ModelService;

@Component(enabled = true)
public class PreTrainedModel implements Analyser {

private static final String SEMICOLON = ";";

	private weka.classifiers.functions.SMO classifier;
	private ModelService classificationModelService;
	private LogService logService;
	
	private Map<String, String> examData;
	private Instance instance;
	private String[] classAttributeNames;

	private ComponentContext ownComponentContext;

	@Reference
	protected void setClassificationModelService(
			ModelService cms) {
		classificationModelService = cms;
	}

	@Reference
	protected void setLogService(
			LogService ls) {
		logService = ls;
	}

	@Activate
	public void activate(ComponentContext cc) throws Exception {
		this.ownComponentContext = cc;
		classifier = classificationModelService.getClassifier();
		classAttributeNames = classifier.classAttributeNames();
	}

	@Override
	public void processPatientExam(PatientExam exam) {
		examData = exam.getExamData();
		String headerLine = "";
		String dataLine = "";
		int nColumns = 0;
		for (String fieldName: classificationModelService.getUsedFields()) {
			if (examData.containsKey(fieldName)) {
				headerLine += fieldName + SEMICOLON;
				final String fieldValue = examData.get(fieldName);
				// TODO fatal error if fieldValue is null?
				dataLine += fieldValue + SEMICOLON;
				++nColumns;
			}
			else if (examData.containsKey(fieldName + " FRONT")) {
				headerLine += fieldName + " FRONT" + SEMICOLON;
				final String frontFieldValue = examData.get(fieldName + " FRONT");
				// TODO fatal error if fieldValue is null?
				dataLine += frontFieldValue + SEMICOLON;
				++nColumns;
				headerLine += fieldName + " BACK" + SEMICOLON;
				final String backFieldValue = examData.get(fieldName + " BACK");
				// TODO fatal error if fieldValue is null?
				dataLine += backFieldValue + SEMICOLON;
				++nColumns;
			}
		}

		String csv = headerLine + "Class\n"
				+ dataLine + "?\n";
		CSVLoader csvLoader = new CSVLoader();
		csvLoader.setFieldSeparator(SEMICOLON);
		try {
			csvLoader.setSource(new ByteArrayInputStream(csv
					.getBytes(Charset.forName("windows-1252"))));
			final Instances dataSet = csvLoader.getDataSet();
			dataSet.setClassIndex(nColumns);
			instance = dataSet.get(0);
		} catch (Exception e) {
			logService.log(ownComponentContext.getServiceReference(), LogService.LOG_WARNING, "Exception thrown when reading CSV record", e);
		}
	}
	
	@Override
	public Map<String, Double> getDistribution() throws Exception {
		if (instance == null) {
			throw new IllegalStateException("no exam data is available");
		}
		double[] distribution = classifier.distributionForInstance(instance);
		Map<String, Double> result = new HashMap<String, Double>();
		for (int i = 0; i < distribution.length; ++i) {
			result.put(classAttributeNames[i], distribution[i]);
		}
		return result;
	}

}

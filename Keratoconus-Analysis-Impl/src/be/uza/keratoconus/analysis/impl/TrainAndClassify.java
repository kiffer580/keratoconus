package be.uza.keratoconus.analysis.impl;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import weka.classifiers.functions.SMO;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import be.uza.keratoconus.configuration.api.PentacamConfigurationService;
import be.uza.keratoconus.analysis.api.Analyser;
import be.uza.keratoconus.datafiles.api.PatientExam;

@Component(enabled = false)
public class TrainAndClassify implements Analyser {

	private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
	private static final boolean BUILD_LOGISTIC_MODELS = true;
	private static final String TRAINING_SET_PATH = "c:/Users/Chris Gray/UZA/5-groups-training-20150601.arff";
	private static final String SEMICOLON = ";";

	private static weka.classifiers.functions.SMO classifier;
	private static Instances trainData;
	private PentacamConfigurationService pentacamConfigurationService;
	private Map<String, String> examData;
	private Instance instance;
	private int cIdx;
	private String[] classAttributeNames;

	@Reference
	protected void setPentacamConfigurationService(
			PentacamConfigurationService pcs) {
		pentacamConfigurationService = pcs;
	}

	@Activate
	public void activate() throws Exception {
		classifier = new SMO();
		classifier.setBuildLogisticModels(BUILD_LOGISTIC_MODELS);
		trainData = new Instances(new FileReader(TRAINING_SET_PATH));
		cIdx = trainData.numAttributes() - 1;
		trainData.setClassIndex(cIdx);
		classifier.buildClassifier(trainData);
		weka.core.SerializationHelper.write(new FileOutputStream("foo"), classifier);
		classAttributeNames = classifier.classAttributeNames();
		// TODO check all have been assigned
	}

	@Override
	public void processPatientExam(PatientExam exam) {
		examData = exam.getExamData();
		String headerLine = "";
		String dataLine = "";
		for (Map.Entry<String, String> entry : examData.entrySet()) {
			String attributeName = entry.getKey();
			String attributeValue = entry.getValue();
			if (!Arrays.asList(pentacamConfigurationService.getCommonFields())
					.contains(attributeName) && !"Surface".equals(attributeName)) {
				headerLine += attributeName + SEMICOLON;
				dataLine += attributeValue + SEMICOLON;
			}
		}
		String csv = headerLine + "Class\n"
				+ dataLine + "?\n";
//		System.out.println("Passing CSV to classifier:\n" + csv);
		CSVLoader csvLoader = new CSVLoader();
		csvLoader.setFieldSeparator(SEMICOLON);
		try {
			csvLoader.setSource(new ByteArrayInputStream(csv
					.getBytes(WINDOWS_1252)));
			instance = csvLoader.getDataSet().get(0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

package be.uza.keratoconus.systemtrayapp;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Reference;
import be.uza.keratoconus.configuration.api.Classification;
import be.uza.keratoconus.configuration.api.ClassificationService;
import be.uza.keratoconus.configuration.api.PentacamConfigurationService;
import be.uza.keratoconus.datafiles.event.AnalysisResultsEvent;
import be.uza.keratoconus.datafiles.event.AnalysisResultsEventConstants;
import be.uza.keratoconus.model.api.AvailableModelsService;
import be.uza.keratoconus.model.api.ModelService;

@Component(immediate = true, configurationPolicy = ConfigurationPolicy.ignore, properties = EventConstants.EVENT_TOPIC
		+ "=" + AnalysisResultsEventConstants.ANALYSIS_RESULTS_TOPIC)
public class CsvPatientRecord implements EventHandler {

	private static final String CSV_PATIENT_RECORD_FILE_NAME = "Keratoconus-Assistant.CSV";
	private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

	private LogService logService;
	private ModelService modelService;
	private PentacamConfigurationService pentacamConfigurationService;
	private ClassificationService classificationService;
	private AvailableModelsService availableModelsService;

	@Reference
	protected void setLogService(LogService ls) {
		logService = ls;
	}

	@Reference
	protected void setAvailableModelsService(AvailableModelsService ams) {
		this.availableModelsService = ams;
	}

	@Reference
	protected void setModelService(ModelService ms) {
		this.modelService = ms;
	}

	@Reference
	protected void setPentacamConfigurationService(
			PentacamConfigurationService pcs) {
		this.pentacamConfigurationService = pcs;
	}

	@Reference
	protected void setClassificationService(ClassificationService cs) {
		this.classificationService = cs;
	}

	@Override
	public void handleEvent(Event event) {
		AnalysisResultsEvent analysisResultsEvent = (AnalysisResultsEvent) event;
		if (analysisResultsEvent.isRecall()) {
			return;
		}

		Map<String, String> examData = analysisResultsEvent.getExam()
				.getExamData();
		final Path directoryPath = pentacamConfigurationService
				.getPentacamDirectoryPath();
		final Path filePath = directoryPath
				.resolve(CSV_PATIENT_RECORD_FILE_NAME);
		boolean fileAlreadyExisted = Files.exists(filePath);
		try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(
				filePath, WINDOWS_1252, StandardOpenOption.CREATE,
				StandardOpenOption.APPEND))) {
			if (!fileAlreadyExisted) {
				logService.log(LogService.LOG_INFO, "CsvPatientRecord: file "
						+ filePath + " did not yet exist, writing header");
				writeCsvHeader(writer);
			}
			logService.log(LogService.LOG_INFO,
					"CsvPatientRecord: appending record to " + filePath);
			writeCsvRecord(writer, examData,
					analysisResultsEvent.getHeadlineKey(),
					analysisResultsEvent.getDistribution());
		} catch (IOException e) {
			logService.log(LogService.LOG_WARNING,
					"Failed to append patient record to " + filePath, e);
		}
	}

	private void writeCsvHeader(PrintWriter writer) {
		for (String fieldName : modelService.getCommonFields()) {
			writer.print(fieldName + ";");
		}
		writer.print("Exam Eye;");
		writer.print("Indication;");
		for (String key : classificationService.keys()) {
			if (classificationIsReallyAnIndication(key)) {
				writer.print("p(" + classificationService.getHeadline(key)
						+ ");");
			}
		}
		writer.print("Model used;");
		writer.println();
	}

	private void writeCsvRecord(PrintWriter writer,
			Map<String, String> examData, String headlineKey,
			Map<String, Double> distribution) {
		for (String fieldName : modelService.getCommonFields()) {
			writer.print(examData.get(fieldName) + ";");
		}
		writer.print(examData.get("Exam Eye") + ";");
		writer.print(classificationService.getHeadline(headlineKey) + ";");
		if (distribution != null) {
			for (String key : classificationService.keys()) {
				if (classificationIsReallyAnIndication(key)) {
					Double p = distribution.get(key);
					writer.printf("%.2f", p == null ? 0.0D : p);
					writer.print(";");
				}
			}
		}
		writer.println(availableModelsService.getSelectedModelName());
		writer.println();
	}

	private boolean classificationIsReallyAnIndication(String key) {
		if (Classification.AMBIGUOUS.equals(key)) {
			return false;
		}
		Classification classification = classificationService.getByKey(key);
		if (classification == null || classification.isNormal()
				|| classification.isUnreliable()) {
			return false;
		}
		return true;
	}
}

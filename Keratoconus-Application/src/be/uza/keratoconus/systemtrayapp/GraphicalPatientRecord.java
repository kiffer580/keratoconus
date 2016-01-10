package be.uza.keratoconus.systemtrayapp;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.SnapshotResult;
import javafx.scene.image.WritableImage;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;
import be.uza.keratoconus.configuration.api.Classification;
import be.uza.keratoconus.configuration.api.Classification.Category;
import be.uza.keratoconus.configuration.api.ClassificationService;
import be.uza.keratoconus.datafiles.event.AnalysisResultsEvent;
import be.uza.keratoconus.datafiles.event.AnalysisResultsEventConstants;
import be.uza.keratoconus.userprefs.api.UserPreferences;

@Component(immediate = true, configurationPolicy = ConfigurationPolicy.require, properties = EventConstants.EVENT_TOPIC
		+ "=" + AnalysisResultsEventConstants.ANALYSIS_RESULTS_TOPIC)
public class GraphicalPatientRecord implements EventHandler {

	private static final String INDICATION = "Indication";

	private static final String PROBABILITIES = "Probabilities";

	private static final String STATUS = "Status";

	private static final String MODEL = "Model";

	private static final String[] ERROR_TEXT = { "", "Warning", "Error" };

	@OCD
	interface Config {
		@AD(required = false, deflt = "800")
		int patient_record_width();
		
		@AD(required = false, deflt = "0")
		int patient_record_height();
		
		@AD(required = false, deflt = "dd/MM/yy hh:mm:ss")
		String patient_record_date_format();
		
		@AD(required = false, deflt = "/css/graphical-patient-record.css")
		String patient_record_stylesheet();
	}
	
	private Config config;
	private ClassificationService classificationService;
	private LogService logService;
	private UserPreferences guiConfig;
	private List<String[]> rowHeaders;
	private DateFormat dateIn;
	private DateFormat dateOut;
	private String styleSheet;

	private double offscreenX;

	private double offscreenY;

	private int record_width;

	private int record_height;

	@Reference
	protected void setClassificationService(ClassificationService cs) {
		classificationService = cs;
	}

	@Reference
	protected void setLogService(LogService ls) {
		logService = ls;
	}

	@Reference
	protected void setGuiConfig(UserPreferences gc) {
		this.guiConfig = gc;
	}

	@Activate
	protected void activate(Map<String, Object> params)
			throws FileNotFoundException {
		config = Configurable.createConfigurable(Config.class, params);
		rowHeaders = extractRowHeaders(params);
		record_width = config.patient_record_width();
		record_height = config.patient_record_height();
		String dateFormatString = config.patient_record_date_format();
		dateIn = dateFormatString == null ? DateFormat.getInstance()
				: new SimpleDateFormat(dateFormatString);
		dateOut = new SimpleDateFormat("ddMMyyyy_hhmmss");
		readStyleSheet(config.patient_record_stylesheet());
		calculateOffscreenCoordinates();
	}

	private List<String[]> extractRowHeaders(Map<String, Object> params) {
		List<String[]> headerLines = new ArrayList<>();
		int i = 1;
		String line = (String) params.get("patient.record.row." + i++);
		while (line != null) {
			headerLines.add(line.split(";"));
			line = (String) params.get("patient.record.row." + i++);
		}
		return headerLines;
	}

	private void readStyleSheet(String path) throws FileNotFoundException {
		String relativePath = path.startsWith("/") ? path : "/" + path;
		final String styleFileName = System.getProperty("user.dir").replace(
				'\\', '/')
				+ relativePath;
		try (Scanner s = new Scanner(new File(styleFileName))) {
			styleSheet = s.useDelimiter("\\Z").next();
		}
	}

	private void calculateOffscreenCoordinates() {
		ObservableList<Screen> screens = Screen.getScreens();
		double maxX = 0.0D;
		double maxY = 0.0D;
		for (Screen s : screens) {
			Rectangle2D bounds = s.getBounds();
			maxX = Math.max(maxX, bounds.getMaxX());
			maxY = Math.max(maxY, bounds.getMaxY());
		}
		offscreenX = maxX + 1;
		offscreenY = maxY + 1;
	}

	@Deactivate
	protected void deactivate() {
	}

	@Override
	public void handleEvent(final Event event) {
		// start a new thread so that the pop-up window doesn't have to wait for us
		new Thread(()->handleAnalysisResultsEvent((AnalysisResultsEvent) event)).start();
	}

	private void handleAnalysisResultsEvent(AnalysisResultsEvent event) {
		if (!event.isRecall() && guiConfig.isPatientRecordEnabled()) {
			final String headlineKey = event.getHeadlineKey();
			final Map<String, String> examData = event.getExam().getExamData();
			final Map<String, Double> distribution = event.getDistribution();
			// Here beginneth an absolute orgy of reactive programming.
			// First we ask JavaFX to run the createRecord method in its own
			// thread.
			Platform.runLater(() -> {
				try {
					createRecord(headlineKey, examData, distribution);
				} catch (ParseException pe) {
					logService
							.log(LogService.LOG_WARNING,
									"Unable to parse exam date in order to create graphical patient record file",
									pe);
				}
			});
		}
	}

	private void createRecord(String headlineKey, Map<String, String> examData,
			Map<String, Double> distribution) throws ParseException {
		final String fileName = generateFileName(examData);
		final WebView webView = new WebView();
		webView.setPrefSize(record_width, record_height);
		final WebEngine webEngine = webView.getEngine();
		final Stage gprStage = new Stage();
		gprStage.initStyle(StageStyle.UTILITY);
		gprStage.setTitle("Keratoconus Graphical Customer Record");
		// Set a listener on the webEngine which will run once the document has
		// been loaded
		webEngine
				.getLoadWorker()
				.stateProperty()
				.addListener(
						(prop, old, newval) -> documentIsLoaded(fileName,
								webView, gprStage));
		// Now create the content and tell the webEngine to load it
		webEngine
				.loadContent(createContent(headlineKey, examData, distribution));
		final Scene scene = new Scene(webView);
		gprStage.setScene(scene);
		gprStage.sizeToScene();
		gprStage.setX(offscreenX);
		gprStage.setY(offscreenY);
		gprStage.show();
	}

	private String createContent(String headlineKey,
			Map<String, String> examData, Map<String, Double> distribution) {
		Classification classification = classificationService
				.getByKey(headlineKey);
		final int errorLevel = Integer.parseInt(examData.get("Error"));
		final boolean suppressDistribution = classification.getCategory() == Category.BAD;
		final boolean suppressErrorDescription = errorLevel == 0;

		String head = wrap("Patient Record", "title") + "\n"
				+ wrap(styleSheet, "style");
		String tableRows = "";
		int ncols = 0;
		for (String[] rh : rowHeaders) {
			String thead = "";
			String tbody = "";
			ncols = Math.max(ncols, rh.length);
			int col = 0;
			for (String h : rh) {
				String value;
				if (MODEL.equals(h)) {
					value = guiConfig.getSelectedModelName().trim();
				} else if (INDICATION.equals(h)) {
					value = classificationService.getHeadline(headlineKey);
				} else {
					value = examData.get(h);
				}

				if (STATUS.equals(h) && !suppressErrorDescription) {
					thead += wrap(ERROR_TEXT[errorLevel], "td") + "\n";
					tbody += wrap(value.trim(), "td") + "\n";
				} else if (MODEL.equals(h)) {
					thead += wrap("Model Used", "td") + "\n";
					tbody += wrap(value.trim(), "td") + "\n";
				} else if (INDICATION.equals(h)) {
					thead += wrap(h, "td") + "\n";
					tbody += wrap(value.trim(), "td") + "\n";
				} else if (PROBABILITIES.equals(h)) {
					thead += wrap(h, "td") + "\n";
					if (suppressDistribution) {
						tbody += wrap("Not applicable", "td") + "\n";
					} else {
						tbody += wrap(presentDistribution(distribution), "td",
								"colspan=" + (ncols - col)) + "\n";
					}
				} else if (value != null && !value.trim().isEmpty()) {
					thead += wrap(h, "td") + "\n";
					tbody += wrap(value.trim(), "td") + "\n";
				}
				++col;
			}
			tableRows += wrap(wrap(thead, "tr"), "thead") + "\n"
					+ wrap(wrap(tbody, "tr"), "tbody");
		}
		String body = wrap(wrap(tableRows, "table"), "div", "id=\"container\"");
		String html = wrap(wrap(head, "head") + "\n" + wrap(body, "body"),
				"html");

		return html;
	}

	private void documentIsLoaded(String fileName, WebView webView,
			Stage gprStage) {
		// The document has been loaded, but it is not yet rendered!
		// Wait for the 'document' property to change before taking the snapshot
		webView.getEngine()
				.documentProperty()
				.addListener(
						(prop, oldDoc, newDoc) -> takeSnapshot(fileName,
								webView, gprStage));
	}

	private void takeSnapshot(String fileName, WebView webView, Stage gprStage) {
				// Now it is finally safe to ask the WebViewer to take a
				// snapshot of itself.
				Platform.runLater(() -> webView.snapshot(
						new Callback<SnapshotResult, Void>() {
				
							@Override
							public Void call(SnapshotResult snapshotResult) {
								Platform.runLater(() -> saveSnapshot(snapshotResult,
										fileName, gprStage));
								return null;
							}
				
						}, null, null));
	}

	private void saveSnapshot(SnapshotResult snapshotResult, String fileName,
			Stage gprStage) {
		// The snapshot has been taken, now comes the painful process of
		// converting it to a JPEG. (If you thought that bit was going to be
		// easy, think again!)
		try {
			String patientRecordDirectory = guiConfig
					.getPatientRecordDirectory();
			if (patientRecordDirectory == null
					|| patientRecordDirectory.isEmpty()) {
				logService
						.log(LogService.LOG_INFO,
								"No patient record directory is defined, not creating a patient record image.");
				return;
			}
			final File file = new File(patientRecordDirectory, fileName);
			final WritableImage image = snapshotResult.getImage();
			final BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image,
					null);
			// Remove alpha-channel from buffered image.
			BufferedImage imageRGB = new BufferedImage(
					bufferedImage.getWidth(), bufferedImage.getHeight(),
					BufferedImage.OPAQUE);
			Graphics2D graphics = imageRGB.createGraphics();
			graphics.drawImage(bufferedImage, 0, 0, null);

			ImageWriter jpegWriter = ImageIO
					.getImageWritersByFormatName("JPEG").next();
			ImageWriteParam jpegWriteParam = jpegWriter.getDefaultWriteParam();
			jpegWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			jpegWriteParam.setCompressionQuality(0.9f);

			try (FileImageOutputStream outputStream = new FileImageOutputStream(
					file)) {
				jpegWriter.setOutput(outputStream);
				IIOImage outputImage = new IIOImage(imageRGB, null, null);
				jpegWriter.write(null, outputImage, jpegWriteParam);
			}
			graphics.dispose();
		} catch (IOException e) {
			logService
					.log(LogService.LOG_WARNING,
							"VisiblePatientRecord: exception thrown while saving graphic",
							e);
		} finally {
			// Phew. Now just dispose of the stage and we can go home.
			Platform.runLater(() -> gprStage.hide());
		}
	}

	private String generateFileName(Map<String, String> examData)
			throws ParseException {
		final String rawDate = examData.get("Exam Date") + " "
				+ examData.get("Exam Time");
		final Date examDate = dateIn.parse(rawDate);
		final String date = dateOut.format(examDate);
		final String eye = examData.get("Exam Eye").startsWith("L") ? "OS"
				: "OD";
		final String fileName = sanitise(examData.get("Last Name")) + "_"
				+ sanitise(examData.get("First Name")) + "_" + eye + "_" + date
				+ "_KeratoconusAssistant.JPG";
		return fileName;
	}

	private String sanitise(String string) {
		StringBuilder sb = new StringBuilder(string);
		for (int i = 0; i < sb.length(); ++i) {
			char c = sb.charAt(i);
			if ("<>:\"/\\|?*".indexOf(c) >= 0) {
				sb.setCharAt(i, '-');
			}
		}
		return sb.toString();
	}

	private String presentDistribution(Map<String, Double> distribution) {
		String result = "";
		for (Map.Entry<String, Double> entry : distribution.entrySet()) {
			double value = entry.getValue();
			if (value > 0.025D) {
				String key = entry.getKey();
				String headline = classificationService.getHeadline(key);
				result += headline + ": " + Math.round(value * 100D) + ", ";
			}
		}
		return result.substring(0, result.length() - 2);
	}

	private String wrap(String content, String tag) {
		return "<" + tag + ">\n" + indent(content) + "\n</" + tag + ">";
	}

	private String wrap(String content, String tag, String param) {
		return "<" + tag + " " + param + ">\n" + indent(content) + "\n</" + tag
				+ ">";
	}

	private String indent(String content) {
		return "  " + content.replace("\n", "\n  ");
	}
}

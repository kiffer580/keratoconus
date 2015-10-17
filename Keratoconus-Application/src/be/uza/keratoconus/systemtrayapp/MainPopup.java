package be.uza.keratoconus.systemtrayapp;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Screen;
import javafx.stage.Stage;

import javax.imageio.ImageIO;

import org.osgi.service.event.EventConstants;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;
import be.uza.keratoconus.configuration.api.Classification;
import be.uza.keratoconus.configuration.api.Classification.Category;
import be.uza.keratoconus.configuration.api.ClassificationService;
import be.uza.keratoconus.configuration.api.PentacamConfigurationService;
import be.uza.keratoconus.datafiles.event.AnalysisResultsEvent;
import be.uza.keratoconus.datafiles.event.AnalysisResultsEventConstants;
import be.uza.keratoconus.datafiles.event.FileCreatedEvent;
import be.uza.keratoconus.datafiles.event.FileEvent;
import be.uza.keratoconus.datafiles.event.FileEventConstants;
import be.uza.keratoconus.userprefs.api.UserPreferences;
import be.uza.keratoconus.userprefs.api.UserPreferences.ChartType;

@Component(immediate = true, configurationPolicy = ConfigurationPolicy.require, properties = EventConstants.EVENT_TOPIC
		+ "="
		+ AnalysisResultsEventConstants.ANALYSIS_RESULTS_TOPIC
		+ ","
		+ EventConstants.EVENT_TOPIC
		+ "="
		+ FileEventConstants.DATAFILE_CREATED_TOPIC
		+ ","
		+ EventConstants.EVENT_TOPIC
		+ "="
		+ FileEventConstants.DATAFILE_CHANGED_TOPIC)
public class MainPopup implements org.osgi.service.event.EventHandler {

	private static final double STAGE_CLEARANCE = 10.0D;

	private static final String SEMICOLON = ";";

	protected static final int VBOX_PADDING = 0;

	private static final String EXAM_DATA_FIELD_STATUS = "Status";

	private static final String EXAM_DATA_FIELD_ERROR = "Error";

	private static final String OTHER = "Other";

	private static final String DETAIL_BUTTON_STRING = "?";

	private static final String PENTACAM = "PENTACAM";

	private static class Delta {
		private double x;
		private double y;
	}

	@OCD
	interface Config {
		@AD(required = false, deflt = "Strong indication of ")
		String prefix_probable();

		@AD(required = false, deflt = "Weak indication of ")
		String prefix_possible();

		@AD(required = false, deflt = "30")
		int display_time_seconds();

		@AD(required = false, deflt = "UNDECORATED")
		String window_stage_style();

		@AD(required = false, deflt = "BOTTOM_RIGHT")
		String window_position();

		@AD(required = false, deflt = "0.9")
		double threshold_explanation();

		@AD(required = false, deflt = "fx-font-size: 8pt; -fx-background-color: white; -fx-text-fill: black;")
		String style_identity();

		@AD(required = false, deflt = "-fx-background-color: red;")
		String style_status_error();

		@AD(required = false, deflt = "-fx-background-color: yellow;")
		String style_status_warning();

		@AD(required = false, deflt = "icon/warning.png")
		String icon_path_warning();

		@AD(required = false, deflt = "icon/error.png")
		String icon_path_error();

		@AD(required = false, deflt = "bar")
		String detail_chart_type();

		@AD(required = false, deflt = "-fx-padding: 2.5px 5px 5px 5px;")
		String style_detail_chart_base();

		@AD(required = false, deflt = "300")
		int window_preferred_width();

		@AD(required = false, deflt = "260")
		double detail_chart_width();

		@AD(required = false, deflt = "No patient data available")
		String text_no_data();
	}

	private enum Error {
		NONE(0), WARNING(1), ERROR(2);
		private static Error[] allValues = values();

		private final int level;

		private Error(int level) {
			this.level = level;
		}

		public int getLevel() {
			return level;
		}

		public static Error fromInt(int n) {
			return allValues[n];
		}
	};

	private PentacamConfigurationService pentacamConfigurationService;
	private ClassificationService classificationService;

	private Stage stage;

	private StackPane mainPane;

	final Delta dragDelta = new Delta();

	final AtomicReference<Thread> timerThreadRef = new AtomicReference<>();

	private Config nodeConfig;

	private Map<String, String> classDescriptions;
	private Map<String, String> chartColours;
	private List<String> identityFormatLines;
	private List<String> identityFieldNames;
	private String[] statusPaneStyleClass = new String[3];
	private String[] statusIconPath = new String[3];
	private String detailChartStyleBase;
	private double headlineThreshold;
	private double explanationThreshold;
	private double windowPreferredWidth;
	private double chartWidth;
	private UserPreferences userPreferences;

	private double savedX;

	private double savedY;

	private boolean chartShowing;

	private LogService logService;

	@Reference
	protected void setPentacamConfigurationService(
			PentacamConfigurationService pcs) {
		this.pentacamConfigurationService = pcs;
	}

	@Reference
	protected void setClassificationService(ClassificationService cs) {
		this.classificationService = cs;
	}

	@Reference
	protected void setUserPreferences(UserPreferences up) {
		this.userPreferences = up;
	}

	@Reference
	protected void setLogService(LogService ls) {
		this.logService = ls;
	}

	@Activate
	protected void activate(Map<String, Object> params) {
		nodeConfig = Configurable.createConfigurable(Config.class, params);
		userPreferences.setDefaultDisplayTimeSeconds(nodeConfig
				.display_time_seconds());
		userPreferences.setDefaultWindowStageStyle(nodeConfig
				.window_stage_style());
		userPreferences.setDefaultWindowPosition(nodeConfig.window_position());
		userPreferences.setDefaultDetailChartType(nodeConfig
				.detail_chart_type());
		headlineThreshold = pentacamConfigurationService.getHeadlineThreshold();
		parseParameters(params);
		mainPane = new StackPane();

		Platform.runLater(this::setUpStage);
	}

	private void parseParameters(Map<String, Object> params) {
		identityFormatLines = new ArrayList<>();
		identityFieldNames = new ArrayList<>();
		for (int identityLineNo = 1;; ++identityLineNo) {
			final String identityLine = (String) params.get("identity.fields."
					+ identityLineNo);
			if (identityLine == null)
				break;
			int leftCurly = identityLine.indexOf('{');
			int rightCurly = -1;
			final StringBuilder formatStringBuilder = new StringBuilder();
			while (leftCurly >= 0) {
				formatStringBuilder.append(identityLine.substring(
						rightCurly + 1, leftCurly));
				rightCurly = identityLine.indexOf('}', leftCurly + 1);
				if (rightCurly < 0) {
					logService.log(LogService.LOG_ERROR,
							"MainPopup: fatal error when reading configuration: Missing right curly in '"
									+ identityLine
									+ "' after left curly in column "
									+ leftCurly);
					break;
				}
				formatStringBuilder.append('{');
				formatStringBuilder.append(identityFieldNames.size());
				formatStringBuilder.append('}');
				final String fieldName = identityLine.substring(leftCurly + 1,
						rightCurly);
				identityFieldNames.add(fieldName);
				leftCurly = identityLine.indexOf('{', rightCurly + 1);
			}
			identityFormatLines.add(formatStringBuilder.toString());
		}
		statusPaneStyleClass[1] = "popup-status-warning";
		statusPaneStyleClass[2] = "popup-status-error";
		statusIconPath[1] = nodeConfig.icon_path_warning();
		statusIconPath[2] = nodeConfig.icon_path_error();
		detailChartStyleBase = fixBaseStyle(nodeConfig
				.style_detail_chart_base());

		explanationThreshold = nodeConfig.threshold_explanation();
		windowPreferredWidth = nodeConfig.window_preferred_width();
		chartWidth = nodeConfig.detail_chart_width();

		for (Category cat : Category.values()) {
			final String catName = cat.getName();
			final String displayTimeString = (String) params
					.get("display.time.seconds." + catName);
			if (displayTimeString != null) {
				double seconds = Double.parseDouble(displayTimeString);
				userPreferences.configureDisplayTimeSeconds(cat, seconds);
			}
		}

		classDescriptions = new HashMap<String, String>();
		chartColours = new HashMap<String, String>();
		chartColours.put(OTHER,
				(String) params.get("detail.chart.color." + OTHER));
		for (String key : classificationService.keys()) {

			final String description = (String) params
					.get("description." + key);
			if (description != null) {
				classDescriptions.put(key, description);
			}
			final String chartColour = (String) params
					.get("detail.chart.color." + key);
			if (chartColour != null) {
				chartColours.put(key, chartColour);
			}
		}
	}

	private String fixBaseStyle(String style) {
		if (style == null) {
			return "";
		}
		if (!style.trim().isEmpty() && !style.trim().endsWith(SEMICOLON)) {
			return style + SEMICOLON;
		}
		return style;
	}

	private void setUpStage() {
		stage = new Stage();
		stage.initStyle(userPreferences.getMainPopupStageStyle());
		String iconPath = userPreferences.getIconPath("large");
		try {
			setIcon(iconPath);
		} catch (IOException e1) {
			try {
				iconPath = userPreferences.getBaseIconPath();
				setIcon(iconPath);
			} catch (IOException e2) {
				logService.log(LogService.LOG_ERROR,
						"Exception thrown when setting main pop-up icon", e2);
			}
		}

		mainPane.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				// record a delta distance for the drag and drop operation.
				dragDelta.x = stage.getX() - mouseEvent.getScreenX();
				dragDelta.y = stage.getY() - mouseEvent.getScreenY();
			}
		});
		mainPane.setOnMouseDragged(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				savedX = mouseEvent.getScreenX() + dragDelta.x;
				stage.setX(savedX);
				savedY = mouseEvent.getScreenY() + dragDelta.y;
				stage.setY(savedY);
			}
		});

		stage.setTitle(pentacamConfigurationService.getApplicationTitle());
		final Scene scene = new Scene(mainPane);
		scene.setFill(null);
		stage.setScene(scene);
	}

	private void setIcon(String path) throws IOException {
		final BufferedImage img = ImageIO.read(new File(path));
		stage.getIcons().add(SwingFXUtils.toFXImage(img, null));
	}

	public void showResults() {
		Platform.runLater(() -> {
			stage.show();
			stage.setAlwaysOnTop(true);
		});
	}

	public void hideResults() {
		Platform.runLater(() -> {
			stage.hide();
			stage.setAlwaysOnTop(false);
		});
	}

	private void buildResults(String headlineKey, Category headlineCategory,
			Map<String, Double> distribution, Map<String, String> examData) {
		Map<String, Double> sorted = sortDistribution(distribution);
		String mainPaneStyle = "-fx-background-color: transparent;";

		Platform.runLater(() -> {
			mainPane.getChildren().clear();
			mainPane.setStyle(mainPaneStyle);
			
			VBox mainBox = new VBox(VBOX_PADDING);
			mainBox.getStylesheets().clear();
			mainBox.getStylesheets().add("file:css/popup.css");
			mainBox.getStyleClass().add("popup-root");
			mainBox.getStyleClass().add("popup-root-" + headlineKey);
			for (String attribute : classificationService.getByKey(headlineKey)
					.getAttributes()) {
				mainBox.getStyleClass().add("popup-root-" + attribute);
			}
			mainPane.setPrefWidth(windowPreferredWidth);
			mainBox.setOnMouseClicked(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent mouseEvent) {
					if (mouseEvent.isStillSincePress()) {
						hideResults();
					}
				}
			});

			final ObservableList<Node> children = mainBox.getChildren();
			mainPane.getChildren().add(mainBox);
			Pane idPane = createIdentityPane(examData);
			// idPane will be null if no identification lines are configured
			if (idPane != null) {
				children.add(idPane);
			}

			if (isErrorOrWarning(examData)) {
				final Error error = Error.fromInt(Integer.parseInt(examData
						.get(EXAM_DATA_FIELD_ERROR)));
				final String status = examData.get(EXAM_DATA_FIELD_STATUS);
				Pane statusBox = createStatusPane(error, status);
				children.add(statusBox);
			}
			if (isError(examData)) {
				// omit indications pane in this case
			} else if (sorted == null) {
				Pane specialIndicationPane = createSpecialIndicationPane(examData);
				Pane specialExplanationPane = createSpecialExplanationPane(examData);
				children.addAll(specialIndicationPane, specialExplanationPane);
			} else {
				Double mostLikelyValue = sorted.get(headlineKey);
				Pane headlinePane = createHeadlinePane(headlineCategory,
						headlineKey, mostLikelyValue);
				children.add(headlinePane);
				if (headlineCategory != Category.NORMAL) {
					Pane detailPane = createDetailPane(sorted);
					Pane explanationPane = createExplanationPane(sorted,
							detailPane);
					children.addAll(explanationPane, detailPane);
				}
			}
			// positionWindow();
		});
	}

	private boolean isError(Map<String, String> examData) {
		return Integer.parseInt(examData.get(EXAM_DATA_FIELD_ERROR)) > 1;
	}

	private boolean isErrorOrWarning(final Map<String, String> examData) {
		return Integer.parseInt(examData.get(EXAM_DATA_FIELD_ERROR)) > 0;
	}

	private boolean isDragged() {
		return dragDelta.x != 0D || dragDelta.y != 0D;
	}

	private void positionWindow() {
		stage.sizeToScene();
		Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
		double xLeft = STAGE_CLEARANCE;
		double xRight = primScreenBounds.getWidth() - stage.getWidth()
				- STAGE_CLEARANCE;
		double xMiddle = xRight * 0.5D;
		double yTop = STAGE_CLEARANCE;
		double yBottom = primScreenBounds.getHeight() - stage.getHeight()
				- STAGE_CLEARANCE;
		double yMiddle = yBottom * 0.5D;
		Pos windowPosition = userPreferences.getMainPopupPosition();
		switch (windowPosition) {
		case TOP_LEFT:
			savedX = xLeft;
			savedY = yTop;
			break;
		case TOP_CENTER:
			savedX = xMiddle;
			savedY = yTop;
			break;
		case TOP_RIGHT:
			savedX = xRight;
			savedY = yTop;
			break;
		case CENTER_LEFT:
			savedX = xLeft;
			savedY = yMiddle;
			break;
		case CENTER:
			savedX = xMiddle;
			savedY = yMiddle;
			break;
		case CENTER_RIGHT:
			savedX = xRight;
			savedY = yMiddle;
			break;
		case BOTTOM_LEFT:
			savedX = xLeft;
			savedY = yBottom;
			break;
		case BOTTOM_CENTER:
			savedX = xMiddle;
			savedY = yBottom;
			break;
		case BOTTOM_RIGHT:
		default:
			savedX = xRight;
			savedY = yBottom;
		}
		stage.setX(savedX);
		stage.setY(savedY);
	}

	private void clearDrag() {
		dragDelta.x = 0D;
		dragDelta.y = 0D;
	}

	private void restoreWindowPosition() {
		Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
		savedX = Math.min(savedX,
				primScreenBounds.getWidth() - stage.getWidth()
						- STAGE_CLEARANCE);
		savedX = Math.max(savedX, STAGE_CLEARANCE);
		savedY = Math.min(savedY,
				primScreenBounds.getHeight() - stage.getHeight()
						- STAGE_CLEARANCE);
		savedY = Math.max(savedY, STAGE_CLEARANCE);
		stage.setX(savedX);
		stage.setY(savedY);
	}

	private void adaptWindowPosition(double oldHeight, double newHeight) {
		Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
		double minY = primScreenBounds.getMinY();
		double maxY = primScreenBounds.getMaxY();
		double fromScreenTopToStageTop = stage.getY() - minY;
		double screenHeight = maxY - minY;
		double factor = fromScreenTopToStageTop / (screenHeight - oldHeight);

		// We round the new Y value because otherwise JavaFX/Windows will
		// silently truncate it
		savedY = Math.round(fromScreenTopToStageTop + (oldHeight - newHeight)
				* factor + minY);

		savedX = Math.min(stage.getX(),
				primScreenBounds.getWidth() - stage.getWidth()
						- STAGE_CLEARANCE);
		savedX = Math.max(savedX, STAGE_CLEARANCE);
		savedY = Math.min(savedY,
				primScreenBounds.getHeight() - stage.getHeight()
						- STAGE_CLEARANCE);
		savedY = Math.max(savedY, STAGE_CLEARANCE);

		stage.setX(savedX);
		stage.setY(savedY);
	}

	private void positionMouse(Button b) throws AWTException {
		final Scene scene = b.getScene();
		final Point2D windowCoord = new Point2D(scene.getWindow().getX(), scene
				.getWindow().getY());
		final Point2D sceneCoord = new Point2D(scene.getX(), scene.getY());
		final Point2D nodeCoord = b.localToScene(b.getWidth() * 0.5D,
				b.getHeight() * 0.5D);
		final int x = (int) Math.round(windowCoord.getX() + sceneCoord.getX()
				+ nodeCoord.getX());
		final int y = (int) Math.round(windowCoord.getY() + sceneCoord.getY()
				+ nodeCoord.getY());
		new Robot().mouseMove(x, y);
	}

	private Map<String, Double> sortDistribution(
			Map<String, Double> distribution) {
		if (distribution == null) {
			return null;
		}

		ArrayList<Entry<String, Double>> entries = new ArrayList<Map.Entry<String, Double>>(
				distribution.entrySet());
		Collections.sort(entries, new Comparator<Entry<String, Double>>() {
			public int compare(Entry<String, Double> obj1,
					Entry<String, Double> obj2) {
				return ((Entry<String, Double>) (obj2)).getValue().compareTo(
						((Entry<String, Double>) (obj1)).getValue());
			}
		});
		Map<String, Double> sorted = new LinkedHashMap<>();
		for (Entry<String, Double> entry : entries) {
			sorted.put(entry.getKey(), entry.getValue());
		}
		return sorted;
	}

	private Pane createIdentityPane(Map<String, String> examData) {
		if (identityFormatLines.size() == 0) {
			return null;
		}

		Object[] identityFieldValues = new Object[identityFieldNames.size()];
		int i = 0;
		for (String fieldName : identityFieldNames) {
			identityFieldValues[i++] = examData.get(fieldName);
		}
		StringBuilder labelBuilder = new StringBuilder();
		for (String format : identityFormatLines) {
			labelBuilder.append('\n');
			labelBuilder.append(MessageFormat.format(format,
					identityFieldValues));
		}
		final Label identityLabel = new Label(labelBuilder.substring(1));
		identityLabel.getStyleClass().add("popup-identity-label");
		HBox idPane = new HBox(identityLabel);
		idPane.getStyleClass().add("popup-identity");
		HBox.setHgrow(identityLabel, Priority.ALWAYS);
		return idPane;
	}

	private Pane createStatusPane(Error error, String status) {
		HBox statusBox = new HBox();
		statusBox.setAlignment(Pos.CENTER_LEFT);
		final ObservableList<Node> statusBoxChildren = statusBox.getChildren();
		int level = error.getLevel();
		String iconPath = statusIconPath[level];
//		if (!iconPath.startsWith("/")) {
//			iconPath = "/" + iconPath;
//		}
//		Image img = new Image(getClass().getResourceAsStream(iconPath));
		Image img = new Image("file:" + iconPath);
		statusBoxChildren.add(new ImageView(img));

		final Label statusLabel = new Label(status);
		statusLabel.setTextAlignment(TextAlignment.CENTER);
		statusLabel.getStyleClass().add("popup-status-label");

		statusBoxChildren.add(statusLabel);
		statusBox.getStyleClass().add(statusPaneStyleClass[level]);
		return statusBox;
	}

	private Pane createHeadlinePane(Category headlineCategory,
			String mostLikelyKey, Double mostLikelyValue) {
		final Text headlineText = new Text(getClassHeadline(mostLikelyKey)
				.toUpperCase());
		headlineText.getStyleClass().add("popup-headline");
		headlineText.getStyleClass().add("popup-headline-" + mostLikelyKey);
		headlineText.getStyleClass().add(
				"popup-headline-" + headlineCategory.getName());
		StackPane headlinePane = new StackPane(headlineText);
		headlinePane.getStyleClass().add("popup-headline");
		headlinePane.getStyleClass().add("popup-headline-" + mostLikelyKey);
		headlinePane.getStyleClass().add(
				"popup-headline-" + headlineCategory.getName());

		StackPane.setAlignment(headlineText, Pos.CENTER_LEFT);
		return headlinePane;
	}

	private Pane createExplanationPane(Map<String, Double> distribution,
			Pane detailPane) {
		String explanation = "";
		double explained = 0.0D;
		for (Map.Entry<String, Double> entry : distribution.entrySet()) {
			String key = entry.getKey();
			Classification classification = classificationService.getByKey(key);
			String description = classDescriptions.get(key);
			double value = entry.getValue();
			explained += Math.round(value * 100D) * 0.01D;
			if (!classification.isNormal()) {
				explanation += (value > headlineThreshold) ? nodeConfig
						.prefix_probable() : nodeConfig.prefix_possible();
				explanation += description == null ? key : description;
				explanation += ".  ";
			}
			if (explained > explanationThreshold) {
				break;
			}

		}
		final Label expLabel = new Label(explanation);
		// expLabel.setMaxWidth(explanationWidth);
		expLabel.getStyleClass().add("popup-explanation-label");
		Button detailButton = createDetailButton(detailPane);
		StackPane.setAlignment(detailButton, Pos.BOTTOM_RIGHT);
		StackPane expPane = new StackPane(expLabel, detailButton);
		StackPane.setAlignment(expLabel, Pos.CENTER_LEFT);
		expPane.getStyleClass().add("popup-explanation");
		Platform.runLater(() -> {
			// get JavaFX to calculate the button dimensions
			expPane.applyCss();
			expPane.layout();
			detailButton.prefWidthProperty()
					.bind(detailButton.heightProperty());
		});
		if (chartShowing) {
			showChart(detailPane, detailButton);
		}

		return expPane;
	}

	private Button createDetailButton(Pane detailPane) {
		Button detailButton = new Button(DETAIL_BUTTON_STRING);
		detailButton.getStyleClass().add("popup-detail-button");
		detailButton.getStyleClass().add("popup-detail-button-closed");
		detailButton.setTooltip(new Tooltip("Click for more details"));
		detailButton.setOnMouseClicked(event -> showChart(detailPane,
				detailButton));
		return detailButton;
	}

	private Pane createDetailPane(Map<String, Double> distribution) {
		double done = 0.0D;
		final ArrayList<String> usedKeys = new ArrayList<String>();
		for (Entry<String, Double> entry : distribution.entrySet()) {
			final String key = entry.getKey();
			final Double value = entry.getValue();
			usedKeys.add(key);
			done += value;
			if (done > explanationThreshold)
				break;
		}

		if (userPreferences.getDetailChartType() == ChartType.PIE) {
			return createPieChart(distribution, usedKeys, done);
		}
		return createBarChart(distribution, usedKeys, done);

	}

	private HBox createBarChart(Map<String, Double> distribution,
			final ArrayList<String> usedKeys, double done) {
		HBox chartPane = new HBox();
		chartPane.getStyleClass().add("popup-detail-chart");
		chartPane.managedProperty().bind(chartPane.visibleProperty());
		chartPane.setVisible(false);
		for (String key : usedKeys) {
			final Double value = distribution.get(key);
			Label l = createBar(key, value);
			chartPane.getChildren().add(l);
		}
		if (done < 0.95D) {
			Label l = createBar(OTHER, 1.0D - done);
			chartPane.getChildren().add(l);
		}
		return chartPane;
	}

	private Label createBar(String key, final Double value) {
		final String barStyle = detailChartStyleBase + "-fx-background-color: "
				+ getSegmentColour(key);
		String barText = Math.round(value * 100D) + "%";
		if (!OTHER.equals(key) || value > 0.1D) {
			barText = key + " " + barText;
		}
		Label bar = new Label(barText);
		Tooltip tip = new Tooltip(getClassHeadline(key) + " "
				+ Math.round(value * 100D) + "%");
		bar.setAlignment(Pos.CENTER);
		bar.setPrefSize(chartWidth * value, 25);
		bar.setStyle(barStyle);
		tip.setStyle(barStyle);
		bar.setTooltip(tip);
		return bar;
	}

	private Pane createPieChart(Map<String, Double> distribution,
			final ArrayList<String> usedKeys, double done) {
		Group chartGroup = new Group();
		double start = 0;
		double radius = chartWidth * 0.25D;
		for (String key : usedKeys) {
			final Double value = distribution.get(key);
			Arc a = createArc(key, start, value, radius);
			start += value;
			chartGroup.getChildren().add(a);
		}
		Arc a = createArc(OTHER, start, 1.0D - start, radius);
		chartGroup.getChildren().add(a);
		start = 0.0D;
		for (String key : usedKeys) {
			final Double value = distribution.get(key);
			Text t = createArcText(key, start, value, radius);
			start += value;
			chartGroup.getChildren().add(t);
		}
		Text t = createArcText(OTHER, start, 1.0D - start, radius);
		chartGroup.getChildren().add(t);
		StackPane chartPane = new StackPane(chartGroup);
		chartPane.getStyleClass().add("popup-detail-chart");
		chartPane.managedProperty().bind(chartPane.visibleProperty());
		chartPane.setVisible(false);
		return chartPane;
	}

	private Arc createArc(String key, double start, double value, double radius) {
		final String arcStyle = detailChartStyleBase + "-fx-fill: "
				+ getSegmentColour(key);
		Arc arc = new Arc(0D, 0D, radius, radius, start * 360D, value * 360D);
		arc.setType(ArcType.ROUND);
		arc.setStyle(arcStyle);
		return arc;
	}

	private Text createArcText(String key, double start, double value,
			double radius) {
		double midpoint = start + 0.5D * value;
		int sector = (int) (midpoint * 4D);
		double sine = Math.sin(midpoint * Math.PI * 2.0D);
		double cosine = Math.cos(midpoint * Math.PI * 2.0D);
		double x = cosine * radius;
		double y = sine * -radius;
		Text text = new Text(getClassHeadline(key) + " "
				+ Math.round(value * 100D) + "%");
		double w = text.getBoundsInLocal().getWidth();
		double h = text.getBoundsInLocal().getHeight();

		switch (sector % 4) {
		case 0:
			text.relocate(x, y - h * sine);
			break;
		case 1:
			text.relocate(x - w, y - h * sine);
			break;
		case 2:
			text.relocate(x - w, y);
			break;
		case 3:
			text.relocate(x, y);
			break;
		}

		return text;
	}

	private String getSegmentColour(String key) {
		String arcStyle = chartColours.get(key);
		if (arcStyle == null) {
			arcStyle = "gray";
		}
		return arcStyle;
	}

	private String getClassHeadline(String key) {
		final String headline = classificationService.getHeadline(key);
		return headline == null ? key : headline;
	}

	private void showChart(Pane detailPane, Button b) {
		chartShowing = true;
		double oldHeight = stage.getHeight();
		Platform.runLater(() -> {
			b.setOnMouseClicked(event -> hideChart(detailPane, b));
			// b.setStyle(detailButtonStyleOpen);
			b.getStyleClass().remove("popup-detail-button-open");
			b.getStyleClass().remove("popup-detail-button-closed");
			b.getStyleClass().add("popup-detail-button-open");
			b.setTooltip(new Tooltip("Click to hide details"));
			detailPane.setVisible(true);
			stage.sizeToScene();
			double newHeight = stage.getHeight();
			adaptWindowPosition(oldHeight, newHeight);
			try {
				positionMouse(b);
			} catch (Exception e) {
				logService.log(LogService.LOG_WARNING,
						"Exception thrown when displaying detail chart", e);
			}
		});
	}

	private void hideChart(Pane detailPane, Button b) {
		chartShowing = false;
		double oldHeight = stage.getHeight();
		Platform.runLater(() -> {
			b.setOnMouseClicked(event -> showChart(detailPane, b));
			b.getStyleClass().remove("popup-detail-button-open");
			b.getStyleClass().remove("popup-detail-button-closed");
			b.getStyleClass().add("popup-detail-button-closed");
			b.setTooltip(new Tooltip("Click to show details"));
			detailPane.setVisible(false);
			stage.sizeToScene();
			double newHeight = stage.getHeight();
			adaptWindowPosition(oldHeight, newHeight);
			try {
				positionMouse(b);
			} catch (Exception e) {
				logService.log(LogService.LOG_WARNING,
						"Exception thrown when hiding detail chart", e);

			}
		});
	}

	private Pane createSpecialIndicationPane(Map<String, String> examData) {
		if (Integer.parseInt(examData.get("Pachy Apex")) > pentacamConfigurationService
				.getThickCorneaThreshold()) {
			final Text headlineText = new Text("THICK CORNEA");
			headlineText.getStyleClass().add("popup-headline");
			headlineText.getStyleClass().add("popup-headline-Thick");
			headlineText.getStyleClass().add("popup-headline-bad");

			StackPane headlinePane = new StackPane(headlineText);
			headlinePane.getStyleClass().add("popup-headline");
			headlinePane.getStyleClass().add("popup-headline-Thick");
			headlinePane.getStyleClass().add("popup-headline-bad");
			StackPane.setAlignment(headlineText, Pos.CENTER_LEFT);
			return headlinePane;
		}
		// TODO this is a bit silly
		return new StackPane();
	}

	private Pane createSpecialExplanationPane(Map<String, String> examData) {
		if (Integer.parseInt(examData.get("Pachy Apex")) > pentacamConfigurationService
				.getThickCorneaThreshold()) {
			final Label expLabel = new Label("Abnormally thick cornea ("
					+ examData.get("Pachy Apex")
					+ " µm on axis), results of model are not reliable.");
			expLabel.getStyleClass().add("popup-explanation-label");
			// expLabel.setMaxWidth(explanationWidth);
			StackPane expPane = new StackPane(expLabel);
			StackPane.setAlignment(expLabel, Pos.CENTER_LEFT);
			expPane.getStyleClass().add("popup-explanation");
			return expPane;
		}
		// TODO this is a bit silly
		return new StackPane();
	}

	private void setAutoDismiss(int time, TimeUnit unit) {
		final Thread newTimerThread = new Thread(() -> {
			try {
				Thread.sleep(TimeUnit.MILLISECONDS.convert(time, unit));
				hideResults();
			} catch (Exception e) {
			}
			timerThreadRef.compareAndSet(Thread.currentThread(), null);
		});
		Thread oldTimerThread = timerThreadRef.getAndSet(newTimerThread);
		if (oldTimerThread != null) {
			oldTimerThread.interrupt();
		}
		mainPane.setOnMouseEntered(event -> newTimerThread.interrupt());
		mainPane.setOnMouseExited(event -> setAutoDismiss(time, unit));
		newTimerThread.start();
	}

	private void clearAutoDismiss() {
		mainPane.setOnMouseEntered(null);
		mainPane.setOnMouseExited(null);
	}

	@Override
	public void handleEvent(org.osgi.service.event.Event event) {
		String topic = event.getTopic();
		if (FileEventConstants.DATAFILE_CREATED_TOPIC.equals(topic)) {
			handleDataFileCreatedEvent((FileCreatedEvent) event);
		} else if (FileEventConstants.DATAFILE_CHANGED_TOPIC.equals(topic)) {
			handleDataFileChangedEvent((FileEvent) event);
		} else {
			handleAnalysisResultsEvent((AnalysisResultsEvent) event);
		}
	}

	private void handleAnalysisResultsEvent(AnalysisResultsEvent event) {
		boolean showAnyway = userPreferences == null || event.isRecall();
		if (showAnyway || userPreferences.isPopupsEnabled()) {
			hideResults();
			final String headlineKey = event.getHeadlineKey();
			final Category headlineCategory = classificationService.getByKey(
					headlineKey).getCategory();
			final Map<String, Double> distribution = event.getDistribution();
			final Map<String, String> examData = event.getExam().getExamData();
			buildResults(headlineKey, headlineCategory, distribution, examData);
			if (showAnyway
					|| userPreferences.getDisplayTimeSeconds(headlineCategory) > 0.0D) {
				showResults();
			}
			Platform.runLater(() -> {
				if (event.isRecall()) {
					if (isDragged()) {
						restoreWindowPosition();
					} else {
						positionWindow();
					}
					clearAutoDismiss();
				} else {
					clearDrag();
					positionWindow();
					setAutoDismiss((int) userPreferences
							.getDisplayTimeSeconds(headlineCategory),
							TimeUnit.SECONDS);
				}
			});
		}
	}

	private void handleDataFileCreatedEvent(FileCreatedEvent event) {
		if (event.getFileName().toUpperCase().startsWith(PENTACAM)) {
			hideResults();
		}
	}

	private void handleDataFileChangedEvent(FileEvent event) {
		if (event.getFileName().toUpperCase().startsWith(PENTACAM)) {
			hideResults();
		}
	}

}

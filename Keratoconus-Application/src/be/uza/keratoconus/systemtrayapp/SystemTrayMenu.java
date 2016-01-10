package be.uza.keratoconus.systemtrayapp;

import java.awt.AWTException;
import java.awt.HeadlessException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.stage.StageStyle;

import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import be.uza.keratoconus.configuration.api.Classification.Category;
import be.uza.keratoconus.configuration.api.PentacamConfigurationService;
import be.uza.keratoconus.systemtrayapp.api.HtmlViewerService;
import be.uza.keratoconus.userprefs.api.PreferencesWindow;
import be.uza.keratoconus.userprefs.api.UserPreferences;
import be.uza.keratoconus.userprefs.impl.UserPreferencesMenu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Component(provide = UserPreferences.class, configurationPolicy = ConfigurationPolicy.ignore)
public class SystemTrayMenu extends PopupMenu implements ActionListener,
		UserPreferences {

	private static final String SELECTED_MODEL_NAME = "selectedModelName";

	private static final String PREFS_FILE_NAME = "preferences.json";

	private static final String MENU_TEXT_USER_PREFS = "User preferences ...";

	private static final long serialVersionUID = 1L;

	private static class UserPrefsDAO {
		private boolean popupsEnabled;
		private boolean messagesEnabled;
		private boolean animateEnabled;
		private Map<Category, Double> displayTimeSeconds;
		private StageStyle mainPopupStageStyle;
		private Pos mainPopupPosition;
		private boolean patientRecordsEnabled;
		private String patientRecordDirectory;
		private ChartType detailChartType;
		private String baseIconPath;
		public String selectedModelName;
	}

	private UserPrefsDAO prefs;
	private LogService logService;
	private EventAdmin eventAdmin;
	private HtmlViewerService aboutService;
	private PentacamConfigurationService pentacamConfigurationService;
	private UserPreferencesMenu prefsMenu;
	private Path prefsDirPath;
	private Path configFilePath;
	private ComponentContext ownComponentContext;
	private double defaultDisplayTimeSeconds = 30D;
	private StageStyle defaultWindowStageStyle;
	private Pos defaultWindowPosition;
	private ChartType defaultDetailChartType = ChartType.BAR;
	private String defaultBaseIconPath;
	private String applicationTitle;
	private Map<PreferencesWindow, Map<String, Object>> preferencesWindowMap = new HashMap<>();
	private boolean active;

	public SystemTrayMenu() throws HeadlessException {
	}

	@Reference
	protected void setPentacamConfigurationService(
			PentacamConfigurationService s) {
		pentacamConfigurationService = s;
	}

	@Reference
	protected void setLogService(LogService s) {
		logService = s;
	}

	@Reference
	protected void setEventAdmin(EventAdmin s) {
		eventAdmin = s;
	}

	@Reference
	protected void setAboutService(HtmlViewerService s) {
		aboutService = s;
	}

	@Reference(dynamic = true, multiple = true, optional = true)
	protected void addPreferencesWindow(PreferencesWindow pw,
			Map<String, Object> properties) {
		preferencesWindowMap.put(pw, properties);
		if (active) {
			prefsMenu.addPreferencesWindow(pw, extractRank(properties));
		}
	}

	protected void removePreferencesWindow(PreferencesWindow pw) {
		if (active) {
			prefsMenu.removePreferencesWindow(pw);
		}
		preferencesWindowMap.remove(pw);
	}

	@Activate
	protected void activate(ComponentContext cc)
			throws IOException, AWTException {
		ownComponentContext = cc;
		prefsDirPath = pentacamConfigurationService.getUserPrefsDirectoryPath();
		applicationTitle = pentacamConfigurationService.getApplicationTitle();
		defaultBaseIconPath = pentacamConfigurationService.getBaseIconPath();
		configFilePath = FileSystems.getDefault().getPath(PREFS_FILE_NAME);
		Files.createDirectories(prefsDirPath);
		prefs = readConfig();

		prefsMenu = new UserPreferencesMenu(MENU_TEXT_USER_PREFS, this);
		setUpMenu();
		for (Entry<PreferencesWindow, Map<String, Object>> entry : preferencesWindowMap
				.entrySet()) {
			PreferencesWindow pw = entry.getKey();
			Map<String, Object> properties = entry.getValue();
			prefsMenu.addPreferencesWindow(pw, extractRank(properties));
		}
		active = true;
	}

	private int extractRank(Map<String, Object> properties) {
		String rankString = (String) properties.get("rank");
		try {
			return Integer.parseInt(rankString);
		} catch (Exception e) {
			logService
					.log(ownComponentContext.getServiceReference(),
							LogService.LOG_WARNING,
							"Configuration problem: could not parse rank of "
									+ PreferencesWindow.class.getSimpleName()
									+ " component "
									+ properties
											.get(ComponentConstants.COMPONENT_NAME));
			return 0;
		}
	}

	@Deactivate
	protected void deactivate() {
		active = false;
		prefsMenu = null;
	}

	private void setUpMenu() {
		addActionListener(this);
		final MenuItem menuItemAbout = new MenuItem("About " + applicationTitle);
		add(menuItemAbout);
		menuItemAbout.addActionListener(event -> Platform
				.runLater(() -> aboutService.showPage("/html/about.html", "About " + applicationTitle)));
		final MenuItem menuItemManual = new MenuItem("User Manual");
		add(menuItemManual);
		menuItemManual.addActionListener(event -> Platform
				.runLater(() -> aboutService.showPage("/html/manual1.html", applicationTitle + " - User Manual")));
		addSeparator();

		add(prefsMenu);
		addSeparator();
		
		final MenuItem menuItemRestart = new MenuItem("Restart "
				+ applicationTitle);
		add(menuItemRestart);
		final MenuItem menuItemExit = new MenuItem("Exit " + applicationTitle);
		add(menuItemExit);
	}

	@Override
	public void configurePopupsOnOff(boolean b) {
		logInfo("Setting popus enabled to " + b);
		prefs.popupsEnabled = b;
		writeConfig(prefs);
	}

	@Override
	public void configureMessagesOnOff(boolean b) {
		logInfo("Setting messages enabled to " + b);
		prefs.messagesEnabled = b;
		writeConfig(prefs);
	}

	@Override
	public void configureAnimateOnOff(boolean b) {
		logInfo("Setting animations enabled to " + b);
		prefs.animateEnabled = b;
		writeConfig(prefs);
	}

	@Override
	public void configureMainPopupPosition(Pos pos) {
		logInfo("Setting main popup position to " + pos);
		prefs.mainPopupPosition = pos;
		writeConfig(prefs);
	}

	@Override
	public void configureMainPopupStageStyle(StageStyle ss) {
		logInfo("Setting main popup stage style to " + ss);
		prefs.mainPopupStageStyle = ss;
		writeConfig(prefs);
	}

	@Override
	public void configureGraphicalPatientRecordOnOff(Boolean b) {
		logInfo("Setting graphical patient records enabled to " + b);
		prefs.patientRecordsEnabled = b;
		writeConfig(prefs);
	}

	@Override
	public void configurePatientRecordDirectory(String path) {
		logInfo("Setting patient record directory to " + path);
		prefs.patientRecordDirectory = path;
		writeConfig(prefs);
	}

	@Override
	public void configureDetailChartType(ChartType ct) {
		logInfo("Setting detail chart type to " + ct);
		prefs.detailChartType = ct;
		writeConfig(prefs);
	}

	@Override
	public void configureSelectedModelName(String modelName) {
		logInfo("Setting selected model name to " + modelName);
		prefs.selectedModelName = modelName;
		writeConfig(prefs);
		Map<String, String> changed = new HashMap<>();
		changed.put(SELECTED_MODEL_NAME, modelName);
		eventAdmin.postEvent(new UserPreferencesChangedEvent(SELECTED_MODEL_NAME, changed));
	}
	
	private UserPrefsDAO readConfig() {
		GsonBuilder builder = new GsonBuilder();
		builder.enableComplexMapKeySerialization();
		Gson gson = builder.create();
		Path path = prefsDirPath.resolve(configFilePath);
		try {
			if (Files.exists(path) && Files.size(path) > 0) {
				try (InputStream fileInputStream = Files.newInputStream(path)) {
					final UserPrefsDAO config = gson.fromJson(
							new InputStreamReader(fileInputStream),
							UserPrefsDAO.class);
					return config;
				}
			}
		} catch (IOException ioe) {
			logService.log(LogService.LOG_WARNING,
					"Exception thrown when reading user configuration", ioe);
		}

		UserPrefsDAO defaultConfig = createDefaultUserConfig();
		defaultConfig.animateEnabled = true;
		defaultConfig.popupsEnabled = true;
		defaultConfig.displayTimeSeconds = new HashMap<>();
		writeConfig(defaultConfig);
		return defaultConfig;
	}

	private UserPrefsDAO createDefaultUserConfig() {
		return new UserPrefsDAO();
	}

	private void writeConfig(UserPrefsDAO userConfig) {
		Gson gson = new Gson();
		Path path = prefsDirPath.resolve(FileSystems.getDefault().getPath(
				PREFS_FILE_NAME));
		logInfo("Writing JSON to " + path);
		try (final OutputStreamWriter writer = new OutputStreamWriter(
				Files.newOutputStream(path))) {
			gson.toJson(userConfig, writer);
		} catch (IOException e) {
			logException(e,
					"Problem encountered when saving user configuration to "
							+ path);
		}
	}

	@Override
	public boolean isPopupsEnabled() {
		return prefs.popupsEnabled;
	}

	@Override
	public boolean isMessagesEnabled() {
		return prefs.messagesEnabled;
	}

	@Override
	public boolean isAnimateEnabled() {
		return prefs.animateEnabled;
	}

	@Override
	public boolean isPatientRecordEnabled() {
		return prefs.patientRecordsEnabled;
	}

	@Override
	public double getDisplayTimeSeconds(Category cat) {
		final Double value = prefs.displayTimeSeconds.get(cat);
		return value == null ? defaultDisplayTimeSeconds : value;
	}

	@Override
	public StageStyle getMainPopupStageStyle() {
		StageStyle value = prefs.mainPopupStageStyle == null ? defaultWindowStageStyle
				: prefs.mainPopupStageStyle;
		return value == null ? StageStyle.UNDECORATED : value;
	}

	@Override
	public Pos getMainPopupPosition() {
		Pos value = prefs.mainPopupPosition == null ? defaultWindowPosition
				: prefs.mainPopupPosition;
		return value == null ? Pos.BOTTOM_RIGHT : value;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command != null) {
			logInfo("Menu item '" + command + "' selected");
			if (command.startsWith("Restart ")) {
				logInfo("Restarting application");
				try {
					FrameworkUtil.getBundle(
							pentacamConfigurationService.getClass()).update();
				} catch (BundleException e1) {
					logException(e1,
							"Exception thrown when restarting framework");
				}
			}
			if (command.startsWith("Exit ")) {
				logInfo("Terminating application");
				try {
					ownComponentContext.getBundleContext().getBundle(0).stop();
				} catch (BundleException e1) {
					logException(e1, "Exception thrown when stopping framework");
				}
			}
		}
	}

	@Override
	public String getPatientRecordDirectory() {
		return prefs.patientRecordDirectory;
	}

	@Override
	public void setDefaultWindowStageStyle(String wss) {
		try {
			Field field = StageStyle.class.getField(wss.toUpperCase());
			defaultWindowStageStyle = (StageStyle) field.get(null);
		} catch (NoSuchFieldException | SecurityException
				| IllegalArgumentException | IllegalAccessException e) {
			logService.log(LogService.LOG_WARNING,
					"Exception thrown when setting default window stage style",
					e);
		}
	}

	@Override
	public void setDefaultWindowPosition(String wpos) {
		try {
			Field field = Pos.class.getField(wpos.replace(' ', '_')
					.toUpperCase());
			defaultWindowPosition = (Pos) field.get(null);
		} catch (NoSuchFieldException | SecurityException
				| IllegalArgumentException | IllegalAccessException e) {
			logService.log(LogService.LOG_WARNING,
					"Exception thrown when setting default window position", e);
		}
	}

	@Override
	public void setDefaultDisplayTimeSeconds(double seconds) {
		defaultDisplayTimeSeconds = seconds;
	}

	@Override
	public void configureDisplayTimeSeconds(Category cat, double seconds) {
		prefs.displayTimeSeconds.put(cat, seconds);
	}

	@Override
	public ChartType getDetailChartType() {
		ChartType value = prefs.detailChartType == null ? defaultDetailChartType
				: prefs.detailChartType;
		return value == null ? ChartType.BAR : value;
	}

	@Override
	public String getBaseIconPath() {
		String path = prefs.baseIconPath == null ? defaultBaseIconPath
				: prefs.baseIconPath;
		return path;
	}

	@Override
	public String getSelectedModelName() {
		return prefs.selectedModelName;
	}
	
	@Override
	public String getIconPath(String variant) {
		String path = prefs.baseIconPath == null ? defaultBaseIconPath
				: prefs.baseIconPath;
		String basePath = path;
		int lastDot = basePath.lastIndexOf('.');
		return basePath.substring(0, lastDot) + "-" + variant
				+ basePath.substring(lastDot);
	}

	@Override
	public void setDefaultDetailChartType(String detail_chart_type) {
		ChartType value = ChartType.valueOf(detail_chart_type.toUpperCase());
		if (value != null) {
			defaultDetailChartType = value;
		}
	}

	private void logInfo(String message) {
		logService.log(ownComponentContext.getServiceReference(),
				LogService.LOG_INFO, message);
	}

	private void logException(Exception e, String message) {
		logService.log(ownComponentContext.getServiceReference(),
				LogService.LOG_WARNING, message, e);
	}

}
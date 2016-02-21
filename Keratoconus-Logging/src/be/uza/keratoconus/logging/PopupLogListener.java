package be.uza.keratoconus.logging;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import be.uza.keratoconus.configuration.api.PentacamConfigurationService;

@Component(immediate = true, provide = {})
public class PopupLogListener implements LogListener {

	private static final String APP_WILL_TERMINATE = "\n\nThe application will terminate.";
	private static final String SEE_LOG_FILE = "See log file for exception stack trace:";
	private PentacamConfigurationService pentacamConfigurationService;
	private CopyOnWriteArrayList<LogReaderService> logReaderServices = new CopyOnWriteArrayList<>();
	private volatile boolean stopping;

	@Reference(dynamic = true, optional = true)
	protected void setPentacamConfigurationService(
			PentacamConfigurationService s) {
		pentacamConfigurationService = s;
	}

	protected void unsetPentacamConfigurationService(
			PentacamConfigurationService s) {
		if (pentacamConfigurationService == s) {
			pentacamConfigurationService = null;
		}
	}

	@Reference(dynamic = true, multiple = true, optional = true)
	protected void addLogReaderService(LogReaderService s) {
		s.addLogListener(this);
		logReaderServices.add(s);
	}

	protected void removeLogReaderService(LogReaderService s) {
		logReaderServices.remove(s);
		s.removeLogListener(this);
	}

	@Override
	public void logged(LogEntry entry) {
		int level = entry.getLevel();
		if (!stopping && level <= LogService.LOG_WARNING) {
			stopping |= level == LogService.LOG_ERROR;
			Platform.runLater(() -> showAlertDialogue(entry, level));
		}
	}

	private void showAlertDialogue(LogEntry entry, int level) {
		String contentText = entry.getMessage();
		final Alert alert = buildAlertDialogue(entry);
		Path loggingDirectoryPath = pentacamConfigurationService.getLoggingDirectoryPath();
		String[] logFileNames = loggingDirectoryPath.toFile().list((dir, name) -> name.startsWith("keratoconus-log-"));
		Arrays.sort(logFileNames);
		String lastLogFileName = logFileNames[logFileNames.length - 1];
		switch (level) {
		case LogService.LOG_WARNING:
			if (entry.getException() != null) {
				contentText += formatLogFileMessage(loggingDirectoryPath, lastLogFileName);
			}
			alert.setContentText(contentText);
			alert.show();
			break;

		case LogService.LOG_ERROR:
			if (entry.getException() != null) {
				contentText += formatLogFileMessage(loggingDirectoryPath, lastLogFileName);
			}
			alert.setContentText(contentText
					+ APP_WILL_TERMINATE);
			alert.showAndWait();
			System.exit(0);
			break;

		default:
		}
	}

	private String formatLogFileMessage(Path loggingDirectoryPath,
			String lastLogFileName) {
		return "\n\n" + SEE_LOG_FILE + "\n" + loggingDirectoryPath + File.separator + lastLogFileName;
	}

	private Alert buildAlertDialogue(LogEntry entry) {
		int level = entry.getLevel();
		AlertType alertType = logLevel2AlertType(level);

		Alert alert = new Alert(alertType);
		if (pentacamConfigurationService != null) {
			alert.setTitle(pentacamConfigurationService.getApplicationTitle());
		}
		
		String headerText = alertType.name();
		if (entry.getServiceReference() != null) {
			Object objectClass = entry.getServiceReference().getProperty(
					"objectClass");
			if (objectClass != null && (!objectClass.getClass().isArray())) {
				headerText += " from " + objectClass;
			}
		}
		alert.setHeaderText(headerText);
		return alert;
	}

	private AlertType logLevel2AlertType(int level) {
		switch (level) {
		case LogService.LOG_ERROR:
			return AlertType.ERROR;
		case LogService.LOG_WARNING:
			return AlertType.WARNING;
		default:
			return AlertType.INFORMATION;
		}
	}

}

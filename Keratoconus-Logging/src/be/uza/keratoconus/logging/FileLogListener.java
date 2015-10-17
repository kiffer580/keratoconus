package be.uza.keratoconus.logging;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import be.uza.keratoconus.configuration.api.PentacamConfigurationService;

@Component(immediate = true, provide = {})
public class FileLogListener implements LogListener {

	private static final String LOG_FILE_BASE_NAME = "keratoconus-log";
	private static final String LOG_FILE_EXTENSION = "txt";
	private static final DateFormat FILE_NAME_FORMAT = new SimpleDateFormat("'" + LOG_FILE_BASE_NAME + "'-yyyyMMdd'T'HHmmss'." + LOG_FILE_EXTENSION + "'");
	
	private PentacamConfigurationService pentacamConfigurationService;
	private CopyOnWriteArrayList<LogReaderService> logReaderServices = new CopyOnWriteArrayList<>();
	private String[] logLevelNames = { null, "ERROR", "WARNING", "INFO",
			"DEBUG" };
	private Path logDirPath;
	private int logLevel;
	private volatile boolean active;
	private String fileName;

	@Reference
	protected void setPentacamConfigurationService(
			PentacamConfigurationService s) {
		pentacamConfigurationService = s;
	}

	@Reference(dynamic = true, multiple = true, optional = true)
	protected void addLogReaderService(LogReaderService s) {
		s.addLogListener(this);
		if (active) {
			writeToLogFile("Adding LogReader: " + s);
			@SuppressWarnings("unchecked")
			Enumeration<LogEntry> logs = s.getLog();
			while (logs.hasMoreElements()) {
				writeToLogFile(logs.nextElement());
			}
		}
		logReaderServices.add(s);
	}

	protected void removeLogReaderService(LogReaderService s) {
		logReaderServices.remove(s);
		writeToLogFile("Removed LogReader: " + s);
		s.removeLogListener(this);
	}

	@Activate
	protected void activate() throws IOException {
		logDirPath = pentacamConfigurationService.getLoggingDirectoryPath();
		logLevel = pentacamConfigurationService.getLogLevel();
		Files.createDirectories(logDirPath);
		fileName = FILE_NAME_FORMAT.format(new Date());

		active = true;
	}

	@Deactivate
	protected void deactivate() {
		active = false;
	}

	@Override
	public void logged(LogEntry entry) {
		if (active && entry.getLevel() <= logLevel) {
			writeToLogFile(entry);
		}
	}

	private void writeToLogFile(LogEntry entry) {
		if (entry.getLevel() <= logLevel) {
			Path path = logDirPath.resolve(FileSystems.getDefault().getPath(
					fileName));
			try (final PrintStream logStream = new PrintStream(
					Files.newOutputStream(path, StandardOpenOption.CREATE,
							StandardOpenOption.APPEND))) {
				String firstLine = new Date(entry.getTime()) + " "
																+ logLevelNames[entry.getLevel()] + " "
																+ entry.getBundle();
				if (entry.getServiceReference() != null) {
					firstLine += " "
							+ entry.getServiceReference();
				}
				String secondLine = "  " + entry.getMessage();
				logStream.println(firstLine);
				logStream.println(secondLine);
				if (entry.getException() != null) {
					entry.getException().printStackTrace(logStream);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void writeToLogFile(String s) {
		Path path = logDirPath.resolve(FileSystems.getDefault().getPath(
				fileName));
		try (final PrintStream logStream = new PrintStream(
				Files.newOutputStream(path, StandardOpenOption.CREATE,
						StandardOpenOption.APPEND))) {
			logStream.println(s);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

package be.uza.keratoconus.datafiles.impl;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import be.uza.keratoconus.configuration.api.PentacamConfigurationService;
import be.uza.keratoconus.datafiles.api.PentacamFile;
import be.uza.keratoconus.datafiles.api.PentacamFilesService;
import be.uza.keratoconus.datafiles.event.FileChangedEvent;
import be.uza.keratoconus.datafiles.event.FileCreatedEvent;

@Component(immediate = true)
public class FileWatcher {

	private WatchService watcher;
	private Path watchee;
	private WatchKey registrationKey;
	private volatile Thread thread;
	private Path pentacamDirectoryPath;
	private PentacamFilesService pentacamFilesService;
	private EventAdmin eventAdmin;
	private LogService logService;
	private Map<String, PentacamFile> files = new HashMap<String, PentacamFile>();
	private PentacamConfigurationService pentacamConfigurationService;

	@Reference
	protected void setLogService(LogService logService) {
		this.logService = logService;
	}

	@Reference
	protected void setEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	@Reference
	protected void setPentacamConfigurationService(
			PentacamConfigurationService pentacamConfigurationService,
			Map<String, Object> properties) {
				this.pentacamConfigurationService = pentacamConfigurationService;
	}

	@Reference
	protected void setPentacamFilesService(
			PentacamFilesService pentacamFilesService) {
		this.pentacamFilesService = pentacamFilesService;
	}

	@Activate
	protected void activate(ComponentContext cc) throws IOException {
		pentacamDirectoryPath = pentacamConfigurationService.getPentacamDirectoryPath();
		List<PentacamFile> allFiles = pentacamFilesService.getAllFiles();
		for (PentacamFile pf : allFiles) {
			String baseName = pf.getBaseName();
			files.put(baseName, pf);
		}
		logService.log(LogService.LOG_INFO, "FileWatcher: watching " + pentacamDirectoryPath);
		watchee = pentacamDirectoryPath;
		watcher = FileSystems.getDefault().newWatchService();
		registrationKey = watchee.register(watcher,
				StandardWatchEventKinds.ENTRY_CREATE,
				StandardWatchEventKinds.ENTRY_MODIFY);
		thread = new Thread(this::watch, "FileWatcher thread");
		thread.start();
	}

	private void watch() {
		while (thread != null) {
			try {
				WatchKey key = watcher.take();
				List<WatchEvent<?>> watchEvents = key.pollEvents();
				for (WatchEvent<?> we : watchEvents) {
					if (we.kind() == StandardWatchEventKinds.OVERFLOW) {
						logService.log(LogService.LOG_WARNING, "File watcher: overflow, events may have been lost");
						continue;
					} else if (we.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
						final Path contextPath = (Path) we.context();
						if (contextPath.getParent() == null) {
							eventAdmin.postEvent(new FileCreatedEvent(contextPath.toString()));
						}
					} else {
						final Path contextPath = (Path) we.context();
						if (contextPath.getParent() == null) {
							eventAdmin.postEvent(new FileChangedEvent(contextPath.toString()));
						}
					}
					if (!key.reset()) {
						logService.log(LogService.LOG_INFO, "FileWatcher: key is no longer valid");
						break;
					}
				}
			} catch (InterruptedException e) {
				break;
			} catch (Exception e) {
				logService.log(LogService.LOG_WARNING, "FileWatcher: exception in main loop", e);
				break;
			}
		}
		logService.log(LogService.LOG_INFO, "FileWatcher: cancelling key");
		registrationKey.cancel();
	}

	@Deactivate
	protected void deactivate() throws IOException {
		thread.interrupt();
		thread = null;
		watcher = null;
	}

}

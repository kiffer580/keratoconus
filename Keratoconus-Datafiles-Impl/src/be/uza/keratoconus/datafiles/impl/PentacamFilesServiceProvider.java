package be.uza.keratoconus.datafiles.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import be.uza.keratoconus.datafiles.api.PentacamFile;
import be.uza.keratoconus.datafiles.api.PentacamFilesService;
import be.uza.keratoconus.model.api.ClassificationModelService;

@Component(immediate = true, provide = {})
// Do not be fooled by the above, we will register a PentacamFilesService when
// we are good and ready
public class PentacamFilesServiceProvider implements PentacamFilesService {

	private Map<String, PentacamFile> files = new ConcurrentHashMap<String, PentacamFile>();
	private Lock filesReadyLock = new ReentrantLock();
	private Condition filesReadyCondition = filesReadyLock.newCondition();
	private boolean allFilesPresent;
	private String[] fileBaseNames;
	private TimeUnit timeoutUnit = TimeUnit.SECONDS;
	private long timeout = 300L;
	private List<PentacamFile> allFiles = new ArrayList<PentacamFile>();
	private BundleContext bundleContext;
	private LogService logService;
	private AtomicReference<Thread> serviceRegistrationThreadRef = new AtomicReference<>();
	private AtomicReference<ServiceRegistration> registrationRef = new AtomicReference<>();
	private ClassificationModelService classificationModelService;

	@Reference
	protected void setLogService(LogService logService) {
		this.logService = logService;
	}

	@Reference
	protected void setClassificationModelService(
			ClassificationModelService cms) {
		this.classificationModelService = cms;
	}

	@Reference(dynamic = true, multiple = true, optional = true)
	protected void addPentacamFile(PentacamFile pf) {
		files.put(pf.getBaseName(), pf);
		checkAllFilesPresent();
	}

	protected void removePentacamFile(PentacamFile pf) {
		files.remove(pf.getBaseName());
	}

	@Activate
	protected void activate(ComponentContext cc) throws IOException {
		bundleContext = cc.getBundleContext();
		fileBaseNames = classificationModelService.getFileBaseNames();
		Thread thread = new Thread(
				() -> registerServiceWhenAllFilesArePresent());
		serviceRegistrationThreadRef.set(thread);
		thread.start();
	}

	@Deactivate
	protected void deactivate() {
		Thread thread = serviceRegistrationThreadRef.getAndSet(null);
		if (thread != null) {
			thread.interrupt();
		}
		ServiceRegistration registration = registrationRef.getAndSet(null);
		if (registration != null) {
			registration.unregister();
		}
	}

	private void registerServiceWhenAllFilesArePresent() {
		checkAllFilesPresent();
		filesReadyLock.lock();
		Date deadline = new Date(System.currentTimeMillis()
				+ timeoutUnit.toMillis(timeout));
		boolean ready = true;
		try {
			while (!allFilesPresent) {
				ready |= filesReadyCondition.await(1L, TimeUnit.SECONDS);
				if (!ready && new Date().after(deadline)) {
					throw new TimeoutException(
							"Timeout when waiting for datafiles to be read.");
				}
			}
			for (String baseName : fileBaseNames) {
				final PentacamFile pf = files.get(baseName);
				allFiles.add(pf);
			}
		} catch (InterruptedException | TimeoutException e) {
			logService.log(LogService.LOG_ERROR,
					"Failed to start all PentacamFile service instances", e);
		} finally {
			filesReadyLock.unlock();
		}

		// Now we register the service
		final Hashtable<String, Object> properties = new Hashtable<String, Object>();
		// TODO does this serve any useful purpose?
		properties.put("files", allFiles);
		registrationRef.set(bundleContext.registerService(
				PentacamFilesService.class.getName(), this, properties));
	}

	@Override
	public List<PentacamFile> getAllFiles() throws FileNotFoundException {
		 return allFiles;
	}

	@Override
	public PentacamFile getFileByBaseName(String baseName)
			throws FileNotFoundException {
		return files.get(baseName);
	}

	private void checkAllFilesPresent() {
		filesReadyLock.lock();
		try {
			Set<String> availableFileBaseNames = files.keySet();
			if (fileBaseNames == null) {
				logService
						.log(LogService.LOG_INFO,
								"Not ready (PentacamFilesService is not yet activated)");
				return;
			}

			HashSet<String> neededSet = new HashSet<>(
					Arrays.asList(fileBaseNames));
			neededSet.removeAll(availableFileBaseNames);
			logService.log(LogService.LOG_INFO, "Have files "
					+ availableFileBaseNames + ", still need " + neededSet);
			if (neededSet.isEmpty()) {
				allFilesPresent = true;
				filesReadyCondition.signalAll();
			}
		} finally {
			filesReadyLock.unlock();
		}
	}

}

package be.uza.keratoconus.model.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import weka.classifiers.functions.SMO;
import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Reference;
import be.uza.keratoconus.model.api.ModelService;

@Component(configurationPolicy = ConfigurationPolicy.ignore)
public class ModelServiceImpl implements ModelService {

	private static final String CONFIG_PATH_PREFIX = "/config/";
	private static final String CONFIG_PATH_SUFFIX = ".properties";
	private static final String MODEL_PATH_PREFIX = "/model/";
	private static final String MODEL_PATH_SUFFIX = ".model";
	private static final String COMMA = ",";
	
	private List<String> modelNames;
	private String[] fileBaseNames;
	private String[] keyFields;
	private String[] commonFields;
	Map<String, String> separators = new HashMap<>();
	Map<String, String> fields = new HashMap<>();
	private LogService logService;
	private String selectedModelName;
	private SMO classifier;

	@Reference
	protected void setLogService(LogService ls) {
		logService = ls;
	}
	
	@Activate
	protected void activate(BundleContext bc) throws IOException {
		modelNames = locateModelNames(bc);
		if (modelNames.size() == 0) {
			throw new RuntimeException("No classification models were found.");
		}
	}

	private List<String> locateModelNames(BundleContext bc) {
		final List<String> modelNames = new ArrayList<>();
		final Set<String> configNames = new HashSet<>();
		final Bundle myBundle = bc.getBundle();
		@SuppressWarnings("unchecked")
		Enumeration<URL> modelEntries = myBundle.findEntries(MODEL_PATH_PREFIX, "*" + MODEL_PATH_SUFFIX, false);
		@SuppressWarnings("unchecked")
		Enumeration<URL> configEntries = myBundle.findEntries(CONFIG_PATH_PREFIX, "*" + CONFIG_PATH_SUFFIX, false);
		while (configEntries.hasMoreElements()) {
			final String configName = path2name(configEntries.nextElement().getPath(), CONFIG_PATH_PREFIX, CONFIG_PATH_SUFFIX);
			configNames.add(configName);
		}
		while (modelEntries.hasMoreElements()) {
			final String modelName = path2name(modelEntries.nextElement().getPath(), MODEL_PATH_PREFIX, MODEL_PATH_SUFFIX);
			if (configNames.remove(modelName)) {
				logService.log(LogService.LOG_INFO, "Found model and config file for: " + modelName);
				modelNames.add(modelName);
			}
			else {
				logService.log(LogService.LOG_INFO, "Found model file with no corresponding config file, ignoring: " + modelName);				
			}
			for (String configName : configNames) {
	logService.log(LogService.LOG_INFO, "Found config file with no corresponding model file, ignoring: " + configName);				
			}
		}
		System.out.println(modelNames);
		return modelNames;
	}

	private String path2name(final String path, String prefix, String suffix) {
		return path.substring(prefix.length(), path.length() - suffix.length());
	}
	
	@Override
	public List<String> getAvailableModelNames() {
		return new ArrayList<>(modelNames);
	}
	
	@Override
	public void selectModel(String modelName) throws Exception {
		System.out.println("ModelService: selecting " + modelName);
		final InputStream stream = getClass().getResourceAsStream(
				CONFIG_PATH_PREFIX + modelName + CONFIG_PATH_SUFFIX);
		Properties config = new Properties();
		config.load(stream);
		fileBaseNames = ((String) config.get("pentacam.files")).split(COMMA);
		keyFields = ((String) config.get("pentacam.fields.key")).split(COMMA);
		commonFields = ((String) config.get("pentacam.fields.common"))
				.split(COMMA);
		for (String fbn : fileBaseNames) {
			separators.put(fbn, (String) config.get(fbn + ".field.separator"));
			fields.put(fbn, (String) config.get(fbn + ".fields"));
		}
		classifier = (SMO) weka.core.SerializationHelper.read(getClass()
				.getResourceAsStream(MODEL_PATH_PREFIX + modelName + MODEL_PATH_SUFFIX));
		selectedModelName = modelName;
	}
	
	@Override
	public String getSelectedModelName() {
		return selectedModelName;
	}
	
	@Override
	public String[] getCommonFields() {
		return commonFields;
	}

	@Override
	public String[] getKeyFields() {
		return keyFields;
	}

	@Override
	public String[] getFileBaseNames() {
		return fileBaseNames;
	}

	@Override
	public String getSeparatorForFile(String fbn) {
		return separators.get(fbn);
	}

	@Override
	public String getFieldsOfFile(String fbn) {
		return fields.get(fbn);
	}

	@Override
	public SMO getClassifier() throws Exception {
		return classifier;

	}
}

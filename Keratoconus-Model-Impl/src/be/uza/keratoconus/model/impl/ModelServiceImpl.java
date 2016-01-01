package be.uza.keratoconus.model.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.osgi.service.log.LogService;

import weka.classifiers.functions.SMO;
import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Reference;
import be.uza.keratoconus.model.api.ModelService;

@Component(configurationPolicy = ConfigurationPolicy.require)
public class ModelServiceImpl implements ModelService {

	private static final String CONFIG_PATH_PREFIX = "/config/";
	private static final String CONFIG_PATH_SUFFIX = ".properties";
	private static final String MODEL_PATH_PREFIX = "/model/";
	private static final String MODEL_PATH_SUFFIX = ".model";
	private static final String COMMA = ",";

	private String[] fileBaseNames;
	private String[] keyFields;
	private String[] commonFields;
	private String[] usedFields;
	Map<String, String> separators = new HashMap<>();
	Map<String, String> fields = new HashMap<>();
	private LogService logService;
	private String modelName;
	private SMO classifier;

	@Reference
	protected void setLogService(LogService ls) {
		logService = ls;
	}

	@Activate
	protected void activate(Map<String, String> props) throws Exception {
		modelName = props.get(MODEL_NAME);
		Properties config = readConfiguration();
		fileBaseNames = ((String) config.get("pentacam.files")).split(COMMA);
		keyFields = ((String) config.get("pentacam.fields.key")).split(COMMA);
		commonFields = ((String) config.get("pentacam.fields.common"))
				.split(COMMA);
		usedFields = ((String) config.get("pentacam.fields.used")).split(COMMA);
		for (String fbn : fileBaseNames) {
			separators.put(fbn, (String) config.get(fbn + ".field.separator"));
			fields.put(fbn, (String) config.get(fbn + ".fields"));
		}
		classifier = (SMO) weka.core.SerializationHelper.read(getClass()
				.getResourceAsStream(
						MODEL_PATH_PREFIX + modelName + MODEL_PATH_SUFFIX));
	}

	private Properties readConfiguration() throws IOException {
		final InputStream stream = getClass().getResourceAsStream(
				CONFIG_PATH_PREFIX + modelName + CONFIG_PATH_SUFFIX);
		Properties config = new Properties();
		config.load(stream);
		String formatVersion = (String) config.get("config.format.version");
		if (formatVersion == null) {
			logService.log(LogService.LOG_WARNING, "No configuration file version found, defaulting to 1.1");
			formatVersion = "1.1";
		} else {
			switch (formatVersion) {
			case "1.1":
				logService.log(LogService.LOG_INFO, "Configuration file version is: "
						+ formatVersion);
				break;
			default:
				logService.log(LogService.LOG_ERROR, "Unknown configuration file version: "
						+ formatVersion + ", known versions are: 1.1");
			}
		}
		return config;
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
	public String[] getUsedFields() {
		return usedFields;
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

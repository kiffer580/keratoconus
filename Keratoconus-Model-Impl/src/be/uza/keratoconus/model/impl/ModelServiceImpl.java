package be.uza.keratoconus.model.impl;

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
		System.out.println("ModelService: activated with properties " + props);
		modelName = props.get(MODEL_NAME);
		final InputStream stream = getClass().getResourceAsStream(CONFIG_PATH_PREFIX + modelName + CONFIG_PATH_SUFFIX); Properties
		config = new Properties(); 
		config.load(stream); 
		fileBaseNames = ((String) config.get("pentacam.files")).split(COMMA);
		keyFields = ((String) config.get("pentacam.fields.key")).split(COMMA);
		commonFields = ((String) config.get("pentacam.fields.common")).split(COMMA); 
		for (String fbn : fileBaseNames) { 
			separators.put(fbn, (String) config.get(fbn + ".field.separator")); 
			fields.put(fbn,(String) config.get(fbn + ".fields")); 
			} 
		classifier = (SMO) weka.core.SerializationHelper.read(getClass().getResourceAsStream(MODEL_PATH_PREFIX + modelName + MODEL_PATH_SUFFIX)); 
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

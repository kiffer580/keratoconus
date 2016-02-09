package be.uza.keratoconus.model.impl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.experimental.categories.Categories.CategoryFilter;
import org.osgi.service.log.LogService;

import weka.classifiers.functions.SMO;
import weka.classifiers.functions.SMO.BinarySMO;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.NominalAttributeInfo;
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
	private String classAttributeName;
	private int classAttributeIndex;
	private List<String> classAttributeValues;
	private List<String> attributeNames;

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
		extractAttributes(classifier);
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

	/*
	 * Extract classifier information using reflection
	 */
	@SuppressWarnings("unchecked")
	private void extractAttributes(SMO classifier) {
		try {
			Attribute classAttribute = examineField(classifier,
					"m_classAttribute");
			classAttributeName = examineField(classAttribute, "m_Name");
			Field attributeNameField = Attribute.class
					.getDeclaredField("m_Name");
			attributeNameField.setAccessible(true);
			System.out.println("classAttributeName = " + classAttributeName);
			classAttributeIndex = examineField(classAttribute, "m_Index");
			System.out.println("classAttributeIndex = " + classAttributeIndex);
			NominalAttributeInfo info = examineField(classAttribute,
					"m_AttributeInfo");

			classAttributeValues = examineField(info, "m_Values");
			System.out
					.println("classAttributeValues = " + classAttributeValues);
			Hashtable<String, Integer> classHashtable = examineField(info,
					"m_Hashtable");
			System.out.println("class hashtable = " + classHashtable);
			SMO.BinarySMO[][] classifierArray = examineField(classifier,
					"m_classifiers");
			Instances instances = examineField(classifierArray[0][1], "m_data");
			List<Attribute> attributeList = examineField(instances,
					"m_Attributes");
			attributeNames = new ArrayList<>();
			for (Attribute a : attributeList) {
				attributeNames.add((String) attributeNameField.get(a));
			}
			System.out.println("attributeNames = " + attributeNames);
		} catch (NoSuchFieldException | SecurityException
				| IllegalArgumentException | IllegalAccessException e) {
			logService.log(LogService.LOG_INFO,
					"Exception thrown while inspecting serialized SMO object",
					e);
		}
	}

	private <T> T examineField(Object obj, String fieldName)
			throws NoSuchFieldException, IllegalAccessException {
		Field classAttributeField = obj.getClass().getDeclaredField(fieldName);
		classAttributeField.setAccessible(true);
		@SuppressWarnings("unchecked")
		T classAttribute = (T) classAttributeField.get(obj);
		return classAttribute;
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

	@Override
	public String getClassAttributeName() {
		return classAttributeName;
	}

	@Override
	public int getClassAttributeIndex() {
		return classAttributeIndex;
	}

	@Override
	public List<String> getClassAttributeValues() {
		return classAttributeValues;
	}

	@Override
	public List<String> getAttributeNames() {
		return attributeNames;
	}
}

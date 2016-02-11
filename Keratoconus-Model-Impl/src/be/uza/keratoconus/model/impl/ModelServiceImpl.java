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
import be.uza.keratoconus.model.api.AttributeNames;
import be.uza.keratoconus.model.api.ModelService;

@Component(configurationPolicy = ConfigurationPolicy.require)
public class ModelServiceImpl implements ModelService {

	private static final String CONFIG_FORMAT_VERSION = "config.format.version";
	private static final String CONFIG_PATH_PREFIX = "/config/";
	private static final String CONFIG_PATH_SUFFIX = ".properties";
	private static final String MODEL_PATH_PREFIX = "/model/";
	private static final String MODEL_PATH_SUFFIX = ".model";
	private static final String COMMA = ",";
	private static final String COLON = ":";
	private static final String SEMICOLON = ";";

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
		String formatVersion = extractFormatVersion(config);
		switch (formatVersion) {
		case "1.1":
			extractProperties_1_1(config);
			break;
		case "1.2":
			extractProperties_1_2(config);
			break;
		default:
			logService.log(LogService.LOG_ERROR, "Unknown configuration file version: "
					+ formatVersion + ", known versions are: 1.1, 1.2");
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
		return config;
	}

	private String extractFormatVersion(Properties config) {
		String formatVersion = (String) config.get(CONFIG_FORMAT_VERSION);
		if (formatVersion == null) {
			logService.log(LogService.LOG_WARNING, "No configuration file version found, defaulting to 1.1");
			return "1.1";
		}
		return formatVersion;
	}

	private void extractProperties_1_1(Properties config) {
		logService.log(LogService.LOG_INFO, "Configuration file version is 1.1");
		fileBaseNames = ((String) config.get("pentacam.files")).split(COMMA);
		keyFields = ((String) config.get("pentacam.fields.key")).split(COMMA);
		commonFields = ((String) config.get("pentacam.fields.common"))
				.split(COMMA);
		usedFields = ((String) config.get("pentacam.fields.used")).split(COMMA);
		for (String fbn : fileBaseNames) {
			separators.put(fbn, (String) config.get(fbn + ".field.separator"));
			fields.put(fbn, (String) config.get(fbn + ".fields"));
		}
	}

	private void extractProperties_1_2(Properties config) {
		logService.log(LogService.LOG_INFO, "Configuration file version is 1.2");
		keyFields = ((String) config.get("pentacam.fields.key")).split(COMMA);
		commonFields = ((String) config.get("pentacam.fields.common"))
				.split(COMMA);
		String[] usedFieldDescriptors = ((String) config.get("pentacam.fields.used")).split(COMMA);
		Map<String,String> checkFields = new HashMap<>();
		List<String> localUsedFields = new ArrayList<>();
		for (int i = 0; i < usedFieldDescriptors.length; ++i) {
			String descriptor = usedFieldDescriptors[i];
			int colon = descriptor.lastIndexOf(COLON);
			int semicolon = descriptor.indexOf(SEMICOLON);
			if (colon < 0) {
				logService.log(LogService.LOG_ERROR, "Error in pentacam.fields.used property: item " + descriptor + " does not contain a colon (:).').");
				return;
			}
			
			String fieldname;
			List<String> fieldAttributes = new ArrayList<>();
 			if (semicolon < 0) {
				fieldname = descriptor.substring(0,  colon);
			}
			else {
				fieldname = descriptor.substring(0,  semicolon);
				fieldAttributes = Arrays.asList(descriptor.substring(semicolon + 1, colon).split(SEMICOLON));
			}
 			System.out.println(fieldname + " attributes = " + fieldAttributes);
			if (!fieldAttributes.contains(AttributeNames.DISCRIMINATOR)) {
				localUsedFields.add(fieldname);
			}
			String filebasename = descriptor.substring(colon + 1);
			String check = checkFields.get(filebasename);
			if (check == null) {
				checkFields.put(filebasename, fieldname);
			}
			else {
				checkFields.put(filebasename,  check + "," + fieldname);
			}
		}
		
		for (String fbn : checkFields.keySet()) {
			System.out.println("checkFields[" + fbn + "] = " + checkFields.get(fbn));
			System.out.println("     fields[" + fbn + "] = " + config.get(fbn + ".fields"));
		}
		
		String[] localFileBaseNames = ((String) config.get("pentacam.files")).split(COMMA);
		for (String fbn : localFileBaseNames) {
			separators.put(fbn, (String) config.get(fbn + ".field.separator"));
			fields.put(fbn, (String) config.get(fbn + ".fields"));
		}
		usedFields = localUsedFields.toArray(new String[localUsedFields.size()]);
		fileBaseNames = localFileBaseNames;
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

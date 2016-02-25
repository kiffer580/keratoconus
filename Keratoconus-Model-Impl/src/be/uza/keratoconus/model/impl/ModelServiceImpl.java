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

import org.osgi.service.log.LogService;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.SingleClassifierEnhancer;
import weka.classifiers.functions.SMO;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.NominalAttributeInfo;
import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Reference;
import be.uza.keratoconus.model.api.Face;
import be.uza.keratoconus.model.api.FieldQualifierNames;
import be.uza.keratoconus.model.api.ModelService;

@Component(configurationPolicy = ConfigurationPolicy.require)
public class ModelServiceImpl implements ModelService {

	public static class IllegalSerializedModelException extends IllegalArgumentException {
		private static final long serialVersionUID = 1L;

		public IllegalSerializedModelException(AbstractClassifier classifier) {
			super("Serialized model is not of type weka.classifiers.functions.SMO or weka.classifiers.SingleClassifierEnhancer: " + classifier);
		}
	}
	
	private class FieldDescriptor {
		private String fieldname;
		private List<String> fieldQualifiers = new ArrayList<>();
		private String filebasename;

		private FieldDescriptor(String descriptor) {
			int colon = descriptor.lastIndexOf(ModelConstants.COLON);
			int semicolon = descriptor.indexOf(ModelConstants.SEMICOLON);
			if (colon < 0) {
				if (semicolon < 0) {
					fieldname = descriptor;
				} else {
					fieldname = descriptor.substring(0, semicolon);
					fieldQualifiers = Arrays.asList(descriptor.substring(
							semicolon + 1).split(ModelConstants.SEMICOLON));
				}
				// filebasename remains null in this case
			} else {
				if (semicolon < 0) {
					fieldname = descriptor.substring(0, colon);
				} else if (colon < semicolon) {
					logService.log(LogService.LOG_ERROR, "Error in pentacam.fields.used property: in item " + descriptor + " first colon (:) precedes first semicolon (;).");
				} else {
					fieldname = descriptor.substring(0, semicolon);
					fieldQualifiers = Arrays.asList(descriptor.substring(
							semicolon + 1, colon).split(ModelConstants.SEMICOLON));
				}
				filebasename = descriptor.substring(colon + 1);
			}
		}
		
		private String getFieldName() {
			return fieldname;
		}
		
		private List<String> getFieldQualifiers() {
			return fieldQualifiers;
		}
		
		private String getFileBaseName() {
			return filebasename;
		}
	}

	private class ModelAttributes {
		private final String classAttributeName;
		private final int classAttributeIndex;
		private final List<String> classAttributeValues;
		private final List<String> attributeNames;

		public ModelAttributes(AbstractClassifier classifier) throws NoSuchFieldException,
				SecurityException, IllegalArgumentException,
				IllegalAccessException {
			SMO smo;
			if (classifier instanceof SMO) {
				smo = (SMO) classifier;
			}
			else if (classifier instanceof SingleClassifierEnhancer) {
				smo = examineField(classifier, "m_Classifier");
			}
			else {
				throw new IllegalSerializedModelException(classifier);
			}
			
			Attribute classAttribute = examineField(smo,
					"m_classAttribute");
			classAttributeName = examineField(classAttribute, "m_Name");
			Field attributeNameField = Attribute.class
					.getDeclaredField("m_Name");
			attributeNameField.setAccessible(true);
			classAttributeIndex = examineField(classAttribute, "m_Index");
			NominalAttributeInfo info = examineField(classAttribute,
					"m_AttributeInfo");

			classAttributeValues = examineField(info, "m_Values");
			// not needed now but could turn out handy
			@SuppressWarnings("unused")
			Hashtable<String, Integer> classHashtable = examineField(info,
					"m_Hashtable");
			SMO.BinarySMO[][] classifierArray = examineField(smo,
					"m_classifiers");
			Instances instances = examineField(classifierArray[0][1], "m_data");
			List<Attribute> attributeList = examineField(instances,
					"m_Attributes");
			attributeNames = new ArrayList<>();
			for (Attribute a : attributeList) {
				attributeNames.add((String) attributeNameField.get(a));
			}
		}

		private String getClassAttributeName() {
			return classAttributeName;
		}

		private int getClassAttributeIndex() {
			return classAttributeIndex;
		}

		private List<String> getClassAttributeValues() {
			return classAttributeValues;
		}

		private List<String> getAttributeNames() {
			return attributeNames;
		}

	}
	
	private String[] fileBaseNames;
	private String[] keyFields;
	private String[] commonFields;
	private String[] usedFields;
	private Map<String, String> separators;
	private Map<String, String> fields;
	private LogService logService;
	private String modelName;
	private ModelAttributes modelAttributes;
	private AbstractClassifier classifier;

	@Reference
	protected void setLogService(LogService ls) {
		logService = ls;
	}

	@Activate
	protected void activate(Map<String, String> props) throws Exception {
		separators = new HashMap<>();
		fields = new HashMap<>();
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
		Object serializedObject = weka.core.SerializationHelper.read(getClass()
				.getResourceAsStream(
						ModelConstants.MODEL_PATH_PREFIX + modelName + ModelConstants.MODEL_PATH_SUFFIX));
		classifier = (AbstractClassifier) serializedObject;
		try {
			modelAttributes = new ModelAttributes(classifier);
			verifyProperties();
		} catch (NoSuchFieldException | SecurityException
				| IllegalArgumentException | IllegalAccessException e) {
			logService.log(LogService.LOG_INFO,
					"Exception thrown while inspecting serialized SMO object, skipping property checks.",
					e);
		}
	}

	private Properties readConfiguration() throws IOException {
		final InputStream stream = getClass().getResourceAsStream(
				ModelConstants.CONFIG_PATH_PREFIX + modelName + ModelConstants.CONFIG_PATH_SUFFIX);
		Properties config = new Properties();
		config.load(stream);
		return config;
	}

	private String extractFormatVersion(Properties config) {
		String formatVersion = (String) config.get(ModelConstants.CONFIG_FORMAT_VERSION);
		if (formatVersion == null) {
			logService.log(LogService.LOG_WARNING, "No configuration file version found, defaulting to 1.1");
			return "1.1";
		}
		return formatVersion;
	}

	private void extractProperties_1_1(Properties config) {
		logService.log(LogService.LOG_INFO, "Configuration file version is 1.1");
		fileBaseNames = ((String) config.get("pentacam.files")).split(ModelConstants.COMMA);
		keyFields = ((String) config.get("pentacam.fields.key")).split(ModelConstants.COMMA);
		commonFields = ((String) config.get("pentacam.fields.common"))
				.split(ModelConstants.COMMA);
		usedFields = ((String) config.get("pentacam.fields.used")).split(ModelConstants.COMMA);
		for (String fbn : fileBaseNames) {
			separators.put(fbn, (String) config.get(fbn + ".field.separator"));
			fields.put(fbn, (String) config.get(fbn + ".fields"));
		}
	}

	private void extractProperties_1_2(Properties config) {
		logService.log(LogService.LOG_INFO, "Configuration file version is 1.2");
		keyFields = ((String) config.get("pentacam.fields.key")).split(ModelConstants.COMMA);
		commonFields = ((String) config.get("pentacam.fields.common"))
				.split(ModelConstants.COMMA);
		// TODO replace checkFields by a Map<String,List<String>> with a more meaningful name
		Map<String,String> checkFields = new HashMap<>();
		Map<String,Map<String,List<String>>> fieldQualifiersMap = new HashMap<>();
		List<String> localUsedFields = new ArrayList<>();
		List<String> usedFieldDescriptors = Arrays.asList(((String) config.get("pentacam.fields.used")).split(ModelConstants.COMMA));
		for (String descriptor : usedFieldDescriptors) {
			FieldDescriptor fd = new FieldDescriptor(descriptor);
			if (!fd.getFieldQualifiers().contains(FieldQualifierNames.DISCRIMINATOR)) {
				localUsedFields.add(fd.getFieldName());
			}
			String fbn = fd.getFileBaseName();
			if (fbn == null) {
				logService.log(LogService.LOG_ERROR, "Error in pentacam.fields.used property: item " + descriptor + " does not contain a colon (:).");
				return;
			}
			String check = checkFields.get(fd.getFileBaseName());
			Map<String,List<String>> filequals;
			if (check == null) {
				checkFields.put(fbn, fd.getFieldName());
				filequals = new HashMap<>();
				fieldQualifiersMap.put(fbn, filequals);
			}
			else {
				checkFields.put(fbn,  check + "," + fd.getFieldName());
				filequals = fieldQualifiersMap.get(fbn);
			}
			filequals.put(fd.getFieldName(), fd.getFieldQualifiers());
		}
		
		String[] localFileBaseNames = ((String) config.get("pentacam.files")).split(ModelConstants.COMMA);
		for (String fbn : localFileBaseNames) {
			separators.put(fbn, (String) config.get(fbn + ".field.separator"));
			StringBuilder sb = new StringBuilder();
			for (String fieldname : checkFields.get(fbn).split(ModelConstants.COMMA)) {
				sb.append(fieldname);
				for (String attr : fieldQualifiersMap.get(fbn).get(fieldname)) {
					sb.append(ModelConstants.SEMICOLON);
					sb.append(attr);
				}
				sb.append(ModelConstants.COMMA);
			}
			sb.setLength(sb.length() - 1);
			fields.put(fbn, sb.toString());
		}
		usedFields = localUsedFields.toArray(new String[localUsedFields.size()]);
		fileBaseNames = localFileBaseNames;
	}

	@SuppressWarnings("unchecked")
	private <T> T examineField(Object obj, String fieldName)
			throws NoSuchFieldException, IllegalAccessException {
		T fieldValue;
		NoSuchFieldException exception = new NoSuchFieldException(fieldName);
		Class<? extends Object> objClass = obj.getClass();
		while (objClass != Object.class) {
			try {
				Field f = objClass.getDeclaredField(fieldName);
				f.setAccessible(true);
				fieldValue = (T) f.get(obj);
				return fieldValue;				
			}
			catch (NoSuchFieldException nsfe) {
				exception = nsfe;
				objClass = objClass.getSuperclass();
			}
		}
		throw exception;
	}

	private void verifyProperties() {
		List<String> expectedFieldNames = new ArrayList<>(modelAttributes.getAttributeNames());
		for (int i = 0; i < expectedFieldNames.size(); ++i) {
			expectedFieldNames.set(i, normalizeAttributeName(expectedFieldNames.get(i)));
		}
		for (String filedesc : fields.values()) {
			for (String descriptor : filedesc.split(ModelConstants.COMMA)) {
				FieldDescriptor fd = new FieldDescriptor(descriptor);
				if (fd.getFieldQualifiers().contains(FieldQualifierNames.DISCRIMINATOR)) {
					continue;
				}
				if (fd.getFieldQualifiers().contains(FieldQualifierNames.BIFACIAL)) {
					if (!expectedFieldNames.remove(fd.getFieldName() + " " + Face.FRONT)) {
						fieldNotInModelWarning(fd.getFieldName() + " " + Face.FRONT);
					}
					if (!expectedFieldNames.remove(fd.getFieldName() + " " + Face.BACK)) {
						fieldNotInModelWarning(fd.getFieldName() + " " + Face.BACK);
					}
				}
				else if (!expectedFieldNames.remove(fd.getFieldName())) {
					fieldNotInModelWarning(fd.getFieldName());
				}
			}
		}
		for (String name : expectedFieldNames) {
			if (!name.equals(modelAttributes.getClassAttributeName())) {
				missingFieldFromModelError(name);
			}
		}
	}
	
	private void fieldNotInModelWarning(String name) {
		logService.log(LogService.LOG_WARNING, "Configuration file " + ModelConstants.CONFIG_PATH_PREFIX + modelName + ModelConstants.CONFIG_PATH_SUFFIX + " defines field " + name + " which is not used in model.");
	}

	private void missingFieldFromModelError(String name) {
		logService.log(LogService.LOG_ERROR, "Configuration file " + ModelConstants.CONFIG_PATH_PREFIX + modelName + ModelConstants.CONFIG_PATH_SUFFIX + " does not define field " + name + " which is used in the model.");
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
	public AbstractClassifier getClassifier() throws Exception {
		return classifier;
	}

	@Override
	public SMO getSMO() throws Exception {
		if (classifier instanceof SMO) {
			return (SMO) classifier;
		}
		if (classifier instanceof SingleClassifierEnhancer) {
			return examineField(classifier, "m_Classifier");
		}
		throw new IllegalSerializedModelException(classifier);
	}

	@Override
	public String getClassAttributeName() {
		return modelAttributes == null ? null : modelAttributes.getClassAttributeName();
	}

	@Override
	public int getClassAttributeIndex() {
		return modelAttributes == null ? null : modelAttributes.getClassAttributeIndex();
	}

	@Override
	public List<String> getClassAttributeValues() {
		return modelAttributes == null ? null : modelAttributes.getClassAttributeValues();
	}

	@Override
	public List<String> getAttributeNames() {
		return modelAttributes == null ? null : modelAttributes.getAttributeNames();
	}
	
	@Override
	public String normalizeAttributeName(String s) {
		StringBuilder sb = new StringBuilder(s);

		while (sb.length() > 0) {
			final int trailingMinus = sb.indexOf("- ");
			if (trailingMinus > 0) {
				sb.deleteCharAt(trailingMinus);
				continue;
			}
			final int lengthMinusOne = sb.length() - 1;
			final char lastChar = sb.charAt(lengthMinusOne);
			if (lastChar == ':' || Character.isWhitespace(lastChar)) {
				sb.setLength(lengthMinusOne);
			} else {
				break;
			}
		}
		return sb.toString();
	}

	@Override
	public String getModelName() {
		return modelName;
	}


}

package be.uza.keratoconus.model.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import be.uza.keratoconus.model.api.ModelFileService;
import be.uza.keratoconus.model.api.ModelService;

@Component
public class ModelFileServiceImpl implements ModelFileService {

	private String[] fileBaseNames;
	private Map<String, String> separators;
	private Map<String, List<String>> fields;
	private LogService logService;
	ModelService modelService;
	private String modelName;

	@Reference
	protected void setLogService(LogService ls) {
		logService = ls;
	}

	@Reference
	protected void setModelService(ModelService ms) {
		modelService = ms;
	}

	@Activate
	protected void activate(Map<String, String> props) throws Exception {
		modelName = modelService.getModelName();
		separators = new HashMap<>();
		fields = new HashMap<>();
		Properties config = readConfiguration();
		extractProperties(config);
	}

	private Properties readConfiguration() throws IOException {
		final InputStream stream = getClass().getResourceAsStream(
				ModelConstants.CONFIG_PATH_PREFIX + modelName
						+ ModelConstants.CONFIG_PATH_SUFFIX);
		Properties config = new Properties();
		config.load(stream);
		return config;
	}

	private void extractProperties(Properties config) {
		fileBaseNames = ((String) config.get("pentacam.files"))
				.split(ModelConstants.COMMA);
		for (String fbn : fileBaseNames) {
			separators.put(fbn, (String) config.get(fbn + ".field.separator"));
			fields.put(fbn, Arrays.asList(modelService.getFieldsOfFile(fbn).split(ModelConstants.COMMA)));
		}
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
	public List<String> getFieldsOfFile(String fbn) {
		return fields.get(fbn);
	}

}

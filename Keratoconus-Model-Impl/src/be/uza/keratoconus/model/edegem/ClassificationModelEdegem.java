package be.uza.keratoconus.model.edegem;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import be.uza.keratoconus.model.api.ClassificationModelService;

@Component(properties = "name=edegem")
public class ClassificationModelEdegem implements ClassificationModelService {

	private static final String COMMA = ",";

	private String[] fileBaseNames;
	private String[] keyFields;
	private String[] commonFields;
	Map<String, String> separators = new HashMap<>();
	Map<String, String> fields = new HashMap<>();

	@Activate
	protected void activate() throws IOException {
		InputStream stream = getClass().getResourceAsStream("/config/edegem.properties");
		Properties config = new Properties();
		config.load(stream);
		System.out.println(config);
		fileBaseNames = ((String) config.get("pentacam.files")).split(COMMA);
		keyFields = ((String) config.get("pentacam.fields.key")).split(COMMA);
		commonFields = ((String) config.get("pentacam.fields.common")).split(COMMA);
		for (String fbn : fileBaseNames) {
			separators.put(fbn, (String) config.get(fbn + ".field.separator"));
			fields.put(fbn, (String) config.get(fbn + ".fields"));
		}
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
}

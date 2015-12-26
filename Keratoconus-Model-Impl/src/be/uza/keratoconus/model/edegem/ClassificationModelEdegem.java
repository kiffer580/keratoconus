package be.uza.keratoconus.model.edegem;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Reference;
import be.uza.keratoconus.model.api.ClassificationModelService;

@Component(properties = "name=edegem", configurationPolicy = ConfigurationPolicy.require)
public class ClassificationModelEdegem implements ClassificationModelService {

	private static final String COMMA = ",";

	private String[] fileBaseNames;
	private String[] keyFields;
	private String[] commonFields;
	Map<String, String> separators = new HashMap<>();
	Map<String, String> fields = new HashMap<>();

	@Activate
	protected void activate(Map<String, Object> props) throws IOException {
		fileBaseNames = ((String) props.get("pentacam.files")).split(COMMA);
		keyFields = ((String) props.get("pentacam.fields.key")).split(COMMA);
		commonFields = ((String) props.get("pentacam.fields.common")).split(COMMA);
		for (String fbn : fileBaseNames) {
			separators.put(fbn, (String) props.get(fbn + ".field.separator"));
			fields.put(fbn, (String) props.get(fbn + ".fields"));
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

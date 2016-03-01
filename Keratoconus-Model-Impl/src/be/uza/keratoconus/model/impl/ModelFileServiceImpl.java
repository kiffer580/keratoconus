/*
    This file is part of Keratoconus Assistant.

    Keratoconus Assistant is free software: you can redistribute it 
    and/or modify it under the terms of the GNU General Public License 
    as published by the Free Software Foundation, either version 3 of 
    the License, or (at your option) any later version.

    Keratoconus Assistant is distributed in the hope that it will be 
    useful, but WITHOUT ANY WARRANTY; without even the implied warranty 
    of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Keratoconus Assistant.  If not, see 
    <http://www.gnu.org/licenses/>.
 */

package be.uza.keratoconus.model.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
	ModelService modelService;
	private String modelName;

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

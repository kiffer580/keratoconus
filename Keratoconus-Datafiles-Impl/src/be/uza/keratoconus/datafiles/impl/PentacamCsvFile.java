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

package be.uza.keratoconus.datafiles.impl;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;
import be.uza.keratoconus.configuration.api.PentacamConfigurationService;
import be.uza.keratoconus.datafiles.api.PentacamField;
import be.uza.keratoconus.datafiles.api.PentacamFile;
import be.uza.keratoconus.model.api.Face;
import be.uza.keratoconus.model.api.ModelService;

import com.opencsv.CSVParser;

/**
 * Implementation of {@link PentacamFile} based on a CSV file.
 * 
 * @author Chris Gray
 *
 */
@Component(configurationPolicy = ConfigurationPolicy.require, immediate = true, properties = "pentacam.file.format=CSV")
public class PentacamCsvFile implements PentacamFile {

	private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
	private static final String CSV = ".CSV";
	private final Map<String, String[]> records = new LinkedHashMap<String, String[]>();
	private String fileName;
	private ModelService classificationModelService;
	private PentacamConfigurationService pentacamConfigurationService;
	private Path directoryPath;
	private Config config;
	private final List<PentacamField> allFields = new ArrayList<PentacamField>();
	private final Map<PentacamField, Integer> commonFieldsMap = new LinkedHashMap<PentacamField, Integer>();
	private final Map<PentacamField, Integer> usedFieldsMap = new LinkedHashMap<PentacamField, Integer>();
	private PentacamField bifacialDiscriminator;
	private int bifacialDiscriminatorIndex = -1;
	private char keyMemberSeparator;
	private List<Integer> keyIndices;
	private LogService logService;
	private Path filePath;
	private char fieldSeparator;
	private ComponentContext ownComponentContext;
	private CSVParser csvParser;

	@Meta.OCD
	interface Config {
		@Meta.AD(required = true, description = "The base name of the CSV file, without the directory path or the .csv extension.")
		String pentacam_file_name();

		@Meta.AD(required = false, description = "The field separator used in this file (e.g. , or ;).")
		String pentacam_field_separator();

		@Meta.AD(required = true, description = "List of fields in this file which are used by the application.")
		List<String> pentacam_fields();
	}

	@Reference
	protected void setLogService(LogService logService) {
		this.logService = logService;
	}

	@Reference
	protected void setClassificationModelService(ModelService cms) {
		classificationModelService = cms;
	}

	@Reference
	protected void setPentacamConfigurationService(
			PentacamConfigurationService pcs) {
		pentacamConfigurationService = pcs;
	}

	@Activate
	protected void activate(ComponentContext cc, Map<String, String> props)
			throws IOException {
		ownComponentContext = cc;
		config = Configurable.createConfigurable(Config.class, props);
		fileName = config.pentacam_file_name();
		directoryPath = pentacamConfigurationService.getPentacamDirectoryPath();
		keyMemberSeparator = pentacamConfigurationService
				.getKeyMemberSeparator();
		filePath = directoryPath.resolve(fileName + CSV);
		logService.log(LogService.LOG_INFO,
				"Activating PentacamCsvFile instance for " + filePath);

		final String pentacam_field_separator = config
				.pentacam_field_separator();
		if (pentacam_field_separator != null) {
			fieldSeparator = pentacam_field_separator.charAt(0);
		}
	}

	private BufferedReader parseHeaders() throws IOException,
			URISyntaxException {
		long deadline = System.currentTimeMillis() + 1000L;
		String headerLine = null;
		// Opening the file can quite easily fail because the Pentacam software
		// has not yet finished closing it, so we build in some retries.
		while (headerLine == null) {
			try {
				BufferedReader reader = Files.newBufferedReader(filePath,
					WINDOWS_1252);
				headerLine = reader.readLine();
				int countCommas = headerLine.split(",").length - 1;
				int countSemicolons = headerLine.split(";").length - 1;
				if (countCommas > countSemicolons) {
					fieldSeparator = ',';
				} else if (countSemicolons > countCommas) {
					fieldSeparator = ';';
				} else {
					logService
							.log(ownComponentContext.getServiceReference(),
									LogService.LOG_ERROR,
									fileName
											+ "  field delimiter is neither ',' nor ';' in header : "
											+ headerLine);
					reader.close();
					return null;
				}

				final Map<String, PentacamFieldImpl> fieldMap = new LinkedHashMap<String, PentacamFieldImpl>();
				final List<String> fieldDescriptors = config.pentacam_fields();
				for (final String desc : fieldDescriptors) {
					final PentacamFieldImpl pf = new PentacamFieldImpl(desc);
					final String name = pf.getName();
					fieldMap.put(name, pf);
					if (pf.isDiscriminator()) {
						bifacialDiscriminator = pf;
					}
				}
				
				csvParser = new CSVParser(fieldSeparator);
				final List<String> commonFieldNames = Arrays
						.asList(classificationModelService.getCommonFields());
				final List<String> keyFieldNames = Arrays
						.asList(classificationModelService.getKeyFields());
				String[] header = csvParser.parseLine(headerLine);
				readCsvHeaders(fieldMap, commonFieldNames, keyFieldNames, header);
				Map<String, Integer> name2index = new HashMap<>();
				for (int i = 0; i < allFields.size(); ++i) {
					final PentacamField pf = allFields.get(i);
					name2index.put(pf.getName(), i);
				}
				for (final String desc : fieldDescriptors) {
					final PentacamFieldImpl pf = new PentacamFieldImpl(desc);
					final String name = pf.getName();
					if (pf.isUsed()) {
						usedFieldsMap.put(pf, name2index.get(name));
					}
				}
				return reader;

			} catch (IOException e) {
				if (System.currentTimeMillis() >= deadline) {
					throw e;
				}
				logService.log(ownComponentContext.getServiceReference(),
						LogService.LOG_INFO,
						"Exception thrown while trying to guess field separator in file "
								+ fileName + ", retrying", e);
				takeANap();
			}
		}

		return null;
	}

	private void takeANap() {
		try {
			Thread.sleep(50L);
		} catch (InterruptedException e) {
		}
	}

	private List<String[]> readCsvFile() throws IOException, URISyntaxException {
		List<String[]> newRecords = new ArrayList<String[]>();
		try (BufferedReader reader = parseHeaders()) {
			String line = reader.readLine();
			while (line != null) {
				final String[] r = csvParser.parseLine(line);
				String key = extractKey(r);
				if (key != null) {
					if (bifacialDiscriminatorIndex >= 0) {
						records.put(key + keyMemberSeparator
								+ r[bifacialDiscriminatorIndex], r);
					} else {
						records.put(key, r);
					}
				}
				newRecords.add(r);
				line = reader.readLine();
			}
			logService.log(ownComponentContext.getServiceReference(),
					LogService.LOG_INFO, "Read " + records.size()
							+ " records from file " + fileName);
		} catch (Exception e) {
			logService.log(ownComponentContext.getServiceReference(),
					LogService.LOG_WARNING, "Unable to read " + filePath, e);
		}
		return newRecords;
	}

	private void readCsvHeaders(final Map<String, PentacamFieldImpl> fieldMap, final List<String> commonFieldNames,
			final List<String> keyFieldNames, String[] header)
			throws IOException, URISyntaxException {
		final List<String> fieldNames = new ArrayList<String>();
		for (int i = 0; i < header.length; ++i) {
			String h = header[i];
			h = classificationModelService.normalizeAttributeName(h);
			fieldNames.add(h);
			PentacamFieldImpl pf = fieldMap.remove(h);
			if (pf == null) {
				pf = fieldMap.remove(h + " " + Face.FRONT);
			}
			if (pf == null) {
				pf = fieldMap.remove(h + " " + Face.BACK);
			}
			if (pf == null) {
				allFields.add(new PentacamFieldImpl(h, false, false));
			} else {
				allFields.add(pf);
				if (pf.equals(bifacialDiscriminator)) {
					bifacialDiscriminatorIndex = i;
				}
			}
		}

		for (String f : commonFieldNames) {
			final int fieldIndex = fieldNames.indexOf(f);
			if (fieldIndex >= 0) {
				PentacamFieldImpl pf = new PentacamFieldImpl(f,
						keyFieldNames.contains(f), true);
				commonFieldsMap.put(pf, fieldIndex);
			}
		}

		if (!fieldMap.isEmpty()) {
			String homeDirectory = System.getProperty("user.dir").replace('\\',
					'/');
			URI manual6uri = new URI("file", "", "/" + homeDirectory
					+ "/html/manual6.html", null, null);
			if (Desktop.isDesktopSupported()) {
				Desktop.getDesktop().browse(manual6uri);
			} else {
				Runtime.getRuntime().exec(
						"cmd /k start " + manual6uri.toASCIIString());
			}
			logService
					.log(ownComponentContext.getServiceReference(),
							LogService.LOG_ERROR,
							"Fields "
									+ fieldMap.keySet()
									+ " not found in file "
									+ fileName
									+ ".\n"
									+ "Probably your Pentacam software is not correctly configured - see chapter 6 \"Pentacam configuration\" of the User Manual for more information.");
		}

		keyIndices = new ArrayList<Integer>();
		for (String aKeyField : keyFieldNames) {
			int index = fieldNames.indexOf(aKeyField);
			if (index < 0) {
				logService.log(ownComponentContext.getServiceReference(),
						LogService.LOG_WARNING, "Key field " + aKeyField
								+ " not found in file " + fileName);
			}
			keyIndices.add(index);
		}
	}

	private List<String[]> readNewRecords(long startOffset) throws IOException {
		List<String[]> newRecords = new ArrayList<String[]>();
		CSVParser csvParser = new CSVParser(fieldSeparator);
		InputStream is = openInputStream();

		try {
			is.skip(startOffset);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					is, WINDOWS_1252));
			String line = reader.readLine();
			while (line != null) {
				final String[] r = csvParser.parseLine(line);
				String key = extractKey(r);
				if (key != null) {
					if (bifacialDiscriminatorIndex >= 0) {
						records.put(key + keyMemberSeparator
								+ r[bifacialDiscriminatorIndex], r);
					} else {
						records.put(key, r);
					}
				}
				newRecords.add(r);
				line = reader.readLine();
			}
			logService.log(ownComponentContext.getServiceReference(),
					LogService.LOG_INFO, "Read " + records.size()
							+ " new records from file " + fileName);
			return newRecords;
		} finally {
			is.close();
		}
	}

	private InputStream openInputStream() throws IOException {
		long deadline = System.currentTimeMillis() + 1000L;
		while (true) {
			try {
				return Files.newInputStream(filePath);
			} catch (IOException ie) {
				if (System.currentTimeMillis() > deadline) {
					throw ie;
				}
			}
		}
	}

	@Override
	public String getBaseName() {
		return fileName;
	}

	@Override
	public List<String[]> getAllRecords() {
		return new ArrayList<String[]>(records.values());
	}

	@Override
	public String[] getRecord(String key) {
		return records.get(key);
	}

	@Override
	public String[] getRecord(String key, String discriminator) {
		return discriminator == null ? records.get(key) : records.get(key
				+ keyMemberSeparator + discriminator);
	}

	@Override
	public List<PentacamField> getAllFields() {
		return allFields;
	}

	@Override
	public boolean isBifacial() {
		return bifacialDiscriminator != null;
	}

	@Override
	public PentacamField getBifacialDiscriminator() {
		return bifacialDiscriminator;
	}

	@Override
	public String extractKey(String[] record) {
		StringBuilder sb = new StringBuilder();
		boolean empty = true;
		for (int i : keyIndices) {
			empty &= record[i].isEmpty();
			sb.append(record[i]);
			sb.append(keyMemberSeparator);
		}
		sb.setLength(sb.length() - 1);
		return empty ? null : sb.toString();
	}

	@Override
	public Map<PentacamField, Integer> getCommonFieldsMap() {
		return commonFieldsMap;
	}

	@Override
	public Map<PentacamField, Integer> getUsedFieldsMap() {
		return usedFieldsMap;
	}

	@Override
	public long getCurrentLength() throws IOException {
		if (!Files.exists(filePath)) {
			return -1;
		}
		return Files.size(filePath);
	}

	@Override
	public List<String[]> getNewRecords(long startOffset) {
		try {
			if (keyIndices == null) {
				// fieldSeparator = guessFieldSeparator();
				return readCsvFile();
			}
			return readNewRecords(startOffset);
		} catch (Exception e) {
			e.printStackTrace();
			logService.log(ownComponentContext.getServiceReference(),
					LogService.LOG_WARNING,
					"Unable to read new records in file " + fileName, e);
			return Collections.emptyList();
		}
	}

	@Override
	public String toString() {
		return "PentacamCsvFile [ fileName="
				+ fileName
				+ ", directoryPath="
				+ directoryPath
				+ (bifacialDiscriminator == null ? ""
						: ", bifacial with discriminator "
								+ bifacialDiscriminator.getName())
				+ ", allFields=" + allFields + ", usedFields=" + usedFieldsMap
				+ ", " + records.size() + " records ]";
	}

}

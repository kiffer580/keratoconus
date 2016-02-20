package be.uza.keratoconus.datafiles.impl;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	private static final String CONFIG_FILE_LIST_SEPARATOR = ",";
	private static final String CSV = ".CSV";
	private final Map<String, String[]> records = new LinkedHashMap<String, String[]>();
	private String fileName;
	private ModelService classificationModelService;
	private PentacamConfigurationService pentacamConfigurationService;
	private Path directoryPath;
	private Config config;
	private final List<PentacamField> allFields = new ArrayList<PentacamField>();
	private final List<PentacamField> commonFields = new ArrayList<PentacamField>();
	private final List<PentacamField> usedFields = new ArrayList<PentacamField>();
	private PentacamField bifacialDiscriminator;
	private int bifacialDiscriminatorIndex = -1;
	private char keyMemberSeparator;
	private List<Integer> keyIndices;
	private LogService logService;
	private Path filePath;
	private char fieldSeparator;
	private Map<String, PentacamFieldImpl> fieldMap;
	private ComponentContext ownComponentContext;

	@Meta.OCD
	interface Config {
		@Meta.AD(required = true, description = "The base name of the CSV file, without the directory path or the .csv extension.")
		String pentacam_file_name();

		@Meta.AD(required = false, description = "The field separator used in this file (e.g. , or ;).")
		String pentacam_field_separator();

		@Meta.AD(required = true, description = "List of fields in this file which are used by the application.")
		String pentacam_fields();
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
	protected void setPentacamConfigurationService(PentacamConfigurationService pcs) {
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
		logService.log(LogService.LOG_INFO, "Activating PentacamCsvFile instance for " + filePath);

		fieldMap = new LinkedHashMap<String, PentacamFieldImpl>();
		final String[] fieldDescriptors = config.pentacam_fields().split(
				CONFIG_FILE_LIST_SEPARATOR);
		for (String desc : fieldDescriptors) {
			final PentacamFieldImpl pf = new PentacamFieldImpl(desc);
			final String name = pf.getName();
			fieldMap.put(name, pf);
			if (pf.isDiscriminator()) {
				bifacialDiscriminator = pf;
			}
			if (pf.isUsed()) {
				usedFields.add(pf);
			}
		}

		final String pentacam_field_separator = config
				.pentacam_field_separator();
		if (pentacam_field_separator != null) {
			fieldSeparator = pentacam_field_separator.charAt(0);
		}
	}

	private char guessFieldSeparator() {
		long deadline = System.currentTimeMillis() + 1000L;
		while (true) {
			try (BufferedReader reader = Files.newBufferedReader(filePath,
					WINDOWS_1252)) {
				String headerLine = reader.readLine();
				int countCommas = headerLine.split(",").length - 1;
				int countSemicolons = headerLine.split(";").length - 1;
				if (countCommas > 1 && countCommas > countSemicolons) {
					return ',';
				}
				if (countSemicolons > 1 && countSemicolons > countCommas) {
					return ';';
				}
				logService.log(ownComponentContext.getServiceReference(), LogService.LOG_ERROR, fileName +
						"  field delimiter is neither ',' nor ';' in header : "  + headerLine);
				return 0;
			} catch (IOException e) {
				if (System.currentTimeMillis() >= deadline) {
					logService.log(ownComponentContext.getServiceReference(), LogService.LOG_ERROR, "Exception thrown while trying to guess field separator in file " + fileName, e);
					// in failure cases we return the null character
					return 0;
				}
				logService.log(ownComponentContext.getServiceReference(), LogService.LOG_INFO, "Exception thrown while trying to guess field separator in file " + fileName + ", retrying", e);
				takeANap();
			}
		}

	}

	private void takeANap() {
		try {
			Thread.sleep(50L);
		} catch (InterruptedException e) {
		}
	}

	private List<String[]> readCsvFile() {
		List<String[]> newRecords = new ArrayList<String[]>();
		final List<String> commonFieldNames = Arrays
				.asList(classificationModelService.getCommonFields());
		final List<String> keyFieldNames = Arrays
				.asList(classificationModelService.getKeyFields());
		if (fieldSeparator == 0) {
			fieldSeparator = guessFieldSeparator();
		}
		CSVParser csvParser = new CSVParser(fieldSeparator);
		try (BufferedReader reader = Files.newBufferedReader(filePath,
				WINDOWS_1252)) {
			final List<String> fieldNames = new ArrayList<String>();
			String[] header = csvParser.parseLine(reader.readLine());
			for (int i = 0; i < header.length; ++i) {
				String h = header[i];
				h = classificationModelService.normalizeAttributeName(h);
				fieldNames.add(h);
				PentacamFieldImpl pf = fieldMap.remove(h);
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
				if (fieldNames.contains(f)) {
					commonFields.add(new PentacamFieldImpl(f, keyFieldNames
							.contains(f), true));
				}
			}

			if (!fieldMap.isEmpty()) {
				final Set<String> fields = new HashSet<>();
				for (PentacamFieldImpl pfi : fieldMap.values()) {
					fields.add(pfi.getName());
				}
				String homeDirectory = System.getProperty("user.dir").replace('\\', '/');
				URI manual6uri = new URI("file", "",
						"/" + homeDirectory + "/html/manual6.html", null, null);
				if (Desktop.isDesktopSupported()) {
					Desktop.getDesktop().browse(manual6uri);
				}
				else {
					Runtime.getRuntime().exec( "cmd /k start " + manual6uri.toASCIIString());
				}
				logService.log(ownComponentContext.getServiceReference(), LogService.LOG_ERROR, "Fields " + fields + " not found in file " + fileName + ".\n" +
						"Probably your Pentacam software is not correctly configured - see chapter 6 \"Pentacam configuration\" of the User Manual for more information.");
//				eventAdmin.postEvent(new ShowPageEvent("/html/manual6.html", pentacamConfigurationService.getApplicationTitle() + " - User Manual"));
			}

			keyIndices = new ArrayList<Integer>();
			for (String aKeyField : keyFieldNames) {
				int index = fieldNames.indexOf(aKeyField);
				if (index < 0) {
					logService.log(ownComponentContext.getServiceReference(), LogService.LOG_WARNING, "Key field " + aKeyField + " not found in file " + fileName);
				}
				keyIndices.add(index);
			}

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
			logService.log(ownComponentContext.getServiceReference(), LogService.LOG_INFO, "Read " + records
					.size() + " records from file " + fileName);
		} catch (Exception e) {
			logService.log(ownComponentContext.getServiceReference(), LogService.LOG_WARNING, "Unable to read " + filePath, e);
		}
		return newRecords;
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
			logService.log(ownComponentContext.getServiceReference(), LogService.LOG_INFO, "Read " + records
					.size() + " new records from file " + fileName);
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
	public List<PentacamField> getCommonFields() {
		return commonFields;
	}

	@Override
	public List<PentacamField> getUsedFields() {
		return usedFields;
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
				fieldSeparator = guessFieldSeparator();
				return readCsvFile();
			}
			return readNewRecords(startOffset);
		} catch (Exception e) {
			e.printStackTrace();
			logService.log(ownComponentContext.getServiceReference(), LogService.LOG_WARNING, "Unable to read new records in file " + fileName, e);
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
				+ ", allFields=" + allFields + ", usedFields=" + usedFields
				+ ", " + records.size() + " records ]";
	}

}

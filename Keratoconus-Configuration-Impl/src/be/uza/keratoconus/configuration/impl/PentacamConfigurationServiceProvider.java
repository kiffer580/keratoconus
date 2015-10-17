package be.uza.keratoconus.configuration.impl;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;
import be.uza.keratoconus.configuration.api.Classification;
import be.uza.keratoconus.configuration.api.Classification.Category;
import be.uza.keratoconus.configuration.api.PentacamConfigurationService;

@Component(configurationPolicy = ConfigurationPolicy.require)
public class PentacamConfigurationServiceProvider implements
		PentacamConfigurationService {

	@Meta.OCD
	interface Config {
		@Meta.AD(required = false, deflt = "Keratoconus Assistant", description = "Title to be used on window bars, tooltips, etc..")
		String application_title();

		@Meta.AD(required = true, description = "Directory in which the Pentacam files are stored.")
		String pentacam_directory();

		@Meta.AD(required = true, description = "Directory in which the user preference files are stored.")
		String userpref_directory();

		@Meta.AD(required = true, description = "Directory in which the log files are stored.")
		String logging_directory();

		@Meta.AD(required = true, description = "Lowest log level which will be written to file.")
		String logging_level();

		@Meta.AD(required = true, description = "Format of the Pentacam files to be used (CSV or DIFF).", optionLabels = {
				"CSV", "DIFF" })
		String pentacam_format();

		@Meta.AD(required = true, description = "Comma-separated list of the base names of the files to be used (without the directory path or the extension).")
		String pentacam_files();

		@Meta.AD(required = true, description = "Comma-separated list of the field names which are common to all files.")
		String pentacam_fields_common();

		@Meta.AD(required = true, description = "Comma-separated list of the field names which are used to construct a unique key for each patient examination.")
		String pentacam_fields_key();

		@Meta.AD(required = false, deflt = "|", description = "Single-character separator used to concatenate key elements.")
		String pentacam_key_separator();

		@Meta.AD(required = true, description = "Comma-separated list of classifications.  Each entry consists of the internal name of the classification followed by a semicolon and one of 'normal', 'side', 'ambiguous', or 'main'.")
		String classifications();

		@Meta.AD(required = false, deflt = "0.666666667", description = "Probability above which an indication is considered to be \"strong\".")
		double threshold_headline();

		@Meta.AD(required = false, deflt = "600", description = "Thickness (microns) above which a cornea is considered to be \"thick\".")
		double threshold_thick_cornea();

		@Meta.AD(required = true, description = "Path to the base icon, relative to directory 'resource/' within the systemtrayapp bundle.")
		String icon_path_base();
	}

	private Config config;
	private String applicationTitle;
	private Path pentacamDirectoryPath;
	private Path prefsDirectoryPath;
	private Path loggingDirectoryPath;
	private int logLevel;
	private String[] fileBaseNames;
	private String[] keyFields;
	private String[] commonFields;
	private char keyMemberSeparator;
	private String classifications;
	private double headlineThreshold;
	private double thickCorneaThreshold;
	private Map<String, String> headlines;

	@Activate
	protected void activate(Map<String, Object> props) throws IOException {
		config = Configurable.createConfigurable(Config.class, props);
		applicationTitle = config.application_title();
		pentacamDirectoryPath = getDirectoryPath(config.pentacam_directory());
		prefsDirectoryPath = getDirectoryPath(config.userpref_directory());
		loggingDirectoryPath = getDirectoryPath(config.logging_directory());
		logLevel = interpretLogLevel(config.logging_level());
		fileBaseNames = Utils.splitOnComma(config.pentacam_files());
		keyFields = Utils.splitOnComma(config.pentacam_fields_key());
		commonFields = Utils.splitOnComma(config.pentacam_fields_common());
		keyMemberSeparator = config.pentacam_key_separator().charAt(0);
		classifications = config.classifications();
		headlineThreshold = config.threshold_headline();
		thickCorneaThreshold = config.threshold_thick_cornea();
		headlines = new HashMap<>();
		for (Map.Entry<String, Object> entry : props.entrySet()) {
			String key = entry.getKey();
			if (key.startsWith("headline.")) {
				String headlineKey = key.substring("headline.".length());
				headlines.put(headlineKey, (String) entry.getValue());
			}
		}
	}

	private Path getDirectoryPath(String pathString) {
		return FileSystems.getDefault().getPath(
				pathString.replace("LOCALAPPDATA",
						System.getenv("LOCALAPPDATA")));
	}

	private int interpretLogLevel(String s) {
		try {
			Field field = LogService.class.getDeclaredField("LOG_"
					+ s.toUpperCase());
			return field.getInt(null);
		} catch (NoSuchFieldException | SecurityException
				| IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			return LogService.LOG_WARNING;
		}
	}

	@Override
	public String getApplicationTitle() {
		return applicationTitle;
	}

	@Override
	public Path getPentacamDirectoryPath() {
		return pentacamDirectoryPath;
	}

	@Override
	public Path getLoggingDirectoryPath() {
		return loggingDirectoryPath;
	}

	@Override
	public Path getUserPrefsDirectoryPath() {
		return prefsDirectoryPath;
	}

	@Override
	public int getLogLevel() {
		return logLevel;
	}

	@Override
	public String[] getFileBaseNames() {
		return fileBaseNames;
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
	public char getKeyMemberSeparator() {
		return keyMemberSeparator;
	}

	@Override
	public Map<String, Classification> getClassifications() {
		String[] elements = classifications.split(",");
		Map<String, Classification> result = new HashMap<>();
		for (String s : elements) {
			int semi = s.indexOf(';');
			String name = semi < 0 ? s : s.substring(0, semi);
			result.put(name, new Classification(s));
		}
		result.put(Classification.AMBIGUOUS, new Classification(
				Classification.AMBIGUOUS, Category.AMBIGUOUS));
		result.put(Classification.UNRELIABLE, new Classification(
				Classification.UNRELIABLE, Category.BAD));
		return result;
	}

	@Override
	public Map<String, String> getHeadlines() {
		return headlines;
	}

	@Override
	public double getHeadlineThreshold() {
		return headlineThreshold;
	}

	@Override
	public double getThickCorneaThreshold() {
		return thickCorneaThreshold;
	}

	@Override
	public String getBaseIconPath() {
		return config.icon_path_base();
	}

}

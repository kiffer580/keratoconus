package be.uza.keratoconus.configuration.api;

import java.nio.file.Path;
import java.util.Map;

import aQute.bnd.annotation.ProviderType;

/**
 * Service which provides global configuration parameters to other components.
 * 
 * @author Chris Gray
 *
 */
@ProviderType
public interface PentacamConfigurationService {

	/**
	 * The title to be used on window bars, tooltips, etc..
	 * 
	 * @return
	 */
	String getApplicationTitle();

	/**
	 * Path to the directory where the Pentacam files are stored.
	 */
	Path getPentacamDirectoryPath();

	/**
	 * Path to the directory where the user configuration files are stored.
	 */
	Path getUserPrefsDirectoryPath();

	/**
	 * Path to the directory where the log files are stored.
	 */
	Path getLoggingDirectoryPath();

	/**
	 * Get the minnimum log level which will be logged to file.
	 * 
	 * @return
	 */
	int getLogLevel();

	/**
	 * Names of the common fields which occur in several files (although not
	 * always in the same position). These fields will be presented in the
	 * master file in the order given (before the data fields).
	 */
	String[] getCommonFields();

	/**
	 * Names of the fields which are used internally to create a unique key.
	 * (Must be a subset of the common fields).
	 */
	String[] getKeyFields();

	/**
	 * The character which is used to concatenate the key fields into a single
	 * string.
	 * 
	 * @return
	 */
	char getKeyMemberSeparator();

	/**
	 * Base names of all the files to be used. They will be used in the order
	 * given, and the fields in the master file will be presented in the same
	 * order (after the common fields).
	 */
	String[] getFileBaseNames();

	/**
	 * Get a map of classifications. These are the same as are used in the
	 * &ldquo;Class&rdquo; column of the training file.
	 * 
	 * @return a map from classification name to {@link Classification} object.
	 */
	Map<String, Classification> getClassifications();

	/**
	 * Get a mapping from headline keys to the short texts which should be used
	 * in system tray messages etc.. A headline key is either a classifications
	 * as are used in the &ldquo;Class&rdquo; column of the training file or a
	 * special key such as {@value Classification#AMBIGUOUS} or
	 * {@value Classification#UNRELIABLE}.
	 * 
	 * @return
	 */
	Map<String, String> getHeadlines();

	/**
	 * Get the threshold above which an indication is considered
	 * &ldquo;strong&rdquo;.
	 * 
	 * @return
	 */
	double getHeadlineThreshold();

	/**
	 * Get the threshold (in &mu;m) above which a cornea is considered
	 * &ldquo;thick&rdquo;.
	 * 
	 * @return
	 */
	double getThickCorneaThreshold();

	/**
	 * Get the path to the base icon, relative to directory 'resource/' within
	 * the systemtrayapp bundle. The application will use variant versions of
	 * this icon if it finds them: the variants are formed by adding "-main",
	 * "-side", "-ambiguous", "-normal", "-bad", and "-large" to the file name
	 * just before the final dot and extension, e.g. 'icon/ktc.jpg' &rarr;
	 * 'icon/ktc-main.jpg'. If no such variant is found then the base icon will
	 * be used.
	 * 
	 * @return
	 */
	String getBaseIconPath();

}

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
	 * The character which is used to concatenate the key fields into a single
	 * string.
	 * 
	 * @return
	 */
	char getKeyMemberSeparator();

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

package be.uza.keratoconus.configuration.api;

import java.util.Set;

/**
 * The ClassificationService provides access to the {@link Classification}s and
 * the associated &ldquo;headline&rdquo; texts. This information is derived from
 * the {@link PentacamConfigurationService}.
 * 
 * @author Chris Gray
 *
 */
public interface ClassificationService {

	public static final String THICK = "Thick";
	public static final String UNRELIABLE = "Unreliable";
	public static final String AMBIGUOUS = "Ambiguous";

	/**
	 * Get a list of the keys which can be used to look up a Classification.
	 * These are the same as the values used in &ldquo;Class&rdquo; column of
	 * the CSV files.
	 * 
	 * @return
	 */
	Set<String> keys();

	/**
	 * Get a Classification object by its key.
	 * 
	 * @param key
	 * @return
	 */
	Classification getByKey(String key);

	/**
	 * Get a &ldquo;headline&rdquo; text using the classification key. The
	 * &ldquo;headline&rdquo; text is a short (generally one or two words)
	 * human-readable description of the classification.
	 * 
	 * @param key
	 * @return
	 */
	String getHeadline(String key);
}

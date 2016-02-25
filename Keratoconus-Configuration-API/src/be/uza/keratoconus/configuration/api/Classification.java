package be.uza.keratoconus.configuration.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import aQute.bnd.annotation.ProviderType;

/**
 * 
 * @author Chris Gray
 *
 */
@ProviderType
public class Classification {

	/**
	 * Special classification key used to indicate that the classifier returned
	 * an ambiguous result.
	 */
	public static final String AMBIGUOUS = "Ambiguous";

	/**
	 * Special classification key used to indicate that the patient examination
	 * data was unreliable and so no classification was possible.
	 */
	public static final String UNRELIABLE = "Unreliable";

	private final String key;
	private final List<String> qualifiers;

	/**
	 * Classifications are divided into a number of categories for purposes such
	 * as representation in the user interface.
	 * <dl>
	 * <dt>MAIN
	 * <dt>(One of) the diagnosis/es with which the application is primarily
	 * concerned, e.g. frank keratoconus or post-refractive surgery.
	 * <dt>SIDE
	 * <dt>A diagnosis which is detected by the model but is not of proimary
	 * interest, e.g. (non-keratoconic) astigmatism.
	 * <dt>NORMAL
	 * <dt>The absence of any diagnosis detected by the model.
	 * <dt>AMBIGUOUS
	 * <dt>A diagnosis which cannot be made with confidence, e.g. &ldquo;forme
	 * fruste&rdquo; keratoconus.
	 * <dt>BAD
	 * <dt>No diagnosis possible because of unreliable patient examination data
	 * or other circumstances in which the model is known to be unreliable, such
	 * as an older patient or an abnormally thick cornea.
	 * </dl>
	 * 
	 * @author Chris Gray
	 *
	 */
	public enum Category {
		MAIN("Main indication"), SIDE("Side indication"), NORMAL("No indication"), AMBIGUOUS("Ambiguous case"), BAD(
				"Unreliable data");

		private final String description;

		private Category(String s) {
			description = s;
		}

		public String getName() {
			return name().toLowerCase();
		}

		public String getDescription() {
			return description;
		}
	}

	/**
	 * Construct a Classification object on the basis of a string in the form <key>;<category>, e.g. "Astig;side".
	 * The <key> part must match the classification name in the WEKA model.
	 * @param s
	 */
	public Classification(String s) {
		String[] split = s.split(";");
		key = split[0];
		qualifiers = new ArrayList<String>();
		for (int i = 1; i < split.length; ++i) {
			qualifiers.add(split[i]);
		}
	}

	/**
	 * Construct a Classification from its key and category.
	 * @param key The key, which must match the classification name in the WEKA model.
	 * @param cat The category
	 */
	public Classification(String key, Category cat) {
		this.key = key;
		qualifiers = Collections.singletonList(cat.name().toLowerCase());
	}

	public String getKey() {
		return key;
	}

	public boolean isNormal() {
		return qualifiers.contains(Category.NORMAL.name().toLowerCase());
	}

	public boolean isAmbiguous() {
		return qualifiers.contains(Category.AMBIGUOUS.name().toLowerCase());
	}

	public boolean isUnreliable() {
		return qualifiers.contains(Category.BAD.name().toLowerCase());
	}

	public boolean isSide() {
		return qualifiers.contains(Category.SIDE.name().toLowerCase());
	}

	public boolean isMain() {
		return qualifiers.contains(Category.MAIN.name().toLowerCase());
	}

	public Category getCategory() {
		for (Category c : Category.values()) {
			if (qualifiers.contains(c.name().toLowerCase())) {
				return c;
			}
		}
		// TODO log ?
		return Category.AMBIGUOUS;
	}
	
	public List<String> getQualifiers() {
		return qualifiers;
	}

	@Override
	public String toString() {
		return "Classification [key=" + key + ", qualifiers=" + qualifiers
				+ "]";
	}
}
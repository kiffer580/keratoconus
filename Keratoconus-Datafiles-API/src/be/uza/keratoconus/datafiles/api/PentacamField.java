package be.uza.keratoconus.datafiles.api;

import aQute.bnd.annotation.ProviderType;

/**
 * A PentacamField describes one record of a PentacamFile.  (Some &ldquo;common&rdquo; fields may occur in more than one file.)
 * @author Chris Gray
 *
 */
@ProviderType
public interface PentacamField {

	/**
	 * Attribute indicating that a field exists in two variants, anterior and posterior..
	 */
	public static final String BIFACIAL = "bifacial";
	
	/**
	 * Attribute indicating that a field is used to distinguish between the two bifacial variants.
	 */
	public static final String DISCRIMINATOR = "discriminator";
	
	/**
	 * Attribute indicating that a field is &ldquo;common&rdquo;.
	 */
	public static final String COMMON = "common";
	
	/**
	 * Attribute indicating that a field forms part of the key used to access patient records.
	 */
	public static final String KEY = "key";
	
	/**
	 * Attribute indicating that a field is not used.
	 */
	public static final String UNUSED = "unused";

	/**
	 * Get the name of the field.
	 * @return
	 */
	String getName();

	/**
	 * Returns true if this is a bifacial field.
	 * @return
	 */
	boolean isBifacial();

	/**
	 * Returns true if this is the discriminator field.
	 * @return
	 */
	boolean isDiscriminator();

	/**
	 * Returns true if this field is not &ldquo;unused&rdquo;.
	 * @return
	 */
	boolean isUsed();

}
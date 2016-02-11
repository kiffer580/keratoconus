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
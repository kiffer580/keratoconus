package be.uza.keratoconus.model.api;

public abstract class AttributeNames {

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


}

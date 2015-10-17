package be.uza.keratoconus.datafiles.api;

import java.io.IOException;
import java.util.List;

import aQute.bnd.annotation.ProviderType;

/**
 * Representation of a Pentacam data file.
 * @author Chris Gray
 *
 */
@ProviderType
public interface PentacamFile {

	/**
	 * Get the base name of this file.
	 * @return The file name without the directory path or the file extension.
	 */
	String getBaseName();
	
	/**
	 * Get one (non-bifacial) record of the file based on the internal key.
	 * @param key
	 */
	String[] getRecord(String key);
	
	/**
	 * Get one side of a bifacial record of the file based on the internal key and the discriminator value ("FRONT" or "BACK").
	 * @param key
	 * @param discriminator
	 */
	String[] getRecord(String key, String discriminator);
	
	/**
	 * Get all records of the file, in order of appearance in the file.
	 * @return a list of records; each record consists of an array holding one string for each field.
	 */
	List<String[]> getAllRecords();
	
	/**
	 * Get all records of the file, from a certain offset in the file up to the end.
	 * @param startOffset the starting offset in bytes.
	 */
	List<String[]> getNewRecords(long startOffset);
	
//	/**
//	 * Get all records of the file, from a certain offset in the file (inclusive) up to a certain offset (exclusive).
//	 * @param startOffset
//	 */
//	List<String[]> getNewRecords(long startOffset, long endOffset);
	
	/**
	 * Get descriptors of all fields of the file, including fields which are not used by the application.
	 */
	List<PentacamField> getAllFields();
	
	/**
	 * Get descriptors of all common fields of the file, i.e. those fields which are shared with most other data files.
	 */
	List<PentacamField> getCommonFields();
	
	/**
	 * Get descriptors of all fields which are used by the application (including the bifacial discriminator field in the case of a bifacial file).
	 */
	List<PentacamField> getUsedFields();
	
	/**
	 * Extract the internal key which is unique within this file (but see {@link #isBifacial()), and also forms the link to corresponding records in other files.
	 */
	String extractKey(String[] record);
	
	/**
	 * Determine whether the file is &ldquo;bifacial&rdquo;.  A bifacial file has two records for each key value, adjacent to one another and in the order anterior &ndash; posterior.
	 * One field serves as a  &ldquo;discriminator&rdquo;; this field has the value "FRONT" in the first (anterior) record and the value "BACK" in the second (posterior) record.
	 */
	boolean isBifacial();

	/**
	 * Get the descriptor of the &ldquo;discriminator&rdquo; field, if there is one (i.e. if the file is &ldquo;bifacial&rdquo;).
	 */
	PentacamField getBifacialDiscriminator();

	/**
	 * Get the current length of this file.
	 * @return the length in bytes.
	 * @throws IOException 
	 */
	long getCurrentLength() throws IOException;

}

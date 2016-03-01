/*
    This file is part of Keratoconus Assistant.

    Keratoconus Assistant is free software: you can redistribute it 
    and/or modify it under the terms of the GNU General Public License 
    as published by the Free Software Foundation, either version 3 of 
    the License, or (at your option) any later version.

    Keratoconus Assistant is distributed in the hope that it will be 
    useful, but WITHOUT ANY WARRANTY; without even the implied warranty 
    of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Keratoconus Assistant.  If not, see 
    <http://www.gnu.org/licenses/>.
 */

package be.uza.keratoconus.model.api;

import java.util.List;

import aQute.bnd.annotation.ProviderType;

/**
 * Service which knows about the data files used by the current classification model.
 * 
 * @author Chris Gray
 *
 */
@ProviderType
public interface ModelFileService {

	/**
	 * Base names of all the files to be used. They will be used in the order
	 * given, and the fields in the master file will be presented in the same
	 * order (after the common fields).
	 */
	String[] getFileBaseNames();

	/**
	 * Get the field separator used in CSV files with a given base name.
	 * 
	 * @param fbn
	 *            The file base name as it occurs in the output of
	 *            {@link #getFileBaseNames()}.
	 * @return the separator, usually ";" or ",".
	 */
	String getSeparatorForFile(String fbn);

	/**
	 * Get the fields of the CSV files with a given base name.
	 * 
	 * @param fbn
	 *            The file base name as it occurs in the output of
	 *            {@link #getFileBaseNames()}.
	 * @return the field names.
	 */
	List<String> getFieldsOfFile(String fbn);

}

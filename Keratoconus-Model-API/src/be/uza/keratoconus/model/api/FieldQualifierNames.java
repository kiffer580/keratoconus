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

public abstract class FieldQualifierNames {

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

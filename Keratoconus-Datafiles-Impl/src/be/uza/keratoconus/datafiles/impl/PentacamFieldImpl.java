package be.uza.keratoconus.datafiles.impl;

import java.util.Arrays;
import java.util.List;

import be.uza.keratoconus.datafiles.api.PentacamField;

public class PentacamFieldImpl implements PentacamField {

	private static final String INTERNAL_DELIMITER = ";";

	private final String name;
	private final boolean bifacial;
	private final boolean used;
	private final boolean discriminator;

	public PentacamFieldImpl(String name, boolean key, boolean common,
			boolean used, boolean bifacial, boolean discriminator) {
		this.name = name;
		this.bifacial = bifacial;
		this.used = used;
		this.discriminator = discriminator;
	}

	public PentacamFieldImpl(String name, boolean key, boolean common) {
		this(name, key, common, false, false, false);
	}

	public PentacamFieldImpl(String property) {
		String[] parts = property.split(INTERNAL_DELIMITER);
		name = parts[0];
		List<String> cdr = Arrays.asList(parts).subList(1, parts.length);
		bifacial = extractBifaciality(cdr);
		used = extractUsefulness(cdr);
		discriminator = extractDiscriminator(cdr);
	}

	private boolean extractBifaciality(List<String> cdr) {
		if (cdr.contains(BIFACIAL)) {
			return true;
		}
		return false;
	}

	private boolean extractDiscriminator(List<String> cdr) {
		if (cdr.contains(DISCRIMINATOR)) {
			return true;
		}
		return false;
	}

	private boolean extractUsefulness(List<String> cdr) {
		if (cdr.contains(UNUSED)) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see be.kiffer.uza.keratoconus.datafiles.PentacamField#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see be.kiffer.uza.keratoconus.datafiles.PentacamField#isBifacial()
	 */
	@Override
	public boolean isBifacial() {
		return bifacial;
	}

	/* (non-Javadoc)
	 * @see be.kiffer.uza.keratoconus.datafiles.PentacamField#isDiscriminator()
	 */
	@Override
	public boolean isDiscriminator() {
		return discriminator;
	}

	/* (non-Javadoc)
	 * @see be.kiffer.uza.keratoconus.datafiles.PentacamField#isUsed()
	 */
	@Override
	public boolean isUsed() {
		return used;
	}

	@Override
	public String toString() {
		return "PentacamField [name=" + name + (bifacial ? ", bifacial" : "")
				+ (used ? ", used" : "")
				+ (discriminator ? ", discriminator" : "") + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (bifacial ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (used ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PentacamFieldImpl other = (PentacamFieldImpl) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return (bifacial == other.bifacial) && (used == other.used);
	}

}

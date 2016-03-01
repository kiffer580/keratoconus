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

package be.uza.keratoconus.systemtrayapp;

import java.util.Map;
import java.util.Map.Entry;

import org.osgi.service.event.Event;

final class UserPreferencesChangedEvent extends Event {
	
	static final String USERPREFS_CHANGED_TOPIC_PREFIX = "be/kiffer/uza/keratoconus/userprefs/changed/";
	
	private Map<String, ?> changedProperties;

	UserPreferencesChangedEvent(String subtopic, Map<String, ?> changedProperties) {
		super(USERPREFS_CHANGED_TOPIC_PREFIX + subtopic, changedProperties);
		this.changedProperties = changedProperties;
	}
	
	public Map<String, ?> getChanges() {
		return changedProperties;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Entry<String, ?> entry : changedProperties.entrySet()) {
			sb.append(" ").append(entry.getKey()).append("=").append(entry.getValue());
		}
		return super.toString() + sb;
	}
}
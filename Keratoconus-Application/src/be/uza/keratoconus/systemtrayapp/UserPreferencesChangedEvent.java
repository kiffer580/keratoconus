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
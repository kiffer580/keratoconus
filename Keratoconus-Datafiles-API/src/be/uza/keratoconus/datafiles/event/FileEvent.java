package be.uza.keratoconus.datafiles.event;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.service.event.Event;

public class FileEvent extends Event {

	protected static Dictionary<String, Object> toProperties(String fileName) {
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(FileEventConstants.FILE_NAME, fileName);
		return properties;
	}

	public String getFileName() {
		return (String) getProperty(FileEventConstants.FILE_NAME);
	}

	public FileEvent(String topic, Dictionary<String, Object> properties) {
		super(topic, properties);
	}

	public String toString() {
		StringBuilder properties = new StringBuilder();
		for (String name : getPropertyNames()) {
			properties.append(" ").append(name).append("=").append(getProperty(name));
		}
		return super.toString() + properties;
	}
	
}

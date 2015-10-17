package be.uza.keratoconus.datafiles.event;

public class FileChangedEvent extends FileEvent {

	public FileChangedEvent(String baseName) {
		super(FileEventConstants.DATAFILE_CHANGED_TOPIC, toProperties(baseName));
	}

}
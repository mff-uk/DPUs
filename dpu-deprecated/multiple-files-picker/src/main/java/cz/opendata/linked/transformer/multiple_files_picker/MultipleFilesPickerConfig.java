package cz.opendata.linked.transformer.multiple_files_picker;

import cz.cuni.mff.xrg.odcs.commons.module.config.DPUConfigObjectBase;

/**
 * Put your DPU's configuration here.
 *
 * You can optionally implement {@link #isValid()} to provide possibility to
 * validate the configuration.
 *
 * <b>This class must have default (parameter less) constructor!</b>
 */
public class MultipleFilesPickerConfig extends DPUConfigObjectBase {

	private static final long serialVersionUID = 5000810352145473446L;

	private String path = null;

	public MultipleFilesPickerConfig() {

	}

	public MultipleFilesPickerConfig(String path) {
		this.path = path;
	}

	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		this.path = path;
	}
	
}

package de.qabel.core.config;

public abstract class AbstractModuleSettings extends SyncSettingItem {
	private String type;

	protected AbstractModuleSettings() {
		this.type = this.getClass().getName();
	}

	public String getType() {
		return type;
	}
}

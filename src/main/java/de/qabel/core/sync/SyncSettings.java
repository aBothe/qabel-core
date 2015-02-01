package de.qabel.core.sync;

import de.qabel.core.config.LocaleModuleSettings;
import de.qabel.core.config.StorageServer;
import de.qabel.core.config.StorageVolume;

/**
 *
 * @author Alexander Bothe
 */
public class SyncSettings extends LocaleModuleSettings {

	StorageServer syncStorageServer;
	StorageVolume syncStorageVolume;

	/**
	 * Every (this value * MIN_TIMEBETWEENSYNCINVOKES), a sync becomes force-invoked. Set to -1 if no force-sync shall
	 * be done.
	 */
	int maxWaitIntervalsBetweenForcedSyncs = 20;

	/**
	 * Milliseconds.
	 */
	int minTimeBetweenSyncInvokes = 5_000;

	public StorageServer getSyncStorageServer() {
		return syncStorageServer;
	}

	public void setSyncStorageServer(StorageServer s) {
		syncStorageServer = s;
	}

	public StorageVolume getSyncStorageVolume() {
		return syncStorageVolume;
	}

	public void setSyncStorageVolume(StorageVolume v) {
		syncStorageVolume = v;
	}

	public int getMaxWaitIntervalsBetweenForcedSyncs() {
		return maxWaitIntervalsBetweenForcedSyncs;
	}

	public void setMaxWaitIntervalsBetweenForcedSyncs(int v) {
		maxWaitIntervalsBetweenForcedSyncs = v;
	}

	public int getMinTimeBetweenSyncInvokes() {
		return minTimeBetweenSyncInvokes;
	}

	public void setMinTimeBetweenSyncInvokes(int v) {
		minTimeBetweenSyncInvokes = v;
	}
}

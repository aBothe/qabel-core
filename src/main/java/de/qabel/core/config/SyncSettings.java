package de.qabel.core.config;

import javax.crypto.SecretKey;

/**
 *
 * @author Alexander Bothe
 */
public class SyncSettings extends LocaleModuleSettings {

	SecretKey syncKey;
	StorageServer syncStorageServer;
	StorageVolume syncStorageVolume;
	/**
	 * Id of the first sync storage blob.
	 * Next blob identifiers will be part of the generated/downloaded blob content
	 * (á la Linked List, just with Blob Ids instead of pointers)
	 */
	String firstBlobName;

	/**
	 * Every (this value * MIN_TIMEBETWEENSYNCINVOKES), a sync becomes force-invoked. Set to -1 if no force-sync shall
	 * be done.
	 */
	int maxWaitIntervalsBetweenForcedSyncs = 20;

	/**
	 * Milliseconds.
	 */
	int minTimeBetweenSyncInvokes = 5_000;

	public SecretKey getSyncStorageKey() {
		return syncKey;
	}

	public void setSyncStorageKey(SecretKey v) {
		syncKey = v;
	}

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

	public String getFirstBlobName() {
		return firstBlobName;
	}

	public void setFirstBlobName(String n) {
		firstBlobName = n;
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

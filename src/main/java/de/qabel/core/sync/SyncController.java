package de.qabel.core.sync;

import de.qabel.core.config.StorageServer;
import de.qabel.core.config.StorageVolume;
import de.qabel.core.drop.DropController;
import de.qabel.core.drop.DropURL;
import de.qabel.core.storage.StorageController;

public class SyncController {

	StorageVolume syncStorageVolume;
	DropURL notificationDrop;

	long lastSyncAttempt;

	public SyncController(DropController dropController, StorageController storageController, StorageServer storageServer, StorageVolume syncStorageVolume, DropURL drop) {
		this.syncStorageVolume = syncStorageVolume;
		this.notificationDrop = drop;

		// Register DropCallback in DropController:
		//      if SyncDropMessage arriving, doSync() gets invoked. 
		// Register ackack actor:
		//      wait 5 minutes
		//      invoke doSync()
		// Hook in a doSync()-invoke somewhere before Qabel starts to shuts down.
	}

	public void doSync() {
		// Compare Date.getTime() - lastSyncAttempt > e.g. 30 seconds to prevent sync spams.

		// Get settings-managing objects //TODO: Which and how can these be reached with no singletons?
		
		// Generate local sync data JSON
		// Acquire header of syncStorageVolume
		// if(header not empty && content length > 0) {
		// Check whether it's changed over time, i.e. via comparing last-modified against lastSyncAttempt or hash comparison against the http-delivered content checksum
		// If stuff changed, acquire entire JSON from storage.
		
		// JSON merging:
		// compare each SyncSettingsItem with the recently downloaded one, take the newer. discard older parts
		// }
		
		// Assign changed SyncSettingsItems to settings managers.
		
		// If no local SyncSettingsItem was newer than a remote one:
		// return;
		
		// Generate local sync data JSON
		// Upload it to given syncStorageVolume

		// Push drop message that a sync has been done
	}

}

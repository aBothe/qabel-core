package de.qabel.core.sync;

import de.qabel.core.config.*;
import de.qabel.core.drop.*;
import de.qabel.core.module.ModuleManager;
import de.qabel.core.storage.*;

public class SyncController {

	final ModuleManager moduleManager;
	StorageVolume syncStorageVolume;
	DropURL notificationDrop;

	long lastSyncAttempt;

	public SyncController(ModuleManager modMgr, StorageController storageController, StorageServer storageServer, StorageVolume syncStorageVolume, DropURL drop) {
		this.moduleManager = modMgr;
		this.syncStorageVolume = syncStorageVolume;
		this.notificationDrop = drop;

		modMgr.getDropController().register(SyncDropMessage.class, new DropCallback<SyncDropMessage>() {
			@Override
			public void onDropMessage(DropMessage<SyncDropMessage> message) {
				doSync();
			}
		});
		
		// Register ackack actor:
		//      wait 5 minutes
		//      invoke doSync()
		// Hook in a doSync()-invoke somewhere before Qabel starts to shuts down.
	}

	public void doSync() {
		// Compare Date.getTime() - lastSyncAttempt > e.g. 30 seconds to prevent sync spams.

		// Get settings-managing objects from moduleManager
		
		// Generate local sync data JSON
		// Acquire header of syncStorageVolume
		// if(header not empty && content length > 0) {
		// Check whether it's changed over time, i.e. via comparing last-modified against lastSyncAttempt or hash comparison against the http-delivered content checksum
		// If stuff changed, acquire entire JSON from storage.
		
		// JSON merging:
		// compare each SyncSettingsItem with the recently downloaded one, take the newer (timestamp comparison should suffice). discard older parts.
		// }
		
		// Assign changed SyncSettingsItems to settings managers: //TODO: Think thread-safe!
		// Perhaps each SyncSettingsItem should have their own AssignFrom()-method to check individually required fields & specific constraints
		
		// If no local SyncSettingsItem was newer than a remote one:
		// return;
		
		// Generate local sync data JSON
		// Upload it to given syncStorageVolume
		// if storage server rejects upload because file is being accessed already {
		// wait 5 seconds
		// doSync() again, keep SO prevention in mind, only e.g. 10 attempts maximum
		// return
		// }

		// Push drop message that a sync has been done
	}

}

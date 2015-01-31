package de.qabel.core.sync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.qabel.core.config.*;
import de.qabel.core.drop.*;
import de.qabel.core.http.StorageHTTP;
import de.qabel.core.module.ModuleManager;
import de.qabel.core.storage.*;

public class SyncController {

	final ModuleManager moduleManager;
	StorageServer storageServer;
	StorageVolume syncStorageVolume;
	DropURL notificationDrop;

	long lastSyncAttempt;

	public SyncController(ModuleManager modMgr, StorageController storageController, StorageServer storageServer, StorageVolume syncStorageVolume, DropURL drop) {
		this.moduleManager = modMgr;
		this.storageServer = storageServer;
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
		SyncedSettings syncedSettings = moduleManager.getSettings().getSyncedSettings();

		// Setup (de)serializer
		String serializedSyncData;
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(SyncedSettings.class, new SyncedSettingsTypeAdapter());
		Gson gson = builder.create();

		boolean newAndOldSettingsDiffer = true;

		// A second download attempt is made if there is new sync data being uploaded
		// on a second peer client.
		int downloadAttempt = 1;
		while(downloadAttempt < 10) {

			// Download syncStorageVolume
			serializedSyncData = getSyncStorageContents();

			if (serializedSyncData != null && serializedSyncData.length() > 0) {
				// Deserialize JSON
				SyncedSettings newSettings;
				try {
					newSettings = gson.fromJson(serializedSyncData, SyncedSettings.class);
				}
				catch(Exception e)
				{
					newSettings = null;
				}

				// JSON merging:
				if(newSettings != null)
					newAndOldSettingsDiffer = mergeSettings(newSettings, newSettings);

				// compare each SyncSettingsItem with the recently downloaded one, take the newer (timestamp comparison should suffice). discard older parts.
			}

			if(!newAndOldSettingsDiffer)
				return;

			// Generate local sync data JSON
			serializedSyncData = gson.toJson(syncedSettings);

			// Upload it to given syncStorageVolume
			switch(putSyncStorageContents(serializedSyncData))
			{
				case Succesful:
					// Push drop message that a sync has been done
					return;
				case ResourceLocked:
					// wait 5 seconds or so
					downloadAttempt++;
					continue;
				case Fail:
					// Inform user that upload couldn't happen
					break;
			}

			break;
		}

		// Silently inform user that sync couldn't be completed
	}

	String getSyncStorageContents()
	{
		StorageHTTP storage = new StorageHTTP();
		// use storageServer and syncStorageVolume to obtain all blobs

		// Merge them to one in-memory byte array
		
		// Make a string out of it
		return "";
	}

	enum SyncPutStatus
	{
		Succesful,
		ResourceLocked,
		Fail
	}

	/**
	 *
	 * @param content
	 * @return true if entire upload was succesful, false if otherwise
	 */
	SyncPutStatus putSyncStorageContents(String content)
	{
		StorageHTTP storage = new StorageHTTP();
		// use storageServer and syncStorageVolume as QSV qualifiers

		// check for http code 423 ('Resource locked') -- TODO: Let the storage server return that value!!  (https://de.wikipedia.org/wiki/HTTP-Statuscode#4xx_.E2.80.93_Client-Fehler)
		// code==423 {
		// return SyncPutStatus.ResourceLocked;
		// }

		return SyncPutStatus.Succesful;
	}

	/**
	 * 
	 * @param newSettings
	 * @param oldSettings Newer stuff from newSettings will be merged into this object
	 * @return true if at least one property got overwritten, false if otherwise
	 */
	static boolean mergeSettings(SyncedSettings newSettings, SyncedSettings oldSettings)
	{
		// Assign changed SyncSettingsItems to settings managers: //TODO: Think thread-safe!
		// Perhaps each SyncSettingsItem should have their own AssignFrom()-method to check individually required fields & specific constraints
		return true;
	}
}

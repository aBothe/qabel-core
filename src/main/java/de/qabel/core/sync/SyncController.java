package de.qabel.core.sync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.qabel.core.config.*;
import de.qabel.core.drop.*;
import de.qabel.core.http.StorageHTTP;
import de.qabel.core.module.ModuleManager;
import de.qabel.core.storage.*;

public class SyncController {

	/**
	 * Milliseconds.
	 */
	private static final int MIN_TIMEBETWEENSYNCINVOKES = 5_000;

	final ModuleManager moduleManager;
	StorageServer storageServer;
	StorageVolume syncStorageVolume;
	DropURL notificationDrop;

	boolean syncInvokeEnqueued = true;

	public SyncController(ModuleManager modMgr, StorageController storageController, StorageServer storageServer, StorageVolume syncStorageVolume, DropURL drop) {
		this.moduleManager = modMgr;
		this.storageServer = storageServer;
		this.syncStorageVolume = syncStorageVolume;
		this.notificationDrop = drop;

		// Setup asynchronous thread that waits for sync invokes.
		Thread syncThread = new Thread() {
			@Override
			public void run() {
				while(true) {
					try {
						Thread.sleep(MIN_TIMEBETWEENSYNCINVOKES);
					} catch (InterruptedException e) { }

					if(!syncInvokeEnqueued)
						continue;

					syncInvokeEnqueued = false;

					doSync();
				}
			}
		};
		syncThread.start();

		// Register for sync notifications
		modMgr.getDropController().register(SyncDropMessage.class, new DropCallback<SyncDropMessage>() {
			@Override
			public void onDropMessage(DropMessage<SyncDropMessage> message) {
				enqueueSync();
			}
		});
		
		// TODO: Hook in a doSync()-invoke somewhere before Qabel starts to shuts down.
	}

	/**
	 * Invoke the sync process asynchronously.
	 * The actual execution will happen in max. MIN_TIMEBETWEENSYNCINVOKES milliseconds.
	 */
	public void enqueueSync()
	{
		syncInvokeEnqueued = true;
	}

	private void doSync() {
		// Get settings-managing objects from moduleManager
		SyncedSettings syncedSettings = moduleManager.getSettings().getSyncedSettings();

		// Setup (de)serializer
		String serializedSyncData;
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(SyncedSettings.class, new SyncedSettingsTypeAdapter());
		Gson gson = builder.create();

		boolean newAndOldSettingsDiffer = true;

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

			// Merge new settings into existing settings
			if(newSettings != null)
				newAndOldSettingsDiffer = mergeSettings(newSettings, newSettings);
		}

		if(!newAndOldSettingsDiffer) {
			return;
		}

		// Generate local sync data JSON
		serializedSyncData = gson.toJson(syncedSettings);

		// Upload it to given syncStorageVolume
		switch(putSyncStorageContents(serializedSyncData))
		{
			case Succesful:
				// Push drop message that a sync has been done -- TODO: Add drop API that allows sending messages to drops, not only contacts;
				//RESEARCH: How to obatin proper dropUrls?
				//moduleManager.getDropController().sendAndForget(new DropMessage<SyncDropMessage>(), moduleManager.getDropController().getDropServers());

				return;
			case ResourceLocked:
				// Try sync again a little time later on as it's likely that new sync 
				// data will have arrived once the sync storage volume isn't locked anymore.
				enqueueSync();
				return;
			case Fail:
				// Inform user that upload couldn't happen
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

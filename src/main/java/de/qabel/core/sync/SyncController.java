package de.qabel.core.sync;

import de.qabel.core.config.SyncSettings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.qabel.core.config.*;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.drop.*;
import de.qabel.core.exceptions.QblDropPayloadSizeException;
import de.qabel.core.exceptions.QblStorageInvalidBlobName;
import de.qabel.core.exceptions.QblStorageInvalidToken;
import de.qabel.core.module.ModuleManager;
import de.qabel.core.storage.*;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.Collection;
import java.util.Set;
import java.util.logging.*;

/**
 * https://github.com/Qabel/qabel-sync-module/wiki/Components-Sync
 *
 * @author Alexander Bothe
 */
public final class SyncController {
	private final static Logger logger = Logger.getLogger(StorageAction.class.getName());

	/**
	 * If set to true, an incoming sync-notification will be ignored. Used for preventing a redundant second sync
	 * invokation.
	 */
	boolean syncNotificationSent = false;
	final ModuleManager moduleManager;
	Thread syncThread;

	SyncSettings getSyncSettings() {
		for (LocaleModuleSettings s : moduleManager.getSettings().getLocalSettings().getLocaleModuleSettings()) {
			if (s instanceof SyncSettings) {
				return (SyncSettings) s;
			}
		}

		return new SyncSettings();
	}

	boolean syncInvokeEnqueued;
	boolean quitSyncThread;

	public SyncController(ModuleManager modMgr, StorageController storageController) {
		this.moduleManager = modMgr;

		// Register for sync notifications
		modMgr.getDropController().register(SyncNotificationMessage.class, new DropCallback<SyncNotificationMessage>() {
			@Override
			public void onDropMessage(DropMessage<SyncNotificationMessage> message) {
				if (syncNotificationSent) {
					return;
				}
				syncNotificationSent = false;

				enqueueSync();
			}
		});

		// TODO: Hook in quitSyncThread() somewhere before Qabel starts to shut down.
		enqueueSync();
	}

	/**
	 * Invoke the data synchronization asynchronously.
	 */
	public void enqueueSync() {
		syncInvokeEnqueued = true;
		tryStartSyncThread();
	}

	/**
	 * Setup asynchronous thread that waits for sync invokes.
	 */
	void tryStartSyncThread() {
		if (syncThread != null && syncThread.isAlive()) {
			return;
		}

		quitSyncThread = false;
		syncThread = new Thread() {
			@Override
			public void run() {
				SyncSettings settings = getSyncSettings();
				int waitInterval = 0;
				while (!quitSyncThread) {

					if (!syncInvokeEnqueued) {
						if (settings.getMaxWaitIntervalsBetweenForcedSyncs() < 0
								|| ++waitInterval < settings.getMaxWaitIntervalsBetweenForcedSyncs()) {
							continue;
						}
					}

					syncInvokeEnqueued = false;
					waitInterval = 0;

					doSync();

					try {
						Thread.sleep(settings.getMinTimeBetweenSyncInvokes());
					} catch (InterruptedException e) {
					}
				}

				// Sync a very last time before Qabel (and thus this thread) shuts down.
				doSync();
			}
		};
		syncThread.start();
	}

	/**
	 * Shuts down the sync thread after a very last sync invoke.
	 *
	 * @param waitUntilFinished
	 */
	public void quitSyncThread(boolean waitUntilFinished) {
		quitSyncThread = true;

		if (!waitUntilFinished) {
			return;
		}

		try {
			syncThread.join(0);
		} catch (InterruptedException e) {
		}
	}

	private void doSync() {
		// Get settings-managing objects from moduleManager
		SyncedSettings syncedSettings = moduleManager.getSettings().getSyncedSettings();

		// Setup (de)serializer
		String serializedSyncData = null;
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(SyncedSettings.class, new SyncedSettingsTypeAdapter());
		Gson gson = builder.create();

		boolean newAndOldSettingsDiffer = true;

		try {
			// Download syncStorageVolume
			serializedSyncData = getSyncStorageContents();
		} catch (IOException | InvalidKeyException | QblStorageInvalidBlobName e) {
			logger.log(Level.WARNING, "Error while downloading sync info.", e);
		}
		
		if (serializedSyncData != null && serializedSyncData.length() > 0) {
			// Deserialize JSON
			SyncedSettings newSettings;
			try {
				newSettings = gson.fromJson(serializedSyncData, SyncedSettings.class);
			} catch (Exception e) {
				newSettings = null;
			}

			// Merge new settings into existing settings
			if (newSettings != null) {
				newAndOldSettingsDiffer = mergeSettings(newSettings, newSettings);
			}
		}

		if (!newAndOldSettingsDiffer) {
			return;
		}

		// Generate local sync data JSON
		serializedSyncData = gson.toJson(syncedSettings);

		// Upload it to given syncStorageVolume
		switch (putSyncStorageContents(serializedSyncData)) {
			case Succesful:
				sendSyncNotification();
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

	String getSyncStorageContents() throws IOException, InvalidKeyException, QblStorageInvalidBlobName {
		SyncSettings settings = getSyncSettings();

		// use storageServer and syncStorageVolume to obtain all blobs
		File firstBlobFile = StorageAction.retrieveBlob(settings.getSyncStorageVolume(), settings.getFirstBlobName(), settings.getSyncStorageKey());

		// read first blob.



		// Merge them to one in-memory byte array


		// Make a string out of it
		return "";
	}

	enum SyncPutStatus {

		Succesful,
		ResourceLocked,
		Fail
	}

	/**
	 *
	 * @param content
	 * @return true if entire upload was succesful, false if otherwise
	 */
	SyncPutStatus putSyncStorageContents(String content) {
		SyncSettings settings = getSyncSettings();

		
		try{
			StorageAction.uploadBlob(null, null, null);
		}catch(IOException | InvalidKeyException | QblStorageInvalidToken e){
			logger.log(Level.WARNING, "Error while uploading sync info.", e);
			return SyncPutStatus.Fail;
		}
		
		// use storageServer and syncStorageVolume as QSV qualifiers
		// check for http code 423 ('Resource locked') -- TODO: Let the storage server return that value!!  (https://de.wikipedia.org/wiki/HTTP-Statuscode#4xx_.E2.80.93_Client-Fehler)
		// code==423 {
		// return SyncPutStatus.ResourceLocked;
		// }
		return SyncPutStatus.Succesful;
	}

	/**
	 * Push drop message that a sync-upload has been completed successfully to the owner's drop which then causes the
	 * user's other clients to invoke sync.
	 */
	void sendSyncNotification() {

		SyncNotificationMessage msg = new SyncNotificationMessage();

		for (Identity id : moduleManager.getSettings().getSyncedSettings().getIdentities().getIdentities()) {
			// TODO: Add drop API that allows sending messages to drops, not only contacts;

			// HACK: For now, construct a Contact that represents the actual user's identity.
			Contact contact = new Contact(id, id.getDropUrls(), id.getEcPublicKey());

			try {
				moduleManager.getDropController().sendAndForget(new DropMessage(id, msg), contact);
			} catch (QblDropPayloadSizeException e) {
				logger.log(Level.WARNING, "Sync notification couldn't be sent via Drop.", e);
			}
		}

		syncNotificationSent = true;
	}

	static <T extends SyncSettingItem> boolean mergeSyncedSettingsItems(Collection<T> newSettings, Collection<T> oldSettings) {
		long lastModified = 0;
		long lastNewModified = 0;

		for (SyncSettingItem i : oldSettings) {
			if (i.getUpdated() > lastModified) {
				lastModified = i.getUpdated();
			}
		}

		for (SyncSettingItem i : newSettings) {
			if (i.getUpdated() > lastNewModified) {
				lastNewModified = i.getUpdated();
			}
		}

		if (lastNewModified == lastModified && newSettings.equals(oldSettings)) {
			return false;
		}

		if (lastNewModified > lastModified) {
			oldSettings.clear();
			oldSettings.addAll(newSettings);
		}

		return true;
	}

	/**
	 *
	 * @param newSettings
	 * @param oldSettings Newer stuff from newSettings will be merged into this object
	 * @return true if at least one property got overwritten, false if otherwise
	 */
	static boolean mergeSettings(SyncedSettings newSettings, SyncedSettings oldSettings) {
		// Assign changed SyncSettingsItems to settings managers: //TODO: Think thread-safe!
		// Perhaps each SyncSettingsItem should have their own AssignFrom()-method to check individually required fields & specific constraints

		boolean changedAThing = mergeSyncedSettingsItems(newSettings.getAccounts().getAccounts(), oldSettings.getAccounts().getAccounts());

		changedAThing |= mergeSyncedSettingsItems(newSettings.getContacts().getContacts(), oldSettings.getContacts().getContacts());

		changedAThing |= mergeSyncedSettingsItems(newSettings.getDropServers().getDropServers(), oldSettings.getDropServers().getDropServers());

		changedAThing |= mergeSyncedSettingsItems(newSettings.getIdentities().getIdentities(), oldSettings.getIdentities().getIdentities());

		changedAThing |= mergeSyncedSettingsItems(newSettings.getStorageServers().getStorageServers(), oldSettings.getStorageServers().getStorageServers());

		changedAThing |= mergeSyncedSettingsItems(newSettings.getStorageVolumes().getStorageVolumes(), oldSettings.getStorageVolumes().getStorageVolumes());

		Set<SyncedModuleSettings> newModuleSettings = newSettings.getSyncedModuleSettings();
		Set<SyncedModuleSettings> oldModuleSettings = oldSettings.getSyncedModuleSettings();

		for (SyncedModuleSettings oldItem : oldSettings.getSyncedModuleSettings()) {
			SyncedModuleSettings newItem = null;
			for (SyncedModuleSettings k : newModuleSettings) //TODO: Faster access via type lookup
			{
				if (k.getType().equals(oldItem.getType())) {
					newItem = k;
					break;
				}
			}

			// Item exists in oldSettings but not in newSettings
			if (newItem == null) {
				// Don't discard newSettings' item but let it stay there
				// Thus, module settings can't be removed, just overwritten by e.g. default (null'd-out) items
				changedAThing = true;
				continue;
			}

			// Nothing has changed
			if (newItem.getUpdated() == oldItem.getUpdated() && oldItem.equals(newItem)) {
				continue;
			}

			// newItem is newer than oldItem
			if (newItem.getUpdated() > oldItem.getUpdated()) {
				// Keep newItem, discard oldItem
				oldModuleSettings.remove(oldItem);
				oldModuleSettings.add(newItem);
			}

			// newItem.getUpdated() must be less than oldItem.getUpdated()
			// keep oldItem, discard newItem
			// changedAThing = true;
			changedAThing = true;
		}

		return changedAThing;
	}
}

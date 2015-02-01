package de.qabel.core.sync;

import de.qabel.core.config.SyncSettings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.qabel.core.config.*;
import de.qabel.core.crypto.QblEncKeyPair;
import de.qabel.core.drop.*;
import de.qabel.core.http.StorageHTTP;
import de.qabel.core.module.ModuleManager;
import de.qabel.core.storage.*;
import java.security.InvalidKeyException;

/**
 * https://github.com/Qabel/qabel-sync-module/wiki/Components-Sync
 *
 * @author Alexander Bothe
 */
public final class SyncController {

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
		modMgr.getDropController().register(SyncDropMessage.class, new DropCallback<SyncDropMessage>() {
			@Override
			public void onDropMessage(DropMessage<SyncDropMessage> message) {
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

	String getSyncStorageContents() {
		StorageHTTP storage = new StorageHTTP();
		SyncSettings settings = getSyncSettings();

		// use storageServer and syncStorageVolume to obtain all blobs
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
		StorageHTTP storage = new StorageHTTP();
		SyncSettings settings = getSyncSettings();

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
		for (Identity id : moduleManager.getSettings().getSyncedSettings().getIdentities().getIdentities()) {
			// TODO: Add drop API that allows sending messages to drops, not only contacts;

			// HACK: For now, construct a Contact that represents the actual user's identity.
			Contact contact = new Contact(id);
			contact.setPrimaryPublicKey(id.getPrimaryKeyPair().getQblPrimaryPublicKey());
			for (QblEncKeyPair key : id.getPrimaryKeyPair().getEncKeyPairs()) {
				try {
					contact.addEncryptionPublicKey(key.getQblEncPublicKey());
				} catch (InvalidKeyException e) {
				}
			}

			contact.getDropUrls().addAll(id.getDrops());

			moduleManager.getDropController().sendAndForget(new DropMessage<SyncDropMessage>(), contact);
		}

		syncNotificationSent = true;
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
		return true;
	}
}

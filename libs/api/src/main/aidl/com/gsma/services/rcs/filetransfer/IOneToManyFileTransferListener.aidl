package com.gsma.services.rcs.filetransfer;

import com.gsma.services.rcs.contact.ContactId;

/**
 * Callback methods for one-to-many file transfer events
 */
interface IOneToManyFileTransferListener {

	void onStateChanged(in String chatId, in String transferId, in int state, in int reasonCode);

	void onDeliveryInfoChanged(in String chatId, in ContactId contact, in String transferId, in int state, in int reasonCode);

	void onProgressUpdate(in String chatId, in String transferId, in long currentSize, in long totalSize);

	void onDeleted(in String chatId, in List<String> transferIds);
}

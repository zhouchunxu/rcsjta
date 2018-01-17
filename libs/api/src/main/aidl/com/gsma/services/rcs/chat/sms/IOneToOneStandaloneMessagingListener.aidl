package com.gsma.services.rcs.chat.sms;

import com.gsma.services.rcs.contact.ContactId;

/**
 * Callback methods for 1-1 standalone messaging events
 */
interface IOneToOneStandaloneMessagingListener {

	void onMessageStatusChanged(in String chatId, in String mimeType, in String msgId,
			in int status, in int reasonCode);

	void onMessagesDeleted(in String chatId, in List<String> msgIds);
}
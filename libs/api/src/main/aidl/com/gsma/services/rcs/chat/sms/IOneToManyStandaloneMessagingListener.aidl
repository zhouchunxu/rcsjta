package com.gsma.services.rcs.chat.sms;

import com.gsma.services.rcs.contact.ContactId;

/**
 * Callback methods for 1-N standalone messaging events
 */
interface IOneToManyStandaloneMessagingListener {

	void onMessageStatusChanged(in String chatId, in String mimeType, in String msgId,
			in int status, in int reasonCode);

	void onMessageDeliveryInfoChanged(in String chatId, in ContactId contact, in String mimeType,
			in String msgId, in int status, in int reasonCode);

	void onMessagesDeleted(in String ChatId, in List<String> msgIds);
}
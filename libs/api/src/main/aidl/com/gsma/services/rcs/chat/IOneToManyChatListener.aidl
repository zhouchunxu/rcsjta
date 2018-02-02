package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.contact.ContactId;

/**
 * One-to-Many Chat event listener
 */
interface IOneToManyChatListener {

	void onMessageStatusChanged(in String chatId, in String mimeType, in String msgId,
			in int status, in int reasonCode);

	void onMessageDeliveryInfoChanged(in String chatId, in ContactId contact, in String mimeType,
			in String msgId, in int status, in int reasonCode);

	void onMessagesDeleted(in String ChatId, in List<String> msgIds);
}
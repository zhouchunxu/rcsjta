package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.chat.Card;
import com.gsma.services.rcs.chat.CloudFile;
import com.gsma.services.rcs.chat.Emoticon;
import com.gsma.services.rcs.chat.IChatMessage;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.Geoloc;

/**
 * One-to-One Chat interface
 */
interface IOneToOneChat {

	ContactId getRemoteContact();

	IChatMessage sendMessage(in String message);

	IChatMessage sendMessage2(in Geoloc geoloc);

	IChatMessage sendMessage3(in Emoticon emoticon);

	IChatMessage sendMessage4(in CloudFile cloudFile);

	IChatMessage sendMessage5(in Card card);

	void setComposingStatus(in boolean ongoing);

	void openChat();

	void resendMessage(in String msgId);

	boolean isAllowedToSendMessage();
}
package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.chat.IChatMessage;
import com.gsma.services.rcs.chat.IOneToOneChatListener;
import com.gsma.services.rcs.chat.IOneToOneChat;
import com.gsma.services.rcs.chat.IOneToManyChatListener;
import com.gsma.services.rcs.chat.IOneToManyChat;
import com.gsma.services.rcs.chat.IGroupChatListener;
import com.gsma.services.rcs.chat.IGroupChat;
import com.gsma.services.rcs.chat.IChatServiceConfiguration;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.ICommonServiceConfiguration;
import com.gsma.services.rcs.RcsServiceRegistration;

/**
 * Chat service API
 */
interface IChatService {

	boolean isServiceRegistered();

	int getServiceRegistrationReasonCode();

	void addEventListener(IRcsServiceRegistrationListener listener);

	void removeEventListener(IRcsServiceRegistrationListener listener);

	int getServiceVersion();

	ICommonServiceConfiguration getCommonConfiguration();

	IChatServiceConfiguration getConfiguration();

	IOneToOneChat getOneToOneChat(in ContactId contact);

	IOneToManyChat getOneToManyChat(in List<ContactId> contacts);

	IGroupChat getGroupChat(in String chatId);

	IGroupChat initiateGroupChat(in List<ContactId> contacts, in String subject);

//	IOneToManyChat initiateOneToManyChat(in List<ContactId> contacts);

	void markMessageAsRead(in String msgId);

	void addEventListener2(in IOneToOneChatListener listener);

	void removeEventListener2(in IOneToOneChatListener listener);

	void addEventListener3(in IOneToManyChatListener listener);

	void removeEventListener3(in IOneToManyChatListener listener);

	void addEventListener4(in IGroupChatListener listener);

	void removeEventListener4(in IGroupChatListener listener);

	IChatMessage getChatMessage(in String msgId);

	boolean isAllowedToInitiateGroupChat();

	boolean isAllowedToInitiateGroupChat2(in ContactId contact);

	void reportMessage(in String msgId);

	void deleteOneToOneChats();

	void deleteOneToManyChats();

	void deleteGroupChats();

	void deleteOneToOneChat(in ContactId contact);

	void deleteOneToManyChat(in String chatId);

	void deleteGroupChat(in String chatId);

	void deleteMessage(in String msgId);

	void clearMessageDeliveryExpiration(in List<String> msgIds);
}
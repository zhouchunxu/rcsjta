package com.gsma.services.rcs.chat.sms;

import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.chat.Card;
import com.gsma.services.rcs.chat.CloudFile;
import com.gsma.services.rcs.chat.Emoticon;
import com.gsma.services.rcs.chat.IChatMessage;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.ICommonServiceConfiguration;
import com.gsma.services.rcs.RcsServiceRegistration;
import com.gsma.services.rcs.chat.sms.IOneToManyStandaloneMessagingListener;
import com.gsma.services.rcs.chat.sms.IOneToOneStandaloneMessagingListener;
import com.gsma.services.rcs.chat.sms.IStandaloneMessagingServiceConfiguration;

/**
 * Standalone messaging service API
 */
interface IStandaloneMessagingService {

	boolean isServiceRegistered();

	int getServiceRegistrationReasonCode();

	void addEventListener(IRcsServiceRegistrationListener listener);

	void removeEventListener(IRcsServiceRegistrationListener listener);

	int getServiceVersion();

	ICommonServiceConfiguration getCommonConfiguration();

	IStandaloneMessagingServiceConfiguration getConfiguration();

	IChatMessage getMessage(in String msgId);

//	String getOrCreateChatId(in List<ContactId> contacts);

	IChatMessage sendMessage(in ContactId contact, in String message);

	IChatMessage sendMessage2(in ContactId contact, in Geoloc geoloc);

	IChatMessage sendMessage3(in ContactId contact, in Emoticon emoticon);

	IChatMessage sendMessage4(in ContactId contact, in CloudFile cloudFile);

	IChatMessage sendMessage5(in ContactId contact, in Card card);

	IChatMessage sendMessageToMany(in List<ContactId> contacts, in String message);

	IChatMessage sendMessageToMany2(in List<ContactId> contacts, in Geoloc geoloc);

	IChatMessage sendMessageToMany3(in List<ContactId> contacts, in Emoticon emoticon);

	IChatMessage sendMessageToMany4(in List<ContactId> contacts, in CloudFile cloudFile);

	IChatMessage sendMessageToMany5(in List<ContactId> contacts, in Card card);

	void resendMessage(in String msgId);

	void markMessageAsRead(in String msgId);

	void addEventListener2(in IOneToOneStandaloneMessagingListener listener);

	void removeEventListener2(in IOneToOneStandaloneMessagingListener listener);

	void addEventListener3(in IOneToManyStandaloneMessagingListener listener);

	void removeEventListener3(in IOneToManyStandaloneMessagingListener listener);

	boolean isAllowedToSendMessage(in ContactId contact);

	boolean isAllowedToSendMessageToMany(in List<ContactId> contacts);

	void deleteOneToOneStandaloneMessages();

	void deleteOneToManyStandaloneMessages();

	void deleteOneToOneStandaloneMessages2(in ContactId contact);

	void deleteOneToManyStandaloneMessages2(in List<ContactId> contacts);

	void deleteMessage(in String msgId);
}
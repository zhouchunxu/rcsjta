package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.chat.Card;
import com.gsma.services.rcs.chat.CloudFile;
import com.gsma.services.rcs.chat.Emoticon;
import com.gsma.services.rcs.chat.IChatMessage;
import com.gsma.services.rcs.chat.IGroupChatListener;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.Geoloc;

/**
 * Group chat interface
 */
interface IGroupChat {

	String getChatId();

	int getDirection();

	int getState();

	int getReasonCode();

	ContactId getRemoteContact();

	String getSubject();

	Map getParticipants();

	int getMaxParticipants();

	long getTimestamp();

	IChatMessage sendMessage(in String text);

	IChatMessage sendMessage2(in Geoloc geoloc);

	IChatMessage sendMessage3(in Emoticon emoticon);

	IChatMessage sendMessage4(in CloudFile cloudFile);

	IChatMessage sendMessage5(in Card card);

	IChatMessage sendCcMessage(in List<ContactId> contacts, in String text);

	IChatMessage sendAnnouncement(in String text);

	void setComposingStatus(in boolean ongoing);

	void inviteParticipants(in List<ContactId> participants);

	void removeParticipants(in List<ContactId> participants);

	void transferOwnership(in ContactId contact);

	void renameSubject(in String subject);

	void renameAlias(in String alias);

	void leave();

//	void disband();

	void openChat();

	void resendMessage(in String msgId);

	boolean isAllowedToSendMessage();

	boolean isAllowedToSendAnnouncement();

	boolean isAllowedToInviteParticipants();

	boolean isAllowedToInviteParticipant(in ContactId participant);

	boolean isAllowedToRemoveParticipants();

	boolean isAllowedToRemoveParticipant(in ContactId participant);

	boolean isAllowedToTransferOwnership();

	boolean isAllowedToTransferOwnership2(in ContactId participant);

	boolean isAllowedToRenameSubject();

	boolean isAllowedToRenameAlias();

	boolean isAllowedToLeave();

//	boolean isAllowedToDisband();
}
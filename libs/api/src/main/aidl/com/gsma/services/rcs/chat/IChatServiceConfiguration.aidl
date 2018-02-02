package com.gsma.services.rcs.chat;

/**
 * Chat service configuration interface
 */
interface IChatServiceConfiguration {

	long getIsComposingTimeout();

	long getGeolocExpirationTime();

	int getGeolocLabelMaxLength();

	int getGroupChatMaxParticipants();

	int getGroupChatMinParticipants();

	int getGroupChatSubjectMaxLength();

	int getGroupChatMessageMaxLength();

	int getOneToManyChatMessageMaxLength();

	int getOneToOneChatMessageMaxLength();

	int getStandaloneMessageMaxLength();

	boolean isStandaloneMessagingSupported();

//	boolean isChatSupported();

	boolean isChatWarnSF();

	boolean isGroupChatSupported();

	boolean isRespondToDisplayReportsEnabled();

	boolean isSmsFallback();

	void setRespondToDisplayReports(in boolean enable);
}
package com.gsma.services.rcs.chat.sms;

/**
 * Standalone messaging service configuration interface
 */
interface IStandaloneMessagingServiceConfiguration {

	int getStandaloneMessageMaxLength();

	boolean isStandaloneMessagingSupported();
}
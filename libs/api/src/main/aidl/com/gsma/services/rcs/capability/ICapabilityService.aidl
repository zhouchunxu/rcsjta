package com.gsma.services.rcs.capability;

import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.capability.Capabilities;
import com.gsma.services.rcs.capability.ICapabilitiesListener;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.ICommonServiceConfiguration;
import com.gsma.services.rcs.RcsServiceRegistration;

/**
 * Capability service API
 */
interface ICapabilityService {

	boolean isServiceRegistered();

	int getServiceRegistrationReasonCode();

	void addEventListener(IRcsServiceRegistrationListener listener);

	void removeEventListener(IRcsServiceRegistrationListener listener);

	Capabilities getMyCapabilities();

	Capabilities getContactCapabilities(in ContactId contact);

	void requestContactCapabilities(in ContactId contact);

	void requestAllContactsCapabilities();

	void requestContactCapabilities2(in List<ContactId> contacts);

	void addCapabilitiesListener(in ICapabilitiesListener listener);

	void removeCapabilitiesListener(in ICapabilitiesListener listener);

	void addCapabilitiesListener2(in ContactId contact, in ICapabilitiesListener listener);

	void removeCapabilitiesListener2(in ContactId contact, in ICapabilitiesListener listener);

	int getServiceVersion();

	ICommonServiceConfiguration getCommonConfiguration();
}

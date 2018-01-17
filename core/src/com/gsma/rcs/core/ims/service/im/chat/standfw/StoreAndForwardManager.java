/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.im.chat.standfw;

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

/**
 * Store & forward manager
 */
public class StoreAndForwardManager {
    /**
     * Store & forward service URI
     */
    public final static String SERVICE_URI = "rcse-standfw@";

    private final InstantMessagingService mImService;

    private final RcsSettings mRcsSettings;

    private final MessagingLog mMessagingLog;

    private final ContactManager mContactManager;

    private final static Logger sLogger = Logger.getLogger(StoreAndForwardManager.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param imService IMS service
     * @param rcsSettings the RCS settings accessor
     * @param contactManager the contact manager
     * @param messagingLog the messaging log accessor
     */
    public StoreAndForwardManager(InstantMessagingService imService, RcsSettings rcsSettings,
            ContactManager contactManager, MessagingLog messagingLog) {
        mImService = imService;
        mRcsSettings = rcsSettings;
        mContactManager = contactManager;
        mMessagingLog = messagingLog;
    }

    /**
     * Receive stored messages
     * 
     * @param invite Received invite
     * @param contact Contact identifier
     * @param timestamp Local timestamp when got SipRequest
     * @throws PayloadException
     * @throws NetworkException
     */
    public void receiveStoreAndForwardMessageInvitation(SipRequest invite, ContactId contact,
            long timestamp) throws PayloadException, NetworkException {
        if (sLogger.isActivated()) {
            sLogger.debug("Receive stored messages");
        }
        TerminatingStoreAndForwardOneToOneChatMessageSession session = new TerminatingStoreAndForwardOneToOneChatMessageSession(
                mImService, invite, contact, mRcsSettings, mMessagingLog, timestamp,
                mContactManager);
        mImService.receiveStoreAndForwardMsgSessionInvitation(session);
        session.startSession();
    }

    /**
     * Receive stored notifications
     *
     * @param invite Received invite
     * @param contact Contact identifier
     * @param timestamp Local timestamp when got SipRequest
     */
    public void receiveStoreAndForwardNotificationInvitation(SipRequest invite, ContactId contact,
            long timestamp) {
        if (sLogger.isActivated()) {
            sLogger.debug("Receive stored notifications");
        }
        TerminatingStoreAndForwardOneToOneChatNotificationSession session = new TerminatingStoreAndForwardOneToOneChatNotificationSession(
                mImService, invite, contact, mRcsSettings, mMessagingLog, timestamp,
                mContactManager);
        mImService.receiveStoreAndForwardNotificationSessionInvitation(session);
        session.startSession();
    }
}

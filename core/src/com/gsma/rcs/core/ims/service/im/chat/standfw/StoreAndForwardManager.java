/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.ImsService;
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

    /**
     * IMS service
     */
    private ImsService imsService;

    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(StoreAndForwardManager.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param imsService IMS service
     */
    public StoreAndForwardManager(ImsService imsService) {
        this.imsService = imsService;
    }

    /**
     * Receive stored messages
     * 
     * @param invite Received invite
     * @param contact Contact identifier
     * @param rcsSettings RCS settings
     * @param messagingLog Messaging log
     * @param timestamp Local timestamp when got SipRequest
     */
    public void receiveStoredMessages(SipRequest invite, ContactId contact,
            RcsSettings rcsSettings, MessagingLog messagingLog, long timestamp) {
        if (logger.isActivated()) {
            logger.debug("Receive stored messages");
        }
        TerminatingStoreAndForwardOneToOneChatMessageSession session = new TerminatingStoreAndForwardOneToOneChatMessageSession(
                imsService, invite, contact, rcsSettings, messagingLog, timestamp);

        imsService.getImsModule().getCore().getListener()
                .handleStoreAndForwardMsgSessionInvitation(session);

        session.startSession();
    }

    /**
     * Receive stored notifications
     * 
     * @param invite Received invite
     * @param contact Contact identifier
     * @param rcsSettings RCS settings
     * @param messagingLog Messaging log
     * @param timestamp Local timestamp when got SipRequest
     */
    public void receiveStoredNotifications(SipRequest invite, ContactId contact,
            RcsSettings rcsSettings, MessagingLog messagingLog, long timestamp) {
        if (logger.isActivated()) {
            logger.debug("Receive stored notifications");
        }
        TerminatingStoreAndForwardOneToOneChatNotificationSession session = new TerminatingStoreAndForwardOneToOneChatNotificationSession(
                imsService, invite, contact, rcsSettings, messagingLog, timestamp);

        // Start the session
        session.startSession();
    }
}

/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2017 China Mobile.
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
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.im.standalone;

import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.services.rcs.contact.ContactId;

/**
 * Standalone messaging session listener
 */
public interface StandaloneMessagingSessionListener/* extends ImsSessionListener*/ {

    /**
     * New message received
     *
     * @param chatId Chat ID
     * @param msg Chat message
     * @param imdnDisplayedRequested Indicates whether display notification is requested
     * @param deliverySuccess True is delivery report succeeded
     */
    public void onMessageReceived(String chatId, ChatMessage msg, boolean imdnDisplayedRequested,
            boolean deliverySuccess);

    /**
     * Notifying that a message has been sent
     *
     * @param chatId Chat ID
     * @param msgId Message ID
     * @param mimeType MIME type
     */
    public void onMessageSent(String chatId, String msgId, String mimeType);

    /**
     * Notifying failure of sending message
     *
     * @param chatId Chat ID
     * @param msgId Message ID
     * @param mimeType MIME type
     */
    public void onMessageFailedSend(String chatId, String msgId, String mimeType);

    /**
     * New message delivery status that are received as part of imdn notification
     *
     * @param chatId Chat ID
     * @param contact the remote contact identifier
     * @param imdn Imdn document
     */
    public void onMessageDeliveryStatusReceived(String chatId, ContactId contact, ImdnDocument imdn);

    /**
     * Handle imdn DISPLAY report sent for message
     *
     * @param chatId Chat ID
     * @param msgId
     */
    public void onMessageDisplayReportSent(String chatId, String msgId);
}

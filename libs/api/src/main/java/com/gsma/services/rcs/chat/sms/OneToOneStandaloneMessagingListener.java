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

package com.gsma.services.rcs.chat.sms;

import com.gsma.services.rcs.chat.ChatLog.Message.Content.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Set;

/**
 * One to one standalone messaging events listener
 */
public abstract class OneToOneStandaloneMessagingListener {

    /**
     * Callback called when a message status/reasonCode is changed.
     * 
     * @param chatId Chat Id
     * @param mimeType MIME-type of message
     * @param msgId Message Id
     * @param status Status
     * @param reasonCode Reason code
     */
    public abstract void onMessageStatusChanged(String chatId, String mimeType, String msgId,
            Status status, ReasonCode reasonCode);

    /**
     * Callback called when a delete operation completed that resulted in that one or several
     * standalone messages was deleted specified by the msgIds parameter corresponding to a specific
     * chat id.
     *
     * @param chatId Chat Id
     * @param msgIds Message ids of those deleted messages
     */
    public abstract void onMessagesDeleted(String chatId, Set<String> msgIds);
}

/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2017 China Mobile.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.rcs.service.broadcaster;

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.GroupChat.Participant.Status;
import com.gsma.services.rcs.chat.ChatLog.GroupChat.State;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;

import java.util.Set;

/**
 * Interface to perform broadcast events on GroupChatListener
 */
public interface IGroupChatEventBroadcaster {

    void broadcastMessageStatusChanged(String chatId, String mimeType, String msgId, com.gsma.services.rcs.chat.ChatLog.Message.Content.Status status,
            ReasonCode reasonCode);

    void broadcastMessageGroupDeliveryInfoChanged(String chatId, ContactId contact,
            String mimeType, String msgId, GroupDeliveryInfo.Status status,
            GroupDeliveryInfo.ReasonCode reasonCode);

    void broadcastSubjectChanged(String chatId, String subject);

    void broadcastOwnershipChanged(String chatId, ContactId contact);

    void broadcastParticipantAliasChanged(String chatId, ContactId contact, String alias);

    void broadcastParticipantStatusChanged(String chatId, ContactId contact,
            Status status);

    void broadcastStateChanged(String chatId, State state, ChatLog.GroupChat.ReasonCode reasonCode);

    void broadcastComposingEvent(String chatId, ContactId contact, boolean status);

    void broadcastInvitation(String chatId);

    void broadcastMessageReceived(String mimeType, String msgId);

    void broadcastMessagesDeleted(String chatId, Set<String> msgIds);

    void broadcastGroupChatsDeleted(Set<String> chatIds);
}

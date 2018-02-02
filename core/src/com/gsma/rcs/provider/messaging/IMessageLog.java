/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.provider.messaging;

import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.services.rcs.chat.ChatLog.Message.GroupChatEvent;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.contact.ContactId;

import android.database.Cursor;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface for the message table
 * 
 * @author LEMORDANT Philippe
 */
public interface IMessageLog {

    /**
     * Add a spam message
     *
     * @param chatId Chat ID
     * @param msg Chat message
     */
    void addOneToOneSpamMessage(String chatId, ChatMessage msg);

    /**
     * Add a chat message
     *
     * @param chatId Chat ID
     * @param msg Chat message
     * @param imdnDisplayedRequested IMDN display report requested
     */
    void addIncomingOneToOneChatMessage(String chatId, ChatMessage msg,
            boolean imdnDisplayedRequested);

    /**
     * Add a chat message
     *
     * @param chatId Chat ID
     * @param msg Chat message
     * @param status Message status
     * @param reasonCode Status reason code
     * @param deliveryExpiration TODO
     */
    void addOutgoingOneToOneChatMessage(String chatId, ChatMessage msg, Status status,
            ReasonCode reasonCode, long deliveryExpiration);

    /**
     * Add an one-to-many chat message
     * 
     * @param chatId Chat ID
     * @param msg Chat message
     * @param recipients the set of recipients
     * @param status Message status
     * @param reasonCode Status reason code
     * @param deliveryExpiration TODO
     */
    void addOutgoingOneToManyChatMessage(String chatId, ChatMessage msg, Set<ContactId> recipients,
            Status status, ReasonCode reasonCode, long deliveryExpiration);

    /**
     * Add an incoming group chat message
     * 
     * @param chatId Chat ID
     * @param msg Chat message
     * @param imdnDisplayedRequested IMDN display report requested
     */
    void addIncomingGroupChatMessage(String chatId, ChatMessage msg, boolean imdnDisplayedRequested);

    /**
     * Add an outgoing group chat message
     * 
     * @param chatId Chat ID
     * @param msg Chat message
     * @param recipients the set of recipients
     * @param status Message status
     * @param reasonCode Status reason code
     */
    void addOutgoingGroupChatMessage(String chatId, ChatMessage msg, Set<ContactId> recipients,
            Status status, ReasonCode reasonCode);

    /**
     * Add group chat system message
     * 
     * @param chatId Chat ID
     * @param contact Contact ID
     * @param status Status
     * @param timestamp Local timestamp when got group chat notification
     * @return the message ID created for the group chat system event
     */
    String addGroupChatEvent(String chatId, ContactId contact, GroupChatEvent.Status status,
            long timestamp);

    /**
     * Update chat message read status
     * 
     * @param msgId message ID
     * @param timestampDisplayed Displayed time
     * @return the number of rows affected.
     */
    int markMessageAsRead(String msgId, long timestampDisplayed);

    /**
     * Set chat message status and reason code. Note that this method should not be used for
     * Status.DELIVERED and Status.DISPLAYED. These states require timestamps and should be set
     * through setChatMessageStatusDelivered and setChatMessageStatusDisplayed respectively.
     * 
     * @param msgId message ID
     * @param status Message status (See restriction above)
     * @param reasonCode Message status reason code
     * @return True if an entry was updated, otherwise false
     */
    boolean setChatMessageStatusAndReasonCode(String msgId, Status status, ReasonCode reasonCode);

    /**
     * Check if the message is already persisted in db
     * 
     * @param msgId message ID
     * @return true if the message already exists in db
     */
    boolean isMessagePersisted(String msgId);

    /**
     * Check if the message is read by remote contact
     * 
     * @param msgId message ID
     * @return true if read, false if unread and null if record is not found
     */
    Boolean isMessageRead(String msgId);

    /**
     * Returns the timestamp_sent of a message
     * 
     * @param msgId message ID
     * @return timestamp_sent
     */
    Long getMessageSentTimestamp(String msgId);

    /**
     * Returns the timestamp of a message
     * 
     * @param msgId message ID
     * @return timestamp
     */
    Long getMessageTimestamp(String msgId);

    /**
     * Get message state from its unique ID
     * 
     * @param msgId message ID
     * @return State
     */
    Status getMessageStatus(String msgId);

    /**
     * Get message reason code from its unique ID
     * 
     * @param msgId message ID
     * @return reason code of the state
     */
    ReasonCode getMessageReasonCode(String msgId);

    /**
     * Get message MIME-type from its unique ID
     * 
     * @param msgId message ID
     * @return MIME-type
     */
    String getMessageMimeType(String msgId);

    /**
     * Get message data from its unique ID
     * 
     * @param msgId message ID
     * @return Cursor or null if no data exists
     */
    Cursor getChatMessageData(String msgId);

    /**
     * Get all standalone messages for specific chatId that are in queued state in ascending
     * order of timestamp
     *
     * @param chatId Chat ID
     * @return Cursor
     */
    Cursor getQueuedStandaloneMessages(String chatId);

    /**
     * Get all one-to-one chat messages for specific contact that are in queued state in ascending
     * order of timestamp
     * 
     * @param contact Contact ID
     * @return Cursor
     */
    Cursor getQueuedOneToOneChatMessages(ContactId contact);

    /**
     * Get all one-to-many chat messages for specific chatId that are in queued state in ascending
     * order of timestamp
     *
     * @param chatId Chat ID
     * @return Cursor
     */
    Cursor getQueuedOneToManyChatMessages(String chatId);

    /**
     * Gets group chat events per contacts for chat ID
     * 
     * @param chatId Chat ID
     * @return group chat events for contacts or null if there is no group chat events
     */
    Map<ContactId, GroupChatEvent.Status> getGroupChatEvents(String chatId);

    /**
     * Returns true if the chat id and contact are same for this message id.
     * 
     * @param messageId the message id
     * @return true if the message belongs to one to one conversation
     */
    boolean isOneToOneChatMessage(String messageId);

    /**
     * Set chat message delivered
     * 
     * @param msgId message ID
     * @param timestampDelivered Delivered time
     * @return True if an entry was updated, otherwise false
     */
    boolean setChatMessageStatusDelivered(String msgId, long timestampDelivered);

    /**
     * Set chat message displayed
     * 
     * @param msgId message ID
     * @param timestampDisplayed Displayed time
     * @return True if an entry was updated, otherwise false
     */
    boolean setChatMessageStatusDisplayed(String msgId, long timestampDisplayed);

    /**
     * Marks undelivered chat messages to indicate that messages have been processed.
     * 
     * @param msgIds List of message IDs
     */
    void clearMessageDeliveryExpiration(List<String> msgIds);

    /**
     * Set message delivery expired for specified message id.
     * 
     * @param msgId message ID
     * @return True if an entry was updated, otherwise false
     */
    boolean setChatMessageDeliveryExpired(String msgId);

    /**
     * Get one-one chat messages with unexpired delivery
     * 
     * @return Cursor
     */
    Cursor getUndeliveredOneToOneChatMessages();

    /**
     * Returns true if delivery for this chat message has expired or false otherwise. Note: false
     * means either that delivery for this chat message has not yet expired, delivery has been
     * successful, delivery expiration has been cleared (see clearMessageDeliveryExpiration) or that
     * this particular chat message is not eligible for delivery expiration in the first place.
     * 
     * @param msgId message ID
     * @return boolean
     */
    Boolean isChatMessageExpiredDelivery(String msgId);

    /**
     * Get chat id for chat message
     * 
     * @param msgId message ID
     * @return ChatId
     */
    String getMessageChatId(String msgId);

    /**
     * Set chat message status and sent timestamp for outgoing messages
     * 
     * @param msgId message ID
     * @param status Message status
     * @param reasonCode Message status reason code
     * @param timestamp New local timestamp
     * @param timestampSent New timestamp sent in payload
     * @return boolean
     */
    boolean setChatMessageStatusAndTimestamp(String msgId, Status status, ReasonCode reasonCode,
            long timestamp, long timestampSent);

    /**
     * Add a one to one chat message for which delivery report has failed
     *
     * @param chatId the chat ID
     * @param msg Chat message
     */
    void addOneToOneFailedDeliveryMessage(String chatId, ChatMessage msg);

    /**
     * Add a group chat message for which delivery report has failed
     * 
     * @param chatId the chat ID
     * @param msg Chat message
     */
    void addGroupChatFailedDeliveryMessage(String chatId, ChatMessage msg);
}

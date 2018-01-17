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

package com.gsma.rcs.provider.messaging;

import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;

import android.net.Uri;

/**
 * Message data constants
 * 
 * @author Jean-Marc AUFFRET
 */
public class MessageData {
    /**
     * Database URIs
     */
    public static final Uri CONTENT_URI = Uri.parse("content://com.gsma.rcs.chat/chatmessage");

    /**
     * History log member id
     */
    public static final int HISTORYLOG_MEMBER_ID = ChatLog.Message.HISTORYLOG_MEMBER_ID;

    /**
     * Unique history ID
     */
    /* package private */static final String KEY_BASECOLUMN_ID = ChatLog.Message.BASECOLUMN_ID;

    /**
     * Id of chat room
     */
    /* package private */static final String KEY_CHAT_ID = ChatLog.Message.CHAT_ID;

    /**
     * Conversation Id. Conversation id can't be the unique id when the race condition happened in
     * 1-2-1, use chat id as the unique key.
     */
    /* package private */static final String KEY_CONVERSATION_ID = ChatLog.Message.CONVERSATION_ID;

    /**
     * ContactId formatted number list of remote contacts with semicolon separated or null if the
     * message is an outgoing group chat message.
     */
    /* package private */static final String KEY_CONTACT = ChatLog.Message.CONTACT;

    /**
     * ContactId formatted number list of courtesy copy contacts with semicolon separated in a chat
     * message.
     */
    /* package private */static final String KEY_COURTESY_COPY = ChatLog.Message.COURTESY_COPY;

    /**
     * Id of the message
     */
    /* package private */static final String KEY_MESSAGE_ID = ChatLog.Message.MESSAGE_ID;

    /**
     * Content of the message (as defined by one of the mimetypes in ChatLog.Message.Mimetype)
     */
    /* package private */static final String KEY_CONTENT = ChatLog.Message.CONTENT;

    /**
     * Multipurpose Internet Mail Extensions (MIME) type of message
     */
    /* package private */static final String KEY_MIME_TYPE = ChatLog.Message.MIME_TYPE;

    /**
     * Status direction of message.
     * 
     * @see Direction
     */
    /* package private */static final String KEY_DIRECTION = ChatLog.Message.DIRECTION;

    /**
     * @see Status
     */
    /* package private */static final String KEY_STATUS = ChatLog.Message.STATUS;

    /**
     * Reason code associated with the message status.
     * 
     * @see ReasonCode
     */
    /* package private */static final String KEY_REASON_CODE = ChatLog.Message.REASON_CODE;

    /**
     * This is set on the receiver side when the message has been displayed.
     * 
     * @see RcsService.ReadStatus for the list of status.
     */
    /* package private */static final String KEY_READ_STATUS = ChatLog.Message.READ_STATUS;

    /**
     * Time when message inserted
     */
    /* package private */static final String KEY_TIMESTAMP = ChatLog.Message.TIMESTAMP;

    /**
     * Time when message sent. If 0 means not sent.
     */
    /* package private */static final String KEY_TIMESTAMP_SENT = ChatLog.Message.TIMESTAMP_SENT;

    /**
     * Time when message delivered. If 0 means not delivered
     */
    /* package private */static final String KEY_TIMESTAMP_DELIVERED = ChatLog.Message.TIMESTAMP_DELIVERED;

    /**
     * Time when message displayed. If 0 means not displayed.
     */
    /* package private */static final String KEY_TIMESTAMP_DISPLAYED = ChatLog.Message.TIMESTAMP_DISPLAYED;

    /**
     * If delivery has expired for this message. Values: 1 (true), 0 (false)
     */
    /* package private */static final String KEY_EXPIRED_DELIVERY = ChatLog.Message.EXPIRED_DELIVERY;

    /**
     * Time when message delivery time-out will expire or 0 if this message is not eligible for
     * delivery expiration.
     */
    /* package private */static final String KEY_DELIVERY_EXPIRATION = "delivery_expiration";

    /**
     * Device type of the message sender. Values: 1 (PC).
     */
    /* package private */static final String KEY_DEVICE_TYPE = ChatLog.Message.DEVICE_TYPE;

    /**
     * If message should be silence to user. Values: 1 (true), 0 (false).
     */
    /* package private */static final String KEY_SILENCE = ChatLog.Message.SILENCE;

    /**
     * If message is in burn-after-reading cycle. Values: 1 (true), 0 (false).
     */
    /* package private */static final String KEY_BAR_CYCLE = ChatLog.Message.BAR_CYCLE;
}

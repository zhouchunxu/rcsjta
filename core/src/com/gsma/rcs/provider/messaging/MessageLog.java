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
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.chat.ChatLog.Message.GroupChatEvent;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class to interface the message table
 */
public class MessageLog implements IMessageLog {

    private LocalContentResolver mLocalContentResolver;

    private GroupDeliveryInfoLog mGroupChatDeliveryInfoLog;

    private final RcsSettings mRcsSettings;

    private static final Logger sLogger = Logger.getLogger(MessageLog.class.getSimpleName());

    private static final String[] PROJECTION_MESSAGE_ID = new String[] {
        MessageData.KEY_MESSAGE_ID
    };

    private static final String[] PROJECTION_GROUP_CHAT_EVENTS = new String[] {
            MessageData.KEY_STATUS, MessageData.KEY_CONTACT
    };

    private static final String SELECTION_GROUP_CHAT_EVENTS = MessageData.KEY_CHAT_ID + "=? AND "
            + MessageData.KEY_MIME_TYPE + "='" + MimeType.GROUPCHAT_EVENT + "' GROUP BY "
            + MessageData.KEY_CONTACT;

    private static final int FIRST_COLUMN_IDX = 0;

    private static final String SELECTION_QUEUED_STANDALONE_MESSAGES = MessageData.KEY_CHAT_ID
            + "=? AND " + MessageData.KEY_STATUS + "=" + Status.SMS_QUEUED.toInt();

    private static final String SELECTION_QUEUED_ONETOONE_CHAT_MESSAGES = MessageData.KEY_CHAT_ID
            + "=? AND " + MessageData.KEY_STATUS + "=" + Status.QUEUED.toInt();

    private static final int CHAT_MESSAGE_DELIVERY_EXPIRED = 1;

    private static final int CHAT_MESSAGE_DELIVERY_EXPIRATION_NOT_APPLICABLE = 0;

    private static final String SELECTION_BY_UNDELIVERED_ONETOONE_CHAT_MESSAGES = MessageData.KEY_EXPIRED_DELIVERY
            + "<>"
            + CHAT_MESSAGE_DELIVERY_EXPIRED
            + " AND "
            + MessageData.KEY_DELIVERY_EXPIRATION
            + "<>"
            + CHAT_MESSAGE_DELIVERY_EXPIRATION_NOT_APPLICABLE
            + " AND "
            + MessageData.KEY_STATUS
            + " NOT IN("
            + Status.DELIVERED.toInt()
            + ","
            + Status.DISPLAYED.toInt() + ")";
    private static final String SELECTION_BY_UNDELIVERED_STATUS = MessageData.KEY_STATUS
            + " NOT IN(" + Status.DELIVERED.toInt() + "," + Status.DISPLAYED.toInt() + ")";

    private static final String ORDER_BY_TIMESTAMP_ASC = MessageData.KEY_TIMESTAMP.concat(" ASC");

    private static final String SELECTION_BY_NOT_DISPLAYED = MessageData.KEY_STATUS + "<>"
            + Status.DISPLAYED.toInt();

    private static final String SELECTION_BY_NOT_READ = MessageData.KEY_READ_STATUS + "="
            + ReadStatus.UNREAD.toInt();

    /**
     * Constructor
     * 
     * @param localContentResolver Local content resolver
     * @param groupChatDeliveryInfoLog the GC delivery information
     * @param rcsSettings the RCS settings accessor
     */
    /* package private */MessageLog(LocalContentResolver localContentResolver,
            GroupDeliveryInfoLog groupChatDeliveryInfoLog, RcsSettings rcsSettings) {
        mLocalContentResolver = localContentResolver;
        mGroupChatDeliveryInfoLog = groupChatDeliveryInfoLog;
        mRcsSettings = rcsSettings;
    }

    private void addIncomingOneToOneMessage(String chatId, ChatMessage msg, Status status,
            ReasonCode reasonCode) {
        ContactId contact = msg.getRemoteContact();
        String msgId = msg.getMessageId();
        if (sLogger.isActivated()) {
            sLogger.debug("Add incoming chat message: contact=" + contact + ", msgId=" + msgId
                    + ", status=" + status + ", reasonCode=" + reasonCode + ".");
        }
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_CHAT_ID, chatId);
        values.put(MessageData.KEY_MESSAGE_ID, msgId);
        values.put(MessageData.KEY_CONTACT, contact.toString());
        values.put(MessageData.KEY_DIRECTION, Direction.INCOMING.toInt());
        values.put(MessageData.KEY_READ_STATUS, ReadStatus.UNREAD.toInt());
        values.put(MessageData.KEY_MIME_TYPE, msg.getMimeType());
        values.put(MessageData.KEY_CONTENT, msg.getContent());
        values.put(MessageData.KEY_TIMESTAMP, msg.getTimestamp());
        values.put(MessageData.KEY_TIMESTAMP_SENT, msg.getTimestampSent());
        values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
        values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);
        values.put(MessageData.KEY_DELIVERY_EXPIRATION, 0);
        values.put(MessageData.KEY_EXPIRED_DELIVERY, 0);
        values.put(MessageData.KEY_STATUS, status.toInt());
        values.put(MessageData.KEY_REASON_CODE, reasonCode.toInt());
        mLocalContentResolver.insert(MessageData.CONTENT_URI, values);
    }

    @Override
    public void addOutgoingOneToOneChatMessage(String chatId, ChatMessage msg, Status status,
            ReasonCode reasonCode, long deliveryExpiration) {
        ContactId contact = msg.getRemoteContact();
        String msgId = msg.getMessageId();
        if (sLogger.isActivated()) {
            sLogger.debug("Add outgoing chat message: contact=" + contact + ", msgId=" + msgId
                    + ", status=" + status + ", reasonCode=" + reasonCode + ".");
        }
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_CHAT_ID, chatId);
        values.put(MessageData.KEY_MESSAGE_ID, msgId);
        values.put(MessageData.KEY_CONTACT, contact.toString());
        values.put(MessageData.KEY_DIRECTION, Direction.OUTGOING.toInt());
        values.put(MessageData.KEY_READ_STATUS, ReadStatus.UNREAD.toInt());
        values.put(MessageData.KEY_MIME_TYPE, msg.getMimeType());
        values.put(MessageData.KEY_CONTENT, msg.getContent());
        values.put(MessageData.KEY_TIMESTAMP, msg.getTimestamp());
        values.put(MessageData.KEY_TIMESTAMP_SENT, msg.getTimestampSent());
        values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
        values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);
        values.put(MessageData.KEY_DELIVERY_EXPIRATION, deliveryExpiration);
        values.put(MessageData.KEY_EXPIRED_DELIVERY, 0);
        values.put(MessageData.KEY_STATUS, status.toInt());
        values.put(MessageData.KEY_REASON_CODE, reasonCode.toInt());
        mLocalContentResolver.insert(MessageData.CONTENT_URI, values);
    }

    @Override
    public void addOutgoingOneToManyChatMessage(String chatId, ChatMessage msg,
            Set<ContactId> recipients, Status status, ReasonCode reasonCode, long deliveryExpiration) {
        String msgId = msg.getMessageId();
        if (sLogger.isActivated()) {
            sLogger.debug("Add outgoing one-to-many chat message: chatId=" + chatId + ", msgId="
                    + msgId + ", status=" + status + ", reasonCode=" + reasonCode + ".");
        }
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_CHAT_ID, chatId);
        values.put(MessageData.KEY_MESSAGE_ID, msgId);
        values.put(MessageData.KEY_CONTACT, "");//TODO
        values.put(MessageData.KEY_DIRECTION, Direction.OUTGOING.toInt());
        values.put(MessageData.KEY_READ_STATUS, ReadStatus.UNREAD.toInt());
        values.put(MessageData.KEY_MIME_TYPE, msg.getMimeType());
        values.put(MessageData.KEY_CONTENT, msg.getContent());
        values.put(MessageData.KEY_TIMESTAMP, msg.getTimestamp());
        values.put(MessageData.KEY_TIMESTAMP_SENT, msg.getTimestampSent());
        values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
        values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);
        values.put(MessageData.KEY_DELIVERY_EXPIRATION, deliveryExpiration);
        values.put(MessageData.KEY_EXPIRED_DELIVERY, 0);
        values.put(MessageData.KEY_STATUS, status.toInt());
        values.put(MessageData.KEY_REASON_CODE, reasonCode.toInt());
        mLocalContentResolver.insert(MessageData.CONTENT_URI, values);
    }

    @Override
    public void addOneToOneSpamMessage(String chatId, ChatMessage msg) {
        addIncomingOneToOneMessage(chatId, msg, Status.REJECTED, ReasonCode.REJECTED_SPAM);
    }

    @Override
    public void addOneToOneFailedDeliveryMessage(String chatId, ChatMessage msg) {
        addIncomingOneToOneMessage(chatId, msg, Status.FAILED, ReasonCode.FAILED_DELIVERY);
    }

    @Override
    public void addIncomingOneToOneChatMessage(String chatId, ChatMessage msg,
            boolean imdnDisplayedRequested) {
        if (imdnDisplayedRequested) {
            addIncomingOneToOneMessage(chatId, msg, Status.DISPLAY_REPORT_REQUESTED,
                    ReasonCode.UNSPECIFIED);
        } else {
            addIncomingOneToOneMessage(chatId, msg, Status.RECEIVED, ReasonCode.UNSPECIFIED);
        }
    }

    @Override
    public void addIncomingGroupChatMessage(String chatId, ChatMessage msg,
            boolean imdnDisplayedRequested) {
        Status chatMessageStatus = imdnDisplayedRequested ? Status.DISPLAY_REPORT_REQUESTED
                : Status.RECEIVED;
        addGroupChatMessage(chatId, msg, Direction.INCOMING, null, chatMessageStatus,
                ReasonCode.UNSPECIFIED);
    }

    @Override
    public void addOutgoingGroupChatMessage(String chatId, ChatMessage msg,
            Set<ContactId> recipients, Status status, ReasonCode reasonCode) {
        addGroupChatMessage(chatId, msg, Direction.OUTGOING, recipients, status, reasonCode);
    }

    /**
     * Add group chat message
     * 
     * @param chatId Chat ID
     * @param msg Chat message
     * @param direction Direction
     * @param status Status
     * @param reasonCode Reason code
     */
    private void addGroupChatMessage(String chatId, ChatMessage msg, Direction direction,
            Set<ContactId> recipients, Status status, ReasonCode reasonCode) {
        String msgId = msg.getMessageId();
        ContactId contact = msg.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.debug("Add group chat message; chatId=" + chatId + ", msg=" + msgId + ", dir="
                    + direction + ", contact=" + contact + ".");
        }
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_CHAT_ID, chatId);
        values.put(MessageData.KEY_MESSAGE_ID, msgId);
        if (contact != null) {
            values.put(MessageData.KEY_CONTACT, contact.toString());
        }
        values.put(MessageData.KEY_DIRECTION, direction.toInt());
        values.put(MessageData.KEY_READ_STATUS, ReadStatus.UNREAD.toInt());
        values.put(MessageData.KEY_STATUS, status.toInt());
        values.put(MessageData.KEY_REASON_CODE, reasonCode.toInt());
        values.put(MessageData.KEY_MIME_TYPE, msg.getMimeType());
        values.put(MessageData.KEY_CONTENT, msg.getContent());
        values.put(MessageData.KEY_TIMESTAMP, msg.getTimestamp());
        values.put(MessageData.KEY_TIMESTAMP_SENT, msg.getTimestampSent());
        values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
        values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);
        values.put(MessageData.KEY_DELIVERY_EXPIRATION, 0);
        values.put(MessageData.KEY_EXPIRED_DELIVERY, 0);
        mLocalContentResolver.insert(MessageData.CONTENT_URI, values);
        if (Direction.OUTGOING == direction) {
            try {
                GroupDeliveryInfo.Status deliveryStatus = GroupDeliveryInfo.Status.NOT_DELIVERED;
                if (mRcsSettings.isAlbatrosRelease()) {
                    deliveryStatus = GroupDeliveryInfo.Status.UNSUPPORTED;
                }
                for (ContactId recipient : recipients) {
                    /* Add entry with delivered and displayed timestamps set to 0. */
                    mGroupChatDeliveryInfoLog.addGroupChatDeliveryInfoEntry(chatId, recipient,
                            msgId, deliveryStatus, GroupDeliveryInfo.ReasonCode.UNSPECIFIED, 0, 0);
                }
            } catch (Exception e) {
                mLocalContentResolver.delete(Uri.withAppendedPath(MessageData.CONTENT_URI, msgId),
                        null, null);
                mLocalContentResolver.delete(
                        Uri.withAppendedPath(GroupDeliveryInfoData.CONTENT_URI, msgId), null, null);
                if (sLogger.isActivated()) {
                    sLogger.warn("Group chat message with msgId '" + msgId
                            + "' could not be added to database!");
                }
            }
        }
    }

    @Override
    public String addGroupChatEvent(String chatId, ContactId contact, GroupChatEvent.Status status,
            long timestamp) {
        if (sLogger.isActivated()) {
            sLogger.debug("Add group chat system message: chatID=" + chatId + ", contact="
                    + contact + ", status=" + status);
        }
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_CHAT_ID, chatId);
        if (contact != null) {
            values.put(MessageData.KEY_CONTACT, contact.toString());
        }
        String msgId = IdGenerator.generateMessageID();
        values.put(MessageData.KEY_MESSAGE_ID, msgId);
        values.put(MessageData.KEY_MIME_TYPE, MimeType.GROUPCHAT_EVENT);
        values.put(MessageData.KEY_STATUS, status.toInt());
        values.put(MessageData.KEY_REASON_CODE, ReasonCode.UNSPECIFIED.toInt());
        values.put(MessageData.KEY_DIRECTION, Direction.IRRELEVANT.toInt());
        values.put(MessageData.KEY_READ_STATUS, ReadStatus.UNREAD.toInt());
        values.put(MessageData.KEY_TIMESTAMP, timestamp);
        values.put(MessageData.KEY_TIMESTAMP_SENT, timestamp);
        values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
        values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);
        values.put(MessageData.KEY_DELIVERY_EXPIRATION, 0);
        values.put(MessageData.KEY_EXPIRED_DELIVERY, 0);
        mLocalContentResolver.insert(MessageData.CONTENT_URI, values);
        return msgId;
    }

    @Override
    public int markMessageAsRead(String msgId, long timestampDisplayed) {
        if (sLogger.isActivated()) {
            sLogger.debug("Mark chat message as read ID=" + msgId);
        }
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_READ_STATUS, ReadStatus.READ.toInt());
        values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, timestampDisplayed);
        return mLocalContentResolver.update(Uri.withAppendedPath(MessageData.CONTENT_URI, msgId),
                values, SELECTION_BY_NOT_READ, null);
    }

    @Override
    public boolean setChatMessageStatusAndReasonCode(String msgId, Status status,
            ReasonCode reasonCode) {
        if (sLogger.isActivated()) {
            sLogger.debug("Update chat message: msgId=" + msgId + ", status=" + status
                    + ", reasonCode=" + reasonCode);
        }
        switch (status) {
            case DELIVERED:
            case DISPLAYED:
                throw new IllegalArgumentException("Status that requires "
                        + "timestamp passed, use specific method taking timestamp"
                        + " to set status " + status.toString());
            default:
        }
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_STATUS, status.toInt());
        values.put(MessageData.KEY_REASON_CODE, reasonCode.toInt());
        return mLocalContentResolver.update(Uri.withAppendedPath(MessageData.CONTENT_URI, msgId),
                values, SELECTION_BY_UNDELIVERED_STATUS, null) > 0;
    }

    @Override
    public boolean isMessagePersisted(String msgId) {
        Cursor cursor = null;
        Uri contentUri = Uri.withAppendedPath(MessageData.CONTENT_URI, msgId);
        try {
            cursor = mLocalContentResolver.query(contentUri, PROJECTION_MESSAGE_ID, null, null,
                    null);
            CursorUtil.assertCursorIsNotNull(cursor, contentUri);
            return cursor.moveToNext();
        } finally {
            CursorUtil.close(cursor);
        }
    }

    private Cursor getMessageData(String columnName, String msgId) {
        String[] projection = new String[] {
            columnName
        };
        Uri contentUri = Uri.withAppendedPath(MessageData.CONTENT_URI, msgId);
        Cursor cursor = mLocalContentResolver.query(contentUri, projection, null, null, null);
        CursorUtil.assertCursorIsNotNull(cursor, contentUri);
        if (!cursor.moveToNext()) {
            CursorUtil.close(cursor);
            return null;
        }
        return cursor;
    }

    private Integer getDataAsInteger(Cursor cursor) {
        try {
            if (cursor.isNull(FIRST_COLUMN_IDX)) {
                return null;
            }
            return cursor.getInt(FIRST_COLUMN_IDX);

        } finally {
            CursorUtil.close(cursor);
        }
    }

    private Long getDataAsLong(Cursor cursor) {
        try {
            if (cursor.isNull(FIRST_COLUMN_IDX)) {
                return null;
            }
            return cursor.getLong(FIRST_COLUMN_IDX);

        } finally {
            CursorUtil.close(cursor);
        }
    }

    private String getDataAsString(Cursor cursor) {
        try {
            return cursor.getString(FIRST_COLUMN_IDX);

        } finally {
            CursorUtil.close(cursor);
        }
    }

    private Boolean getDataAsBoolean(Cursor cursor) {
        try {
            if (cursor.isNull(FIRST_COLUMN_IDX)) {
                return null;
            }
            return cursor.getInt(FIRST_COLUMN_IDX) == 1;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    @Override
    public Boolean isMessageRead(String msgId) {
        Cursor cursor = getMessageData(MessageData.KEY_READ_STATUS, msgId);
        if (cursor == null) {
            return null;
        }
        return (getDataAsInteger(cursor) == ReadStatus.READ.toInt());
    }

    @Override
    public Long getMessageSentTimestamp(String msgId) {
        Cursor cursor = getMessageData(MessageData.KEY_TIMESTAMP_SENT, msgId);
        if (cursor == null) {
            return null;
        }
        return getDataAsLong(cursor);
    }

    @Override
    public Long getMessageTimestamp(String msgId) {
        Cursor cursor = getMessageData(MessageData.KEY_TIMESTAMP, msgId);
        if (cursor == null) {
            return null;
        }
        return getDataAsLong(cursor);
    }

    @Override
    public Status getMessageStatus(String msgId) {
        Cursor cursor = getMessageData(MessageData.KEY_STATUS, msgId);
        if (cursor == null) {
            return null;
        }
        return Status.valueOf(getDataAsInteger(cursor));
    }

    @Override
    public ReasonCode getMessageReasonCode(String msgId) {
        Cursor cursor = getMessageData(MessageData.KEY_REASON_CODE, msgId);
        if (cursor == null) {
            return null;
        }
        return ReasonCode.valueOf(getDataAsInteger(cursor));
    }

    @Override
    public String getMessageMimeType(String msgId) {
        Cursor cursor = getMessageData(MessageData.KEY_MIME_TYPE, msgId);
        if (cursor == null) {
            return null;
        }
        return getDataAsString(cursor);
    }

    @Override
    public String getMessageChatId(String msgId) {
        Cursor cursor = getMessageData(MessageData.KEY_CHAT_ID, msgId);
        if (cursor == null) {
            return null;
        }
        return getDataAsString(cursor);
    }

    @Override
    public Boolean isChatMessageExpiredDelivery(String msgId) {
        Cursor cursor = getMessageData(MessageData.KEY_EXPIRED_DELIVERY, msgId);
        if (cursor == null) {
            return null;
        }
        return getDataAsBoolean(cursor);
    }

    @Override
    public Cursor getChatMessageData(String msgId) {
        Uri contentUri = Uri.withAppendedPath(MessageData.CONTENT_URI, msgId);
        Cursor cursor = mLocalContentResolver.query(contentUri, null, null, null, null);
        CursorUtil.assertCursorIsNotNull(cursor, contentUri);
        return cursor;
    }

    @Override
    public Cursor getQueuedStandaloneMessages(String chatId) {
        String[] selectionArgs = new String[] {
                chatId
        };
        Cursor cursor = mLocalContentResolver.query(MessageData.CONTENT_URI, null,
                SELECTION_QUEUED_STANDALONE_MESSAGES, selectionArgs, ORDER_BY_TIMESTAMP_ASC);
        CursorUtil.assertCursorIsNotNull(cursor, MessageData.CONTENT_URI);
        return cursor;
    }

    @Override
    public Cursor getQueuedOneToOneChatMessages(ContactId contact) {
        String[] selectionArgs = new String[] {
            contact.toString()
        };
        Cursor cursor = mLocalContentResolver.query(MessageData.CONTENT_URI, null,
                SELECTION_QUEUED_ONETOONE_CHAT_MESSAGES, selectionArgs, ORDER_BY_TIMESTAMP_ASC);
        CursorUtil.assertCursorIsNotNull(cursor, MessageData.CONTENT_URI);
        return cursor;
    }

    @Override
    public Map<ContactId, GroupChatEvent.Status> getGroupChatEvents(String chatId) {
        String[] selectionArgs = new String[] {
            chatId
        };
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(MessageData.CONTENT_URI,
                    PROJECTION_GROUP_CHAT_EVENTS, SELECTION_GROUP_CHAT_EVENTS, selectionArgs,
                    ORDER_BY_TIMESTAMP_ASC);
            CursorUtil.assertCursorIsNotNull(cursor, MessageData.CONTENT_URI);
            if (!cursor.moveToNext()) {
                return Collections.emptyMap();
            }
            Map<ContactId, GroupChatEvent.Status> groupChatEvents = new HashMap<>();
            int columnIdxStatus = cursor.getColumnIndexOrThrow(MessageData.KEY_STATUS);
            int columnIdxContact = cursor.getColumnIndexOrThrow(MessageData.KEY_CONTACT);
            do {
                GroupChatEvent.Status status = GroupChatEvent.Status.valueOf(cursor
                        .getInt(columnIdxStatus));
                ContactId contact = ContactUtil.createContactIdFromTrustedData(cursor
                        .getString(columnIdxContact));
                groupChatEvents.put(contact, status);
            } while (cursor.moveToNext());
            return groupChatEvents;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    @Override
    public boolean isOneToOneChatMessage(String msgId) {
        Cursor cursor = null;
        try {
            Uri contentUri = Uri.withAppendedPath(MessageData.CONTENT_URI, msgId);
            cursor = mLocalContentResolver.query(contentUri, new String[] {
                    MessageData.KEY_CONTACT, MessageData.KEY_CHAT_ID
            }, null, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, contentUri);
            if (!cursor.moveToNext()) {
                return false;
            }
            String contactId = cursor.getString(cursor
                    .getColumnIndexOrThrow(MessageData.KEY_CONTACT));
            String chatId = cursor.getString(cursor.getColumnIndexOrThrow(MessageData.KEY_CHAT_ID));
            return chatId.equals(contactId);

        } finally {
            CursorUtil.close(cursor);
        }
    }

    @Override
    public boolean setChatMessageStatusDelivered(String msgId, long timestampDelivered) {
        if (sLogger.isActivated()) {
            sLogger.debug("setChatMessageStatusDelivered msgId=" + msgId + ", timestampDelivered="
                    + timestampDelivered);
        }
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_STATUS, Status.DELIVERED.toInt());
        values.put(MessageData.KEY_REASON_CODE, ReasonCode.UNSPECIFIED.toInt());
        values.put(MessageData.KEY_TIMESTAMP_DELIVERED, timestampDelivered);
        values.put(MessageData.KEY_EXPIRED_DELIVERY, 0);
        return mLocalContentResolver.update(Uri.withAppendedPath(MessageData.CONTENT_URI, msgId),
                values, SELECTION_BY_NOT_DISPLAYED, null) > 0;
    }

    @Override
    public boolean setChatMessageStatusDisplayed(String msgId, long timestampDisplayed) {
        if (sLogger.isActivated()) {
            sLogger.debug("setChatMessageStatusDisplayed msgId=" + msgId + ", timestampDisplayed="
                    + timestampDisplayed);
        }
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_STATUS, Status.DISPLAYED.toInt());
        values.put(MessageData.KEY_REASON_CODE, ReasonCode.UNSPECIFIED.toInt());
        values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, timestampDisplayed);
        values.put(MessageData.KEY_EXPIRED_DELIVERY, 0);
        return mLocalContentResolver.update(Uri.withAppendedPath(MessageData.CONTENT_URI, msgId),
                values, null, null) > 0;
    }

    @Override
    public void clearMessageDeliveryExpiration(List<String> msgIds) {
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_DELIVERY_EXPIRATION, 0);
        values.put(MessageData.KEY_EXPIRED_DELIVERY, 0);
        List<String> parameters = new ArrayList<>();
        for (int i = 0; i < msgIds.size(); i++) {
            parameters.add("?");
        }
        String selection = MessageData.KEY_MESSAGE_ID + " IN (" + TextUtils.join(",", parameters)
                + ")";
        mLocalContentResolver.update(MessageData.CONTENT_URI, values, selection,
                msgIds.toArray(new String[msgIds.size()]));
    }

    @Override
    public boolean setChatMessageDeliveryExpired(String msgId) {
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_EXPIRED_DELIVERY, 1);
        return mLocalContentResolver.update(Uri.withAppendedPath(MessageData.CONTENT_URI, msgId),
                values, null, null) > 0;
    }

    @Override
    public Cursor getUndeliveredOneToOneChatMessages() {
        Cursor cursor = mLocalContentResolver.query(MessageData.CONTENT_URI, null,
                SELECTION_BY_UNDELIVERED_ONETOONE_CHAT_MESSAGES, null, ORDER_BY_TIMESTAMP_ASC);
        CursorUtil.assertCursorIsNotNull(cursor, MessageData.CONTENT_URI);
        return cursor;
    }

    @Override
    public boolean setChatMessageStatusAndTimestamp(String msgId, Status status,
            ReasonCode reasonCode, long timestamp, long timestampSent) {
        if (sLogger.isActivated()) {
            sLogger.debug("Update chat message: msgId=" + msgId + ", status=" + status
                    + ", reasonCode=" + reasonCode + ", timestamp=" + timestamp
                    + ", timestampSent=" + timestampSent);
        }
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_STATUS, status.toInt());
        values.put(MessageData.KEY_REASON_CODE, reasonCode.toInt());
        values.put(MessageData.KEY_TIMESTAMP, timestamp);
        values.put(MessageData.KEY_TIMESTAMP_SENT, timestampSent);
        return mLocalContentResolver.update(Uri.withAppendedPath(MessageData.CONTENT_URI, msgId),
                values, null, null) > 0;
    }

    @Override
    public void addGroupChatFailedDeliveryMessage(String chatId, ChatMessage msg) {
        addGroupChatMessage(chatId, msg, Direction.INCOMING, null, Status.FAILED,
                ReasonCode.FAILED_DELIVERY);
    }
}

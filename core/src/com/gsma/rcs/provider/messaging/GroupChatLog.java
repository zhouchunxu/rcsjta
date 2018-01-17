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

import com.gsma.rcs.core.ims.service.im.chat.GroupChatInfo;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatLog.GroupChat.Participant.Status;
import com.gsma.services.rcs.chat.ChatLog.GroupChat.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.GroupChat.State;
import com.gsma.services.rcs.contact.ContactId;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.SparseArray;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class interfaces the chat table
 */
public class GroupChatLog implements IGroupChatLog {

    private static final String PARTICIPANT_INFO_PARTICIPANT_SEPARATOR = ",";

    private static final String PARTICIPANT_INFO_STATUS_SEPARATOR = "=";

    private final LocalContentResolver mLocalContentResolver;

    private final static String SELECT_CHAT_ID_STATUS_REJECTED = GroupChatData.KEY_STATE + "="
            + ChatLog.GroupChat.State.ABORTED.toInt() + " AND " + GroupChatData.KEY_REASON_CODE + "="
            + ChatLog.GroupChat.ReasonCode.ABORTED_BY_USER.toInt() + " AND " + GroupChatData.KEY_USER_ABORTION + "="
            + UserAbortion.SERVER_NOT_NOTIFIED.toInt();

    private static final String SELECT_ACTIVE_GROUP_CHATS = GroupChatData.KEY_STATE + "="
            + ChatLog.GroupChat.State.STARTED.toInt();

    // @formatter:off
    private static final String[] PROJECTION_GC_INFO = new String[] {
        GroupChatData.KEY_CHAT_ID,
        GroupChatData.KEY_REJOIN_ID,
        GroupChatData.KEY_PARTICIPANTS,
        GroupChatData.KEY_SUBJECT,
        GroupChatData.KEY_TIMESTAMP
    };
    // @formatter:on

    private static final String[] PROJECTION_GC_CHAT_ID = new String[] {
        GroupChatData.KEY_CHAT_ID
    };

    private static final Logger sLogger = Logger.getLogger(GroupChatLog.class.getSimpleName());

    private static final int FIRST_COLUMN_IDX = 0;

    private enum UserAbortion {

        SERVER_NOTIFIED(0), SERVER_NOT_NOTIFIED(1);

        private final int mValue;

        private static SparseArray<UserAbortion> mValueToEnum = new SparseArray<>();
        static {
            for (UserAbortion entry : UserAbortion.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        UserAbortion(int value) {
            mValue = value;
        }

        public final int toInt() {
            return mValue;
        }
    }

    /**
     * Constructor
     * 
     * @param localContentResolver Local content resolver
     */
    /* package private */GroupChatLog(LocalContentResolver localContentResolver) {
        mLocalContentResolver = localContentResolver;
    }

    /**
     * Convert participants to string representation
     * 
     * @param participants the participants
     * @return the string with comma separated values of key pairs formatted as follows: "key=value"
     */
    private String generateEncodedParticipantInfos(Map<ContactId, Status> participants) {
        StringBuilder builder = new StringBuilder();
        int size = participants.size();
        for (Map.Entry<ContactId, Status> participant : participants.entrySet()) {
            builder.append(participant.getKey());
            builder.append(PARTICIPANT_INFO_STATUS_SEPARATOR);
            builder.append(participant.getValue().toInt());
            if (--size != 0) {
                builder.append(PARTICIPANT_INFO_PARTICIPANT_SEPARATOR);
            }
        }
        return builder.toString();
    }

    /**
     * Convert string representation of participants into participants
     * 
     * @param participants the participants
     * @return the participants and their individual status
     */
    private Map<ContactId, Status> parseEncodedParticipantInfos(String participants) {
        String[] encodedParticipantInfos = participants
                .split(PARTICIPANT_INFO_PARTICIPANT_SEPARATOR);
        Map<ContactId, Status> participantInfos = new HashMap<>();
        for (String encodedParticipantInfo : encodedParticipantInfos) {
            String[] participantInfo = encodedParticipantInfo
                    .split(PARTICIPANT_INFO_STATUS_SEPARATOR);
            ContactId participant = ContactUtil.createContactIdFromTrustedData(participantInfo[0]);
            Status status = Status.valueOf(Integer
                    .parseInt(participantInfo[1]));
            participantInfos.put(participant, status);
        }
        return participantInfos;
    }

    @Override
    public void addGroupChat(String chatId, ContactId contact, String subject,
                             Map<ContactId, Status> participants, State state, ReasonCode reasonCode,
                             Direction direction, long timestamp) {
        String encodedParticipants = generateEncodedParticipantInfos(participants);
        if (sLogger.isActivated()) {
            sLogger.debug("addGroupChat; chatID=" + chatId + ", subject=" + subject + ", state="
                    + state + " reasonCode=" + reasonCode + ", direction=" + direction
                    + ", timestamp=" + timestamp + ", participants=" + encodedParticipants);
        }
        ContentValues values = new ContentValues();
        values.put(GroupChatData.KEY_CHAT_ID, chatId);
        if (contact != null) {
            values.put(GroupChatData.KEY_CONTACT, contact.toString());
        }
        values.put(GroupChatData.KEY_STATE, state.toInt());
        values.put(GroupChatData.KEY_REASON_CODE, reasonCode.toInt());
        values.put(GroupChatData.KEY_SUBJECT, subject);

        values.put(GroupChatData.KEY_PARTICIPANTS, encodedParticipants);
        values.put(GroupChatData.KEY_DIRECTION, direction.toInt());
        values.put(GroupChatData.KEY_TIMESTAMP, timestamp);
        values.put(GroupChatData.KEY_USER_ABORTION, UserAbortion.SERVER_NOTIFIED.toInt());
        mLocalContentResolver.insert(GroupChatData.CONTENT_URI, values);
    }

    @Override
    public void acceptGroupChatNextInvitation(String chatId) {
        if (sLogger.isActivated()) {
            sLogger.debug("acceptGroupChatNextInvitation (chatId=" + chatId + ")");
        }
        ContentValues values = new ContentValues();
        values.put(GroupChatData.KEY_USER_ABORTION, UserAbortion.SERVER_NOTIFIED.toInt());
        mLocalContentResolver.update(Uri.withAppendedPath(GroupChatData.CONTENT_URI, chatId),
                values, SELECT_CHAT_ID_STATUS_REJECTED, null);
    }

    @Override
    public boolean setGroupChatParticipantsStateAndReasonCode(String chatId,
                                                              Map<ContactId, Status> participants, State state, ReasonCode reasonCode) {
        String encodedParticipants = generateEncodedParticipantInfos(participants);
        if (sLogger.isActivated()) {
            sLogger.debug("setGCParticipantsStateAndReasonCode (chatId=" + chatId
                    + ") (participants=" + encodedParticipants + ") (state=" + state
                    + ") (reasonCode=" + reasonCode + ")");
        }
        ContentValues values = new ContentValues();
        values.put(GroupChatData.KEY_PARTICIPANTS, encodedParticipants);
        values.put(GroupChatData.KEY_STATE, state.toInt());
        values.put(GroupChatData.KEY_REASON_CODE, reasonCode.toInt());
        return mLocalContentResolver.update(
                Uri.withAppendedPath(GroupChatData.CONTENT_URI, chatId), values, null, null) > 0;
    }

    @Override
    public boolean setGroupChatStateAndReasonCode(String chatId, State state, ReasonCode reasonCode) {
        if (sLogger.isActivated()) {
            sLogger.debug("setGroupChatStateAndReasonCode (chatId=" + chatId + ") (state=" + state
                    + ") (reasonCode=" + reasonCode + ")");
        }
        ContentValues values = new ContentValues();
        values.put(GroupChatData.KEY_STATE, state.toInt());
        values.put(GroupChatData.KEY_REASON_CODE, reasonCode.toInt());
        return mLocalContentResolver.update(
                Uri.withAppendedPath(GroupChatData.CONTENT_URI, chatId), values, null, null) > 0;
    }

    @Override
    public boolean setGroupChatParticipants(String chatId,
            Map<ContactId, Status> participants) {
        String encodedParticipants = generateEncodedParticipantInfos(participants);
        if (sLogger.isActivated()) {
            sLogger.debug("updateGroupChatParticipant (chatId=" + chatId + ") (participants="
                    + encodedParticipants + ")");
        }
        ContentValues values = new ContentValues();
        values.put(GroupChatData.KEY_PARTICIPANTS, encodedParticipants);
        return mLocalContentResolver.update(
                Uri.withAppendedPath(GroupChatData.CONTENT_URI, chatId), values, null, null) > 0;
    }

    @Override
    public boolean setGroupChatRejoinId(String chatId, String rejoinId, boolean updateStateToStarted) {
        if (sLogger.isActivated()) {
            sLogger.debug("Update group chat rejoin ID to ".concat(rejoinId));
        }
        ContentValues values = new ContentValues();
        values.put(GroupChatData.KEY_REJOIN_ID, rejoinId);
        if (updateStateToStarted) {
            values.put(GroupChatData.KEY_STATE, ChatLog.GroupChat.State.STARTED.toInt());
            values.put(GroupChatData.KEY_REASON_CODE, ChatLog.GroupChat.ReasonCode.UNSPECIFIED.toInt());
        }
        return mLocalContentResolver.update(
                Uri.withAppendedPath(GroupChatData.CONTENT_URI, chatId), values, null, null) > 0;
    }

    @Override
    public GroupChatInfo getGroupChatInfo(String chatId) {
        Cursor cursor = null;
        try {
            Uri contentUri = Uri.withAppendedPath(GroupChatData.CONTENT_URI, chatId);
            cursor = mLocalContentResolver.query(contentUri, PROJECTION_GC_INFO, null, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, contentUri);
            if (!cursor.moveToNext()) {
                return null;
            }
            long timestamp = cursor.getLong(cursor
                    .getColumnIndexOrThrow(GroupChatData.KEY_TIMESTAMP));
            String subject = cursor.getString(cursor
                    .getColumnIndexOrThrow(GroupChatData.KEY_SUBJECT));
            String rejoinId = cursor.getString(cursor
                    .getColumnIndexOrThrow(GroupChatData.KEY_REJOIN_ID));
            Uri rejoinUri = rejoinId != null ? Uri.parse(rejoinId) : null;
            Map<ContactId, Status> participants = parseEncodedParticipantInfos(cursor
                    .getString(cursor.getColumnIndexOrThrow(GroupChatData.KEY_PARTICIPANTS)));
            return new GroupChatInfo(rejoinUri, chatId, participants, subject, timestamp);

        } finally {
            CursorUtil.close(cursor);
        }
    }

    @Override
    public Map<ContactId, Status> getParticipants(String chatId) {
        Cursor cursor = getGroupChatData(GroupChatData.KEY_PARTICIPANTS, chatId);
        if (cursor == null) {
            return null;
        }
        return parseEncodedParticipantInfos(getDataAsString(cursor));
    }

    @Override
    public Map<ContactId, Status> getParticipants(String chatId,
                                                  Set<Status> statuses) {
        Map<ContactId, Status> participants = getParticipants(chatId);
        if (participants == null) {
            return null;
        }
        Map<ContactId, Status> matchingParticipants = new HashMap<>();
        for (Map.Entry<ContactId, Status> participant : participants.entrySet()) {
            Status status = participant.getValue();
            if (statuses.contains(status)) {
                matchingParticipants.put(participant.getKey(), status);
            }
        }
        return matchingParticipants;
    }

    @Override
    public boolean isGroupChatNextInviteRejected(String chatId) {
        Cursor cursor = null;
        try {
            Uri contentUri = Uri.withAppendedPath(GroupChatData.CONTENT_URI, chatId);
            cursor = mLocalContentResolver.query(contentUri, PROJECTION_GC_CHAT_ID,
                    SELECT_CHAT_ID_STATUS_REJECTED, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, contentUri);
            return cursor.moveToNext();
        } finally {
            CursorUtil.close(cursor);
        }
    }

    private Cursor getGroupChatData(String columnName, String chatId) {
        String[] projection = new String[] {
            columnName
        };
        Uri contentUri = Uri.withAppendedPath(GroupChatData.CONTENT_URI, chatId);
        Cursor cursor = mLocalContentResolver.query(contentUri, projection, null, null, null);
        CursorUtil.assertCursorIsNotNull(cursor, contentUri);
        if (!cursor.moveToNext()) {
            CursorUtil.close(cursor);
            return null;
        }
        return cursor;
    }

    private String getDataAsString(Cursor cursor) {
        try {
            return cursor.getString(FIRST_COLUMN_IDX);
        } finally {
            CursorUtil.close(cursor);
        }
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

    public State getGroupChatState(String chatId) {
        Cursor cursor = getGroupChatData(GroupChatData.KEY_STATE, chatId);
        if (cursor == null) {
            return null;
        }
        return ChatLog.GroupChat.State.valueOf(getDataAsInteger(cursor));
    }

    public ReasonCode getGroupChatReasonCode(String chatId) {
        Cursor cursor = getGroupChatData(GroupChatData.KEY_REASON_CODE, chatId);
        if (cursor == null) {
            return null;
        }
        return ChatLog.GroupChat.ReasonCode.valueOf(getDataAsInteger(cursor));
    }

    @Override
    public boolean setRejectNextGroupChatNextInvitation(String chatId) {
        if (sLogger.isActivated()) {
            sLogger.debug("setRejectNextGroupChatNextInvitation (chatId=" + chatId + ")");
        }
        ContentValues values = new ContentValues();
        values.put(GroupChatData.KEY_USER_ABORTION, UserAbortion.SERVER_NOT_NOTIFIED.toInt());
        return mLocalContentResolver.update(
                Uri.withAppendedPath(GroupChatData.CONTENT_URI, chatId), values, null, null) > 0;
    }

    @Override
    public Set<String> getChatIdsOfActiveGroupChatsForAutoRejoin() {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(GroupChatData.CONTENT_URI, PROJECTION_GC_CHAT_ID,
                    SELECT_ACTIVE_GROUP_CHATS, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, GroupChatData.CONTENT_URI);
            Set<String> activeGroupChats = new HashSet<>();
            while (cursor.moveToNext()) {
                String chatId = cursor.getString(FIRST_COLUMN_IDX);
                activeGroupChats.add(chatId);
            }
            return activeGroupChats;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    @Override
    public Cursor getGroupChatData(String chatId) {
        Uri contentUri = Uri.withAppendedPath(GroupChatData.CONTENT_URI, chatId);
        Cursor cursor = mLocalContentResolver.query(contentUri, null, null, null, null);
        CursorUtil.assertCursorIsNotNull(cursor, contentUri);
        return cursor;
    }

    @Override
    public boolean isGroupChatPersisted(String chatId) {
        Cursor cursor = null;
        try {
            Uri contentUri = Uri.withAppendedPath(GroupChatData.CONTENT_URI, chatId);
            cursor = mLocalContentResolver.query(contentUri, PROJECTION_GC_CHAT_ID, null, null,
                    null);
            CursorUtil.assertCursorIsNotNull(cursor, contentUri);
            return cursor.moveToNext();
        } finally {
            CursorUtil.close(cursor);
        }
    }

}

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

package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.ReasonCode;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.SparseArray;

import java.util.HashMap;
import java.util.Map;

/**
 * Content provider for chat history
 * 
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 */
public class ChatLog {
    /**
     * Group chat
     */
    public static class GroupChat {
        /**
         * Content provider URI for chat conversations
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://com.gsma.services.rcs.provider.chat/groupchat");

        /**
         * History log member id
         */
        public static final int HISTORYLOG_MEMBER_ID = 0;

        /**
         * The name of the column containing the unique id across provider tables.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String BASECOLUMN_ID = BaseColumns._ID;

        /**
         * The name of the column containing the unique ID of the group chat.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String CHAT_ID = "chat_id";

        /**
         * The name of the column containing the state of the group chat.
         * <P>
         * Type: INTEGER
         * </P>
         * 
         * @see State
         */
        public static final String STATE = "state";

        /**
         * The name of the column containing the reason code of the state of the group chat.
         * <P>
         * Type: INTEGER
         * </P>
         * 
         * @see ReasonCode
         */
        public static final String REASON_CODE = "reason_code";

        /**
         * The name of the column containing the subject of the group chat.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String SUBJECT = "subject";

        /**
         * The name of the column containing the direction of the group chat.
         * <P>
         * Type: INTEGER
         * </P>
         * 
         * @see Direction
         */
        public static final String DIRECTION = "direction";

        /**
         * The name of the column containing the time when group chat is created.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String TIMESTAMP = "timestamp";

        /**
         * The name of the column containing the list of participants and associated status.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String PARTICIPANTS = "participants";

        /**
         * ContactId formatted number of the inviter of the group chat or null if this is a group
         * chat initiated by the local user (ie outgoing group chat).
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String CONTACT = "contact";

        /**
         * Utility method to get participants from its string representation in the ChatLog
         * provider.
         * 
         * @param ctx the context
         * @param participants Participants in string representation
         * @return Participants
         * @throws RcsPermissionDeniedException
         */
        public static Map<ContactId, Participant.Status> getParticipants(Context ctx,
                String participants) throws RcsPermissionDeniedException {
            ContactUtil contactUtils = ContactUtil.getInstance(ctx);
            String[] tokens = participants.split(",");
            Map<ContactId, Participant.Status> participantResult = new HashMap<>();
            for (String participant : tokens) {
                String[] keyValue = participant.split("=");
                if (keyValue.length == 2) {
                    String contact = keyValue[0];
                    Participant.Status status = Participant.Status.valueOf(Integer
                            .parseInt(keyValue[1]));
                    participantResult.put(contactUtils.formatContact(contact), status);
                }
            }
            return participantResult;
        }

        /**
         * Group chat state
         */
        public enum State {

            /**
             * Chat invitation received
             */
            INVITED(0),

            /**
             * Chat invitation sent
             */
            INITIATING(1),

            /**
             * Chat is started
             */
            STARTED(2),

            /**
             * Chat has been aborted
             */
            ABORTED(3),

            /**
             * Chat has failed
             */
            FAILED(4),

            /**
             * Chat has been accepted and is in the process of becoming started.
             */
            ACCEPTING(5),

            /**
             * Chat invitation was rejected.
             */
            REJECTED(6);

            private final int mValue;

            private static SparseArray<State> mValueToEnum = new SparseArray<>();
            static {
                for (State state : State.values()) {
                    mValueToEnum.put(state.toInt(), state);
                }
            }

            State(int value) {
                mValue = value;
            }

            /**
             * Gets integer value associated to State instance
             *
             * @return value
             */
            public final int toInt() {
                return mValue;
            }

            /**
             * Returns a State instance for the specified integer value.
             *
             * @param value the value associated to the state
             * @return instance
             */
            public static State valueOf(int value) {
                State state = mValueToEnum.get(value);
                if (state != null) {
                    return state;
                }
                throw new IllegalArgumentException("No enum const class " + State.class.getName() + ""
                        + value + "!");
            }
        }

        /**
         * Group chat state reason code
         */
        public enum ReasonCode {

            /**
             * No specific reason code specified.
             */
            UNSPECIFIED(0),

            /**
             * Group chat is aborted by local user.
             */
            ABORTED_BY_USER(1),

            /**
             * Group chat is aborted by remote user.
             */
            ABORTED_BY_REMOTE(2),

            /**
             * Group chat is aborted by inactivity.
             */
            ABORTED_BY_INACTIVITY(3),

            /**
             * Group chat is rejected because already taken by the secondary device.
             */
            REJECTED_BY_SECONDARY_DEVICE(4),

            /**
             * Group chat invitation was rejected as it was detected as spam.
             */
            REJECTED_SPAM(5),

            /**
             * Group chat invitation was rejected due to max number of chats open already.
             */
            REJECTED_MAX_CHATS(6),

            /**
             * Group chat invitation was rejected by remote.
             */
            REJECTED_BY_REMOTE(7),

            /**
             * Group chat invitation was rejected by timeout.
             */
            REJECTED_BY_TIMEOUT(8),

            /**
             * Group chat invitation was rejected by system.
             */
            REJECTED_BY_SYSTEM(9),

            /**
             * Group chat initiation failed.
             */
            FAILED_INITIATION(10);

            private final int mValue;

            private static SparseArray<ReasonCode> mValueToEnum = new SparseArray<>();
            static {
                for (ReasonCode reasonCode : ReasonCode.values()) {
                    mValueToEnum.put(reasonCode.toInt(), reasonCode);
                }
            }

            ReasonCode(int value) {
                mValue = value;
            }

            /**
             * Gets integer value associated to ReasonCode instance
             *
             * @return value
             */
            public final int toInt() {
                return mValue;
            }

            /**
             * Returns a ReasonCode instance for the specified integer value.
             *
             * @param value the value associated to the reason code
             * @return instance
             */
            public static ReasonCode valueOf(int value) {
                ReasonCode reasonCode = mValueToEnum.get(value);
                if (reasonCode != null) {
                    return reasonCode;
                }
                throw new IllegalArgumentException("No enum const class " + ReasonCode.class.getName()
                        + "" + value + "!");
            }
        }

        /**
         * Group chat participant info
         */
        public static class Participant {
            /**
             * Content provider URI for Group Participant Info
             */
            public static final Uri CONTENT_URI = Uri
                    .parse("content://com.gsma.services.rcs.provider.chat/participant");

            /**
             * The name of the column containing the unique id across provider tables.
             * <P>
             * Type: INTEGER
             * </P>
             */
            public static final String BASECOLUMN_ID = BaseColumns._ID;

            /**
             * The name of the column containing the unique ID of the group chat.
             * <P>
             * Type: TEXT
             * </P>
             */
            public static final String CHAT_ID = "chat_id";

            /**
             * ContactId formatted number of the participant of the group chat.
             * <P>
             * Type: TEXT
             * </P>
             */
            public static final String CONTACT = "contact";

            /**
             * The name of the column containing the status of the group participant.
             * <P>
             * Type: INTEGER
             * </P>
             */
            public static final String STATUS = "status";

            /**
             * The name of the column containing the role of the group participant.
             * <P>
             * Type: INTEGER
             * </P>
             */
            public static final String ROLE = "role";

            /**
             * The name of the column containing the type of the group participant.
             * <P>
             * Type: INTEGER
             * </P>
             */
            public static final String TYPE = "etype";

            /**
             * The name of the column containing the alias of the group participant.
             * <P>
             * Type: TEXT
             * </P>
             */
            public static final String ALIAS = "alias";

            /**
             * The name of the column containing the portrait of the group participant.
             * <P>
             * Type: TEXT
             * </P>
             */
            public static final String PORTRAIT = "portrait";

            /**
             * Group chat participant status
             */
            public enum Status {
                /**
                 * Invite can not be sent, instead it has been queued
                 */
                INVITE_QUEUED(0),
                /**
                 * Participant is about to be invited
                 */
                INVITING(1),
                /**
                 * Participant is invited
                 */
                INVITED(2),
                /**
                 * Participant is connected
                 */
                CONNECTED(3),
                /**
                 * Participant disconnected
                 */
                DISCONNECTED(4),
                /**
                 * Participant has departed
                 */
                DEPARTED(5),
                /**
                 * Participant status is failed
                 */
                FAILED(6),
                /**
                 * Participant declined invitation
                 */
                DECLINED(7),
                /**
                 * Participant invitation has timed-out
                 */
                TIMEOUT(8),
                /**
                 * Boot can not be sent, instead it has been queued
                 */
                BOOT_QUEUED(9),
                /**
                 * Participant is about to be booted
                 */
                BOOTING(10),
                /**
                 * Participant is booted
                 */
                BOOTED(11);

                private final int mValue;

                private static SparseArray<Status> mValueToEnum = new SparseArray<>();
                static {
                    for (Status status : Status.values()) {
                        mValueToEnum.put(status.toInt(), status);
                    }
                }

                Status(int value) {
                    mValue = value;
                }

                public final int toInt() {
                    return mValue;
                }

                public static Status valueOf(int value) {
                    Status status = mValueToEnum.get(value);
                    if (status != null) {
                        return status;
                    }
                    throw new IllegalArgumentException("No enum const class "
                            + Status.class.getName() + "" + value + "!");
                }
            }

            /**
             * Group chat participant role
             */
            public enum Role {
                CHAIRMAN(0), PARTICIPANT(1);

                private final int mValue;

                private static SparseArray<Role> mValueToEnum = new SparseArray<Role>();
                static {
                    for (Role entry : Role.values()) {
                        mValueToEnum.put(entry.toInt(), entry);
                    }
                }

                Role(int value) {
                    mValue = value;
                }

                /**
                 * @return value
                 */
                public final int toInt() {
                    return mValue;
                }

                /**
                 * @param value
                 * @return Role
                 */
                public final static Role valueOf(int value) {
                    Role entry = mValueToEnum.get(value);
                    if (entry != null) {
                        return entry;
                    }
                    throw new IllegalArgumentException("No enum const class " + Role.class.getName() + "."
                            + value + "!");
                }
            }

            /**
             * Group chat participant type
             */
            public enum Type {
                UNKNOWN(0), GPMANAGE(1);

                private final int mValue;

                private static SparseArray<Type> mValueToEnum = new SparseArray<>();
                static {
                    for (Type entry : Type.values()) {
                        mValueToEnum.put(entry.toInt(), entry);
                    }
                }

                Type(int value) {
                    mValue = value;
                }

                /**
                 * @return value
                 */
                public final int toInt() {
                    return mValue;
                }

                /**
                 * @param value
                 * @return Type
                 */
                public final static Type valueOf(int value) {
                    Type entry = mValueToEnum.get(value);
                    if (entry != null) {
                        return entry;
                    }
                    throw new IllegalArgumentException("No enum const class " + Type.class.getName()
                            + "." + value + "!");
                }
            }

            private Participant() {
            }
        }

        private GroupChat() {
        }
    }

    /**
     * Chat message from a single chat or group chat
     */
    public static class Message {
        /**
         * Content provider URI for chat messages
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://com.gsma.services.rcs.provider.chat/chatmessage");

        /**
         * History log member id
         */
        public static final int HISTORYLOG_MEMBER_ID = 1;

        /**
         * The name of the column containing the unique id across provider tables.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String BASECOLUMN_ID = BaseColumns._ID;

        /**
         * The name of the column containing the chat ID.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String CHAT_ID = "chat_id";

        /**
         * The name of the column containing the message ID.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String MESSAGE_ID = "msg_id";

        /**
         * The name of the column containing the message status.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String STATUS = "status";

        /**
         * The name of the column containing the message status reason code.
         * <P>
         * Type: INTEGER
         * </P>
         * 
         * @see ReasonCode
         */
        public static final String REASON_CODE = "reason_code";

        /**
         * The name of the column containing the message read status.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String READ_STATUS = "read_status";

        /**
         * The name of the column containing the message direction.
         * <P>
         * Type: INTEGER
         * </P>
         * 
         * @see Direction
         */
        public static final String DIRECTION = "direction";

        /**
         * The name of the column containing the MSISDN of the remote contact.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String CONTACT = "contact";

        /**
         * The name of the column containing the message content.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String CONTENT = "content";

        /**
         * The name of the column containing the time when message is created.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String TIMESTAMP = "timestamp";

        /**
         * The name of the column containing the time when message is sent. If 0 means not sent.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String TIMESTAMP_SENT = "timestamp_sent";

        /**
         * The name of the column containing the time when message is delivered. If 0 means not
         * delivered.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String TIMESTAMP_DELIVERED = "timestamp_delivered";

        /**
         * The name of the column containing the time when message is displayed. If 0 means not
         * displayed.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String TIMESTAMP_DISPLAYED = "timestamp_displayed";

        /**
         * If delivery has expired for this message. Values: 1 (true), 0 (false)
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String EXPIRED_DELIVERY = "expired_delivery";

        /**
         * The name of the column containing the MIME-TYPE of the message body.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String MIME_TYPE = "mime_type";


        /**
         * The name of the column containing the conversation ID.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String CONVERSATION_ID = "conversation_id";

        /**
         * The name of the column containing the contribution ID.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String CONTRIBUTION_ID = "contribution_id";

        /**
         * The name of the column containing the list of the MSISDNs of the non-primary
         * ("courtesy copy") contacts.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String COURTESY_COPY = "courtesy_copy";

        /**
         * If this is a silence supported message. Values: 1 (true), 0 (false)
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String SILENCE = "silence";

        /**
         * The name of the column containing the remote device type of the message. Values: 1 (PC)
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String DEVICE_TYPE = "device_type";

        /**
         * If this is a burn-after-reading message. Values: 1 (true), 0 (false)
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String BAR_CYCLE = "bar_cycle";

        /**
         * Message MIME-types
         */
        public static class MimeType {

            /**
             * MIME-type of text messages
             */
            public static final String TEXT_MESSAGE = "text/plain";

            /**
             * MIME-type of geoloc messages
             */
            public static final String GEOLOC_MESSAGE = "application/geoloc";

            /**
             * MIME-type of vemoticon messages
             */
            public static final String VEMOTICON_MESSAGE = "application/vemoticon+xml";

            /**
             * MIME-type of cloudfile messages
             */
            public static final String CLOUDFILE_MESSAGE = "application/cloudfile+xml";

            /**
             * MIME-type of card messages
             */
            public static final String CARD_MESSAGE = "application/card+xml";

            /**
             * MIME-type of cmredbag messages
             */
            public static final String CMREDBAG_MESSAGE = "application/cmredbag+xml";

            /**
             * MIME-type of public account xml messages
             */
            public static final String XML_MESSAGE = "application/xml";

            /**
             * MIME-type of group chat events
             */
            public static final String GROUPCHAT_EVENT = "rcs/groupchat-event";

            private MimeType() {
            }
        }

        public static class Content {
            /**
             * Status of the message
             */
            public enum Status {

                /**
                 * The message has been rejected
                 */
                REJECTED(0),

                /**
                 * The message is queued to be sent by rcs service when possible
                 */
                QUEUED(1),

                /**
                 * The message is in progress of sending
                 */
                SENDING(2),

                /**
                 * The message has been sent
                 */
                SENT(3),

                /**
                 * The message sending has been failed
                 */
                FAILED(4),

                /**
                 * The message has been delivered to the remote.
                 */
                DELIVERED(5),

                /**
                 * The message has been received and a displayed delivery report is requested
                 */
                DISPLAY_REPORT_REQUESTED(6),

                /**
                 * The message is delivered and no display delivery report is requested.
                 */
                RECEIVED(7),

                /**
                 * The message has been displayed
                 */
                DISPLAYED(8),

                /**
                 * The message is queued to be sent by standalone message service
                 */
                SMS_QUEUED(9);

                private final int mValue;

                private static SparseArray<Status> mValueToEnum = new SparseArray<>();
                static {
                    for (Status entry : Status.values()) {
                        mValueToEnum.put(entry.toInt(), entry);
                    }
                }

                Status(int value) {
                    mValue = value;
                }

                public final int toInt() {
                    return mValue;
                }

                public static Status valueOf(int value) {
                    Status entry = mValueToEnum.get(value);
                    if (entry != null) {
                        return entry;
                    }
                    throw new IllegalArgumentException("No enum const class "
                            + Status.class.getName() + "" + value + "!");
                }
            }

            /**
             * Reason code of the message status
             */
            public enum ReasonCode {

                /**
                 * No specific reason code specified.
                 */
                UNSPECIFIED(0),

                /**
                 * Sending of the message failed.
                 */
                FAILED_SEND(1),

                /**
                 * Delivering of the message failed.
                 */
                FAILED_DELIVERY(2),

                /**
                 * Displaying of the message failed.
                 */
                FAILED_DISPLAY(3),

                /**
                 * Incoming one-to-one message was detected as spam.
                 */
                REJECTED_SPAM(4);

                private final int mValue;

                private static SparseArray<ReasonCode> mValueToEnum = new SparseArray<>();
                static {
                    for (ReasonCode entry : ReasonCode.values()) {
                        mValueToEnum.put(entry.toInt(), entry);
                    }
                }

                ReasonCode(int value) {
                    mValue = value;
                }

                public final int toInt() {
                    return mValue;
                }

                public static ReasonCode valueOf(int value) {
                    ReasonCode entry = mValueToEnum.get(value);
                    if (entry != null) {
                        return entry;
                    }
                    throw new IllegalArgumentException("No enum const class "
                            + ReasonCode.class.getName() + "" + value + "!");
                }
            }

            private Content() {
            }
        }

        public static class GroupChatEvent {
            /**
             * Status of group chat event message
             */
            public enum Status {

                /**
                 * JOINED.
                 */
                JOINED(0),

                /**
                 * DEPARTED.
                 */
                DEPARTED(1),

                /**
                 * BOOTED
                 */
                BOOTED(2),

                /**
                 * CHAIRED
                 */
                CHAIRED(3);

                private final int mValue;

                private static SparseArray<Status> mValueToEnum = new SparseArray<>();
                static {
                    for (Status entry : Status.values()) {
                        mValueToEnum.put(entry.toInt(), entry);
                    }
                }

                Status(int value) {
                    mValue = value;
                }

                public final int toInt() {
                    return mValue;
                }

                public static Status valueOf(int value) {
                    Status entry = mValueToEnum.get(value);
                    if (entry != null) {
                        return entry;
                    }
                    throw new IllegalArgumentException("No enum const class "
                            + Status.class.getName() + "" + value + "!");
                }
            }

            private GroupChatEvent() {
            }
        }

        private Message() {
        }
    }

    private ChatLog() {
    }
}

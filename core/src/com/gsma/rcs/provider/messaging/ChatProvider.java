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

import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.history.HistoryMemberBaseIdCreator;
import com.gsma.rcs.service.api.ServerApiPersistentStorageException;
import com.gsma.rcs.utils.DatabaseUtils;
import com.gsma.services.rcs.chat.ChatLog;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Chat provider
 * 
 * @author Jean-Marc AUFFRET
 */
@SuppressWarnings("ConstantConditions")
public class ChatProvider extends ContentProvider {

    private static final int INVALID_ROW_ID = -1;

    private static final String SELECTION_WITH_CHAT_ID_ONLY = GroupChatData.KEY_CHAT_ID
            .concat("=?");

    private static final String SELECTION_WITH_MSG_ID_ONLY = MessageData.KEY_MESSAGE_ID
            .concat("=?");

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(GroupChatData.CONTENT_URI.getAuthority(), GroupChatData.CONTENT_URI
                .getPath().substring(1), UriType.InternalChat.CHAT);
        sUriMatcher.addURI(GroupChatData.CONTENT_URI.getAuthority(), GroupChatData.CONTENT_URI
                .getPath().substring(1).concat("/*"), UriType.InternalChat.CHAT_WITH_ID);
        sUriMatcher.addURI(ChatLog.GroupChat.CONTENT_URI.getAuthority(),
                ChatLog.GroupChat.CONTENT_URI.getPath().substring(1), UriType.Chat.CHAT);
        sUriMatcher.addURI(ChatLog.GroupChat.CONTENT_URI.getAuthority(),
                ChatLog.GroupChat.CONTENT_URI.getPath().substring(1).concat("/*"),
                UriType.Chat.CHAT_WITH_ID);
        sUriMatcher.addURI(MessageData.CONTENT_URI.getAuthority(), MessageData.CONTENT_URI
                .getPath().substring(1), UriType.InternalMessage.MESSAGE);
        sUriMatcher.addURI(MessageData.CONTENT_URI.getAuthority(), MessageData.CONTENT_URI
                .getPath().substring(1).concat("/*"), UriType.InternalMessage.MESSAGE_WITH_ID);
        sUriMatcher.addURI(ChatLog.Message.CONTENT_URI.getAuthority(), ChatLog.Message.CONTENT_URI
                .getPath().substring(1), UriType.Message.MESSAGE);
        sUriMatcher.addURI(ChatLog.Message.CONTENT_URI.getAuthority(), ChatLog.Message.CONTENT_URI
                .getPath().substring(1).concat("/*"), UriType.Message.MESSAGE_WITH_ID);

    }

    /**
     * Messages table name
     */
    public static final String TABLE_MESSAGE = "message";

    /**
     * Group chats table name
     */
    public static final String TABLE_GROUP_CHAT = "groupchat";

    /**
     * Database name
     */
    public static final String DATABASE_NAME = "chat.db";

    /**
     * String to allow projection for exposed group chat URI to a set of columns.
     */
    private static final String[] GROUP_CHAT_COLUMNS_ALLOWED_FOR_EXTERNAL_ACCESS = new String[] {
            GroupChatData.KEY_BASECOLUMN_ID, GroupChatData.KEY_CHAT_ID, GroupChatData.KEY_CONTACT,
            GroupChatData.KEY_STATE, GroupChatData.KEY_SUBJECT, GroupChatData.KEY_DIRECTION,
            GroupChatData.KEY_TIMESTAMP, GroupChatData.KEY_REASON_CODE,
            GroupChatData.KEY_PARTICIPANTS
    };

    private static final Set<String> GROUP_CHAT_COLUMNS_SET_ALLOWED_FOR_EXTERNAL_ACCESS = new HashSet<>(
            Arrays.asList(GROUP_CHAT_COLUMNS_ALLOWED_FOR_EXTERNAL_ACCESS));

    /**
     * String to allow projection for exposed message URI to a set of columns.
     */
    private static final String[] MESSAGE_COLUMNS_ALLOWED_FOR_EXTERNAL_ACCESS = new String[] {
            MessageData.KEY_BASECOLUMN_ID, MessageData.KEY_CHAT_ID, MessageData.KEY_CONTACT,
            MessageData.KEY_CONTENT, MessageData.KEY_DIRECTION, MessageData.KEY_EXPIRED_DELIVERY,
            MessageData.KEY_MESSAGE_ID, MessageData.KEY_MIME_TYPE, MessageData.KEY_READ_STATUS,
            MessageData.KEY_REASON_CODE, MessageData.KEY_STATUS, MessageData.KEY_TIMESTAMP,
            MessageData.KEY_TIMESTAMP_DELIVERED, MessageData.KEY_TIMESTAMP_DISPLAYED,
            MessageData.KEY_TIMESTAMP_SENT, MessageData.KEY_CONVERSATION_ID,
            MessageData.KEY_COURTESY_COPY, MessageData.KEY_DEVICE_TYPE, MessageData.KEY_SILENCE,
            MessageData.KEY_BAR_CYCLE
    };

    private static final Set<String> MESSAGE_COLUMNS_SET_ALLOWED_FOR_EXTERNAL_ACCESS = new HashSet<>(
            Arrays.asList(MESSAGE_COLUMNS_ALLOWED_FOR_EXTERNAL_ACCESS));

    private static final class UriType {

        private static final class Chat {

            private static final int CHAT = 1;

            private static final int CHAT_WITH_ID = 2;
        }

        private static final class Message {

            private static final int MESSAGE = 3;

            private static final int MESSAGE_WITH_ID = 4;
        }

        private static final class InternalChat {

            private static final int CHAT = 5;

            private static final int CHAT_WITH_ID = 6;
        }

        private static final class InternalMessage {

            private static final int MESSAGE = 7;

            private static final int MESSAGE_WITH_ID = 8;
        }

    }

    private static final class CursorType {

        private static final class Chat {

            private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/groupchat";

            private static final String TYPE_ITEM = "vnd.android.cursor.item/groupchat";
        }

        private static final class Message {

            private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/chatmessage";

            private static final String TYPE_ITEM = "vnd.android.cursor.item/chatmessage";
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 17;

        public DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // @formatter:off
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_GROUP_CHAT + '('
                    + GroupChatData.KEY_CHAT_ID + " TEXT NOT NULL PRIMARY KEY,"
                    + GroupChatData.KEY_BASECOLUMN_ID + " INTEGER NOT NULL,"
                    + GroupChatData.KEY_REJOIN_ID + " TEXT,"
                    + GroupChatData.KEY_SUBJECT + " TEXT,"
                    + GroupChatData.KEY_PARTICIPANTS + " TEXT NOT NULL,"
                    + GroupChatData.KEY_STATE + " INTEGER NOT NULL,"
                    + GroupChatData.KEY_REASON_CODE + " INTEGER NOT NULL,"
                    + GroupChatData.KEY_DIRECTION + " INTEGER NOT NULL,"
                    + GroupChatData.KEY_TIMESTAMP + " INTEGER NOT NULL,"
                    + GroupChatData.KEY_USER_ABORTION + " INTEGER NOT NULL,"
                    + GroupChatData.KEY_CONTACT + " TEXT)");
            // @formatter:on
            db.execSQL("CREATE INDEX " + TABLE_GROUP_CHAT + '_' + GroupChatData.KEY_BASECOLUMN_ID
                    + "_idx" + " ON " + TABLE_GROUP_CHAT + '(' + GroupChatData.KEY_BASECOLUMN_ID
                    + ')');
            db.execSQL("CREATE INDEX " + TABLE_GROUP_CHAT + '_' + GroupChatData.KEY_TIMESTAMP
                    + "_idx" + " ON " + TABLE_GROUP_CHAT + '(' + GroupChatData.KEY_TIMESTAMP + ')');
            // @formatter:off
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MESSAGE + '('
                    + MessageData.KEY_BASECOLUMN_ID + " INTEGER NOT NULL,"
                    + MessageData.KEY_CHAT_ID + " TEXT NOT NULL,"
                    + MessageData.KEY_CONVERSATION_ID + " TEXT,"
                    + MessageData.KEY_CONTACT + " TEXT,"
                    + MessageData.KEY_COURTESY_COPY + " TEXT,"
                    + MessageData.KEY_MESSAGE_ID + " TEXT NOT NULL PRIMARY KEY,"
                    + MessageData.KEY_CONTENT + " TEXT,"
                    + MessageData.KEY_MIME_TYPE + " TEXT NOT NULL,"
                    + MessageData.KEY_DIRECTION + " INTEGER NOT NULL,"
                    + MessageData.KEY_STATUS + " INTEGER NOT NULL,"
                    + MessageData.KEY_REASON_CODE + " INTEGER NOT NULL,"
                    + MessageData.KEY_READ_STATUS + " INTEGER NOT NULL,"
                    + MessageData.KEY_TIMESTAMP + " INTEGER NOT NULL,"
                    + MessageData.KEY_TIMESTAMP_SENT + " INTEGER NOT NULL,"
                    + MessageData.KEY_TIMESTAMP_DELIVERED + " INTEGER NOT NULL,"
                    + MessageData.KEY_TIMESTAMP_DISPLAYED + " INTEGER NOT NULL,"
                    + MessageData.KEY_DELIVERY_EXPIRATION + " INTEGER NOT NULL,"
                    + MessageData.KEY_EXPIRED_DELIVERY + " INTEGER NOT NULL,"
                    + MessageData.KEY_DEVICE_TYPE + " INTEGER,"
                    + MessageData.KEY_SILENCE + " INTEGER,"
                    + MessageData.KEY_BAR_CYCLE + " INTEGER)");
            // @formatter:on
            db.execSQL("CREATE INDEX " + TABLE_MESSAGE + '_' + MessageData.KEY_BASECOLUMN_ID
                    + "_idx" + " ON " + TABLE_MESSAGE + '(' + MessageData.KEY_BASECOLUMN_ID + ')');
            db.execSQL("CREATE INDEX " + TABLE_MESSAGE + '_' + MessageData.KEY_CHAT_ID + "_idx"
                    + " ON " + TABLE_MESSAGE + '(' + MessageData.KEY_CHAT_ID + ')');
            db.execSQL("CREATE INDEX " + MessageData.KEY_TIMESTAMP + "_idx" + " ON "
                    + TABLE_MESSAGE + '(' + MessageData.KEY_TIMESTAMP + ')');
            db.execSQL("CREATE INDEX " + MessageData.KEY_TIMESTAMP_SENT + "_idx" + " ON "
                    + TABLE_MESSAGE + '(' + MessageData.KEY_TIMESTAMP_SENT + ')');
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
            db.execSQL("DROP TABLE IF EXISTS ".concat(TABLE_GROUP_CHAT));
            db.execSQL("DROP TABLE IF EXISTS ".concat(TABLE_MESSAGE));
            onCreate(db);
        }
    }

    private SQLiteOpenHelper mOpenHelper;

    private String getSelectionWithChatId(String selection) {
        if (TextUtils.isEmpty(selection)) {
            return SELECTION_WITH_CHAT_ID_ONLY;
        }
        return "(" + SELECTION_WITH_CHAT_ID_ONLY + ") AND (" + selection + ')';
    }

    private String[] getSelectionArgsWithChatId(String[] selectionArgs, String chatId) {
        return DatabaseUtils.appendIdWithSelectionArgs(chatId, selectionArgs);
    }

    private String getSelectionWithMessageId(String selection) {
        if (TextUtils.isEmpty(selection)) {
            return SELECTION_WITH_MSG_ID_ONLY;
        }
        return "(" + SELECTION_WITH_MSG_ID_ONLY + ") AND (" + selection + ')';
    }

    private String[] getSelectionArgsWithMessageId(String[] selectionArgs, String messageId) {
        return DatabaseUtils.appendIdWithSelectionArgs(messageId, selectionArgs);
    }

    private String[] restrictGroupChatProjectionToExternallyDefinedColumns(String[] projection)
            throws UnsupportedOperationException {
        if (projection == null || projection.length == 0) {
            return GROUP_CHAT_COLUMNS_ALLOWED_FOR_EXTERNAL_ACCESS;
        }
        for (String projectedColumn : projection) {
            if (!GROUP_CHAT_COLUMNS_SET_ALLOWED_FOR_EXTERNAL_ACCESS.contains(projectedColumn)) {
                throw new UnsupportedOperationException("No visibility to the accessed column "
                        + projectedColumn + "!");
            }
        }
        return projection;
    }

    private String[] restrictMessageProjectionToExternallyDefinedColumns(String[] projection)
            throws UnsupportedOperationException {
        if (projection == null || projection.length == 0) {
            return MESSAGE_COLUMNS_ALLOWED_FOR_EXTERNAL_ACCESS;
        }
        for (String projectedColumn : projection) {
            if (!MESSAGE_COLUMNS_SET_ALLOWED_FOR_EXTERNAL_ACCESS.contains(projectedColumn)) {
                throw new UnsupportedOperationException("No visibility to the accessed column "
                        + projectedColumn + "!");
            }
        }
        return projection;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalChat.CHAT:
                /* Intentional fall through */
            case UriType.Chat.CHAT:
                return CursorType.Chat.TYPE_DIRECTORY;

            case UriType.InternalChat.CHAT_WITH_ID:
                /* Intentional fall through */
            case UriType.Chat.CHAT_WITH_ID:
                return CursorType.Chat.TYPE_ITEM;

            case UriType.InternalMessage.MESSAGE:
                /* Intentional fall through */
            case UriType.Message.MESSAGE:
                return CursorType.Message.TYPE_DIRECTORY;

            case UriType.InternalMessage.MESSAGE_WITH_ID:
                /* Intentional fall through */
            case UriType.Message.MESSAGE_WITH_ID:
                return CursorType.Message.TYPE_ITEM;

            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sort) {
        Cursor cursor = null;
        try {
            switch (sUriMatcher.match(uri)) {
                case UriType.InternalChat.CHAT_WITH_ID:
                    String chatId = uri.getLastPathSegment();
                    selection = getSelectionWithChatId(selection);
                    selectionArgs = getSelectionArgsWithChatId(selectionArgs, chatId);
                    SQLiteDatabase db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(TABLE_GROUP_CHAT, projection, selection, selectionArgs, null,
                            null, sort);
                    CursorUtil.assertCursorIsNotNull(cursor, uri);
                    cursor.setNotificationUri(getContext().getContentResolver(),
                            Uri.withAppendedPath(ChatLog.GroupChat.CONTENT_URI, chatId));
                    return cursor;

                case UriType.InternalChat.CHAT:
                    db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(TABLE_GROUP_CHAT, projection, selection, selectionArgs, null,
                            null, sort);
                    CursorUtil.assertCursorIsNotNull(cursor, uri);
                    cursor.setNotificationUri(getContext().getContentResolver(),
                            ChatLog.GroupChat.CONTENT_URI);
                    return cursor;

                case UriType.Chat.CHAT_WITH_ID:
                    chatId = uri.getLastPathSegment();
                    selection = getSelectionWithChatId(selection);
                    selectionArgs = getSelectionArgsWithChatId(selectionArgs, chatId);
                    /* Intentional fall through */
                    //$FALL-THROUGH$
                case UriType.Chat.CHAT:
                    db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(TABLE_GROUP_CHAT,
                            restrictGroupChatProjectionToExternallyDefinedColumns(projection),
                            selection, selectionArgs, null, null, sort);
                    CursorUtil.assertCursorIsNotNull(cursor, uri);
                    cursor.setNotificationUri(getContext().getContentResolver(), uri);
                    return cursor;

                case UriType.InternalMessage.MESSAGE_WITH_ID:
                    String msgId = uri.getLastPathSegment();
                    selection = getSelectionWithMessageId(selection);
                    selectionArgs = getSelectionArgsWithMessageId(selectionArgs, msgId);
                    db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(TABLE_MESSAGE, projection, selection, selectionArgs, null,
                            null, sort);
                    CursorUtil.assertCursorIsNotNull(cursor, uri);
                    cursor.setNotificationUri(getContext().getContentResolver(),
                            Uri.withAppendedPath(ChatLog.Message.CONTENT_URI, msgId));
                    return cursor;

                case UriType.InternalMessage.MESSAGE:
                    /* Intentional fall through */
                    db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(TABLE_MESSAGE, projection, selection, selectionArgs, null,
                            null, sort);
                    CursorUtil.assertCursorIsNotNull(cursor, uri);
                    cursor.setNotificationUri(getContext().getContentResolver(),
                            ChatLog.Message.CONTENT_URI);
                    return cursor;

                case UriType.Message.MESSAGE_WITH_ID:
                    msgId = uri.getLastPathSegment();
                    selection = getSelectionWithMessageId(selection);
                    selectionArgs = getSelectionArgsWithMessageId(selectionArgs, msgId);
                    /* Intentional fall through */
                    //$FALL-THROUGH$
                case UriType.Message.MESSAGE:
                    db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(TABLE_MESSAGE,
                            restrictMessageProjectionToExternallyDefinedColumns(projection),
                            selection, selectionArgs, null, null, sort);
                    CursorUtil.assertCursorIsNotNull(cursor, uri);
                    cursor.setNotificationUri(getContext().getContentResolver(), uri);
                    return cursor;

                default:
                    throw new IllegalArgumentException("Unsupported URI " + uri + "!");
            }

        } /*
           * TODO: Do not catch, close cursor, and then throw same exception. Callers should handle
           * exception.
           */
        catch (RuntimeException e) {
            if (cursor != null) {
                cursor.close();
            }
            throw e;
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalChat.CHAT_WITH_ID:
                String chatId = uri.getLastPathSegment();
                selection = getSelectionWithChatId(selection);
                selectionArgs = getSelectionArgsWithChatId(selectionArgs, chatId);
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int count = db.update(TABLE_GROUP_CHAT, values, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(
                            Uri.withAppendedPath(ChatLog.GroupChat.CONTENT_URI, chatId), null);
                }
                return count;

            case UriType.InternalChat.CHAT:
                db = mOpenHelper.getWritableDatabase();
                count = db.update(TABLE_GROUP_CHAT, values, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(ChatLog.GroupChat.CONTENT_URI,
                            null);
                }
                return count;

            case UriType.InternalMessage.MESSAGE_WITH_ID:
                String msgId = uri.getLastPathSegment();
                selection = getSelectionWithMessageId(selection);
                selectionArgs = getSelectionArgsWithMessageId(selectionArgs, msgId);
                db = mOpenHelper.getWritableDatabase();
                count = db.update(TABLE_MESSAGE, values, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(
                            Uri.withAppendedPath(ChatLog.Message.CONTENT_URI, msgId), null);
                }
                return count;

            case UriType.InternalMessage.MESSAGE:
                db = mOpenHelper.getWritableDatabase();
                count = db.update(TABLE_MESSAGE, values, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(ChatLog.Message.CONTENT_URI,
                            null);
                }
                return count;

            case UriType.Chat.CHAT_WITH_ID:
                /* Intentional fall through */
            case UriType.Chat.CHAT:
                /* Intentional fall through */
            case UriType.Message.MESSAGE_WITH_ID:
                /* Intentional fall through */
            case UriType.Message.MESSAGE:
                throw new UnsupportedOperationException("This provider (URI=" + uri
                        + ") supports read only access!");

            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues initialValues) {
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalChat.CHAT:
                /* Intentional fall through */
            case UriType.InternalChat.CHAT_WITH_ID:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                String chatId = initialValues.getAsString(GroupChatData.KEY_CHAT_ID);
                initialValues.put(GroupChatData.KEY_BASECOLUMN_ID, HistoryMemberBaseIdCreator
                        .createUniqueId(getContext(), ChatLog.GroupChat.HISTORYLOG_MEMBER_ID));

                if (db.insert(TABLE_GROUP_CHAT, null, initialValues) == INVALID_ROW_ID) {
                    throw new ServerApiPersistentStorageException("Unable to insert row for URI "
                            + uri + '!');
                }
                Uri notificationUri = Uri.withAppendedPath(ChatLog.GroupChat.CONTENT_URI, chatId);
                getContext().getContentResolver().notifyChange(notificationUri, null);
                return notificationUri;

            case UriType.InternalMessage.MESSAGE:
                /* Intentional fall through */
            case UriType.InternalMessage.MESSAGE_WITH_ID:
                db = mOpenHelper.getWritableDatabase();
                String messageId = initialValues.getAsString(MessageData.KEY_MESSAGE_ID);
                initialValues.put(MessageData.KEY_BASECOLUMN_ID, HistoryMemberBaseIdCreator
                        .createUniqueId(getContext(), MessageData.HISTORYLOG_MEMBER_ID));

                if (db.insert(TABLE_MESSAGE, null, initialValues) == INVALID_ROW_ID) {
                    throw new ServerApiPersistentStorageException("Unable to insert row for URI "
                            + uri + '!');
                }
                notificationUri = Uri.withAppendedPath(ChatLog.Message.CONTENT_URI, messageId);
                getContext().getContentResolver().notifyChange(notificationUri, null);
                return notificationUri;

            case UriType.Chat.CHAT:
                /* Intentional fall through */
            case UriType.Chat.CHAT_WITH_ID:
                /* Intentional fall through */
            case UriType.Message.MESSAGE:
                /* Intentional fall through */
            case UriType.Message.MESSAGE_WITH_ID:
                throw new UnsupportedOperationException("This provider (URI=" + uri
                        + ") supports read only access!");

            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalChat.CHAT_WITH_ID:
                String chatId = uri.getLastPathSegment();
                selection = getSelectionWithChatId(selection);
                selectionArgs = getSelectionArgsWithChatId(selectionArgs, chatId);
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int count = db.delete(TABLE_GROUP_CHAT, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(
                            Uri.withAppendedPath(ChatLog.GroupChat.CONTENT_URI, chatId), null);
                }
                return count;

            case UriType.InternalChat.CHAT:
                db = mOpenHelper.getWritableDatabase();
                count = db.delete(TABLE_GROUP_CHAT, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(ChatLog.GroupChat.CONTENT_URI,
                            null);
                }
                return count;

            case UriType.InternalMessage.MESSAGE_WITH_ID:
                String msgId = uri.getLastPathSegment();
                selection = getSelectionWithMessageId(selection);
                selectionArgs = getSelectionArgsWithMessageId(selectionArgs, msgId);
                db = mOpenHelper.getWritableDatabase();
                count = db.delete(TABLE_MESSAGE, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(
                            Uri.withAppendedPath(ChatLog.Message.CONTENT_URI, msgId), null);
                }
                return count;

            case UriType.InternalMessage.MESSAGE:
                db = mOpenHelper.getWritableDatabase();
                count = db.delete(TABLE_MESSAGE, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(ChatLog.Message.CONTENT_URI,
                            null);
                }
                return count;

            case UriType.Chat.CHAT_WITH_ID:
                /* Intentional fall through */
            case UriType.Chat.CHAT:
                /* Intentional fall through */
            case UriType.Message.MESSAGE_WITH_ID:
                /* Intentional fall through */
            case UriType.Message.MESSAGE:
                throw new UnsupportedOperationException("This provider (URI=" + uri
                        + ") supports read only access!");

            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

}

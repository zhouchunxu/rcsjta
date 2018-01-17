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

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.content.ContentManager;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpThumbnail;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.fthttp.FtHttpResume;
import com.gsma.rcs.provider.fthttp.FtHttpResumeDownload;
import com.gsma.rcs.provider.fthttp.FtHttpResumeUpload;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.FileUtils;
import com.gsma.rcs.utils.MimeManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransfer.Disposition;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Class to interface the 'filetransfer' table
 */
public class FileTransferLog implements IFileTransferLog {
    private static final String SELECTION_FILE_BY_T_ID = FileTransferData.KEY_UPLOAD_TID + "=?";

    private static final String SELECTION_BY_PAUSED_BY_SYSTEM = FileTransferData.KEY_STATE + "="
            + State.PAUSED.toInt() + " AND " + FileTransferData.KEY_REASON_CODE + "="
            + ReasonCode.PAUSED_BY_SYSTEM.toInt();

    private static final String SELECTION_BY_EQUAL_CHAT_ID_AND_CONTACT = FileTransferData.KEY_CHAT_ID
            + "=" + FileTransferData.KEY_CONTACT;

    private static final String SELECTION_BY_QUEUED_AND_UPLOADED_BUT_NOT_TRANSFERRED_FILE_TRANSFERS = FileTransferData.KEY_STATE
            + "="
            + State.QUEUED.toInt()
            + " OR ("
            + FileTransferData.KEY_STATE
            + "="
            + State.STARTED.toInt()
            + " AND "
            + FileTransferData.KEY_DIRECTION
            + "="
            + Direction.OUTGOING.toInt()
            + " AND "
            + FileTransferData.KEY_DOWNLOAD_URI
            + " IS NOT NULL)";

    private static final String SELECTION_BY_INTERRUPTED_FILE_TRANSFERS = FileTransferData.KEY_STATE
            + " IN ('"
            + State.STARTED.toInt()
            + "','"
            + State.INVITED.toInt()
            + "','"
            + State.ACCEPTING.toInt() + "','" + State.INITIATING.toInt() + "')";

    private static final String SELECTION_BY_NOT_DISPLAYED = FileTransferData.KEY_STATE + "<>"
            + State.DISPLAYED.toInt();

    private static final int FILE_TRANSFER_DELIVERY_EXPIRED = 1;

    private static final int FILE_TRANSFER_DELIVERY_EXPIRATION_NOT_APPLICABLE = 0;

    private static final String SELECTION_BY_UNDELIVERED_ONETOONE_FILE_TRANSFERS = FileTransferData.KEY_EXPIRED_DELIVERY
            + "<>"
            + FILE_TRANSFER_DELIVERY_EXPIRED
            + " AND "
            + FileTransferData.KEY_DELIVERY_EXPIRATION
            + "<>"
            + FILE_TRANSFER_DELIVERY_EXPIRATION_NOT_APPLICABLE
            + " AND "
            + FileTransferData.KEY_STATE
            + " NOT IN("
            + State.DELIVERED.toInt()
            + ","
            + State.DISPLAYED.toInt() + ")";

    private static final String SELECTION_BY_UNDELIVERED_STATUS = FileTransferData.KEY_STATE
            + " NOT IN(" + State.DELIVERED.toInt() + "," + State.DISPLAYED.toInt() + ")";

    private static final String ORDER_BY_TIMESTAMP_ASC = FileTransferData.KEY_TIMESTAMP
            .concat(" ASC");

    private final static String[] PROJECTION_FILE_TRANSFER_ID = new String[] {
        FileTransferData.KEY_FT_ID
    };

    private static final String SELECTION_BY_NOT_READ = FileTransferData.KEY_READ_STATUS + "="
            + ReadStatus.UNREAD.toInt();

    private static final int FIRST_COLUMN_IDX = 0;

    private final LocalContentResolver mLocalContentResolver;

    private final GroupDeliveryInfoLog mGroupChatDeliveryInfoLog;

    private final RcsSettings mRcsSettings;

    private static final Logger sLogger = Logger.getLogger(FileTransferLog.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param localContentResolver Local content resolver
     * @param groupChatDeliveryInfoLog Group chat delivery info log
     * @param rcsSettings RcsSettings
     */
    /* package private */FileTransferLog(LocalContentResolver localContentResolver,
            GroupDeliveryInfoLog groupChatDeliveryInfoLog, RcsSettings rcsSettings) {
        mLocalContentResolver = localContentResolver;
        mGroupChatDeliveryInfoLog = groupChatDeliveryInfoLog;
        mRcsSettings = rcsSettings;
    }

    @Override
    public void addOneToOneFileTransfer(String fileTransferId, String chatId, ContactId contact,
            Direction direction, MmContent content, MmContent fileIcon, State state,
            ReasonCode reasonCode, long timestamp, long timestampSent, long fileExpiration,
            long fileIconExpiration) {
        if (sLogger.isActivated()) {
            sLogger.debug("Add file transfer entry Id=" + fileTransferId + ", contact=" + contact
                    + ", filename=" + content.getName() + ", size=" + content.getSize() + ", MIME="
                    + content.getEncoding() + ", state=" + state + ", reasonCode=" + reasonCode
                    + ", timestamp=" + timestamp + ", timestampSent=" + timestampSent);
        }
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_FT_ID, fileTransferId);
        values.put(FileTransferData.KEY_CHAT_ID, contact.toString());
        values.put(FileTransferData.KEY_CONTACT, contact.toString());
        values.put(FileTransferData.KEY_FILE, content.getUri().toString());
        values.put(FileTransferData.KEY_FILENAME, content.getName());
        values.put(FileTransferData.KEY_MIME_TYPE, content.getEncoding());
        values.put(FileTransferData.KEY_DIRECTION, direction.toInt());
        if (content.isPlayable()) {
            values.put(FileTransferData.KEY_DISPOSITION, FileTransfer.Disposition.RENDER.toInt());
        } else {
            values.put(FileTransferData.KEY_DISPOSITION, FileTransfer.Disposition.ATTACH.toInt());
        }
        values.put(FileTransferData.KEY_TRANSFERRED, 0);
        values.put(FileTransferData.KEY_FILESIZE, content.getSize());
        if (fileIcon != null) {
            values.put(FileTransferData.KEY_FILEICON, fileIcon.getUri().toString());
            values.put(FileTransferData.KEY_FILEICON_MIME_TYPE, fileIcon.getEncoding());
            values.put(FileTransferData.KEY_FILEICON_EXPIRATION, fileIconExpiration);
        } else {
            values.put(FileTransferData.KEY_FILEICON_EXPIRATION,
                    FileTransferData.UNKNOWN_EXPIRATION);
        }
        values.put(FileTransferData.KEY_READ_STATUS, ReadStatus.UNREAD.toInt());
        values.put(FileTransferData.KEY_STATE, state.toInt());
        values.put(FileTransferData.KEY_REASON_CODE, reasonCode.toInt());
        values.put(FileTransferData.KEY_TIMESTAMP, timestamp);
        values.put(FileTransferData.KEY_TIMESTAMP_SENT, timestampSent);
        values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, 0);
        values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, 0);
        values.put(FileTransferData.KEY_DELIVERY_EXPIRATION, 0);
        values.put(FileTransferData.KEY_EXPIRED_DELIVERY, 0);
        values.put(FileTransferData.KEY_FILE_EXPIRATION, fileExpiration);
        mLocalContentResolver.insert(FileTransferData.CONTENT_URI, values);
    }

    @Override
    public void addOutgoingOneToManyFileTransfer(String fileTransferId, String chatId,
            Set<ContactId> recipients, MmContent content, MmContent fileIcon, State state,
            ReasonCode reasonCode, long timestamp, long timestampSent, long fileExpiration,
            long fileIconExpiration) {
        if (sLogger.isActivated()) {
            sLogger.debug("addOutgoingOneToManyFileTransfer: Id=" + fileTransferId + ", chatId="
                    + chatId + " filename=" + content.getName() + ", size=" + content.getSize()
                    + ", MIME=" + content.getEncoding() + ", state=" + state + ", reasonCode="
                    + reasonCode + ", timestamp=" + timestamp + ", timestampSent=" + timestampSent);
        }
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_FT_ID, fileTransferId);
        values.put(FileTransferData.KEY_CHAT_ID, chatId);
        values.put(FileTransferData.KEY_CONVERSATION_ID, chatId);
        values.put(FileTransferData.KEY_CONTACT, "");//TODO
        values.put(FileTransferData.KEY_FILE, Uri.decode(content.getUri().toString()));
        values.put(FileTransferData.KEY_FILENAME, content.getName());
        values.put(FileTransferData.KEY_MIME_TYPE, content.getEncoding());
        values.put(FileTransferData.KEY_DIRECTION, Direction.OUTGOING.toInt());
        values.put(FileTransferData.KEY_TRANSFERRED, 0);
        values.put(FileTransferData.KEY_FILESIZE, content.getSize());
        values.put(FileTransferData.KEY_READ_STATUS, ReadStatus.UNREAD.toInt());
        values.put(FileTransferData.KEY_DELIVERY_MESSAGE_ID, "");//TODO
        values.put(FileTransferData.KEY_TIMESTAMP, timestamp);
        values.put(FileTransferData.KEY_TIMESTAMP_SENT, timestampSent);
        values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, 0);
        values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, 0);
        values.put(FileTransferData.KEY_DELIVERY_EXPIRATION, 0);
        values.put(FileTransferData.KEY_EXPIRED_DELIVERY, 0);
        values.put(FileTransferData.KEY_STATE, state.toInt());
        values.put(FileTransferData.KEY_REASON_CODE, reasonCode.toInt());
//        if (thumbnail != null) {
//            values.put(FileTransferData.KEY_FILEICON, thumbnail.getUri().toString());
//            values.put(FileTransferData.KEY_FILEICON_MIME_TYPE, thumbnail.getEncoding());
//        }
        values.put(FileTransferData.KEY_FILEICON_EXPIRATION, FileTransferData.UNKNOWN_EXPIRATION);
        values.put(FileTransferData.KEY_FILE_EXPIRATION, FileTransferData.UNKNOWN_EXPIRATION);
        if (content.isPlayable()) {
            values.put(FileTransferData.KEY_DISPOSITION, FileTransfer.Disposition.RENDER.toInt());
        } else {
            values.put(FileTransferData.KEY_DISPOSITION, FileTransfer.Disposition.ATTACH.toInt());
        }
        values.put(FileTransferData.KEY_DURATION, content.getTimelen());
        values.put(FileTransferData.KEY_SILENCE, 0);
        mLocalContentResolver.insert(FileTransferData.CONTENT_URI, values);

        // TODO: 2016/11/30 tmply use group chat delivery info log interface
        try {
            for (ContactId contact : recipients) {
                /* Add entry with delivered and displayed timestamps set to 0. */
                mGroupChatDeliveryInfoLog.addGroupChatDeliveryInfoEntry(chatId, contact,
                        fileTransferId, GroupDeliveryInfo.Status.NOT_DELIVERED,
                        GroupDeliveryInfo.ReasonCode.UNSPECIFIED, 0, 0);
            }
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("One to many file transfer with fileTransferId '" + fileTransferId
                        + "' could not be added to database!", e);
            }
            mLocalContentResolver.delete(
                    Uri.withAppendedPath(FileTransferData.CONTENT_URI, fileTransferId), null, null);
            mLocalContentResolver.delete(
                    Uri.withAppendedPath(GroupDeliveryInfoData.CONTENT_URI, fileTransferId), null,
                    null);
            /* TODO: Throw exception */
        }
    }

    @Override
    public void addOutgoingGroupFileTransfer(String fileTransferId, String chatId,
            MmContent content, MmContent thumbnail, Set<ContactId> recipients, State state,
            ReasonCode reasonCode, long timestamp, long timestampSent) {
        if (sLogger.isActivated()) {
            sLogger.debug("addOutgoingGroupFileTransfer: Id=" + fileTransferId + ", chatId="
                    + chatId + " filename=" + content.getName() + ", size=" + content.getSize()
                    + ", MIME=" + content.getEncoding());
        }
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_FT_ID, fileTransferId);
        values.put(FileTransferData.KEY_CHAT_ID, chatId);
        values.put(FileTransferData.KEY_FILE, content.getUri().toString());
        values.put(FileTransferData.KEY_FILENAME, content.getName());
        values.put(FileTransferData.KEY_MIME_TYPE, content.getEncoding());
        values.put(FileTransferData.KEY_DIRECTION, Direction.OUTGOING.toInt());
        values.put(FileTransferData.KEY_TRANSFERRED, 0);
        values.put(FileTransferData.KEY_FILESIZE, content.getSize());
        values.put(MessageData.KEY_READ_STATUS, ReadStatus.UNREAD.toInt());
        values.put(FileTransferData.KEY_TIMESTAMP, timestamp);
        values.put(FileTransferData.KEY_TIMESTAMP_SENT, timestampSent);
        values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, 0);
        values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, 0);
        values.put(FileTransferData.KEY_DELIVERY_EXPIRATION, 0);
        values.put(FileTransferData.KEY_EXPIRED_DELIVERY, 0);
        values.put(FileTransferData.KEY_STATE, state.toInt());
        values.put(FileTransferData.KEY_REASON_CODE, reasonCode.toInt());
        if (thumbnail != null) {
            values.put(FileTransferData.KEY_FILEICON, thumbnail.getUri().toString());
            values.put(FileTransferData.KEY_FILEICON_MIME_TYPE, thumbnail.getEncoding());
        }
        values.put(FileTransferData.KEY_FILEICON_EXPIRATION, FileTransferData.UNKNOWN_EXPIRATION);
        values.put(FileTransferData.KEY_FILE_EXPIRATION, FileTransferData.UNKNOWN_EXPIRATION);
        if (content.isPlayable()) {
            values.put(FileTransferData.KEY_DISPOSITION, FileTransfer.Disposition.RENDER.toInt());
        } else {
            values.put(FileTransferData.KEY_DISPOSITION, FileTransfer.Disposition.ATTACH.toInt());
        }
        mLocalContentResolver.insert(FileTransferData.CONTENT_URI, values);

        try {
            for (ContactId contact : recipients) {
                /* Add entry with delivered and displayed timestamps set to 0. */
                mGroupChatDeliveryInfoLog.addGroupChatDeliveryInfoEntry(chatId, contact,
                        fileTransferId, GroupDeliveryInfo.Status.NOT_DELIVERED,
                        GroupDeliveryInfo.ReasonCode.UNSPECIFIED, 0, 0);
            }
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("Group file transfer with fileTransferId '" + fileTransferId
                        + "' could not be added to database!", e);
            }
            mLocalContentResolver.delete(
                    Uri.withAppendedPath(FileTransferData.CONTENT_URI, fileTransferId), null, null);
            mLocalContentResolver.delete(
                    Uri.withAppendedPath(GroupDeliveryInfoData.CONTENT_URI, fileTransferId), null,
                    null);
            /* TODO: Throw exception */
        }
    }

    @Override
    public void addIncomingGroupFileTransfer(String fileTransferId, String chatId,
            ContactId contact, MmContent content, MmContent fileIcon, State state,
            ReasonCode reasonCode, long timestamp, long timestampSent, long fileExpiration,
            long fileIconExpiration) {
        if (sLogger.isActivated()) {
            sLogger.debug("Add incoming file transfer entry: fileTransferId=" + fileTransferId
                    + ", chatId=" + chatId + ", contact=" + contact + ", filename="
                    + content.getName() + ", size=" + content.getSize() + ", MIME="
                    + content.getEncoding() + ", state=" + state + ", reasonCode=" + reasonCode
                    + ", timestamp=" + timestamp + ", timestampSent=" + timestampSent
                    + ", expiration=" + fileExpiration);
        }
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_FT_ID, fileTransferId);
        values.put(FileTransferData.KEY_CHAT_ID, chatId);
        values.put(FileTransferData.KEY_FILE, content.getUri().toString());
        values.put(FileTransferData.KEY_CONTACT, contact.toString());
        values.put(FileTransferData.KEY_FILENAME, content.getName());
        values.put(FileTransferData.KEY_MIME_TYPE, content.getEncoding());
        values.put(FileTransferData.KEY_DIRECTION, Direction.INCOMING.toInt());
        values.put(FileTransferData.KEY_TRANSFERRED, 0);
        values.put(FileTransferData.KEY_FILESIZE, content.getSize());
        values.put(FileTransferData.KEY_READ_STATUS, ReadStatus.UNREAD.toInt());
        values.put(FileTransferData.KEY_STATE, state.toInt());
        values.put(FileTransferData.KEY_REASON_CODE, reasonCode.toInt());
        values.put(FileTransferData.KEY_TIMESTAMP, timestamp);
        values.put(FileTransferData.KEY_TIMESTAMP_SENT, timestampSent);
        values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, 0);
        values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, 0);
        values.put(FileTransferData.KEY_DELIVERY_EXPIRATION, 0);
        values.put(FileTransferData.KEY_EXPIRED_DELIVERY, 0);
        if (fileIcon != null) {
            values.put(FileTransferData.KEY_FILEICON, fileIcon.getUri().toString());
            values.put(FileTransferData.KEY_FILEICON_MIME_TYPE, fileIcon.getEncoding());
            values.put(FileTransferData.KEY_FILEICON_EXPIRATION, fileIconExpiration);
        } else {
            values.put(FileTransferData.KEY_FILEICON_EXPIRATION,
                    FileTransferData.UNKNOWN_EXPIRATION);
        }
        values.put(FileTransferData.KEY_FILE_EXPIRATION, fileExpiration);
        if (content.isPlayable()) {
            values.put(FileTransferData.KEY_DISPOSITION, FileTransfer.Disposition.RENDER.toInt());
        } else {
            values.put(FileTransferData.KEY_DISPOSITION, FileTransfer.Disposition.ATTACH.toInt());
        }
        mLocalContentResolver.insert(FileTransferData.CONTENT_URI, values);
    }

    /**
     * Set file transfer state and reason code. Note that this method should not be used for
     * State.DELIVERED and State.DISPLAYED. These states require timestamps and should be set
     * through setFileTransferDelivered and setFileTransferDisplayed respectively.
     * 
     * @param fileTransferId File transfer ID
     * @param state File transfer state (see restriction above)
     * @param reasonCode File transfer state reason code
     */
    @Override
    public boolean setFileTransferStateAndReasonCode(String fileTransferId, State state,
            ReasonCode reasonCode) {
        if (sLogger.isActivated()) {
            sLogger.debug("setFileTransferStateAndReasonCode: fileTransferId=" + fileTransferId
                    + ", state=" + state + ", reasonCode=" + reasonCode);
        }

        switch (state) {
            case DELIVERED:
            case DISPLAYED:
                throw new IllegalArgumentException("State that requires "
                        + "timestamp passed, use specific method taking timestamp"
                        + " to set state " + state.toString());
            default:
        }

        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_STATE, state.toInt());
        values.put(FileTransferData.KEY_REASON_CODE, reasonCode.toInt());
        return mLocalContentResolver.update(
                Uri.withAppendedPath(FileTransferData.CONTENT_URI, fileTransferId), values,
                SELECTION_BY_UNDELIVERED_STATUS, null) > 0;
    }

    @Override
    public int markFileTransferAsRead(String fileTransferId, long timestampDisplayed) {
        if (sLogger.isActivated()) {
            sLogger.debug("Mark file transfer as read ID=" + fileTransferId);
        }
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_READ_STATUS, ReadStatus.READ.toInt());
        values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, timestampDisplayed);
        return mLocalContentResolver.update(
                Uri.withAppendedPath(FileTransferData.CONTENT_URI, fileTransferId), values,
                SELECTION_BY_NOT_READ, null);
    }

    @Override
    public boolean setFileTransferProgress(String fileTransferId, long currentSize) {
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_TRANSFERRED, currentSize);
        return mLocalContentResolver.update(
                Uri.withAppendedPath(FileTransferData.CONTENT_URI, fileTransferId), values, null,
                null) > 0;
    }

    @Override
    public Long getFileTransferProgress(String fileTransferId) {
        Cursor cursor = getFileTransferData(FileTransferData.KEY_TRANSFERRED, fileTransferId);
        if (cursor == null) {
            return null;
        }
        return getDataAsLong(cursor);
    }

    @Override
    public boolean setFileTransferred(String fileTransferId, MmContent content,
            long fileExpiration, long fileIconExpiration, long deliveryExpiration) {
        if (sLogger.isActivated()) {
            sLogger.debug("setFileTransferred (Id=" + fileTransferId + ") (uri=" + content.getUri()
                    + ")");
        }
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_STATE, State.TRANSFERRED.toInt());
        values.put(FileTransferData.KEY_REASON_CODE, ReasonCode.UNSPECIFIED.toInt());
        values.put(FileTransferData.KEY_TRANSFERRED, content.getSize());
        values.put(FileTransferData.KEY_FILE_EXPIRATION, fileExpiration);
        values.put(FileTransferData.KEY_FILEICON_EXPIRATION, fileIconExpiration);
        values.put(FileTransferData.KEY_DELIVERY_EXPIRATION, deliveryExpiration);
        return mLocalContentResolver.update(
                Uri.withAppendedPath(FileTransferData.CONTENT_URI, fileTransferId), values, null,
                null) > 0;
    }

    @Override
    public boolean isFileTransfer(String fileTransferId) {
        Cursor cursor = null;
        try {
            Uri contentUri = Uri.withAppendedPath(FileTransferData.CONTENT_URI, fileTransferId);
            cursor = mLocalContentResolver.query(contentUri, PROJECTION_FILE_TRANSFER_ID, null,
                    null, null);
            CursorUtil.assertCursorIsNotNull(cursor, contentUri);
            return cursor.moveToNext();
        } finally {
            CursorUtil.close(cursor);
        }
    }

    @Override
    public boolean setFileUploadTId(String fileTransferId, String tId) {
        if (sLogger.isActivated()) {
            sLogger.debug("setFileUploadTId (tId=" + tId + ") (fileTransferId=" + fileTransferId
                    + ")");
        }
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_UPLOAD_TID, tId);
        return mLocalContentResolver.update(
                Uri.withAppendedPath(FileTransferData.CONTENT_URI, fileTransferId), values, null,
                null) > 0;
    }

    @Override
    public boolean setFileDownloadAddress(String fileTransferId, Uri downloadAddress) {
        if (sLogger.isActivated()) {
            sLogger.debug("setFileDownloadAddress (address=" + downloadAddress
                    + ") (fileTransferId=" + fileTransferId + ")");
        }
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_DOWNLOAD_URI, downloadAddress.toString());
        return mLocalContentResolver.update(
                Uri.withAppendedPath(FileTransferData.CONTENT_URI, fileTransferId), values, null,
                null) > 0;
    }

    @Override
    public boolean setRemoteSipId(String fileTransferId, String sipInstanceId) {
        if (sLogger.isActivated()) {
            sLogger.debug("setRemoteSipId (sip ID=" + sipInstanceId + ") (fileTransferId="
                    + fileTransferId + ")");
        }
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_REMOTE_SIP_ID, sipInstanceId);
        return mLocalContentResolver.update(
                Uri.withAppendedPath(FileTransferData.CONTENT_URI, fileTransferId), values, null,
                null) > 0;
    }

    @Override
    public List<FtHttpResume> retrieveFileTransfersPausedBySystem() {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(FileTransferData.CONTENT_URI, null,
                    SELECTION_BY_PAUSED_BY_SYSTEM, null, ORDER_BY_TIMESTAMP_ASC);
            CursorUtil.assertCursorIsNotNull(cursor, FileTransferData.CONTENT_URI);
            if (!cursor.moveToNext()) {
                return Collections.emptyList();
            }

            int sizeColumnIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FILESIZE);
            int mimeTypeColumnIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_MIME_TYPE);
            int contactColumnIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_CONTACT);
            int chatIdColumnIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_CHAT_ID);
            int fileColumnIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FILE);
            int fileNameColumnIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FILENAME);
            int directionColumnIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_DIRECTION);
            int fileTransferIdColumnIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FT_ID);
            int fileIconColumnIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FILEICON);
            int timestampColumnIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_TIMESTAMP);
            int timestampSentColumnIdx = cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_TIMESTAMP_SENT);

            int downloadUriColumnIdx = cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_DOWNLOAD_URI);
            int tIdColumnIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_UPLOAD_TID);
            int fileExpirationColumnIdx = cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_FILE_EXPIRATION);
            int iconExpirationColumnIdx = cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_FILEICON_EXPIRATION);
            int remoteSipIdColumnIdx = cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_REMOTE_SIP_ID);

            List<FtHttpResume> fileTransfers = new ArrayList<>();
            do {
                long size = cursor.getLong(sizeColumnIdx);
                String mimeType = cursor.getString(mimeTypeColumnIdx);
                String fileTransferId = cursor.getString(fileTransferIdColumnIdx);
                String phoneNumber = cursor.getString(contactColumnIdx);
                ContactId contact = phoneNumber == null ? null : ContactUtil
                        .createContactIdFromTrustedData(phoneNumber);
                String chatId = cursor.getString(chatIdColumnIdx);
                String file = cursor.getString(fileColumnIdx);
                String fileName = cursor.getString(fileNameColumnIdx);
                Direction direction = Direction.valueOf(cursor.getInt(directionColumnIdx));
                String fileIcon = cursor.getString(fileIconColumnIdx);
                long timestamp = cursor.getLong(timestampColumnIdx);
                long timestampSent = cursor.getLong(timestampSentColumnIdx);
                boolean isGroup = !chatId.equals(phoneNumber);
                MmContent content = ContentManager.createMmContent(Uri.parse(file), mimeType, size,
                        fileName);
                Uri fileIconUri = fileIcon != null ? Uri.parse(fileIcon) : null;
                if (direction == Direction.INCOMING) {

                    String downloadUri = cursor.getString(downloadUriColumnIdx);
                    long fileExpiration = cursor.getLong(fileExpirationColumnIdx);
                    long iconExpiration = cursor.getLong(iconExpirationColumnIdx);
                    String remoteSipId = cursor.getString(remoteSipIdColumnIdx);
                    /*
                     * File transfer is paused by system only if already accepted
                     */
                    fileTransfers.add(new FtHttpResumeDownload(Uri.parse(downloadUri), Uri
                            .parse(file), fileIconUri, content, contact, chatId, fileTransferId,
                            isGroup, timestamp, timestampSent, fileExpiration, iconExpiration,
                            true, remoteSipId));
                } else {
                    String tId = cursor.getString(tIdColumnIdx);
                    fileTransfers.add(new FtHttpResumeUpload(content, fileIconUri, tId, contact,
                            chatId, fileTransferId, isGroup, timestamp, timestampSent));
                }
            } while (cursor.moveToNext());
            return fileTransfers;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    @Override
    public FtHttpResumeUpload retrieveFtHttpResumeUpload(String tId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(FileTransferData.CONTENT_URI, null,
                    SELECTION_FILE_BY_T_ID, new String[] {
                        tId
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, FileTransferData.CONTENT_URI);
            if (!cursor.moveToNext()) {
                return null;
            }
            String fileName = cursor.getString(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_FILENAME));
            long size = cursor.getLong(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_TRANSFERRED));
            String mimeType = cursor.getString(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_MIME_TYPE));
            String fileTransferId = cursor.getString(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_FT_ID));
            String phoneNumber = cursor.getString(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_CONTACT));
            ContactId contact = phoneNumber == null ? null : ContactUtil
                    .createContactIdFromTrustedData(phoneNumber);
            long timestamp = cursor.getLong(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_TIMESTAMP));
            long timestampSent = cursor.getLong(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_TIMESTAMP_SENT));
            String chatId = cursor.getString(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_CHAT_ID));
            String file = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferData.KEY_FILE));
            String fileIcon = cursor.getString(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_FILEICON));
            boolean isGroup = !chatId.equals(phoneNumber);
            MmContent content = ContentManager.createMmContent(Uri.parse(file), mimeType, size,
                    fileName);
            Uri fileIconUri = fileIcon != null ? Uri.parse(fileIcon) : null;
            return new FtHttpResumeUpload(content, fileIconUri, tId, contact, chatId,
                    fileTransferId, isGroup, timestamp, timestampSent);

        } finally {
            CursorUtil.close(cursor);
        }
    }

    private Cursor getFileTransferData(String columnName, String fileTransferId) {
        String[] projection = {
            columnName
        };
        Uri contentUri = Uri.withAppendedPath(FileTransferData.CONTENT_URI, fileTransferId);
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
    public Cursor getFileTransferData(String fileTransferId) {
        Uri contentUri = Uri.withAppendedPath(FileTransferData.CONTENT_URI, fileTransferId);
        Cursor cursor = mLocalContentResolver.query(contentUri, null, null, null, null);
        CursorUtil.assertCursorIsNotNull(cursor, contentUri);
        return cursor;
    }

    @Override
    public Uri getFileTransferIcon(String fileTransferId) {
        Cursor cursor = getFileTransferData(FileTransferData.KEY_FILEICON, fileTransferId);
        if (cursor == null) {
            return null;
        }
        String uriString = getDataAsString(cursor);
        if (uriString == null) {
            return null;
        }
        return Uri.parse(uriString);
    }

    @Override
    public State getFileTransferState(String fileTransferId) {
        Cursor cursor = getFileTransferData(FileTransferData.KEY_STATE, fileTransferId);
        if (cursor == null) {
            return null;
        }
        return State.valueOf(getDataAsInteger(cursor));
    }

    @Override
    public ReasonCode getFileTransferReasonCode(String fileTransferId) {
        Cursor cursor = getFileTransferData(FileTransferData.KEY_REASON_CODE, fileTransferId);
        if (cursor == null) {
            return null;
        }
        return ReasonCode.valueOf(getDataAsInteger(cursor));
    }

    @Override
    public Long getFileTransferTimestamp(String fileTransferId) {
        Cursor cursor = getFileTransferData(FileTransferData.KEY_TIMESTAMP, fileTransferId);
        if (cursor == null) {
            return null;
        }
        return getDataAsLong(cursor);
    }

    @Override
    public Long getFileTransferSentTimestamp(String fileTransferId) {
        Cursor cursor = getFileTransferData(FileTransferData.KEY_TIMESTAMP_SENT, fileTransferId);
        if (cursor == null) {
            return null;
        }
        return getDataAsLong(cursor);
    }

    @Override
    public String getFileTransferChatId(String fileTransferId) {
        Cursor cursor = getFileTransferData(FileTransferData.KEY_CHAT_ID, fileTransferId);
        if (cursor == null) {
            return null;
        }
        return getDataAsString(cursor);
    }

    @Override
    public Boolean isFileTransferExpiredDelivery(String fileTransferId) {
        Cursor cursor = getFileTransferData(FileTransferData.KEY_EXPIRED_DELIVERY, fileTransferId);
        if (cursor == null) {
            return null;
        }
        return getDataAsBoolean(cursor);
    }

    @Override
    public boolean isGroupFileTransfer(String fileTransferId) {
        /*
         * Warning: return true if record does not exist.
         */
        Cursor cursor = null;
        try {
            Uri contentUri = Uri.withAppendedPath(FileTransferData.CONTENT_URI, fileTransferId);
            cursor = mLocalContentResolver.query(contentUri, PROJECTION_FILE_TRANSFER_ID,
                    SELECTION_BY_EQUAL_CHAT_ID_AND_CONTACT, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, contentUri);
            /*
             * For a one-to-one file transfer, value of chatID is equal to the value of contact
             */
            return !cursor.moveToNext();

        } finally {
            CursorUtil.close(cursor);
        }
    }

    @Override
    public boolean isOneToManyFileTransfer(String fileTransferId) {
        return false;
    }

    @Override
    public FtHttpResume getFileTransferResumeInfo(String fileTransferId) {
        Cursor cursor = null;
        try {
            Uri contentUri = Uri.withAppendedPath(FileTransferData.CONTENT_URI, fileTransferId);
            cursor = mLocalContentResolver.query(contentUri, null, null, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, contentUri);
            if (!cursor.moveToNext()) {
                return null;
            }
            String phoneNumber = cursor.getString(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_CONTACT));
            ContactId contact = phoneNumber != null ? ContactUtil
                    .createContactIdFromTrustedData(phoneNumber) : null;
            String chatId = cursor.getString(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_CHAT_ID));
            String fileUri = cursor.getString(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_FILE));
            Direction direction = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_DIRECTION)));
            String fileIcon = cursor.getString(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_FILEICON));
            long timestamp = cursor.getLong(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_TIMESTAMP));
            long timestampSent = cursor.getLong(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_TIMESTAMP_SENT));
            long fileSize = cursor.getLong(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_FILESIZE));
            String fileName = cursor.getString(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_FILENAME));
            String fileMimetype = cursor.getString(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_MIME_TYPE));
            boolean isGroup = !chatId.equals(phoneNumber);
            Uri file = Uri.parse(fileUri);
            MmContent content = ContentManager.createMmContent(file, fileMimetype, fileSize,
                    fileName);
            Uri fileIconUri = fileIcon != null ? Uri.parse(fileIcon) : null;
            if (Direction.INCOMING == direction) {
                String downloadUri = cursor.getString(cursor
                        .getColumnIndexOrThrow(FileTransferData.KEY_DOWNLOAD_URI));
                long fileExpiration = cursor.getLong(cursor
                        .getColumnIndexOrThrow(FileTransferData.KEY_FILE_EXPIRATION));
                long iconExpiration = cursor.getLong(cursor
                        .getColumnIndexOrThrow(FileTransferData.KEY_FILEICON_EXPIRATION));
                FileTransfer.State state = FileTransfer.State.valueOf(cursor.getInt(cursor
                        .getColumnIndexOrThrow(FileTransferData.KEY_STATE)));
                String remoteSipId = cursor.getString(cursor
                        .getColumnIndexOrThrow(FileTransferData.KEY_REMOTE_SIP_ID));
                /*
                 * If state is INVITED then file transfer is not accepted
                 */
                boolean accepted = !(FileTransfer.State.INVITED == state);
                return new FtHttpResumeDownload(Uri.parse(downloadUri), file, fileIconUri, content,
                        contact, chatId, fileTransferId, isGroup, timestamp, timestampSent,
                        fileExpiration, iconExpiration, accepted, remoteSipId);
            }
            String tId = cursor.getString(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_UPLOAD_TID));
            return new FtHttpResumeUpload(content, fileIconUri, tId, contact, chatId,
                    fileTransferId, isGroup, timestamp, timestampSent);

        } finally {
            CursorUtil.close(cursor);
        }
    }

    @Override
    public Cursor getQueuedAndUploadedButNotTransferredFileTransfers() {
        Cursor cursor = mLocalContentResolver.query(FileTransferData.CONTENT_URI, null,
                SELECTION_BY_QUEUED_AND_UPLOADED_BUT_NOT_TRANSFERRED_FILE_TRANSFERS, null,
                ORDER_BY_TIMESTAMP_ASC);
        CursorUtil.assertCursorIsNotNull(cursor, FileTransferData.CONTENT_URI);
        return cursor;
    }

    @Override
    public Cursor getInterruptedFileTransfers() {
        Cursor cursor = mLocalContentResolver.query(FileTransferData.CONTENT_URI, null,
                SELECTION_BY_INTERRUPTED_FILE_TRANSFERS, null, ORDER_BY_TIMESTAMP_ASC);
        CursorUtil.assertCursorIsNotNull(cursor, FileTransferData.CONTENT_URI);
        return cursor;
    }

    @Override
    public boolean setFileTransferStateAndTimestamp(String fileTransferId, State state,
            ReasonCode reasonCode, long timestamp, long timestampSent) {
        if (sLogger.isActivated()) {
            sLogger.debug("setFileTransferStateAndTimestamp: fileTransferId=" + fileTransferId
                    + ", state=" + state + ", reasonCode=" + reasonCode + ", timestamp="
                    + timestamp + ", timestampSent=" + timestampSent);
        }

        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_STATE, state.toInt());
        values.put(FileTransferData.KEY_REASON_CODE, reasonCode.toInt());
        values.put(FileTransferData.KEY_TIMESTAMP, timestamp);
        values.put(FileTransferData.KEY_TIMESTAMP_SENT, timestampSent);
        return mLocalContentResolver.update(
                Uri.withAppendedPath(FileTransferData.CONTENT_URI, fileTransferId), values, null,
                null) > 0;
    }

    @Override
    public boolean setFileTransferDelivered(String fileTransferId, long timestampDelivered) {
        if (sLogger.isActivated()) {
            sLogger.debug("setFileTransferDelivered fileTransferId=" + fileTransferId
                    + ", timestampDelivered=" + timestampDelivered);
        }

        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_STATE, FileTransfer.State.DELIVERED.toInt());
        values.put(FileTransferData.KEY_REASON_CODE, FileTransfer.ReasonCode.UNSPECIFIED.toInt());
        values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, timestampDelivered);
        values.put(FileTransferData.KEY_EXPIRED_DELIVERY, 0);

        return mLocalContentResolver.update(
                Uri.withAppendedPath(FileTransferData.CONTENT_URI, fileTransferId), values,
                SELECTION_BY_NOT_DISPLAYED, null) > 0;
    }

    @Override
    public boolean setFileTransferDisplayed(String fileTransferId, long timestampDisplayed) {
        if (sLogger.isActivated()) {
            sLogger.debug("setFileTransferDisplayed fileTransferId=" + fileTransferId
                    + ", timestampDisplayed=" + timestampDisplayed);
        }

        ContentValues values = new ContentValues();

        values.put(FileTransferData.KEY_STATE, FileTransfer.State.DISPLAYED.toInt());
        values.put(FileTransferData.KEY_REASON_CODE, FileTransfer.ReasonCode.UNSPECIFIED.toInt());
        values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, timestampDisplayed);
        values.put(FileTransferData.KEY_EXPIRED_DELIVERY, 0);

        return mLocalContentResolver.update(
                Uri.withAppendedPath(FileTransferData.CONTENT_URI, fileTransferId), values, null,
                null) > 0;
    }

    @Override
    public void clearFileTransferDeliveryExpiration(List<String> fileTransferIds) {
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_EXPIRED_DELIVERY, 0);
        values.put(FileTransferData.KEY_DELIVERY_EXPIRATION, 0);
        List<String> parameters = new ArrayList<>();
        for (int i = 0; i < fileTransferIds.size(); i++) {
            parameters.add("?");
        }
        String selection = FileTransferData.KEY_FT_ID + " IN (" + TextUtils.join(",", parameters)
                + ")";
        mLocalContentResolver.update(FileTransferData.CONTENT_URI, values, selection,
                fileTransferIds.toArray(new String[fileTransferIds.size()]));
    }

    @Override
    public boolean setFileTransferDeliveryExpired(String fileTransferId) {
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_EXPIRED_DELIVERY, 1);
        return mLocalContentResolver.update(
                Uri.withAppendedPath(FileTransferData.CONTENT_URI, fileTransferId), values, null,
                null) > 0;
    }

    @Override
    public Cursor getUnDeliveredOneToOneFileTransfers() {
        Cursor cursor = mLocalContentResolver.query(FileTransferData.CONTENT_URI, null,
                SELECTION_BY_UNDELIVERED_ONETOONE_FILE_TRANSFERS, null, ORDER_BY_TIMESTAMP_ASC);
        CursorUtil.assertCursorIsNotNull(cursor, FileTransferData.CONTENT_URI);
        return cursor;
    }

    @Override
    public void setFileTransferDownloadInfo(String fileTransferId,
            FileTransferHttpInfoDocument ftHttpInfo) {
        if (sLogger.isActivated()) {
            sLogger.debug("setFileTransferDownloadInfo fileTransferId=".concat(fileTransferId));
        }
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_DOWNLOAD_URI, ftHttpInfo.getUri().toString());
        values.put(FileTransferData.KEY_TRANSFERRED, ftHttpInfo.getSize());
        values.put(FileTransferData.KEY_FILE_EXPIRATION, ftHttpInfo.getExpiration());
        FileTransferHttpThumbnail fileIcon = ftHttpInfo.getFileThumbnail();
        if (fileIcon != null) {
            values.put(FileTransferData.KEY_FILEICON_DOWNLOAD_URI, fileIcon.getUri().toString());
            values.put(FileTransferData.KEY_FILEICON_SIZE, fileIcon.getSize());
            values.put(FileTransferData.KEY_FILEICON_MIME_TYPE, fileIcon.getMimeType());
            values.put(FileTransferData.KEY_FILEICON_EXPIRATION, fileIcon.getExpiration());
        }
        mLocalContentResolver.update(
                Uri.withAppendedPath(FileTransferData.CONTENT_URI, fileTransferId), values, null,
                null);
    }

    @Override
    public FileTransferHttpInfoDocument getFileDownloadInfo(Cursor cursor)
            throws FileAccessException {
        String fileTransferId = cursor.getString(cursor
                .getColumnIndexOrThrow(FileTransferData.KEY_FT_ID));
        String file = cursor.getString(cursor
                .getColumnIndexOrThrow(FileTransferData.KEY_DOWNLOAD_URI));
        if (file == null) {
            throw new FileAccessException(
                    "File download URI not available for file transfer ID=".concat(fileTransferId));
        }
        String fileName = cursor.getString(cursor
                .getColumnIndexOrThrow(FileTransferData.KEY_FILENAME));
        int size = cursor.getInt(cursor.getColumnIndexOrThrow(FileTransferData.KEY_TRANSFERRED));
        String mimeType = cursor.getString(cursor
                .getColumnIndexOrThrow(FileTransferData.KEY_MIME_TYPE));
        long fileExpiration = cursor.getLong(cursor
                .getColumnIndexOrThrow(FileTransferData.KEY_FILE_EXPIRATION));
        long fileIconExpiration = cursor.getLong(cursor
                .getColumnIndexOrThrow(FileTransferData.KEY_FILEICON_EXPIRATION));
        String fileIcon = cursor.getString(cursor
                .getColumnIndexOrThrow(FileTransferData.KEY_FILEICON_DOWNLOAD_URI));
        int fileIconSize = cursor.getInt(cursor
                .getColumnIndexOrThrow(FileTransferData.KEY_FILEICON_SIZE));
        String fileIconMimeType = cursor.getString(cursor
                .getColumnIndexOrThrow(FileTransferData.KEY_FILEICON_MIME_TYPE));
        FileTransferHttpThumbnail fileIconData = fileIcon != null ? new FileTransferHttpThumbnail(
                mRcsSettings, Uri.parse(fileIcon), fileIconMimeType, fileIconSize,
                fileIconExpiration) : null;
        FileTransferHttpInfoDocument infoDoc = new FileTransferHttpInfoDocument(mRcsSettings,
                Uri.parse(file), fileName, size, mimeType, fileExpiration, fileIconData);
        Disposition disposition = Disposition.valueOf(cursor.getInt(cursor
                .getColumnIndexOrThrow(FileTransferData.KEY_DISPOSITION)));
        infoDoc.setFileDisposition(disposition);
        if (MimeManager.isAudioType(mimeType)) {
            file = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferData.KEY_FILE));
            long duration = FileUtils.getDurationFromFile(AndroidFactory.getApplicationContext(),
                    Uri.parse(file));
            infoDoc.setPlayingLength((int) (duration / 1000L) + 1);
        }
        return infoDoc;
    }

    @Override
    public FileTransferHttpInfoDocument getFileDownloadInfo(String fileTransferId)
            throws FileAccessException {
        Cursor cursor = null;
        try {
            Uri contentUri = Uri.withAppendedPath(FileTransferData.CONTENT_URI, fileTransferId);
            cursor = mLocalContentResolver.query(contentUri, null, null, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, FileTransferData.CONTENT_URI);
            if (!cursor.moveToNext()) {
                return null;
            }
            return getFileDownloadInfo(cursor);

        } finally {
            CursorUtil.close(cursor);
        }
    }

    @Override
    public void setFileTransferTimestamps(String fileTransferId, long timestamp, long timestampSent) {
        if (sLogger.isActivated()) {
            sLogger.debug("setFileTransferTimestamps: fileTransferId=" + fileTransferId
                    + ", timestamp=" + timestamp + ", timestampSent=" + timestampSent);
        }
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_TIMESTAMP, timestamp);
        values.put(FileTransferData.KEY_TIMESTAMP_SENT, timestampSent);
        mLocalContentResolver.update(
                Uri.withAppendedPath(FileTransferData.CONTENT_URI, fileTransferId), values, null,
                null);
    }

    @Override
    public boolean setFileInfoDequeued(String fileTransferId, long deliveryExpiration) {
        if (sLogger.isActivated()) {
            sLogger.debug("setFileInfoDequeued (Id=" + fileTransferId + ") (deliveryExpiration="
                    + deliveryExpiration + ")");
        }
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_STATE, State.TRANSFERRED.toInt());
        values.put(FileTransferData.KEY_REASON_CODE, ReasonCode.UNSPECIFIED.toInt());
        values.put(FileTransferData.KEY_DELIVERY_EXPIRATION, deliveryExpiration);
        return mLocalContentResolver.update(
                Uri.withAppendedPath(FileTransferData.CONTENT_URI, fileTransferId), values, null,
                null) > 0;
    }

    @Override
    public Uri getFile(String fileTransferId) {
        Cursor cursor = getFileTransferData(FileTransferData.KEY_FILE, fileTransferId);
        if (cursor == null) {
            return null;
        }
        return Uri.parse(getDataAsString(cursor));
    }

    @Override
    public Direction getFileTransferDirection(String fileTransferId) {
        Cursor cursor = getFileTransferData(FileTransferData.KEY_DIRECTION, fileTransferId);
        if (cursor == null) {
            return null;
        }
        return Direction.valueOf(getDataAsInteger(cursor));
    }
}

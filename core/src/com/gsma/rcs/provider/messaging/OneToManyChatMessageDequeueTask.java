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

package com.gsma.rcs.provider.messaging;

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.DequeueTask;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.service.api.OneToManyChatImpl;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.database.Cursor;

import java.util.Set;

/**
 * OneToManyChatMessageDequeueTask tries to dequeues and sends the queued one-to-many chat messages of a
 * specific chatId.
 */
public class OneToManyChatMessageDequeueTask extends DequeueTask {

    private final String mChatId;

    public OneToManyChatMessageDequeueTask(Context ctx, Core core, String chatId,
            MessagingLog messagingLog, ChatServiceImpl chatService, RcsSettings rcsSettings,
            ContactManager contactManager, FileTransferServiceImpl fileTransferService) {
        super(ctx, core, contactManager, messagingLog, rcsSettings, chatService,
                fileTransferService);
        mChatId = chatId;
    }

    @Override
    public void run() {
        boolean logActivated = mLogger.isActivated();
        if (logActivated) {
            mLogger.debug("Execute task to dequeue one-to-many chat messages for chatId " + mChatId);
        }
        String id = null;
        String mimeType = null;
        Cursor cursor = null;
        try {
            if (!isImsConnected()) {
                if (logActivated) {
                    mLogger.debug("IMS not connected, exiting dequeue task to dequeue one-to-many chat messages for chatId " + mChatId);
                }
                return;
            }
            if (isShuttingDownOrStopped()) {
                if (logActivated) {
                    mLogger.debug("Core service is shutting down/stopped, exiting dequeue task to dequeue one-to-many chat messages for chatId " + mChatId);
                }
                return;
            }
            cursor = mMessagingLog.getQueuedOneToManyChatMessages(mChatId);
            int msgIdIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_MESSAGE_ID);
            int contentIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_CONTENT);
            int mimeTypeIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_MIME_TYPE);
            OneToManyChatImpl oneToManyChat = mChatService.getOrCreateOneToManyChat(mChatId);
            while (cursor.moveToNext()) {
                try {
                    if (!isImsConnected()) {
                        if (logActivated) {
                            mLogger.debug("IMS not connected, exiting dequeue task to dequeue one-to-many chat messages for chatId " + mChatId);
                        }
                        return;
                    }
                    if (isShuttingDownOrStopped()) {
                        if (logActivated) {
                            mLogger.debug("Core service is shutting down/stopped, exiting dequeue task to dequeue one-to-many chat messages for chatId " + mChatId);
                        }
                        return;
                    }
                    id = cursor.getString(msgIdIdx);
                    mimeType = cursor.getString(mimeTypeIdx);
                    Set<ContactId> contacts = null;
                    if (!isPossibleToDequeueOneToManyChatMessage(contacts)) {
                        setOneToManyChatMessageAsFailedDequeue(mChatId, id, mimeType);
                        continue;
                    }
                    // if (!isAllowedToDequeueOneToManyChatMessage(contacts)) {
                    // continue;
                    // }
                    String content = cursor.getString(contentIdx);
                    long timestamp = System.currentTimeMillis();
                    /* For outgoing message, timestampSent = timestamp */
                    ChatMessage msg = ChatUtils.createChatMessage(id, mimeType, content, null,
                            null, timestamp, timestamp);
                    oneToManyChat.dequeueOneToManyChatMessage(msg);

                } catch ( NetworkException e) {
                    if (logActivated) {
                        mLogger.debug("Failed to dequeue one-to-many chat message '" + id
                                + "' message for chatId '" + mChatId+ "' due to: "
                                + e.getMessage());
                    }
                } catch (PayloadException e) {
                    mLogger.error("Failed to dequeue one-to-many chat message '" + id
                            + "' message for chatId '" + mChatId, e);
                    setOneToManyChatMessageAsFailedDequeue(mChatId, id, mimeType);

                } catch (RuntimeException e) {
                    /*
                     * Normally all the terminal and non-terminal cases should be handled above so
                     * if we come here that means that there is a bug and so we output a stack trace
                     * so the bug can then be properly tracked down and fixed. We also mark the
                     * respective entry that failed to dequeue as FAILED.
                     */
                    mLogger.error("Failed to dequeue one-to-many chat message '" + id
                            + "'for chatId '" + mChatId + "' ", e);
                    setOneToManyChatMessageAsFailedDequeue(mChatId, id, mimeType);
                }
            }
        } catch (RuntimeException e) {
            /*
             * Normally all the terminal and non-terminal cases should be handled above so if we
             * come here that means that there is a bug and so we output a stack trace so the bug
             * can then be properly tracked down and fixed. We also mark the respective entry that
             * failed to dequeue as FAILED.
             */
            mLogger.error("Exception occured while dequeueing one-to-many chat message with msgId '"
                    + id + "'for chatId '" + mChatId + "' ", e);
            if (id == null) {
                return;
            }
            setOneToManyChatMessageAsFailedDequeue(mChatId, id, mimeType);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}

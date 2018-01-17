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
import com.gsma.rcs.service.api.StandaloneMessagingServiceImpl;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.database.Cursor;

import java.util.Set;

/**
 * StandaloneMessageDequeueTask tries to dequeue all queued standalone messages of a specific chat
 * id
 */
public class StandaloneMessageDequeueTask extends DequeueTask {

    private final String mChatId;
    private final StandaloneMessagingServiceImpl mSmService;

    public StandaloneMessageDequeueTask(Context ctx, Core core, MessagingLog messagingLog,
            StandaloneMessagingServiceImpl smService, ChatServiceImpl chatService,
            FileTransferServiceImpl fileTransferService, ContactManager contactManager,
            RcsSettings rcsSettings, String chatId) {
        super(ctx, core, contactManager, messagingLog, rcsSettings, chatService,
                fileTransferService);
        mSmService = smService;
        mChatId = chatId;
    }

    public void run() {
        boolean logActivated = mLogger.isActivated();
        if (logActivated) {
            mLogger.debug("Execute task to dequeue standalone messages for chatId ".concat(mChatId));
        }
        Set<ContactId> recipients = null;
        String convId = null;
        String msgId = null;
        String mimeType = null;
        String content = null;
        boolean barCycle = false;
        Cursor cursor = null;
        try {
            if (!isImsConnected()) {
                if (logActivated) {
                    mLogger.debug("IMS not connected, exiting dequeue task to dequeue standalone messages");
                }
                return;
            }
            if (isShuttingDownOrStopped()) {
                if (logActivated) {
                    mLogger.debug("Core service is shutting down/stopped, exiting dequeue task to dequeue standalone messages");
                }
                return;
            }
            cursor = mMessagingLog.getQueuedStandaloneMessages(mChatId);
            int convIdIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_CONVERSATION_ID);
            int msgIdIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_MESSAGE_ID);
            int contactIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_CONTACT);
            int contentIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_CONTENT);
            int mimeTypeIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_MIME_TYPE);
            //int pcIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_PC_MSG);
            int barCycleIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_BAR_CYCLE);
            while (cursor.moveToNext()) {
                try {
                    if (!isImsConnected()) {
                        if (logActivated) {
                            mLogger.debug("IMS not connected, exiting dequeue task to dequeue standalone messages");
                        }
                        return;
                    }
                    if (isShuttingDownOrStopped()) {
                        if (logActivated) {
                            mLogger.debug("Core service is shutting down/stopped, exiting dequeue task to dequeue standalone messages");
                        }
                        return;
                    }
                    convId = cursor.getString(convIdIdx);
                    msgId = cursor.getString(msgIdIdx);
                    mimeType = cursor.getString(mimeTypeIdx);
                    content = cursor.getString(contentIdx);
                    String semiSepNumbers = cursor.getString(contactIdx);
                    recipients = ContactUtil.parseUniqueEncodedRecipients(semiSepNumbers);
                    boolean isOneToMany = ContactUtil.isMultiplePhoneNumber(numbers);
                    if (!isPossibleToDequeueStandaloneMessage()) {
                        setStandaloneMessageAsFailedDequeue(mChatId, msgId, mimeType);
                        continue;
                    }
                    if (!isAllowedToDequeueStandaloneMessage(/*recipients*/)) {
                        continue;
                    }
                    if ( barCycle) {
                        if (!isAllowedToDequeueOneToOneBarIm(null)) {
                            continue;
                        }
                    }

                    long timestamp = System.currentTimeMillis();
                    /* For outgoing message, timestampSent = timestamp */
                    ChatMessage msg = ChatUtils.createChatMessage(msgId, mimeType, content, null,
                            null, timestamp, timestamp);
                    if (isOneToMany) {
                        mSmService.dequeueOneToOneStandaloneMessage(mChatId, msg);
                    } else {
                        mSmService.dequeueOneToManyStandaloneMessage(mChatId, null, msg);
                    }

                } catch (NetworkException e) {
                    if (logActivated) {
                        mLogger.debug("Failed to dequeue standalone message '" + msgId
                                + "' message for chat '" + mChatId + "' due to: " + e.getMessage());
                    }
                } catch (PayloadException e) {
                    mLogger.error("Failed to dequeue standalone message '" + msgId
                            + "' message for chat '" + mChatId + "'", e);
                    setStandaloneMessageAsFailedDequeue(mChatId, msgId, mimeType);
                }
            }
        } catch (RuntimeException e) {
            /*
             * Normally all the terminal and non-terminal cases should be handled above so if we
             * come here that means that there is a bug and so we output a stack trace so the bug
             * can then be properly tracked down and fixed. We also mark the respective entry that
             * failed to dequeue as FAILED.
             */
            mLogger.error("Exception occured while dequeueing standalone message with msgId '"
                    + msgId + "'for chatId '" + mChatId + "' ", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

}

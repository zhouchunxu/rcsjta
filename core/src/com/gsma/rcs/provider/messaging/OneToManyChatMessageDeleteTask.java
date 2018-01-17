/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2016 China Mobile.
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

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.standalone.LargeMessageModeSession;
import com.gsma.rcs.provider.DeleteTask;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.ChatService;

import java.util.Set;

/**
 * StandaloneMessageDeleteTask tries to delete all the standalone messages of a specific chat id
 */
public class OneToManyChatMessageDeleteTask extends DeleteTask.GroupedByChatId {

    private static final Logger sLogger = Logger.getLogger(OneToManyChatMessageDeleteTask.class
            .getName());

    private static final String SELECTION_STANDALONE_MESSAGES = new StringBuilder(
            MessageData.KEY_CHAT_ID).append("=").append(MessageData.KEY_CONTACT).append(" AND ")
            .append(MessageData.KEY_SERVICE_ID).append("=")
            .append(ChatService.Service.STANDALONE_MESSAGING.toInt()).toString();

    private static final String SELECTION_STANDALONE_MESSAGES_BY_CHATID = new StringBuilder(
            MessageData.KEY_CHAT_ID).append("=?").append(" AND ")
            .append(MessageData.KEY_SERVICE_ID).append("=")
            .append(ChatService.Service.STANDALONE_MESSAGING.toInt()).toString();

    private static final String SELECTION_GROUPDELIVERY_BY_CHATID = new StringBuilder(
            GroupDeliveryInfoData.KEY_CHAT_ID).append("=?").toString();// TODO, check the 1-n
                                                                       // delivery for memory
                                                                       // leakage

    private final ChatServiceImpl mChatService;

    private final InstantMessagingService mImService;

    /**
     * Deletion of all one to mass messages.
     *
     * @param service the chat service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     */
    public OneToManyChatMessageDeleteTask(ChatServiceImpl service, InstantMessagingService imService,
                                          LocalContentResolver contentResolver) {
        super(contentResolver, MessageData.CONTENT_URI, MessageData.KEY_MESSAGE_ID,
                MessageData.KEY_CHAT_ID, SELECTION_STANDALONE_MESSAGES);
        mChatService = service;
        mImService = imService;
    }

    /**
     * Deletion of all chat messages from the specified group chat id.
     *
     * @param service the chat service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param chatId the chat id
     */
    public OneToManyChatMessageDeleteTask(ChatServiceImpl service, InstantMessagingService imService,
                                          LocalContentResolver contentResolver, String chatId) {
        super(contentResolver, MessageData.CONTENT_URI, MessageData.KEY_MESSAGE_ID,
                MessageData.KEY_CHAT_ID, SELECTION_STANDALONE_MESSAGES_BY_CHATID, chatId);
        mChatService = service;
        mImService = imService;
    }

    /**
     * Deletion of a specific message.
     *
     * @param service the chat service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param chatId the chat id (optional, can be null)
     * @param messageId the message id
     */
    public OneToManyChatMessageDeleteTask(ChatServiceImpl service, InstantMessagingService imService,
                                          LocalContentResolver contentResolver, String chatId, String messageId) {
        super(contentResolver, MessageData.CONTENT_URI, MessageData.KEY_MESSAGE_ID,
                MessageData.KEY_CHAT_ID, null, messageId);
        mChatService = service;
        mImService = imService;
    }

    @Override
    protected void onRowDelete(String chatId, String msgId) throws PayloadException {
        if (isSingleRowDelete()) {
            return;

        }
        // Only suitable for outgoing, incoming session will not be found with this true msgId.
        LargeMessageModeSession session = mImService.getLargeMessageModeSession(msgId);
        if (session == null) {
            mChatService.removeStandaloneMessaging(chatId);
            mLocalContentResolver.delete(GroupDeliveryInfoData.CONTENT_URI,
                    SELECTION_GROUPDELIVERY_BY_CHATID, new String[] {
                        chatId
                    });

            return;
        }
        try {
            session.deleteSession();
        } catch (NetworkException e) {
            /*
             * If network is lost during a delete operation the remaining part of the delete
             * operation (delete from persistent storage) can succeed to 100% anyway since delete
             * can be executed anyway while no network connectivity is present and still succeed.
             */
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
        }
        mChatService.removeStandaloneMessaging(chatId);
        mLocalContentResolver.delete(GroupDeliveryInfoData.CONTENT_URI,
                SELECTION_GROUPDELIVERY_BY_CHATID, new String[] {
                    chatId
                });
    }

    @Override
    protected void onCompleted(String chatId, Set<String> msgIds) {
        mChatService.broadcastStandaloneMessagesDeleted(chatId, msgIds);
    }

}

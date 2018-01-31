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

import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.provider.DeleteTask;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.utils.logger.Logger;

import java.util.Set;

public class OneToManyChatMessageDeleteTask extends DeleteTask.GroupedByChatId {

    private static final String SELECTION_ONETOMANY_CHATMESSAGES = MessageData.KEY_CONTACT
            + " has ';'";

    private static final String SELECTION_ONETOMANY_CHATMESSAGES_BY_CHATID = MessageData.KEY_CHAT_ID
            + "=?";

    private static final String SELECTION_GROUPDELIVERY_BY_CHATID = GroupDeliveryInfoData.KEY_CHAT_ID
            + "=?";

    private static final String SELECTION_GROUPDELIVERY_BY_MSGID = GroupDeliveryInfoData.KEY_ID
            + "=?";

    private final ChatServiceImpl mChatService;

    private final InstantMessagingService mImService;

    private static final Logger sLogger = Logger.getLogger(OneToManyChatMessageDeleteTask.class
            .getName());

    /**
     * Deletion of all one to many messages.
     *
     * @param chatService the chat service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     */
    public OneToManyChatMessageDeleteTask(ChatServiceImpl chatService,
            InstantMessagingService imService, LocalContentResolver contentResolver) {
        super(contentResolver, MessageData.CONTENT_URI, MessageData.KEY_MESSAGE_ID,
                MessageData.KEY_CONTACT, SELECTION_ONETOMANY_CHATMESSAGES);
        mChatService = chatService;
        mImService = imService;
        setAllAtOnce(true);
    }

    /**
     * Deletion of a specified one-to-many chat.
     *
     * @param chatService the chat service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param chatId the chat id
     */
    public OneToManyChatMessageDeleteTask(ChatServiceImpl chatService,
            InstantMessagingService imService, LocalContentResolver contentResolver, String chatId) {
        super(contentResolver, MessageData.CONTENT_URI, MessageData.KEY_MESSAGE_ID,
                MessageData.KEY_CONTACT, SELECTION_ONETOMANY_CHATMESSAGES_BY_CHATID, chatId);
        mChatService = chatService;
        mImService = imService;
        setAllAtOnce(true);
    }

    /**
     * Deletion of a specific message.
     *
     * @param chatService the chat service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param chatId the chat id
     * @param messageId the message id
     */
    public OneToManyChatMessageDeleteTask(ChatServiceImpl chatService,
            InstantMessagingService imService, LocalContentResolver contentResolver, String chatId,
            String messageId) {
        super(contentResolver, MessageData.CONTENT_URI, MessageData.KEY_MESSAGE_ID,
                MessageData.KEY_CHAT_ID, null, messageId);
        mChatService = chatService;
        mImService = imService;
    }

    @Override
    protected void onRowDelete(String chatId, String msgId) throws PayloadException {
        if (isSingleRowDelete()) {
            mLocalContentResolver.delete(GroupDeliveryInfoData.CONTENT_URI,
                    SELECTION_GROUPDELIVERY_BY_MSGID, new String[] {
                        msgId
                    });
        }
    }

    @Override
    protected void onCompleted(String chatId, Set<String> msgIds) {
        if (!isSingleRowDelete()) {
            mLocalContentResolver.delete(GroupDeliveryInfoData.CONTENT_URI,
                    SELECTION_GROUPDELIVERY_BY_CHATID, new String[] {
                        chatId
                    });
        }
        mImService.onOneToManyChatMessagesDeleted(chatId, msgIds);
    }
}

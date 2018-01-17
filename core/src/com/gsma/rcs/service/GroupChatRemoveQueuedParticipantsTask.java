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

package com.gsma.rcs.service;

import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.ChatLog.GroupChat.Participant.Status;

import java.util.HashSet;
import java.util.Set;

public class GroupChatRemoveQueuedParticipantsTask implements Runnable {

    private static final Set<Status> REMOVE_QUEUED_STATUSES = new HashSet<>();
    static {
        REMOVE_QUEUED_STATUSES.add(Status.BOOT_QUEUED);
    }

    private final String mChatId;

    private final ChatServiceImpl mChatService;

    private final InstantMessagingService mImService;

    private static final Logger sLogger = Logger
            .getLogger(GroupChatRemoveQueuedParticipantsTask.class.getName());

    public GroupChatRemoveQueuedParticipantsTask(String chatId, ChatServiceImpl chatService,
            InstantMessagingService imService) {
        mChatId = chatId;
        mChatService = chatService;
        mImService = imService;
    }

    @Override
    public void run() {
//        GroupChatImpl groupChat = mChatService.getOrCreateGroupChat(mChatId);
//        Set<ContactId> participantsToBeRemoved = groupChat.getParticipants(REMOVE_QUEUED_STATUSES)
//                .keySet();
//        if (participantsToBeRemoved.size() == 0) {
//            return;
//        }
//        GroupChatSession session = mImService.getGroupChatSession(mChatId);
//        try {
//            if (session != null && session.isMediaEstablished()) {
//                if (sLogger.isActivated()) {
//                    sLogger.debug(new StringBuilder("Removing ")
//                            .append(Arrays.toString(participantsToBeRemoved.toArray()))
//                            .append(" from the group chat session ").append(mChatId).append(".")
//                            .toString());
//                }
//                if (session.getMaxNumberOfReducedParticipants() < participantsToBeRemoved.size()) {
//                    for (ContactId contact : participantsToBeRemoved) {
//                        groupChat.onRemoveParticipantFailed(contact,
//                                "Minimum number of participants reached");
//                    }
//                    return;
//                }
//                session.removeParticipants(participantsToBeRemoved);
//            }
//        } catch (NetworkException | PayloadException | RuntimeException e) {
//            if (session != null) {
//                session.handleError(new ChatError(ChatError.SEND_RESPONSE_FAILED, e));
//            } else {
//                sLogger.error(ExceptionUtil.getFullStackTrace(e));
//            }
//        }
    }
}

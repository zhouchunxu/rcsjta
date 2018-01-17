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

package com.gsma.rcs.core.ims.service.im.chat;

import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.services.rcs.chat.ChatLog.GroupChat.Participant.Status;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Map;

/**
 * Group chat session listener
 */
public interface GroupChatSessionListener extends ChatSessionListener {

    /**
     * New conference event
     * 
     * @param contact Contact identifier
     * @param status ParticipantStatus for the contact
     * @param timestamp Local timestamp when got notification
     */
    void onConferenceEventReceived(ContactId contact, Status status, long timestamp);

    /**
     * A session invitation has been received
     * 
     * @param contact Remote contact
     * @param subject the subject
     * @param participants Participants
     * @param timestamp Local timestamp when got invitation
     */
    void onSessionInvited(ContactId contact, String subject,
                          Map<ContactId, Status> participants, long timestamp);

    /**
     * Chat is auto-accepted and the session is in the process of being started
     * 
     * @param contact Remote contact
     * @param subject the subject
     * @param participants Participants
     * @param timestamp Local timestamp when got invitation
     */
    void onSessionAutoAccepted(ContactId contact, String subject,
                               Map<ContactId, Status> participants, long timestamp);

    /**
     * One or several participants has been updated
     * 
     * @param updatedParticipants Updated participants
     * @param allParticipants All group participants
     */
    void onParticipantsUpdated(Map<ContactId, Status> updatedParticipants,
            Map<ContactId, Status> allParticipants);

    /**
     * Handle Delivery report send via MSRP Failure
     * 
     * @param msgId the message ID
     * @param chatId the chat ID
     * @param chunktype the chunk type
     */
    void onDeliveryReportSendViaMsrpFailure(String msgId, String chatId, TypeMsrpChunk chunktype);

    /**
     * Handle IM error
     * 
     * @param error Error
     */
    void onImError(ChatError error);
}

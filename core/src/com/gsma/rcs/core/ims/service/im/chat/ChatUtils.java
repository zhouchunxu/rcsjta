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

package com.gsma.rcs.core.ims.service.im.chat;

import static com.gsma.rcs.utils.StringUtils.UTF8;
import static com.gsma.rcs.utils.StringUtils.UTF8_STR;

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.sip.FeatureTags;
import com.gsma.rcs.core.ims.network.sip.Multipart;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimParser;
import com.gsma.rcs.core.ims.service.im.chat.geoloc.GeolocInfoDocument;
import com.gsma.rcs.core.ims.service.im.chat.geoloc.GeolocInfoParser;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnParser;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnUtils;
import com.gsma.rcs.core.ims.service.im.chat.iscomposing.IsComposingInfo;
import com.gsma.rcs.core.ims.service.im.chat.message.CardInfoDocument;
import com.gsma.rcs.core.ims.service.im.chat.message.CardInfoParser;
import com.gsma.rcs.core.ims.service.im.chat.message.CloudFileInfoDocument;
import com.gsma.rcs.core.ims.service.im.chat.message.CloudFileInfoParser;
import com.gsma.rcs.core.ims.service.im.chat.message.MediaArticle;
import com.gsma.rcs.core.ims.service.im.chat.message.VemoticonInfoDocument;
import com.gsma.rcs.core.ims.service.im.chat.message.VemoticonInfoParser;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpResumeInfo;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpResumeInfoParser;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.DateUtils;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.chat.Card;
import com.gsma.services.rcs.chat.CloudFile;
import com.gsma.services.rcs.chat.Emoticon;
import com.gsma.services.rcs.chat.ChatLog.GroupChat.Participant.Status;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.contact.ContactId;

import android.text.TextUtils;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import javax2.sip.header.ContactHeader;
import javax2.sip.header.ExtensionHeader;

/**
 * Chat utility functions
 * 
 * @author jexa7410
 */
public class ChatUtils {
    /**
     * Anonymous URI
     */
    public final static String ANONYMOUS_URI = "sip:anonymous@anonymous.invalid";

    /**
     * Conversation ID header
     */
    public static final String HEADER_CONVERSATION_ID = "Conversation-ID";

    /**
     * Contribution ID header
     */
    public static final String HEADER_CONTRIBUTION_ID = "Contribution-ID";

    private static final String CRLF = "\r\n";

    private static final Logger sLogger = Logger.getLogger(ChatUtils.class.getName());

    /**
     * Get supported feature tags for a group chat
     * 
     * @param rcsSettings the RCS settings accessor
     * @return List of tags
     */
    public static List<String> getSupportedFeatureTagsForGroupChat(RcsSettings rcsSettings) {
        List<String> tags = new ArrayList<>();
        switch (rcsSettings.getImMsgTech()) {
            case CPM:
                tags.add(FeatureTags.FEATURE_3GPP_CPM_SESSION);
                break;
            case SIMPLE_IM:
            default:
                tags.add(FeatureTags.FEATURE_OMA_IM);
                break;
        }
        List<String> additionalRcseTags = new ArrayList<>();
        if (rcsSettings.isGeoLocationPushSupported()) {
            additionalRcseTags.add(FeatureTags.FEATURE_RCSE_GEOLOCATION_PUSH);
        }
        if (rcsSettings.isFileTransferSupported()) {
            additionalRcseTags.add(FeatureTags.FEATURE_RCSE_FT);
        }
        if (rcsSettings.isFileTransferHttpSupported()) {
            additionalRcseTags.add(FeatureTags.FEATURE_RCSE_FT_HTTP);
        }
        if (rcsSettings.isFileTransferStoreForwardSupported()) {
            additionalRcseTags.add(FeatureTags.FEATURE_RCSE_FT_SF);
        }
        if (!additionalRcseTags.isEmpty()) {
            tags.add(FeatureTags.FEATURE_RCSE + "=\"" + TextUtils.join(",", additionalRcseTags)
                    + "\"");
        }
        return tags;
    }

    /**
     * Get Accept-Contact tags for a group chat
     * 
     * @param rcsSettings the RCS settings accessor
     * @return List of tags
     */
    public static List<String> getAcceptContactTagsForGroupChat(RcsSettings rcsSettings) {
        List<String> tags = new ArrayList<>();
        switch (rcsSettings.getImMsgTech()) {
            case CPM:
                tags.add(FeatureTags.FEATURE_3GPP_CPM_SESSION);
                if (rcsSettings.isCmccRelease()) {
                    tags.add(FeatureTags.FEATURE_RCSE + "=\""
                            + FeatureTags.Cmcc.FEATURE_RCSE_GROUP_MANAGE + "\"");
                }
                break;
            case SIMPLE_IM:
            default:
                tags.add(FeatureTags.FEATURE_OMA_IM);
                break;
        }
        return tags;
    }

    /**
     * Get supported feature tags for a chat
     * 
     * @param rcsSettings the RCS settings accessor
     * @return List of tags
     */
    public static List<String> getSupportedFeatureTagsForChat(RcsSettings rcsSettings) {
        List<String> tags = new ArrayList<>();
        switch (rcsSettings.getImMsgTech()) {
            case CPM:
                tags.add(FeatureTags.FEATURE_3GPP_CPM_SESSION);
                break;
            case SIMPLE_IM:
            default:
                tags.add(FeatureTags.FEATURE_OMA_IM);
                break;
        }
        List<String> additionalRcseTags = new ArrayList<>();
        if (rcsSettings.isGeoLocationPushSupported()) {
            additionalRcseTags.add(FeatureTags.FEATURE_RCSE_GEOLOCATION_PUSH);
        }
        if (rcsSettings.isFileTransferSupported()) {
            additionalRcseTags.add(FeatureTags.FEATURE_RCSE_FT);
        }
        if (rcsSettings.isFileTransferHttpSupported()) {
            additionalRcseTags.add(FeatureTags.FEATURE_RCSE_FT_HTTP);
        }
        if (rcsSettings.isFileTransferStoreForwardSupported()) {
            additionalRcseTags.add(FeatureTags.FEATURE_RCSE_FT_SF);
        }
        if (!additionalRcseTags.isEmpty()) {
            tags.add(FeatureTags.FEATURE_RCSE + "=\"" + TextUtils.join(",", additionalRcseTags)
                    + "\"");
        }
        return tags;
    }

    /**
     * Get feature tag for pager mode
     *
     * @param rcsSettings the RCS settings accessor
     * @return featureTag
     */
    public static String getFeatureTagForPagerMode(RcsSettings rcsSettings) {
        switch (rcsSettings.getImMsgTech()) {
            case CPM:
                return FeatureTags.FEATURE_3GPP_CPM_MSG;
            case SIMPLE_IM:
            default:
                return FeatureTags.FEATURE_OMA_IM;
        }
    }

    /**
     * Get feature tag for large message mode
     *
     * @param rcsSettings the RCS settings accessor
     * @return List of tags
     */
    public static String getFeatureTagForLargeMessageMode(RcsSettings rcsSettings) {
        switch (rcsSettings.getImMsgTech()) {
            case CPM:
                return FeatureTags.FEATURE_3GPP_CPM_LARGE_MSG;
            case SIMPLE_IM:
            default:
                return FeatureTags.FEATURE_OMA_IM;
        }
    }

    /**
     * Get feature tag for session mode
     *
     * @param rcsSettings the RCS settings accessor
     * @return featureTag
     */
    public static String getFeatureTagForSessionMode(RcsSettings rcsSettings) {
        switch (rcsSettings.getImMsgTech()) {
            case CPM:
                return FeatureTags.FEATURE_3GPP_CPM_SESSION;
            case SIMPLE_IM:
            default:
                return FeatureTags.FEATURE_OMA_IM;
        }
    }

    /**
     * Get feature tag for a file transfer
     *
     * @param rcsSettings the RCS settings accessor
     * @return featureTag
     */
    public static String getFeatureTagForFileTransfer(RcsSettings rcsSettings) {
        switch (rcsSettings.getImMsgTech()) {
            case CPM:
                return FeatureTags.FEATURE_3GPP_CPM_FILE_TRANSFER;
            case SIMPLE_IM:
            default:
                return FeatureTags.FEATURE_OMA_IM;
        }
    }

    /**
     * Get conversation ID
     * 
     * @param request the SIP request
     * @return String
     */
    public static String getConversationId(SipRequest request) {
        ExtensionHeader conversHeader = (ExtensionHeader) request.getHeader(HEADER_CONVERSATION_ID);
        if (conversHeader != null) {
            return conversHeader.getValue();
        }
        return null;
    }

    /**
     * Get contribution ID
     * 
     * @param request the SIP request
     * @return String
     */
    public static String getContributionId(SipRequest request) {
        ExtensionHeader contribHeader = (ExtensionHeader) request.getHeader(HEADER_CONTRIBUTION_ID);
        if (contribHeader != null) {
            return contribHeader.getValue();
        }
        return null;
    }

    /**
     * Is a group chat session invitation
     * 
     * @param request Request
     * @return Boolean
     */
    public static boolean isGroupChatInvitation(SipRequest request) {
        ContactHeader contactHeader = (ContactHeader) request.getHeader(ContactHeader.NAME);
        String param = contactHeader.getParameter("isfocus");
        return param != null;
    }

    /**
     * Is a burn-after-reading session request
     *
     * @param request Request
     * @return Boolean
     */
    public static boolean isBarCycle(SipRequest request) {
        return SipUtils.isFeatureTagPresent(request, FeatureTags.Cmcc.FEATURE_BAR_CYCLE);
    }

    /**
     * Get referred identity as a ContactId
     * 
     * @param request SIP request
     * @return ContactId
     */
    public static ContactId getReferredIdentityAsContactId(SipRequest request) {
        /* First use the Referred-By header */
        String referredBy = SipUtils.getReferredByHeader(request);
        PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(referredBy);
        if (number == null) {
            /* Use the Asserted-Identity header if parsing of Referred-By header failed */
            String assertedId = SipUtils.getAssertedIdentity(request);
            number = ContactUtil.getValidPhoneNumberFromUri(assertedId);
            if (number == null) {
                return null;
            }
        }
        return ContactUtil.createContactIdFromValidatedData(number);
    }

    /**
     * Get referred identity as a contact URI
     * 
     * @param request SIP request
     * @return SIP URI
     */
    public static String getReferredIdentityAsContactUri(SipRequest request) {
        String referredBy = SipUtils.getReferredByHeader(request);
        if (referredBy != null) {
            // Use the Referred-By header
            return referredBy;
        }
        // Use the Asserted-Identity header
        return SipUtils.getAssertedIdentity(request);
    }

    /**
     * Is a plain text type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isTextPlainType(String mime) {
        return mime != null && mime.toLowerCase().startsWith(MimeType.TEXT_MESSAGE);
    }

    /**
     * Is a composing event type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isApplicationIsComposingType(String mime) {
        return mime != null && mime.toLowerCase().startsWith(IsComposingInfo.MIME_TYPE);
    }

    /**
     * Is a CPIM message type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isMessageCpimType(String mime) {
        return mime != null && mime.toLowerCase().startsWith(CpimMessage.MIME_TYPE);
    }

    /**
     * Is an IMDN message type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isMessageImdnType(String mime) {
        return mime != null && mime.toLowerCase().startsWith(ImdnDocument.MIME_TYPE);
    }

    /**
     * Is a geolocation event type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isGeolocType(String mime) {
        return mime != null && mime.toLowerCase().startsWith(GeolocInfoDocument.MIME_TYPE);
    }

    /**
     * Is a vemoticon event type
     *
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isVemoticonType(String mime) {
        return mime != null && mime.toLowerCase().startsWith(VemoticonInfoDocument.MIME_TYPE);
    }

    /**
     * Is a cloud file event type
     *
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isCloudFileType(String mime) {
        return mime != null && mime.toLowerCase().startsWith(CloudFileInfoDocument.MIME_TYPE);
    }

    /**
     * Is a xml event type
     *
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isXmlType(String mime) {
        return mime != null && mime.toLowerCase().startsWith("application/xml");
    }

    /**
     * Is a card event type
     *
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isCardType(String mime) {
        return mime != null && mime.toLowerCase().startsWith(CardInfoDocument.MIME_TYPE);
    }

    /**
     * Generate resource-list for a chat session
     * 
     * @param participants Set of participants
     * @return XML document
     */
    public static String generateChatResourceList(Set<ContactId> participants) {
        StringBuilder resources = new StringBuilder("<?xml version=\"1.0\" encoding=\"")
                .append(UTF8_STR).append("\"?>").append(CRLF)
                .append("<resource-lists xmlns=\"urn:ietf:params:xml:ns:resource-lists\" ")
                .append("xmlns:cp=\"urn:ietf:params:xml:ns:copycontrol\">").append("<list>")
                .append(CRLF);
        for (ContactId contact : participants) {
            resources.append(" <entry uri=\"").append(PhoneUtils.formatContactIdToUri(contact))
                    .append("\" cp:copyControl=\"to\"/>").append(CRLF);
        }
        return resources.append("</list></resource-lists>").toString();
    }

    /**
     * Get participants from contacts
     * 
     * @param contacts contacts
     * @param status status
     * @return participants
     */
    public static Map<ContactId, Status> getParticipants(Set<ContactId> contacts, Status status) {
        Map<ContactId, Status> participants = new HashMap<>();
        for (ContactId contact : contacts) {
            participants.put(contact, status);
        }
        return participants;
    }

    /**
     * Is IMDN service
     * 
     * @param request Request
     * @return Boolean
     */
    public static boolean isImdnService(SipRequest request) {
        String content = request.getContent();
        String contentType = request.getContentType();
        return (content != null) && (content.contains(ImdnDocument.IMDN_NAMESPACE))
                && (contentType != null) && (contentType.equalsIgnoreCase(CpimMessage.MIME_TYPE));
    }

    private static boolean isImdnNotifRequested(SipRequest request, String report)
            throws PayloadException {
        /* Read ID from multipart content */
        String content = request.getContent();
        SipUtils.assertContentIsNotNull(content, request);
        int index = content.indexOf(ImdnUtils.HEADER_IMDN_DISPO_NOTIF);
        if (index == -1) {
            return false;
        }
        index = index + ImdnUtils.HEADER_IMDN_DISPO_NOTIF.length() + 1;
        String part = content.substring(index);
        String notif = part.substring(0, part.indexOf(CRLF));
        return notif.indexOf(report) != -1;
    }

    /**
     * Is IMDN notification "displayed" requested
     * 
     * @param request Request
     * @return Boolean
     * @throws PayloadException
     */
    public static boolean isImdnDisplayedRequested(SipRequest request) throws PayloadException {
        return isImdnNotifRequested(request, ImdnDocument.DISPLAY);
    }

    /**
     * Returns the message ID from a SIP request
     * 
     * @param request Request
     * @return Message ID
     * @throws PayloadException
     */
    public static String getMessageId(SipRequest request) throws PayloadException {
        /* Read ID from multipart content */
        String content = request.getContent();
        SipUtils.assertContentIsNotNull(content, request);
        int index = content.indexOf(ImdnUtils.HEADER_IMDN_MSG_ID);
        if (index == -1) {
            return null;
        }
        index = index + ImdnUtils.HEADER_IMDN_MSG_ID.length() + 1;
        String part = content.substring(index);
        return part.substring(0, part.indexOf(CRLF)).trim();
    }

    // @FIXME: This method should have URI as parameter instead of String
    private static String addUriDelimiters(String uri) {
        return PhoneUtils.URI_START_DELIMITER + uri + PhoneUtils.URI_END_DELIMITER;
    }

    /**
     * Format to a SIP-URI for CPIM message
     * 
     * @param input Input
     * @return SIP-URI
     */
    private static String formatCpimSipUri(String input) {
        input = input.trim();
        if (input.startsWith(PhoneUtils.URI_START_DELIMITER)) {
            /* Already a SIP-URI format */
            return input;
        }
        /* It's already a SIP address with display name */
        if (input.startsWith("\"")) {
            return input;
        }
        if (input.startsWith(PhoneUtils.SIP_URI_HEADER)
                || input.startsWith(PhoneUtils.TEL_URI_HEADER)) {
            /* It is already a SIP or TEL URI: just add URI delimiters */
            return addUriDelimiters(input);
        }
        PhoneNumber validatedNumber = ContactUtil.getValidPhoneNumberFromUri(input);
        if (validatedNumber == null) {
            /* It's not a valid phone number: just add URI delimiters */
            return addUriDelimiters(input);
        }
        /* It's a valid phone number, format it to URI and add delimiters */
        ContactId contact = ContactUtil.createContactIdFromValidatedData(validatedNumber);
        // @FIXME: This method should have URI as parameter instead of String
        return addUriDelimiters(PhoneUtils.formatContactIdToUri(contact).toString());
    }

    /**
     * Build a CPIM message
     * 
     * @param from From
     * @param to To
     * @param content Content
     * @param contentType Content type
     * @param timestampSent Timestamp sent in payload for CPIM DateTimes
     * @return String
     */
    public static String buildCpimMessage(String from, String to, String content,
            String contentType, long timestampSent) {
        return CpimMessage.HEADER_FROM + ": " + formatCpimSipUri(from) + CRLF
                + CpimMessage.HEADER_TO + ": " + formatCpimSipUri(to) + CRLF
                + CpimMessage.HEADER_DATETIME + ": " + DateUtils.encodeDate(timestampSent) + CRLF
                + CRLF + CpimMessage.HEADER_CONTENT_TYPE + ": " + contentType + ";charset="
                + UTF8_STR + CRLF + CRLF + content;
    }

    /**
     * Build a CPIM message with full IMDN headers
     * 
     * @param from From URI
     * @param to To URI
     * @param messageId Message ID
     * @param content Content
     * @param contentType Content type
     * @param timestampSent Timestamp sent in payload for CPIM DateTime
     * @return String
     */
    public static String buildCpimMessageWithImdn(String from, String to, String messageId,
            String content, String contentType, long timestampSent) {
        return CpimMessage.HEADER_FROM + ": " + formatCpimSipUri(from) + CRLF
                + CpimMessage.HEADER_TO + ": " + formatCpimSipUri(to) + CRLF
                + CpimMessage.HEADER_NS + ": " + ImdnDocument.IMDN_NAMESPACE + CRLF
                + ImdnUtils.HEADER_IMDN_MSG_ID + ": " + messageId + CRLF
                + CpimMessage.HEADER_DATETIME + ": " + DateUtils.encodeDate(timestampSent) + CRLF
                + ImdnUtils.HEADER_IMDN_DISPO_NOTIF + ": " + ImdnDocument.POSITIVE_DELIVERY + ", "
                + ImdnDocument.DISPLAY + CRLF + CRLF + CpimMessage.HEADER_CONTENT_TYPE + ": "
                + contentType + ";charset=" + UTF8_STR + CRLF + CpimMessage.HEADER_CONTENT_LENGTH
                + ": " + content.getBytes(UTF8).length + CRLF + CRLF + content;
    }

    /**
     * Build a CPIM message with IMDN delivered header
     * 
     * @param from From URI
     * @param to To URI
     * @param messageId Message ID
     * @param content Content
     * @param contentType Content type
     * @param timestampSent Timestamp sent in payload for CPIM DateTime
     * @return String
     */
    public static String buildCpimMessageWithoutDisplayedImdn(String from, String to,
            String messageId, String content, String contentType, long timestampSent) {
        return CpimMessage.HEADER_FROM + ": " + formatCpimSipUri(from) + CRLF
                + CpimMessage.HEADER_TO + ": " + formatCpimSipUri(to) + CRLF
                + CpimMessage.HEADER_NS + ": " + ImdnDocument.IMDN_NAMESPACE + CRLF
                + ImdnUtils.HEADER_IMDN_MSG_ID + ": " + messageId + CRLF
                + CpimMessage.HEADER_DATETIME + ": " + DateUtils.encodeDate(timestampSent) + CRLF
                + ImdnUtils.HEADER_IMDN_DISPO_NOTIF + ": " + ImdnDocument.POSITIVE_DELIVERY + CRLF
                + CRLF + CpimMessage.HEADER_CONTENT_TYPE + ": " + contentType + ";charset="
                + UTF8_STR + CRLF + CpimMessage.HEADER_CONTENT_LENGTH + ": "
                + content.getBytes(UTF8).length + CRLF + CRLF + content;
    }

    public static String buildCpimMessageWithImdnAndCc(String from, String to, Set<ContactId> ccRecipients,
            String messageId, String content, String contentType, long timestampSent) {
        String base64Content = content; //Base64.encodeBase64ToString(content.getBytes(UTF8));
        StringBuilder cpimBuilder = new StringBuilder(CpimMessage.HEADER_FROM).append(": ")
                .append(formatCpimSipUri(from)).append(CRLF).append(CpimMessage.HEADER_TO)
                .append(": ").append(formatCpimSipUri(to)).append(CRLF);
        if (ccRecipients != null) {
            for (ContactId contact : ccRecipients) {
                cpimBuilder.append(CpimMessage.HEADER_CC).append(": ")
                        .append(formatCpimSipUri(contact.toString())).append(CRLF);
            }
        }
        cpimBuilder.append(CpimMessage.HEADER_NS).append(": ").append(ImdnDocument.IMDN_NAMESPACE)
                .append(CRLF).append(CpimMessage.HEADER_DATETIME).append(": ")
                .append(DateUtils.encodeDate(timestampSent)).append(CRLF)
                .append(ImdnUtils.HEADER_IMDN_MSG_ID).append(": ").append(messageId).append(CRLF)
                .append(ImdnUtils.HEADER_IMDN_DISPO_NOTIF).append(": ")
                .append(ImdnDocument.NEGATIVE_DELIVERY).append(", ")
                .append(ImdnDocument.POSITIVE_DELIVERY).append(CRLF).append(CRLF)
                .append(CpimMessage.HEADER_CONTENT_TYPE).append(": ").append(contentType)
                .append(";charset=").append(UTF8_STR).append(CRLF)
                .append(CpimMessage.HEADER_CONTENT_LENGTH).append(": ")
                .append(base64Content.getBytes(UTF8).length).append(CRLF)
                .append(CpimMessage.HEADER_CONTENT_TRANSFER_ENCODING).append(": ").append("base64")
                .append(CRLF).append(CRLF).append(base64Content);
        return cpimBuilder.toString();
    }

    /**
     * Build a CPIM delivery report
     * 
     * @param from From
     * @param to To
     * @param imdn IMDN report
     * @param timestampSent Timestamp sent in payload for CPIM DateTime
     * @return String
     */
    public static String buildCpimDeliveryReport(String from, String to, String imdn,
            long timestampSent) {
        return CpimMessage.HEADER_FROM + ": " + formatCpimSipUri(from) + CRLF
                + CpimMessage.HEADER_TO + ": " + formatCpimSipUri(to) + CRLF
                + CpimMessage.HEADER_NS + ": " + ImdnDocument.IMDN_NAMESPACE + CRLF
                + ImdnUtils.HEADER_IMDN_MSG_ID + ": " + IdGenerator.generateMessageID() + CRLF
                + CpimMessage.HEADER_DATETIME + ": " + DateUtils.encodeDate(timestampSent) + CRLF
                + CRLF + CpimMessage.HEADER_CONTENT_TYPE + ": " + ImdnDocument.MIME_TYPE + CRLF
                + CpimMessage.HEADER_CONTENT_DISPOSITION + ": " + ImdnDocument.NOTIFICATION + CRLF
                + CpimMessage.HEADER_CONTENT_LENGTH + ": " + imdn.getBytes(UTF8).length + CRLF
                + CRLF + imdn;
    }

    /**
     * Parse a CPIM delivery report
     * 
     * @param cpim CPIM document
     * @return IMDN document
     * @throws ParseFailureException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public static ImdnDocument parseCpimDeliveryReport(String cpim) throws SAXException,
            ParserConfigurationException, ParseFailureException {
        ImdnDocument imdn = null;
        // Parse CPIM document
        CpimParser cpimParser = new CpimParser(cpim);
        CpimMessage cpimMsg = cpimParser.getCpimMessage();
        if (cpimMsg != null) {
            // Check if the content is a IMDN message
            String contentType = cpimMsg.getContentType();
            if ((contentType != null) && isMessageImdnType(contentType)) {
                // Parse the IMDN document
                imdn = parseDeliveryReport(cpimMsg.getMessageContent());
            }
        }
        return imdn;
    }

    /**
     * Parse a delivery report
     * 
     * @param xml XML document
     * @return IMDN document
     * @throws ParseFailureException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public static ImdnDocument parseDeliveryReport(String xml) throws SAXException,
            ParserConfigurationException, ParseFailureException {
        InputSource input = new InputSource(new ByteArrayInputStream(xml.getBytes()));
        ImdnParser parser = new ImdnParser(input).parse();
        return parser.getImdnDocument();
    }

    /**
     * Build a delivery report
     * 
     * @param msgId Message ID
     * @param status Status
     * @param timestamp Timestamp sent in payload for IMDN datetime
     * @return XML document
     */
    public static String buildImdnDeliveryReport(String msgId, ImdnDocument.DeliveryStatus status,
            long timestamp) {
        String method;
        switch (status) {
            case DISPLAYED:
                method = "display-notification";
                break;
            case DELIVERED:
                method = "delivery-notification";
                break;
            default:
                method = "processing-notification";
                break;
        }
        return "<?xml version=\"1.0\" encoding=\"" + UTF8_STR + "\"?>" + CRLF
                + "<imdn xmlns=\"urn:ietf:params:xml:ns:imdn\">" + CRLF + "<message-id>" + msgId
                + "</message-id>" + CRLF + "<datetime>" + DateUtils.encodeDate(timestamp)
                + "</datetime>" + CRLF + "<" + method + "><status><" + status + "/></status></"
                + method + ">" + CRLF + "</imdn>";
    }

    /**
     * Build a geoloc document
     * 
     * @param geoloc Geolocation
     * @param contact Contact
     * @param msgId Message ID
     * @param timestamp Local timestamp
     * @return XML document
     */
    public static String buildGeolocDocument(Geoloc geoloc, String contact, String msgId,
            long timestamp) {
        String expire = DateUtils.encodeDate(geoloc.getExpiration());
        String label = geoloc.getLabel();
        if (label == null) {
            label = "";
        }
        return "<?xml version=\"1.0\" encoding=\"" + UTF8_STR + "\"?>" + CRLF
                + "<rcsenvelope xmlns=\"urn:gsma:params:xml:ns:rcs:rcs:geolocation\""
                + " xmlns:rpid=\"urn:ietf:params:xml:ns:pidf:rpid\""
                + " xmlns:gp=\"urn:ietf:params:xml:ns:pidf:geopriv10\""
                + " xmlns:gml=\"http://www.opengis.net/gml\""
                + " xmlns:gs=\"http://www.opengis.net/pidflo/1.0\"" + " entity=\"" + contact
                + "\">" + CRLF + "<rcspushlocation id=\"" + msgId + "\" label=\"" + label + "\" >"
                + "<rpid:place-type rpid:until=\"" + expire + "\">" + "</rpid:place-type>" + CRLF
                + "<rpid:time-offset rpid:until=\"" + expire + "\"></rpid:time-offset>" + CRLF
                + "<gp:geopriv>" + CRLF + "<gp:location-info>" + CRLF
                + "<gs:Circle srsName=\"urn:ogc:def:crs:EPSG::4326\">" + CRLF + "<gml:pos>"
                + geoloc.getLatitude() + " " + geoloc.getLongitude() + "</gml:pos>" + CRLF
                + "<gs:radius uom=\"urn:ogc:def:uom:EPSG::9001\">" + geoloc.getAccuracy()
                + "</gs:radius>" + CRLF + "</gs:Circle>" + CRLF + "</gp:location-info>" + CRLF
                + "<gp:usage-rules>" + CRLF + "<gp:retention-expiry>" + expire
                + "</gp:retention-expiry>" + CRLF + "</gp:usage-rules>" + CRLF + "</gp:geopriv>"
                + CRLF + "<timestamp>" + DateUtils.encodeDate(timestamp) + "</timestamp>" + CRLF
                + "</rcspushlocation>" + CRLF + "</rcsenvelope>" + CRLF;
    }

    /**
     * Parse a geoloc document
     * 
     * @param xml XML document
     * @return Geolocation
     * @throws PayloadException
     */
    public static Geoloc parseGeolocDocument(String xml) throws PayloadException {
        try {
            InputSource geolocInput = new InputSource(new ByteArrayInputStream(xml.getBytes(UTF8)));
            GeolocInfoParser geolocParser = new GeolocInfoParser(geolocInput).parse();
            GeolocInfoDocument geolocDocument = geolocParser.getGeoLocInfo();
            if (geolocDocument == null) {
                throw new PayloadException("Unable to parse geoloc document!");
            }
            return new Geoloc(geolocDocument.getLabel(), geolocDocument.getLatitude(),
                    geolocDocument.getLongitude(), geolocDocument.getExpiration(),
                    geolocDocument.getRadius());

        } catch (ParserConfigurationException | ParseFailureException | SAXException e) {
            throw new PayloadException("Unable to parse geoloc document!", e);
        }
    }

    /**
     * Parse a file transfer resume info
     * 
     * @param xml XML document
     * @return File transfer resume info
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public static FileTransferHttpResumeInfo parseFileTransferHttpResumeInfo(byte[] xml)
            throws ParserConfigurationException, SAXException, IOException {
        return new FileTransferHttpResumeInfoParser(new InputSource(new ByteArrayInputStream(xml)))
                .getResumeInfo();
    }

    /**
     * Build a vemoticon document
     *
     * @param emoticon Vemoticon
     * @return XML document
     */
    public static String buildVemoticonDocument(Emoticon emoticon) {
        // @formatter:off
        return "<?xml version=\"1.0\" encoding=\"" + UTF8_STR + "\"?>" + CRLF
                + "<vemoticon xmlns=\"http://vemoticon.bj.ims.mnc000.mcc460.3gppnetwork.org/types\">" + CRLF
                + "<sms>" + emoticon.getSms() + "</sms>" + CRLF
                + "<eid>" + emoticon.getEid() + "</eid>" + CRLF
                + "</vemoticon>" + CRLF;
        // @formatter:on
    }

    /**
     * Parse a vemoticon document
     *
     * @param xml XML document
     * @return Vemoticon
     * @throws PayloadException
     */
    public static Emoticon parseVemoticonDocument(String xml) throws PayloadException {
        try {
            InputSource vemoticonInput = new InputSource(new ByteArrayInputStream(
                    xml.getBytes(UTF8)));
            VemoticonInfoParser vemoticonParser = new VemoticonInfoParser(vemoticonInput).parse();
            VemoticonInfoDocument vemoticonDocument = vemoticonParser.getVemoticonInfo();
            if (vemoticonDocument == null) {
                throw new PayloadException("Unable to parse vemoticon document!");
            }
            return new Emoticon(vemoticonDocument.getSms(), vemoticonDocument.getEid());

        } catch (ParserConfigurationException | ParseFailureException | SAXException e) {
            throw new PayloadException("Unable to parse vemoticon document!", e);
        }
    }

    /**
     * Build a cloudFile document
     *
     * @param cloudFile CloudFile
     * @return XML document
     */
    public static String buildCloudFileDocument(CloudFile cloudFile) {
        // @formatter:off
        return "<?xml version=\"1.0\" encoding=\"" + UTF8_STR + "\"?>" + CRLF
                + "<cloudfile xmlns=\"http://cloudfile.cmcc.com/types\">" + CRLF
                + "<filename>" + cloudFile.getFileName() + "</filename>" + CRLF
                + "<filesize>" + cloudFile.getFileSize() + "</filesize>" + CRLF
                + "<downloadurl>" + cloudFile.getDownloadUrl() + "</downloadurl>" + CRLF
                + "</cloudfile>" + CRLF;
        // @formatter:on
    }

    /**
     * Parse a cloudFile document
     *
     * @param xml XML document
     * @return CloudFile
     * @throws PayloadException
     */
    public static CloudFile parseCloudFileDocument(String xml) throws PayloadException {
        try {
            InputSource cloudFileInput = new InputSource(new ByteArrayInputStream(
                    xml.getBytes(UTF8)));
            CloudFileInfoParser cloudFileParser = new CloudFileInfoParser(cloudFileInput).parse();
            CloudFileInfoDocument cloudFileDocument = cloudFileParser.getCouldFileInfo();
            if (cloudFileDocument == null) {
                throw new PayloadException("Unable to parse cloud file document!");
            }
            return new CloudFile(cloudFileDocument.getFilename(), cloudFileDocument.getFilesize(),
                    cloudFileDocument.getDownloadurl());

        } catch (ParserConfigurationException | ParseFailureException | SAXException e) {
            throw new PayloadException("Unable to parse cloud file document!", e);
        }
    }

    /**
     * Build a card document
     *
     * @param card Card
     * @return XML document
     */
    public static String buildCardDocument(Card card) {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"").append(UTF8_STR).append("\"?>")
                .append(CRLF)
                .append("<resource-lists xmlns=\"urn:ietf:params:xml:ns:resource-lists\"")
                .append(CRLF).append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")
                .append(CRLF).append("xmlns:cp=\"urn:ietf:params:xml:ns:capacity\">").append(CRLF)
                .append("<msg_content>").append(CRLF).append("<media_type>")
                .append(card.getMediaType()).append("</media_type>").append(CRLF);
        if (card.getOperationType() != null) {
            builder.append("<operation_type>").append(card.getOperationType())
                    .append("</operation_type>").append(CRLF);
        }
        if (card.getCreateTime() != null) {
            builder.append("<create_time>").append(card.getCreateTime()).append("</create_time>")
                    .append(CRLF);
        }
        // TODO not full realized
        Card.MediaArticle article = card.getCard();
        if (article != null) {
            builder.append("<card>").append(CRLF);
            String value = article.getTitle();
            if (value != null) {
                builder.append("<title>").append(value).append("</title>").append(CRLF);
            }
            value = article.getAuthor();
            if (value != null) {
                builder.append("<author>").append(value).append("</author>").append(CRLF);
            }
            value = article.getThumbLink();
            if (value != null) {
                builder.append("<thumb_link>").append(value).append("</thumb_link>").append(CRLF);
            }
            value = article.getOriginalLink();
            if (value != null) {
                builder.append("<original_link>").append(value).append("</original_link>")
                        .append(CRLF);
            }
            value = article.getBodyLink();
            if (value != null) {
                builder.append("<body_link>").append(value).append("</body_link>").append(CRLF);
            }
            value = article.getSourceLink();
            if (value != null) {
                builder.append("<source_link>").append(value).append("</source_link>").append(CRLF);
            }
            value = article.getMainText();
            if (value != null) {
                builder.append("<main_text>").append(value).append("</main_text>").append(CRLF);
            }
            value = article.getMediaUuid();
            if (value != null) {
                builder.append("<media_uuid>").append(value).append("</media_uuid>").append(CRLF);
            }
            builder.append("</card>").append(CRLF);
        }
        builder.append("</msg_content>").append(CRLF);
        return builder.toString();
    }

    /**
     * Parse a card document
     *
     * @param xml XML document
     * @return Card
     * @throws PayloadException
     */
    public static Card parseCardDocument(String xml) throws PayloadException {
        try {
            InputSource cardInput = new InputSource(new ByteArrayInputStream(xml.getBytes(UTF8)));
            CardInfoParser cardParser = new CardInfoParser(cardInput).parse();
            CardInfoDocument cardDocument = cardParser.getCardInfo();
            if (cardDocument == null) {
                throw new PayloadException("Unable to parse card document!");
            }
            MediaArticle article = cardDocument.getCard();
            Card.MediaArticle card = new Card.MediaArticle(article.getTitle());
            return new Card(cardDocument.getMediaType(), cardDocument.getOperationType(),
                    cardDocument.getCreateTime(), cardDocument.getPaUuid(),
                    cardDocument.getForwardable(), cardDocument.getTrustLevel(),
                    cardDocument.getAccessId(), cardDocument.getAuthLevel(),
                    cardDocument.getVersion(), cardDocument.getDisplayStyle(),
                    cardDocument.getDefaultText(), cardDocument.getDefaultLink(), card);
        } catch (ParserConfigurationException | ParseFailureException | SAXException e) {
            throw new PayloadException("Unable to parse card document!", e);
        }
    }

    /**
     * Create a text message
     * 
     * @param remote Remote contact identifier
     * @param msg Text message
     * @param timestamp Local timestamp
     * @param timestampSent Timestamp sent in payload
     * @return Text message
     */
    public static ChatMessage createTextMessage(ContactId remote, String msg, long timestamp,
            long timestampSent) {
        String msgId = IdGenerator.generateMessageID();
        return new ChatMessage(msgId, remote, msg, MimeType.TEXT_MESSAGE, timestamp, timestampSent,
                null);
    }

    /**
     * Create a file transfer message
     * 
     * @param remote Remote contact identifier
     * @param fileInfo File XML description
     * @param msgId Message ID
     * @param timestamp The local timestamp for the file transfer
     * @param timestampSent The timestamp sent in payload for the file transfer
     * @return File message
     */
    public static ChatMessage createFileTransferMessage(ContactId remote, String fileInfo,
            String msgId, long timestamp, long timestampSent) {
        return new ChatMessage(msgId, remote, fileInfo, FileTransferHttpInfoDocument.MIME_TYPE,
                timestamp, timestampSent, null);
    }

    /**
     * Create a geoloc message
     * 
     * @param remote Remote contact
     * @param geoloc Geolocation
     * @param timestamp Local timestamp
     * @param timestampSent Timestamp sent in payload
     * @return Geolocation message
     */
    public static ChatMessage createGeolocMessage(ContactId remote, Geoloc geoloc, long timestamp,
            long timestampSent) {
        String msgId = IdGenerator.generateMessageID();
        return new ChatMessage(msgId, remote, geoloc.toString(), MimeType.GEOLOC_MESSAGE,
                timestamp, timestampSent, null);
    }

    /**
     * Create a vemoticon message
     *
     * @param remote Remote contact
     * @param vemoticon Vemoticon
     * @param timestamp Local timestamp
     * @param timestampSent Timestamp sent in payload
     * @return Vemoticon message
     */
    public static ChatMessage createEmoticonMessage(ContactId remote, Emoticon vemoticon,
            long timestamp, long timestampSent) {
        String msgId = IdGenerator.generateMessageID();
        String content = ChatUtils.buildVemoticonDocument(vemoticon);
        return new ChatMessage(msgId, remote, content, MimeType.VEMOTICON_MESSAGE, timestamp,
                timestampSent, null);
    }

    /**
     * Create a cloudFile message
     *
     * @param remote Remote contact
     * @param cloudFile CloudFile
     * @param timestamp Local timestamp
     * @param timestampSent Timestamp sent in payload
     * @return CloudFile message
     */
    public static ChatMessage createCloudFileMessage(ContactId remote, CloudFile cloudFile,
            long timestamp, long timestampSent) {
        String msgId = IdGenerator.generateMessageID();
        String content = ChatUtils.buildCloudFileDocument(cloudFile);
        return new ChatMessage(msgId, remote, content, MimeType.CLOUDFILE_MESSAGE, timestamp,
                timestampSent, null);
    }

    /**
     * Create a card message
     *
     * @param remote Remote contact
     * @param card Card
     * @param timestamp Local timestamp
     * @param timestampSent Timestamp sent in payload
     * @return Card message
     */
    public static ChatMessage createCardMessage(ContactId remote, Card card, long timestamp,
            long timestampSent) {
        String msgId = IdGenerator.generateMessageID();
        String content = ChatUtils.buildCardDocument(card);
        return new ChatMessage(msgId, remote, content, MimeType.CARD_MESSAGE, timestamp,
                timestampSent, null);
    }

    /**
     * Create a cmRedBag message
     *
     * @param remote Remote contact
     * @param cmRedBag CmRedBag
     * @param timestamp Local timestamp
     * @param timestampSent Timestamp sent in payload
     * @return CmRedBag message
     */
//    public static ChatMessage createCmRedBagMessage(ContactId remote, CmRedBag cmRedBag,
//            long timestamp, long timestampSent) {
//        String msgId = IdGenerator.generateMessageID();
//        String content = ChatUtils.buildCmRedBagDocument(cmRedBag);
//        return new ChatMessage(msgId, remote, content, MimeType.CMREDBAG_MESSAGE, timestamp,
//                timestampSent, null);
//    }

    /**
     * Get the first message
     * 
     * @param invite Request
     * @param timestamp Local timestamp
     * @return First message
     * @throws PayloadException
     */
    public static ChatMessage getFirstMessage(SipRequest invite, long timestamp)
            throws PayloadException {
        ChatMessage msg = getFirstMessageFromCpim(invite, timestamp);
        if (msg != null) {
            return msg;
        }
        return getFirstMessageFromSubject(invite, timestamp);
    }

    public static boolean isContainingFirstMessage(SipRequest invite) {
        return invite.getContent() != null || invite.getSubject().length() > 0;
    }

    /**
     * Get the subject
     * 
     * @param invite Request
     * @return String
     */
    public static String getSubject(SipRequest invite) {
        return invite.getSubject();
    }

    /**
     * Get the first message from CPIM content
     * 
     * @param invite Request
     * @param timestamp Local timestamp
     * @return First message
     * @throws PayloadException
     */
    private static ChatMessage getFirstMessageFromCpim(SipRequest invite, long timestamp)
            throws PayloadException {
        CpimMessage cpimMsg = extractCpimMessage(invite);
        if (cpimMsg == null) {
            return null;
        }
        ContactId remote = getReferredIdentityAsContactId(invite);
        if (remote == null) {
            if (sLogger.isActivated()) {
                sLogger.warn("getFirstMessageFromCpim: cannot parse contact");
            }
            return null;
        }
        String msgId = getMessageId(invite);
        String content = cpimMsg.getMessageContent();
        long timestampSent = cpimMsg.getTimestampSent();
        String mime = cpimMsg.getContentType();
        if (msgId == null || content == null || mime == null) {
            return null;
        }
        if (isGeolocType(mime)) {
            return new ChatMessage(msgId, remote,
                    ChatUtils.networkGeolocContentToPersistedGeolocContent(content),
                    MimeType.GEOLOC_MESSAGE, timestamp, timestampSent, null);

        } else if (FileTransferUtils.isFileTransferHttpType(mime)) {
            return new ChatMessage(msgId, remote, content, FileTransferHttpInfoDocument.MIME_TYPE,
                    timestamp, timestampSent, null);

        } else if (isTextPlainType(mime)) {
            return new ChatMessage(msgId, remote, content, MimeType.TEXT_MESSAGE, timestamp,
                    timestampSent, null);
        }
        sLogger.warn("Unknown MIME-type in first message; msgId=" + msgId + ", mime='" + mime
                + "'.");
        return null;
    }

    /**
     * Get the first message from the Subject header
     * 
     * @param invite Request
     * @param timestamp Local timestamp
     * @return First message
     * @throws PayloadException
     */
    private static ChatMessage getFirstMessageFromSubject(SipRequest invite, long timestamp)
            throws PayloadException {
        String subject = invite.getSubject();
        if (TextUtils.isEmpty(subject)) {
            return null;
        }
        ContactId remote = getReferredIdentityAsContactId(invite);
        if (remote == null) {
            throw new PayloadException("Cannot parse contact from message subject!");
        }
        /**
         * Since in subject, there is no DateTime or datetime included, then we need to fake that by
         * using the local timestamp even if this is not the real proper timestamp from the remote
         * side in this case.
         */
        return new ChatMessage(IdGenerator.generateMessageID(), remote, subject,
                MimeType.TEXT_MESSAGE, timestamp, timestamp, null);
    }

    /**
     * Extract CPIM message from incoming INVITE request
     * 
     * @param request Request
     * @return CpimMessage
     */
    public static CpimMessage extractCpimMessage(SipRequest request) {
        Multipart multi = new Multipart(request.getContent(), request.getBoundaryContentType());
        if (!multi.isMultipart()) {
            return null;
        }
        String cpimPart = multi.getPart(CpimMessage.MIME_TYPE);
        if (cpimPart == null) {
            return null;
        }
        return new CpimParser(cpimPart.getBytes(UTF8)).getCpimMessage();
    }

    /**
     * Get participants from 'resource-list' present in XML document and include the 'remote' as
     * participant.
     * 
     * @param request Request
     * @param status Status to assign to created participants
     * @return Participants based on contacts in the request
     * @throws PayloadException
     */
    public static Map<ContactId, Status> getParticipants(SipRequest request, Status status)
            throws PayloadException {
        Map<ContactId, Status> participants = new HashMap<>();
        String content = request.getContent();
        String boundary = request.getBoundaryContentType();
        Multipart multi = new Multipart(content, boundary);
        if (multi.isMultipart()) {
            String listPart = multi.getPart("application/resource-lists+xml");
            if (listPart != null) {
                participants = ParticipantInfoUtils.parseResourceList(listPart, status);
                ContactId remote = getReferredIdentityAsContactId(request);
                if (remote == null) {
                    if (sLogger.isActivated()) {
                        sLogger.warn("getParticipants: cannot parse contact");
                    }
                } else {
                    /* Include remote contact if format if correct */
                    if (!remote.equals(ImsModule.getImsUserProfile().getUsername())) {
                        participants.put(remote, status);
                    }
                }
            }
        }
        return participants;
    }

    /**
     * Is request is for FToHTTP
     * 
     * @param request SIP request
     * @return true if FToHTTP
     */
    public static boolean isFileTransferOverHttp(SipRequest request) {
        CpimMessage message = extractCpimMessage(request);
        if (message == null) {
            return false;
        }
        String contentType = message.getContentType();
        return contentType != null
                && contentType.startsWith(FileTransferHttpInfoDocument.MIME_TYPE);
    }

    /**
     * Generate persisted MIME-type from network pay-load
     * 
     * @param apiMimeType Network pay-load MIME-type
     * @return API MIME-type
     */
    public static String apiMimeTypeToNetworkMimeType(String apiMimeType) {
        /*
         * Geolocation chat messages does not have the same mimetype in the payload as in the TAPI.
         * Text chat messages do.
         */
        if (apiMimeType.startsWith(MimeType.GEOLOC_MESSAGE)) {
            return GeolocInfoDocument.MIME_TYPE;
        } else if (isTextPlainType(apiMimeType)) {
            return MimeType.TEXT_MESSAGE;
        } else if (apiMimeType.startsWith(FileTransferHttpInfoDocument.MIME_TYPE)) {
            return FileTransferHttpInfoDocument.MIME_TYPE;
        }
        throw new IllegalArgumentException("Unsupported input mimetype detected : " + apiMimeType);
    }

    /**
     * Generate persisted content from network pay-load
     * 
     * @param content the content from network payload
     * @return Persisted content
     * @throws PayloadException
     */
    public static String networkGeolocContentToPersistedGeolocContent(String content)
            throws PayloadException {
        Geoloc geoloc = parseGeolocDocument(content);
        return geoloc.toString();
    }

    /**
     * Generate network pay-load content from persisted content
     * 
     * @param content the persisted content
     * @param msgId the message ID
     * @param timestamp the timestamp
     * @return Network payload content
     */
    public static String persistedGeolocContentToNetworkGeolocContent(String content, String msgId,
            long timestamp) {
        Geoloc geoloc = new Geoloc(content);
        return ChatUtils.buildGeolocDocument(geoloc, ImsModule.getImsUserProfile().getPublicUri(),
                msgId, timestamp);
    }

    /**
     * Create a chat message either text or geolocation.
     * 
     * @param msgId the message ID
     * @param apiMimeType The mime-type as exposed to client applications
     * @param content the content
     * @param contact The remote contact
     * @param displayName the display name
     * @param timestamp the timestamp
     * @param timestampSent The timestamp sent in payload for the chat message
     * @return ChatMessage
     */
    public static ChatMessage createChatMessage(String msgId, String apiMimeType, String content,
            ContactId contact, String displayName, long timestamp, long timestampSent) {
        if (MimeType.TEXT_MESSAGE.equals(apiMimeType)) {
            return new ChatMessage(msgId, contact, content, MimeType.TEXT_MESSAGE, timestamp,
                    timestampSent, displayName);

        } else if (MimeType.GEOLOC_MESSAGE.equals(apiMimeType)) {
            return new ChatMessage(msgId, contact, content, MimeType.GEOLOC_MESSAGE, timestamp,
                    timestampSent, displayName);
        }
        throw new IllegalArgumentException("Unable to create message, Invalid mimetype "
                + apiMimeType);
    }

}

/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.core.ims.network.sip;

/**
 * Feature tags
 * 
 * @author jexa7410
 * @author yplo6403
 */
public class FeatureTags {
    /**
     * OMA IM feature tag
     */
    public final static String FEATURE_OMA_IM = "+g.oma.sip-im";

    /**
     * OMA CPM pager mode standalone message feature tag
     */
    public final static String FEATURE_OMA_CPM_MSG = "urn%3Aurn-7%3A3gpp-service.ims.icsi.oma.cpm.msg";

    /**
     * OMA CPM pager mode group standalone message feature tag
     */
    public final static String FEATURE_OMA_CPM_MSG_GROUP = "urn%3Aurn-7%3A3gpp-service.ims.icsi.oma.cpm.msg.group";

    /**
     * OMA CPM large message mode standalone message feature tag
     */
    public final static String FEATURE_OMA_CPM_LARGE_MSG = "urn%3Aurn-7%3A3gpp-service.ims.icsi.oma.cpm.largemsg";

    /**
     * OMA CPM large message mode group standalone message feature tag
     */
    public final static String FEATURE_OMA_CPM_LARGE_MSG_GROUP = "urn%3Aurn-7%3A3gpp-service.ims.icsi.oma.cpm.largemsg.group";

    /**
     * OMA CPM session feature tag
     */
    public final static String FEATURE_OMA_CPM_SESSION = "urn%3Aurn-7%3A3gpp-service.ims.icsi.oma.cpm.session";

    /**
     * OMA CPM group session feature tag
     */
    public final static String FEATURE_OMA_CPM_SESSION_GROUP = "urn%3Aurn-7%3A3gpp-service.ims.icsi.oma.cpm.session.group";

    /**
     * OMA CPM file transfer feature tag
     */
    public final static String FEATURE_OMA_CPM_FILE_TRANSFER = "urn%3Aurn-7%3A3gpp-service.ims.icsi.oma.cpm.filetransfer";

    /**
     * OMA CPM group file transfer feature tag
     */
    public final static String FEATURE_OMA_CPM_FILE_TRANSFER_GROUP = "urn%3Aurn-7%3A3gpp-service.ims.icsi.oma.cpm.filetransfer.group";

    /**
     * 3GPP video share feature tag
     */
    public final static String FEATURE_3GPP_VIDEO_SHARE = "+g.3gpp.cs-voice";

    /**
     * 3GPP CPM pager mode standalone message feature tag
     */
    public final static String FEATURE_3GPP_CPM_MSG = "+g.3gpp.icsi-ref=\"urn%3Aurn-7%3A3gpp-service.ims.icsi.oma.cpm.msg\"";

    /**
     * 3GPP CPM large message mode standalone message feature tag
     */
    public final static String FEATURE_3GPP_CPM_LARGE_MSG = "+g.3gpp.icsi-ref=\"urn%3Aurn-7%3A3gpp-service.ims.icsi.oma.cpm.largemsg\"";

    /**
     * 3GPP CPM session mode standalone message feature tag
     */
    public final static String FEATURE_3GPP_CPM_SESSION = "+g.3gpp.icsi-ref=\"urn%3Aurn-7%3A3gpp-service.ims.icsi.oma.cpm.session\"";

    /**
     * 3GPP CPM file transfer feature tag
     */
    public final static String FEATURE_3GPP_CPM_FILE_TRANSFER = "+g.3gpp.icsi-ref=\"urn%3Aurn-7%3A3gpp-service.ims.icsi.oma.cpm.filetransfer\"";

    /**
     * 3GPP image share feature tag
     */
    public final static String FEATURE_3GPP_IMAGE_SHARE = "+g.3gpp.iari-ref=\"urn%3Aurn-7%3A3gpp-application.ims.iari.gsma-is\"";

    /**
     * 3GPP image share feature tag for RCS 2.0
     */
    public final static String FEATURE_3GPP_IMAGE_SHARE_RCS2 = "+g.3gpp.app_ref=\"urn%3Aurn-7%3A3gpp-application.ims.iari.gsma-is\"";

    /**
     * 3GPP location share feature tag
     */
    public final static String FEATURE_3GPP_LOCATION_SHARE = "+g.3gpp.iari-ref=\"urn%3Aurn-7%3A3gpp-application.ims.iari.rcs.geopush\"";

    /**
     * RCS-e feature tag prefix
     */
    public final static String FEATURE_RCSE = "+g.3gpp.iari-ref";

    /**
     * 3GPP feature tag prefix
     */
    public final static String FEATURE_3GPP = "+g.3gpp.icsi-ref";

    /**
     * RCS-e image share feature tag
     */
    public final static String FEATURE_RCSE_IMAGE_SHARE = "urn%3Aurn-7%3A3gpp-application.ims.iari.gsma-is";

    /**
     * RCS-e chat feature tag
     */
    public final static String FEATURE_RCSE_CHAT = "urn%3Aurn-7%3A3gpp-application.ims.iari.rcse.im";

    /**
     * RCS-e file transfer feature tag
     */
    public final static String FEATURE_RCSE_FT = "urn%3Aurn-7%3A3gpp-application.ims.iari.rcse.ft";

    /**
     * RCS-e file transfer over HTTP feature tag
     */
    public final static String FEATURE_RCSE_FT_HTTP = "urn%3Aurn-7%3A3gpp-application.ims.iari.rcs.fthttp";

    /**
     * RCS-e presence discovery feature tag
     */
    public final static String FEATURE_RCSE_PRESENCE_DISCOVERY = "urn%3Aurn-7%3A3gpp-application.ims.iari.rcse.dp";

    /**
     * RCS-e social presence feature tag
     */
    public final static String FEATURE_RCSE_SOCIAL_PRESENCE = "urn%3Aurn-7%3A3gpp-application.ims.iari.rcse.sp";

    /**
     * RCS-e geolocation push feature tag
     */
    public final static String FEATURE_RCSE_GEOLOCATION_PUSH = "urn%3Aurn-7%3A3gpp-application.ims.iari.rcs.geopush";

    /**
     * RCS-e file transfer thumbnail feature tag
     */
    public final static String FEATURE_RCSE_FT_THUMBNAIL = "urn%3Aurn-7%3A3gpp-application.ims.iari.rcs.ftthumb";

    /**
     * RCS-e file transfer S&F feature tag
     */
    public final static String FEATURE_RCSE_FT_SF = "urn%3Aurn-7%3A3gpp-application.ims.iari.rcs.ftstandfw";

    /**
     * RCS-e group chat S&F feature tag
     */
    public final static String FEATURE_RCSE_GC_SF = "urn%3Aurn-7%3A3gpp-application.ims.iari.rcs.fullsfgroupchat";

    /**
     * 3GPP IP call feature tag
     */
    public final static String FEATURE_3GPP_IP_VOICE_CALL = "urn%3Aurn-7%3A3gpp-service.ims.icsi.mmtel";

    /**
     * RCS-e IP call feature tag
     */
    public final static String FEATURE_RCSE_IP_VOICE_CALL = "+g.gsma.rcs.ipcall";

    /**
     * RCS IP video call feature tag
     */
    public final static String FEATURE_RCSE_IP_VIDEO_CALL = "video";

    /**
     * RCS-e IARI extension feature tag prefix
     */
    public final static String FEATURE_RCSE_IARI_EXTENSION = "urn%3Aurn-7%3A3gpp-application.ims.iari.rcs";

    /**
     * RCS-e ICSI extension feature tag prefix
     */
    public final static String FEATURE_RCSE_ICSI_EXTENSION = "urn%3Aurn-7%3A3gpp-service.ims.icsi";

    /**
     * 3GPP RCS extension feature tag
     */
    public final static String FEATURE_3GPP_EXTENSION = "urn%3Aurn-7%3A3gpp-service.ims.icsi.gsma.rcs.extension";

    /**
     * 3GPP RCS service extension
     */
    public final static String FEATURE_3GPP_SERVICE_EXTENSION = "urn:urn-7:3gpp-service.ims.icsi.gsma.rcs.extension";

    /**
     * SIP Automata feature tag
     * 
     * <pre>
     * @see RFC 3840 "Indicating User Agent Capabilities in the Session Initiation Protocol (SIP)"
     * 
     * The automata tag indicates whether the UA represents an automata (such as a voicemail server, 
     * conference server, IVR, or recording device) or a human.
     * </pre>
     */
    public final static String FEATURE_SIP_AUTOMATA = "automata";

    /**
     * CMCC RCS custom feature tags suite
     */
    public static class Cmcc {
        /**
         * RCS-e vemoticon message feature tag
         */
        public final static String FEATURE_RCSE_EMOTICON = "urn%3Aurn-7%3A3gpp-application.ims.iari.rcs.mnc000.mcc460.vemoticon;version=1_0";

        /**
         * RCS-e cloud file feature tag
         */
        public final static String FEATURE_RCSE_CLOUD_FILE = "urn%3Aurn-7%3A3gpp-application.ims.iari.rcs.mnc000.mcc460.cloudfile;version=1_0";

        /**
         * RCS-e card message feature tag
         */
        public final static String FEATURE_RCSE_CARD_MESSAGE = "urn%3Aurn-7%3A3gpp-application.ims.iari.rcs.mnc000.mcc460.cardmsg;version=1_0";

        /**
         * RCS-e common extension feature tag
         */
        public final static String FEATURE_RCSE_COMMON_EXTENSION = "urn%3Aurn-7%3A3gpp-application.ims.iari.rcs.mnc000.mcc460.commonextension";

        /**
         * RCS-e group chat management feature tag
         */
        public final static String FEATURE_RCSE_GROUP_MANAGE = "urn%3Aurn-7%3A3gpp-application.ims.iari.rcs.mnc000.mcc460.gpmanage;vs=1";

        /**
         * RCS-e public account message feature tag
         */
        public final static String FEATURE_RCSE_PUBLIC_MESSAGE = "urn%3Aurn-7%3A3gpp-application.ims.iari.rcs.mnc000.mcc460.publicmsg";

        /**
         * RCS-e burn after reading cycle feature tag, only supported in 1-1/n
         */
        public final static String FEATURE_BAR_CYCLE = "barCycle";
    }
}

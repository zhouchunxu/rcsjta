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

package com.gsma.services.rcs.chat;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.StringTokenizer;

/**
 * Card class
 */
public class Card implements Parcelable, Serializable {
    /**
     * Mandatory 当该字段值为1010时，为链接分享 当该字段值为1020时，为流量红包 当该字段值为1030时，为现金红包
     */
    private String mMediaType;

    /**
     * Optional 当属于红包消息时，为红包操作类型 generate_redbag：发红包 compete_redbag：抢红包通知
     */
    private String mOperationType;

    /**
     * Optional 采用RFC3339格式（采用UTC+8的北京时间表示方法）,精确到毫秒级
     */
    private String mCreateTime;

    /**
     * 当链接分享消息为转发的公众账号消息时，为消息所属公众账号标识
     */
    private String mPaUuid;

    /**
     * 是否允许转发 0：允许转发 1：不允许转发 默认为允许转发
     */
    private String mForwordable;

    /**
     * 用于区分该卡片消息携带链接的安全等级
     */
    private String mTrustLevel;

    /**
     * 用于表明当用户点击卡片消息时是否需要携带身份信息 0：不需要携带身份信息 1：需要携带用户标识 2：需要携带群标识 默认为不需要携带身份信息
     */
    private String mAccessId;

    /**
     * 用于表明当用户点击卡片消息时是否需要携带认证信息 0：不需要携带认证信息 1：需要携带用户认证信息 默认为不需要携带认证信息
     */
    private String mAuthLevel;

    /**
     * 卡片消息体协议版本号
     */
    private String mVersion;

    /**
     * 用于存放消息的前景、背景、文字样式等
     */
    private String mDisplayStyle;

    /**
     * 用于在不支持media_type定义类型时，显示的提示语文本
     */
    private String mDefaultText;

    /**
     * 用于在不支持media_type定义类型时，可跳转的链接
     */
    private String mDefaultLink;

    /**
     * mandatory 包含卡片消息具体内容, mandatory
     */
    private MediaArticle mCard;

    public static class MediaArticle implements Parcelable, Serializable {
        // 条件必选 String 当属于红包消息时，为红包祝福语，必选
        // 当属于链接分享消息时，为文章标题，可选
        private String mTitle;

        // 条件必选 String 当属于红包消息时，为红包来源（模板文本），必选
        // 当属于链接分享消息时，为文章作者，可选
        private String mAuthor;

        // 条件必选 String 当属于红包消息时，为红包典型图片（图片URL），必选
        // 当属于链接分享消息时，为链接详情的缩略图链接，可选
        private String mThumbLink;

        // 可选 String 当属于链接分享消息时，为链接详情中的原图链接
        private String mOriginalLink;

        // 必选 String 当属于红包消息时，为红包详情（群ID、圈子ID、红包ID）的链接地址
        // 当属于链接分享消息时，为消息正文内容页的链接
        private String mBodyLink;

        // 可选 String 当文章链接到外部网页时的外部链接地址
        private String mSourceLink;

        // 可选 String 当属于链接分享消息时，为链接内容的摘要
        private String mMainText;

        // 必选 String 当属于红包消息时，为红包消息在钱包AS服务上的唯一标识
        // 当属于链接分享消息时，为文章资源在公众平台上的唯一标识
        private String mMediaUuid;

        public MediaArticle(String title, String author, String thumbLink, String originalLink,
                String bodyLink, String sourceLink, String mainText, String mediaUuid) {
            mTitle = title;
            mAuthor = author;
            mThumbLink = thumbLink;
            mOriginalLink = originalLink;
            mBodyLink = bodyLink;
            mSourceLink = sourceLink;
            mMainText = mainText;
            mMediaUuid = mediaUuid;
        }

        /**
         * Constructor: returns a MediaArticle instance as parsed from the CONTENT field of a
         * CardMessage in the ChatLog.Message provider.
         *
         * @param mediaArticle Provider media article format
         */
        public MediaArticle(String mediaArticle) {
            StringTokenizer items = new StringTokenizer(mediaArticle, ",");
            mTitle = String.valueOf(items.nextToken());
            mAuthor = String.valueOf(items.nextToken());
            mThumbLink = String.valueOf(items.nextToken());
            mOriginalLink = String.valueOf(items.nextToken());
            mBodyLink = String.valueOf(items.nextToken());
            mSourceLink = String.valueOf(items.nextToken());
            mMainText = String.valueOf(items.nextToken());
            mMediaUuid = String.valueOf(items.nextToken());
        }

        /**
         * Constructor
         *
         * @param source Parcelable source
         * @hide
         */
        protected MediaArticle(Parcel source) {
            mTitle = source.readString();
            mAuthor = source.readString();
            mThumbLink = source.readString();
            mOriginalLink = source.readString();
            mBodyLink = source.readString();
            mSourceLink = source.readString();
            mMainText = source.readString();
            mMediaUuid = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mTitle);
            dest.writeString(mAuthor);
            dest.writeString(mThumbLink);
            dest.writeString(mOriginalLink);
            dest.writeString(mBodyLink);
            dest.writeString(mSourceLink);
            dest.writeString(mMainText);
            dest.writeString(mMediaUuid);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<MediaArticle> CREATOR = new Creator<MediaArticle>() {
            @Override
            public MediaArticle createFromParcel(Parcel in) {
                return new MediaArticle(in);
            }

            @Override
            public MediaArticle[] newArray(int size) {
                return new MediaArticle[size];
            }
        };

        public String getTitle() {
            return mTitle;
        }

        public String getAuthor() {
            return mAuthor;
        }

        public String getThumbLink() {
            return mThumbLink;
        }

        public String getOriginalLink() {
            return mOriginalLink;
        }

        public String getBodyLink() {
            return mBodyLink;
        }

        public String getSourceLink() {
            return mSourceLink;
        }

        public String getMainText() {
            return mMainText;
        }

        public String getMediaUuid() {
            return mMediaUuid;
        }

        /**
         * Returns the media article in provider format.
         *
         * @return String
         */
        @Override
        public String toString() {
            return new StringBuilder().append(mTitle).append(",").append(mAuthor).append(",")
                    .append(mThumbLink).append(",").append(mOriginalLink).append(",")
                    .append(mBodyLink).append(",").append(mSourceLink).append(",")
                    .append(mMainText).append(",").append(mMediaUuid).toString();
        }
    }

    public Card(String mediaType, String operationType, String createTime, String paUuid,
            String forwordable, String trustLevel, String accessId, String authLevel,
            String version, String displayStyle, String defaultText, String defaultLink,
            MediaArticle card) {
        this.mMediaType = mediaType;
        this.mOperationType = operationType;
        this.mCreateTime = createTime;
        this.mPaUuid = paUuid;
        this.mForwordable = forwordable;
        this.mTrustLevel = trustLevel;
        this.mAccessId = accessId;
        this.mAuthLevel = authLevel;
        this.mVersion = version;
        this.mDisplayStyle = displayStyle;
        this.mDefaultText = defaultText;
        this.mDefaultLink = defaultLink;
        this.mCard = card;
    }

    /**
     * Constructor: returns a Card instance as parsed from the CONTENT field of a CardMessage in the
     * ChatLog.Message provider.
     *
     * @param card Provider card format
     */
    public Card(String card) {
        StringTokenizer items = new StringTokenizer(card, ",");
        mMediaType = String.valueOf(items.nextToken());
        mOperationType = String.valueOf(items.nextToken());
        mCreateTime = String.valueOf(items.nextToken());
        mPaUuid = String.valueOf(items.nextToken());
        mForwordable = String.valueOf(items.nextToken());
        mTrustLevel = String.valueOf(items.nextToken());
        mAccessId = String.valueOf(items.nextToken());
        mAuthLevel = String.valueOf(items.nextToken());
        mVersion = String.valueOf(items.nextToken());
        mDisplayStyle = String.valueOf(items.nextToken());
        mDefaultText = String.valueOf(items.nextToken());
        mDefaultLink = String.valueOf(items.nextToken());
        mCard = new MediaArticle(String.valueOf(items.nextToken()), String.valueOf(items
                .nextToken()), String.valueOf(items.nextToken()),
                String.valueOf(items.nextToken()), String.valueOf(items.nextToken()),
                String.valueOf(items.nextToken()), String.valueOf(items.nextToken()),
                String.valueOf(items.nextToken()));
    }

    protected Card(Parcel source) {
        mMediaType = source.readString();
        mOperationType = source.readString();
        mCreateTime = source.readString();
        mPaUuid = source.readString();
        mForwordable = source.readString();
        mTrustLevel = source.readString();
        mAccessId = source.readString();
        mAuthLevel = source.readString();
        mVersion = source.readString();
        mDisplayStyle = source.readString();
        mDefaultText = source.readString();
        mDefaultLink = source.readString();
        mCard = source.readParcelable(MediaArticle.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mMediaType);
        dest.writeString(mOperationType);
        dest.writeString(mCreateTime);
        dest.writeString(mPaUuid);
        dest.writeString(mForwordable);
        dest.writeString(mTrustLevel);
        dest.writeString(mAccessId);
        dest.writeString(mAuthLevel);
        dest.writeString(mVersion);
        dest.writeString(mDisplayStyle);
        dest.writeString(mDefaultText);
        dest.writeString(mDefaultLink);
        dest.writeParcelable(mCard, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Card> CREATOR = new Creator<Card>() {
        @Override
        public Card createFromParcel(Parcel in) {
            return new Card(in);
        }

        @Override
        public Card[] newArray(int size) {
            return new Card[size];
        }
    };

    public String getMediaType() {
        return mMediaType;
    }

    public String getOperationType() {
        return mOperationType;
    }

    public String getCreateTime() {
        return mCreateTime;
    }

    public String getPaUuid() {
        return mPaUuid;
    }

    public String getForwordable() {
        return mForwordable;
    }

    public String getTrustLevel() {
        return mTrustLevel;
    }

    public String getAccessId() {
        return mAccessId;
    }

    public String getAuthLevel() {
        return mAuthLevel;
    }

    public String getVersion() {
        return mVersion;
    }

    public String getDisplayStyle() {
        return mDisplayStyle;
    }

    public String getDefaultText() {
        return mDefaultText;
    }

    public String getDefaultLink() {
        return mDefaultLink;
    }

    public MediaArticle getCard() {
        return mCard;
    }

    /**
     * Returns the card in provider format.
     *
     * @return String
     */
    @Override
    public String toString() {
        return new StringBuilder().append(mMediaType).append(",").append(mOperationType)
                .append(",").append(mCreateTime).append(",").append(mPaUuid).append(",")
                .append(mForwordable).append(",").append(mTrustLevel).append(",").append(mAccessId)
                .append(",").append(mAuthLevel).append(",").append(mVersion).append(",")
                .append(mDisplayStyle).append(",").append(mDefaultText).append(",")
                .append(mDefaultLink).append(",").append(mCard.toString()).toString();
    }
}

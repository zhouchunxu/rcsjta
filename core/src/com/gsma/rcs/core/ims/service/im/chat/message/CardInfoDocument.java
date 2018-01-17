
package com.gsma.rcs.core.ims.service.im.chat.message;

/**
 * Card info document
 * 
 */
public class CardInfoDocument {
    /**
     * MIME type
     */
    public static final String MIME_TYPE = "application/card+xml";

    private String mediaType;
    private String operationType;
    private String createTime;
    private String paUuid;
    private String forwardable;
    private String trustLevel;
    private String accessId;
    private String authLevel;
    private String version;
    private String displayStyle;
    private String defaultText;
    private String defaultLink;
    private MediaArticle card;

    public CardInfoDocument() {
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getPaUuid() {
        return paUuid;
    }

    public void setPaUuid(String paUuid) {
        this.paUuid = paUuid;
    }

    public String getForwardable() {
        return forwardable;
    }

    public void setForwardable(String forwardable) {
        this.forwardable = forwardable;
    }

    public String getTrustLevel() {
        return trustLevel;
    }

    public void setTrustLevel(String trustLevel) {
        this.trustLevel = trustLevel;
    }

    public String getAccessId() {
        return accessId;
    }

    public void setAccessId(String accessId) {
        this.accessId = accessId;
    }

    public String getAuthLevel() {
        return authLevel;
    }

    public void setAuthLevel(String authLevel) {
        this.authLevel = authLevel;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDisplayStyle() {
        return displayStyle;
    }

    public void setDisplayStyle(String displayStyle) {
        this.displayStyle = displayStyle;
    }

    public String getDefaultText() {
        return defaultText;
    }

    public void setDefaultText(String defaultText) {
        this.defaultText = defaultText;
    }

    public String getDefaultLink() {
        return defaultLink;
    }

    public void setDefaultLink(String defaultLink) {
        this.defaultLink = defaultLink;
    }

    public MediaArticle getCard() {
        return card;
    }

    public void setCard(MediaArticle card) {
        this.card = card;
    }
}

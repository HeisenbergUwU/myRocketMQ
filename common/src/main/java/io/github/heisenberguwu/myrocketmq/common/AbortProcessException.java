package io.github.heisenberguwu.myrocketmq.common;

import io.github.heisenberguwu.myrocketmq.common.help.FAQUrl;

public class AbortProcessException extends RuntimeException {
    private static final long serialVersionUID = -5728810933841185841L;
    private int responseCode;
    private String errorMessage;

    public AbortProcessException(String errorMessage, Throwable cause) {
        super(FAQUrl.attachDefaultURL(errorMessage), cause);
        this.responseCode = -1;
        this.errorMessage = errorMessage;
    }

    public AbortProcessException(int responseCode, String errorMessage) {
        super(FAQUrl.attachDefaultURL("CODE: " + UtilAll.responseCode2String(responseCode) + "  DESC: "
            + errorMessage));
        this.responseCode = responseCode;
        this.errorMessage = errorMessage;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public AbortProcessException setResponseCode(final int responseCode) {
        this.responseCode = responseCode;
        return this;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
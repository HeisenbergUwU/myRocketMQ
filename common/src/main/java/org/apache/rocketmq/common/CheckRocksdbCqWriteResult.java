package org.apache.rocketmq.common;

public class CheckRocksdbCqWriteResult {
    String checkResult;

    int checkStatus;


    public enum CheckStatus {
        CHECK_OK(0),
        CHECK_NOT_OK(1),
        CHECK_IN_PROGRESS(2),
        CHECK_ERROR(3);

        private int value;

        CheckStatus(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public String getCheckResult() {
        return checkResult;
    }

    public void setCheckResult(String checkResult) {
        this.checkResult = checkResult;
    }

    public int getCheckStatus() {
        return checkStatus;
    }

    public void setCheckStatus(int checkStatus) {
        this.checkStatus = checkStatus;
    }
}
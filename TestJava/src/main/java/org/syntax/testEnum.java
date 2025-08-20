package org.syntax;

public class testEnum {
    public static void main(String[] args) {
        LocalTransactionState commitMessage = LocalTransactionState.COMMIT_MESSAGE;

        System.out.println(commitMessage.ordinal());
    }
}


enum LocalTransactionState {
    COMMIT_MESSAGE,
    ROLLBACK_MESSAGE,
    UNKNOW,
}

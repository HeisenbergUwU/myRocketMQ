package org.apache.rocketmq.remoting;

import org.apache.rocketmq.remoting.protocol.RemotingCommand;

public class testRemotingCommand {
    public static void main(String[] args) {
        RemotingCommand ok = RemotingCommand.createResponseCommand(200, "ok");

        System.out.println(ok);
    }
}

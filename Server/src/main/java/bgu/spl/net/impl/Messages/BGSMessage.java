package bgu.spl.net.impl.Messages;

import bgu.spl.net.api.Message;

public abstract class BGSMessage implements Message<Short> {
    short opCode;

    public BGSMessage(short opCode) {
        this.opCode = opCode;
    }

    public short getOpCode() {
        return opCode;
    }

    public void setOpCode(short opCode) {
        this.opCode = opCode;
    }
}

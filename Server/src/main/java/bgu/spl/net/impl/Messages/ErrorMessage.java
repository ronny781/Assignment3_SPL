package bgu.spl.net.impl.Messages;

public class ErrorMessage extends BGSMessage {
    short messageOpCode;

    public ErrorMessage(short messageOpCode) {
        super((short) 11);
        this.messageOpCode = messageOpCode;
    }

    public short getMessageOpCode() {
        return messageOpCode;
    }

    public void setMessageOpCode(short messageOpCode) {
        this.messageOpCode = messageOpCode;
    }
}
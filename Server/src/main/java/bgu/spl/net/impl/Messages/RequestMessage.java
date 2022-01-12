package bgu.spl.net.impl.Messages;

import java.util.ArrayList;

public class RequestMessage extends BGSMessage{
    ArrayList<String> arguments;

    public RequestMessage(){
        super((short) 0);
        this.arguments = null;
    }

    public RequestMessage(short opCode, ArrayList<String> operations, short courseNum) {
        super(opCode);
        this.arguments = operations;
    }

    public ArrayList<String> getArguments() {
        return arguments;
    }

    public void setArguments(ArrayList<String> arguments) {
        this.arguments = arguments;
    }

}

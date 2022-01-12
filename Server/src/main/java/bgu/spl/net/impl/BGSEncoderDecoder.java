package bgu.spl.net.impl;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.impl.Messages.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class BGSEncoderDecoder implements MessageEncoderDecoder<BGSMessage>{
    private byte[] bytes = new byte[1 << 10]; // start with 1k
    private int len = 0;

    /**
     * Simple helper function for conversion
     * @param byteArr array of bytes of length 2
     * @return short after conversion
     */
    public static short bytesToShort(byte[] byteArr)
    {
        short result = (short)((byteArr[0] & 0xff) << 8);
        result += (short)(byteArr[1] & 0xff);
        return result;
    }

    /**
     * Simple helper function for conversion
     * @param num short to convert
     * @return array of bytes of length 2
     */
    public byte[] shortToBytes(short num)
    {
        byte[] bytesArr = new byte[2];
        bytesArr[0] = (byte)((num >> 8) & 0xFF);
        bytesArr[1] = (byte)(num & 0xFF);
        return bytesArr;
    }

    @Override
    public BGSMessage decodeNextByte(byte nextByte) {
        pushByte(nextByte);
        if(len != 0 && bytes[len-1]==';')
            return popMessage();
        return null;
    }

    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }

        bytes[len++] = nextByte;
    }

    private BGSMessage popMessage() { // Pop message after all bytes received
        short opCode = bytesToShort(new byte[]{bytes[0], bytes[1]});
        RequestMessage message = new RequestMessage();
        message.setOpCode(opCode);
        ArrayList<String> arguments = new ArrayList<>();
        // start from index 2 because the first 2 bytes are opCodes
        boolean first = true;
        for (int i = 2, stringStart = 2; i < len; i++) {
            if((opCode == 4) && first) {
                first = false;
                arguments.add(new String(bytes, i, 1, StandardCharsets.UTF_8));
                i++;
                stringStart++;
            }
            if (bytes[i] == '\0') { // end of string operation
                arguments.add(new String(bytes, stringStart, i-stringStart, StandardCharsets.UTF_8));
                stringStart = i + 1;
            }
            if( opCode == 2 && i==len-2){ // len-2 because of ;
                arguments.add(new String(bytes, len-2, 1, StandardCharsets.UTF_8));
            }
        }
        message.setArguments(arguments);
        len=0; //reset position on bytes array
        return message;
    }

    @Override
    public byte[] encode(BGSMessage message) { // Encode message from server to client

        byte[] result; // the return message

        byte[] opCode = shortToBytes(message.getOpCode()); // length 2

        if(message instanceof ACKMessage){
            ACKMessage ackMessage = (ACKMessage)message;
            byte[] ackMessageBytes = ackMessage.getAckMessage().getBytes(StandardCharsets.UTF_8);
            byte[] messageOpCode = shortToBytes(ackMessage.getMessageOpCode()); //length 2
            int messageLen = opCode.length + ackMessageBytes.length + messageOpCode.length+1;
            result = new byte[messageLen];
            System.arraycopy(opCode,0,result,0,2);
            System.arraycopy(messageOpCode,0,result,2,2);
            System.arraycopy(ackMessageBytes,0,result,4,ackMessageBytes.length);
            result[result.length-1] = ';';
        }
        else if(message instanceof ErrorMessage){
            byte[] messageOpCode = shortToBytes( ((ErrorMessage) message).getMessageOpCode());
            result = new byte[5];
            System.arraycopy(opCode,0,result,0,2);
            System.arraycopy(messageOpCode,0,result,2,2);
            result[result.length-1] = ';';
        }
        else {
            NotificationMessage notificationMessage = (NotificationMessage) message;
            byte type = notificationMessage.getType();
            int messageLen;
            int dateLength = 0;
            byte[] notificationDate = null;
            byte[] notificationType = new byte[]{type};
            byte[] notificationPostingUser = (notificationMessage.getPostingUser()+"\0").getBytes(StandardCharsets.UTF_8);
            byte[] notificationContent = (notificationMessage.getContent()+"\0").getBytes(StandardCharsets.UTF_8);
            int postingUserLength = notificationPostingUser.length;
            int contentLength = notificationContent.length;
            if (type == 0) {
                notificationDate = (notificationMessage.getDate() + "\0").getBytes(StandardCharsets.UTF_8);
                dateLength = notificationDate.length;
                messageLen = opCode.length + postingUserLength + contentLength + dateLength + 2;
            }
            else {
                messageLen = opCode.length + postingUserLength + contentLength + 2;
            }
            result = new byte[messageLen];
            System.arraycopy(opCode, 0, result, 0, 2);
            System.arraycopy(notificationType, 0, result, 2, 1);
            System.arraycopy(notificationPostingUser, 0, result, 3, postingUserLength);
            System.arraycopy(notificationContent, 0, result, 3+postingUserLength , contentLength);
            if (type == 0)
                System.arraycopy(notificationDate, 0, result, 3+postingUserLength + contentLength , dateLength);
            result[result.length-1] = ';';
        }

        return result;
    }
}

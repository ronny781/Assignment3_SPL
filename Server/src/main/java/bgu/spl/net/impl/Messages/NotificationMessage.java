package bgu.spl.net.impl.Messages;

public class NotificationMessage extends BGSMessage{
  //  short messageOpCode;
    private byte type; // PM=0, Post = 1
    private String postingUser;
    private String content;
    private String date;

    public NotificationMessage(byte type, String postingUser, String content, String date){
        super((short) 9);
        this.type = type;
        this.postingUser = postingUser;
        this.content = content;
        this.date = date;
    }


    public byte getType() {
        return type;
    }

    public String getPostingUser() {
        return postingUser;
    }

    public String getContent() {
        return content;
    }
    public String getDate() {
        return date;
    }
}

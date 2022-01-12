package bgu.spl.net.impl;

import bgu.spl.net.api.bidi.BidiMessagingProtocol;
import bgu.spl.net.api.bidi.Connections;
import bgu.spl.net.impl.Messages.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BGSProtocol implements BidiMessagingProtocol<BGSMessage> {
    private final Database database = Database.getInstance();
    private  ConnectionsImpl connections;
    private User user ;
    private boolean shouldTerminate = false;
    private int connId;



    @Override
    public void start(int connectionId, Connections<BGSMessage> connections) { // Initialize the connection
        this.connId = connectionId;
        this.connections = (ConnectionsImpl) connections;
    }


    @Override
    public void process(BGSMessage message) {
        short opCode = message.getOpCode();
        RequestMessage msg = (RequestMessage) message;
        switch (opCode) {
            case 1:
                register(msg);
                break;
            case 2:
                login(msg);
                break;
            case 3:
                logout(msg);
                break;
            case 4:
                follow(msg);
                break;
            case 5:
                post(msg);
                break;
            case 6:
                PM(msg);
                break;
            case 7:
                logStat(msg);
                break;
            case 8:
                stat(msg);
                break;
            case 12:
                block(msg);
                break;
        }
    }


    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    //REGISTER
    public void register(RequestMessage msg) {
        List<String> arguments = msg.getArguments();
        if (database.isRegistered(arguments.get(0))) {
            connections.send(connId, new ErrorMessage(msg.getOpCode())); //Send error message back to client
            return;
        }
        String username = arguments.get(0);
        String password = arguments.get(1);
        String birthday = arguments.get(2);
        database.addUser(username, password, birthday);
        connections.send(connId, new ACKMessage(msg.getOpCode(), "")); //Send ACK message to client
    }

    //LOGIN
    public void login(RequestMessage msg) {
        List<String> arguments = msg.getArguments();
        String username = arguments.get(0);
        String password = arguments.get(1);
        String captcha = arguments.get(2);
        if (!database.isRegistered(username) ||
                !database.correctCredentials(username, password) ||
                database.getUser(username).isLoggedIn() ||
                captcha.equals("0")) {
            connections.send(connId, new ErrorMessage(msg.getOpCode())); //Send error message back to client
            return;
        }
        user = database.getUser(username); //Assign user field to current logged user
        user.setConnectionId(connId);
        user.setLoggedIn(true);
        ConcurrentLinkedQueue<NotificationMessage> notifications = user.getNotifications();
        connections.send(connId, new ACKMessage(msg.getOpCode(), "")); //Send ACK message to client
        for(NotificationMessage n : notifications) //Read all notifications that added up for me when I was offline
            connections.send(connId, n);
    }

    //LOGOUT
    public void logout(RequestMessage msg) {
        if (user== null || !user.isLoggedIn()) {
            connections.send(connId, new ErrorMessage(msg.getOpCode())); //Send error message back to client
            return;
        }
        connections.send(connId, new ACKMessage(msg.getOpCode(), "")); //Send ACK message to client
        connections.disconnect(connId);
        user.setLoggedIn(false);
        shouldTerminate = true;
    }

    //FOLLOW/UNFOLLOW
    public void follow(RequestMessage msg) {
        List<String> arguments = msg.getArguments();
        String followBit = arguments.get(0);
        String username = arguments.get(1).substring(1);

        if (user == null || !user.isLoggedIn()) {
            connections.send(connId, new ErrorMessage(msg.getOpCode()));
            return;
        }
        if (followBit.equals("0")) { // Follow
            if (user.isFollowing(username) || user.isBlocked(username)) {
                connections.send(connId, new ErrorMessage(msg.getOpCode()));
                return;
            }
            user.follow(database.getUser(username));
        } else { // Unfollow
            if (!user.isFollowing(username)) {
                connections.send(connId, new ErrorMessage(msg.getOpCode()));
                return;
            }
            user.removeFromFollowing(username);
        }
        connections.send(connId, new ACKMessage(msg.getOpCode(), "")); //Send ACK message to client
    }

    //POST
    public void post(RequestMessage msg) {
        if (user== null || !user.isLoggedIn()) {
            connections.send(connId, new ErrorMessage(msg.getOpCode())); //Send error message back to client
            return;
        }
        database.getPosts().add(msg);
        List<String> arguments = msg.getArguments();

        StringBuilder content = new StringBuilder();
        List<String> usernames = new ArrayList<>();
        for(String word: arguments) {
            if (word.charAt(0) == '@')
                usernames.add(word.substring(1));
            content.append(word + " ");
        }
        String cont = content.toString().substring(0,content.length()-1); // Convert to string and removes space at the end
        HashSet<User> toSend = new HashSet<>(user.getFollowers().values()); //Add all followers to my sendTo list
        for(String username : usernames)
            if (database.isRegistered(username) && !user.isBlocked(username)) {
                toSend.add(database.getUser(username)); // Add all tagged usernames to my sendTo list
            }

        for (User recipient : toSend) {
            if (recipient.isLoggedIn()) // If recipient is logged in send the message directly to him
                connections.send(recipient.getConnectionId(), new NotificationMessage( (byte)1, user.getUsername(), cont, ""));
            else // Add message to his notification list
                recipient.getNotifications().add(new NotificationMessage((byte)1, user.getUsername(), cont, ""));
        }
        user.increaseNumOfPosts();
        connections.send(connId, new ACKMessage(msg.getOpCode(), "")); //Send ACK message to client
    }
    //PM
    public void PM(RequestMessage msg) {
        List<String> arguments = msg.getArguments();
        String username = arguments.get(0);
        if (user == null || !user.isLoggedIn() || !database.isRegistered(username) ||
            !user.isFollowing(username) || user.isBlocked(username)) {
            connections.send(connId, new ErrorMessage(msg.getOpCode())); //Send error message back to client
            return;
        }
        StringBuilder cont = new StringBuilder();
        for(int i=1 ; i <arguments.size()-1 ;i++) // Concatenate content of message
            cont.append(arguments.get(i)+ " ") ;
        String content = cont.toString().substring(0,cont.length()-1); // remove last space
        String date = arguments.get(arguments.size()-1);
        String[] splitContent = content.split(" ");
        HashSet<String> filters = database.getWordsToFilter();
        StringBuilder str = new StringBuilder();
        for(int i=0; i < splitContent.length; i++) { // Filter forbidden words
            if (filters.contains(splitContent[i]))
                splitContent[i] = "<filtered>";
            str.append(splitContent[i] + " ");
        }
        String postFilter = str.toString().substring(0,str.length()-1); // remove last space
        database.getPmMessages().add(msg);
        User recipient = database.getUser(username);
        if (recipient.isLoggedIn()) // If recipient is logged in send the message directly to him
            connections.send(recipient.getConnectionId(), new NotificationMessage((byte)0, user.getUsername(), postFilter, date));
        else  // Add message to his notification list
            recipient.getNotifications().add(new NotificationMessage((byte)0, user.getUsername(), postFilter, date));

        connections.send(connId, new ACKMessage(msg.getOpCode(), "")); //Send ACK message to client
    }

    //LOGSTAT
    public void logStat(RequestMessage msg) { // Retrieving stats from all logged in users
        if (user== null || !user.isLoggedIn()) {
            connections.send(connId, new ErrorMessage(msg.getOpCode())); //Send error message back to client
            return;
        }
        ConcurrentHashMap<String, User> registrationMap = database.getRegisteredUsers();
        String ackMessage = "";
        for (String username : registrationMap.keySet()){ // Concatenate string of stats from logged in users
            if(database.getUser(username).isLoggedIn() && !user.isBlocked(username))
                ackMessage += database.getUser(username).getStats() + "\n";
        }

        connections.send(connId, new ACKMessage(msg.getOpCode(), ackMessage)); //Send ACK message to client
    }
    //STAT
    public void stat(RequestMessage msg) {// Retrieving stats from given list of users
        if (user== null || !user.isLoggedIn()) {
            connections.send(connId, new ErrorMessage(msg.getOpCode()));
            return;
        }
        List<String> arguments = msg.getArguments();
        String usernames = arguments.get(0);
        String[] splitUsernames = usernames.split("\\|");
        for (String username : splitUsernames) {
            if (!database.isRegistered(username) || user.isBlocked(username)) {
                connections.send(connId, new ErrorMessage(msg.getOpCode()));
                return;
            }
        }
        String ackMessage = "";
        for (String username : splitUsernames) {
            ackMessage += database.getUser(username).getStats() + "\n";
        }
        connections.send(connId, new ACKMessage(msg.getOpCode(), ackMessage)); //Send ACK message to client
    }
    //BLOCK
    public void block(RequestMessage msg) { // Block user method
        String username = msg.getArguments().get(0);
        if (user == null || !user.isLoggedIn()|| !database.isRegistered(username)) {
            connections.send(connId, new ErrorMessage(msg.getOpCode())); //Send error message back to client
            return;
        }
        user.block(username);
        database.getUser(username).block(user.getUsername());
        connections.send(connId, new ACKMessage(msg.getOpCode(), "")); //Send ACK message to client
    }
    public void setConnId(int id){
        connId = id;
    }
    public int getConnId() {
        return connId;
    }

}

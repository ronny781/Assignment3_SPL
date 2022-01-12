package bgu.spl.net.impl;

import bgu.spl.net.api.Message;
import bgu.spl.net.api.bidi.Connections;
import bgu.spl.net.impl.Messages.BGSMessage;
import bgu.spl.net.srv.BlockingConnectionHandler;
import bgu.spl.net.srv.ConnectionHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionsImpl<T> implements Connections<T> {
    private final ConcurrentHashMap<Integer, ConnectionHandler<T>> activeClients = new ConcurrentHashMap<>(); //Map of connection id to connection handler
    private AtomicInteger connId = new AtomicInteger(1); //connection id assigner

    public static ConnectionsImpl getInstance() {
        return connection.instance ;
    }
    private static class connection {
        private static final ConnectionsImpl instance = new ConnectionsImpl() ;
    }

    @Override
    public boolean send(int connectionId, T msg) { //This method used to send message to a client
        if(!activeClients.containsKey(connectionId)) // According to assignment when connId doesn't exist return false.
            return false;
        activeClients.get(connectionId).send(msg);
        return true;
    }

    @Override
    public void broadcast(T msg) {
        for(ConnectionHandler<T> handler : activeClients.values()){
            handler.send(msg);
        }
    }

    @Override
    public void disconnect(int connectionId) {
        activeClients.remove(connectionId);
    } //Disconnect by deleting pair from map

    public ConcurrentHashMap getActiveClients() {
        return activeClients;
    }

    public int assignConnId(){
        return connId.getAndAdd(1);
    }

}

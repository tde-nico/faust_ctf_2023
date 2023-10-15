package de.faust.auction.communication;

import java.io.*;
import java.net.Socket;

public class RPCObjectConnection {
    protected final RPCConnection connection;

    public RPCObjectConnection(Socket socket) throws IOException {
        connection = new RPCConnection(socket);
    }

    public void sendObject(Serializable object) throws Exception {
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        ObjectOutputStream objOut = new ObjectOutputStream(byteOutStream);
        objOut.writeObject(object);
        objOut.flush();
        byte[] data = byteOutStream.toByteArray();
        connection.sendChunk(data);
    }

    public Serializable receiveObject() throws IOException, ClassNotFoundException {
        byte[] data = connection.receiveChunk();
        ByteArrayInputStream byteInStream = new ByteArrayInputStream(data);
        ObjectInputStream objIn = new ObjectInputStream(byteInStream);
        return (Serializable) objIn.readObject();
    }

    public void reconnect() throws IOException {
        this.connection.reconnect();
    }

    public boolean isConnected(){
        return this.connection.isConnected();
    }

    @Deprecated
    public String getClientID(){
        return this.connection.getClientID();
    }

    public void close() throws IOException {
        connection.close();
    }
}

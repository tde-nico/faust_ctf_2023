package de.faust.auction.communication;

import java.io.*;

public class RPCRequest implements Serializable {
    private int objectID;
    private Object[] args;
    private String methodName;
    private String rpcID;
    private int sequenceNumber;
    private String clientID;

    public RPCRequest(int objectID, String methodName, Object[] args, String clientID, String rpcGuid, int sequenceNumber) {
        this.objectID = objectID;
        this.methodName = methodName;
        this.args = args;
        this.clientID = clientID;
        this.rpcID = rpcGuid;
        this.sequenceNumber = sequenceNumber;
    }

    public RPCRequest() {
    }

    public int getObjectID() {
        return objectID;
    }

    public Object[] getArgs() {
        return args;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getClientID() {
        return clientID;
    }

    public String getRpcID(){return this.rpcID;}
    public int getSequenceNumber(){return sequenceNumber;}

    /*@Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(objectID);
        out.writeObject(args);
        out.writeUTF(methodName);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.objectID = in.readInt();
        this.args = (Object[]) in.readObject();
        this.methodName = in.readUTF();
    }*/
}


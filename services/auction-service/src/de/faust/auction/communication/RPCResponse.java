package de.faust.auction.communication;

import java.io.*;

public class RPCResponse implements Serializable {

    private Object object;
    private String rpcID;
    private int sequenceNumber;

    public RPCResponse(Object obj, String rpcID, int sequenceNumber) {
        this.object = obj;
        this.rpcID = rpcID;
        this.sequenceNumber = sequenceNumber;
    }

    public RPCResponse() {
    }

    public Object getObject() {
        return object;
    }

    public String getRpcID(){return rpcID;}

    public long getSequenceNumber(){return this.sequenceNumber;}

    /*@Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(this.object);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.object = in.readObject();
    }*/
}

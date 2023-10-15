package de.faust.auction.communication;

import java.io.*;
import java.util.Objects;

public class RPCRemoteReference implements Externalizable {

    private String host;
    private int objectID;

    public RPCRemoteReference(String host, int objectID){
        this.host = host;
        this.objectID = objectID;
    }

    public RPCRemoteReference() {
    }

    public int getObjectID() {
        return objectID;
    }

    public String getHost() {
        return host;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(host);
        out.writeInt(objectID);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        host = in.readUTF();
        objectID = in.readInt();
    }

    @Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof RPCRemoteReference reference)) {
            return false;
        }

        return objectID== (reference.objectID) && host.equals(reference.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, objectID);
    }

    @Override
    public String toString() {
        return "RPCRemoteReference{" +
                "host='" + host + '\'' +
                ", objectID=" + objectID +
                '}';
    }
}

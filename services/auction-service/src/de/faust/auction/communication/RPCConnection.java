package de.faust.auction.communication;

import java.net.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.rmi.server.RMISocketFactory;

public class RPCConnection {
    public static final int DEFAULT_TIMEOUT_MS = 10 * 1000;
    
    private Socket socket;
    private final InetAddress target;
    private final int localPort;
    private final int remotePort;

    public RPCConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.target = socket.getInetAddress();
        this.localPort = socket.getLocalPort();
        this.remotePort = socket.getPort();
    }

    public void sendChunk(byte[] chunk) throws Exception {
        int length = chunk.length;
        // Write the length to the output stream (write only sends one byte => the integer, which consists of four bytes, needs to be split)
        byte[] length_bytes = ByteBuffer.allocate(4).putInt(length).array();
        socket.getOutputStream().write(length_bytes);

        // write the data to the stream
        socket.getOutputStream().write(chunk);
        socket.getOutputStream().flush();
    }

    public byte[] receiveChunk() throws IOException {
        // the first four bytes encode the length of the chunk
        byte[] bytes = new byte[4];
        int r = socket.getInputStream().readNBytes(bytes, 0, 4);
        if (r != 4) {
            throw new IOException("Premature end of stream. Tried to read 4 bytes got " + r + " bytes.");
        }
        int length = ByteBuffer.wrap(bytes).getInt();
        byte[] chunk = new byte[length];
        r = socket.getInputStream().readNBytes(chunk, 0, length);
        if (r != length) {
            throw new IOException("Premature end of stream");
        }
        return chunk;
    }

    public boolean isConnected() {
        return !this.socket.isClosed();
    }

    public String getClientID(){
        return this.socket.getInetAddress().getHostName() + this.socket.getPort();
    }

    public void close() throws IOException {
        socket.close();
    }
    public void reconnect() throws IOException {
        if (!socket.isClosed()){
            socket.close();
        }
        socket = new Socket(this.target, this.remotePort, null, 0);
    }
    
    public static void enableTimeouts() {
        try {
            RMISocketFactory.setSocketFactory(new RMISocketFactory() {
                void setTimeouts(Socket socket) throws SocketException {
                    socket.setSoTimeout(DEFAULT_TIMEOUT_MS);
                    socket.setSoLinger(false, 0);
                    socket.setTcpNoDelay(true);
                }
                
                public Socket createSocket(String host, int port)
                        throws IOException {
                    Socket socket = new Socket();
                    setTimeouts(socket);
                    socket.connect(new InetSocketAddress(host, port), DEFAULT_TIMEOUT_MS);
                    return socket;
                }
    
                public ServerSocket createServerSocket(int port)
                        throws IOException {
                    return new ServerSocket(port) {
                        @Override
                        public Socket accept() throws IOException {
                            Socket socket = super.accept();
                            setTimeouts(socket);
                            return socket;
                        }
                    };
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
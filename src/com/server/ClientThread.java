package com.server;

import com.messages.BaseMessage;
import com.messages.MessageDispatcherDelegate;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;


public class ClientThread extends Thread {
    private boolean isRunning;
    private String clientId;
    private String clientName;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public Socket getSocket() {
        return socket;
    }

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private MessageDispatcherDelegate delegate;
    private boolean active;
    private InetAddress address;

    public InetAddress getAddress() {
        return address;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public ClientThread(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new ObjectOutputStream(this.socket.getOutputStream());
        this.in = new ObjectInputStream(this.socket.getInputStream());

    }

    public void setDispatcherDelegate(MessageDispatcherDelegate delegate) {
        this.delegate = delegate;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClientThread that = (ClientThread) o;

        if (isRunning != that.isRunning) return false;
        if (clientId != null ? !clientId.equals(that.clientId) : that.clientId != null) return false;
        if (clientName != null ? !clientName.equals(that.clientName) : that.clientName != null) return false;
        if (socket != null ? !socket.equals(that.socket) : that.socket != null) return false;
        if (out != null ? !out.equals(that.out) : that.out != null) return false;
        if (in != null ? !in.equals(that.in) : that.in != null) return false;
        return delegate != null ? delegate.equals(that.delegate) : that.delegate == null;
    }

    @Override
    public int hashCode() {
        int result = (isRunning ? 1 : 0);
        result = 31 * result + (clientId != null ? clientId.hashCode() : 0);
        result = 31 * result + (clientName != null ? clientName.hashCode() : 0);
        result = 31 * result + (socket != null ? socket.hashCode() : 0);
        result = 31 * result + (out != null ? out.hashCode() : 0);
        result = 31 * result + (in != null ? in.hashCode() : 0);
        result = 31 * result + (delegate != null ? delegate.hashCode() : 0);
        return result;
    }

    @Override
    public void run() {
        isRunning = true;
        while (isRunning) {
            try {
                BaseMessage message = (BaseMessage) in.readObject();
                delegate.onMessageReceived(message, clientId);
                Thread.sleep(10);
            } catch (InterruptedException | IOException | ClassNotFoundException e) {
                isRunning = false;
                delegate.removeClient(this.clientId);
            }
        }
        try {
            if (in != null)
                in.close();
            if (out != null) {
                out.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(BaseMessage message) throws IOException {
        out.writeObject(message);
    }

}

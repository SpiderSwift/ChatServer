package com.server;

import java.io.IOException;
import java.net.ServerSocket;

public class Server extends Thread {
    private boolean isRunning;
    private ServerSocket serverSocket;
    private MessageDispatcher dispatcher;

    public MessageDispatcher getDispatcher() {
        return dispatcher;
    }

    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        dispatcher = new MessageDispatcher();
    }

    @Override
    public void run() {
        isRunning = true;
        while (isRunning) {
            try {
                ClientThread client = new ClientThread(serverSocket.accept());
                dispatcher.registerClientThread(client);
                client.start();
                Thread.sleep(10);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                isRunning = false;
            }
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
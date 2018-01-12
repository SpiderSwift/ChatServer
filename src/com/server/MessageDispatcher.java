package com.server;

import com.messages.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class MessageDispatcher implements MessageDispatcherDelegate {

    private HashMap<String,ClientThread> clients;

    private MainController controller;

    public void setController(MainController controller) {
        this.controller = controller;
    }

    public MessageDispatcher() {
        this.clients = new HashMap<>();
    }

    @Override
    public void onMessageReceived(BaseMessage message, String... clientId) {
        switch (message.getType()) {
            case BaseMessage.MSG_TYPE_CLIENT :
                processClientMessage((ClientMessage) message ,clientId[0]);
                break;
            case BaseMessage.MSG_TYPE_CLIENT_GROUP :
                processClientGroupMessage((ClientGroupMessage) message);
                break;
            case BaseMessage.MSG_TYPE_PUBLIC_KEY_EXCHANGE :
                processPublicKeyExchangeMessage((PublicKeyExchangeMessage) message, clientId[0]);
                break;
            case BaseMessage.MSG_TYPE_INTRODUCE :
                processIntroduceMessage((IntroduceMessage) message, clientId[0]);
                break;
            case BaseMessage.MSG_TYPE_ERROR:
                processErrorMessage((ErrorMessage) message);
                break;
            case BaseMessage.MSG_TYPE_IN_OUT:
                processInOutMessage((ClientInOutMessage) message);
                break;
            case BaseMessage.MSG_TYPE_AVAILABLE:
                processAvailableMessage((AvailableMessage) message, clientId[0]);
                break;
            case BaseMessage.MSG_TYPE_DECISION:
                processDecisionMessage((DecisionMessage) message, clientId[0]);
                break;
            case BaseMessage.MSG_TYPE_SESSION:
                processSessionMessage((SessionMessage) message, clientId[0]);
                break;
            case BaseMessage.MSG_TYPE_INTERRUPT:
                processInterruptMessage((InterruptMessage) message, clientId[0]);
                break;
            case BaseMessage.MSG_TYPE_SESSION_END:
                processSessionEndMessage((SessionEndMessage) message, clientId[0]);
                break;
        }
    }

    private void processSessionEndMessage(SessionEndMessage message, String senderId) {
        ClientThread clientSender = clients.get(senderId);
        clientSender.setActive(true);
    }

    private void processInterruptMessage(InterruptMessage message, String senderId) {
        ClientThread receiverThread = clients.get(message.getInterruptId());
        ClientThread senderThread = clients.get(senderId);
        receiverThread.setActive(true);
        senderThread.setActive(true);
        try {
            receiverThread.sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processSessionMessage(SessionMessage message, String senderId) {
        //обрабатывается только на клиенте...
    }

    private void processDecisionMessage(DecisionMessage message, String senderId) {
        ClientThread clientReceiver = clients.get(message.getReceiverId());
        System.out.println(clientReceiver);
        ClientThread clientSender = clients.get(senderId);
        if (message.isDecision()) {
            SessionMessage forReceiver = new SessionMessage(clientSender.getAddress());
            SessionMessage forSender = new SessionMessage(clientReceiver.getAddress());
            try {
                clientSender.sendMessage(forSender);
                clientReceiver.sendMessage(forReceiver);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            clientReceiver.setActive(true);
            clientSender.setActive(true);
            try {
                clientReceiver.sendMessage(new SessionEndMessage());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void processAvailableMessage(AvailableMessage message, String senderId) {
        ClientThread clientReceiver = clients.get(message.getClientId());
        ClientThread clientSender = clients.get(senderId);
        if (clientReceiver.isActive()) {
            clientReceiver.setActive(false);
            clientSender.setActive(false);
            message.setAvailable(true);
            DecisionMessage decisionMessage = new DecisionMessage(false, senderId);
            try {
                clientReceiver.sendMessage(decisionMessage);
                clientSender.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            message.setAvailable(false);
            try {
                clientSender.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }



    private void processIntroduceMessage(IntroduceMessage message, String clientId) {
        ClientThread client = clients.get(clientId);
        controller.addLog(message.getClientName() + " is trying to connect\n");
        client.setClientName(message.getClientName());
        message.setClients(hashMapToArrayList(clientId));
        try {
            controller.addLog("Sending back\n");
            client.sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final ClientInOutMessage inMessage = new ClientInOutMessage(true, client.getClientId(),
                                                                    new OnlineClient(client.getClientName(),client.getClientId()));
        processInOutMessage(inMessage);
        client.setActive(true);

    }



    private void processPublicKeyExchangeMessage(PublicKeyExchangeMessage message, String clientId) {
        message.setSenderId(clientId);
        ClientThread client = clients.get(message.getRecipientId());
        try {
            client.sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processClientGroupMessage(ClientGroupMessage message) {
        //to a group
        //might not be implemented
        for (String clientId: message.getRecipientId()) {
            ClientThread client = clients.get(clientId);
            try {
                client.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void processClientMessage(ClientMessage message, String clientId) {
        //from 1 client to another
        message.setSenderId(clientId);
        ClientThread client = clients.get(message.getRecipientId());
        try {
            client.sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
            removeClient(message.getRecipientId());
        }
    }

    private void processErrorMessage(ErrorMessage message) {

    }

    private void processInOutMessage(ClientInOutMessage message) {
        Thread thr = new Thread(new Runnable() {
            @Override
            public void run() {
                sendToEveryone(message, message.getClientId());
            }
        });
        thr.start();
    }

    @Override
    public void removeClient(String clientId) {
        ClientThread client = clients.get(clientId);
        controller.addLog(client.getClientName() + " " + client.getClientId() + " is getting removed\n");
        clients.remove(clientId);
        final ClientInOutMessage outMessage = new ClientInOutMessage(false, clientId, null);
        processInOutMessage(outMessage);
    }

    public void registerClientThread(ClientThread client) {
        client.setDispatcherDelegate(this);
        client.setClientId(String.valueOf(System.currentTimeMillis()));
        client.setAddress(client.getSocket().getInetAddress());
        clients.put(client.getClientId(), client);
    }

    private ArrayList<OnlineClient> hashMapToArrayList(String clientId) {
        ArrayList<OnlineClient> list = new ArrayList<>();
        for (String key : clients.keySet()) {
            if (!Objects.equals(key, clientId)) {
                ClientThread client = clients.get(key);
                list.add(new OnlineClient(client.getClientName(), client.getClientId()));
            }
        }
        return list;
    }

    private void sendToEveryone(BaseMessage message, String clientId) {
        for (String key : clients.keySet()) {
            try {
                if (!Objects.equals(key, clientId)) {
                    ClientThread client = clients.get(key);
                    client.sendMessage(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

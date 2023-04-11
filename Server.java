import java.io.*;
import java.net.*;
import java.util.Vector;


public class Server{
//    DataInputStream[] input= new DataInputStream[2];
//    DataOutputStream[] output= new DataOutputStream[2];
    Vector<ClientHandler> clientList;
    boolean exit;
    int numUsers;
    int nextId;
    final int maxConnected=1;
    public static void main(String[] args) {
        try {
            Server server = new Server();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    Server() throws IOException, InterruptedException {
        clientList= new Vector<>();
        numUsers=0;
        nextId=0;
        exit=false;

        runServer();
    }

    private void runServer() throws IOException {
        ServerSocket server = new ServerSocket(80, 2);
        System.out.println("Server created. Waiting for clients...");

        while(!exit){
            Socket connection = server.accept();
            DataOutputStream output=new DataOutputStream(connection.getOutputStream());
            DataInputStream input=new DataInputStream(connection.getInputStream());

            if(numUsers+1<=maxConnected){
                numUsers++;
                output.writeUTF(MessageCodes.SERVER_OPEN);

                String name=input.readUTF();
                System.out.println(name+" is connected");

                ClientHandler handler= new ClientHandler(nextId,input,output,name);
                handler.start();

                //Sends a message to all that a new user is connected
                sendMessageToAllUsers(nextId,MessageCodes.NEW_CONNECTED,name);

                clientList.add(handler);
                nextId++;
            }
            else{
                output.writeUTF(MessageCodes.SERVER_CLOSED);
                output.flush();
            }
        }
    }

    public void sendMessageToAllUsers(int senderId, String info,String message ) throws IOException {
        for(int i=0; i<clientList.size();i++){
            if(i!=senderId && clientList.get(i).connected){
                clientList.get(i).sendMessage(info,message);
                System.out.println(message+" was sent to user "+clientList.get(i).id);
            }
        }
    }

    public void sendMessageToSpecific(int recipientId, String info,String message) throws IOException {
        clientList.get(recipientId).sendMessage(info,message);
        System.out.println(message+" was sent to user "+clientList.get(recipientId).id);
    }
    class ClientHandler extends Thread{
        DataInputStream inputStream;
        DataOutputStream outputStream;
        String name;
        int id;
        boolean connected;

        ClientHandler(int id,DataInputStream is, DataOutputStream os, String name){
            connected=true;
            this.id=id;
            inputStream=is;
            outputStream=os;
            this.name=name;
        }

        @Override
        public void run() {
            try {
                initMessage();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String message;
            do{
                try {
                    message=inputStream.readUTF();
                    processMessage(message);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            while(connected);

            numUsers--;
        }

        private void processMessage(String message) throws IOException {
            if(message.equals(MessageCodes.USER_EXIT)){
                System.out.println(name +" disconnected ");
                outputStream.writeUTF(MessageCodes.USER_EXIT);
                connected=false;
            }else if(message.startsWith(MessageCodes.PRIVATE_MESSAGE)){
                String privateRecipientName=message.substring(9);
                int recipientId;
                if((recipientId=getId(privateRecipientName,name))!=-1){
                    sendMessageToSpecific(recipientId,MessageCodes.PRIVATE_MESSAGE,message);
                }
                else{
                    sendMessage(MessageCodes.ERROR_MESSAGE,privateRecipientName);
                }
            }
            else {
                System.out.println(message +" received from "+name);
                sendMessageToAllUsers(id,name,message);
            }

        }

        private void initMessage() throws IOException {
            String message="Welcome to the chat room "+name+"!";
            sendMessage(MessageCodes.INIT_MESSAGE,message);

            message="Users connected: "+numUsers;
            sendMessage(MessageCodes.INIT_MESSAGE,message);
            sendMessage(MessageCodes.INIT_MESSAGE,commandGuide());
        }

        private String commandGuide() {
            return"Commands:\n  *exit: exit the program\n" +
                    "  *private-user: send a private message to a user\n";
        }

        private int getId(String recipientName, String senderName){
            int recipientId=-1;

            for (ClientHandler clientHandler : clientList) {
                if (clientHandler.name.equals(recipientName) && !clientHandler.name.equals(senderName) ) {
                    recipientId = clientHandler.id;
                    break;
                }
            }
            return recipientId;
        }
        public void sendMessage(String info, String message) throws IOException {
            outputStream.writeUTF(info);
            outputStream.flush();
            if(info.equals(MessageCodes.INIT_MESSAGE)){
                outputStream.writeUTF(message);
                outputStream.flush();
                System.out.println(message +" sent to "+name);
            }
            else if(info.equals(MessageCodes.NEW_CONNECTED)){
                outputStream.writeUTF("("+message+" just connected)");
                outputStream.flush();
            }
            else if(info.equals(MessageCodes.ERROR_MESSAGE)){
                outputStream.writeUTF(message+" is not connected");
                outputStream.flush();
            }
            else if(!info.equals(MessageCodes.SERVER_EXIT)){
                outputStream.writeUTF(message);
                outputStream.flush();
                System.out.println(message +" sent from "+info+"to "+name);
            }
        }
    }
}

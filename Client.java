import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Scanner;

public class Client{
    private DataInputStream in;
    private DataOutputStream out;
    private String name;
    private boolean exit;


    public static void main(String [] args){
        try {
            Client client= new Client();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    Client() throws IOException, InterruptedException {
        exit=false;
        String serverAcceptance;
        getStreams();

        serverAcceptance=in.readUTF();
        if(serverAcceptance.equals(MessageCodes.SERVER_OPEN)){
            enterUserInfo();
            Handler handler= new Handler();
            handler.start();


            Scanner userInput= new Scanner(System.in);

            while(!exit){
                String message=userInput.nextLine();
                sendMessage(message);
                if(message.equals(MessageCodes.USER_EXIT)){
                    exit=true;
                }
            }
        }
        else{
            System.out.println("Chat room is full, try again later...");
        }
    }

    private void getStreams(){
        Socket connection= null;
        try {
            connection = new Socket("localhost", 80);
        } catch (IOException e) {
            System.err.println("Could not connect to server");
        }


        try {
            out= new DataOutputStream(connection.getOutputStream());
            in= new DataInputStream(connection.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    private void enterUserInfo() throws IOException {
        Scanner userIn= new Scanner(System.in);
        System.out.print("Enter your username: ");
        name=userIn.nextLine();
    }

    private void sendMessage(String message) throws IOException {
        out.writeUTF(message);
        out.flush();
    }


    class Handler extends Thread{
        @Override
        public void run() {
            try {
                out.writeUTF(name);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String info;

            while(!exit){
                try {
                    info=in.readUTF();
                    processMessage(info);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        }

        private void processMessage(String info) throws IOException {
            if(info.equals(MessageCodes.SERVER_EXIT)||info.equals(MessageCodes.USER_EXIT)){
                userExit();
            }
            else if(info.equals(MessageCodes.INIT_MESSAGE) ||info.equals(MessageCodes.NEW_CONNECTED)){
                String initMessage;
                initMessage=in.readUTF();
                System.out.println(initMessage);
            }
            else{
                String message=in.readUTF();

                System.out.println(info+": "+message);
            }
        }

        private void userExit() throws IOException {
            exit=true;
            System.out.println("Goodbye "+name+"!");
            out.close();
            in.close();
        }
    }

}

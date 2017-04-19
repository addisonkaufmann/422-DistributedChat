import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import javax.swing.JOptionPane;

public class ChatServer {

	public static int PORT_NUMBER = 4004;

	public static void main(String[] args) {
		new ChatServer();
	}

	private ArrayList<ObjectOutputStream> clientOutputStreams;
	private ArrayList<String> users;

	public ChatServer() {
		clientOutputStreams = new ArrayList<ObjectOutputStream>();
		users = new ArrayList<String>();
		try {
			@SuppressWarnings("resource")
			ServerSocket serverSock = new ServerSocket(PORT_NUMBER);
			while (true) {
				Socket clientSocket = serverSock.accept();
				ObjectOutputStream writer = new ObjectOutputStream(clientSocket.getOutputStream());
				clientOutputStreams.add(writer);
				
				Thread t = new Thread(new ReadInputThread(clientSocket, users));
				t.start();
				System.out.println("got a connection");
			}
		} catch (Exception ex) {
		}
	}
	
	private class ReadInputThread implements Runnable {

		ObjectInputStream reader;
		Socket sock;
		ArrayList<String> users; 

		public ReadInputThread(Socket clientSocket, ArrayList<String> users) {
			this.users = users;
			try {
				sock = clientSocket;
				if (sock == null)
					JOptionPane.showMessageDialog(null, "socket is null");
				reader = new ObjectInputStream(sock.getInputStream());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		@Override
		public void run() {

			String message;
			try {

//				while (true) {
					// Wait for the client send a writeObject message to the server
					message = (String) reader.readObject();
					users.add(message);
					System.out.println(users.toString());
					// Send the same message from the server to all clients
					tellEveryone(message);
//				}
			} catch (Exception ex) {

			}
		}

		// Send the same message to all clients
		public void tellEveryone(String message) {
			for (ObjectOutputStream output : clientOutputStreams) {
				try {
					output.writeObject(users);
					output.flush();
				} catch (Exception ex) {
					clientOutputStreams.remove(output);
				}
			}
		}
	}

}
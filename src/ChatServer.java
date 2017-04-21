import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JOptionPane;

public class ChatServer {

	public static int PORT_NUMBER = 4004;

	public static void main(String[] args) {
		new ChatServer();
	}

	private ArrayList<ObjectOutputStream> clientOutputStreams;
	private Vector<String> users;

	public ChatServer() {
		clientOutputStreams = new ArrayList<ObjectOutputStream>();
		users = new Vector<String>();
		try {
			@SuppressWarnings("resource")
			ServerSocket serverSock = new ServerSocket(PORT_NUMBER);
			while (true) {
				Socket clientSocket = serverSock.accept();
				ObjectOutputStream writer = new ObjectOutputStream(clientSocket.getOutputStream());
				clientOutputStreams.add(writer);
				
				Thread t = new Thread(new ReadInputThread(clientSocket));
				t.start();
				System.out.println("got a connection");
			}
		} catch (Exception ex) {
		}
	}
	
	private class ReadInputThread implements Runnable {

		ObjectInputStream reader;
		Socket sock;

		public ReadInputThread(Socket clientSocket) {
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
					users = new Vector<String>(users); 
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
					System.out.println("writing " + users.toString());
					output.writeObject(users);
					output.flush();
				} catch (Exception ex) {
					clientOutputStreams.remove(output);
				}
			}
		}
	}

}
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
// ufkc u too
	private ArrayList<ObjectOutputStream> clientOutputStreams;
	private VectorListModel<String> users;

	public ChatServer() {
		clientOutputStreams = new ArrayList<ObjectOutputStream>();
		users = new VectorListModel<String>();
		try {
			@SuppressWarnings("resource")
			ServerSocket serverSock = new ServerSocket(PORT_NUMBER);
			while (true) {
				Socket clientSocket = serverSock.accept();
				ObjectOutputStream writer = new ObjectOutputStream(clientSocket.getOutputStream());
				clientOutputStreams.add(writer);
				
				Thread t = new Thread(new ReadInputThread(clientSocket, writer));
				t.start();
				System.out.println("got a connection");
			}
		} catch (Exception ex) {
		}
	}
	
	private class ReadInputThread implements Runnable {

		ObjectInputStream reader;
		ObjectOutputStream writer;
		Socket sock;

		public ReadInputThread(Socket clientSocket, ObjectOutputStream writer) {
			try {
				sock = clientSocket;
				if (sock == null)
					JOptionPane.showMessageDialog(null, "socket is null");
				reader = new ObjectInputStream(sock.getInputStream());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			this.writer = writer;
		}

		@Override
		public void run() {

			String message;
			try {

//				while (true) {
					// Wait for the client send a writeObject message to the server
					message = (String) reader.readObject();
					users.add(message);
					users = new VectorListModel<String>(users); 
					System.out.println(users.toString());
					// Send the same message from the server to all clients
					tellEveryone();
					
					// Wait for closing of the chat client
					message = (String) reader.readObject(); 
					System.out.println(message);
					for (String user : users) {
						System.out.println(users);
						if (user.equals(message)) {
							System.out.println("Found user to remove");
							users.remove(user);
							break;
						}
					}
					users = new VectorListModel<String>(users);
					clientOutputStreams.remove(this.writer);
					tellEveryone();
//				}
			} catch (Exception ex) {

			}
		}

		// Send the same message to all clients
		public void tellEveryone() {
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
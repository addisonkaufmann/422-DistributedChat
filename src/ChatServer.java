import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JOptionPane;

/**
 * This class runs a server for a distributed chat client. The server should be started first
 * before any ChatClient instances. The server accepts new client connections and informs all other 
 * currently connected clients of a new clients and of clients disconnecting.
 * @author Aaron & Addison
 *
 */
public class ChatServer {

	public static int PORT_NUMBER = 4004;

	public static void main(String[] args) {
		new ChatServer();
	}

	private ArrayList<ObjectOutputStream> clientOutputStreams;
	private Vector<User> users;

	public ChatServer() {
		clientOutputStreams = new ArrayList<ObjectOutputStream>();
		users = new Vector<User>();
		try {
			@SuppressWarnings("resource")
			ServerSocket serverSock = new ServerSocket(PORT_NUMBER);
			
			// Continuously accept new clients
			while (true) {
				Socket clientSocket = serverSock.accept();
				System.out.println(clientSocket.getPort());
				ObjectOutputStream writer = new ObjectOutputStream(clientSocket.getOutputStream());
				clientOutputStreams.add(writer);
				
				// Create new thread to handle the newest client
				Thread t = new Thread(new ReadInputThread(clientSocket, writer)); 
				t.start();
				System.out.println("got a connection");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	/**
	 * This class is a thread that interacts with one specific client. It receives the client's username
	 * and then informs the other chat clients of the new user. It then waits for the client to
	 * send a message that it is disconnecting. Once received, the thread removes this client's connection
	 * from the list of connections.
	 *
	 */
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
				// Tell the new client its port
				writer.writeObject(sock.getPort());
				System.out.println("Told them their port");
				// Wait for the client to send back it's new User object with their name
				User u = (User) reader.readObject();
				System.out.println("Got back their user object");
				users.add(u);
				users = new Vector<User>(users); 
				System.out.println(users.toString());
				// Send the same message from the server to all clients
				tellEveryone();
				
				// Wait for closing of the chat client
				message = (String) reader.readObject(); 
				System.out.println(message);
				for (User user : users) {
					System.out.println(users);
					if (user.getName().equals(message)) {
						System.out.println("Found user to remove");
						users.remove(user);
						break;
					}
				}
				users = new Vector<User>(users); 
				clientOutputStreams.remove(this.writer);
				tellEveryone();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		/**
		 * This method loops through each client sending them the updates list of users.
		 */
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
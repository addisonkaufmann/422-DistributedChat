import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListModel;

public class ChatClient extends JFrame {

	public static void main(String[] args) {
		new ChatClient();
	}

	private String name;
	private boolean firstEntry = true;
	private JTextField outgoing;
	private ObjectOutputStream writer;
	private ObjectInputStream inputFromServer;
	private Socket socketServer;
	public static String host = "localhost";

	private JTextArea text;
	private JList<String> userList = new JList<>();
	private Vector<User> users;
	private int port;

	public ChatClient() {
		setTitle("Chat Client");
		setSize(380, 480);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new ListenForWindowClose());
		Container cp = getContentPane();
		cp.setLayout(new FlowLayout());

		outgoing = new JTextField("Replace me with your name");
		outgoing.addActionListener(new InputFieldListener());
		cp.add(outgoing);
		
		users = new Vector<User>();

//		inputFromServerTextArea = new JTextArea();
		text = new JTextArea();
		cp.add(userList);
		cp.add(text);

//		JScrollPane scroller = new JScrollPane(inputFromServerTextArea);
//		scroller.setSize(300, 400);
//		scroller.setLocation(30, 40);
//		scroller.setBackground(Color.WHITE);
//		cp.add(scroller);

		setVisible(true);
		outgoing.requestFocus();
		makeConnectionAndReadAllServerOutputFromServer();
	}

	private void makeConnectionAndReadAllServerOutputFromServer() {
		try {
			// host could be "localhost", port could be 4000
			socketServer = new Socket(host, ChatServer.PORT_NUMBER);
			writer = new ObjectOutputStream(socketServer.getOutputStream());
			inputFromServer = new ObjectInputStream(socketServer.getInputStream());
			System.out.println("Found server who accepted me");
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(null,
					"Could not find server at " + host + " on port " + ChatServer.PORT_NUMBER);
			System.exit(0);
		}
		
		String message;
		try {
			port = (int) inputFromServer.readObject(); // Get this client's port from server
			System.out.println("received my port: " + port);
			while (true) {
				Vector<User> temp = (Vector<User>) inputFromServer.readObject();
				users = new Vector<User>(temp);
				text.append(users.toString() + "\n");
				System.out.println(users.toString());
				
			}
		} catch (Exception ex) {
			try {
				// Attempt to gracefully close the connection with the server 
				// by telling it this client is logging off
				writer.writeObject(this.name);
			} catch (IOException e) {
				System.out.println("Couldn't gracefully close connection.");
				e.printStackTrace();
			}
			System.out.println("Client lost server");
		}
	}

	public class InputFieldListener implements ActionListener {
		// Precondition: This client has successfully connected to
		// a server and writer is a reference to the server's output stream.
		public void actionPerformed(ActionEvent ev) {

			try {
				if (firstEntry) {
					name = outgoing.getText();
					firstEntry = false;
					System.out.println("test");
					writer.writeObject(new User(name, port));
					System.out.println("Sent my user object");
				} else
					// Don't do this . . . should be done in separate thread with another client
					//writer.writeObject(name + ": " + outgoing.getText());
				writer.flush();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			outgoing.setText("");
			outgoing.requestFocus();
		}
	}
	
	
	private class ListenForWindowClose extends WindowAdapter {

		@Override
		public void windowClosing(WindowEvent e) {
			try {
				writer.writeObject(ChatClient.this.name);  // Notify the server that this client is closing.
			} catch (IOException e1) {
				e1.printStackTrace();
			} finally {
				System.exit(0);
			}
		}
	}
}
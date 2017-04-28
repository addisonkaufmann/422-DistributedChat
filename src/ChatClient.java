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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListModel;

public class ChatClient extends JFrame {

	public static void main(String[] args) {
		new ChatClient();
	}

	private User me = null;
	private JTextField nameEntry;
	private ObjectOutputStream writer;
	private ObjectInputStream inputFromServer;
	private Socket socketServer;
	public static String host = "localhost"; //ipconfig -- wireless ac network controller, virtual switch 192.168.0.3

	private JTextArea text;
	private ArrayList<JCheckBox> userBoxes;
	private JPanel boxesPanel;
	private JButton createButton;
	private SelectionListener selectListener;

	private Vector<User> users;
	private int port;
	private String name;

	public ChatClient() {
		setTitle("Chat Client");
		setSize(380, 480);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new ListenForWindowClose());
		Container cp = getContentPane();
		cp.setLayout(new FlowLayout());

		nameEntry = new JTextField("Replace me with your name");
		nameEntry.addActionListener(new InputFieldListener());
		cp.add(nameEntry);
		

		users = new Vector<User>();

//		inputFromServerTextArea = new JTextArea();
		text = new JTextArea();
//		cp.add(userList);
		cp.add(text);
		
		selectListener = new SelectionListener();
		boxesPanel = new JPanel();
		userBoxes = new ArrayList<>();
		updateBoxes();
		createButton = new JButton("Create Chat");
		createButton.setEnabled(false);
		createButton.addActionListener(new createChatListener());
		cp.add(boxesPanel); 
		cp.add(createButton);

//		JScrollPane scroller = new JScrollPane(inputFromServerTextArea);
//		scroller.setSize(300, 400);
//		scroller.setLocation(30, 40);
//		scroller.setBackground(Color.WHITE);
//		cp.add(scroller);

		setVisible(true);
		nameEntry.requestFocus();
		makeConnectionAndReadAllServerOutputFromServer();
	}
	
	private class AcceptChats implements Runnable {

		@Override
		public void run() {
			ServerSocket serverSock;
			try {
				serverSock = new ServerSocket(0);
				me = new User(name, serverSock.getLocalPort());
				while (true) {
					Socket clientSocket = serverSock.accept();
					System.out.println("Accepted a chat");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	private void updateBoxes(){
		boxesPanel.removeAll();
		userBoxes.clear();
		
		if (me == null) {
			return;
		}
		else if (me.getName().equals("")) {
			return;
		}
		
		for( User user : users){
			if (user.getName().equals(me.getName())) {
				continue;
			}
			JCheckBox box = new JCheckBox(user.getName());
			box.addActionListener(selectListener);
			userBoxes.add(box);
			boxesPanel.add(box);
		}
		

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
				updateBoxes();
				text.append(users.toString() + "\n");
				System.out.println(users.toString());
				
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			try {
				// Attempt to gracefully close the connection with the server 
				// by telling it this client is logging off
				writer.writeObject(me.getName());
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
				name = nameEntry.getText();
				Thread acceptConnections = new Thread(new AcceptChats());
				acceptConnections.start();
				while (me == null);
				writer.writeObject(me);
				System.out.println("Sent my user object");
				writer.flush();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			Container cp = getContentPane();
			cp.remove(nameEntry);
			cp.repaint();
			
		}
	}
	
	
	private class ListenForWindowClose extends WindowAdapter {

		@Override
		public void windowClosing(WindowEvent e) {
			try {
				writer.writeObject(ChatClient.this.me.getName());  // Notify the server that this client is closing.
			} catch (IOException e1) {
				e1.printStackTrace();
			} finally {
				System.exit(0);
			}
		}
	}
	
	private class SelectionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			System.out.println("action");
			createButton.setEnabled(true);
			for(JCheckBox box: userBoxes){
				if (box.isSelected()){
					return;
				}
			}
			createButton.setEnabled(false);
		}
	}
	
	private class createChatListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			ArrayList<String> userNames = new ArrayList<String>();
			String names = "";
			for (JCheckBox box: userBoxes){
				if (box.isSelected()){
					for (User u : users) {
						if (u.getName().equals(box.getText())) {
							Thread newChat = new Thread(new ChatConnection(u));
							newChat.start();
						}
					}
					userNames.add(box.getText());
					names += box.getText() + ", ";
					
				}
			}
			
			
			System.out.println("Created a chat with " + names);
		}
		
	}
	
	private class ChatConnection implements Runnable {
		
		private User user;
		
		public ChatConnection(User u) {
			this.user = u;
		}

		@Override
		public void run() {
			try {
				socketServer = new Socket(host, user.getPort());
				writer = new ObjectOutputStream(socketServer.getOutputStream());
				inputFromServer = new ObjectInputStream(socketServer.getInputStream());
				System.out.println("Connected to " + user.getName());
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
	}
}

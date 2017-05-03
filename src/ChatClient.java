import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

/**
 * The ChatClient class requires a ChatServer to be running to connect to. The host should be changed
 * to reflect the location of the server. This class allows user to enter a username and begin chatting with
 * other connected individuals as well as start group chats. Duplicate usernames are not allowed, and duplicate
 * chats containing the exact same members are not allowed.
 * A particular chat will close when a single participant disconnects.
 * 
 * Implementation:
 * When the program starts, chatclient X waits for a name input. When it's given, X's name is sent to the server, and
 * then out to all other users so X is now visible. A thread is started in X to read these user list updates from the server
 * continually (and update the UI).
 *  
 * Now, X can start chats with others. This is handled through a buttonlistener on the "create chat" button,
 * which starts a chatconnection thread. The chatConnection initializes the connection, and creates a reader thread, which reads
 * input from the chat peer and manage the UI. If X selects multiple users when creating the chat, X becomes the "host" of the 
 * group chat. The group members send messages to X, which X broadcasts to all other members.  
 *  
 * X could also be added to a chat by another user. This is handled through the acceptChats thread, which starts when X makes
 * server connection. acceptChats continually accepts connections from other users, and creates a reader thread to handle
 * the chatting. If X is part of a group chat, but not the host, the behaviour is mostly the same as for a one-on-one chat, in
 * that X is only interacting directly with the host.
 *  
 * @author Aaron & Addison
 *
 */
public class ChatClient extends JFrame {

	public static void main(String[] args) {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		try {
			ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("Roboto-Condensed.ttf")));
		} catch (Exception e) {
			e.printStackTrace();
		}
		new ChatClient();
	}

	private User me = null;
	private JTextField nameEntry;
	private ObjectOutputStream writer;
	private ObjectInputStream inputFromServer;

	private Socket socketServer;
	public static String host = "localhost"; //ipconfig -- wireless ac network controller, virtual switch 192.168.0.3
	private String myIP;
	
	private JTextArea text;
	private ArrayList<JCheckBox> userBoxes;
	private JPanel boxesPanel;
	private JButton createButton;
	private SelectionListener selectListener;
	private JTabbedPane chatPane;
	private Vector<User> users;
	private int port;
	private String name;
	private Font textFont;
	private ArrayList<JComponent> chatPanels;

	
	/**
	 * Constructor
	 * Initializes the gui elements, and calls serverConnect
	 */
	public ChatClient() {
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Exception e1) {
			System.out.println("Error loading theme");
			e1.printStackTrace();
		}

		setTitle("Chat Client");
		setSize(380, 480);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new ListenForWindowClose());
		Container cp = getContentPane();
		cp.setLayout(new BorderLayout());
		
		try {
			myIP = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		textFont = new Font("Roboto Condensed", Font.PLAIN, 14);
		
		nameEntry = new JTextField("Your name");
		nameEntry.addActionListener(new InputFieldListener());
		nameEntry.setFont(textFont);
		cp.add(nameEntry, BorderLayout.PAGE_START);
		
		
		users = new Vector<User>();

		text = new JTextArea();
		
		selectListener = new SelectionListener();
		
		boxesPanel = new JPanel();
		boxesPanel.setBackground(Color.WHITE);
		boxesPanel.setLayout(new BoxLayout(boxesPanel, BoxLayout.PAGE_AXIS));

		chatPane = new JTabbedPane();
		chatPanels = new ArrayList<>();
		chatPane.setBackground(Color.WHITE);
		chatPane.setFont(textFont);
		cp.add(chatPane, BorderLayout.CENTER);
		
		userBoxes = new ArrayList<>();
		updateBoxes();
		createButton = new JButton("Create Chat");
		createButton.setEnabled(false);
		createButton.addActionListener(new createChatListener());
		createButton.setFont(textFont);
		cp.add(boxesPanel, BorderLayout.LINE_START);


		setVisible(true);
		nameEntry.requestFocus();
		serverConnect();
	}
	
	/**
	 * A thread that continually accepts chat connections from peers.
	 * When it accepts the socket, it creates a read and write stream, 
	 * then creates a chatreader thread to manage the IO of the chat. 
	 *
	 */
	private class AcceptChats implements Runnable {
		//receiving the chat request
		@Override
		public void run() {
			ServerSocket serverSock;
			try {
				serverSock = new ServerSocket(0);
				me = new User(name, myIP, serverSock.getLocalPort());
				while (true) {
					Socket peerSocket = serverSock.accept();
										
					ObjectOutputStream newChatWriterStream = new ObjectOutputStream(peerSocket.getOutputStream());
					newChatWriterStream.writeObject(me.getName());
					newChatWriterStream.flush();
					
					ObjectInputStream newChatReaderStream = new ObjectInputStream(peerSocket.getInputStream());
					
					System.out.println("Accepted a chat");

					ChatReader reader = new ChatReader(newChatReaderStream, newChatWriterStream);
					Thread t = new Thread(reader);
					t.start();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * Updates the checkboxes representing the currently
	 * online users.
	 */
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
			box.setFont(textFont);
			box.setOpaque(false);
			box.addItemListener(selectListener);
			userBoxes.add(box);
			boxesPanel.add(box);
		}
		boxesPanel.add(createButton);
		boxesPanel.repaint();
		boxesPanel.updateUI();
		

	}

	/**
	 *	Makes the connection to the server, and loops through reading
	 *	the vector of new users that have logged in, or users that have
	 *	left the chat. Calls updateboxes() to update the gui based on 
	 *	new users.
	 */
	private void serverConnect() {
		try {
			socketServer = new Socket(host, ChatServer.PORT_NUMBER);
			writer = new ObjectOutputStream(socketServer.getOutputStream());
			inputFromServer = new ObjectInputStream(socketServer.getInputStream());
			System.out.println("Found server who accepted me");
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(null,
					"Could not find server at " + host + " on port " + ChatServer.PORT_NUMBER);
			System.exit(0);
		}
		
		try {
			port = (int) inputFromServer.readObject(); // Get this client's port from server
			System.out.println("received my port: " + port);
			while (true) {
				@SuppressWarnings("unchecked")
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

	
	/**
	 * Listener for the initial "your name" textfield. Simply writes
	 * the name to the server. If the name exists within the current
	 * vector of users, it warns the user and does not send the name.
	 *
	 */
	public class InputFieldListener implements ActionListener {
		public void actionPerformed(ActionEvent ev) {

			try {
				name = nameEntry.getText();
				for (User u : users) {
					if (u.getName().equals(name)) {
						nameEntry.setText("Name taken. Please replace with a different username.");
						return;
					}
				}
				setTitle("Chat Client: " + name);
				System.out.println("My name is " + name);
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
	
	/**
	 * Input listener for the chatfield. This method gets text from the chat
	 * box, then sends it to the chat peer (or group host). It writes the message
	 * to it's own chatbox, unless it is a group receiver, then it will not 
	 * write until it receives the message back from the group host.
	 *
	 */
	private class chatInputListener implements ActionListener {
		private ObjectOutputStream writer = null;
		private Vector<ObjectOutputStream> allWriters = null;
		private JTextArea chatArea;
		private boolean isGroupHost = false;
		private boolean isGroupReceiver;
		
		public chatInputListener(ObjectOutputStream writer, JTextArea area, boolean isGroupReceiver){
			this.writer = writer;
			this.chatArea = area;
			this.isGroupReceiver = isGroupReceiver;
		}
		
		public chatInputListener(Vector<ObjectOutputStream> writers, JTextArea area){
			this.allWriters = writers;
			this.chatArea = area;
			isGroupHost = true;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			JTextField inputField = (JTextField)e.getSource();
			String message = me.getName() + ": " + inputField.getText() + "\n";
			inputField.setText("");
			try {
				if (isGroupHost){
					chatArea.append(message); //groupHost
					for (ObjectOutputStream writer : allWriters){
						writer.writeObject(message);
						writer.flush();
					}
				} else {
					if (!isGroupReceiver){
						chatArea.append(message);
					}
					writer.writeObject(message);
					writer.flush();
				} 
			} catch (IOException e1) {
				System.out.println("Error in chatInputListener");
				e1.printStackTrace();
			}
		}
		
	}
	
	/**
	 * Attempts to notify the server that the client is disconnected before the application is closed.
	 *
	 */
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
	
	
	/**
	 * Listens for selections in the list of user checkboxes,
	 * when at least one box is selected, the create chat button
	 * will be enabled.
	 *
	 */
	private class SelectionListener implements ItemListener {

		@Override
		public void itemStateChanged(ItemEvent arg0) {
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
	
	/**
	 * Listens for a "create chat" button click and starts a new
	 * ChatConnection thread to manage the chat.
	 *
	 */
	private class createChatListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			Vector<User> newChatUsers = new Vector<User>();
			String names = "";
			for (JCheckBox box: userBoxes){
				if (box.isSelected()){
					names += box.getText() + ", ";
					box.setSelected(false);
					for (User u : users) {	
						if (u.getName().equals(box.getText())) {
							newChatUsers.add(u);
						}
					}	
				}
			}
			names = names.substring(0, names.length()-2);
			for (int i = 0; i < chatPane.getTabCount(); i++){
				if (names.equals(chatPane.getTitleAt(i))){
					JOptionPane.showMessageDialog(null,"You already have a chat open with those members");
					return;
				}
			}
			
			Thread newChat = new Thread(new ChatConnection(newChatUsers));
			newChat.start();
			
			
			System.out.println("Creating a chat with " + names);
		}
		
	}
	
	/**
	 * Returns a new JPanel to add to the jtabbed pane, 
	 * when a new chat gets created. The JPanel
	 * contains an inputfield and a textarea to hold
	 * the chat history. 
	 * @return
	 */
	private JPanel newChatPanel() {
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		JTextArea chatText = new JTextArea("");
		chatText.setEditable(false);
		chatText.setFont(textFont);
		JTextField inputBox = new JTextField("Type here");
		inputBox.setFont(textFont);
		inputBox.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				inputBox.setText("");
			}

			@Override
			public void focusLost(FocusEvent e) {}	
		});
		p.add(chatText, BorderLayout.CENTER);
		p.add(inputBox, BorderLayout.PAGE_END);
		return p;
	}
	
	/**
	 * Chat connection represents a single a tab of the gui (i.e. single or group chat).
	 * It initializes the direct peer connection either between two users, or between
	 * a group host and group members. It maintains a vector of the users, and their respective
	 * IO streams. After initializing these vectors, it creates a chatreader thread to manage
	 * the IO of the chat. 
	 *
	 */
	private class ChatConnection implements Runnable {
		//initiating the chat
		
		private Vector<User> chatUsers;
		private boolean isGroup;
		private Vector<ObjectOutputStream> chatWriterStreams;
		private Vector<ObjectInputStream> chatReaderStreams;
		private JPanel groupChatPanel;
		
		public ChatConnection(Vector<User> u) {
			this.chatUsers = u;
			this.isGroup = u.size() > 1;
			this.groupChatPanel = newChatPanel();

			chatWriterStreams = new Vector<ObjectOutputStream>();
			chatReaderStreams = new Vector<ObjectInputStream>();
		}
		
		public String getOtherNames(User current){
			String s = me.getName() + ", ";
			for (User u: chatUsers){
				if (!u.equals(current)){
					s += u.getName() + ", ";
				}
			}
			return s.substring(0, s.length() - 2);
		}

		@Override
		public void run() {
			for (User user : chatUsers){
				System.out.println("building connection to " + user.getName() + ", out of " + chatUsers.size() + " users");
				try {
					Socket peerSocket = new Socket(user.getIP(), user.getPort());
					
					ObjectOutputStream newChatWriterStream = new ObjectOutputStream(peerSocket.getOutputStream());
					chatWriterStreams.add(newChatWriterStream);
					newChatWriterStream.writeObject(getOtherNames(user));
					newChatWriterStream.flush();					
					ObjectInputStream newChatReaderStream = new ObjectInputStream(peerSocket.getInputStream());
					chatReaderStreams.add(newChatReaderStream);
					
					if (!this.isGroup){
						ChatReader reader = new ChatReader(newChatReaderStream, newChatWriterStream);
						Thread t = new Thread(reader);
						t.start();
					}
					System.out.println("Connected to " + user.getName());
					
					
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if (this.isGroup){
				JTextField chatField = (JTextField)groupChatPanel.getComponent(1);
				chatField.addActionListener(new chatInputListener(chatWriterStreams, (JTextArea)groupChatPanel.getComponent(0)));
				chatPane.addTab(chatUsers.toString().substring(1, chatUsers.toString().length()-1), groupChatPanel);
				for (ObjectInputStream ois : chatReaderStreams){
					GroupChatReader reader = new GroupChatReader(ois, chatWriterStreams, groupChatPanel);
					Thread t = new Thread(reader);
					t.start();
				}
			}
		}
		
	}
	
	/**
	 * GROUP HOST
	 * This class is only used for the group host. It reads peer input from it's stream
	 * and then writes it back to all the group members. 
	 *
	 */
	private class GroupChatReader implements Runnable {
		ObjectInputStream reader;
		Vector<ObjectOutputStream> allWriters;
		boolean firstEntry = true;
		JPanel chatPanel = null;
		JTextArea chatArea = null;
		
		public GroupChatReader( ObjectInputStream ois, Vector<ObjectOutputStream> oos, JPanel groupChatPanel){
			this.reader = ois;
			this.allWriters = oos;
			this.chatPanel = groupChatPanel;
		}

		@Override
		public void run() {
			String message = null;
			while (true){
				try {
					message = (String)reader.readObject();
					if (firstEntry){
						System.out.println(message);
						firstEntry = false;
					} else {
						chatArea = (JTextArea) chatPanel.getComponent(0);
						for (ObjectOutputStream writer: allWriters){
							writer.writeObject(message);
						}
						chatArea.append(message);
					}
					
				} catch (Exception e) {
					System.out.println("Error in GroupChat reader Thread");
					e.printStackTrace();
					try {
						reader.close();
						writer.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					chatPane.remove(chatPanel);
					return;
				} 				
			}
		}		
	}

	/**
	 * Single chat or group receiver
	 * This is the regular chatreader used for group receivers and single chatters. As such, 
	 * it only has one reader and writer. If it's the first entry, it creates the tab to view
	 * the chat, else it just appends to the tab. 
	 * @author Addison
	 *
	 */
	private class ChatReader implements Runnable {
		ObjectInputStream reader;
		ObjectOutputStream writer;
		boolean firstEntry = true;
		JPanel chatPanel = null;
		JTextArea chatArea = null;
		JTextField chatField = null;
		
		public ChatReader( ObjectInputStream ois, ObjectOutputStream oos){
			this.reader = ois;
			this.writer = oos;
		}

		@Override
		public void run() {
			String message = null;
			while (true){
				try {
					message = (String)reader.readObject();
					if (firstEntry){
						chatPanel = newChatPanel();
						chatField = (JTextField) chatPanel.getComponent(1);
						chatArea = (JTextArea) chatPanel.getComponent(0);
						chatField.addActionListener(new chatInputListener(writer, chatArea, message.indexOf(",") != -1));
						chatPane.addTab(message, chatPanel);
						System.out.println(message);
						firstEntry = false;
					} else {
						chatArea.append(message);
					}
					
				} catch (Exception e) {
					System.out.println("Error in Chat reader Thread");
					e.printStackTrace();
					try {
						reader.close();
						writer.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					chatPane.remove(chatPanel);
					return;
				} 				
			}
		}		
	}
}

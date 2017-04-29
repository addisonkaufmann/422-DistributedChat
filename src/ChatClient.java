import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ChatClient extends JFrame {

	public static void main(String[] args) {
		new ChatClient();
	}

	private User me = null;
	private JTextField nameEntry;
	private ObjectOutputStream writer;
	private ObjectInputStream inputFromServer;
	private Vector<ObjectOutputStream> chatWriterStreams;
	private Vector<ObjectInputStream> chatReaderStreams;
	private Socket socketServer;
	public static String host = "localhost"; //ipconfig -- wireless ac network controller, virtual switch 192.168.0.3

	private JTextArea text;
	private ArrayList<JCheckBox> userBoxes;
	private JPanel boxesPanel;
	private JButton createButton;
	private SelectionListener selectListener;
	private JTabbedPane chatPane;

	private Vector<User> users;
	private int port;
	private String name;
	
	private ArrayList<JComponent> chatPanels;

	public ChatClient() {
		setTitle("Chat Client");
		setSize(380, 480);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new ListenForWindowClose());
		Container cp = getContentPane();
		cp.setLayout(new BorderLayout());

		nameEntry = new JTextField("Your name");
		nameEntry.addActionListener(new InputFieldListener());
		cp.add(nameEntry, BorderLayout.PAGE_START);
		
		chatWriterStreams = new Vector<ObjectOutputStream>();
		chatReaderStreams = new Vector<ObjectInputStream>();
		users = new Vector<User>();

		text = new JTextArea();
//		cp.add(text);
		
		selectListener = new SelectionListener();
		
		boxesPanel = new JPanel();
		boxesPanel.setBackground(Color.WHITE);
//		boxesPanel.setPreferredSize(new Dimension(100,100));
		boxesPanel.setLayout(new BoxLayout(boxesPanel, BoxLayout.PAGE_AXIS));

		chatPane = new JTabbedPane();
		chatPanels = new ArrayList<>();
		cp.add(chatPane, BorderLayout.CENTER);
		
		userBoxes = new ArrayList<>();
		updateBoxes();
		createButton = new JButton("Create Chat");
		createButton.setEnabled(false);
		createButton.addActionListener(new createChatListener());
		cp.add(boxesPanel, BorderLayout.LINE_START);



		setVisible(true);
		nameEntry.requestFocus();
		makeConnectionAndReadAllServerOutputFromServer();
	}
	
	private class AcceptChats implements Runnable {
		//receiving the chat request
		@Override
		public void run() {
			ServerSocket serverSock;
			try {
				serverSock = new ServerSocket(0);
				me = new User(name, serverSock.getLocalPort());
				while (true) {
					Socket peerSocket = serverSock.accept();
										
					ObjectOutputStream newChatWriterStream = new ObjectOutputStream(peerSocket.getOutputStream());
					chatWriterStreams.add(newChatWriterStream);
					newChatWriterStream.writeObject(me.getName());
					
					ObjectInputStream newChatReaderStream = new ObjectInputStream(peerSocket.getInputStream());
					chatReaderStreams.add(newChatReaderStream);
					
					System.out.println("Accepted a chat");

					ChatReader reader = new ChatReader(newChatReaderStream, newChatWriterStream);
					reader.run();
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
			box.addItemListener(selectListener);
			userBoxes.add(box);
			boxesPanel.add(box);
		}
		boxesPanel.add(createButton);
		boxesPanel.repaint();
		boxesPanel.updateUI();
		

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
				setTitle("Chat Client: " + name);
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
	
	private class chatInputListener implements ActionListener {
		private ObjectOutputStream writer;
		private JTextArea chatArea;
		
		public chatInputListener(ObjectOutputStream writer, JTextArea area){
			this.writer = writer;
			this.chatArea = area;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			JTextField inputField = (JTextField)e.getSource();
			String message = me.getName() + ": " + inputField.getText() + "\n";
			inputField.setText("");
			chatArea.append(message);
			try {
				writer.writeObject(message);
			} catch (IOException e1) {
				System.out.println("Error in chatInputListener");
				e1.printStackTrace();
			}
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
	
	private class SelectionListener implements ItemListener {

//		@Override
//		public void actionPerformed(ActionEvent e) {
//
//		}

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
	
	private class createChatListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			ArrayList<User> newChatUsers = new ArrayList<User>();
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
			
			Thread newChat = new Thread(new ChatConnection(newChatUsers.get(0)));
			newChat.start();
			
			
			System.out.println("Creating a chat with " + names);
		}
		
	}
	
	private JPanel newChatPanel(ObjectOutputStream chatWriterStream) {
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		JTextArea chatText = new JTextArea("");
		chatText.setEditable(false);
		JTextField inputBox = new JTextField("Type here");
		inputBox.addActionListener(new chatInputListener(chatWriterStream, chatText));
		p.add(chatText, BorderLayout.CENTER);
		p.add(inputBox, BorderLayout.PAGE_END);
		return p;
	}
	
	private class ChatConnection implements Runnable {
		//initiating the chat
		
		private User user;
		
		public ChatConnection(User u) {
			this.user = u;
		}

		@Override
		public void run() {
			try {
				Socket peerSocket = new Socket(host, user.getPort());
				
				ObjectOutputStream newChatWriterStream = new ObjectOutputStream(peerSocket.getOutputStream());
				chatWriterStreams.add(newChatWriterStream);
				newChatWriterStream.writeObject(me.getName());
				
				ObjectInputStream newChatReaderStream = new ObjectInputStream(peerSocket.getInputStream());
				chatReaderStreams.add(newChatReaderStream);
				
				ChatReader reader = new ChatReader(newChatReaderStream, newChatWriterStream);
				reader.run();
				System.out.println("Connected to " + user.getName());
				
				
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
	}
	
	private class ChatReader implements Runnable {
		ObjectInputStream reader;
		ObjectOutputStream writer;
		boolean firstEntry = true;
		JPanel chatPanel = null;
		JTextArea chatArea = null;
		
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
						chatPanel = newChatPanel(writer);
						chatArea = (JTextArea) chatPanel.getComponent(0);
						chatPane.addTab(message, chatPanel);
						chatPane.updateUI();
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

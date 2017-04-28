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

	private String name;
	private boolean firstEntry = true;
	private JTextField outgoing;
	private ObjectOutputStream writer;
	private ObjectInputStream inputFromServer;
	private Socket socketServer;
	public static String host = "localhost"; //ipconfig -- wireless ac network controller, virtual switch 192.168.0.3
	private Container cp;

	private JTextArea text;
//	private JList<String> userList = new JList<>();
	private VectorListModel<String> users;
	private ArrayList<JCheckBox> userBoxes;
	private JPanel boxesPanel;
	private JButton createButton;
	private SelectionListener selectListener;


	public ChatClient() {
		setTitle("Chat Client");
		setSize(380, 480);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new ListenForWindowClose());
		cp = getContentPane();
		cp.setLayout(new FlowLayout());

		outgoing = new JTextField("Replace me with your name");
		outgoing.addActionListener(new InputFieldListener());
		cp.add(outgoing);
		
		users = new VectorListModel<String>();
//		userList.setModel(users);

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
		outgoing.requestFocus();
		makeConnectionAndReadAllServerOutputFromServer();
	}
	
	private void updateBoxes(){
		boxesPanel.removeAll();
		userBoxes.clear();
		
		for( String user : users){
			JCheckBox box = new JCheckBox(user);
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
			while (true) {
				VectorListModel<String> temp = (VectorListModel<String>) inputFromServer.readObject();
				users = new VectorListModel<String>(temp);
				updateBoxes();
//				userList.setModel(users);
//				userList.updateUI();
//				cp.remove(userList);
//				cp.add(userList);
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
					writer.writeObject(name);
				} else
					// Don't do this . . . should be done in separate thread with another client
					//writer.writeObject(name + ": " + outgoing.getText());
				writer.flush();
			} catch (Exception ex) {
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
					userNames.add(box.getText());
					names += box.getText() + ", ";
					
				}
			}
			
			System.out.println("Creating a chat with " + names);
		}
		
	}
}
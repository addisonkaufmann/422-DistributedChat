import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

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

	private JTextArea inputFromServerTextArea;

	public ChatClient() {
		setTitle("Chat Client");
		setSize(380, 480);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Container cp = getContentPane();
		cp.setLayout(null);

		outgoing = new JTextField("Replace me with your name");
		outgoing.addActionListener(new InputFieldListener());
		outgoing.setSize(300, 20);
		outgoing.setLocation(30, 10);
		cp.add(outgoing);

		inputFromServerTextArea = new JTextArea();

		JScrollPane scroller = new JScrollPane(inputFromServerTextArea);
		scroller.setSize(300, 400);
		scroller.setLocation(30, 40);
		scroller.setBackground(Color.WHITE);
		cp.add(scroller);

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
			while (true) {
				Vector<String> temp = (Vector<String>) inputFromServer.readObject();
				message = temp.toString();
				System.out.println(message);
				inputFromServerTextArea.append(message + "\n");
				
			}
		} catch (Exception ex) {
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
					writer.writeObject(name + " has joined the chat");
				} else
					writer.writeObject(name + ": " + outgoing.getText());
				writer.flush();
			} catch (Exception ex) {
			}
			outgoing.setText("");
			outgoing.requestFocus();
		}
	}
}
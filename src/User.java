import java.io.Serializable;

/**
 * The User class holds information regarding the username and connection of a particular client.
 * @author Aaron & Addison
 *
 */
public class User implements Serializable {
	private int port;
	private String name;
	private String ip;
	
	public User(String name, String ip, int port) {
		this.port = port;
		this.name = name;
		this.ip = ip;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getIP() {
		return this.ip;
	}
	
	public int getPort() {
		return this.port;
	}
	
	public String toString(){
		return this.name;
	}
}

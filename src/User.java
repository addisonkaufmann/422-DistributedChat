import java.io.Serializable;

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

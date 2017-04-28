import java.io.Serializable;

public class User implements Serializable {
	private int port;
	private String name;
	
	public User(String name, int port) {
		this.port = port;
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
	
	public int getPort() {
		return this.port;
	}
	
	public String toString(){
		return this.name;
	}
}

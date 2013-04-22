
public class Command {
	char opcode;
	String parameters;
	long time;
	
	public Command(char opcode, String parameters, long time) {
		this.opcode = opcode;
		this.parameters = parameters;
		this.time = time;
	}
	
	public char getOpcode() {
		return opcode;
	}
	public void setOpcode(char opcode) {
		this.opcode = opcode;
	}
	public String getParameters() {
		return parameters;
	}
	public void setParameters(String parameters) {
		this.parameters = parameters;
	}
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
	
	
}

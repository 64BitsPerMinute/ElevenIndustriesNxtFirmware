import java.util.Stack;

import lejos.nxt.Battery;
import lejos.nxt.Button;
import lejos.nxt.LightSensor;
import lejos.nxt.Motor;
import lejos.nxt.SensorPort;
import lejos.nxt.SensorPortListener;
import lejos.nxt.SoundSensor;
import lejos.nxt.TouchSensor;
import lejos.nxt.UltrasonicSensor;
import lejos.robotics.Touch;
import lejos.robotics.navigation.DifferentialPilot;

public class RobotSystem {

	private static Touch sTouch = new TouchSensor(SensorPort.S1);
	private static SoundSensor sSound = new SoundSensor(SensorPort.S2);
	private static LightSensor sLight = new LightSensor(SensorPort.S3);
	private static UltrasonicSensor sUltra = new UltrasonicSensor(SensorPort.S4);
	
	static int xPosition = 0;
	static int yPosition = 0;
	static long previousSoundTime = 0;
	
	Stack<Command> log = new Stack<Command>();
	private DifferentialPilot pilot = new DifferentialPilot(2.25f, 5.5f,
			Motor.B, Motor.C);

	public RobotSystem() {
		pilot.setRotateSpeed(30);
		
		SensorPort.S1.addSensorPortListener(new SensorPortListener() { 
					@Override
					public void stateChanged(SensorPort arg0, int arg1, int arg2) {
						if (sTouch.isPressed()) {
							System.out.print("\nYOU POKED ME");
							pilot.stop();
							log('F',"");
							pilot.backward();
							log('D',"");
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							pilot.stop();
							log('F',"");
						}
					}
				});
		
		SensorPort.S2.addSensorPortListener(new SensorPortListener() {
			@Override
			public void stateChanged(SensorPort arg0, int arg1, int arg2) {
				if(sSound.readValue()>85 && (System.currentTimeMillis()-previousSoundTime)>300){
					pilot.stop();
					log('F',"");
					System.out.print("\nLOUD SOUND BOOM!");
					previousSoundTime = System.currentTimeMillis();
				}
			}
		});
	}
	
	public void breakpoint(){
		this.stop();
		Button.waitForPress();
	}
	
	public int getSoundData(){
		return sSound.readValue();
	}
	
	public int getLightData(){
		return sLight.getLightValue();
	}
	
	public int getUltraSonicData(){
		return sUltra.getDistance();
	}
	
	public String getTouchData(){
		if(sTouch.isPressed()){
			return "1";
		} else {
			return "0";
		}
	}
	
	public int getBatteryStrength(){
		return Battery.getVoltageMilliVolt();
	}
	
	public int getMotorBSpeed(){
		return  Motor.B.getSpeed();
	}
	
	public int getMotorCSpeed(){
		return  Motor.C.getSpeed();
	}
	
	public int getMotorBAngle(){
		return Motor.B.getLimitAngle();
	}
	
	public int getMotorCAngle(){
		return Motor.C.getLimitAngle();
	}

	//Returning status string. s =success, f = fail, n = null
	
	public String turnLeft() {
		pilot.rotateLeft();
		return "S";
	}

	public String turnRight() {
		pilot.rotateRight();
		return "S";
	}

	public String moveForward(String speeds) {
		int leftSpeed = Integer.parseInt(speeds.substring(0,4));
		int rightSpeed = Integer.parseInt(speeds.substring(4,8));
		double ratio = (double) rightSpeed/ (double) leftSpeed;
		if(ratio == 1.0){
			pilot.steer(0);
			return "S";
		} else if(ratio>1.0){
			pilot.steer(25);
			return "S";
		} else if(ratio<1.0) {
			pilot.steer(-25);
			return "S";
		}
		return "F";
	}

	public String moveBackward(String speeds) {
		int leftSpeed = Integer.parseInt(speeds.substring(0,4));
		int rightSpeed = Integer.parseInt(speeds.substring(4,8));
		double ratio = (double) rightSpeed/ (double) leftSpeed;
		if(ratio == 1.0){
			pilot.steerBackward(0);
			return "S";
		} else if(ratio>1.0){
			pilot.steerBackward(25);
			return "S";
		} else if(ratio<1.0) {
			pilot.steerBackward(-25);
			return "S";
		}
		return "F";
	}

	public String stop() {
		pilot.stop();
		return "S";
	}
	
	public int[] getLocation() {
			int[] ret = {xPosition, yPosition};
		return ret;
	}

	public void setHome() {
		log.clear();
		this.log('F',"");
	}

	public String goHome() {
		pilot.stop();
		for(int i = 0;i<log.size()-1;i++){
			Command c = log.pop();
			invertCommand(log.peek(),c.getTime()-log.peek().getTime());
		}	
		pilot.stop();
		setHome();
		return "S";
	}

	private void invertCommand(Command c, long time) {
		String leftSpeed;
		String rightSpeed;
		switch(c.getOpcode()){
		case 'A':
			this.turnRight();
			break;
		case 'B':
			this.turnLeft();
			break;
		case 'C':
			leftSpeed = c.getParameters().substring(0,4);
			rightSpeed = c.getParameters().substring(4,8);
			this.moveBackward(rightSpeed + leftSpeed);
			break;
		case 'D':
			leftSpeed = c.getParameters().substring(0,4);
			rightSpeed = c.getParameters().substring(4,8);
			this.moveForward(rightSpeed + leftSpeed);
			break;
		case 'F':
			this.stop();
			return;
		default:
			this.stop();
			return;
		}
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void log(char c, String parameters) {
		log.push(new Command(c,parameters,System.currentTimeMillis()));
	}

}

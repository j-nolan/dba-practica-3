package main;

import java.util.ArrayList;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;

public class MasterOfDrones extends SingleAgent {
	
	// The battery, the position of the agents
	private ArrayList<Integer> battery;
	private ArrayList<Integer> x;
	private ArrayList<Integer> y;
	private ArrayList<String> roles;
	private ArrayList<String> botNames;
	
	// The state of each drone, can be: 0 = ready, 1 = objectiveSeen, 2 = objectiveFound.
	private ArrayList<Integer> state;
	
	// The world has a given amount of energy that decrease everytime an agent refuels
	// The total remaining amount is saved in this attribute
	private int totalWorldEnergy;
	
	private ArrayList<String> wantedMoves;
	
	private int[][] matrixMoves;
	
	// The internal representation of the map
	Map map;
	
	/**
	 * Constructor
	 * @author Zacarías Romero Sellamitou
	 * @param aid
	 * @throws Exception
	 */
	public MasterOfDrones(AgentID aid, Map map) throws Exception {
		super(aid);
		System.out.println("MASTER!!");
		this.map = map;
		this.battery = new ArrayList<Integer>(5);
		this.x = new ArrayList<Integer>(5);
		this.y = new ArrayList<Integer>(5);
		this.state = new ArrayList<Integer>(5);
		this.wantedMoves = new ArrayList<String>(5);
		this.roles = new ArrayList<String>(5);
		this.botNames = new ArrayList<String>(5);
		
		for (int i=0;i<5;i++) {
			battery.add(0);
			x.add(0);
			y.add(0);
			state.add(0);
			wantedMoves.add("ayy");
			roles.add("ayy");
		}
		
		botNames.add("ayy");
		botNames.add("bot1");
		botNames.add("bot2");
		botNames.add("bot3");
		botNames.add("bot4");
		
		matrixMoves = new int[5][8];
	}
	
	/**
	 * Method that returns the greater value in a row
	 * @Author Zacarías Romero Sellamitou
	 */
	private int greaterRow(int row) {
		int res = 0;
		int aux = 0;
		for (int i=0;i<8;i++) {
			if (matrixMoves[row][i] >= res) {
				res = i;
				aux = matrixMoves[row][i];
			}
		}
		System.out.println("El mejor de la fila: "+aux+ " en la posicion: "+res);
		return res;
	}
	/**
	 * Method
	 * @author Zacarías Romero Sellamitou
	 */
	private int check(int i, Direction direction) {
		int res = 0;
		int xFutura = x.get(i) + direction.getXCoord();
		int yFutura = y.get(i) + direction.getYCoord();
		switch (roles.get(i)) {
		case "fly":
			for (int j=0;j<9;j++) {
				int x_aux = xFutura + ((j%3) - 1);
				int y_aux = yFutura + ((j/3) - 1);
				//System.out.println(botNames.get(i) + ": En las coordenadas "+ x_aux + ","+y_aux+" tenemos: "+map.get(x_aux,y_aux).toString());
				if(map.get(x_aux,y_aux) == MapPixel.UNKNOWN)
					res++;
			}
			break;
		case "parrot":
			for (int j=0;j<25;j++) {
				int x_aux = xFutura + (j%5) - 2;
				int y_aux = yFutura + (j/5) - 2;
				if(map.get(x_aux,y_aux) == MapPixel.UNKNOWN)
					res++;
			}
			break;
		case "falcon":
			for (int j=0;j<121;j++) {
				int x_aux = xFutura + (j%11) - 5;
				int y_aux = yFutura + (j/11) - 5;
				if(map.get(x_aux,y_aux) == MapPixel.UNKNOWN)
					res++;
			}
			break;
		}
		//System.out.println(botNames.get(i)+ ": El resultado es: " +res);
		return res;
	}
	/**
	 * Method that gets the perception of the bots
	 * @author Zacarías Romero Sellamitou
	 */
	
	private void getPerception() {
		JSONObject json = new JSONObject();
		ACLMessage msg;
		String botName = null;
		// Send request to master
		// Wait for server answer
		try {
			for (int i=0;i<4;i++) {
				msg = receiveACLMessage();
				botName = msg.getSender().getLocalName();
				//System.out.println(msg);
				json = new JSONObject(msg.getContent());
				
				if (msg.getPerformativeInt() == ACLMessage.INFORM_REF) {
					int index = Character.getNumericValue(botName.charAt(3));
					battery.set(index,json.getInt("battery"));
					x.set(index,json.getInt("x"));
					y.set(index,json.getInt("y"));
					state.set(index,json.getInt("state"));
					totalWorldEnergy = json.getInt("total");
					roles.set(index,json.getString("role"));
					JSONArray jArray = json.getJSONArray("sensor");
					map.update(x.get(index), y.get(index), jArray);
				} else {
					System.err.println(botName + ": Unexpected answer on getPerception : " + msg.getPerformativeInt());
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Method that evaluates the wanted moves of the drones and answers them
	 * @author Zacarías Romero Sellamitou
	 */
	
	private void evaluate() {
		JSONObject json = new JSONObject();
		ACLMessage msg;
		Direction[] directions = Direction.values();
		for (int i=1;i<5;i++) {
			int index = 0;
			for (Direction direction : directions) {
				if (map.validMove(x.get(i), y.get(i), direction)) {
					//wantedMoves.set(i,direction.toString());
					matrixMoves[i][index]=check(i,direction);
					index++;
				} else {
					matrixMoves[i][index] = 0;
					index++;
				}
			}
		}
		
		for(int i=1;i<5;i++) {
			if (battery.get(i) < 5) wantedMoves.set(i,"refuel");
			else {
				int greater = greaterRow(i);
				//System.out.println("El mejor valor del bot "+botNames.get(i)+ " es: "+greater);
				wantedMoves.set(i,directions[greater].toString());
			}
			System.out.println(botNames.get(i) + " quiere realizar: "+ wantedMoves.get(i));
		}
		try {
		msg = new ACLMessage();
		for(int i=1;i<5;i++) {
			json.put(botNames.get(i),wantedMoves.get(i));
		}
		msg.setContent(json.toString());
		msg.setSender(getAid());
		msg.addReceiver(new AgentID("bot1"));
		msg.addReceiver(new AgentID("bot2"));
		msg.addReceiver(new AgentID("bot3"));
		msg.addReceiver(new AgentID("bot4"));
		msg.setPerformative(ACLMessage.INFORM);
		send(msg);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Bot main execution loop. Before entering the loop, it will take care of doing the login
	 * procedure (only for the initializer bot), get the key from the server and check in to the world.
	 * @author James Nolan
	 */
	public void execute() {
		// Loop getting info about all drones and the move they want, evaluating it and send each one if they can
		// procceed or not
		while (true) {
			getPerception();
			evaluate();
		}
	}
}

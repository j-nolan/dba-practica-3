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
	private ArrayList<Double> equis;
	private ArrayList<Double> ygriega;
	private double goalX = -1;
	private double goalY = -1;
	private boolean work = true;
	
	// Variables to perform right hand algorithm
	private double max;
	private ArrayList<Boolean> rightHand;
	private Direction wantedDirection;
	
	private ArrayList<Integer> lastDirection;
	private int auxx = 0;
	private double[][] scanner;
	
	// The state of each drone, can be: 0 = ready, 1 = objectiveSeen, 2 = objectiveFound.
	private ArrayList<Integer> state;
	
	int statee;
	
	// The world has a given amount of energy that decrease everytime an agent refuels
	// The total remaining amount is saved in this attribute
	private int totalWorldEnergy;
	
	private ArrayList<String> wantedMoves;
	
	private int[][] matrixMoves;
	
	private ArrayList<Integer> mejores;
	
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
		this.mejores = new ArrayList<Integer>(5);
		this.scanner = new double[100][100];
		this.lastDirection = new ArrayList<Integer>(5);
		this.rightHand = new ArrayList<Boolean>(5);
		this.equis = new ArrayList<Double>(15);
		this.ygriega = new ArrayList<Double>(15);
		
		for (int i=0;i<5;i++) {
			lastDirection.add(i,10);
		}
		for (int i=0;i<5;i++) {
			battery.add(0);
			x.add(0);
			y.add(0);
			state.add(0);
			mejores.add(0);
			wantedMoves.add("ayy");
			roles.add("ayy");
			rightHand.add(false);
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
			if (matrixMoves[row][i] >= aux/* && !lastDirection.contains(i)*/) {
				res = i;
				aux = matrixMoves[row][i];
			}
		}
		System.out.println(row + ": El mejor de la fila: "+aux+ " en la posicion: "+res);
		mejores.set(row,aux);
		return res;
	}
	
	private int bestBot() {
		int res = 0;
		int valor = 0;
		for (int i=1;i<5;i++) {
			if (mejores.get(i) >= valor) {
				res = i;
				valor = mejores.get(i);
			}
		}
		return res;
	}
	
	private double equisMedia() {
		double res = 0;
		for (int i=0;i<equis.size();i++) {
			res+=equis.get(i);
		}
		return res/equis.size();
	}
	
	private double yMedia() {
		double res = 0;
		for (int i=0;i<ygriega.size();i++) {
			res+=ygriega.get(i);
		}
		return res/ygriega.size();
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
					if(state.get(index) == 2) {
						statee = 2;
						if(true/*goalX==-1*/) {
							boolean find = true;
							for(int k=0;k<jArray.length() && find;k++) {
								if(jArray.getInt(k) == 3) {
									int sqrt = (int)Math.sqrt(jArray.length());
									equis.add((double)x.get(index) + ((k%sqrt) - sqrt/2));
									ygriega.add((double)y.get(index) + ((k/sqrt) - sqrt/2));
									//System.out.println(index+ ": ENCONTRADO OBJETIVO x:" + (x.get(index) + ((k%sqrt) - sqrt/2)) + " y:"+(y.get(index) + ((k/sqrt) - sqrt/2)));
									//makeScanner();
									if(statee == 0) statee = 1;
									//find = false;
								}
							}
							goalX = equisMedia();
							goalY = yMedia();
							//System.out.println("La media es - x: "+goalX+" y: "+goalY);
							makeScanner();
						}
					}
					if(state.get(index) == 1/* && goalX==-1*/) {
						boolean find = true;
						for(int k=0;k<jArray.length() && find;k++) {
							if(jArray.getInt(k) == 3) {
								int sqrt = (int)Math.sqrt(jArray.length());
								equis.add((double)x.get(index) + ((k%sqrt) - sqrt/2));
								ygriega.add((double)y.get(index) + ((k/sqrt) - sqrt/2));
								//System.out.println(index+ ": ENCONTRADO OBJETIVO x:" + (x.get(index) + ((k%sqrt) - sqrt/2)) + " y:"+(y.get(index) + ((k/sqrt) - sqrt/2)));
								//makeScanner();
								if(statee == 0) statee = 1;
								//find = false;
							}
						}
						goalX = equisMedia();
						goalY = yMedia();
						//System.out.println("La media es - x: "+goalX+" y: "+goalY);
						makeScanner();
					}
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
	
	private void makeScanner() {
		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < 100; j++) {
				scanner[i][j] = distancePoint(goalX, goalY, j, i);
			}
		}
	}
	
	private double distancePoint(double x1, double y1, double x2, double y2) {
		return Math.sqrt(Math.pow((y2-y1),2)  + Math.pow((x2-x1),2));
	}
	
	private boolean fly() {
		boolean res = false;
		for (int i=1;i<5;i++) 
			if (roles.get(i).equals("fly") && state.get(i)!=2) return true;
		return res;
	}
	
	private int bestFly() {
		int res = 0;
		double aux = 100000;
		for (int i=1;i<5;i++) {
			if (roles.get(i).equals("fly") && state.get(i)!=2) {
				if(scanner[y.get(i)][x.get(i)] <= aux) {
					res = i;
					aux = scanner[y.get(i)][x.get(i)];
				}
			}
		}
		return res;
	}
	
	private boolean parrot() {
		boolean res = false;
		for (int i=1;i<5;i++) 
			if (roles.get(i).equals("parrot") && state.get(i)!=2) return true;
		return res;
	}
	
	private int bestParrot() {
		int res = 0;
		double aux = 100000;
		for (int i=1;i<5;i++) {
			if (roles.get(i).equals("parrot") && state.get(i)!=2) {
				if(scanner[y.get(i)][x.get(i)] <= aux) {
					res = i;
					aux = scanner[y.get(i)][x.get(i)];
				}
			}
		}
		return res;
	}
	
	private boolean falcon() {
		boolean res = false;
		for (int i=1;i<5;i++) 
			if (roles.get(i).equals("falcon") && state.get(i)!=2) return true;
		return res;
	}
	
	private int bestFalcon() {
		int res = 0;
		double aux = 100000;
		for (int i=1;i<5;i++) {
			if (roles.get(i).equals("falcon") && state.get(i)!=2) {
				if(scanner[y.get(i)][x.get(i)] <= aux) {
					res = i;
					aux = scanner[y.get(i)][x.get(i)];
				}
			}
		}
		return res;
	}
	
	private boolean finish() {
		boolean finish = true;
		for (int i=1;i<5;i++) {
			if (state.get(i)!=2) return false;
		}
		return finish;
	}
	/**
	 * Method that evaluates the wanted moves of the drones and answers them
	 * @author Zacarías Romero Sellamitou
	 */
	
	private void evaluate() {
		if (finish()) {
			ACLMessage msg = new ACLMessage();
			msg.setSender(getAid());
			msg.addReceiver(new AgentID("bot1"));
			msg.addReceiver(new AgentID("bot2"));
			msg.addReceiver(new AgentID("bot3"));
			msg.addReceiver(new AgentID("bot4"));
			msg.setPerformative(ACLMessage.CANCEL);
			send(msg);
			work = false;
		}
		JSONObject json = new JSONObject();
		ACLMessage msg;
		Direction[] directions = Direction.values();
		for (int i=1;i<5;i++) {
			int index = 0;
			boolean ghost = roles.get(i).equals("fly");
			for (Direction direction : directions) {
				if (map.validMove(x.get(i), y.get(i), direction, ghost) && !crash(x.get(i), y.get(i), direction, i)) {
					//wantedMoves.set(i,direction.toString());
					matrixMoves[i][index]=check(i,direction);
					index++;
				} else {
					matrixMoves[i][index] = -1;
					index++;
				}
			}
		}
		for (int i=1;i<5;i++) {
			for (int j=0;j<8;j++) {
				System.out.print(matrixMoves[i][j] + " ");
			}
			System.out.println();
		}
		for(int i=1;i<5;i++) {
			int greater = greaterRow(i);
			mejores.set(i,matrixMoves[i][greater]);
		}
		
		System.out.println(mejores.toString());

		int mejorBot = bestBot();
		System.out.println("El estado es: " + statee);
		System.out.println("El mejor es: " + mejorBot);
		for (int i=1;i<5;i++) {
			if (statee == 2) {
				if (fly()) {
					int bf = bestFly();
					if(i==1) System.out.println("Mandando mosca " + bf);
					if(i==bf) wantedMoves.set(i,goObjective(i));
					else wantedMoves.set(i,"idle");
				}
				else if (parrot()) {
					int bp = bestParrot();
					if(i==1) System.out.println("Mandando parrot" + bp);
					if(i==bp) wantedMoves.set(i,goObjective(i));
					else wantedMoves.set(i,"idle");
				}
				else if (falcon()) {
					int bf = bestFalcon();
					if(i==1) System.out.println("Mandando falcon" + bf);
					if(i==bf)wantedMoves.set(i,goObjective(i));
					else wantedMoves.set(i,"idle");
				}
			}
			else if (state.get(i) == 2) wantedMoves.set(i,"idle");
			/*else if (battery.get(i) < 5) wantedMoves.set(i,"refuel");*/
			else if (state.get(i) == 1) {
				wantedMoves.set(i,goObjective(i));
				for (int k=1;k<5;k++) {
					if (k!=i) {
						wantedMoves.set(k,"idle");
					}
				}
				break;
			}
			else if(i==mejorBot) {
				if(battery.get(i) < 5) wantedMoves.set(i,"refuel");
				else {
					wantedMoves.set(i,directions[greaterRow(i)].toString());
				}
			}
			else wantedMoves.set(i,"idle");
		}
		
		System.out.println(wantedMoves.toString());
		
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
		for (int i=1;i<5;i++) {
			map.setDrone(x.get(i),y.get(i));
		}
	}

	private String goObjective(int bot) {
		if (battery.get(bot) < 5) return "refuel";
		if(rightHand.get(bot)) {
			if(state.get(bot) == 2) {
				rightHand.set(bot,false);
				return "idle";
			}
			else {
			System.out.println(bot +": MANO DERECHA con: "+wantedDirection.toString());
			switch(wantedDirection) {
			case EAST:
				if(scanner[y.get(bot)][x.get(bot)]<max) {
					rightHand.set(bot,false);
					return goObjective(bot);
				} else {
					if(!crash(x.get(bot),y.get(bot),Direction.EAST,bot)) {
						wantedDirection = Direction.SOUTH;
						return "moveE";
					}
					else if(!crash(x.get(bot),y.get(bot),Direction.NORTH_EAST,bot)) {
						wantedDirection = Direction.SOUTH_EAST;
						return "moveNE";
					} else if(!crash(x.get(bot),y.get(bot),Direction.NORTH,bot)) {
						wantedDirection = Direction.EAST;
						return "moveN";
					} else if (!crash(x.get(bot),y.get(bot),Direction.NORTH_WEST,bot)) {
						wantedDirection = Direction.NORTH_EAST;
						return "moveNW";
					}else {
						rightHand.set(bot,false);
						return goObjective(bot);
					}
				}
			case NORTH:
				if(scanner[y.get(bot)][x.get(bot)]<max) {
					rightHand.set(bot,false);
					return goObjective(bot);
				} else {
					if(!crash(x.get(bot),y.get(bot),Direction.NORTH,bot)) {
						wantedDirection = Direction.EAST;
						return "moveN";
					}
					else if(!crash(x.get(bot),y.get(bot),Direction.NORTH_WEST,bot)) {
						wantedDirection = Direction.NORTH_EAST;
						return "moveNW";
					} else if(!crash(x.get(bot),y.get(bot),Direction.WEST,bot)) {
						wantedDirection = Direction.NORTH;
						return "moveW";
					} else if (!crash(x.get(bot),y.get(bot),Direction.SOUTH_WEST,bot)) {
						wantedDirection = Direction.NORTH_WEST;
						return "moveSW";
					}else {
						rightHand.set(bot,false);
						return goObjective(bot);
					}
				}
			case NORTH_EAST:
				if(scanner[y.get(bot)][x.get(bot)]<max) {
					rightHand.set(bot,false);
					return goObjective(bot);
				} else {
					if(!crash(x.get(bot),y.get(bot),Direction.NORTH_EAST,bot)) {
						wantedDirection = Direction.SOUTH_EAST;
						return "moveNE";
					} else if(!crash(x.get(bot),y.get(bot),Direction.NORTH,bot)) {
						wantedDirection = Direction.EAST;
						return "moveN";
					} else if(!crash(x.get(bot),y.get(bot),Direction.NORTH_WEST,bot)) {
						wantedDirection = Direction.NORTH_EAST;
						return "moveNW";
					}else if (!crash(x.get(bot),y.get(bot),Direction.WEST,bot)) {
						wantedDirection = Direction.NORTH;
						return "moveW";
					}else {
						rightHand.set(bot,false);
						return goObjective(bot);
					}
				}
			case NORTH_WEST:
				if(scanner[y.get(bot)][x.get(bot)]<max) {
					rightHand.set(bot,false);
					return goObjective(bot);
				} else {
					if(!crash(x.get(bot),y.get(bot),Direction.NORTH_WEST,bot)) {
						wantedDirection = Direction.NORTH_EAST;
						return "moveNW";
					} else if(!crash(x.get(bot),y.get(bot),Direction.WEST,bot)) {
						wantedDirection = Direction.NORTH;
						return "moveW";
					} else if(!crash(x.get(bot),y.get(bot),Direction.SOUTH_WEST,bot)) {
						wantedDirection = Direction.NORTH_WEST;
						return "moveSW";
					}else if (!crash(x.get(bot),y.get(bot),Direction.SOUTH,bot)) {
						wantedDirection = Direction.WEST;
						return "moveS";
					}else {
						rightHand.set(bot,false);
						return goObjective(bot);
					}
				}
			case SOUTH:
				if(scanner[y.get(bot)][x.get(bot)]<max) {
					rightHand.set(bot,false);
					return goObjective(bot);
				} else {
					if(!crash(x.get(bot),y.get(bot),Direction.SOUTH,bot)) {
						wantedDirection = Direction.WEST;
						return "moveS";
					}
					else if(!crash(x.get(bot),y.get(bot),Direction.SOUTH_EAST,bot)) {
						wantedDirection = Direction.SOUTH_WEST;
						return "moveSE";
					} else if(!crash(x.get(bot),y.get(bot),Direction.EAST,bot)) {
						wantedDirection = Direction.SOUTH;
						return "moveE";
					}else if (!crash(x.get(bot),y.get(bot),Direction.NORTH_EAST,bot)) {
						wantedDirection = Direction.SOUTH_EAST;
						return "moveNE";
					}else {
						rightHand.set(bot,false);
						return goObjective(bot);
					}
				}
			case SOUTH_EAST:
				if(scanner[y.get(bot)][x.get(bot)]<max) {
					rightHand.set(bot,false);
					return goObjective(bot);
				} else {
					if(!crash(x.get(bot),y.get(bot),Direction.SOUTH_EAST,bot)) {
						wantedDirection = Direction.SOUTH_WEST;
						return "moveSE";
					} else if(!crash(x.get(bot),y.get(bot),Direction.EAST,bot)) {
						wantedDirection = Direction.SOUTH;
						return "moveE";
					} else if(!crash(x.get(bot),y.get(bot),Direction.NORTH_EAST,bot)) {
						wantedDirection = Direction.SOUTH_EAST;
						return "moveNE";
					}else if (!crash(x.get(bot),y.get(bot),Direction.NORTH,bot)) {
						wantedDirection = Direction.EAST;
						return "moveN";
					}else {
						rightHand.set(bot,false);
						return goObjective(bot);
					}
				}
			case SOUTH_WEST:
				if(scanner[y.get(bot)][x.get(bot)]<max) {
					rightHand.set(bot,false);
					return goObjective(bot);
				} else {
					if(!crash(x.get(bot),y.get(bot),Direction.SOUTH_WEST,bot)) {
						wantedDirection = Direction.NORTH_WEST;
						return "moveSW";
					} else if(!crash(x.get(bot),y.get(bot),Direction.SOUTH,bot)) {
						wantedDirection = Direction.WEST;
						return "moveS";
					} else if(!crash(x.get(bot),y.get(bot),Direction.SOUTH_EAST,bot)) {
						wantedDirection = Direction.SOUTH_WEST;
						return "moveSE";
					}else if (!crash(x.get(bot),y.get(bot),Direction.EAST,bot)) {
						wantedDirection = Direction.SOUTH;
						return "moveE";
					}else {
						rightHand.set(bot,false);
						return goObjective(bot);
					}
				}
			case WEST:
				if(scanner[y.get(bot)][x.get(bot)]<max) {
					rightHand.set(bot,false);
					return goObjective(bot);
				} else {
					if(!crash(x.get(bot),y.get(bot),Direction.WEST,bot)) {
						wantedDirection = Direction.NORTH;
						return "moveW";
					}
					else if(!crash(x.get(bot),y.get(bot),Direction.SOUTH_WEST,bot)) {
						wantedDirection = Direction.NORTH_WEST;
						return "moveSW";
					} else if(!crash(x.get(bot),y.get(bot),Direction.SOUTH,bot)) {
						wantedDirection = Direction.WEST;
						return "moveS";
					}else if (!crash(x.get(bot),y.get(bot),Direction.SOUTH_EAST,bot)) {
						wantedDirection = Direction.SOUTH_WEST;
						return "moveSE";
					}else {
						rightHand.set(bot,false);
						return goObjective(bot);
					}
				}
			default:
				break;
			}
			}
		} else {
			double res = 1000000;
			int pos = 0;
			for (int i=0;i<9;i++) {
				if (i!=4) {
					int x_aux = x.get(bot) + ((i%3) - 1);
					int y_aux = y.get(bot) + ((i/3) - 1);
					if (x_aux<100 && y_aux<100 && x_aux>=0 && y_aux>=0) {
						if(scanner[y_aux][x_aux] < res) {
							res = scanner[y_aux][x_aux];
							pos = i;
						}
					}
				}
			}
			for(int i=0;i<9;i++) {
				int x_aux = x.get(bot) + ((i%3) - 1);
				int y_aux = y.get(bot) + ((i/3) - 1);
				if(i%3 == 0) System.out.println();
				if (x_aux<100 && y_aux<100 && x_aux>=0 && y_aux>=0) System.out.print(scanner[y_aux][x_aux] + " - ");
				else System.out.print(-1);
			}
			System.out.println(bot + " moviendose al objetivo en dirección: " +pos);
			switch(pos) {
				case 0:
					if (!crash(x.get(bot),y.get(bot),Direction.NORTH_WEST,bot))	return "moveNW";
					else {
						System.out.println(bot+ ": Activando mano derecha");
						rightHand.set(bot,true);;
						max = scanner[y.get(bot)][x.get(bot)];
						wantedDirection = Direction.NORTH_WEST;
						return goObjective(bot);
					}
				case 1:
					if (!crash(x.get(bot),y.get(bot),Direction.NORTH,bot))return "moveN";
					else {
						System.out.println(bot+ ": Activando mano derecha");
						rightHand.set(bot,true);;
						max = scanner[y.get(bot)][x.get(bot)];
						wantedDirection = Direction.NORTH;
						return goObjective(bot);
					}
				case 2:
					if (!crash(x.get(bot),y.get(bot),Direction.NORTH_EAST,bot))return "moveNE";
					else {
						System.out.println(bot+ ": Activando mano derecha");
						rightHand.set(bot,true);;
						max = scanner[y.get(bot)][x.get(bot)];
						wantedDirection = Direction.NORTH_EAST;
						return goObjective(bot);
					}
				case 3:
					if (!crash(x.get(bot),y.get(bot),Direction.WEST,bot))return "moveW";
					else {
						System.out.println(bot+ ": Activando mano derecha");
						rightHand.set(bot,true);;
						max = scanner[y.get(bot)][x.get(bot)];
						wantedDirection = Direction.WEST;
						return goObjective(bot);
					}
				case 4:
					return "idle";
				case 5:
					if (!crash(x.get(bot),y.get(bot),Direction.EAST,bot))return "moveE";
					else {
						System.out.println(bot+ ": Activando mano derecha");
						rightHand.set(bot,true);;
						max = scanner[y.get(bot)][x.get(bot)];
						wantedDirection = Direction.EAST;
						return goObjective(bot);
					}
				case 6:
					if (!crash(x.get(bot),y.get(bot),Direction.SOUTH_WEST,bot))return "moveSW";
					else {
						System.out.println(bot+ ": Activando mano derecha");
						rightHand.set(bot,true);;
						max = scanner[y.get(bot)][x.get(bot)];
						wantedDirection = Direction.SOUTH_WEST;
						return goObjective(bot);
					}
				case 7:
					if (!crash(x.get(bot),y.get(bot),Direction.SOUTH,bot))return "moveS";
					else {
						System.out.println(bot+ ": Activando mano derecha");
						rightHand.set(bot,true);;
						max = scanner[y.get(bot)][x.get(bot)];
						wantedDirection = Direction.SOUTH;
						return goObjective(bot);
					}
				case 8:
					if (!crash(x.get(bot),y.get(bot),Direction.SOUTH_EAST,bot))return "moveSE";
					else {
						System.out.println(bot+ ": Activando mano derecha");
						rightHand.set(bot,true);
						max = scanner[y.get(bot)][x.get(bot)];
						wantedDirection = Direction.SOUTH_EAST;
						return goObjective(bot);
					}
			}
		}
		return "idle";
	}

	private boolean crash(int x1, int y1, int bot) {
		boolean res = false;
		for (int i=1;i<5;i++) {
			if(i!=bot) {
				if(x.get(i) == x1 && y.get(i) == y1) return true;
			}
		}
		return res;
	}
	
	private boolean crash(int x1, int y1, Direction direction, int bot) {
		boolean res = false;
		for (int i=1;i<5;i++) {
			int actualX = x1 + direction.getXCoord();
			int actualY = y1 + direction.getYCoord();
			if(i!=bot) {
				if(x.get(i) == actualX && y.get(i) == actualY) return true;
			}
		}
		res = !map.validMove(x1,y1,direction,roles.get(bot).equals("fly"));
		return res;
	}

	/**
	 * Bot main execution loop. Before entering the loop, it will take care of doing the login
	 * procedure (only for the initializer bot), get the key from the server and check in to the world.
	 * @author James Nolan
	 */
	public void execute() {
		// Loop getting info about all drones and the move they want, evaluating it and send each one if they can
		// procceed or not
		while (work) {
			getPerception();
			evaluate();
		}
	}
}

package org.vectomatic.svg.chess;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.alonsoruibal.chess.Move;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.websockets.client.WebSocket;
import com.google.gwt.websockets.client.WebSocketCallback;

class XBoardException extends Exception {
	private static final long serialVersionUID = 1L;
	String line;
	String message;
	public String getMessage() {
		return "Error (" + message + "): " + line;
	}
	public XBoardException(String explanation, String line) {
		this.message = explanation;
		this.line = line;
	}
	
}

class GoDelayerTimer extends Timer {

	boolean hasSchedule = false;
	XBoardEngine xb;
	
	public GoDelayerTimer(XBoardEngine xb) {
		this.xb = xb;
	}
	
	@Override
	public void run() {
		hasSchedule = false;
		xb.go();
	}
	
	@Override
	public void cancel() {
		hasSchedule = false;
		super.cancel();
	}
	
	boolean hasSchedule() {
		return hasSchedule;
	}
	
	@Override
	public void schedule(int delay) {
		super.schedule(delay);
		hasSchedule = true;
	}
	
	@Override
	public void scheduleRepeating(int delay) { throw new RuntimeException(); }
	
}

public class XBoardEngine implements Runnable, WebSocketCallback {
	private SuperSearchObserver observer;
	private boolean initialized;
	private Queue<Integer> moveQueue = new LinkedList<Integer>();
	private boolean searching = false;
	private WebSocket socket;
	private String messageBuffer = "";
	final private String prefix = "XBoardEngine: ";

	public void setInterface(SuperSearchObserver observer) {
		this.observer = observer;
	}
	
	private GoDelayerTimer goDelayer = new GoDelayerTimer(this);
	
	public void go() {
		if (!initialized) {
			GWT.log("Not initialized");
			if (!goDelayer.hasSchedule()) {
				GWT.log("Scheduling check...");
				goDelayer.schedule(100);
			}
			return;
		}
		if (!searching) {
			run();
		} else {
			GWT.log("Already searching...");
		}
	}

	@Override
	public void run() {
		boolean wasSearching = searching;
		searching = true;
		
		if (moveQueue.peek() == null) {
				GWT.log(prefix + "Waiting for move queue content...");
				if (!wasSearching) {
					GWT.log("Starting new timer...");
					Timer t = new PollQueueTimer(this);
					t.scheduleRepeating(3000);
				}
				return;
		}
		
		searching = false;
		if (observer != null) observer.bestMove(moveQueue.poll(), -1); else GWT.log("Observer null!");
	}
	
	class PollQueueTimer extends Timer {
		XBoardEngine engine;
		PollQueueTimer(XBoardEngine engine) {
			this.engine = engine;
		}
		public void run() {
			engine.run();
			if (!engine.searching) cancel();
		}
	}
	
	boolean protocolSet = false;
	Integer protocolVersion;
	private boolean forceMode;
	
	private void processCommand(String line) {
		GWT.log(prefix + line);
		
		String[] arr = line.split(" ");
		
		String command = arr[0];
		List<String> args = Arrays.asList(arr).subList(1, arr.length);
		
		Queue<String> answerQueue = new LinkedList<String>();

		try {
			if (handlePotentialProtocol(command,args)) return;
			if (handlePotentialProtocolVersion(command,args,answerQueue)) return;

			if (command.equals("random")) {
			} else if (command.equals("quit")) {
				Window.alert("XBoard died");
			} else if (command.equals("force")) {
				
			} else if (command.equals("go")) {
				// Leave force mode and set the engine to play the color that is
				// on move. Associate the engine's clock with the color that is
				// on move, the opponent's clock with the color that is not on
				// move. Start the engine's clock. Start thinking and eventually
				// make a move.
			} else if (command.equals("new")) {
				// Reset the board to the standard chess starting position. Set
				// White on move. Leave force mode and set the engine to play
				// Black. Associate the engine's clock with Black and the
				// opponent's clock with White. Reset clocks and time controls
				// to the start of a new game. Use wall clock for time
				// measurement. Stop clocks. Do not ponder on this move, even if
				// pondering is on. Remove any search depth limit previously set
				// by the sd command.
				observer.restart();
			} else if (command.equals("playother")) {
				throw new XBoardException("unsupported", line);
			} else if (command.equals("white")) {
				throw new XBoardException("unsupported", line);
			} else if (command.equals("black")) {
				throw new XBoardException("unsupported", line);
			} else if (command.equals("level")) {
				if (args.size() != 3) throw new XBoardException("wrong number arguments, should be 3", line);
				observer.enableTime(Integer.valueOf(args.get(1)));
			} else if (command.equals("st")) {
				throw new XBoardException("unsupported", line);
			} else if (command.equals("sd")) {
				throw new XBoardException("unsupported", line);
			} else if (command.equals("nps")) {
				throw new XBoardException("unsupported", line);
			} else if (command.equals("time")) {
				if (args.size() != 1) throw new XBoardException("wrong number arguments, should be 1", line);
				observer.setClientPlayerTime(Integer.valueOf(args.get(0)));
			} else if (command.equals("otim")) {
				if (args.size() != 1) throw new XBoardException("wrong number arguments, should be 1", line);
				observer.setClientOpponentTime(Integer.valueOf(args.get(0)));
			} else if (command.equals("usermove")) {
				if (args.size() != 1) throw new XBoardException("wrong number arguments, should be 1", line);
				handleUserMove(args.get(0),answerQueue);
			} else if (command.equals("ping")) {
				if (args.size() != 1) throw new XBoardException("wrong number arguments, should be 1", line);
				answerQueue.add("pong " + args.get(0));
			} else if (command.equals("draw")) {
			} else if (command.equals("result")) {
			} else if (command.equals("setboard")) {
				if (args.size() != 1) throw new XBoardException("wrong number arguments, should be 1", line);
				observer.setFEN(args.get(0));
			} else if (command.equals("edit")) {
				throw new XBoardException("unsupported", line);
			} else if (command.equals("hint")) {
				answerQueue.add("Hint: resign");
			} else if (command.equals("bk")) {
				answerQueue.add("\tI have no book\n");
			} else if (command.equals("undo")) {
				observer.xBoardUndo();
			} else if (command.equals("remove")) {
				observer.xBoardRemove();
			} else if (command.equals("hard")) {
			} else if (command.equals("easy")) {
			} else if (command.equals("post")) {
				throw new XBoardException("unsupported", line);
			} else if (command.equals("nopost")) {
			} else if (command.equals("analyze")) {
			} else if (command.equals("name")) {
			} else if (command.equals("rating")) {
			} else if (command.equals("ics")) {
			} else if (command.equals("computer")) {
			} else if (command.equals("pause")) {
			} else if (command.equals("resume")) {
			} else if (command.equals("memory")) {
			} else if (command.equals("cores")) {
			} else if (command.equals("egtpath")) {
			} else if (command.equals("accepted")) {
			} else if (command.equals("rejected")) {
				GWT.log(prefix + "Rejected: " + args.get(0));
			} else {
				if (args.size() == 0 && command.length() <= 5) {
					handleUserMove(command, answerQueue);
				} else {
					throw new XBoardException("Unknown command",line);
				}
			}
		} catch (XBoardException e) {
			answerQueue.add(e.getMessage());
		} finally {

			while (!answerQueue.isEmpty()) {
				String msg = answerQueue.poll();
				GWT.log(prefix + "Sending: " + msg);
				commSend(msg);
			}
		}
	
	}
	
	private void handleUserMove(String move, Queue<String> answerQueue) {
		int movenum = Move.getFromString(observer.getBoard(), move);
		if (movenum == -1) {
			throw new RuntimeException("Invalid move: " + move);
		}
		moveQueue.add(movenum);
	}

	boolean handlePotentialProtocol(String command, List<String> args) throws XBoardException {
		boolean parsed = false;
		if (command.equals("xboard")) {
			if (protocolSet) {
				throw new XBoardException("protocol already set","xboard");
			}
			protocolSet = true;
			parsed = true;
		}

		if (!protocolSet) {
			throw new XBoardException("protocol not set","n/a");
		}
		return parsed;
	}
	
	boolean handlePotentialProtocolVersion(String command, List<String> args, Queue<String> answerQueue) throws XBoardException {
		boolean parsed = false;
		if (command.equals("protover")) {
			if (protocolVersion != null) {
				throw new XBoardException("protocol version already set","protover");
			}
			if (args.size() != 1) throw new XBoardException("invalid number of arguments for protocol version","protover"); 
			protocolVersion = Integer.valueOf(args.get(0));
			if (protocolVersion != 2) throw new XBoardException("unsupported protocol version", "protover");
			parsed = true;
			
			answerQueue.add("feature done=0");
			answerQueue.add("feature ping=1");
			answerQueue.add("feature setboard=1");
			answerQueue.add("feature playother=1");
			answerQueue.add("feature usermove=1");
			answerQueue.add("feature done=1");
		}

		if (protocolVersion == null) {
			throw new XBoardException("protocol version not set","n/a");
		}
		return parsed;
	}
	
	private void pumpMoves() {
		int pos;
		while ((pos = messageBuffer.indexOf('\n')) != -1) {
			String command = messageBuffer.substring(0, pos);
			messageBuffer = messageBuffer.substring(pos+1);
			
			processCommand(command);
		}
	}
	
	private static native String b64decode(String a) /*-{
		return window.atob(a);
	}-*/;
	
	private static native String b64encode(String a) /*-{
		return window.btoa(a);
	}-*/;
	
	@Override
	public void onMessage(String message) {
		GWT.log(prefix + "message: " + message);
		messageBuffer += b64decode(message);
		pumpMoves();
	}
	
	@Override
	public void onDisconnect() {
		GWT.log(prefix + "Disconnected");
		initialized = false;
//		init(connectString);
	}
	
	@Override
	public void onConnect() {
		GWT.log(prefix  + "Connected");
		initialized = true;
	}
	
	String connectString;
	
	public void init(String connectString) {
		this.connectString = connectString;
		GWT.log(prefix + " initing");
		socket = new WebSocket(this);
		socket.connect("ws://" + connectString + "/", "base64");
//		Duration initializeWaitDuration = new Duration();
//		while (!initialized) {
//			if (initializeWaitDuration.elapsedMillis() > 100) throw new RuntimeException("Waited too long!");
//		}
	}

	public void sendMove(int i) {
		commSend("move " + Move.toString(i));
	}
	
	private void commSend(String msg) {
		GWT.log(msg);
		socket.send(b64encode(msg + "\n"));
	}
}

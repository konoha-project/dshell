package dshell.lib;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import static dshell.lib.TaskOption.Behavior.background;

import dshell.util.Utils;

public class RequestReceiver {
	public void invoke() {
		try {
			System.err.println("run dshell receiver mode");
			ObjectInputStream receiver = new ObjectInputStream(System.in);
			CommandRequest request = (CommandRequest) receiver.readObject();
			System.err.println("receive command request");
			ArrayList<ArrayList<String>> cmdsList = request.getCmdsList();
			TaskOption option = request.getOption();
			if(option.is(background)) {
				new TaskBuilder(cmdsList, option).invoke();
			}
			else {
				Task task = (Task) new TaskBuilder(cmdsList, option).invoke();
				new ObjectOutputStream(System.out).writeObject(task);
			}
			System.exit(0);
		}
		catch (IOException e) {
			e.printStackTrace();
			Utils.fatal(1, "IO problem");
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
			Utils.fatal(1, "invalid class");
		}
	}
}

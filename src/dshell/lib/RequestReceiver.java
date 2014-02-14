package dshell.lib;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import static dshell.lib.TaskOption.Behavior.background;

import dshell.util.Utils;

public class RequestReceiver {
	public static void invoke(String requestString) {
		try {
			CommandRequest request = CommandRequest.decodeFromString(requestString);
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
	}
}

package dshell.remote;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import static dshell.lib.TaskOption.Behavior.background;
import dshell.lib.CommandArg;
import dshell.lib.RuntimeContext;
import dshell.lib.Task;
import dshell.lib.TaskBuilder;
import dshell.lib.TaskOption;
import dshell.lib.Utils;

public class RequestReceiver {
	public static void invoke(String requestString) {
		try {
			CommandRequest request = CommandRequest.decodeFromString(requestString);
			RuntimeContext.loadContext(request.getContext());
			ArrayList<ArrayList<CommandArg>> cmdsList = request.getCmdsList();
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

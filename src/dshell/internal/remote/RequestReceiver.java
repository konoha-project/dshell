package dshell.internal.remote;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import static dshell.internal.lib.TaskOption.Behavior.background;
import dshell.internal.lib.CommandArg;
import dshell.internal.lib.RuntimeContext;
import dshell.internal.lib.Task;
import dshell.internal.lib.TaskBuilder;
import dshell.internal.lib.TaskOption;
import dshell.internal.lib.Utils;

public class RequestReceiver {
	public static void invoke(String requestString) {
		try {
			CommandRequest request = CommandRequest.decodeFromString(requestString);
			RuntimeContext.loadContext(request.getContext());
			ArrayList<ArrayList<CommandArg>> cmdsList = request.getCmdsList();
			TaskOption option = request.getOption();
			if(option.is(background)) {
				TaskBuilder.createTask(cmdsList, option);
			}
			else {
				Task task = (Task) TaskBuilder.createTask(cmdsList, option);
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

package dshell.lib;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;

import dshell.util.Utils;

public class ClassListLoader {
	private final String packageName;

	public ClassListLoader(String packageName) {
		this.packageName = packageName;
	}

	public ArrayList<Class<?>> getClassList() {
		ArrayList<Class<?>> classList = new ArrayList<Class<?>>();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		String path = this.packageName.replace(".", "/");
		ArrayList<File> dirList = new ArrayList<File>();
		try {
			Enumeration<URL> resources = classLoader.getResources(path);
			while(resources.hasMoreElements()) {
				URL resource = resources.nextElement();
				dirList.add(new File(resource.getFile()));
			}
			for(File dir : dirList) {
				classList.addAll(this.loadClassList(packageName, dir));
			}
		}
		catch (IOException e) {
			Utils.fatal(1, "getting resource faild: " + path);
		}
		return classList;
	}

	private ArrayList<Class<?>> loadClassList(String packageName, File dir) {
		ArrayList<Class<?>> classList = new ArrayList<Class<?>>();
		if(!dir.exists()) {
			return classList;
		}

		File[] files = dir.listFiles();
		for(File file : files) {
			String fileName = file.getName();
			if(file.isDirectory()) {
				classList.addAll(this.loadClassList(packageName + "." + fileName, file));
			}
			else if(file.getName().endsWith(".class")) {
				String className = packageName + "." + fileName.substring(0, fileName.length() - 6);
				try {
					classList.add(Class.forName(className));
				}
				catch (ClassNotFoundException e) {
					Utils.fatal(1, "loading class failed: " + className);
				}
			}
		}
		return classList;
	}
}

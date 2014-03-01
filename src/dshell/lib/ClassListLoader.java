package dshell.lib;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassListLoader {
	public static ArrayList<Class<?>> loadClassList(String packageName) {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		ArrayList<Class<?>> classList = new ArrayList<Class<?>>();
		String path = packageName.replace(".", "/");
		ArrayList<URL> resourceList = new ArrayList<URL>();
		try {
			Enumeration<URL> resources = classLoader.getResources(path);
			while(resources.hasMoreElements()) {
				URL url = resources.nextElement();
				resourceList.add(url);
			}
			for(URL url : resourceList) {
				String protocol = url.getProtocol();
				if(protocol.equals("file")) {
					classList.addAll(loadFromDir(classLoader, packageName, new File(url.getFile())));
				}
				else if(protocol.equals("jar")) {
					classList.addAll(loadFromJarFile(classLoader, packageName, url));
				}
			}
		}
		catch (IOException e) {
			Utils.fatal(1, "getting resource faild: " + path);
		}
		return classList;
	}

	private static boolean isClassFile(String fileName) {
		return fileName.endsWith(".class");
	}

	private static String removeClassSuffix(String fileName) {
		return fileName.substring(0, fileName.length() - ".class".length());
	}

	private static ArrayList<Class<?>> loadFromDir(ClassLoader classLoader, String packageName, File dir) {
		ArrayList<Class<?>> classList = new ArrayList<Class<?>>();
		if(!dir.exists()) {
			return classList;
		}

		File[] files = dir.listFiles();
		for(File file : files) {
			String fileName = file.getName();
			if(file.isDirectory()) {
				classList.addAll(loadFromDir(classLoader, packageName + "." + fileName, file));
			}
			else if(isClassFile(file.getName())) {
				String className = packageName + "." + removeClassSuffix(fileName);
				try {
					classList.add(classLoader.loadClass(className));
				}
				catch (ClassNotFoundException e) {
					Utils.fatal(1, "loading class failed: " + className);
				}
			}
		}
		return classList;
	}

	private static ArrayList<Class<?>> loadFromJarFile(ClassLoader classLoader, String packageName, URL jarFileUrl) {
		ArrayList<Class<?>> classList = new ArrayList<Class<?>>();
		String path = packageName.replace(".", "/");
		try {
			JarURLConnection connection = (JarURLConnection)jarFileUrl.openConnection();
			JarFile jarFile = connection.getJarFile();
			Enumeration<JarEntry> entries = jarFile.entries();
			while(entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String entryName = entry.getName();
				if(entryName.startsWith(path) && isClassFile(entryName)) {
					String className = removeClassSuffix(entryName.replace("/", "."));
					try {
						classList.add(classLoader.loadClass(className));
					}
					catch (ClassNotFoundException e) {
						Utils.fatal(1, "loading class failed: " + className);
					}
				}
			}
			jarFile.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return classList;
	}
}

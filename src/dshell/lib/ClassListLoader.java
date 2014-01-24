package dshell.lib;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import dshell.util.Utils;

public class ClassListLoader {
	private final String packageName;
	private final ClassLoader classLoader;

	public ClassListLoader(String packageName) {
		this.packageName = packageName;
		this.classLoader = Thread.currentThread().getContextClassLoader();
	}

	public ArrayList<Class<?>> loadClassList() {
		ArrayList<Class<?>> classList = new ArrayList<Class<?>>();
		String path = this.packageName.replace(".", "/");
		ArrayList<URL> resourceList = new ArrayList<URL>();
		try {
			Enumeration<URL> resources = this.classLoader.getResources(path);
			while(resources.hasMoreElements()) {
				URL url = resources.nextElement();
				resourceList.add(url);
			}
			for(URL url : resourceList) {
				String protocol = url.getProtocol();
				if(protocol.equals("file")) {
					classList.addAll(this.loadFromDir(this.packageName, new File(url.getFile())));
				}
				else if(protocol.equals("jar")) {
					classList.addAll(this.loadFromJarFile(this.packageName, url));
				}
			}
		}
		catch (IOException e) {
			Utils.fatal(1, "getting resource faild: " + path);
		}
		return classList;
	}

	private boolean isClassFile(String fileName) {
		return fileName.endsWith(".class");
	}

	private String removeClassSuffix(String fileName) {
		return fileName.substring(0, fileName.length() - ".class".length());
	}

	private ArrayList<Class<?>> loadFromDir(String packageName, File dir) {
		ArrayList<Class<?>> classList = new ArrayList<Class<?>>();
		if(!dir.exists()) {
			return classList;
		}

		File[] files = dir.listFiles();
		for(File file : files) {
			String fileName = file.getName();
			if(file.isDirectory()) {
				classList.addAll(this.loadFromDir(packageName + "." + fileName, file));
			}
			else if(this.isClassFile(file.getName())) {
				String className = packageName + "." + this.removeClassSuffix(fileName);
				try {
					classList.add(this.classLoader.loadClass(className));
				}
				catch (ClassNotFoundException e) {
					Utils.fatal(1, "loading class failed: " + className);
				}
			}
		}
		return classList;
	}

	private ArrayList<Class<?>> loadFromJarFile(String packageName, URL jarFileUrl) {
		ArrayList<Class<?>> classList = new ArrayList<Class<?>>();
		String path = packageName.replace(".", "/");
		try {
			JarURLConnection connection = (JarURLConnection)jarFileUrl.openConnection();
			JarFile jarFile = connection.getJarFile();
			Enumeration<JarEntry> entries = jarFile.entries();
			while(entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String entryName = entry.getName();
				if(entryName.startsWith(path) && this.isClassFile(entryName)) {
					String className = this.removeClassSuffix(entryName.replace("/", "."));
					try {
						classList.add(this.classLoader.loadClass(className));
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

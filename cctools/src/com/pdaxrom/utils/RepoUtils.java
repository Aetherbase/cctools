package com.pdaxrom.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.util.Log;

public class RepoUtils {
	private static final String TAG = "RepoUtils";
	
    // XML node keys
    static public final String KEY_PACKAGE	= "package"; // parent node
    static public final String KEY_NAME		= "name";
    static public final String KEY_FILE		= "file";
    static public final String KEY_SIZE		= "size";
    static public final String KEY_FILESIZE	= "filesize";
    static public final String KEY_VERSION	= "version";
    static public final String KEY_DESC		= "description";
    static public final String KEY_DEPENDS	= "depends";
    static public final String KEY_ARCH		= "arch";
    static public final String KEY_STATUS	= "status";

	private static String	_ndkArch;
	private static int		_ndkVersion;

	public static void setVersion(String ndkArch, int ndkVersion) {
		_ndkArch = ndkArch;
		_ndkVersion = ndkVersion;
	}
	
	public static List<PackageInfo> getRepoFromUrl(String url) {
        return parseRepoXml(getRepoXmlFromUrl(url)); // getting DOM element
	}
	
	public static List<PackageInfo> getRepoFromDir(String path) {
		return parseRepoXml(getRepoXmlFromDir(path));
	}
	
	public static String getRepoXmlFromUrl(String url) {
		XMLParser parser = new XMLParser();
		String xml = parser.getXmlFromUrl(url + "/Packages");
		return replaceMacro(xml);
	}
	
	public static String getRepoXmlFromDir(String path) {
		File dir = new File(path);
		if (dir.isDirectory()) {
			StringBuilder sb = new StringBuilder();
			sb.append("<repo>").append("\n");

			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					String lowercaseName = name.toLowerCase();
					if (lowercaseName.endsWith(".pkgdesc")) {
						return true;
					} else {
						return false;
					}
				}
			};

			for (String filePath: dir.list(filter)) {
				Log.i(TAG, "Read file " + filePath);
				File f = new File(path + "/" + filePath);
				try {
					FileInputStream fin = new FileInputStream(f);
					BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
					String line = null;
					while((line = reader.readLine()) != null) {
						sb.append(line).append("\n");
					}
					reader.close();
				} catch (IOException e) {
					Log.e(TAG, "getRepoXmlFromDir() IO error " + e);
				}
			}
			sb.append("</repo>");
			Log.i(TAG, "installed xml = " + replaceMacro(sb.toString()));
			return replaceMacro(sb.toString());
		}
		return null;
	}
	
    public static boolean isContainsPackage(List<PackageInfo> repo, String pkg) {
    	for (PackageInfo packageInfo: repo) {
    		if (packageInfo.getName().contains(pkg)) {
    			return true;
    		}
    	}
    	return false;
    }

    private static List<PackageInfo> parseRepoXml(String repo) {
		List<PackageInfo> list = new ArrayList<PackageInfo>();
		
		if (repo != null) {
			XMLParser parser = new XMLParser();
	        Document doc = parser.getDomElement(repo); // getting DOM element
	        NodeList nl = doc.getElementsByTagName(KEY_PACKAGE);

	    	for (int i = 0; i < nl.getLength(); i++) {
	    		Element e = (Element) nl.item(i);
	    		PackageInfo packageInfo = new PackageInfo(
	    				parser.getValue(e, KEY_NAME),
	    				parser.getValue(e, KEY_FILE),
	    				Integer.valueOf(parser.getValue(e, KEY_SIZE)),
	    				Integer.valueOf(0 /*parser.getValue(e, KEY_FILESIZE)*/), //filesize is undefined in package info file
	    				parser.getValue(e, KEY_VERSION),
	    				parser.getValue(e, KEY_DESC),
	    				parser.getValue(e, KEY_DEPENDS),
	    				parser.getValue(e, KEY_ARCH));
				list.add(packageInfo);
				Log.i(TAG, "added pkg = " + packageInfo.getName());
	    	}
		}
		return list;
    }
    
    private static String replaceMacro(String str) {
    	if (str != null) {
    		str = str.replaceAll("\\$\\{HOSTARCH\\}", _ndkArch);
    		str = str.replaceAll("\\$\\{HOSTNDKARCH\\}", _ndkArch);
    		str = str.replaceAll("\\$\\{HOSTNDKVERSION\\}", String.valueOf(_ndkVersion));
    	}
    	return str;
    }

    public static List<PackageInfo> checkingForUpdates(List<PackageInfo> repoList, List<PackageInfo> installedList) {
    	List<PackageInfo> list = null;
    	
    	for (PackageInfo installedPkg: installedList) {
    		for (PackageInfo pkg: repoList) {
    			if (installedPkg.getName().contentEquals(pkg.getName())) {
    				if (!installedPkg.getVersion().contentEquals(pkg.getVersion())) {
    					if (list == null) {
    						list = new ArrayList<PackageInfo>();
    					}
    					list.add(pkg);
    				}
    				break;
    			}
    		}
    	}    	
    	return list;
    }
}

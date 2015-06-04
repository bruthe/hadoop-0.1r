/**
 * Copyright 2005 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.fs;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;

/**
 * Filesystem disk space usage statistics. Uses the unix 'df' program. Tested on
 * Linux, FreeBSD, Cygwin.
 */
public class DF {
	public static final long DF_INTERVAL_DEFAULT = 3 * 1000; // default DF refresh interval

	private String dirPath;
	private long dfInterval; // DF refresh interval in msec
	private long lastDF; // last time doDF() was performed

	private String filesystem;
	private long capacity;
	private long used;
	private long available;
	private int percentUsed;
	private String mount;

	// update by bruce:20150528
	private int osType;
	private final static int OS_TYPE_UNIX = 0;
	private final static int OS_TYPE_WINXP = 1;
	private String charset;

	public DF(String path, Configuration conf) throws IOException {
		this(path, conf.getLong("dfs.df.interval", DF.DF_INTERVAL_DEFAULT), conf.get("system.charset", "UTF-8"));
	}

	public DF(String path, long dfInterval, String charset) throws IOException {
		this.dirPath = path;
		this.dfInterval = dfInterval;
		this.charset = charset;
		
		lastDF = (dfInterval < 0) ? 0 : -dfInterval;

		// update by bruce:20150528
		osType = OS_TYPE_UNIX;
		String osName = System.getProperty("os.name");
		if (osName.indexOf("Windows") != -1 || (osName.indexOf("XP") != -1 || osName.indexOf("NT") != -1 || osName.indexOf("2003") != -1)) {
			osType = OS_TYPE_WINXP;
		}
		this.doDF(charset);
	}
  
	private void doDF(String charset) throws IOException {
		if (lastDF + dfInterval > System.currentTimeMillis())
			return;
		Process process;
		process = Runtime.getRuntime().exec(getExecString());

		try {
			if (process.waitFor() != 0) {
				throw new IOException(new BufferedReader(new InputStreamReader(process.getErrorStream(), charset)).readLine());
			}
			parseExecResult(new BufferedReader(new InputStreamReader(process.getInputStream(), charset)));
		} catch (InterruptedException e) {
			throw new IOException(e.toString());
		} finally {
			process.destroy();
		}
	}

	// / ACCESSORS

	public String getDirPath() {
		return dirPath;
	}

	public String getFilesystem() throws IOException {
		doDF(charset);
		return filesystem;
	}

	public long getCapacity() throws IOException {
		doDF(charset);
		return capacity;
	}

	public long getUsed() throws IOException {
		doDF(charset);
		return used;
	}

	public long getAvailable() throws IOException {
		doDF(charset);
		return available;
	}

	public int getPercentUsed() throws IOException {
		doDF(charset);
		return percentUsed;
	}

	public String getMount() throws IOException {
		doDF(charset);
		return mount;
	}
  
	public String toString() {
	    return
	      "df -k " + mount +"\n" +
	      filesystem + "\t" +
	      capacity / 1024 + "\t" +
	      used / 1024 + "\t" +
	      available / 1024 + "\t" +
	      percentUsed + "%\t" +
	      mount;
	}

	// private String[] getExecString() {
	// return new String[] {"df","-k",dirPath};
	// }
	// update by bruce:20150528
	private String[] getExecString() {
		switch (osType) {
		case OS_TYPE_WINXP:
			return new String[] { "fsutil", "volume", "diskfree", getDirPath() };
		case OS_TYPE_UNIX:
		default:
			return new String[] { "df", "-k", dirPath };
		}
	}
  
	private void parseExecResult(BufferedReader lines) throws IOException {
//		lines.readLine(); // skip headings
//
//		StringTokenizer tokens = new StringTokenizer(lines.readLine()," \t\n\r\f%");
//
//		this.filesystem = tokens.nextToken();
//		if (!tokens.hasMoreTokens()) { // for long filesystem name
//			tokens = new StringTokenizer(lines.readLine(), " \t\n\r\f%");
//		}
//		this.capacity = Long.parseLong(tokens.nextToken()) * 1024;
//		this.used = Long.parseLong(tokens.nextToken()) * 1024;
//		this.available = Long.parseLong(tokens.nextToken()) * 1024;
//		this.percentUsed = Integer.parseInt(tokens.nextToken());
//		this.mount = tokens.nextToken();
//		this.lastDF = System.currentTimeMillis();
		
		switch (osType) {
		case OS_TYPE_WINXP:
			parseWinExecResult(lines);
			break;
		case OS_TYPE_UNIX:
		default:
			parseUnixExecResult(lines);
		}
	}
  
	private void parseUnixExecResult(BufferedReader lines) throws IOException {
		lines.readLine(); // skip headings
	
		String line = lines.readLine();
		if (line == null) {
			throw new IOException("Expecting a line not the end of stream");
		}
	
		StringTokenizer tokens = new StringTokenizer(line, " \t\n\r\f%");
		this.filesystem = tokens.nextToken();
		if (!tokens.hasMoreTokens()) { // for long filesystem name
			line = lines.readLine();
			if (line == null) {
				throw new IOException("Expecting a line not the end of stream");
			}
			tokens = new StringTokenizer(line, " \t\n\r\f%");
		}
		this.capacity = Long.parseLong(tokens.nextToken()) * 1024;
		this.used = Long.parseLong(tokens.nextToken()) * 1024;
		this.available = Long.parseLong(tokens.nextToken()) * 1024;
		this.percentUsed = Integer.parseInt(tokens.nextToken());
		this.mount = tokens.nextToken();
		this.lastDF = System.currentTimeMillis();
	}
	
	private static ResourceBundle resourceBundle = ResourceBundle.getBundle("windisk",new Locale("zh", "CN"));
	private void parseWinExecResult(BufferedReader lines) throws IOException {
		this.filesystem = dirPath.substring(0, dirPath.indexOf(':') + 1);
		String line;
		for (line = lines.readLine(); line != null; line = lines.readLine()) {
			if (line.equals(""))
				continue;
			if (line.startsWith(resourceBundle.getString("windows.TotalBytes"))) {
				this.capacity = Long.parseLong(line.substring(line.lastIndexOf(' ') + 1, line.length()));
				continue;
			}
			if (line.startsWith(resourceBundle.getString("windows.TotalFreeBytes"))) {
				this.available = Long.parseLong(line.substring(line.lastIndexOf(' ') + 1, line.length()));
				continue;
			}
		}
		this.used = capacity - available;
		this.percentUsed = (int) ((used * 100) / capacity);
		this.mount = filesystem + "/"; // windows mounting is not supported
	}
  
	public static void main(String[] args) throws Exception {
		String path = ".";
		if (args.length > 0)
			path = args[0];

		System.out.println(new DF(path, DF_INTERVAL_DEFAULT, "GB2312").toString());
	}
}

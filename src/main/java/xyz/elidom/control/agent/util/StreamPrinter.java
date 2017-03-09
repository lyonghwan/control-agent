package xyz.elidom.control.agent.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Stream Based Printer
 *  
 * @author shortstop
 */
public class StreamPrinter {

	public static void printStream(Process process) throws IOException, InterruptedException {
		process.waitFor();
		try (InputStream psout = process.getInputStream()) {
			copy(psout, System.out);
		}
	}

	public static void copy(InputStream input, OutputStream output) throws IOException {
		byte[] buffer = new byte[1024];
		int n = 0;
		while ((n = input.read(buffer)) != -1) {
			output.write(buffer, 0, n);
		}
	}
}

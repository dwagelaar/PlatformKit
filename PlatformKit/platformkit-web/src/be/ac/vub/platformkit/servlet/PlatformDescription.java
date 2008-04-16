package be.ac.vub.platformkit.servlet;

import java.io.*;

public class PlatformDescription {
	private java.lang.String browserID = null;

	private byte[] data = null;

	public java.io.InputStream getInputStream() {
		return new ByteArrayInputStream(getData());
	}

	public void setFromInputStream(java.io.InputStream stream)
			throws java.io.IOException {
		byte[] buf = new byte[1024];
		ByteArrayOutputStream out = new ByteArrayOutputStream(buf.length);
		for (int read = stream.read(buf); read > -1; read = stream.read(buf)) {
			out.write(buf, 0, read);
		}
		out.flush();
		setData(out.toByteArray());
	}

	public java.lang.String getBrowserID() {
		return browserID;
	}

	public void setBrowserID(java.lang.String browserID) {
		this.browserID = browserID;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

}

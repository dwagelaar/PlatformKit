package be.ac.vub.platformkit.servlet;

import java.io.*;

public class StreamData implements java.io.Serializable {
private byte[] data = null;

public java.io.InputStream getInputStream() {
return new ByteArrayInputStream(getData());
}

public byte[] getData() {
return data;
}

public void setData(byte[] data) {
this.data = data;
}

}


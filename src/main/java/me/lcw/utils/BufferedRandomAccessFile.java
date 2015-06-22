package me.lcw.utils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class BufferedRandomAccessFile extends RandomAccessFile {
  public static final int DEFAULT_BUFFER_SIZE = 1024;
  private byte[] buffer;
  private int bufPos = 0;
  private int bufEnd = 0;

  
  public BufferedRandomAccessFile(File file, String ops) throws FileNotFoundException{
    this(file, ops, DEFAULT_BUFFER_SIZE);
  }
  
  public BufferedRandomAccessFile(File file, String ops, int bufSize) throws FileNotFoundException {
    super(file, ops);
    buffer = new byte[bufSize];
  }
  
  @Override
  public long getFilePointer() throws IOException {
    return (super.getFilePointer() - bufEnd) + bufPos;
  }
  
  @Override
  public int read() throws IOException {
    if(bufEnd - bufPos <= 0) {
      if(super.getFilePointer() >= super.length()) {
        return -1;
      }
      bufEnd = (int)Math.min(buffer.length, super.length() - super.getFilePointer());
      super.read(buffer, 0, bufEnd);
      bufPos = 0;
    }
    return buffer[bufPos++] & 0xff;
  }
  
  @Override
  public int read(byte[] ba) throws IOException {
    return read(ba, 0, ba.length);
  }
  
  @Override
  public int read(byte[] ba, int off, int len) throws IOException {
    if(len == 0) {
      return 0;
    }
    if(len <= (bufEnd - bufPos)) {
      System.arraycopy(buffer, bufPos, ba, off, len);
      bufPos+=len;
      return len;
    } else {
      if(bufEnd - bufPos > 0) {
        super.seek(getFilePointer());
        bufPos = 0;
        bufEnd = 0;
      }
      return super.read(ba, off, len);
    }
  }
  
  @Override
  public void seek(long position) throws IOException {
    if(bufEnd - bufPos == 0 || position >= super.getFilePointer()) {
      bufPos = 0;
      bufEnd = 0;
      super.seek(position);
    } else {
      bufPos += (int) (position - getFilePointer());
    }
  }
  
}

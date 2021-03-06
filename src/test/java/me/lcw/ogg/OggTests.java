package me.lcw.ogg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import javax.imageio.ImageIO;

import me.lcw.utils.Base64;

import org.junit.Test;

public class OggTests {
  
  @Test
  public void t2() throws IOException, NoSuchAlgorithmException {
    Random rnd = new Random();
    for(int i=1; i<201; i++) {
      byte[] ba = new byte[i];
      rnd.nextBytes(ba);
      String s = Base64.encode(ba);
      assertTrue(Arrays.equals(ba, Base64.decode(s)));
    }
  }

  @Test
  public void noHeadersTest() throws IOException, NoSuchAlgorithmException {
    String filename = ClassLoader.getSystemClassLoader().getResource("Simple-Test.ogg").getFile();
    OggFile ohp = OggFile.parse(filename);
    System.out.println(filename);
    assertEquals("Test", ohp.getTitle());
    assertEquals("Simple", ohp.getArtist());
  }
  
  @Test
  public void fullHeadersTest() throws IOException, NoSuchAlgorithmException {
    String filename = ClassLoader.getSystemClassLoader().getResource("silence.ogg").getFile();
    OggFile ohp = OggFile.parse(filename);
    System.out.println(filename);
    for(Entry<VorbisTags, List<String>> entry: ohp.getAllTag().entrySet()) {
      for(String s: entry.getValue()) {
        System.out.println(entry.getKey()+":"+s);
        if(entry.getKey() == VorbisTags.METADATA_BLOCK_PICTURE) {
          ByteBuffer bb = ByteBuffer.wrap(Base64.decode(s));
          int type = bb.getInt();
          if(type == 3) {
            int mime_len = bb.getInt();
            bb.position(bb.position() + mime_len);
            int desc_len = bb.getInt();
            bb.position(bb.position() + desc_len);
            bb.getInt();
            bb.getInt();
            bb.getInt();
            bb.getInt();
            int size = bb.getInt();
            byte[] ba = new byte[size];
            bb.get(ba);
            assertEquals("f79937cbdfde18cb05466d68438206db29d1ee32b2ab7f11c9116087dd415a98", hashByteArray(ba));
          } else if(type == 4) {
            int mime_len = bb.getInt();
            bb.position(bb.position() + mime_len);
            int desc_len = bb.getInt();
            bb.position(bb.position() + desc_len);
            bb.getInt();
            bb.getInt();
            bb.getInt();
            bb.getInt();
            int size = bb.getInt();
            byte[] ba = new byte[size];
            bb.get(ba);
            assertEquals("3d9ec87e1d2146e7a147777cfd7323a848f87197ed2b7c06e8e74d262b960653", hashByteArray(ba));
          }
        }
      }
    }
    assertEquals("This is a Comment!", ohp.getFirstTag(VorbisTags.COMMENT));
    assertEquals("SomeTitle", ohp.getTitle());
    assertEquals("SomeArtist", ohp.getArtist());
    assertEquals("SomeAlbum", ohp.getAlbum());
    assertEquals("01", ohp.getFirstTag(VorbisTags.TRACKNUMBER));
    assertEquals("Classic", ohp.getFirstTag(VorbisTags.GENRE));
    assertEquals("1999", ohp.getFirstTag(VorbisTags.DATE));
    assertEquals(2, ohp.getChannels());
    assertEquals(1152122880, ohp.getSampleRate());
    assertEquals(0, ohp.getVersion());
    BufferedImage bi = ImageIO.read(new ByteArrayInputStream(ohp.getCover()));
    byte[] ba = intsToBytes(bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), null, 0, bi.getWidth()), (byte) 32);
    assertEquals("d6fe746e830870c83c8b4654f20949fea3bd2eeaa631f6363f22b63bee26f370", hashByteArray(ba));
  }
  
  
  public static String hashByteArray(byte[] data) throws NoSuchAlgorithmException, IOException {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.update(data);
    byte[] mdbytes = md.digest();
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < mdbytes.length; i++) {
      sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
    }
    return sb.toString();
  }
  
  public static byte[] intsToBytes(int[] array, byte bpp) {
    byte cp = (byte) (bpp/8);
    byte[] nArray = new byte[array.length*cp];
    for (int i =0; i< array.length; i++) {
      int c = i*cp;
      nArray[c] = (byte) ((array[i] >> 16) & 0xff);
      nArray[c+1] = (byte) ((array[i] >> 8) & 0xff);
      nArray[c+2] = (byte) (array[i] & 0xff);
      if (cp == 4) {
        nArray[c+3] = (byte) ((array[i] >> 24) & 0xff);
      }
    }
    return nArray;
  }
}

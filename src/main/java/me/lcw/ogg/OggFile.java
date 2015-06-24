package me.lcw.ogg;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import me.lcw.utils.Base64;
import me.lcw.utils.BufferedRandomAccessFile;

public class OggFile {
  private static final String START_TAG = "OggS";
  private final File oggFile;
  private final String oggFilePath;
  private final int version;
  private final int channels;
  private final int sampleRate;
  @SuppressWarnings("unused")
  private final int bitRateMax;
  @SuppressWarnings("unused")
  private final int bitRateNom;
  @SuppressWarnings("unused")
  private final int bitRateMin;
  @SuppressWarnings("unused")
  private final int blocksize0;
  @SuppressWarnings("unused")
  private final int blocksize1;
  @SuppressWarnings("unused")
  private final int framing;
  private final HashMap<VorbisTags, List<String>> tags;


  protected OggFile(String oggFilePath, int version, int channels, int sampleRate, int bitRateMax, int bitRateNom, 
      int bitRateMin, int blocksize0, int blocksize1, int framing, HashMap<VorbisTags, List<String>> tags) {
    this.oggFilePath = oggFilePath;
    this.oggFile = new File(oggFilePath);
    this.version = version;
    this.channels = channels;
    this.sampleRate = sampleRate;
    this.bitRateMax = bitRateMax;
    this.bitRateNom = bitRateNom;
    this.bitRateMin = bitRateMin;
    this.blocksize0 = blocksize0;
    this.blocksize1 = blocksize1;
    this.framing = framing;
    this.tags = tags;

    if(getFirstTag(VorbisTags.ARTIST) == null && getFirstTag(VorbisTags.TITLE) == null) {
      String[] tmp = oggFile.getName().split("-");
      if(tmp.length == 2) {
        List<String> artList = new ArrayList<String>();
        List<String> titleList = new ArrayList<String>();
        artList.add(tmp[0].replace(".", " "));
        titleList.add(tmp[1].substring(0, tmp[1].length()-4).replace(".", " "));
        tags.put(VorbisTags.ARTIST, Collections.unmodifiableList(artList));
        tags.put(VorbisTags.TITLE, Collections.unmodifiableList(titleList));
      } else if (oggFile.getName().length() > 4){
        List<String> artList = new ArrayList<String>();
        artList.add(oggFile.getName().substring(0, oggFile.getName().length()-4));
        tags.put(VorbisTags.ARTIST, Collections.unmodifiableList(artList));
      }
    }
  }

  public String getFilePath() {
    return oggFilePath;
  }

  public int getChannels() {
    return channels;
  }
  
  public int getVersion() {
    return version;
  }
  
  public int getSampleRate() {
    return sampleRate;
  }
  
  public List<String> getListOfTags(VorbisTags tag) {
    return tags.get(tag);
  }

  public String getFirstTag(VorbisTags tag) {
    List<String> sl = tags.get(tag);
    if(sl == null) {
      return null;
    }
    return sl.get(0);
  }
  
  public String getTitle() {
    return getFirstTag(VorbisTags.TITLE);
  }
  
  public String getArtist() {
    return getFirstTag(VorbisTags.ARTIST);
  }
  
  public String getAlbum() {
    return getFirstTag(VorbisTags.ALBUM);
  }
  
  public byte[] getCover() throws IOException {
    if(getFirstTag(VorbisTags.COVERART) != null) {
      return Base64.decode(getFirstTag(VorbisTags.COVERART));
    }
    if(getFirstTag(VorbisTags.METADATA_BLOCK_PICTURE) != null) {
      for(String s: getListOfTags(VorbisTags.METADATA_BLOCK_PICTURE)) {
        ByteBuffer bb = ByteBuffer.wrap(Base64.decode(s));
        if(bb.getInt() == 3) {
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
          return ba;
        }
      }
    }
    return null;
  }

  public Map<VorbisTags, List<String>> getAllTag() {
    return Collections.unmodifiableMap(tags);
  }

  public static OggFile parse(String oggFile) throws IOException {
    return parse(new File(oggFile));
  }

  public static OggFile parse(File oggFile) throws IOException {
    boolean foundTags = false;
    boolean foundStats = false;
    int version = 0;
    int channels= 0;
    int sampleRate= 0;
    int bitRateMax= 0;
    int bitRateNom= 0;
    int bitRateMin= 0;
    int blocksize0= 0;
    int blocksize1= 0;
    int framing= 0;
    HashMap<VorbisTags, List<String>> tags = new HashMap<VorbisTags, List<String>>();
    
    RandomAccessFile raf = new BufferedRandomAccessFile(oggFile, "r", 1024*32);    
    try{
      List<OggHeader> headers = new ArrayList<OggHeader>();
      while(headers.size() < 10 && (!foundTags || !foundStats)) {
        OggHeader h = parseHeader(raf, true);
        for(Segment cseg: h.segments) {
          if(cseg.bb.remaining() > 7) {
            int vorbis_tag_id =  cseg.bb.get();
            byte[] ba = new byte[6];
            cseg.bb.get(ba);
            String vorbis_tag = new String(ba);
            if(vorbis_tag_id == 1 && vorbis_tag.equals("vorbis")) {
              foundStats = true;
              version = cseg.bb.getInt();
              channels = cseg.bb.get();
              sampleRate = cseg.bb.getInt();
              bitRateMax = cseg.bb.getInt();
              bitRateNom = cseg.bb.getInt();
              bitRateMin = cseg.bb.getInt();
            } else if (vorbis_tag_id == 3 && vorbis_tag.equals("vorbis")) {
              foundTags = true;
              cseg.bb.order(ByteOrder.LITTLE_ENDIAN);
              int ts = cseg.bb.getInt();
              byte[] tba = new byte[ts];
              cseg.bb.get(tba);
              //String ventag = new String(tba);
              int tag_number = cseg.bb.getInt();
              for(int i=0; i<tag_number; i++) {
                int tag_l = cseg.bb.getInt();
                byte[] tag_ba = new byte[tag_l];
                cseg.bb.get(tag_ba);
                String ttag = new String(tag_ba);
                String[] tmp = ttag.split("=");
                if(tmp.length == 2) {
                  try{
                    VorbisTags enumvTag = VorbisTags.valueOf(tmp[0].toUpperCase());
                    List<String> nList = new ArrayList<String>();
                    if(tags.get(enumvTag) != null) {
                      nList.addAll(tags.get(enumvTag));
                    }
                    nList.add(tmp[1]);
                    tags.put(enumvTag, Collections.unmodifiableList(nList));
                  } catch(IllegalArgumentException e) {
                    //TODO: might look into making the key a string and allowing any arbitrary tags
                  }
                }
              }
            }
          }
        }
        headers.add(h);
      }
    }finally {
      raf.close();
    }
    if(foundTags && foundStats) {
      return new OggFile(oggFile.getAbsolutePath(), version, channels, sampleRate, bitRateMax, bitRateNom,  bitRateMin, blocksize0, blocksize1, framing, tags);
    } else {
      throw new IllegalArgumentException("Problems parsing OggFile!");
    }
  }

  public static OggHeader parseHeader(RandomAccessFile raf, boolean cont) throws IOException {
    OggHeader oh = new OggHeader();
    byte[] header = new byte[4];
    raf.read(header);
    if(!new String(header).equals(START_TAG)) {
      throw new IllegalArgumentException("Not a Valid Ogg File!!! " + new String(header));
    }
    raf.skipBytes(10);
    oh.serial = raf.readInt();
    raf.readInt();
    oh.checksum = raf.readInt();
    oh.segments = getSegments(raf, cont);
    return oh;
  }

  public static Segment[] getSegments(RandomAccessFile raf, boolean cont) throws IOException {
    int segs = raf.read();
    LinkedList<Integer> segSizes = new LinkedList<Integer>();
    int last = 0;
    for(int i=0;i<segs;i++) {
      int size = raf.read();
      if(last == 255) {
        int tmp = segSizes.pollLast();
        tmp+=size;
        segSizes.add(tmp);
      } else {
        segSizes.add(size);
      }
      last = size;
    }
    Segment[] segments = new Segment[segSizes.size()];
    for(int i=0; i<segSizes.size(); i++) {
      byte[] ba = new byte[segSizes.get(i)];
      raf.read(ba);
      segments[i] = new Segment();
      segments[i].size = segSizes.get(i);
      segments[i].bb = ByteBuffer.wrap(ba);
      segments[i].last = last;
    }
    if(last == 255 && cont == true) {
      while(true) {
        OggHeader oh = parseHeader(raf, false);
        Segment fseg = segments[segSizes.size() - 1];
        Segment nseg = oh.segments[0];
        int fs = fseg.bb.remaining();
        int ns = nseg.bb.remaining();
        byte[] ba = new byte[fs + ns];
        Segment doneseg = new Segment();
        fseg.bb.get(ba, 0, fs);
        nseg.bb.get(ba, fs, ns);
        doneseg.bb = ByteBuffer.wrap(ba);
        doneseg.size = fs + ns;
        segments[segSizes.size() - 1] = doneseg;
        if(oh.segments.length > 1 || oh.segments[0].last != 255) {
          break;
        }
      }
    }
    return segments;
  }

  public static class OggHeader {
    int serial;
    int checksum;
    Segment[] segments;
  }

  public static class Segment {
    int size;
    ByteBuffer bb;
    int last;
  }
}

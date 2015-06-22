#OggHeaderParser
This is a simple ogg header parser for Java.  Its made to be very lightweight and simple to use.

##Example

```java
public class Example {
    public static void main(String[] args) {
        OggFile of = OggFile.parse('/path/to/file.ogg');
        System.out.println("Title:"+of.getTitle());
        System.out.println("Artist:"+of.getArtist());
        if(of.getCover() != null) {
            System.out.println("Has Cover Image");
        } else {
            System.out.println("No Cover Image");
        }
    }
}
```
# JFXToolsAndDemos

A collection of tools and demos for JavaFX.

## VLCJFXVideoPlayer

This is a very simple example of a video player which uses the new WritableImage
of JavaFX 13 with support for Buffers. The idea is to let VLC directly render into
this buffer and use the image directly in an ImageView without any explicit rendering
into a canvas or such thing. Only this brings the desired performance boost.

What I have not considered yet is any kind of synchronization.
I think an extension of the PixelBuffer to support some kind of double-buffering
would be the right thing to do.

This should work on macOS and Linux but there currently seem to be problems with the VLC code
itself on Windows.

In order to run the code, a recent version of the VLC player (3.0.x+) must be installed
in a standard location on the system. Other dependencies can be found in the pom.xml.

Tested on macOS 10.14.6 and Linux.

### Build

From the top-level project directory call:

```
mvn clean install
```

### Run

From the top-level project directory call:

```
mvn exec:java -pl jfxtools-vlcjfx-demos -Dexec.mainClass=de.mpmediasoft.jfxtools.vlcjfx.demo.VLCJFXVideoPlayerDemo1Launcher
```

or

```
mvn exec:java -pl jfxtools-vlcjfx-demos -Dexec.mainClass=de.mpmediasoft.jfxtools.vlcjfx.demo.VLCJFXVideoPlayerDemo2Launcher
```





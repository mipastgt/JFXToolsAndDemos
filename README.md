# JFXToolsAndDemos

A collection of tools and demos for JavaFX.

## Build

From the top-level project directory call:

```
mvn clean install
```

## Modules

### VLCJFXVideoPlayer

This is a very simple example of a video player which uses the new WritableImage
of JavaFX 13 with support for Buffers to improve performance. The idea is to let the
well known [VLC media player](http://www.videolan.org/vlc/), which can play almost
everything you throw at it, directly render into
this buffer and use the image directly in an ImageView without any explicit rendering
into a canvas or such thing. Only this brings the desired performance boost.

What I have not considered yet is any kind of synchronization.
I think an extension of the PixelBuffer to support some kind of double-buffering
would be the right thing to do.

In order to run the code, a recent version of the VLC player (3.0.x+) must be installed
in a standard location on the system.

This should work on macOS and Linux but there currently seem to be problems with the VLC code
itself on Windows. Tested on macOS 10.14.6 and Linux.

This software is based on the [VLCJ project](https://github.com/caprica/vlcj).
Other dependencies can be found in the pom.xml.

#### Run

From the top-level project directory call:

```
mvn exec:java -pl jfxtools-vlcjfx-demos -Dexec.mainClass=de.mpmediasoft.jfxtools.vlcjfx.demo.VLCJFXVideoPlayerDemo1Launcher
```

or

```
mvn exec:java -pl jfxtools-vlcjfx-demos -Dexec.mainClass=de.mpmediasoft.jfxtools.vlcjfx.demo.VLCJFXVideoPlayerDemo2Launcher
```

### AWTImage

A simple wrapper arround an AWT image which utilizes the new WritableImage
of JavaFX 13 with support for Buffers. Internally a JavaFX image is created
which directly uses the same memory as the AWT image. So if you render
into the AWT image with a AWT graphics context, the result will immediately
appear on the screen.

#### Run

There are two examples for AWTImage. The first one just does some drawing and demonstrates the update behaviour
and the second one utilizes AWTImage to build a little JavaFX PDF viewer based on Apache PDFBox.

From the top-level project directory call:

```
mvn exec:java -pl jfxtools-awtimage-demos -Dexec.mainClass=de.mpmediasoft.jfxtools.awtimage.demo.AWTImageDemo
```

or

```
mvn exec:java -pl jfxtools-awtimage-demos -Dexec.mainClass=de.mpmediasoft.jfxtools.awtimage.demo.PDFViewerDemo"
```


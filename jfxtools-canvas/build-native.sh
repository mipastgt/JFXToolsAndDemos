#!/bin/bash

# Building the native parts of the project.
# It follows https://www.baeldung.com/jni
# For Windows you will need to have http://www.mingw.org/ installed.
# It should work for macOS out of the box but the script
# is totally untested on Linux and Windows (with MinGW)

cd `dirname $0`

JSRC=src/main/java
CSRC=src/main/c
LIBS=target/libs
TINC=target/include
TTMP=target/tmp

mkdir -p $LIBS
mkdir -p $TINC
mkdir -p $TTMP

echo "Generate JNI C header file"
javac -h $TINC $JSRC/de/mpmediasoft/jfxtools/canvas/NativeRenderer.java
rm $JSRC/de/mpmediasoft/jfxtools/canvas/NativeRenderer.class

if [[ "$OSTYPE" == "linux-gnu" ]]; then
    echo "Creating native library for Linux"
    gcc -c -fPIC -I${TINC} -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux \
    $CSRC/de_mpmediasoft_jfxtools_canvas_NativeRenderer.c \
    -o $TTMP/de_mpmediasoft_jfxtools_canvas_NativeRenderer.o
    gcc -shared -o $LIBS/libnativerenderer.so $TTMP/de_mpmediasoft_jfxtools_canvas_NativeRenderer.o -lc        

elif [[ "$OSTYPE" == "darwin"* ]]; then
    echo "Creating native library for macOS"
    gcc -c -fPIC -I${TINC} -I${JAVA_HOME}/include -I${JAVA_HOME}/include/darwin \
    $CSRC/de_mpmediasoft_jfxtools_canvas_NativeRenderer.c \
    -o $TTMP/de_mpmediasoft_jfxtools_canvas_NativeRenderer.o
    gcc -dynamiclib -o $LIBS/libnativerenderer.dylib $TTMP/de_mpmediasoft_jfxtools_canvas_NativeRenderer.o -lc        

elif [[ "$OSTYPE" == "cygwin" ]]; then
    # POSIX compatibility layer and Linux environment emulation for Windows
    echo "Currently unsupported OS"

elif [[ "$OSTYPE" == "msys" ]]; then
    echo "Creating native library for Lightweight shell and GNU utilities compiled for Windows (part of MinGW)"    
    gcc -c -I${TINC} -I%JAVA_HOME%\include -I%JAVA_HOME%\include\win32 $CSRC\de_mpmediasoft_jfxtools_canvas_NativeRenderer.c -o $TTMP\de_mpmediasoft_jfxtools_canvas_NativeRenderer.o    
    gcc -shared -o $LIBS\libnativerenderer.dll $TTMP\de_mpmediasoft_jfxtools_canvas_NativeRenderer.o -Wl,--add-stdcall-alias

elif [[ "$OSTYPE" == "win32" ]]; then
    # I'm not sure this can happen.
    echo "Currently unsupported OS"

elif [[ "$OSTYPE" == "freebsd"* ]]; then
    # ...
    echo "Currently unsupported OS"

else
    # Unknown.
    echo "Unknown OS"
fi


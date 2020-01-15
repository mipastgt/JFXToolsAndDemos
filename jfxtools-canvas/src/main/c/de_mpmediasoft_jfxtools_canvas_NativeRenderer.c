/*
A C implementation of the JNI interface as defined in NativeRenderer.java.
The code in here is not really relevant for the example. It just renders a map
consisting of quadratic red and green tiles on a blue background. It is
basically just a placeholder for some real code which uses, e.g., OpenGL or
some other rendering library to create an image representation in a memory buffer.
*/

#include "de_mpmediasoft_jfxtools_canvas_NativeRenderer.h"
#include <stdlib.h>
#include <stdio.h>

#define FALSE 0
#define TRUE !(FALSE)

// Using fbo_clear() without double-buffering may cause flickering.
// We use that here to show the effectiveness of using double-buffering.
int USE_FBO_CLEAR = TRUE;

int *bak_buf = 0;
int *buf = 0;
long buf_single_size_int = 0;
jlong buf_total_size_byte = 0;

int buffers = 0;
int current_buffer_index = 0;
int current_buffer_offset_int = 0;

int view_x = 0;
int view_y = 0;
int view_width = 0;
int view_height = 0;

int num_tiles_x = 11; // must be an odd number
int num_tiles_y = 11; // "
int tile_size = 256;

int alpha_mask = 0xFF000000;
int even_color = 0xFFFF0000;
int odd_color = 0xFF00FF00;
int bg_color = 0xFF0000FF;

void fbo_clear() {
    int c = bg_color | alpha_mask;
    for (int i = 0; i < buf_single_size_int; ++i) {
        buf[current_buffer_offset_int + i] = c;
    }
}

void fbo_fill(int minx, int miny, int maxx, int maxy, int color) {
    int c = color | alpha_mask;
    for (int y = miny; y <= maxy; ++y) {
        if (0 <= y & y < view_height) {
            int row_offset_int = view_width * y;
            for (int x = minx; x <= maxx; ++x) {
                if (0 <= x & x < view_width) {
                    buf[current_buffer_offset_int + row_offset_int + x] = c;
                }
            }
        }
    }
}

void fill(int minx, int miny, int maxx, int maxy, int color) {
    int fbo_minx = minx - view_x;
    int fbo_miny = miny - view_y;
    int fbo_maxx = maxx - view_x;
    int fbo_maxy = maxy - view_y;
    if (((0 <= fbo_minx && fbo_minx < view_width)  || (0 <= fbo_maxx && fbo_maxx < view_width)) &&
        ((0 <= fbo_miny && fbo_miny < view_height) || (0 <= fbo_maxy && fbo_maxy < view_height))) {
        fbo_fill(fbo_minx, fbo_miny, fbo_maxx, fbo_maxy, color);
    }
}

JNIEXPORT void JNICALL Java_de_mpmediasoft_jfxtools_canvas_NativeRenderer_init (JNIEnv * env, jobject thisObject) {
    // Nothing to be done yet.
}

JNIEXPORT void JNICALL Java_de_mpmediasoft_jfxtools_canvas_NativeRenderer_dispose (JNIEnv* env, jobject thisObject) {
    if (bak_buf != 0) free(bak_buf);
    if (buf != 0) free(buf);
}

JNIEXPORT jobject JNICALL Java_de_mpmediasoft_jfxtools_canvas_NativeRenderer_createCanvas (JNIEnv * env, jobject thisObject, jint width, jint height, jint numBuffers, jint nativeColorModel) {
    if (1 <= numBuffers && numBuffers <= 2 && nativeColorModel == 0) {
        if (USE_FBO_CLEAR && numBuffers == 1) {
            fprintf(stdout, "Using fbo_clear() without double-buffering may cause flickering.\n"); fflush(stdout); 
        }
        view_x = 0;
        view_y = 0;
        view_width = width;
        view_height = height;
        
        buffers = numBuffers;
        current_buffer_index = 0;

        buf_single_size_int = view_width * view_height;
        current_buffer_offset_int = 0;
        buf_total_size_byte = buf_single_size_int * buffers * sizeof(int);
        
        // Delay cleanup of buffer because it may still be used by the rendering thread.
        if (bak_buf != 0) free(bak_buf);
        bak_buf = buf;
        
        buf = (int *) malloc(buf_total_size_byte);    
        return (*env)->NewDirectByteBuffer(env, buf, buf_total_size_byte);               
    } else {
        return 0L;
    }
}

JNIEXPORT void JNICALL Java_de_mpmediasoft_jfxtools_canvas_NativeRenderer_moveTo (JNIEnv* env, jobject thisObject, jint x, jint y) {
    view_x = x;
    view_y = y;
}

JNIEXPORT jint JNICALL Java_de_mpmediasoft_jfxtools_canvas_NativeRenderer_render (JNIEnv* env, jobject thisObject) {
    if (buf != 0) {
        ++current_buffer_index;
        if (current_buffer_index >= buffers) current_buffer_index = 0;
            
        current_buffer_offset_int = current_buffer_index * buf_single_size_int;

        if (USE_FBO_CLEAR) {
            fbo_clear(); // This flickers without double-buffering.
        } else {
            if (view_x < 0) {fbo_fill(0, 0, -1 - view_x, view_height,  bg_color);}
            if (view_y < 0) {fbo_fill(0, 0, view_width,   -1 - view_y, bg_color);}
            
            int map_width = num_tiles_x * tile_size;
            int map_height = num_tiles_y * tile_size;
            
            if (view_x >= map_width - view_width) {fbo_fill(view_width - (view_x - map_width + view_width), 0, view_width, view_height,  bg_color);}
            if (view_y >= map_height - view_height) {fbo_fill(0, view_height - (view_y - map_height + view_height), view_width, view_height,  bg_color);}
        }
        
        int k = 0;
        for (int i = 0; i < num_tiles_y; ++i) {
             for (int j = 0; j < num_tiles_x; ++j) {
                int minx = j * tile_size;
                int miny = i * tile_size;
                int maxx = minx + tile_size - 1;
                int maxy = miny + tile_size - 1;
                fill(minx, miny, maxx, maxy, (k%2 == 0) ? even_color : odd_color);
                ++k;
            }
        }
        
        return current_buffer_index;
    }
    return 0;
}


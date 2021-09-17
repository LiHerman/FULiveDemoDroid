/* Read a yuv file directly and play with infinte loop
 *
 * Example:
 * $ ./yuv420_infiniteloop /dev/video1 akiyo_qcif.yuv 176 144 30

 * This will loop a yuv file named akiyo_qcif.yuv over video 1
 * 
 * Modified by T. Xu <x.tongda@nyu.edu> from yuv4mpeg_to_v4l2 example, 
 * original Copyright (C) 2011  Eric C. Cooper <ecc@cmu.edu>
 * Released under the GNU General Public License
 */
#include <stdio.h>
#include <stdbool.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <linux/videodev2.h>
#include <jni.h>
#include <android/log.h>

static const char *TAG = "yuv_setup";
char *prog;
int deviceId=-1;

struct yuv_setup {
    char *device;
    char *file_name;
    int frame_width;
    int frame_height;
    int frame_bytes;
    float fps;
};

void
fail(char *msg) {
    fprintf(stderr, "%s: %s\n", prog, msg);
    exit(1);
}

struct yuv_setup
process_args(int argc, char **argv) {
    prog = argv[0];
    struct yuv_setup setup;
    if (argc != 6) {
        fail("invalid argument");
    } else {
        setup.device = argv[1];
        setup.file_name = argv[2];
        setup.frame_width = atoi(argv[3]);
        setup.frame_height = atoi(argv[4]);
        setup.frame_bytes = 3 * setup.frame_height * setup.frame_width / 2;
        setup.fps = atof(argv[5]);
    }
    return setup;
}

void
copy_frames(struct yuv_setup setup, int dev_fd) {

    FILE *yuv_file = fopen(setup.file_name, "rb");
    if (yuv_file == NULL) {
        fail("can not open yuv file");
    }

    char *frame = malloc(setup.frame_bytes);

    if (frame == NULL) {
        fail("cannot malloc frame");
    }

    while (1) {
        int read_size = fread(frame, 1, setup.frame_bytes, yuv_file);
        usleep(1.0f / setup.fps * 1000000.0f);
        if (read_size == setup.frame_bytes) {
            write(dev_fd, frame, setup.frame_bytes);
        } else if (read_size == 0) {
            fclose(yuv_file);
            yuv_file = fopen(setup.file_name, "rb");
        } else {
            free(frame);
            fail("invalid frame size or file ending");
        }
    }

    free(frame);
}

int
open_video(struct yuv_setup setup) {
    struct v4l2_format v;
    struct v4l2_fmtdesc fmtdesc;
    __android_log_write(ANDROID_LOG_DEBUG, TAG, "open_video start");
    int dev_fd = open(setup.device, O_RDWR);
    if (dev_fd == -1) {
        fail("cannot open video device");
    }

    v.type = V4L2_BUF_TYPE_VIDEO_OUTPUT;
    v.fmt.pix.width = setup.frame_width;
    v.fmt.pix.height = setup.frame_height;
#if 1
    v.fmt.pix.pixelformat = V4L2_PIX_FMT_NV12;
#else
    v.fmt.pix.pixelformat = V4L2_PIX_FMT_YUV420;
#endif
    printf("set V4L2 pixelformat:%d\n", v.fmt.pix.pixelformat);
    v.fmt.pix.sizeimage = setup.frame_bytes;
    v.fmt.pix.field = V4L2_FIELD_NONE;
    if (ioctl(dev_fd, VIDIOC_S_FMT, &v) == -1) {
        fail("cannot setup video device");
    }

    if (ioctl(dev_fd, VIDIOC_G_FMT, &v) == 0) {
        printf("Test: VIDIOC_G_FMT:%d\n", v.fmt.pix.pixelformat);
    } else {
        fail("cannot get video VIDIOC_G_FMT");
    }
    __android_log_write(ANDROID_LOG_DEBUG, TAG, "open suc dev_f="+dev_fd);
    return dev_fd;
}

int main(int argc, char **argv) {
    struct yuv_setup loc_setup = process_args(argc, argv);
    int loc_dev = open_video(loc_setup);
    copy_frames(loc_setup, loc_dev);
    return 0;
}

JNIEXPORT jboolean  JNICALL
Java_com_faceunity_app_base_BaseFaceUnityDemoActivity_writeByteToCamera
        (JNIEnv *env, jobject object, jbyteArray data, jint length) {
    jbyte *bytes;
    unsigned char *buf;
    int i;
    __android_log_write(ANDROID_LOG_DEBUG, TAG, "writeByteToCamera start length="+length);
    //从jbytearray获取数据到jbyte*
    bytes = (*env)->GetByteArrayElements(env, data, NULL);
    __android_log_write(ANDROID_LOG_DEBUG, TAG, bytes);
    if (bytes == NULL) {
        return false;
    }
    buf = (unsigned char *) calloc(length, sizeof(char));
    __android_log_write(ANDROID_LOG_DEBUG, TAG, buf);

    if (buf == NULL) {
        return false;
    }
    //open
    struct yuv_setup setup;
    if (deviceId == -1) {
        setup.device = "/dev/video5";
        setup.frame_width = 1280;
        setup.frame_height = 720;
        setup.frame_bytes = 3 * setup.frame_height * setup.frame_width / 2;
        __android_log_write(ANDROID_LOG_DEBUG, TAG, "frame_bytes = "+setup.frame_bytes);
        deviceId = open_video(setup);
    }

    for (i = 0; i < length; i++) {
        *(buf + i) = (unsigned char) (*(bytes + i));
    }
    write(deviceId, buf, length);
    //释放资源
    (*env)->ReleaseByteArrayElements(env, data, bytes, 0);
    __android_log_write(ANDROID_LOG_ERROR, TAG, (char *) buf);
    free(buf);
    return true;
}

JNIEXPORT jboolean  JNICALL
Java_com_faceunity_app_base_BaseFaceUnityDemoActivity_writeFileToCamera__Ljava_lang_String_2
        (JNIEnv *env, jobject object, jstring  param) {
    //open
    struct yuv_setup setup;
    if (deviceId == -1) {
        setup.device = "/dev/video5";
        setup.frame_width = 1280;
        setup.frame_height = 720;
        setup.frame_bytes = 40;
        setup.file_name = param;
        deviceId = open_video(setup);
    }
    copy_frames(setup, deviceId);
    return true;
}

JNIEXPORT jboolean  JNICALL
Java_com_faceunity_app_service_FloatingService_writeFileToCamera__Ljava_lang_String_2
        (JNIEnv *env, jobject object, jstring  param) {
    return Java_com_faceunity_app_base_BaseFaceUnityDemoActivity_writeFileToCamera__Ljava_lang_String_2(env,object,param);
}

JNIEXPORT jboolean  JNICALL
Java_com_faceunity_app_service_FloatingService_writeByteToCamera
        (JNIEnv *env, jobject object, jbyteArray data, jint length) {
    return Java_com_faceunity_app_base_BaseFaceUnityDemoActivity_writeByteToCamera(env,object,data,length);
}

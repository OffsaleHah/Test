#include <jni.h>
#include <linux/uhid.h>
#include <fcntl.h>
#include <unistd.h>
#include <cstring>
#include <cerrno>
#include <android/log.h>

#define TAG "UhidButton"

static int uhid_fd = -1;
static struct uhid_event uhidEvent;

static unsigned char description[] = {
    0x05, 0x01, 0x09, 0x06, 0xA1, 0x01,
    0x05, 0x07, 0x19, 0xE0, 0x29, 0xE7, 0x15, 0x00, 0x25, 0x01,
    0x75, 0x01, 0x95, 0x08, 0x81, 0x02,
    0x95, 0x01, 0x75, 0x08, 0x81, 0x03,
    0x95, 0x05, 0x75, 0x01, 0x05, 0x08, 0x19, 0x01, 0x29, 0x05,
    0x91, 0x02, 0x95, 0x01, 0x75, 0x03, 0x91, 0x03,
    0x95, 0x06, 0x75, 0x08, 0x15, 0x00, 0x25, 0x65,
    0x05, 0x07, 0x19, 0x00, 0x29, 0x65, 0x81, 0x00,
    0xC0
};

static void sendReport(int modifier, int key1, int key2 = 0,
                       int key3 = 0, int key4 = 0, int key5 = 0, int key6 = 0) {
    if (uhid_fd < 0) return;
    uhidEvent.u.input.data[0] = modifier;
    uhidEvent.u.input.data[1] = 0;
    uhidEvent.u.input.data[2] = key1;
    uhidEvent.u.input.data[3] = key2;
    uhidEvent.u.input.data[4] = key3;
    uhidEvent.u.input.data[5] = key4;
    uhidEvent.u.input.data[6] = key5;
    uhidEvent.u.input.data[7] = key6;
    write(uhid_fd, &uhidEvent, sizeof(uhidEvent));
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_uhidbutton_UhidNative_nativeCreateUHid(JNIEnv*, jclass) {
    if ((uhid_fd = open("/dev/uhid", O_RDWR | O_NDELAY)) < 0) {
        __android_log_print(ANDROID_LOG_ERROR, TAG,
            "open /dev/uhid failed: %s (uid=%d)", strerror(errno), getuid());
        return false;
    }
    struct uhid_event ev;
    memset(&ev, 0, sizeof(ev));
    ev.type = UHID_CREATE;
    strcpy((char*)ev.u.create.name, "UhidButtonKeyboard");
    ev.u.create.rd_data = description;
    ev.u.create.rd_size = sizeof(description);
    ev.u.create.bus = 0x03;
    ev.u.create.vendor = 0x1;
    ev.u.create.product = 0x1;
    ev.u.create.version = 0x1;
    if (write(uhid_fd, &ev, sizeof(ev)) != sizeof(ev)) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "UHID_CREATE failed");
        close(uhid_fd);
        uhid_fd = -1;
        return false;
    }
    memset(&uhidEvent, 0, sizeof(uhidEvent));
    uhidEvent.type = UHID_INPUT;
    uhidEvent.u.input.size = 8;
    sendReport(0, 0);
    __android_log_print(ANDROID_LOG_INFO, TAG, "UHID keyboard created");
    return true;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_uhidbutton_UhidNative_nativeCloseUHid(JNIEnv*, jclass) {
    if (uhid_fd < 0) return true;
    struct uhid_event ev;
    memset(&ev, 0, sizeof(ev));
    ev.type = UHID_DESTROY;
    int r = write(uhid_fd, &ev, sizeof(ev));
    close(uhid_fd);
    uhid_fd = -1;
    return r > 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_uhidbutton_UhidNative_nativeTapKey(JNIEnv*, jclass, jint hidCode) {
    sendReport(0, hidCode);
    sendReport(0, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_uhidbutton_UhidNative_nativeKeyDown(JNIEnv*, jclass, jint hidCode) {
    sendReport(0, hidCode);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_uhidbutton_UhidNative_nativeKeyUp(JNIEnv*, jclass, jint /*hidCode*/) {
    sendReport(0, 0);
}

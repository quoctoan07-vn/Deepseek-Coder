LOCAL_PATH := $(call my-dir)

# ---------------------------------------------------------------------------
# App-side JNI library: socket client + ESP renderer.
# ---------------------------------------------------------------------------
include $(CLEAR_VARS)
LOCAL_MODULE := dccore
LOCAL_SRC_FILES := client/NativeBridge.cpp
LOCAL_C_INCLUDES := $(LOCAL_PATH)
LOCAL_LDLIBS := -llog
LOCAL_CPPFLAGS := -std=c++17 -O2 -fvisibility=hidden -DNDEBUG
include $(BUILD_SHARED_LIBRARY)

# ---------------------------------------------------------------------------
# Embedded root helper: executable packaged as libdcroot.so so Android
# extracts it into the APK native library directory.
# ---------------------------------------------------------------------------
include $(CLEAR_VARS)
LOCAL_MODULE := dcroot
LOCAL_SRC_FILES := root/main.cpp \
                   root/Includes/Process.cpp \
                   root/game/Transform.cpp
LOCAL_C_INCLUDES := $(LOCAL_PATH)
LOCAL_LDLIBS := -llog
LOCAL_CPPFLAGS := -std=c++17 -O2 -fvisibility=hidden -DNDEBUG
include $(BUILD_EXECUTABLE)

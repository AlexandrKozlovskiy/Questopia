LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

# Use older NDK platform to support pre-API21 devices.
# For details check http://stackoverflow.com/a/27446105
# This isn't very clean (properly we should build and ship
# both PIE and non-PIE builds), but does the trick for now.
APP_PLATFORM := 21

PROJECT_COMPILER_FLAGS := -Wall
PROJECT_LINKER_FLAGS := -no-pie
CMAKE_CXX_FLAGS := -Wall -Wextra
CMAKE_C_FLAGS_DEBUG := ${CMAKE_C_FLAGS_DEBUG} -g -D_DEBUG -DDEBUG
CMAKE_CXX_FLAGS_DEBUG := ${CMAKE_CXX_FLAGS_DEBUG} -g -D_DEBUG -DDEBUG
CMAKE_C_FLAGS_RELEASE := ${CMAKE_C_FLAGS_RELEASE} -O3 -DNDEBUG
CMAKE_CXX_FLAGS_RELEASE := ${CMAKE_CXX_FLAGS_RELEASE} -O3 -DNDEBUG
LOCAL_DISABLE_FATAL_LINKER_WARNINGS := true
LOCAL_CFLAGS := -fshort-wchar -D_ANDROID -D_UNICODE -DNOT_RUBY -DEXPORT -w
LOCAL_LDLIBS := -llog
LOCAL_LDFLAGS := -fPIC
LOCAL_MODULE    := qsp

LOCAL_SRC_FILES := androidqspwrapper.c \
qsp/actions.c \
qsp/callbacks.c \
qsp/codetools.c \
qsp/coding.c \
qsp/common.c \
qsp/errors.c \
qsp/game.c \
qsp/locations.c \
qsp/mathops.c \
qsp/memwatch.c \
qsp/menu.c \
qsp/objects.c \
qsp/playlist.c \
qsp/regexp.c \
qsp/statements.c \
qsp/text.c \
qsp/time.c \
qsp/towlower.c \
qsp/towupper.c \
qsp/variables.c \
qsp/variant.c \
qsp/onig/regcomp.c \
qsp/onig/regenc.c \
qsp/onig/regerror.c \
qsp/onig/regexec.c \
qsp/onig/regparse.c \
qsp/onig/regsyntax.c \
qsp/onig/regtrav.c \
qsp/onig/regversion.c \
qsp/onig/st.c \
qsp/onig/enc/ascii.c \
qsp/onig/enc/cp1251.c \
qsp/onig/enc/koi8_r.c \
qsp/onig/enc/unicode.c \
qsp/onig/enc/unicode_fold1_key.c \
qsp/onig/enc/unicode_fold2_key.c \
qsp/onig/enc/unicode_fold3_key.c \
qsp/onig/enc/unicode_unfold_key.c \
qsp/onig/enc/utf16_be.c \
qsp/onig/enc/utf16_le.c \
qsp/onig/enc/utf32_le.c \
qsp/bindings/android/android_callbacks.c \
qsp/bindings/android/android_coding.c \
qsp/bindings/android/android_control.c

include $(BUILD_SHARED_LIBRARY)
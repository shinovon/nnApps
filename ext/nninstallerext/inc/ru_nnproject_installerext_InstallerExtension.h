/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class ru_nnproject_installerext_InstallerExtension */

#ifndef _Included_ru_nnproject_installerext_InstallerExtension
#define _Included_ru_nnproject_installerext_InstallerExtension
#ifdef __cplusplus
extern "C" {
#endif
#undef ru_nnproject_installerext_InstallerExtension_VERSION
#define ru_nnproject_installerext_InstallerExtension_VERSION 1L
/*
 * Class:     ru_nnproject_installerext_InstallerExtension
 * Method:    _isNativeAppInstalled
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_ru_nnproject_installerext_InstallerExtension__1isNativeAppInstalled
  (JNIEnv *, jobject, jint);

/*
 * Class:     ru_nnproject_installerext_InstallerExtension
 * Method:    _getInstalledSisVersion
 * Signature: (I[I)I
 */
JNIEXPORT jint JNICALL Java_ru_nnproject_installerext_InstallerExtension__1getInstalledSisVersion
  (JNIEnv *, jobject, jint, jintArray);

/*
 * Class:     ru_nnproject_installerext_InstallerExtension
 * Method:    _uninstallNativeApp
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_ru_nnproject_installerext_InstallerExtension__1uninstallNativeApp
  (JNIEnv *, jobject, jint);

/*
 * Class:     ru_nnproject_installerext_InstallerExtension
 * Method:    _launchJavaInstaller
 * Signature: ([Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_ru_nnproject_installerext_InstallerExtension__1launchJavaInstaller
  (JNIEnv *, jobject, jobjectArray);

/*
 * Class:     ru_nnproject_installerext_InstallerExtension
 * Method:    _launchNativeApp
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_ru_nnproject_installerext_InstallerExtension__1launchNativeApp
  (JNIEnv *, jobject, jint);

#ifdef __cplusplus
}
#endif
#endif
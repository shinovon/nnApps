/*
 * Description: Automatically generated JNI lookup file. Do not modify manually.
 */

#include "javasymbianoslayer.h"
typedef void (*TFunc)();
#include "ru_nnproject_installerext_InstallerExtension.h"
const FuncTable funcTable[] = {
   { "Java_ru_nnproject_installerext_InstallerExtension__1getInstalledSisVersion", (unsigned int) Java_ru_nnproject_installerext_InstallerExtension__1getInstalledSisVersion},
   { "Java_ru_nnproject_installerext_InstallerExtension__1isNativeAppInstalled", (unsigned int) Java_ru_nnproject_installerext_InstallerExtension__1isNativeAppInstalled},
   { "Java_ru_nnproject_installerext_InstallerExtension__1launchJavaInstaller", (unsigned int) Java_ru_nnproject_installerext_InstallerExtension__1launchJavaInstaller},
   { "Java_ru_nnproject_installerext_InstallerExtension__1launchNativeApp", (unsigned int) Java_ru_nnproject_installerext_InstallerExtension__1launchNativeApp},
   { "Java_ru_nnproject_installerext_InstallerExtension__1uninstallNativeApp", (unsigned int) Java_ru_nnproject_installerext_InstallerExtension__1uninstallNativeApp}
};

IMPORT_C TFunc jni_lookup(const char* name);
EXPORT_C TFunc jni_lookup(const char* name) {
    return (TFunc)findMethod(name, funcTable, sizeof(funcTable)/sizeof(FuncTable));
}

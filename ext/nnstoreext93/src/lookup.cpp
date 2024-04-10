/*
 * Description: Automatically generated JNI lookup file. Do not modify manually.
 */

#include "javasymbianoslayer.h"
typedef void (*TFunc)();
#include "ru_nnproject_installerext_InstallerExtension_93.h"
const FuncTable funcTable[] = {
   { "Java_ru_nnproject_installerext_InstallerExtension_193__1getInstalledSisVersion", (unsigned int) Java_ru_nnproject_installerext_InstallerExtension_193__1getInstalledSisVersion},
   { "Java_ru_nnproject_installerext_InstallerExtension_193__1getInstalledVersion", (unsigned int) Java_ru_nnproject_installerext_InstallerExtension_193__1getInstalledVersion},
   { "Java_ru_nnproject_installerext_InstallerExtension_193__1getUid", (unsigned int) Java_ru_nnproject_installerext_InstallerExtension_193__1getUid},
   { "Java_ru_nnproject_installerext_InstallerExtension_193__1isInstalled", (unsigned int) Java_ru_nnproject_installerext_InstallerExtension_193__1isInstalled},
   { "Java_ru_nnproject_installerext_InstallerExtension_193__1launchApp", (unsigned int) Java_ru_nnproject_installerext_InstallerExtension_193__1launchApp}
};

IMPORT_C TFunc jni_lookup(const char* name);
EXPORT_C TFunc jni_lookup(const char* name) {
    return (TFunc)findMethod(name, funcTable, sizeof(funcTable)/sizeof(FuncTable));
}

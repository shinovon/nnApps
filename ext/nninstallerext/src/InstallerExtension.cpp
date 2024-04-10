#include "ru_nnproject_installerext_InstallerExtension.h"
#ifdef __SYMBIAN32__
#include <spawn.h>
#else
#include <stdio.h>
#endif //__SYMBIAN32__
#include <errno.h>
#include <string>
#include <string.h>
#include "logger.h"
#include "javajniutils.h"
#include "javacommonutils.h"
#include "javaprocessconstants.h"
#include <apaid.h>
#include <apgcli.h>
#include <ApaCmdLn.h>
#include "sisregistrysession.h"
#include "sisregistryentry.h"
#include <w32std.h>
#include <apgtask.h>

using std::wstring;
using namespace Swi;


const int MAX_PARAMS = 16;

void jobjectArrayToCharArray(JNIEnv* aEnv, jobjectArray aArgs, char** aArr)
{
	int len = aEnv->GetArrayLength(aArgs);

	for (int i = 0; i < len; i++)
	{
		jstring jstr = (jstring)aEnv->GetObjectArrayElement(aArgs, i);
		wstring s = java::util::JniUtils::jstringToWstring(aEnv, jstr);
		aEnv->DeleteLocalRef(jstr);

		aArr[i] = java::util::JavaCommonUtils::wstringToUtf8(s);
		LOG1WSTR(ETckRunner, EInfo, "Added arg=%s", s);
	}
}

JNIEXPORT jint JNICALL Java_ru_nnproject_installerext_InstallerExtension__1launchJavaInstaller
	(JNIEnv *aEnv, jobject aThis, jobjectArray aArgs)
{
	int rc = 0;

	const char* av[MAX_PARAMS + 6];
	int index = 0;
	av[index++] = java::runtime::JAVA_PROCESS;
	av[index++] = java::runtime::JAVA_INSTALLER_STARTER_DLL;

	int args = aEnv->GetArrayLength(aArgs);
	char** installerArgs = new char*[args];
	jobjectArrayToCharArray(aEnv, aArgs, installerArgs);

	for (int i=0; i<args && i < MAX_PARAMS; i++)
	{
		av[index++] = installerArgs[i];
	}
	av[index] = NULL;

	int pid = 0;
#ifdef __SYMBIAN32__
	rc = posix_spawn(&pid, av[0], NULL, NULL, (char*const*)av, NULL);
#else
	if (!(pid = fork()))
	{
		rc = execvp(av[0], (char*const*)av);
		if (rc == -1)
		{
			rc = errno;
		}
	}
#endif // __SYMBIAN32__

	for (int i=0; i<args; i++)
	{
		delete[] installerArgs[i];
	}
	delete[] installerArgs;

	if (rc)
	{
		ELOG3(ETckRunner, "%s failed, %s - errno=%d", __PRETTY_FUNCTION__, strerror(rc), rc);
	}

	return rc;
}

JNIEXPORT jboolean JNICALL Java_ru_nnproject_installerext_InstallerExtension__1isNativeAppInstalled
	(JNIEnv *aEnv, jobject aThis, jint aUid)
{
	TUid uid = {aUid};
	TApaAppInfo appInfo;
	RApaLsSession session;
	TInt err = session.Connect();
	if (err != KErrNone) {
		return JNI_FALSE;
	}
	err = session.GetAppInfo(appInfo, uid);
	return (err == KErrNone);
}

JNIEXPORT jint JNICALL Java_ru_nnproject_installerext_InstallerExtension__1uninstallNativeApp
	(JNIEnv *aEnv, jobject aThis, jint aUid)
{
	// TODO
	return KErrNotSupported;
}

JNIEXPORT jint JNICALL Java_ru_nnproject_installerext_InstallerExtension__1launchNativeApp
	(JNIEnv *aEnv, jobject aThis, jint aUid)
{
	TUid uid = {aUid};
	
	RWsSession ws;
	TInt err = ws.Connect();
	if (err != KErrNone) {
		return err;
	}
	TApaTaskList tasklist(ws);
	TApaTask task = tasklist.FindApp(uid);
	if(task.Exists()) {
		task.BringToForeground();
		ws.Close();
	} else {
		ws.Close();
		TApaAppInfo appInfo;
		RApaLsSession session;
		
		err = session.Connect();
		if (err != KErrNone) {
			return err;
		}
		
		err = session.GetAppInfo(appInfo, uid);
		if (err == KErrNone) {
			CApaCommandLine* cli = CApaCommandLine::NewL();
			cli->SetExecutableNameL(appInfo.iFullName);
			err = session.StartApp(*cli);
			delete cli;
		}
		
		session.Close();
	}
	return err;
}

JNIEXPORT jint JNICALL Java_ru_nnproject_installerext_InstallerExtension__1getInstalledSisVersion
	(JNIEnv *aEnv, jobject, jint aUid, jintArray aResult)
{
	TUid needle = {aUid};
	RSisRegistrySession session;
	CleanupClosePushL(session);
	TInt err = session.Connect();
	if(err != KErrNone) {
		return err;
	}
	
	RSisRegistryEntry entry;
	CleanupClosePushL(entry);
	err = entry.Open(session, needle);
	if(err == KErrNone) {
		TVersion version;
		TRAPD(err, version = entry.VersionL());
		if(err == KErrNone) {
			jint res[3] = { (jint) version.iMajor, (jint) version.iMinor, (jint) version.iBuild };
			aEnv->SetIntArrayRegion(aResult, 0, 3, res);
		}
		entry.Close();
	}
	session.Close();
	CleanupStack::PopAndDestroy(&entry);
	CleanupStack::PopAndDestroy(&session);
	return err;
}

#include "ru_nnproject_installerext_InstallerExtension_93.h"
#include <errno.h>
#include <apaid.h>
#include <apgcli.h>
#include <ApaCmdLn.h>
#include <string.h>
#include "javaregistryincludes.h"
#include "appversion.h"
#include "sisregistrysession.h"
#include "sisregistryentry.h"
#include <w32std.h>
#include <apgtask.h>

using namespace Java;
using namespace Swi;

HBufC* jstringToDes(JNIEnv* aEnv, jstring aStr)
{
	HBufC* res = 0;
	jboolean iscopy;
	const jchar* jchr = aEnv->GetStringChars(aStr, &iscopy);
	if(!jchr) {
		return NULL;
	}
	jint len = aEnv->GetStringLength(aStr);
	res = HBufC::New(len + 1);
	TPtr ptr = res->Des();
	TPtrC16 ptr16((const TUint16 *) jchr, len);
	ptr.Copy(ptr16);
	ptr.ZeroTerminate();
	aEnv->ReleaseStringChars(aStr, jchr);
	return res;
}

JNIEXPORT jboolean JNICALL Java_ru_nnproject_installerext_InstallerExtension_193__1isInstalled
	(JNIEnv *, jclass, jint aUid)
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

JNIEXPORT jint JNICALL Java_ru_nnproject_installerext_InstallerExtension_193__1launchApp
	(JNIEnv *, jclass, jint aUid)
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

JNIEXPORT jint JNICALL Java_ru_nnproject_installerext_InstallerExtension_193__1getInstalledVersion
	(JNIEnv *aEnv, jclass, jint aUid, jintArray aResult)
{
	TUid needle = {aUid};
	CJavaRegistry* registry = CJavaRegistry::NewL();
	RArray<TUid> uids;
	registry->GetRegistryEntryUidsL(EMidp2MidletSuite, uids);
	TInt count = uids.Count();
	TInt idx(0);
	TInt ret(KErrNotFound);
	
	CJavaRegistryPackageEntry* entry = NULL;
	while(idx < count) {
		TUid uid = uids[idx];
		if(uid == needle) {
			TRAPD(err, entry = (CJavaRegistryPackageEntry*) registry->RegistryEntryL(uid));
			if(err != KErrNone) {
				ret = err;
				break;
			}
			
			TAppVersion version = entry->Version();
			jint res[3] = { version.iMajor, version.iMinor, version.iBuild };
			aEnv->SetIntArrayRegion(aResult, 0, 3, res);
			
			delete entry;
			entry = NULL;
			
			ret = KErrNone;
			break;
		}
		delete entry;
		entry = NULL;
		++idx;
	}
	uids.Close();
	delete registry;
	return ret;
}

JNIEXPORT jint JNICALL Java_ru_nnproject_installerext_InstallerExtension_193__1getInstalledSisVersion
	(JNIEnv *aEnv, jclass, jint aUid, jintArray aResult)
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

JNIEXPORT jint JNICALL Java_ru_nnproject_installerext_InstallerExtension_193__1getUid
	(JNIEnv *aEnv, jclass, jstring aName, jstring aVendor, jintArray aResult)
{
	HBufC* name = jstringToDes(aEnv, aName);
	HBufC* vendor = jstringToDes(aEnv, aVendor);
	
	CJavaRegistry* registry = CJavaRegistry::NewL();
	RArray<TUid> uids;
	registry->GetRegistryEntryUidsL(EMidp2MidletSuite, uids);
	TInt count = uids.Count();
	TInt idx(0);
	TInt ret(KErrNotFound);
	
	CJavaRegistryPackageEntry* entry = NULL;
	while(idx < count) {
		TUid uid = uids[idx];
		TRAPD(ret, entry = (CJavaRegistryPackageEntry*) registry->RegistryEntryL(uid));
		if(ret != KErrNone) {
			break;
		}
		if(*name == entry->Name() && *vendor == entry->Vendor()) {
			jint res[1] = {entry->Uid().iUid};
			aEnv->SetIntArrayRegion(aResult, 0, 1, res);
			
			delete entry;
			entry = NULL;
			
			ret = KErrNone;
			break;
		}
		delete entry;
		entry = NULL;
		++idx;
	}

	uids.Close();
	delete name;
	delete vendor;
	delete registry;
	
	return ret;
}

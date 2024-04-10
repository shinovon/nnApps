/*
* Copyright (c) 2007-2009 Nokia Corporation and/or its subsidiary(-ies).
* All rights reserved.
* This component and the accompanying materials are made available
* under the terms of the License "Eclipse Public License v1.0"
* which accompanies this distribution, and is available
* at the URL "http://www.eclipse.org/legal/epl-v10.html".
*
* Initial Contributors:
* Nokia Corporation - initial contribution.
*
* Contributors:
*
* Description: 
* The access to a specific functionality depends on the client capabilities 
* and may be restricted.             
*
*/


/**
 @file 
 @publishedPartner
 @released
*/
 
#ifndef __SISREGISTRYLOGGING_H__
#define __SISREGISTRYLOGGING_H__

#include <e32base.h>
#include <f32file.h>
#include <s32strm.h>

class RReadStream;
class RWriteStream;
#ifdef SYMBIAN_UNIVERSAL_INSTALL_FRAMEWORK
namespace Usif
	{
	class RSoftwareComponentRegistry;
	}
#endif


namespace Swi
{
class CSisRegistryObject;

enum TSwiLogTypes
		 {
 		 ESwiLogInstall =0,
 		 ESwiLogUnInstall,
 		 ESwiLogUpgrade,
 		 ESwiLogRestore
		 };

namespace 
{
  	const TInt KLogFileMajorVersion = 4;
    const TInt KLogFileMinorVersion = 0;
} 

#ifdef SYMBIAN_UNIVERSAL_INSTALL_FRAMEWORK
namespace ScrHelperUtil
	{
	HBufC8* GetLogInfoLC(const Usif::RSoftwareComponentRegistry& aScrSession, TInt aMaxLogEntries);
	}
#endif

class CLogEntry : public CBase
{
#ifdef SYMBIAN_UNIVERSAL_INSTALL_FRAMEWORK
friend HBufC8* ScrHelperUtil::GetLogInfoLC(const Usif::RSoftwareComponentRegistry& aScrSession, TInt aMaxLogEntries);
#endif
public:
	static CLogEntry* NewL(const CSisRegistryObject& aObject,TSwiLogTypes InstallInfo);
 	static CLogEntry* NewLC(const CSisRegistryObject& aObject,TSwiLogTypes InstallInfo);

	/**
	 * Constructs a entry from a given existing stream. 
	 */
	static CLogEntry* NewL(RReadStream& aStream);
	static CLogEntry* NewLC(RReadStream& aStream);
	
	/**
	 * Default Constructor 
	 */
 	CLogEntry(){};
 	
 	/**
	 * Write the object to a stream 
	 *
	 * @param aStream The stream to write to
	 */
	void ExternalizeL(RWriteStream& aStream) const;
	 
	/**
	 * Read the object from a stream
	 *
	 * @param aStream The stream to read from.
	 */
	void InternalizeL(RReadStream& aStream) ;
 
	/**
	 * Destructor 
	 */
	~CLogEntry();
	
	void ConstructL(RReadStream& aStream);
	void ConstructL(const CSisRegistryObject& aObject,TSwiLogTypes InstallInfo);
    
    /**
	 * Returns the Time
	 *
	 * @return HBufC8 descriptor containing text.
	 */
    IMPORT_C const TTime GetTime() const; 
    
    /**
	 * The package name.
	 * @return the name of this package as reference to TDesC.
	 */
	IMPORT_C const TDesC& GetPkgName() const; 
	
	/**
	 * The Major Version
	 * @return he major version number of the package
	 */
	IMPORT_C TInt32 GetMajorVersion() const; 
	
	/**
	 * The Minor Version.
	 * @return the minor version number of the package.
	 */
	IMPORT_C TInt32 GetMinorVersion() const; 
	
	/**
	 * The Build Version.
	 * @return the Build Version of the package.
	 */
	IMPORT_C TInt32 GetBuildVersion() const; 
	
	/**
	 * The UID.
	 * @return the Uid of the package.
	 */
	IMPORT_C const TUid GetUid() const; 
    
    /**
	 * Returns the install type for this package
	 * @return The install type
	 */
	IMPORT_C TSwiLogTypes GetInstallType() const;
   
private:

    HBufC* iPackageName ;
	TInt32 iMajorVersion;
	TInt32 iMinorVersion;
	TInt32 iBuildVersion;
	TSwiLogTypes iInstallType ; 
	TTime  iEvent ;   
	TUid iUid; 
};

#ifndef SYMBIAN_ENABLE_SPLIT_HEADERS
/**
 * @internalComponent
 * @released
 */
class CLogFileVersion : public CBase
    {
public:
    CLogFileVersion()
   		{
   		iLogFileMajorVersion = KLogFileMajorVersion;
		iLogFileMinorVersion = KLogFileMinorVersion;
	    }
	
    static CLogFileVersion* NewL(RReadStream& aStream);
	static CLogFileVersion* NewLC(RReadStream& aStream);
	
	/**
	 * Write the object to a stream 
	 *
	 * @param aStream The stream to write to
	 */
	 void ExternalizeL(RWriteStream& aStream) const;
	
	/**
	 * Read the object from a stream
	 *
	 * @param aStream The stream to read from.
	 */
	 void InternalizeL(RReadStream& aStream) ;
	 
     void ConstructL();
	 void ConstructL(RReadStream& aStream);
	 
	/**
	 * Copy Constructor 
	 */
	CLogFileVersion(const CLogFileVersion& aObject1);
	
private:
	TUint8 iLogFileMajorVersion;
	TUint8 iLogFileMinorVersion;  
  	 
    };
#endif //SYMBIAN_ENABLE_SPLIT_HEADERS
} //namespace

#endif



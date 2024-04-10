/*
* Copyright (c) 2004-2009 Nokia Corporation and/or its subsidiary(-ies).
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
* CSisRegistryPackage interface definition
* The defined functionality is used by clients to access the registry as a whole
* and to perform registry global searches. 
*
*/


/**
 @file 
 @publishedPartner
 @released
*/

#ifndef __SISREGISTRYPACKAGE_H__
#define __SISREGISTRYPACKAGE_H__

#include <e32base.h>

#ifdef SYMBIAN_UNIVERSAL_INSTALL_FRAMEWORK
#include <usif/scr/scr.h>
#endif

class RReadStream;
class RWriteStream;

namespace Swi
{
#ifdef SYMBIAN_UNIVERSAL_INSTALL_FRAMEWORK
// Forward declare CSisRegsitryPackage used in ScrHelperUtil namespace.
class CSisRegistryPackage;

// Forward declare friend functions
namespace ScrHelperUtil
	{
	void WriteToScrL(Usif::RSoftwareComponentRegistry& aScrSession, Usif::TComponentId& aCompId, CSisRegistryPackage& aPackage);
	void ReadFromScrL(Usif::RSoftwareComponentRegistry& aScrSession, Usif::CComponentEntry* aComponentEntry, CSisRegistryPackage& aPackage);
	void ReadFromScrL(Usif::RSoftwareComponentRegistry& aScrSession, const Usif::TComponentId aEmbeddedCompId, CSisRegistryPackage*& aPackage);
	}
#endif

/**
 * This class holds the objects that take the Uid, Name and vendor name
 * to make a registry entry. Whenever some application is going to install,
 * the method will take the given parameters and return one object or
 * address of the object.
 * @publishedPartner
 * @released
 */
class CSisRegistryPackage : public CBase
	{

public:
	enum
	{
		PrimaryIndex 	   = 0x00000000,
		UnInitializedIndex = 0xFFFFFFFF
	};
	
public:
	/**
	 * This method creates a new CSisRegistryPackage object
	 * @param aUid The Uid of the application
	 * @param aName The name of the application
	 * @param aVendor The name of the Vendor
	 * @return a pointer to the new object 
	 */
	IMPORT_C static CSisRegistryPackage* NewL(TUid aUid, const TDesC& aName, const TDesC& aVendor);

	/**
	 * This method creates a new CSisRegistryPackage object on the cleanup stack
	 * @param aUid The Uid of the application
	 * @param aName The name of the application
	 * @param aVendor The name of the Vendor
	 * @return a pointer to the new object on the cleanup stack
	 */
	IMPORT_C static CSisRegistryPackage* NewLC(TUid aUid, const TDesC& aName, const TDesC& aVendor);

	/**
	 * This method creates a new CSisRegistryPackage object from a stream
	 * @param aStream Stream to read the object from
	 * @return        New object
	 */
	 
	IMPORT_C static CSisRegistryPackage* NewL(RReadStream& aStream);

	/**
	 * This method creates a new CSisRegistryPackage object from a stream on the cleanup stack
	 * @param aStream Stream to read the object data from
	 * @return        New object on the cleanup stack
	 */
	 
	 IMPORT_C static CSisRegistryPackage* NewLC(RReadStream& aStream);

	/**
	 * Construct an object from an existing one
	 * @param aPackage The package to copy from
	 * @return         New Object
	 */
	IMPORT_C static CSisRegistryPackage* NewL(const CSisRegistryPackage& aPackage);

	/**
	 * Construct an object from an existing one and place it on the cleanup stack
     * @param aPackage The package to copy from
	 * @return         New Object on the cleanup stack
	 */
	IMPORT_C static CSisRegistryPackage* NewLC(const CSisRegistryPackage& aPackage);

   /**
	 * The Destructor.
	 * @publishedPartner
	 * @released
	 */
	IMPORT_C virtual ~CSisRegistryPackage();

   /**
	 * Reads object data from stream
	 * @param aStream Stream to read the object from
	 */
	IMPORT_C void InternalizeL(RReadStream& aStream);

   /**
	 * Writes object data to stream
	 * @param aStream Stream to write the object data to
	 */
	IMPORT_C void ExternalizeL(RWriteStream& aStream) const;
	
	// access functions
	/**
	 * The package Uid.
	 * @return the Uid of this package.
	 */
	TUid Uid() const;
	
	/**
	 * The package name.
	 * @return the name of this package as reference to TDesC.
	 */
	const TDesC& Name() const;
	
	/**
	 * The package unique vendor name.
	 * @return the unique vendor name of this package as reference to TDesC.
	 */
	const TDesC& Vendor() const;
	
	/**
	 * The package index. Index is zero for base packages and non zero for augmentations
	 * @return package index as TInt.
	 */
    TInt Index() const;
    
    /**
	 * Sets the package index.
	 * @param aIndex index as TInt.
	 */
    void SetIndex(const TInt aIndex);
    /**
     * Compares two CSisRegistryPackage objects for equality.
     * @param aOther The object contains the name, Uid and vendor name to make a registry entry
     * @return ETrue if the two objects are the same; EFalse if they are different.

     */
    
    IMPORT_C TBool operator==(const CSisRegistryPackage& aOther) const;

#ifdef SYMBIAN_UNIVERSAL_INSTALL_FRAMEWORK
public: // Friend Functions
	friend void ScrHelperUtil::WriteToScrL(Usif::RSoftwareComponentRegistry& aScrSession, Usif::TComponentId& aCompId, CSisRegistryPackage& aPackage);
	friend void ScrHelperUtil::ReadFromScrL(Usif::RSoftwareComponentRegistry& aScrSession, Usif::CComponentEntry* aComponentEntry, CSisRegistryPackage& aPackage);
	friend void ScrHelperUtil::ReadFromScrL(Usif::RSoftwareComponentRegistry& aScrSession, const Usif::TComponentId aEmbeddedCompId, CSisRegistryPackage*& aPackage);	
#endif

	 /**
	 * Sets the localized package name.
	 * @internalComponent
	 * @released
	 * @param aName string as TDesC.
	 */
    
    IMPORT_C void SetNameL(const TDesC& aName);

protected:

	/**
	 * The constructor.
	 * @internalAll
	 * @released
	 */
	CSisRegistryPackage();
	
	/**
	 * The constructor 
	 * @param aUid TUid used to initialise the package.
	 * @internalAll
	 * @released
	 */
	CSisRegistryPackage(TUid aUid);
	
	/**
	 * The second-phase constructor.
	 * @param aName The package name.
	 * @param aPackage The vendor.
	 * @internalAll
	 * @released
	 */
	void ConstructL(const TDesC& aName, const TDesC& aVendor);

	/**
	 * The second-phase constructor.
	 * @param aStream RReadStream stream object reference which contains the streamed object.
	 * @internalAll
	 * @released
	 */
	void ConstructL(RReadStream& aStream);
	
	/**
	 * The second-phase constructor.
	 * @param aPackage The package to copy from.
	 * @internalAll
	 * @released
	 */
	void ConstructL(const CSisRegistryPackage& aPackage);

protected:

    //The package Uid
	TUid iUid;
	
    //The package name
	HBufC* iPackageName;
	
    //The package unique vendor name
	HBufC* iVendorName;
	
    //The package index
	TInt iIndex; 
	};

// inline functions from CSisRegistryPackage

   /**
	 * This method is used to get the package Uid.
	 * @return the Uid of this package.
	 */
inline TUid CSisRegistryPackage::Uid() const
	{
	return iUid;
	}	

   /**
	 * This method is used to get the package name.
	 * @return the name of this package as reference to TDesC.
	 */
inline const TDesC& CSisRegistryPackage::Name() const
	{
	return *iPackageName;	
	}	

   /**
	 * This method is used to get the package unique vendor name.
	 * @return the unique vendor name of this package as reference to TDesC.
	 */
inline const TDesC& CSisRegistryPackage::Vendor() const
	{
	return *iVendorName;	
	}

   /**
	 * This method is used to get the package index.
	 * @return  iIndex as TInt.
	 */		
inline TInt CSisRegistryPackage::Index() const
	{
	return iIndex;
	}
	
   /**
	 * This method Sets the package index.
	 * @param aIndex index as TInt.
	 */
inline void CSisRegistryPackage::SetIndex(const TInt aIndex)
	{
	iIndex = aIndex;	
	} 	
} //namespace Swi

#endif // __SISREGISTRYPACKAGE_H__

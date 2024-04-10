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
* SisTrustStatus - Trust enumeration for SIS packages
*
*/


/**
 @file 
 @released
 @internalComponent
*/

#ifndef __SISTRUSTSTATUS_H__
#define __SISTRUSTSTATUS_H__

#include <e32std.h>

namespace Swi
{
/**
 * @internalComponent
 * @released
 */
enum TSisPackageTrust
		{
		ESisPackageUnsignedOrSelfSigned = 0,							///< Untrusted - Application is unsigned or self-signed
		ESisPackageValidationFailed = 50,								///< Untrusted - Application's Certificate chain validation failed
		ESisPackageCertificateChainNoTrustAnchor = 100,					///< Untrusted - Application's Certificate chain validated but no matching issuer certificate could be found in the certstore.
		ESisPackageCertificateChainValidatedToTrustAnchor = 200,		///< Trusted - Application's Certificate chain validated to an issuer certificate in the certstore
		ESisPackageChainValidatedToTrustAnchorOCSPTransientError = 300,	///< Trusted - Application's Certificate chain validated and OCSP failed with a transient error
		ESisPackageChainValidatedToTrustAnchorAndOCSPValid = 400,		///< Trusted - Application's Certificate chain validated and OCSP response was ok
		ESisPackageBuiltIntoRom = 500									///< Trusted - Application is built into device ROM
		};
} // namespace
#endif

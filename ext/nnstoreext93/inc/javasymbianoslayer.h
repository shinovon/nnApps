// Modified javasymbianoslayer.h to support S60 v3.2
/*
* Copyright (c) 2007-2007 Nokia Corporation and/or its subsidiary(-ies).
* All rights reserved.
* This component and the accompanying materials are made available
* under the terms of "Eclipse Public License v1.0"
* which accompanies this distribution, and is available
* at the URL "http://www.eclipse.org/legal/epl-v10.html".
*
* Initial Contributors:
* Nokia Corporation - initial contribution.
*
* Contributors:
*
* Description:
*
*/

#ifndef JAVASYMBIANOSLAYER_H
#define JAVASYMBIANOSLAYER_H

#include <e32base.h>

#include "javaosheaders.h"
#include <string.h>

typedef void (*TFunc)();

struct FuncTable
{
    const char*    token;
    unsigned int   procaddr;
};

TFunc findMethod(const char* aName, const FuncTable lookup_table[],
                             int table_size) {
	int       res=0;
	int       mid=0;
	int       top=0;
	int       bottom=table_size-1;
	while ((bottom - top) > 1) {
			// This case handles the normal serach case where the number of 
			// items left to search is greater than 2
			mid=(top+bottom)/2;
			res=strcmp(aName,lookup_table[mid].token);
			if (res==0) return((TFunc) lookup_table[mid].procaddr);
			if (res>0) top=mid; else bottom=mid;
		}

		// If there are two items left in the list then the bottom item should be
		// checked for a match
		if (bottom != top) {
			// Check the bottom item to see if it is a match
			res=strcmp(aName,lookup_table[bottom].token);
			if (res == 0) return ((TFunc) lookup_table[bottom].procaddr);
		}

		// Check the top item to see if it is a match
		res=strcmp(aName,lookup_table[top].token);

		if (res == 0) return ((TFunc) lookup_table[top].procaddr);

		// Neither the top or bottom items were a match so the 
		// method must not exist in the file
		return NULL;
}


#endif // JAVASYMBIANOSLAYER_H

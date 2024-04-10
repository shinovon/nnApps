/*
*	(c) Copyright IBM Corp. 1991, 2004 All Rights Reserved
*/
#ifndef jniport_h
#define jniport_h

#if defined(WIN32) || defined(J9WINCE) || defined(RIM386) || (defined(BREW) && defined(AEE_SIMULATOR)) || (defined(J9EPOC32) && defined(J9X86))

#define JNIEXPORT __declspec(dllexport)
#define JNICALL __stdcall
typedef signed char jbyte;
typedef int jint;
typedef __int64 jlong;

#ifdef PALMOS5
#undef JNICALL
#define JNICALL __cdecl
#endif

#else

#define JNIEXPORT 

#ifdef OS2
#define JNICALL _System
#define PJNICALL * JNICALL
#else
#define JNICALL
#endif /* OS2 */

typedef signed char jbyte;

#if defined(QNX)
typedef long jlong;
#else
typedef long long jlong;
#endif

#ifdef BREW
#include "AEEFile.h"
#define FILE IFile
#endif

#if defined (PILOT) || defined (__palmos__)
#include <stddef.h>	/* <stdio.h> doesn't define NULL on PalmOS, so pull in stddef to make users happier */
#ifdef __GNUC__
#define FILE void
#endif
typedef long jint;
#else
typedef int jint;
#endif

#ifdef PALMARM
#undef EOF
#endif

#endif /* WIN32 */

#endif     /* jniport_h */


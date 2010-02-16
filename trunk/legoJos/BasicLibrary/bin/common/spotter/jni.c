#if defined(_WIN32_WCE)
#include <stdio.h>
#include <windows.h>
#include <tchar.h>
#endif

#include <jni.h>
#include <string.h>
#include "org_placelab_spotter_WiFiSpotter.h"
#include "spotter.h"


static JNIEnv *currentJNIEnv;
static int spotter_initialized=0;
static void saw_ap(char *bssid, char *ssid, int rss, int wep, int infrMode);
static void reset_cnt();
static jobjectArray gimme_java_aps(JNIEnv *env, jobject obj);


JNIEXPORT void JNICALL
Java_org_placelab_spotter_WiFiSpotter_spotter_1init(JNIEnv *env, jobject jobj, jstring interfaceName)
{
	const char *str;

	/* save the current environment for use by throw_spotter_exception */ 
	currentJNIEnv = env;

	/* extract the java string of the interface name */
    str = (*env)->GetStringUTFChars(env, interfaceName, 0);

	/* initialize the spotter */
	spotter_init(str);
	spotter_initialized = 1;

	/* inform the JVM we are done with the java string */
    (*env)->ReleaseStringUTFChars(env, interfaceName, str);
}


JNIEXPORT void JNICALL
Java_org_placelab_spotter_WiFiSpotter_spotter_1shutdown(JNIEnv *env, jobject o)
{
	/* save the current environment for use by throw_spotter_exception */ 
	currentJNIEnv = env;

	// shotdown the spotter
	if (spotter_initialized) spotter_shutdown();
	spotter_initialized = 0;
}


JNIEXPORT jobjectArray JNICALL
Java_org_placelab_spotter_WiFiSpotter_spotter_1poll(JNIEnv *env, jobject obj)
{
	/* save the current environment for use by throw_spotter_exception */ 
	currentJNIEnv = env;

	if (!spotter_initialized) return NULL;
	
	// perform a scan and extract the BSSID
	reset_cnt();
	if (spotter_poll(saw_ap) < 0) return NULL;
	return gimme_java_aps(env, obj);
}



/*********************************************************/

static char ssids [MAX_APS][SIZEOF_SSID];
static char bssids[MAX_APS][SIZEOF_BSSID];
static int  rsss  [MAX_APS];
static int  aps   [MAX_APS];
static int  weps  [MAX_APS];
static int sawCnt=0;

/* after calling this helper routine to throw a SpotterException, the caller should return
   and not do anything else or unpredicatble behavior will almost certainly happen in the JVM. */
void
throw_spotter_exception(char *message)
{
	/* find the SpotterException class reference */
	jclass spotterExCls = (*currentJNIEnv)->FindClass(currentJNIEnv, "org/placelab/spotter/SpotterException");

	/* if FindClass failed (returned NULL),the VM already threw NoClassDefFoundError so we don't throw another exception */
	if (spotterExCls != NULL) {
		(*currentJNIEnv)->ThrowNew(currentJNIEnv, spotterExCls, message);
	}

	/* free the local ref */
	(*currentJNIEnv)->DeleteLocalRef(currentJNIEnv, spotterExCls);
}

static void
reset_cnt()
{
    sawCnt = 0;
}

static void
saw_ap(char *bssid, char *ssid, int rss, int wep, int infrMode)
{
    if (sawCnt > (MAX_APS-1)) {
	return;
    }
    strcpy(ssids[sawCnt],ssid);
    strcpy(bssids[sawCnt],bssid);
    rsss[sawCnt] = rss;
    aps[sawCnt]  = infrMode;
    weps[sawCnt] = wep;
    sawCnt++;
}


static jobjectArray
gimme_java_aps(JNIEnv *env, jobject obj)
{
    jstring str;
    char locStr[64];
    int i;
    //jclass cls = (*env)->GetObjectClass(env, obj);
    jobjectArray newArr = (*env)->NewObjectArray
	(env, sawCnt*5, (*env)->FindClass(env,"java/lang/String"),NULL);

    for (i=0;i<sawCnt;i++) {
	str= (*env)->NewStringUTF(env,bssids[i]);
	(*env)->SetObjectArrayElement(env,newArr,i*5,str);
	(*env)->DeleteLocalRef(env,str); // not really needed

	str= (*env)->NewStringUTF(env,ssids[i]);
	(*env)->SetObjectArrayElement(env,newArr,i*5+1,str);
	(*env)->DeleteLocalRef(env,str); // not really needed

	sprintf(locStr,"%d",rsss[i]);
	str= (*env)->NewStringUTF(env,locStr);
	(*env)->SetObjectArrayElement(env,newArr,i*5+2,str);
	(*env)->DeleteLocalRef(env,str); // not really needed

	sprintf(locStr,"%d",weps[i]);
	str= (*env)->NewStringUTF(env,locStr);
	(*env)->SetObjectArrayElement(env,newArr,i*5+3,str);
	(*env)->DeleteLocalRef(env,str); // not really needed

	sprintf(locStr,"%d",aps[i]);
	str= (*env)->NewStringUTF(env,locStr);
	(*env)->SetObjectArrayElement(env,newArr,i*5+4,str);
	(*env)->DeleteLocalRef(env,str); // not really needed

    }
    return newArr;
}

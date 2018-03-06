#include <jni.h>

extern int ss_server_main(int argc, char **argv);
extern void ss_server_stop();
extern void vpn_interface_init(JNIEnv *env);
extern void vpn_interface_final();

JNIEXPORT jint JNICALL
Java_com_kkt_smartsocks_SSProxyServer_SSProxyServerStart(JNIEnv *env, jobject instance, jint argc,
                                                         jobject argv)
{
    jclass classArrayList = (*env)->GetObjectClass(env, argv);
    jmethodID arrayListItemGet = (*env)->GetMethodID(env, classArrayList,
                                                     "get", "(I)Ljava/lang/Object;");
    int i = 0;
    char *serverArgv[32] = {0};
    jstring paramString;

    vpn_interface_init(env);

    for (i = 0; i < argc; i++) {
        paramString = (*env)->CallObjectMethod(env, argv, arrayListItemGet, i);
        serverArgv[i] = (*env)->GetStringUTFChars(env, paramString, NULL);
    }

    return ss_server_main(argc, serverArgv);
}

JNIEXPORT jint JNICALL
Java_com_kkt_smartsocks_SSProxyServer_SSProxyServerStop(JNIEnv *env, jobject instance)
{
    ss_server_stop();
    vpn_interface_final();
}
//
// Created by owen on 18-3-6.
//

#include <jni.h>

static JNIEnv *g_env = NULL;

void vpn_interface_init(JNIEnv *env)
{
    g_env = env;
}

void vpn_interface_protect_socket(int socket)
{
    jclass clazz = NULL;
    jmethodID protectSocketFdMethod;

    if (NULL == g_env) {
        return;
    }

    clazz =(*g_env)->FindClass(g_env, "com/kkt/smartsocks/core/LocalVpnService");
    if (NULL == clazz) {
        return;
    }

    protectSocketFdMethod = (*g_env)->GetStaticMethodID(g_env, clazz, "protectSocket", "(I)V");
    if (NULL == protectSocketFdMethod) {
        return;
    }

    (*g_env)->CallStaticVoidMethod(g_env, clazz, protectSocketFdMethod, (jint)socket);
}

void vpn_interface_final()
{
    g_env = NULL;
}
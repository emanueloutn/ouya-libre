/* OuyaLibre standalone JNI bootstrap, MIT. Uses the public JNI invocation table.
 * Initializes LibVLC without the separate VLC media-library module. */
typedef int jint;
typedef const struct JavaVMTable *JavaVM;
struct JavaVMTable {void *reserved0,*reserved1,*reserved2;void *destroy,*attach,*detach;jint (*GetEnv)(JavaVM *,void **,jint);void *attachDaemon;};
extern int VLCJNI_OnLoad(JavaVM *,void *);
extern void VLCJNI_OnUnload(JavaVM *,void *);
__attribute__((visibility("default"))) jint JNI_OnLoad(JavaVM *vm,void *reserved){void *env=0;(void)reserved;if((*vm)->GetEnv(vm,&env,0x00010006)!=0)return -1;return VLCJNI_OnLoad(vm,env)==0?0x00010006:-1;}
__attribute__((visibility("default"))) void JNI_OnUnload(JavaVM *vm,void *reserved){void *env=0;(void)reserved;if((*vm)->GetEnv(vm,&env,0x00010006)==0)VLCJNI_OnUnload(vm,env);}

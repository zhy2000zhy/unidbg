package com.zhy;

import com.github.unidbg.Module;
import com.github.unidbg.*;
import com.github.unidbg.arm.HookStatus;
import com.github.unidbg.arm.backend.DynarmicFactory;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.hook.HookContext;
import com.github.unidbg.hook.ReplaceCallback;
import com.github.unidbg.hook.hookzz.*;
import com.github.unidbg.hook.whale.IWhale;
import com.github.unidbg.hook.whale.Whale;
import com.github.unidbg.hook.xhook.IxHook;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.XHookImpl;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.io.IOException;

public class test_xhook extends AbstractJni {
    private final VM vm;
    private final DvmClass dvmClass;
    private final Module module;
    private final AndroidEmulator emulator;
    private final String processName = "com.zhy.test2";
    private final String classname = "com.zhy.test2.MainActivity";
    private final Memory memory;

    public test_xhook() {
        emulator = AndroidEmulatorBuilder
                .for64Bit().setProcessName("com.zhy.test2")
                .addBackendFactory(new DynarmicFactory(true))
                .build();
        /*//so中多线程支持
        emulator = AndroidEmulatorBuilder
                .for64Bit().setProcessName("com.zhy.test2")
                .addBackendFactory(new Unicorn2Factory(false))
                .build();
        emulator.getBackend().registerEmuCountHook(10000);
        emulator.getSyscallHandler().setEnableThreadDispatcher(true);
        emulator.getSyscallHandler().setVerbose(true);*/

        memory = emulator.getMemory();
        memory.setLibraryResolver(createLibraryResolver(23));
        String filepath =System.getProperty("user.dir");//获取当前项目目录 unidbg目录
        File fileapk = new File(filepath+"/assets/test.apk");
        System.out.println(fileapk);
        vm = emulator.createDalvikVM(fileapk);
        vm.setJni(this);
        vm.setVerbose(true);

        DalvikModule dalvikModule = vm.loadLibrary(new File("unidbg-android/src/test/resources/example/libnative-mutihook.so"),false);
        module = dalvikModule.getModule();
        //xhook();
        HookZz_hook();
        Dobby_hook();
        whale_hook();
        dalvikModule.callJNI_OnLoad(emulator);
        dvmClass = vm.resolveClass(classname);
    }

    public static void main(String[] args) throws IOException {
        // 测试
        test_xhook test01 = new test_xhook();
        test01.start();
        test01.stop();
    }

    private void start() {
        // 9. 调用jni方法
        String str = "hello";
        DvmObject result = dvmClass.callStaticJniMethodObject(emulator, "stringFromJNI(Landroid/content/Context,Ljava/lang/String;)Ljava/lang/String",vm.resolveClass("android/content/Context").newObject(null),str);
        // 10. 打印结果
        System.out.println("result = " + result.getValue());
    }

    private void stop() throws IOException {
        emulator.close();
    }

    private LibraryResolver createLibraryResolver(int ver) {
        return new AndroidResolver(ver);
    }

    private void xhook(){
        IxHook xHook = XHookImpl.getInstance(emulator);
        xHook.register("libnative-xhook.so", "ptrace", new ReplaceCallback() {
            @Override
            public HookStatus onCall(Emulator<?> emulator, HookContext context, long originFunction) {
                int ptrace_arg0 = context.getIntArg(0);
                System.out.println(ptrace_arg0);
                context.push(ptrace_arg0);
                return HookStatus.RET(emulator,originFunction);
            }

            @Override
            public void postCall(Emulator<?> emulator, HookContext context) {
                System.out.println("ptrace="+context.pop()+",ret="+context.getIntArg(0));

            }
        },true);
        xHook.refresh();
    }


    private void HookZz_hook(){
        IHookZz hookZz = HookZz.getInstance(emulator);
        hookZz.enable_arm_arm64_b_branch();
        hookZz.wrap(module.findSymbolByName("ptrace"), new WrapCallback<RegisterContext>() {
            @Override
            public void preCall(Emulator<?> emulator, RegisterContext ctx, HookEntryInfo info) {
                int ptrace_args0 = ctx.getIntArg(0);
                System.out.println(ptrace_args0);
            }

            @Override
            public void postCall(Emulator<?> emulator, RegisterContext ctx, HookEntryInfo info) {
                System.out.println("ptrace ret=" + ctx.getIntArg(0));
            }

        });
        hookZz.disable_arm_arm64_b_branch();
    }

    private void Dobby_hook(){
        Dobby dobby = Dobby.getInstance(emulator);
        dobby.replace(module.findSymbolByName("_Z27create_thread_check_traceidv"), new ReplaceCallback() {
            @Override
            public HookStatus onCall(Emulator<?> emulator, HookContext context, long originFunction) {
                System.out.println("create_thread_check_traceid.onCall function address => 0x" + Long.toHexString(originFunction));
                return HookStatus.RET(emulator, originFunction);
            }

            @Override
            public void postCall(Emulator<?> emulator, HookContext context) {
                System.out.println("create_thread_check_traceid.postCall ");
            }
        },true);
    }

    private void whale_hook(){
        IWhale whale = Whale.getInstance(emulator);
        Symbol system_property_get = module.findSymbolByName("__system_property_get");

        whale.inlineHookFunction(system_property_get, new ReplaceCallback() {
            @Override
            public HookStatus onCall(Emulator<?> emulator, long originFunction) {
                System.out.println("WInlineHookFunction system_property_get = " + emulator.getContext().getPointerArg(0).getString(0));
                return HookStatus.RET(emulator, originFunction);
            }

        });
    }
}

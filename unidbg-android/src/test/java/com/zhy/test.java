package com.zhy;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.LibraryResolver;
import com.github.unidbg.arm.backend.DynarmicFactory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.io.IOException;

public class test extends AbstractJni {
    private final VM vm;
    private final DvmClass dvmClass;
    private final AndroidEmulator emulator;
    private final String processName = "com.zhy.test2";
    private final String classname = "com.zhy.test2.MainActivity";
    private final Memory memory;

    public test() {
        emulator = AndroidEmulatorBuilder
                .for64Bit().setProcessName("com.zhy.test2")
                .addBackendFactory(new DynarmicFactory(true))
                .build();
        memory = emulator.getMemory();
        memory.setLibraryResolver(createLibraryResolver(23));
        String filepath =System.getProperty("user.dir");
        File fileapk = new File(filepath+"/assets/test.apk");
        System.out.println(fileapk);
        vm = emulator.createDalvikVM(fileapk);
        vm.setJni(this);
        vm.setVerbose(true);

        DalvikModule dalvikModule = vm.loadLibrary(new File("unidbg-android/src/test/resources/example/libnative-lib02.so"),false);
        dalvikModule.callJNI_OnLoad(emulator);
        dvmClass = vm.resolveClass(classname);
    }

    public static void main(String[] args) throws IOException {
        // 测试
        test test01 = new test();
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

}

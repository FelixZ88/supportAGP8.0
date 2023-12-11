package com.cs.plugin.timecost

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter

class TimeCostClassVisitor(nextVisitor: ClassVisitor,val config: TimeCostConfig) : ClassVisitor(
    Opcodes.ASM5, nextVisitor) {

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
        print("TimeCostClassVisitor $name")
        if (name == "<clinit>" || name == "<init>") {
            return methodVisitor
        }
        //如果不在配置的方法名列表中，不执行
        val methodNameConfig = config.methodNames.get()
        if (methodNameConfig.isNotEmpty()) {
            if (methodNameConfig.none { name == it }) {
                return methodVisitor
            }
        }
        //从配置中读取tag
        val tag = config.logTag.get()
        val newMethodVisitor =
            object : AdviceAdapter(Opcodes.ASM5, methodVisitor, access, name, descriptor) {
                private var startTimeLocal = -1 // 保存 startTime 的局部变量索引

                override fun visitInsn(opcode: Int) {
                    super.visitInsn(opcode)
                }

                override fun visitMethodInsn(
                    opcode: Int,
                    owner: String?,
                    name: String?,
                    descriptor: String?,
                    isInterface: Boolean
                ) {
                    print("visitMethodInsn $owner $name $descriptor")
                    if (owner == "android.util.Log" && name == "i") {
                        super.visitMethodInsn(opcode, owner, "d", descriptor, isInterface)
                        return
                    }
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
                }

                @Override
                override fun onMethodEnter() {
                    super.onMethodEnter();
                    print("onMethodEnter")
                    // 在onMethodEnter中插入代码 val startTime = System.currentTimeMillis()
                    mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/System",
                        "currentTimeMillis",
                        "()J",
                        false
                    )
                    startTimeLocal = newLocal(Type.LONG_TYPE) // 创建一个新的局部变量来保存 startTime
                    mv.visitVarInsn(Opcodes.LSTORE, startTimeLocal)
                }

                @Override
                override fun onMethodExit(opcode: Int) {
                    print("onMethodExit")
                    // 在onMethodExit中插入代码 Log.e("tag", "Method: $name, timecost: " + (System.currentTimeMillis() - startTime))
                    mv.visitTypeInsn(
                        Opcodes.NEW,
                        "java/lang/StringBuilder"
                    );
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitLdcInsn("PluginThread: Method: $name, timecost: ");
                    mv.visitMethodInsn(
                        Opcodes.INVOKESPECIAL,
                        "java/lang/StringBuilder",
                        "<init>",
                        "(Ljava/lang/String;)V",
                        false
                    );
                    mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/System",
                        "currentTimeMillis",
                        "()J",
                        false
                    );
                    mv.visitVarInsn(Opcodes.LLOAD, startTimeLocal);
                    mv.visitInsn(Opcodes.LSUB);
                    mv.visitMethodInsn(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/StringBuilder",
                        "append",
                        "(J)Ljava/lang/StringBuilder;",
                        false
                    );
                    mv.visitMethodInsn(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/StringBuilder",
                        "toString",
                        "()Ljava/lang/String;",
                        false
                    );
                    //从配置中读取tag
                    mv.visitLdcInsn(tag)
                    mv.visitInsn(Opcodes.SWAP)
                    mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "android/util/Log",
                        "e",
                        "(Ljava/lang/String;Ljava/lang/String;)I",
                        false
                    )
                    mv.visitInsn(POP)
                    super.onMethodExit(opcode);
                }
            }
        return newMethodVisitor
    }
}
package JaideepSinghHeer.Minecraft.mod;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.net.ServerSocket;

public class SPLANtransformerPort implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        //System.out.println("--------------------> Trying Injection (PORT) !");
        if(transformedName.equals("net.minecraft.util.HttpUtil"))
        {
            // Found Target Class

            // Create a new ClassNode to roam around the Class file.
            ClassNode node = new ClassNode();
            // Assign a ClassReader to the basicClass byte array, i.e. similar to asigning buffer to input.
            ClassReader reader = new ClassReader(basicClass);
            // Attach node to the ClassReader(similar to buffer)
            reader.accept(node, 0);

            // Make node Cycle through all the classes
            for (MethodNode mn : node.methods) {
                if (mn.desc.equals("()I") && mn.exceptions.size() == 1 && mn.exceptions.get(0).equals("java/io/IOException") && mn.signature == null) {
                    // Reached Target Method
                    System.out.println("--------------------> Reached Target Method (PORT) !");
                    InsnList insnList = mn.instructions;

                    for (int i = 0; i < insnList.size(); i++) {
                        AbstractInsnNode ain = insnList.get(i);
                        if (ain != null && ain.getType() == AbstractInsnNode.METHOD_INSN && ain.getOpcode() == Opcodes.INVOKEVIRTUAL && ((MethodInsnNode) ain).desc.equals("()I")) {
                            AbstractInsnNode ain2 = ain.getNext();
                            if (ain2 != null && ain2.getType() == AbstractInsnNode.VAR_INSN && ain2.getOpcode() == Opcodes.ISTORE) {
                                //Pretty positive we've found the right entry-point.
                                insnList.remove(ain.getPrevious());
                                insnList.remove(ain);
                                insnList.insertBefore(ain2, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(getClass()), "getPort", "()I", false));
                            }
                        }
                    }

                }
            }

            // Create a ClassWriter and tell it to automatically manage Stack Size
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            // Attach ClassWriter to node(similar to output buffer)
            node.accept(writer);

            // Convert ClassWriter(output buffer) to byte array
            // And Return
            return writer.toByteArray();
        }
        else
        return basicClass;
    }
    public static int getPort()
    {
        System.out.println("Port To Set : "+ServerPropertiesLAN.instance.port);
        if(!(ServerPropertiesLAN.instance.port >0&&ServerPropertiesLAN.instance.port<65536))
        try {
            ServerSocket serversocket = new ServerSocket(0);
            ServerPropertiesLAN.instance.port = serversocket.getLocalPort();
            serversocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Setting Port : "+ServerPropertiesLAN.instance.port);
        return ServerPropertiesLAN.instance.port;
    }
}

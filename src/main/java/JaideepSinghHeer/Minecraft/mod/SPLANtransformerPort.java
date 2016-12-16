package JaideepSinghHeer.Minecraft.mod;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.net.ServerSocket;


/**
 * This Class acts as the {@link IClassTransformer} for the ByteCode Editing during Compilation.
 * It is used on all the ByteCode files and is hence given a chance to Edit any ByteCode file.
 * Every ByteCode file before compilation undergoes Transformation by all the Classes derived from {@link IClassTransformer}
 * and registered as TransformerClasses by the {@link net.minecraftforge.fml.relauncher.IFMLLoadingPlugin} Classes.
 *
 * Hence, once registered, this Class can Edit the ByteCode of any Minecraft Class during Compilation.
 * <Probalby> :)
 *
 */
public class SPLANtransformerPort implements IClassTransformer {
    /**
     * This is the main and only function called during Compilation for ByteCode Manipulation.
     *
     * @param name I have no idea about what that is. <Sorry> :)
     * @param transformedName It is the canonical Class name of the Class whose ByteCode is currently being compiled.
     * @param basicClass It is the ByteArray which stores the Original ByteCode of this Class.
     *
     * @return It returns the ByteArray which should be Compiled instead, i.e. It returns the new edited ByteCode..!
     */
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

    /**
     * This function is made to replace the serversocket.getLocalPort() function call
     * in the getSuitableLanPort() function of the {@link net.minecraft.util.HttpUtil} Class.
     *
     * The getSuitableLanPort() function call is searched-for in the ByteCode
     * and is then replaced by a call to this function
     * which returns the same DataType, i.e. Integer.
     *
     * Hence the variable 'i' in the getSuitableLanPort() function of the {@link net.minecraft.util.HttpUtil} Class
     * stores the return value of our custom function
     * which is then returned back to be open as a LAN Port ...!
     *
     */
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

package JaideepSinghHeer.Minecraft.mod;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import java.io.*;
import java.util.Map;
import java.util.regex.Pattern;

//@Mod(modid = ServerPropertiesLAN.MODID,name=ServerPropertiesLAN.MODNAME, version = ServerPropertiesLAN.VERSION,clientSideOnly = true,acceptableRemoteVersions = "*",useMetadata = true)
public class ServerPropertiesLAN extends DummyModContainer implements IFMLLoadingPlugin
{
    public int port=-1;

    public static final String MODID = "serverpropertieslan";
    public static final String MODNAME = "Server Properties LAN";
    public static final String VERSION = "2";
    private PropertyManagerClient ServerProperties = null;
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

    public ServerPropertiesLAN()
    {
        super(new ModMetadata());
        System.out.println("-=-=-=-=-=-=-=ServerPropertiesLAN-Constructed=-=-=-=-=-=-=-");
        instance = this;
        ModMetadata md = getMetadata();
        md.modId=MODID;
        md.version=VERSION;
        md.name=MODNAME;
        md.authorList = Lists.newArrayList("Jaideep Singh Heer");
        md.description = "MeoW.!";
        md.credits = "by Jaideep Singh Heer";
        md.logoFile = "logo.png";
        md.screenshots = new String[]{"scr1.jpg","scr2.jpg", "logo2.png"};
        md.url = "https://minecraft.curseforge.com/projects/server-properties-for-lan";
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller)
    {
        //System.out.println("-=-=-=-=-=-=-=EventBusRegistered=-=-=-=-=-=-=-=-");
        bus.register(this);
        return true;
    }

    public static ServerPropertiesLAN instance;

    @Subscribe
    public void onServerStarting(FMLServerStartingEvent event) {
        System.out.println("========================>> Server Starting !");
        ServerProperties = new PropertyManagerClient(new File(DimensionManager.getCurrentSaveRootDirectory()+"\\server.properties"));
        LOGGER.warn(DimensionManager.getCurrentSaveRootDirectory()+"\\server.properties");
        port = ServerProperties.getIntProperty("port", 25565);
        System.out.println("-------------------> Port Read : "+port);
        event.getServer().setOnlineMode(ServerProperties.getBooleanProperty("online-mode", true));
        event.getServer().setCanSpawnAnimals(ServerProperties.getBooleanProperty("spawn-animals", true));
        event.getServer().setCanSpawnNPCs(ServerProperties.getBooleanProperty("spawn-npcs", true));
        event.getServer().setAllowPvp(ServerProperties.getBooleanProperty("pvp", true));
        event.getServer().setAllowFlight(ServerProperties.getBooleanProperty("allow-flight", false));
        event.getServer().setResourcePack(ServerProperties.getStringProperty("resource-pack", ""), this.loadResourcePackSHA());
        event.getServer().setMOTD(ServerProperties.getStringProperty("motd", "<! "+event.getServer().getServerOwner() + "'s " + event.getServer().worldServers[0].getWorldInfo().getWorldName()+" ON LAN !>"));
        event.getServer().setPlayerIdleTimeout(ServerProperties.getIntProperty("player-idle-timeout", 0));
        event.getServer().setBuildLimit(ServerProperties.getIntProperty("max-build-height", 256));
        //if(!Minecraft.getMinecraft().getVersion().substring(0,3).equalsIgnoreCase("1.8")) PlayerProfileCache.setOnlineMode(event.getServer().isServerInOnlineMode());
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{SPLANtransformerPort.class.getCanonicalName()};
    }

    @Override
    public String getModContainerClass() {
        return ServerPropertiesLAN.class.getCanonicalName();
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data){
        ;
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }


    private String loadResourcePackSHA()
    {
        if (ServerProperties.hasProperty("resource-pack-hash"))
        {
            if (ServerProperties.hasProperty("resource-pack-sha1"))
            {
                LOGGER.warn("resource-pack-hash is deprecated and found along side resource-pack-sha1. resource-pack-hash will be ignored.");
            }
            else
            {
                LOGGER.warn("resource-pack-hash is deprecated. Please use resource-pack-sha1 instead.");
                ServerProperties.getStringProperty("resource-pack-sha1", ServerProperties.getStringProperty("resource-pack-hash", ""));
                ServerProperties.removeProperty("resource-pack-hash");
            }
        }

        String s = ServerProperties.getStringProperty("resource-pack-sha1", "");

        if (!s.isEmpty() && !Pattern.compile("^[a-fA-F0-9]{40}$").matcher(s).matches())
        {
            LOGGER.warn("Invalid sha1 for ressource-pack-sha1");
        }

        if (!ServerProperties.getStringProperty("resource-pack", "").isEmpty() && s.isEmpty())
        {
            LOGGER.warn("You specified a resource pack without providing a sha1 hash. Pack will be updated on the client only if you change the name of the pack.");
        }

        return s;
    }


    }


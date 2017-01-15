package JaideepSinghHeer.Minecraft.mod;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.server.management.UserListWhitelist;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import java.io.*;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * We cannot use the {@link Mod} annotation as the mod is already instantiated
 * via the {@link IFMLLoadingPlugin} interface as a CoreMod.
 *
 * We need it to be a CoreMod so that we can Edit the ByteCode
 * using the {@link net.minecraft.launchwrapper.IClassTransformer} interface.
 *
 * We Edit the ByteCode of the {@link net.minecraft.util.HttpUtil} class to return our specified Port for LAN connections.
 * @see net.minecraft.util.HttpUtil for the getSuitableLanPort() method which returns a LAN port.
 *
 */

//@Mod(modid = ServerPropertiesLAN.MODID,name=ServerPropertiesLAN.MODNAME, version = ServerPropertiesLAN.VERSION,clientSideOnly = true,acceptableRemoteVersions = "*",useMetadata = true)

public class ServerPropertiesLAN extends DummyModContainer implements IFMLLoadingPlugin
{
    public int port=-1;
    private static boolean whiteListFirstRun;
    private static MinecraftServer server;

    public static final String MODID = "serverpropertieslan";
    public static final String MODNAME = "Server Properties LAN";
    public static final String VERSION = "2.1";

    // This Class manages all the File IO.
    private PropertyManagerClient ServerProperties = null;
    // Logger to get output in The Log.
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

    /**
     * We don't want to do a lot of work so we extend {@link DummyModContainer}.
     * As it contains a {@link ModMetadata} object,
     * it must be initialised.
     *
     */
    public ServerPropertiesLAN()
    {
        super(new ModMetadata());
        whiteListFirstRun = false;
        System.out.println("-=-=-=-=-=-=-=ServerPropertiesLAN-Constructed=-=-=-=-=-=-=-");
        // static instance to always get the correct object.
        instance = this;
        // Mod Metadata defined in DummyModContainer received by func. getMetadata()
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

    /**
     * We cannot use {@link net.minecraftforge.fml.common.Mod.EventHandler} as that is a part of the {@link Mod} annotation
     * and hence requires a Class to be annotated with the {@link Mod} annotation which cannot be done for CoreMods.<See Above>
     *
     * Therefore we must register this class to the {@link EventBus} provided to it for being the {@link ModContainer}.
     *
     */
    @Override
    public boolean registerBus(EventBus bus, LoadController controller)
    {
        //System.out.println("-=-=-=-=-=-=-=EventBusRegistered=-=-=-=-=-=-=-=-");
        bus.register(this);
        return true;
    }

    /**
     * The static instance of this Class to be accessed as a Mod.
     * Forge automatically instantiates an Object for us
     * and we assign that to this object(called instance) in the constructor.
     */
    public static ServerPropertiesLAN instance;

    /**
     * This function is subscribed to the {@link EventBus} via the {@link Subscribe} annotation.
     * The type of event({@link net.minecraftforge.fml.common.eventhandler.Event}) to be subscribed is judged from the prototype.
     * This function gets the {@link net.minecraft.server.MinecraftServer} from the event
     * and gets the world save directory using the {@link DimensionManager}.
     *
     * It then uses the {@link PropertyManagerClient} Class to save/load data from the server.properties file
     * and sets the attributes of the {@link net.minecraft.server.MinecraftServer} via its functions.
     *
     */
    @Subscribe
    public void onServerStarting(FMLServerStartingEvent event) {
        System.out.println("========================>> Server Starting !");
        ServerProperties = new PropertyManagerClient(new File(DimensionManager.getCurrentSaveRootDirectory()+"\\server.properties"));
        LOGGER.warn(DimensionManager.getCurrentSaveRootDirectory()+"\\server.properties");
        port = ServerProperties.getIntProperty("port", 25565);
        System.out.println("-------------------> Port Read : "+port);
        server = event.getServer();
        server.setOnlineMode(ServerProperties.getBooleanProperty("online-mode", true));
        server.setCanSpawnAnimals(ServerProperties.getBooleanProperty("spawn-animals", true));
        server.setCanSpawnNPCs(ServerProperties.getBooleanProperty("spawn-npcs", true));
        server.setAllowPvp(ServerProperties.getBooleanProperty("pvp", true));
        server.setAllowFlight(ServerProperties.getBooleanProperty("allow-flight", false));
        server.setResourcePack(ServerProperties.getStringProperty("resource-pack-sha1", ""), this.loadResourcePackSHA());
        server.setMOTD(ServerProperties.getStringProperty("motd", "<! "+server.getServerOwner() + "'s " + server.worldServers[0].getWorldInfo().getWorldName()+" ON LAN !>"));
        server.setPlayerIdleTimeout(ServerProperties.getIntProperty("player-idle-timeout", 0));
        server.setBuildLimit(ServerProperties.getIntProperty("max-build-height", 256));

        // Get the PlayerList Settings Object
        PlayerList customPlayerList =  server.getPlayerList();

        // REFLECTION !!
        try {
            // Set MaxPlayers
            Field field = PlayerList.class.getDeclaredField("maxPlayers");
            field.setAccessible(true);
            field.set(customPlayerList, ServerProperties.getIntProperty("max-players", 1));

            // Set MaxViewDistance
            Field dist = PlayerList.class.getDeclaredField("viewDistance");
            dist.setAccessible(true);
            int d = ServerProperties.getIntProperty("max-view-distance", 0);
            if(d>0)dist.set(customPlayerList, d);

            if (ServerProperties.getBooleanProperty("white-list", false))
            {
                File whitelistjson = new File(DimensionManager.getCurrentSaveRootDirectory() + "\\whitelist.json");
                UserListWhitelist whitelist = new UserListWhitelist(whitelistjson);
                if(!whitelistjson.exists()) {
                    whitelistjson.createNewFile();
                    whitelist.writeChanges();
                    whiteListFirstRun = true;
                    // Set WhiteList
                    field = PlayerList.class.getDeclaredField("whiteListedPlayers");
                    field.setAccessible(true);
                    field.set(customPlayerList, whitelist);
                }
                else {
                    // Not In First Run.
                    customPlayerList.setWhiteListEnabled(true);
                }
            }
            server.setPlayerList(customPlayerList);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        //if(!Minecraft.getMinecraft().getVersion().substring(0,3).equalsIgnoreCase("1.8")) PlayerProfileCache.setOnlineMode(event.getServer().isServerInOnlineMode());
    }

    /**
     * This doesn't work sadly ! :(
     * The Event is never fired on the client side it seems :(
     * */
    @Subscribe
    public void onPlayerConnect(EntityJoinWorldEvent e) {
        System.out.println("==================>> Player Connect");
        if (whiteListFirstRun) {
            System.out.println("--------------ID="+e.getEntity().getUniqueID()+"\tNAME="+e.getEntity().getName());
            server.getPlayerList().addWhitelistedPlayer(e.getEntity().getServer().getPlayerList().getAllProfiles()[0]);
            server.getPlayerList().setWhiteListEnabled(true);
            try {
                server.getPlayerList().getWhitelistedPlayers().writeChanges();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * These functions are a part of the {@link IFMLLoadingPlugin} interface.
     * @see IFMLLoadingPlugin for details.
     */
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


    /**
     * This function checks the current ResoursePackSHA's validity
     * and returns the final ResoursePackSHA values of the server.
     */
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


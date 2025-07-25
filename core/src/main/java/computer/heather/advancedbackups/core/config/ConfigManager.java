package computer.heather.advancedbackups.core.config;

import computer.heather.advancedbackups.core.ABCore;
import computer.heather.advancedbackups.core.backups.BackupWrapper;
import computer.heather.advancedbackups.core.backups.ThreadedBackup;
import computer.heather.advancedbackups.core.config.ConfigTypes.BooleanValue;
import computer.heather.advancedbackups.core.config.ConfigTypes.ConfigValidationEnum;
import computer.heather.advancedbackups.core.config.ConfigTypes.FloatValue;
import computer.heather.advancedbackups.core.config.ConfigTypes.FreeStringValue;
import computer.heather.advancedbackups.core.config.ConfigTypes.LongValue;
import computer.heather.advancedbackups.core.config.ConfigTypes.StringArrayValue;
import computer.heather.advancedbackups.core.config.ConfigTypes.ValidatedStringValue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ConfigManager {

    private static HashMap<String, ConfigTypes> entries = new HashMap<>();


    public static void register(String key, ConfigTypes configType) {
        entries.put(key, configType);
    }


    public static final BooleanValue enabled = new BooleanValue("config.advancedbackups.enabled", true, ConfigManager::register);
    public static final BooleanValue save = new BooleanValue("config.advancedbackups.save", true, ConfigManager::register);
    public static final BooleanValue toggleSave = new BooleanValue("config.advancedbackups.togglesave", true, ConfigManager::register);
    public static final LongValue buffer = new LongValue("config.advancedbackups.buffer", 1048576, 1024, Integer.MAX_VALUE, ConfigManager::register); //5mb
    public static final BooleanValue flush = new BooleanValue("config.advancedbackups.flush", false, ConfigManager::register);
    public static final BooleanValue activity = new BooleanValue("config.advancedbackups.activity", true, ConfigManager::register);
    public static final StringArrayValue blacklist = new StringArrayValue("config.advancedbackups.blacklist", new String[]{"session.lock", "*_old"}, ConfigManager::register);
    public static final ValidatedStringValue type = new ValidatedStringValue("config.advancedbackups.type", "differential", new String[]{"zip", "differential", "incremental"}, ConfigManager::register);
    public static final FreeStringValue path = new FreeStringValue("config.advancedbackups.path", "./backups", ConfigManager::register);
    public static final FloatValue minFrequency = new FloatValue("config.advancedbackups.frequency.min", 0.25F, 0F, 500F, ConfigManager::register);
    public static final FloatValue maxFrequency = new FloatValue("config.advancedbackups.frequency.max", 24F, 0.5F, 500F, ConfigManager::register);
    public static final BooleanValue uptime = new BooleanValue("config.advancedbackups.frequency.uptime", true, ConfigManager::register);
    public static final StringArrayValue timesArray = new StringArrayValue("config.advancedbackups.frequency.schedule", new String[]{"1:00"}, ConfigManager::register);
    public static final BooleanValue shutdown = new BooleanValue("config.advancedbackups.frequency.shutdown", false, ConfigManager::register);
    public static final BooleanValue startup = new BooleanValue("config.advancedbackups.frequency.startup", false, ConfigManager::register);
    public static final LongValue delay = new LongValue("config.advancedbackups.frequency.delay", 30, 5, 1000, ConfigManager::register);
    /*
     * New logging options!
     * Clients = OPS, ALL, NONE. OPS is default, means operator permission is required. ALL is all clients with the mod. NONE disabled.
     * Client frequency - how often progress is sent to clients. Only the latest one is sent, and the client toasts persist until a complete, failed or cancelled notification is recieved.
     * Console - enable or disable console logging for backup progress. Start / finish are always logged.
     * Console frequency - how often progress is logged in console.
     */
    public static final ValidatedStringValue clients = new ValidatedStringValue("config.advancedbackups.logging.clients", "ops", new String[]{"ops", "all", "none"}, ConfigManager::register);
    public static final LongValue clientFrequency = new LongValue("config.advancedbackups.logging.clientfrequency", 500L, 0L, Long.MAX_VALUE, ConfigManager::register);
    public static final BooleanValue console = new BooleanValue("config.advancedbackups.logging.console", true, ConfigManager::register);
    public static final LongValue consoleFrequency = new LongValue("config.advancedbackups.logging.consolefrequency", 5000L, 0L, Long.MAX_VALUE, ConfigManager::register);
    public static final LongValue compression = new LongValue("config.advancedbackups.zips.compression", 4, 1, 9, ConfigManager::register);
    public static final BooleanValue splitZips = new BooleanValue("config.advancedbackups.zips.split", false, ConfigManager::register);
    public static final LongValue zipSplitSize = new LongValue("config.advancedbackups.zips.splitsize", 1024, 1, Long.MAX_VALUE, ConfigManager::register);
    public static final LongValue length = new LongValue("config.advancedbackups.chains.length", 50, 5, 500, ConfigManager::register);
    public static final BooleanValue compressChains = new BooleanValue("config.advancedbackups.chains.compress", true, ConfigManager::register);
    public static final BooleanValue smartChains = new BooleanValue("config.advancedbackups.chains.smart", true, ConfigManager::register);
    public static final FloatValue chainsPercent = new FloatValue("config.advancedbackups.chains.maxpercent", 50F, 1F, 100F, ConfigManager::register);
    public static final FloatValue size = new FloatValue("config.advancedbackups.purge.size", 50F, 0F, Float.MAX_VALUE, ConfigManager::register);
    public static final LongValue daysToKeep = new LongValue("config.advancedbackups.purge.days", 0L, 0L, Long.MAX_VALUE, ConfigManager::register);
    public static final LongValue backupsToKeep = new LongValue("config.advancedbackups.purge.count", 0L, 0L, Long.MAX_VALUE, ConfigManager::register);
    public static final BooleanValue purgeIncrementals = new BooleanValue("config.advancedbackups.purge.incrementals", true, ConfigManager::register);
    public static final LongValue incrementalChains = new LongValue("config.advancedbackups.purge.incrementalchains", 1, 1, Long.MAX_VALUE, ConfigManager::register);


    public static void loadOrCreateConfig() {
        // Called when the config needs to be loaded, but one may not exist.
        // Creates a new config it one doesn't exist, then loads it.
        File dir = new File("./config");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File("./AdvancedBackups.properties");
        if (file.exists()) {
            migrateConfig();
        }
        file = new File(dir, "AdvancedBackups.properties");
        if (!file.exists()) {
            writeConfig();
        }
        loadConfig();
    }

    private static void writeConfig() {
        // Called to write to a config file.
        // Create a complete properties file in the cwd, including any existing changes
        ABCore.infoLogger.accept("Preparing to write to properties file...");
        File file = new File("./config/AdvancedBackups.properties");
        try {
            file.createNewFile();
            file.setWritable(true);
            InputStream is = ConfigManager.class.getClassLoader().getResourceAsStream("advancedbackups-properties.txt");

            String text = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));

            for (String key : entries.keySet()) {
                Matcher matcher = Pattern.compile(Pattern.quote(key) + "$", Pattern.MULTILINE).matcher(text);
                text = matcher.replaceAll(key + "=" + entries.get(key).save());
            }

            FileWriter writer = new FileWriter(file);
            writer.write(text);
            writer.close();
        } catch (IOException e) {
            // TODO : Scream to user
            ABCore.logStackTrace(e);
        }
    }


    private static void loadConfig() {
        //Load the config file.
        Properties props = new Properties();
        File file = new File("./config/AdvancedBackups.properties");

        FileReader reader;
        try {
            reader = new FileReader(file);
            props.load(reader);
            reader.close();
        } catch (IOException e) {
            // TODO : Scream to user
            ABCore.logStackTrace(e);
            return;
        }

        ArrayList<String> missingProps = new ArrayList<>();

        for (String key : entries.keySet()) {
            if (!props.containsKey(key)) {
                missingProps.add(key);
                ABCore.warningLogger.accept("Missing key : " + key);
                continue;
            }
            ConfigValidationEnum valid = entries.get(key).validate(props.getProperty(key));
            if (valid != ConfigValidationEnum.VALID) {
                missingProps.add(key);
                ABCore.warningLogger.accept(valid.getError() + " : " + key);
                continue;

            }
            entries.get(key).load(props.getProperty(key));
        }

        if (props.containsKey("config.advancedbackups.size")) {
            ABCore.warningLogger.accept("Migrating old config value :");
            ABCore.warningLogger.accept("config.advancedbackups.size -> config.advancedbackups.purge.size");

            size.load(props.getProperty("config.advancedbackups.size"));

        }

        if (!missingProps.isEmpty()) {
            ABCore.warningLogger.accept("The following properties were missing from the loaded file :");
            for (String string : missingProps) {
                ABCore.warningLogger.accept(string);
            }
            ABCore.warningLogger.accept("Properties file will be regenerated! Existing config values will be preserved.");

            writeConfig();
        }


        BackupWrapper.configuredPlaytime = new ArrayList<>();
        for (String time : timesArray.get()) {
            String[] hm = time.split(":");
            long hours = Long.parseLong(hm[0]) * 3600000L;
            long mins = Long.parseLong(hm[1]) * 60000;
            BackupWrapper.configuredPlaytime.add(hours + mins);
        }

        ABCore.backupPath = path.get() + "/" + (ABCore.worldDir.getParent().toFile().getName());

        ThreadedBackup.blacklist.clear();

        for (String string : blacklist.get()) {

            string = string.replace("\\", "/");
            string = string.replaceAll("[^a-zA-Z0-9*]", "\\\\$0");
            string = "^" + string.replace("*", ".*") + "$";

            ThreadedBackup.blacklist.add(Pattern.compile(string, Pattern.CASE_INSENSITIVE));
        }
    }


    private static void migrateConfig() {
        //Load the config file.
        Properties props = new Properties();
        File file = new File("./AdvancedBackups.properties");
        FileReader reader;
        try {
            reader = new FileReader(file);
            props.load(reader);
            reader.close();
            file.delete();
        } catch (IOException e) {
            // TODO : Scream to user
            ABCore.logStackTrace(e);
            return;
        }


        for (String key : entries.keySet()) {
            if (!props.containsKey(key)) {
                continue;
            }
            ConfigValidationEnum valid = entries.get(key).validate(props.getProperty(key));
            if (valid != ConfigValidationEnum.VALID) {
                continue;

            }
            entries.get(key).load(props.getProperty(key));
        }

        ABCore.warningLogger.accept("Config in old location detected! Migrating.");
        writeConfig();
    }
}
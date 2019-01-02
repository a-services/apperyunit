package io.appery.apperyunit;

import java.io.File;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Some static flags and methods for the app.
 */
class Utils {

    /**
     * App version number. If different from the number defined in `ApperyUnit` DB,
     * then GUI will suggest to upgrade.
     */
    public static String apperyUnitVersion = "1.01";

    /**
     * Folder name related to current folder where we'll look for 
     * '.dependencies', '.params' and '.paramlist's.
     */
    static String paramsFolder = "parameters";  
    
    /**
     * Folder name related to current folder where we'll store
     * JSON files with request results.
     */
    static String fixturesFolder = "fixtures";
    
    /**
     * Folder name related to current folder where we'll store
     * server-code libraries.
     */
    static String librariesFolder = "libraries";

    /**
     * File name of dependencies descriptor.
     */
    static String dependenciesJsonFile = "dependencies.json";

    //static boolean download_mode;
    
    static String script_name;
    static boolean echo_mode, test_mode;

    /* We have 2 secret options: `debug` and `color`.
     * 
     * `debug`::
     *    is switched on by `AU_DEBUG` environment variable and allows
     *    to bypass `PasswordDialog`.
     * `color`::
     *    is used on Windows computers to specify that console supports ANSI colors     
     */
    static boolean debug_mode;
    static boolean color_mode = !System.getProperty("os.name").startsWith("Windows");

    /**
     * ANSI color codes for console.
     * Can be empty if console doesn't support ANSI colors
     * and `color_mode` is switched off.
     */
    static String bold = "", ital = "", norm = "", red = "", cyan = "";

    static JTextArea console_area;
    static BatchRunner batch_runner;
   
    /**
     * Date format used in Appery server code logs.
     */
    static DateFormat logDateFormat = new SimpleDateFormat("dd.MM.yyyy, KK:mm:ss aa");

    static String logDate(long tstamp) {
        return logDateFormat.format(new Date(tstamp));
    }
    
    /**
     * Checks that folder `folderName` exists and creates it if necessary.
     */
    static void ensureFolder(String folderName) {
        File f = new File(folderName);
        if (f.exists()) {
            return;
        } 
        f.mkdir();
    }

    /**
     * Ensures ApperyUnit special folders to exist.
     */
    static void ensureApperyUnitFolders() {
        ensureFolder(fixturesFolder);
        ensureFolder(paramsFolder);
        ensureFolder(librariesFolder);
    }
    
    /**
     * Replacement for `println` that can send output to Swing console,
     * or `SwingWorker`, or just plain old `System.out`.
     */
    static void console(String msg) {
        if (console_area!=null) {
            if (batch_runner!=null) {
                batch_runner.print(msg + '\n');
            } else {
                console_area.append(msg + '\n');
            }
        } else {
            System.out.println(msg);
        }
    }
    
    /**
     * Replace special HTML entities, like the deprecated HTML tag `xmp` did.
     */
    static String xmp(String s) {
        return s.replaceAll("&", "&amp;").replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;").replaceAll("\"", "&quot;");
    }
    
    /**
     * ANSI color codes.
     * See http://misc.flogisoft.com/bash/tip_colors_and_formatting
     */
    static void initColors() {
        bold = color_mode ? "\u001B[1m\u001B[92m" : "";
        ital = color_mode ? "\u001B[92m" : "";
        norm = color_mode ? "\u001B[0m" : "";
        red = color_mode ? "\u001B[91m" : "";
        cyan = color_mode ? "\u001B[96m" : "";
    }
    
    static void setIcon(JFrame frame) {
        URL iconURL = frame.getClass().getResource("/io/appery/apperyunit/au-icon.png");
        ImageIcon icon = new ImageIcon(iconURL);
        frame.setIconImage(icon.getImage());
    }
}
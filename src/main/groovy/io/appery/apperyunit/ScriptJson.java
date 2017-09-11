package io.appery.apperyunit;

import java.util.List;

/**
 * Information about server-code script
 * downloaded as JSON from Appery.
 */
public class ScriptJson extends ScriptNodeJson {
    boolean isDownloaded;
    String name;
    String guid;
    List dependencies;
}
package io.appery.apperyunit;

/** 
 * User object in Swing tree.
 */
public class ScriptNode {

    /** `
     * data` is `null` for the root.
     */
    boolean isRoot;
    
    /**
     * Is `data` `ScriptJson` or `FolderJson` ?
     */
    boolean isScript;
    
    /**
     * Node marked as already downloaded.
     */
    boolean isDownloaded;
    
    /**
     * Name to display in Swing tree.
     */
    String name;
   
    /**
     *  `ScriptJson` or `FolderJson` for this Swing tree node.
     */
    ScriptNodeJson data;

    ScriptNode(String name) {
        this.name = name;    
    }
    
    public String toString() {
        return name;
    }

}

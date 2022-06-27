package example;

/**
 * EI_EXPOSE_REP issue for array usage
 */


import java.util.UUID;

public class ArrayDemo {
    private final String author;

    private String[] permissionsToGive = null;
    private String[] permissionsToNeeded = new String[0];
    private String[] permissionsToNotNeeded = new String[0];
    private String[] actions = new String[0];

    public ArrayDemo(String author) {
        this.author = author;
    }


    public ArrayDemo withPermissionsToGive(String[] permissionsToGive) {
        this.permissionsToGive = permissionsToGive;
        return this;
    }

    public ArrayDemo withPermissionsToNeeded(String[] permissionsToNeeded) {
        this.permissionsToNeeded = permissionsToNeeded;
        return this;
    }

    public ArrayDemo withPermissionsToNotNeeded(String[] permissionsToNotNeeded) {
        this.permissionsToNotNeeded = permissionsToNotNeeded;
        return this;
    }


    public ArrayDemo withActions(String[] actions) {
        this.actions = actions;
        return this;
    }


}


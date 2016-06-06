package network.thunder.core.database;

/**
 * Created by Jean-Pierre Rupp on 06/06/16.
 */

public interface PersistentTransaction {
    public void commitTransaction();
    public void rollbackTransaction();
}

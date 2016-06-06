package network.thunder.core.database;

import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.high.Channel;

import java.util.Iterator;
import java.util.Optional;

/**
 * Created by Jean-Pierre Rupp on 06/06/16.
 */

public interface PersistentChannel {
    Optional<Channel> getChannel(PersistentSession session, Integer id);
    Optional<Integer> insertChannel(PersistentSession session, Channel channel);
    Iterator<Channel> getOpenChannels(PersistentSession session);
    Iterator<Channel> getOpenChannelsForNode(PersistentSession session, NodeKey nodekey);
    Iterator<Channel> getChannelsForNode(PersistentSession session, NodeKey nodekey);
    Iterator<Channel> getIPObjectsWithActiveChannel(PersistentSession session);
    void updateChannel(PersistentSession session, Channel channel);
    void deleteChannel(PersistentSession session, Channel channel);
}

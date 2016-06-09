package network.thunder.core.database;

import network.thunder.core.communication.NodeKey;
import org.bitcoinj.core.ECKey;

import javax.persistence.AttributeConverter;

/**
 * Created by xeno on 09/06/16.
 */
public class HibernateConverterNodeKey implements AttributeConverter<NodeKey, byte[]> {
    @Override
    public byte[] convertToDatabaseColumn (NodeKey nodeKey) {
        return nodeKey == null ? null : nodeKey.unwrap() == null ? null : nodeKey.unwrap().toASN1();
    }

    @Override
    public NodeKey convertToEntityAttribute (byte[] bytes) {
        return bytes == null ? null : NodeKey.wrap(ECKey.fromASN1(bytes));
    }
}

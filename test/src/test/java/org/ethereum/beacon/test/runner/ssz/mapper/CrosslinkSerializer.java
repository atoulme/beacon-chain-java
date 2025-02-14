package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.operations.attestation.Crosslink;

public class CrosslinkSerializer implements ObjectSerializer<Crosslink> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;

  public CrosslinkSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Class accepts() {
    return Crosslink.class;
  }

  @Override
  public ObjectNode map(Crosslink instance) {
    ObjectNode crosslink = mapper.createObjectNode();
//    crosslink.set("epoch", ComparableBigIntegerNode.valueOf(instance.getEpoch()));
//    crosslink.put("previous_crosslink_root", instance.getPreviousCrosslinkRoot().toString());
//    crosslink.put("crosslink_data_root", instance.getCrosslinkDataRoot().toString());
    return crosslink;
  }
}

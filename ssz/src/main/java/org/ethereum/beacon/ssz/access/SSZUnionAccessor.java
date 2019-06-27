package org.ethereum.beacon.ssz.access;

import java.util.List;

/**
 * Handles ssz unions, is responsible of accessing its child value,
 * its type index and new instance creation
 */
public interface SSZUnionAccessor extends SSZCompositeAccessor {

  interface UnionAccessor extends CompositeAccessor {

    /**
     * Returns Union children type descriptors
     */
    List<SSZField> getChildDescriptors();

    int getTypeIndex(Object unionInstance);

    @Override
    default int getChildrenCount(Object compositeInstance) {
      return 1;
    }
  }

  UnionAccessor getAccessor(SSZField containerDescriptor);
}

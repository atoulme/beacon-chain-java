package org.ethereum.beacon.ssz;

import net.consensys.cava.ssz.SSZException;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.type.SSZListType;
import org.ethereum.beacon.ssz.type.SSZType;
import org.ethereum.beacon.ssz.type.TypeResolver;
import org.ethereum.beacon.ssz.visitor.SSZHasherSerializer;
import org.ethereum.beacon.ssz.visitor.SSZHasherSerializer.SSZHasherSerializerResult;
import org.ethereum.beacon.ssz.visitor.SSZVisitorHandler;
import org.ethereum.beacon.ssz.visitor.SSZVisitorHost;

import javax.annotation.Nullable;

/** SSZ hasher serializer */
public class HasherSerializer
    implements BytesSerializer, SSZVisitorHandler<SSZHasherSerializerResult> {

  private final SSZVisitorHost sszVisitorHost;
  private final TypeResolver typeResolver;

  public HasherSerializer(SSZVisitorHost sszVisitorHost, TypeResolver typeResolver) {
    this.sszVisitorHost = sszVisitorHost;
    this.typeResolver = typeResolver;
  }

  /**
   * Serializes input to byte[] data
   *
   * @param inputObject input value
   * @param inputClazz Class of value
   * @return SSZ serialization
   */
  @Override
  public <C> byte[] encode(@Nullable C inputObject, Class<? extends C> inputClazz) {
    return visit(inputObject, inputClazz).getSerializedBody().getArrayUnsafe();
  }

  private <C> SSZHasherSerializerResult visit(C input, Class<? extends C> clazz) {
    return visitAny(typeResolver.resolveSSZType(new SSZField(clazz)), input);
  }

  @Override
  public SSZHasherSerializerResult visitAny(SSZType sszType, Object value) {
    return sszVisitorHost.handleAny(sszType, value, new SSZHasherSerializer());
  }

  @Override
  public SSZHasherSerializerResult visitList(
      SSZListType descriptor, Object listValue, int startIdx, int len) {
    return sszVisitorHost.handleSubList(
        descriptor, listValue, startIdx, len, new SSZHasherSerializer());
  }

  public <C> C decode(byte[] data, Class<? extends C> clazz) {
    throw new SSZException("Hasher doesn't support decoding!");
  }
}

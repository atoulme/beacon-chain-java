package tech.pegasys.artemis.util.collections;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public interface WriteVector<IndexType extends Number, ValueType>
    extends ReadVector<IndexType, ValueType> {

  static <IndexType extends Number, ValueType> WriteVector<IndexType, ValueType>
      wrap(List<ValueType> srcList, Function<Integer, IndexType> indexConverter) {
    return ListImpl.wrap(srcList, indexConverter);
  }

  static <IndexType extends Number, ValueType> WriteVector<IndexType, ValueType>
      create(Function<Integer, IndexType> indexConverter) {
    return new ListImpl<>(indexConverter);
  }

  void sort(Comparator<? super ValueType> c);

  ValueType set(IndexType index, ValueType element);

  void setAll(ValueType singleValue);

  void setAll(Iterable<ValueType> singleValue);

  ReadList<IndexType, ValueType> createImmutableCopy();

  default ValueType update(IndexType index, Function<ValueType, ValueType> updater) {
    ValueType newValue = updater.apply(get(index));
    set(index, newValue);
    return newValue;
  }
}

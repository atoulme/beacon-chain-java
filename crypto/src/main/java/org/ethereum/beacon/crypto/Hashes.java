package org.ethereum.beacon.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.BytesValue;
import org.ethereum.beacon.types.Hash48;

/** Utility methods to calculate message hashes */
public abstract class Hashes {
  private Hashes() {}

  private static final BouncyCastleProvider PROVIDER;

  private static final String KECCAK256 = "KECCAK-256";
  private static final String KECCAK512 = "KECCAK-512";

  static {
    Security.addProvider(PROVIDER = new BouncyCastleProvider());
  }

  /**
   * A low level method that calculates hash using give algorithm.
   *
   * @param input a message.
   * @param algorithm an algorithm.
   * @return the hash.
   */
  private static byte[] digestUsingAlgorithm(BytesValue input, String algorithm) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(algorithm, PROVIDER);
      input.update(digest);
      return digest.digest();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Calculates keccak256 hash.
   *
   * @param input input message.
   * @return the hash.
   */
  public static Hash32 keccak256(BytesValue input) {
    byte[] output = digestUsingAlgorithm(input, KECCAK256);
    return Hash32.wrap(Bytes32.wrap(output));
  }

  /**
   * Calculates keccak384 by using first 384 bits of {@code KECCAK-512} output.
   *
   * @param input input message.
   * @return the hash.
   */
  public static Hash48 keccak384(BytesValue input) {
    byte[] output = digestUsingAlgorithm(input, KECCAK512);
    return Hash48.wrap(Bytes48.wrap(output, 0));
  }
}

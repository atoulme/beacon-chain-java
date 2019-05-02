package org.ethereum.beacon.test;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.test.runner.shuffle.ShuffleRunner;
import org.ethereum.beacon.test.type.shuffle.ShuffleTest;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ShuffleTests extends TestUtils {
  private String TESTS_DIR = "shuffling";
  private String TESTS_SUBDIR = "core";

  @Test
  public void testShuffling() {
    Path testFileDir = Paths.get(PATH_TO_TESTS, TESTS_DIR, TESTS_SUBDIR);
    runTestsInResourceDir(
        testFileDir,
        ShuffleTest.class,
        input -> {
          ShuffleRunner testRunner =
              new ShuffleRunner(
                  input.getValue0(),
                  input.getValue1(),
                  objects ->
                      input
                          .getValue1()
                          .compute_committee(
                              objects.getValue0(), 0, objects.getValue2(), objects.getValue1()));
          return testRunner.run();
        });
  }

  /**
   * Runs tests on optimized version of get_shuffling, like in {@link
   * BeaconChainSpec#compute_committee2(List, Hash32, int, int)}
   */
  @Test
  public void testShuffling2() {
    Path testFileDir = Paths.get(PATH_TO_TESTS, TESTS_DIR, TESTS_SUBDIR);
    runTestsInResourceDir(
        testFileDir,
        ShuffleTest.class,
        input -> {
          ShuffleRunner testRunner =
              new ShuffleRunner(
                  input.getValue0(),
                  input.getValue1(),
                  objects ->
                      input
                          .getValue1()
                          .compute_committee2(
                              objects.getValue0(), 0, objects.getValue2(), objects.getValue1()));
          return testRunner.run();
        });
  }
}

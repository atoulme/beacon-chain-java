package org.ethereum.beacon.wire.net.netty;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultMessageSizeEstimator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.ethereum.beacon.wire.net.Client;

public class NettyClient implements Client<SocketAddress> {
  private final NioEventLoopGroup workerGroup;

  public NettyClient(NioEventLoopGroup workerGroup) {
    this.workerGroup = workerGroup;
  }

  public NettyClient(Executor executor) {
    this(new NioEventLoopGroup(2, executor));
  }

  public NettyClient() {
    this(new NioEventLoopGroup(2,
        new ThreadFactoryBuilder().setNameFormat("netty-client-worker-%d").build()));
  }

  @Override
  public CompletableFuture<NettyChannel> connect(SocketAddress address) {
    Bootstrap b = new Bootstrap();
    b.group(workerGroup);
    b.channel(NioSocketChannel.class);

    b.option(ChannelOption.SO_KEEPALIVE, true);
    b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT);
    b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15 * 1000);
    b.remoteAddress(address);

    CompletableFuture<NettyChannel> ret = new CompletableFuture<>();

    b.handler(new NettyChannelInitializer(ret::complete));

    // Start the client.
    b.connect().addListener((ChannelFutureListener) future -> {
      try {
        future.get();
      } catch (Exception e) {
        ret.completeExceptionally(e);
      }
    });

    return ret;
  }
}

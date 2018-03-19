package vsse.vsse_and;

import android.net.Uri;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import vsse.client.ClientContext;
import vsse.proto.Filedesc.Credential;
import vsse.proto.RequestOuterClass;
import vsse.proto.RequestOuterClass.Request;
import vsse.proto.RequestOuterClass.SearchRequest.MsgCase;
import vsse.proto.ResponseOuterClass;
import vsse.proto.ResponseOuterClass.SearchResponse;

/**
 * Created by y on 2018/3/15.
 */

public class Connection {
    private final String host;
    private final int port;
    private final ClientContext clientContext;
    private static NioEventLoopGroup workerGroup = new NioEventLoopGroup();
    private SocketChannel channel;
    private final CompletableFuture<Void> shutdownFuture = new CompletableFuture<>();
    private Map<Long, CompletableFuture> waitingMsg = new TreeMap<>();

    public Connection(Uri uri) {
        this.host = uri.getHost();
        this.port = uri.getPort();
        this.clientContext = new ClientContext(Credential.newBuilder()
                .setK(uri.getQueryParameter("k"))
                .setK0(uri.getQueryParameter("k0"))
                .setK1(uri.getQueryParameter("k1")).build());
    }

    public void open() throws Throwable {
        Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.handler(
                new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                        ch.pipeline().addLast(new ProtobufDecoder(ResponseOuterClass.Response.getDefaultInstance()));
                        ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                        ch.pipeline().addLast(new ProtobufEncoder());
                        ch.pipeline().addLast(new ChannelInboundHelper());
                    }
                });
        ChannelFuture f = b.connect(host, port);

        try {
            f.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!f.isSuccess()) {
            throw f.cause();
        }
    }

    public List<String> search(MsgCase type, String... args) throws Exception {
        RequestOuterClass.SearchRequest query = clientContext.createQuery(type, args);

        Request req = Request.newBuilder()
                .setSearchRequest(query)
                .setSequence(System.nanoTime())
                .build();

        CompletableFuture<SearchResponse> ret = new CompletableFuture<>();
        waitingMsg.put(req.getSequence(), ret);
        channel.writeAndFlush(req);
        SearchResponse resp = ret.get();
        clientContext.verify(query, resp);
        return clientContext.extractFiles(resp)
                .stream()
                .map(clientContext.getSecurityUtil()::decrypt)
                .map(String::new)
                .collect(Collectors.toList());
    }

    public void close() {
        channel.shutdown();
    }

    private class ChannelInboundHelper extends ChannelInboundHandlerAdapter {

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            Connection.this.channel = (SocketChannel) ctx.channel();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            shutdownFuture.completeExceptionally(cause);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ResponseOuterClass.Response response = (ResponseOuterClass.Response) msg;
            long sequence = response.getReqSequence();
            if (waitingMsg.containsKey(sequence)) {
                Object ret = null;
                switch (response.getMsgCase()) {

                    case UPLOAD_RESPONSE:
                        ret = response.getUploadResponse();
                        break;
                    case SEARCH_RESPONSE:
                        ret = response.getSearchResponse();
                        break;
                    case MSG_NOT_SET:
                        ret = response;
                        break;
                }
                waitingMsg.get(sequence).complete(ret);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            shutdownFuture.complete(null);
        }
    }
}

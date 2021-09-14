package cn.wx.proxy.verticle;

import cn.wx.proxy.contant.SocketParams;
import cn.wx.proxy.contant.SocketProxyStage;
import cn.wx.proxy.handler.SocketTunnel;
import cn.wx.proxy.util.SocketParseUtils;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;

import io.vertx.mutiny.core.net.SocketAddress;
import io.vertx.mutiny.core.Context;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.net.NetClient;
import io.vertx.mutiny.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Set;

/**
 * socket 代理
 *
 * @author wangxin
 */
@Slf4j
public class SocketProxyVerticle extends AbstractVerticle {

  private NetClient netClient;

  @Override
  public Uni<Void> asyncStart() {
    netClient = vertx.createNetClient();
    return vertx.createNetServer()
      .connectHandler(socket -> {
          socket.handler(buff -> {
            SocketProxyStage stage = vertx.getOrCreateContext().get(socket.writeHandlerID() + ":stage");
            if (stage == null) {
              handlerHandshake(vertx.getOrCreateContext(), socket, buff.getBytes()).subscribe().with(v -> {

              });
            } else if (SocketProxyStage.MethodAuth.equals(stage)) {
              handlerMethodAuth(vertx.getOrCreateContext(), socket, buff);
            } else if (SocketProxyStage.Cmd.equals(stage)) {
              handlerCmd(vertx, socket, buff.getBytes());
            }
          });
        }
      )
      .listen(1080)
      .onItem().invoke(() -> log.info("socket 代理启功在端口 {}", 1080))
      .onFailure().invoke(t -> {
        throw new RuntimeException(t);
      })
      .replaceWithVoid();
  }


  /**
   * 处理握手
   *
   * @param context 存储值
   * @param socket  s
   * @param bytes   b
   */
  private Uni<Void> handlerHandshake(Context context, NetSocket socket, byte[] bytes) {
    // 解析socket版本
    SocketParams.VER version = SocketParseUtils.parseVer(bytes);
    if (version == null) {
      socket.close();
      return Uni.createFrom().failure(new RuntimeException("无法解析版本"));
    }

    // 解析授权方式
    Set<SocketParams.METHOD> auths = SocketParseUtils.parseMethod(bytes);
    if (auths.contains(SocketParams.METHOD.None)) {
      context.put(socket.writeHandlerID() + ":stage", SocketProxyStage.Cmd);
      return socket.write(Buffer.buffer()
        .appendByte(version.byteCode())
        .appendByte(SocketParams.METHOD.None.byteCode()));
    } else if (auths.contains(SocketParams.METHOD.Passwd)) {
      context.put(socket.writeHandlerID() + ":stage", SocketProxyStage.MethodAuth);
      return socket.write(Buffer.buffer()
        .appendByte(version.byteCode())
        .appendByte(SocketParams.METHOD.Passwd.byteCode()));
    } else {
      // 不支持其他登录方式
      return socket.end(Buffer.buffer()
        .appendByte(version.byteCode())
        .appendByte(SocketParams.METHOD.NoAcceptable.byteCode()));
    }
  }


  /**
   * 处理客户端登录
   *
   * @param context c
   * @param socket  s
   * @param buff    b
   */
  private void handlerMethodAuth(Context context, NetSocket socket, Buffer buff) {
    byte[] bytes = buff.getBytes();

    int usernameStartIdx = 2;
    int usernameLen = bytes[1];
    int passwordStartIdx = usernameStartIdx + usernameLen + 1;
    int passwordLen = bytes[passwordStartIdx - 1];

    String username = new String(Arrays.copyOfRange(bytes, usernameStartIdx, usernameStartIdx + usernameLen));
    String password = new String(Arrays.copyOfRange(bytes, passwordStartIdx, passwordStartIdx + passwordLen));

    log.info("{} {}:{}", "auth", username, password);

    context.put(socket.writeHandlerID() + ":stage", SocketProxyStage.Cmd);

    // 响应成功
    socket.write(
      Buffer.buffer()
        .appendByte(SocketParams.METHOD_PASSWD_VER)
        .appendByte(SocketParams.MethodPasswdStatus.Success.byteCode())
    );
  }


  /**
   * 处理客户端命令
   *
   * @param vertx  c
   * @param socket s
   * @param bytes  b
   */
  private void handlerCmd(Vertx vertx, NetSocket socket, byte[] bytes) {

    // 解析socket版本
    SocketParams.VER version = SocketParams.VER.valueOfCode(bytes[0]);
    if (version == null) {
      socket.close();
      return;
    }

    // 解析cmd
    SocketParams.CMD cmd = SocketParams.CMD.valueOfCode(bytes[1]);

    // 解析地址
    SocketAddress addr = SocketParseUtils.parseAddr(bytes);

    log.info("{}(4.4) {} >> {}", cmd, socket.remoteAddress(), addr);

    // 建立和远程服务器连接
    netClient.connect(addr).subscribe().with(
      remoteSocket -> {
        // 建立隧道

        new SocketTunnel(vertx, socket, remoteSocket);

        // 响应客户端成功
        byte[] clone = bytes.clone();
        clone[1] = SocketParams.REP.Succeeded.byteCode();
        socket.write(Buffer.buffer().appendBytes(clone));
      },
      t -> {

        log.error("连接远程服务器失败", t);
        // 响应失败
        byte[] clone = bytes.clone();
        clone[1] = SocketParams.REP.ConnectionRefused.byteCode();
        socket.write(
          Buffer.buffer().appendBytes(clone)
        );
      }
    );
  }

}

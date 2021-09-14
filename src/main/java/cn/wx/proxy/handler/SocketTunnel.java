package cn.wx.proxy.handler;


import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

/**
 * client >>>> proxy(own) >>>> remote
 *  - 每一个代理请求都会创建一个处理器
 *
 * @author wangxin
 */
@Slf4j
public class SocketTunnel {
  private Vertx vertx;
  private NetSocket clientSocket;
  private NetSocket remoteSocket;


  public SocketTunnel(Vertx vertx, NetSocket clientSocket, NetSocket remoteSocket) {
    this.vertx = vertx;
    this.clientSocket = clientSocket;
    this.remoteSocket = remoteSocket;
    init();
  }

  private void init() {

    this.clientSocket.closeHandler(() -> remoteSocket.close());
    this.remoteSocket.closeHandler(() -> clientSocket.close());

    // client >>> remote
    this.clientSocket.handler(buff -> {
      log.info("{} {} >> {}", "Send", clientSocket.remoteAddress(), remoteSocket.remoteAddress());
      remoteSocket.write(buff);
    });
    // remote >>> client
    this.remoteSocket.handler(buff -> {
      log.info("{} {} << {}", "Rev", clientSocket.remoteAddress(), remoteSocket.remoteAddress());
      clientSocket.write(buff);
    });
  }

}

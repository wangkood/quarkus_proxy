package cn.wx.proxy.verticle;

import cn.wx.proxy.config.HttpProxyConfig;
import io.quarkus.runtime.StartupEvent;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpClient;
import io.vertx.mutiny.core.http.HttpServer;
import io.vertx.mutiny.core.http.HttpServerRequest;
import io.vertx.mutiny.core.net.NetClient;
import io.vertx.mutiny.core.net.SocketAddress;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

/**
 * http & https 代理
 *
 * @author wangxin
 */
@Slf4j
@ApplicationScoped
public class HttpProxyVerticle extends AbstractVerticle {

  private static NetClient netClient;
  private static HttpClient httpClient;
  private static HttpServer httpServer;

  @Inject
  HttpProxyConfig httpProxyConfig;

  @Inject
  Vertx vertx;


  @Override
  public Uni<Void> asyncStart() {
    netClient = vertx.createNetClient();
    httpClient = vertx.createHttpClient();
    httpServer = vertx.createHttpServer();
    return httpServer
      .requestHandler(request -> {
        System.out.println(request.uri());
        if (request.method().equals(HttpMethod.CONNECT)) {
          handlerTunnelProxy(request);
        } else if (request.headers().contains("Proxy-Connection")) {
          handlerHttpProxy(request);
        } else {
          request.response().end("HELLO, I`M PROXY SERVER");
        }
      })
      .listen(httpProxyConfig.port())
      .onItem().invoke(() -> log.info("http 代理服务器启功在端口 {}", httpProxyConfig.port()))
      .onFailure().invoke(t -> {
        throw new RuntimeException(t);
      })
      .replaceWithVoid();
  }

  /**
   * 处理 https 隧道请求
   *
   * @param clientReq 请求
   */
  private void handlerTunnelProxy(HttpServerRequest clientReq) {

    // 建立和目标服务器之间的连接
    String[] hostSplit = clientReq.uri().split(":");
    netClient.connect(SocketAddress.inetSocketAddress(Integer.parseInt(hostSplit[1]), hostSplit[0])).subscribe().with(
      remoteSocket -> {
        // 建立客户和目标服务器之间隧道
        clientReq.toNetSocket().subscribe().with(
          clientSocket -> {
            clientSocket.handler(remoteSocket::write).closeHandler(remoteSocket::close);
            remoteSocket.handler(clientSocket::write).closeHandler(clientSocket::close);
            log.info(
              "Tunnel {}:{} ----> {}:{}",
              clientSocket.remoteAddress().host(),
              clientSocket.remoteAddress().port(),
              remoteSocket.remoteAddress().host(),
              remoteSocket.remoteAddress().port()
            );
          },
          t -> log.error("", t)
        );
      },
      t -> {
        log.error("链接远端服务器失败", t);
        clientReq.connection().close();
      }
    );
  }

  /**
   * 处理 http 代理请求
   *
   * @param clientReq 请求
   */
  private void handlerHttpProxy(HttpServerRequest clientReq) {
//    clientReq.body().onSuccess(buff ->
//      httpClient.request(clientReq.method(), clientReq.host(), clientReq.uri())
//        .onFailure(t -> log.error("", t))
//        .onSuccess(httpClientReq -> {
//          // 复制请求头
//          for (Map.Entry<String, String> header : clientReq.headers()) {
//            if (Objects.equals("Proxy-Connection", header.getKey())) {
//              httpClientReq.putHeader(HttpHeaders.CONNECTION, header.getValue());
//            }
//            httpClientReq.putHeader(header.getKey(), header.getValue());
//          }
//          // 复制请求体并发送
//          httpClientReq.send(buff).onSuccess(remoteResp -> {
//            // 复制远程服务器响应头
//            for (Map.Entry<String, String> header : remoteResp.headers()) {
//              clientReq.response().putHeader(header.getKey(), header.getValue());
//            }
//            remoteResp.body().onSuccess(respBuff -> {
//              clientReq.response().setStatusCode(remoteResp.statusCode()).end(respBuff);
//              log.info("Proxy {} ----> {}", clientReq.remoteAddress(), clientReq.uri());
//            });
//          });
//        })
//    );
  }

}

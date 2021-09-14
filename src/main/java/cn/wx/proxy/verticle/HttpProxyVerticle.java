package cn.wx.proxy.verticle;

import cn.wx.proxy.config.HttpProxyConfig;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.http.HttpClient;
import io.vertx.mutiny.core.http.HttpServerRequest;
import io.vertx.mutiny.core.net.NetClient;
import io.vertx.mutiny.core.net.SocketAddress;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;

/**
 * http & https 代理
 *
 * @author wangxin
 */
@Slf4j
public class HttpProxyVerticle extends AbstractVerticle {

  private static final String HTTP_PROXY_CONNECTION_HEADER = "Proxy-Connection";

//  private final HttpProxyConfig config;

  private NetClient netClient;
  private HttpClient httpClient;

  @Override
  public Uni<Void> asyncStart() {
    netClient = vertx.createNetClient();
    httpClient = vertx.createHttpClient();

    return vertx.createHttpServer()
      .requestHandler(request -> {
        if (request.method().equals(HttpMethod.CONNECT)) {
          handlerTunnelProxy(request);
        } else if (request.headers().contains(HTTP_PROXY_CONNECTION_HEADER)) {
          handlerHttpProxy(request);
        } else {
          request.response().end("HELLO, I`M PROXY SERVER").subscribe().with(v -> {

          });
        }
      })
      .listen(8090)
      .onItem().invoke(() -> log.info("http 代理服务器启功在端口 {}", 8090))
      .onFailure().transform(t -> t)
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
        log.error("连接远程服务器失败", t);
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
    clientReq.body().subscribe().with(buff ->
      httpClient.request(clientReq.method(), clientReq.host(), clientReq.uri()).subscribe().with(
        httpClientReq -> {
          // 复制请求头
          for (Map.Entry<String, String> header : clientReq.headers()) {
            if (Objects.equals(HTTP_PROXY_CONNECTION_HEADER, header.getKey())) {
              httpClientReq.putHeader(HttpHeaders.CONNECTION, header.getValue());
            }
            httpClientReq.putHeader(header.getKey(), header.getValue());
          }
          // 复制请求体并发送
          httpClientReq.send(buff).subscribe().with(remoteResp -> {
            // 复制远程服务器响应给客户端
            for (Map.Entry<String, String> header : remoteResp.headers()) {
              clientReq.response().putHeader(header.getKey(), header.getValue());
            }
            remoteResp.body().subscribe().with(
              respBuff -> {
                clientReq.response().setStatusCode(remoteResp.statusCode()).end(respBuff);
                log.info("Proxy {} ----> {}", clientReq.remoteAddress(), clientReq.uri());
              }
            );
          });
        },
        t -> log.error("连接远程服务器失败", t)
      )
    );
  }




}

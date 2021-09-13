package cn.wx.proxy;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.mutiny.core.Vertx;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;

/**
 * 一个代理服务器
 *
 * @author wangxin
 */
@QuarkusMain
@ApplicationScoped
public class ProxyApplication implements QuarkusApplication {

  @Override
  public int run(String... args) {
    return 10;
  }

  public void init(@Observes StartupEvent e, Vertx vertx, Instance<AbstractVerticle> verticles) {
    for (AbstractVerticle verticle : verticles) {
      vertx.deployVerticle(verticle).await().indefinitely();
    }
  }

  public static void main(String[] args) {

    Quarkus.run(ProxyApplication.class, args);
  }
}

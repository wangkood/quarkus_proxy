package cn.wx.proxy.config;

import cn.wx.proxy.verticle.HttpProxyVerticle;
import cn.wx.proxy.verticle.SocketProxyVerticle;
import io.quarkus.runtime.StartupEvent;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * 部署verticle
 *
 * @author wangxin
 */
@Slf4j
@ApplicationScoped
public class BaseConfig {


  public void init(@Observes StartupEvent e, Vertx vertx) {
    vertx.setTimer(1000, i -> {
      // 部署http代理
      vertx.deployVerticleAndForget(
        "cn.wx.proxy.verticle.HttpProxyVerticle",
        new DeploymentOptions()
          .setInstances(Runtime.getRuntime().availableProcessors() * 2)
          .setMaxWorkerExecuteTime(5)
          .setMaxWorkerExecuteTimeUnit(TimeUnit.MILLISECONDS)
      );

      // 部署socket代理
      vertx.deployVerticleAndForget(
        "cn.wx.proxy.verticle.SocketProxyVerticle",
        new DeploymentOptions()
          .setInstances(Runtime.getRuntime().availableProcessors() * 2)
          .setMaxWorkerExecuteTime(5)
          .setMaxWorkerExecuteTimeUnit(TimeUnit.MILLISECONDS)
      );
    });


  }


}

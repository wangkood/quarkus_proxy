package cn.wx.proxy.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "proxy.http")
public interface HttpProxyConfig {

  @WithDefault("8090")
  Integer port();
}
